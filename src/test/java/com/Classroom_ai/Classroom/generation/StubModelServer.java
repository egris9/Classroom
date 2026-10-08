package com.Classroom_ai.Classroom.generation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A real HTTP server that speaks just enough of {@code /v1/chat/completions} for the adapter tests. Each request is
 * answered by the {@code reply} function, which gets the user message and returns the assistant's content. A request
 * with {@code "stream": true} is answered as server-sent events instead: the pieces given to {@link #streamsChunks},
 * or else the reply cut into words.
 */
final class StubModelServer implements AutoCloseable {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpServer server;
    private final List<JsonNode> requests = new CopyOnWriteArrayList<>();
    private final ConcurrentLinkedQueue<Function<String, String>> script = new ConcurrentLinkedQueue<>();
    private volatile Function<String, String> reply = userMessage -> "ok";
    private volatile int status = 200;
    private volatile long delayMillis = 0;
    private volatile List<String> streamPayloads = null;
    private volatile boolean streamFinishes = true;
    private volatile long stallMillis = 0;

    StubModelServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            JsonNode request = MAPPER.readTree(exchange.getRequestBody());
            requests.add(request);
            if (delayMillis > 0) {
                try {
                    Thread.sleep(delayMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            String userMessage = lastUserMessage(request);
            Function<String, String> next = script.poll();
            String content = (next != null ? next : reply).apply(userMessage);
            if (status == 200 && request.path("stream").asBoolean(false)) {
                stream(exchange, content);
                return;
            }
            byte[] body = MAPPER.writeValueAsBytes(Map.of("choices",
                    List.of(Map.of("index", 0, "finish_reason", "stop",
                            "message", Map.of("role", "assistant", "content", content)))));
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, status == 200 ? body.length : -1);
            if (status == 200) {
                exchange.getResponseBody().write(body);
            }
            exchange.close();
        });
        server.start();
    }

    /** What llama.cpp sends: a role chunk with null content, the pieces, a finish chunk, then {@code [DONE]}. */
    private void stream(HttpExchange exchange, String content) throws IOException {
        List<String> payloads = streamPayloads != null ? streamPayloads : wordChunks(content);
        exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
        exchange.sendResponseHeaders(200, 0);
        OutputStream out = exchange.getResponseBody();
        sendData(out, delta("{\"role\":\"assistant\",\"content\":null}", "null"));
        for (String payload : payloads) {
            sendData(out, payload);
        }
        if (streamFinishes) {
            sendData(out, delta("{}", "\"stop\""));
            sendData(out, "[DONE]");
        } else if (stallMillis > 0) {
            try {
                Thread.sleep(stallMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        exchange.close();
    }

    private static void sendData(OutputStream out, String payload) throws IOException {
        try {
            out.write(utf8("data: " + payload + "\n\n"));
            out.flush();
        } catch (IOException clientGone) {
            // the adapter hung up on purpose
        }
    }

    private static String delta(String delta, String finishReason) {
        return "{\"choices\":[{\"index\":0,\"delta\":" + delta + ",\"finish_reason\":" + finishReason + "}]}";
    }

    private static List<String> wordChunks(String content) {
        List<String> payloads = new ArrayList<>();
        Matcher word = Pattern.compile("\\S+\\s*").matcher(content);
        while (word.find()) {
            payloads.add(contentChunk(word.group()));
        }
        return payloads;
    }

    private static String contentChunk(String content) {
        try {
            return delta("{\"content\":" + MAPPER.writeValueAsString(content) + "}", "null");
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    /** A streamed reply is exactly these content pieces, in this order. */
    StubModelServer streamsChunks(String... contents) {
        this.streamPayloads = Arrays.stream(contents).map(StubModelServer::contentChunk).toList();
        return this;
    }

    /** A streamed reply is exactly these raw {@code data:} payloads, for chunks that are not plain content. */
    StubModelServer streamsData(String... payloads) {
        this.streamPayloads = List.of(payloads);
        return this;
    }

    /**
     * The stream sends its pieces and then neither finishes nor says {@code [DONE]}: it is held open for
     * {@code stallMillis} (a model that stopped answering) and then closed (a model that went away).
     */
    StubModelServer cutsStreamShort(long stallMillis) {
        this.streamFinishes = false;
        this.stallMillis = stallMillis;
        return this;
    }

    String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    /** Every request the server has received, oldest first. */
    List<JsonNode> requests() {
        return requests;
    }

    /** The user message of the nth request. */
    String userMessage(int n) {
        return lastUserMessage(requests.get(n));
    }

    StubModelServer replyWith(Function<String, String> reply) {
        this.reply = reply;
        return this;
    }

    /** The next request is answered by this function, then the server falls back to {@link #replyWith}. */
    StubModelServer thenReply(String content) {
        script.add(userMessage -> content);
        return this;
    }

    StubModelServer failWith(int status) {
        this.status = status;
        return this;
    }

    StubModelServer delayBy(long millis) {
        this.delayMillis = millis;
        return this;
    }

    private static String lastUserMessage(JsonNode request) {
        String last = "";
        for (JsonNode message : request.path("messages")) {
            if ("user".equals(message.path("role").asText())) {
                last = message.path("content").asText();
            }
        }
        return last;
    }

    @Override
    public void close() {
        server.stop(0);
    }

    static byte[] utf8(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }
}
