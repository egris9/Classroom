package com.Classroom_ai.Classroom.generation;

import com.Classroom_ai.Classroom.api.ExerciseResponse;
import com.Classroom_ai.Classroom.api.ExerciseSetResponse;
import com.Classroom_ai.Classroom.api.SummaryResponse;
import com.Classroom_ai.Classroom.auth.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
    public SummaryResponse requestSummary(@PathVariable Long fileId) {
        return respond(generation.requestSummary(userService.getAuthenticatedUser(), fileId));
    }

    @GetMapping("/summaries")
    public List<SummaryResponse> summaries(@PathVariable Long fileId) {
        return generation.summariesOf(userService.getAuthenticatedUser(), fileId).stream()
                .map(GenerationController::respond).toList();
    }

    @PostMapping("/exercise-sets")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ExerciseSetResponse requestExerciseSet(@PathVariable Long fileId) {
        return respond(generation.requestExerciseSet(userService.getAuthenticatedUser(), fileId));
    }

    @GetMapping("/exercise-sets")
    public List<ExerciseSetResponse> exerciseSets(@PathVariable Long fileId) {
        return generation.exerciseSetsOf(userService.getAuthenticatedUser(), fileId).stream()
                .map(this::respond).toList();
    }

    private static SummaryResponse respond(Summary s) {
        return new SummaryResponse(s.getId(), s.getStatus().name(), s.getContent(), s.isPublished(),
                s.getAuthor().getId(), s.getModel(), s.getFailureCode(), s.getFailureMessage(), s.getCreatedAt());
    }

    private ExerciseSetResponse respond(ExerciseSet e) {
        List<ExerciseResponse> exercises = generation.exercisesOf(e).stream()
                .map(x -> new ExerciseResponse(x.question(), x.answer())).toList();
        return new ExerciseSetResponse(e.getId(), e.getStatus().name(), exercises, e.isPublished(),
                e.getAuthor().getId(), e.getModel(), e.getFailureCode(), e.getFailureMessage(), e.getCreatedAt());
    }
}
