package com.Classroom_ai.Classroom.tools;

import com.Classroom_ai.Classroom.api.ChatRequest;
import com.Classroom_ai.Classroom.auth.UserService;
import com.Classroom_ai.Classroom.generation.ChatMessage;
import com.Classroom_ai.Classroom.generation.GenerationFailure;
import com.Classroom_ai.Classroom.generation.GenerationService;
import com.Classroom_ai.Classroom.generation.TextGeneration;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A general chat with the study assistant, streamed as server-sent events: {@code delta} ({@code {"text":"..."}}) for
 * each piece of the reply, then {@code done}, or {@code error} ({@code {"code","message"}}) if the reply cannot be
 * finished. The history is checked, and the visitor admitted ({@link TrialGate}), before the stream opens, so a
 * refusal is an ordinary 400 or 403 JSON answer. The model call waits its turn on the one generation thread and is
 * cancelled if the reader goes away.
 */
@RestController
@RequestMapping("/api/tools")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final TrialGate gate;
    private final UserService users;
    private final GenerationService generation;
    private final TextGeneration textGeneration;
    private final ObjectMapper mapper;
    private final int maxMessages;
    private final int maxMessageChars;
    private final long streamTimeoutMillis;

    public ChatController(TrialGate gate, UserService users, GenerationService generation,
                          TextGeneration textGeneration, ObjectMapper mapper,
                          @Value("${tools.chat.max-messages:20}") int maxMessages,
                          @Value("${tools.chat.max-message-chars:4000}") int maxMessageChars,
                          @Value("${generation.timeout:120s}") Duration timeout) {
        this.gate = gate;
        this.users = users;
        this.generation = generation;
        this.textGeneration = textGeneration;
        this.mapper = mapper;
        this.maxMessages = maxMessages;
        this.maxMessageChars = maxMessageChars;
        this.streamTimeoutMillis = timeout.plusMinutes(1).toMillis();
    }

    @PostMapping(value = "/chat", consumes = MediaType.APPLICATION_JSON_VALUE)
    public SseEmitter chat(@RequestBody ChatRequest body, HttpServletRequest request, HttpServletResponse response) {
        List<ChatMessage> history = validated(body);
        gate.admit(request, response, users.findAuthenticatedUser().orElse(null));

        SseEmitter emitter = new SseEmitter(streamTimeoutMillis);
        AtomicReference<Future<?>> reply = new AtomicReference<>();
        Runnable stopTheModel = () -> {
            Future<?> started = reply.get();
            if (started != null) {
                started.cancel(true);
            }
        };
        emitter.onCompletion(stopTheModel);
        emitter.onError(failure -> stopTheModel.run());
        emitter.onTimeout(() -> {
            stopTheModel.run();
            fail(emitter, "MODEL_TIMEOUT", "The reply took too long.");
        });
        // Opens the stream here, on the request's own thread: its headers are written before the model thread can
        // write to the same response, however soon the first piece of the reply comes. Readers skip a comment.
        send(emitter, SseEmitter.event().comment("chat"));
        reply.set(generation.startAdHoc(() -> {
            stream(history, emitter);
            return null;
        }));
        return emitter;
    }

    /** Only the user and the assistant may speak, within the limits, and the last word must be the user's. */
    private List<ChatMessage> validated(ChatRequest body) {
        List<ChatRequest.Turn> turns = body.messages();
        if (turns == null || turns.isEmpty()) {
            throw new BadChatRequestException("Send at least one message.");
        }
        if (turns.size() > maxMessages) {
            throw new BadChatRequestException("A chat can hold at most " + maxMessages + " messages.");
        }
        List<ChatMessage> history = new ArrayList<>();
        for (ChatRequest.Turn turn : turns) {
            if (turn == null || turn.role() == null || turn.content() == null) {
                throw new BadChatRequestException("Every message needs a role and some content.");
            }
            if (!turn.role().equals("user") && !turn.role().equals("assistant")) {
                throw new BadChatRequestException("A message is from the user or the assistant.");
            }
            if (turn.content().isBlank()) {
                throw new BadChatRequestException("A message cannot be empty.");
            }
            if (turn.content().length() > maxMessageChars) {
                throw new BadChatRequestException("A message can have at most " + maxMessageChars + " characters.");
            }
            history.add(new ChatMessage(turn.role(), turn.content()));
        }
        if (!history.get(history.size() - 1).role().equals("user")) {
            throw new BadChatRequestException("The last message must be from the user.");
        }
        return history;
    }

    /** Runs on the generation thread. */
    private void stream(List<ChatMessage> history, SseEmitter emitter) {
        try {
            textGeneration.chat(history, delta -> send(emitter, "delta", Map.of("text", delta)));
            send(emitter, "done", Map.of());
            emitter.complete();
        } catch (ReaderGone gone) {
            log.debug("The reader of a chat left before the reply was complete");
        } catch (GenerationFailure failure) {
            fail(emitter, failure.code(), failure.getMessage());
        } catch (RuntimeException unexpected) {
            log.error("A chat reply failed", unexpected);
            fail(emitter, "GENERATION_FAILED", "The model could not produce a reply.");
        }
    }

    /** The last event of a stream that did not finish. If the reader is already gone there is no one to tell. */
    private void fail(SseEmitter emitter, String code, String message) {
        try {
            send(emitter, "error", Map.of("code", code, "message", message));
            emitter.complete();
        } catch (ReaderGone gone) {
            log.debug("The reader of a chat left before its error was sent");
        }
    }

    private void send(SseEmitter emitter, String event, Map<String, String> data) {
        String json;
        try {
            json = mapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
        send(emitter, SseEmitter.event().name(event).data(json));
    }

    private void send(SseEmitter emitter, SseEmitter.SseEventBuilder event) {
        try {
            emitter.send(event);
        } catch (IOException | IllegalStateException e) {
            throw new ReaderGone(e);
        }
    }

    /** The connection to the reader is closed: thrown out of the model call to stop it. */
    private static final class ReaderGone extends RuntimeException {
        ReaderGone(Throwable cause) {
            super(cause);
        }
    }
}
