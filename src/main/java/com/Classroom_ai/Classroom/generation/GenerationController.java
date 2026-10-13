package com.Classroom_ai.Classroom.generation;

import com.Classroom_ai.Classroom.api.ExerciseResponse;
import com.Classroom_ai.Classroom.api.ExerciseSetRequest;
import com.Classroom_ai.Classroom.api.ExerciseSetResponse;
import com.Classroom_ai.Classroom.api.SummaryRequest;
import com.Classroom_ai.Classroom.api.SummaryResponse;
import com.Classroom_ai.Classroom.auth.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/files/{fileId}")
public class GenerationController {
    private final GenerationService generation;
    private final UserService userService;

    public GenerationController(GenerationService generation, UserService userService) {
        this.generation = generation;
        this.userService = userService;
    }

    @PostMapping("/summaries")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public SummaryResponse requestSummary(@PathVariable Long fileId,
                                          @RequestBody(required = false) SummaryRequest body) {
        SummaryLevel level = body == null || body.level() == null ? SummaryLevel.MEDIUM : body.level();
        return respond(generation.requestSummary(userService.getAuthenticatedUser(), fileId, level));
    }

    @GetMapping("/summaries")
    public List<SummaryResponse> summaries(@PathVariable Long fileId) {
        return generation.summariesOf(userService.getAuthenticatedUser(), fileId).stream()
                .map(GenerationController::respond).toList();
    }

    @PostMapping("/exercise-sets")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ExerciseSetResponse requestExerciseSet(@PathVariable Long fileId,
                                                  @Valid @RequestBody(required = false) ExerciseSetRequest body) {
        ExerciseOptions options = body == null ? ExerciseOptions.DEFAULT
                : ExerciseOptions.of(body.types(), body.difficulty(), body.count());
        return respond(generation.requestExerciseSet(userService.getAuthenticatedUser(), fileId, options));
    }

    @GetMapping("/exercise-sets")
    public List<ExerciseSetResponse> exerciseSets(@PathVariable Long fileId) {
        return generation.exerciseSetsOf(userService.getAuthenticatedUser(), fileId).stream()
                .map(this::respond).toList();
    }

    private static SummaryResponse respond(Summary s) {
        return new SummaryResponse(s.getId(), s.getStatus().name(), s.getContent(), s.getLevel().name(), s.isPublished(),
                s.getAuthor().getId(), s.getModel(), s.getFailureCode(), s.getFailureMessage(), s.getCreatedAt());
    }

    private ExerciseSetResponse respond(ExerciseSet e) {
        List<ExerciseResponse> exercises = generation.exercisesOf(e).stream()
                .map(ExerciseResponse::of).toList();
        ExerciseOptions options = e.getOptions();
        int count = e.getAskedCount() != null ? e.getAskedCount() : exercises.size();
        return new ExerciseSetResponse(e.getId(), e.getStatus().name(), exercises,
                options.types().stream().map(ExerciseType::name).toList(), options.difficulty().name(), count,
                e.isPublished(), e.getAuthor().getId(), e.getModel(), e.getFailureCode(), e.getFailureMessage(),
                e.getCreatedAt());
    }
}
