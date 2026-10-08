package com.Classroom_ai.Classroom.generation;

import com.Classroom_ai.Classroom.auth.User;
import com.Classroom_ai.Classroom.course.CourseFile;
import com.Classroom_ai.Classroom.course.CourseFileService;
import com.Classroom_ai.Classroom.material.Materials;
import com.Classroom_ai.Classroom.material.StoredFileMissingException;
import com.Classroom_ai.Classroom.membership.Membership;
import com.Classroom_ai.Classroom.membership.Role;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Requests a Summary or an Exercise set for a CourseFile and runs it in the background. A request is stored as
 * PENDING and returned at once; the job then reads the PDF, asks {@link TextGeneration}, and stores either the
 * result (DONE) or why it failed (FAILED). A Teacher's result is published to the course, a Student's is private.
 */
@Service
public class GenerationService {

    public static final int EXERCISES_PER_SET = 5;

    private static final Logger log = LoggerFactory.getLogger(GenerationService.class);
    private static final TypeReference<List<Exercise>> EXERCISE_LIST = new TypeReference<>() {
    };

    private final SummaryRepository summaries;
    private final ExerciseSetRepository exerciseSets;
    private final CourseFileService fileService;
    private final Membership membership;
    private final Materials materials;
    private final PdfText pdfText;
    private final TextGeneration textGeneration;
    private final ObjectMapper mapper;
    private final Duration timeout;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "generation");
        thread.setDaemon(true);
        return thread;
    });

    public GenerationService(SummaryRepository summaries, ExerciseSetRepository exerciseSets,
                             CourseFileService fileService, Membership membership, Materials materials,
                             PdfText pdfText, TextGeneration textGeneration, ObjectMapper mapper,
                             @Value("${generation.timeout:120s}") Duration timeout) {
        this.timeout = timeout;
        this.summaries = summaries;
        this.exerciseSets = exerciseSets;
        this.fileService = fileService;
        this.membership = membership;
        this.materials = materials;
        this.pdfText = pdfText;
        this.textGeneration = textGeneration;
        this.mapper = mapper;
    }

    /** Requests wait in memory, so any still PENDING after a restart will never run. */
    @EventListener(ApplicationReadyEvent.class)
    public void failInterrupted() {
        if ("fake".equals(textGeneration.modelName())) {
            log.warn("Text generation is running the demo adapter: summaries only quote the PDF. "
                    + "Set GENERATION_ADAPTER=local, GENERATION_BASE_URL and GENERATION_MODEL to use a model.");
        } else {
            log.info("Text generation is using the model {}", textGeneration.modelName());
        }
        failPending(summaries);
        failPending(exerciseSets);
    }

    private <T extends Generated> void failPending(GeneratedRepository<T> repository) {
        List<T> interrupted = repository.findByStatus(Status.PENDING);
        for (T item : interrupted) {
            item.setStatus(Status.FAILED);
            item.setFailureCode("INTERRUPTED");
            item.setFailureMessage("The server restarted before this finished. Please ask again.");
        }
        repository.saveAll(interrupted);
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }

    /**
     * Queues work that belongs to no course file (the AI tools page) behind the course jobs, on the same single
     * generation thread, so the model is never asked two things at once. Cancelling the future stops the work,
     * whether it is still waiting or already running.
     */
    public <T> Future<T> startAdHoc(Supplier<T> work) {
        Callable<T> job = work::get;
        return executor.submit(job);
    }

    /**
     * {@link #startAdHoc} and wait for the result. Waits at most {@code generation.timeout}, queue time included;
     * then the work is cancelled and this throws {@code MODEL_TIMEOUT}. A failure of the work, such as a
     * {@link GenerationFailure}, is rethrown as it is.
     */
    public <T> T runAdHoc(Supplier<T> work) {
        Future<T> future = startAdHoc(work);
        try {
            return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new GenerationFailure("MODEL_TIMEOUT", "The model did not answer in time.", e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof RuntimeException failure) {
                throw failure;
            }
            throw new IllegalStateException(e.getCause());
        } catch (InterruptedException e) {
            future.cancel(true);
            Thread.currentThread().interrupt();
            throw new GenerationFailure("MODEL_UNAVAILABLE", "The request was interrupted.", e);
        }
    }

    /** Any member of the file's course may ask for a summary. */
    @Transactional
    public Summary requestSummary(User user, Long fileId) {
        CourseFile file = fileService.get(user, fileId);
        Role role = membership.roleOf(user, file.getCourse());
        Summary summary = new Summary();
        begin(summary, file, user, role);
        Summary saved = summaries.save(summary);
        afterCommit(() -> run(saved.getId(), summaries, this::summarise));
        return saved;
    }

    /** Only the Teacher of the file's course may ask for an exercise set. */
    @Transactional
    public ExerciseSet requestExerciseSet(User user, Long fileId) {
        CourseFile file = fileService.get(user, fileId);
        membership.requireTeacher(user, file.getCourse());
        ExerciseSet set = new ExerciseSet();
        begin(set, file, user, Role.TEACHER);
        ExerciseSet saved = exerciseSets.save(set);
        afterCommit(() -> run(saved.getId(), exerciseSets, this::exercise));
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Summary> summariesOf(User user, Long fileId) {
        CourseFile file = fileService.get(user, fileId);
        return summaries.visibleTo(file.getId(), user.getId());
    }

    @Transactional(readOnly = true)
    public List<ExerciseSet> exerciseSetsOf(User user, Long fileId) {
        CourseFile file = fileService.get(user, fileId);
        return exerciseSets.visibleTo(file.getId(), user.getId());
    }

    public List<Exercise> exercisesOf(ExerciseSet set) {
        if (set.getExercisesJson() == null) {
            return List.of();
        }
        try {
            return mapper.readValue(set.getExercisesJson(), EXERCISE_LIST);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void begin(Generated item, CourseFile file, User user, Role role) {
        item.setFile(file);
        item.setAuthor(user);
        item.setPublished(role == Role.TEACHER);
    }

    private void afterCommit(Runnable job) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                executor.execute(job);
            }
        });
    }

    private <T extends Generated> void run(Long id, GeneratedRepository<T> repository, Consumer<T> work) {
        T item = repository.findById(id).orElse(null);
        if (item == null) {
            return;
        }
        try {
            work.accept(item);
            item.setModel(textGeneration.modelName());
            item.setStatus(Status.DONE);
        } catch (RuntimeException e) {
            fail(item, e);
        }
        repository.save(item);
    }

    private void summarise(Summary summary) {
        summary.setContent(textGeneration.summarize(textOf(summary.getFile())));
    }

    private void exercise(ExerciseSet set) {
        List<Exercise> exercises = textGeneration.generateExercises(textOf(set.getFile()), EXERCISES_PER_SET);
        try {
            set.setExercisesJson(mapper.writeValueAsString(exercises));
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String textOf(CourseFile file) {
        return pdfText.extract(materials.open(file));
    }

    private void fail(Generated item, RuntimeException e) {
        item.setStatus(Status.FAILED);
        item.setModel(textGeneration.modelName());
        if (e instanceof GenerationFailure failure) {
            item.setFailureCode(failure.code());
            item.setFailureMessage(failure.getMessage());
        } else if (e instanceof StoredFileMissingException) {
            item.setFailureCode("FILE_MISSING");
            item.setFailureMessage("The stored PDF could not be found.");
        } else {
            log.error("Generation {} failed", item.getId(), e);
            item.setFailureCode("GENERATION_FAILED");
            item.setFailureMessage("The model could not produce a result.");
        }
    }
}
