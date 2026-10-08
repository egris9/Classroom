import { getToken } from "./auth.js";
import { API_URL } from "./client.js";
import { toolsError } from "./tools.js";

const parse = (data) => {
    try {
        return JSON.parse(data);
    } catch {
        return {};
    }
};

/**
 * Sends the conversation and calls `onDelta(text)` for each piece of the reply as the model writes it.
 * Resolves when the reply is complete. Rejects with an Error carrying `code` when the request is refused
 * (TRIAL_USED, BAD_REQUEST), when the stream ends with an `error` event, or when it closes before `done`
 * (CONNECTION_LOST). Aborting `signal` rejects with the browser's AbortError.
 */
export async function streamChat(messages, { onDelta, signal } = {}) {
    const headers = { "Content-Type": "application/json", Accept: "text/event-stream" };
    const token = getToken();
    if (token) {
        headers.Authorization = `Bearer ${token}`;
    }

    let response;
    try {
        response = await fetch(`${API_URL}/api/tools/chat`, {
            method: "POST",
            headers,
            credentials: "include",
            body: JSON.stringify({ messages }),
            signal,
        });
    } catch (error) {
        if (error.name === "AbortError") {
            throw error;
        }
        throw toolsError("NETWORK", "Could not reach the server.");
    }
    if (!response.ok || !response.body) {
        const body = await response.json().catch(() => null);
        throw toolsError(body?.code ?? "REQUEST_FAILED", body?.message || "The chat could not start.");
    }

    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    let buffer = "";
    let event = "message";
    let data = [];

    // Handles one complete line. Returns true when the `done` event has been read.
    const take = (line) => {
        if (line === "") {
            const name = event;
            const payload = parse(data.join("\n"));
            event = "message";
            data = [];
            if (name === "delta" && payload.text) {
                onDelta?.(payload.text);
            } else if (name === "error") {
                throw toolsError(payload.code ?? "GENERATION_FAILED", payload.message || "The reply could not be finished.");
            }
            return name === "done";
        }
        if (line.startsWith(":")) {
            return false;
        }
        const colon = line.indexOf(":");
        const field = colon === -1 ? line : line.slice(0, colon);
        const value = colon === -1 ? "" : line.slice(colon + 1).replace(/^ /, "");
        if (field === "event") {
            event = value;
        } else if (field === "data") {
            data.push(value);
        }
        return false;
    };

    try {
        for (;;) {
            const { value, done } = await reader.read();
            if (done) {
                break;
            }
            buffer += decoder.decode(value, { stream: true });
            const lines = buffer.split(/\r?\n/);
            buffer = lines.pop();
            for (const line of lines) {
                if (take(line)) {
                    return;
                }
            }
        }
    } catch (error) {
        if (error.name === "AbortError" || error.code) {
            throw error;
        }
        throw toolsError("CONNECTION_LOST", "The connection was lost before the reply was complete.");
    } finally {
        reader.cancel().catch(() => {});
    }
    throw toolsError("CONNECTION_LOST", "The connection was lost before the reply was complete.");
}
