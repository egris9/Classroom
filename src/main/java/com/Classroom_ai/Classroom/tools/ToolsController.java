package com.Classroom_ai.Classroom.tools;

import com.Classroom_ai.Classroom.api.ExerciseResponse;
import com.Classroom_ai.Classroom.api.ToolsExerciseSetResponse;
import com.Classroom_ai.Classroom.api.ToolsSummaryResponse;
import com.Classroom_ai.Classroom.api.ToolsTextRequest;
import com.Classroom_ai.Classroom.auth.User;
import com.Classroom_ai.Classroom.auth.UserService;
import com.Classroom_ai.Classroom.generation.GenerationService;
import com.Classroom_ai.Classroom.generation.PdfText;
import com.Classroom_ai.Classroom.generation.TextGeneration;
import com.Classroom_ai.Classroom.material.FileTooLargeException;
import com.Classroom_ai.Classroom.material.Materials;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.util.unit.DataSize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.Function;

/**
 * Summaries and Exercise sets for text or a PDF that belongs to no course. Open to anyone: a visitor with no account
 * gets one use ({@link TrialGate}) and smaller limits than a signed-in user. Nothing is stored. A request waits for
 * the single generation thread, behind any course jobs, and gives up with 504 after {@code generation.timeout}.
 */
@RestController
@RequestMapping("/api/tools")
public class ToolsController {

    private record Limits(int maxChars, long maxPdfBytes, String tooLong) {
    }

    /** What was sent: checked before it costs anything, and read only once the visitor is admitted. */
    private interface Input {
        void check(Limits limits);

        String text(Limits limits);
    }

    private final TrialGate gate;
    private final UserService users;
    private final GenerationService generation;
    private final TextGeneration textGeneration;
    private final PdfText pdfText;
    private final Materials materials;
    private final Limits anonymous;
    private final Limits signedIn;

    public ToolsController(TrialGate gate, UserService users, GenerationService generation,
                           TextGeneration textGeneration, PdfText pdfText, Materials materials,
                           @Value("${tools.anon.max-chars:20000}") int anonMaxChars,
                           @Value("${tools.anon.max-pdf:5MB}") DataSize anonMaxPdf,
                           @Value("${tools.max-chars:100000}") int maxChars,
                           @Value("${upload.max-size}") DataSize maxPdf) {
        this.gate = gate;
        this.users = users;
        this.generation = generation;
        this.textGeneration = textGeneration;
        this.pdfText = pdfText;
        this.materials = materials;
        this.anonymous = new Limits(anonMaxChars, anonMaxPdf.toBytes(),
                "That is more than the " + anonMaxChars + " characters a visitor can send. Sign up to send more.");
        this.signedIn = new Limits(maxChars, maxPdf.toBytes(),
                "That is more than the " + maxChars + " characters the AI tools accept.");
    }

    @PostMapping(value = "/summaries", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ToolsSummaryResponse summariseText(@RequestBody @Valid ToolsTextRequest body,
                                              HttpServletRequest request, HttpServletResponse response) {
        return summarise(new TextInput(body.text()), request, response);
    }

    @PostMapping(value = "/summaries", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ToolsSummaryResponse summarisePdf(@RequestPart("file") MultipartFile file,
                                             HttpServletRequest request, HttpServletResponse response) {
        return summarise(new PdfInput(file), request, response);
    }

    @PostMapping(value = "/exercise-sets", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ToolsExerciseSetResponse exercisesFromText(@RequestBody @Valid ToolsTextRequest body,
                                                      HttpServletRequest request, HttpServletResponse response) {
        return exercises(new TextInput(body.text()), request, response);
    }

    @PostMapping(value = "/exercise-sets", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ToolsExerciseSetResponse exercisesFromPdf(@RequestPart("file") MultipartFile file,
                                                     HttpServletRequest request, HttpServletResponse response) {
        return exercises(new PdfInput(file), request, response);
    }

    private ToolsSummaryResponse summarise(Input input, HttpServletRequest request, HttpServletResponse response) {
        return run(input, request, response, text ->
                new ToolsSummaryResponse(textGeneration.summarize(text), textGeneration.modelName()));
    }

    private ToolsExerciseSetResponse exercises(Input input, HttpServletRequest request, HttpServletResponse response) {
        return run(input, request, response, text -> new ToolsExerciseSetResponse(
                textGeneration.generateExercises(text, GenerationService.EXERCISES_PER_SET).stream()
                        .map(exercise -> new ExerciseResponse(exercise.question(), exercise.answer())).toList(),
                textGeneration.modelName()));
    }

    /**
     * Input that is refused for its size or type costs the visitor nothing. Once admitted, the visitor's try is
     * spent whatever happens next, so a failure cannot be retried for free. The PDF is parsed only after that.
     */
    private <T> T run(Input input, HttpServletRequest request, HttpServletResponse response, Function<String, T> work) {
        User user = users.findAuthenticatedUser().orElse(null);
        Limits limits = user == null ? anonymous : signedIn;
        input.check(limits);
        gate.admit(request, response, user);
        String text = input.text(limits);
        return generation.runAdHoc(() -> work.apply(text));
    }

    private record TextInput(String value) implements Input {
        @Override
        public void check(Limits limits) {
            if (value.length() > limits.maxChars()) {
                throw new TextTooLongException(limits.tooLong());
            }
        }

        @Override
        public String text(Limits limits) {
            return value;
        }
    }

    private final class PdfInput implements Input {
        private final MultipartFile file;

        PdfInput(MultipartFile file) {
            this.file = file;
        }

        @Override
        public void check(Limits limits) {
            if (file.getSize() > limits.maxPdfBytes()) {
                throw new FileTooLargeException("The file is larger than "
                        + DataSize.ofBytes(limits.maxPdfBytes()).toMegabytes() + "MB.");
            }
            try {
                materials.requirePdf(file);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public String text(Limits limits) {
            String text;
            try {
                text = pdfText.extract(file.getBytes());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            if (text.length() > limits.maxChars()) {
                throw new TextTooLongException(limits.tooLong());
            }
            return text;
        }
    }
}
