package com.Classroom_ai.Classroom.generation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

/**
 * A real HTTP server that speaks just enough of {@code /v1/chat/completions} for the adapter tests. Each request is
 * answered by the {@code reply} function, which gets the user message and returns the assistant's content.
 */
final class StubModelServer implements AutoCloseable {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpServer server;
    private final List<JsonNode> requests = new CopyOnWriteArrayList<>();
    private final ConcurrentLinkedQueue<Function<String, String>> script = new ConcurrentLinkedQueue<>();
    private volatile Function<String, String> reply = userMessage -> "ok";
    private volatile int status = 200;
    private volatile long delayMillis = 0;

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
