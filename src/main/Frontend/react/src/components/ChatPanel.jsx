import { useEffect, useId, useRef, useState } from "react";
import { Send, Square, Trash2 } from "lucide-react";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Textarea } from "@/components/ui/textarea";
import { Markdown } from "@/components/Markdown";
import { cn } from "@/lib/utils";
import { streamChat } from "../api/stream.js";
import { nextStep } from "../lib/toolErrors.js";

const STORAGE_KEY = "tools_chat";
// The backend's limits for one request (tools.chat.max-messages, tools.chat.max-message-chars).
const MAX_MESSAGES = 20;
const MAX_MESSAGE_CHARS = 4000;

// Starting points for an empty chat. A click fills the composer; the reader still decides to send.
const SUGGESTIONS = [
    "Explain photosynthesis in simple words",
    "Give me three tips for remembering formulas",
    "What is the difference between a theory and a law?",
];

const isMessage =(item) =>
    (item?.role === "user" || item?.role === "assistant") && typeof item.content === "string";

const loadMessages = () => {
    try {
        const stored = JSON.parse(sessionStorage.getItem(STORAGE_KEY));
        return Array.isArray(stored) ? stored.filter(isMessage) : [];
    } catch {
        return [];
    }
};

// The request carries the end of the conversation, each message cut to what the backend accepts.
const forRequest = (history) =>
    history
        .filter((message) => message.content.trim() !== "")
        .slice(-MAX_MESSAGES)
        .map(({ role, content }) => ({ role, content: content.slice(0, MAX_MESSAGE_CHARS) }));

function Bubble({ role, children }) {
    const mine = role === "user";
    return (
        <li className={cn("flex", mine ? "justify-end" : "justify-start")}>
            <div
                className={cn(
                    "max-w-[85%] break-words rounded-lg px-3 py-2 text-sm",
                    mine ? "bg-primary text-primary-foreground" : "bg-muted text-foreground",
                )}
            >
                <span className="sr-only">{mine ? "You: " : "Assistant: "}</span>
                {children}
            </div>
        </li>
    );
}

/**
 * A chat with the study assistant. The reply is shown as the model writes it. The conversation lives in this
 * tab's sessionStorage only. `attempt` runs one call for the visitor and resolves to `{ gated: true }` when the
 * sign-up gate was shown instead. `active` says the panel is the visible tab, so the list can scroll to its end.
 */
export function ChatPanel({ attempt, active = true }) {
    const draftId = useId();
    const [messages, setMessages] = useState(loadMessages);
    const [draft, setDraft] = useState("");
    const [reply, setReply] = useState("");
    const [streaming, setStreaming] = useState(false);
    const [error, setError] = useState(null);
    const listRef = useRef(null);
    const abortRef = useRef(null);

    useEffect(() => {
        try {
            sessionStorage.setItem(STORAGE_KEY, JSON.stringify(messages));
        } catch {
            // Storage is full or blocked: the chat still works, it is only not kept across a reload.
        }
    }, [messages]);

    // Leaving the page stops the reply.
    useEffect(() => () => abortRef.current?.abort(), []);

    useEffect(() => {
        const viewport = listRef.current?.querySelector("[data-radix-scroll-area-viewport]");
        if (viewport) {
            viewport.scrollTop = viewport.scrollHeight;
        }
    }, [messages, reply, error, active]);

    /** Sends `history`, whose last message is the user's, and adds the reply to it. */
    const send = async (history) => {
        const controller = new AbortController();
        abortRef.current = controller;
        setError(null);
        setReply("");
        setStreaming(true);
        let written = "";
        try {
            const outcome = await attempt(() =>
                streamChat(forRequest(history), {
                    signal: controller.signal,
                    onDelta: (piece) => {
                        written += piece;
                        setReply(written);
                    },
                }),
            );
            if (outcome.gated) {
                // Nothing was sent: give the message back to the composer.
                setMessages(history.slice(0, -1));
                setDraft(history[history.length - 1].content);
                return;
            }
            setMessages([...history, { role: "assistant", content: written }]);
        } catch (err) {
            if (err.name === "AbortError") {
                // Stopped by the reader: what was written so far stays.
                if (written) {
                    setMessages([...history, { role: "assistant", content: written }]);
                }
            } else {
                setError({ code: err.code ?? "REQUEST_FAILED", message: err.message, spentTrial: err.spentTrial });
            }
        } finally {
            abortRef.current = null;
            setStreaming(false);
            setReply("");
        }
    };

    const submit = (event) => {
        event?.preventDefault();
        const content = draft.trim();
        if (!content || streaming) {
            return;
        }
        const history = [...messages, { role: "user", content }];
        setMessages(history);
        setDraft("");
        send(history);
    };

    const onKeyDown = (event) => {
        if (event.key === "Enter" && !event.shiftKey && !event.nativeEvent.isComposing) {
            event.preventDefault();
            submit();
        }
    };

    const clear = () => {
        setMessages([]);
        setError(null);
    };

    const awaitingReply = messages.length > 0 && messages[messages.length - 1].role === "user";

    return (
        <div className="space-y-4">
            <Card>
                <ScrollArea ref={listRef} className="h-[min(60vh,28rem)]">
                    <ul aria-live="polite" aria-busy={streaming} aria-label="Conversation" className="space-y-3 p-4">
                        {messages.length === 0 && !streaming && (
                            <li className="space-y-4 py-6 text-center">
                                <p className="text-sm text-muted-foreground">
                                    Ask a question about what you are studying. The assistant answers briefly and says so
                                    when it does not know.
                                </p>
                                <div className="flex flex-wrap justify-center gap-2">
                                    {SUGGESTIONS.map((suggestion) => (
                                        <Button
                                            key={suggestion}
                                            type="button"
                                            variant="outline"
                                            className="h-auto min-h-11 whitespace-normal px-4 py-2"
                                            onClick={() => setDraft(suggestion)}
                                        >
                                            {suggestion}
                                        </Button>
                                    ))}
                                </div>
                            </li>
                        )}
                        {messages.map((message, index) => (
                            <Bubble key={index} role={message.role}>
                                {message.role === "user" ? (
                                    <p dir="auto" className="whitespace-pre-wrap">
                                        {message.content}
                                    </p>
                                ) : (
                                    <Markdown>{message.content}</Markdown>
                                )}
                            </Bubble>
                        ))}
                        {streaming && (
                            <Bubble role="assistant">
                                {reply ? <Markdown>{reply}</Markdown> : <p className="text-muted-foreground">Writing...</p>}
                            </Bubble>
                        )}
                    </ul>
                </ScrollArea>
            </Card>

            {error && (
                <Alert variant="destructive">
                    <AlertDescription className="flex flex-wrap items-center justify-between gap-2">
                        <span>
                            {error.message} ({error.code}) {nextStep(error.code, error.spentTrial)}
                        </span>
                        {awaitingReply && !error.spentTrial && (
                            <Button variant="outline" className="h-11 px-4" onClick={() => send(messages)}>
                                Try again
                            </Button>
                        )}
                    </AlertDescription>
                </Alert>
            )}

            <form onSubmit={submit} className="space-y-2">
                <Label htmlFor={draftId}>Your message</Label>
                <Textarea
                    id={draftId}
                    dir="auto"
                    rows={2}
                    maxLength={MAX_MESSAGE_CHARS}
                    placeholder="Ask a question"
                    value={draft}
                    onChange={(event) => setDraft(event.target.value)}
                    onKeyDown={onKeyDown}
                    aria-describedby={`${draftId}-hint`}
                />
                <div className="flex flex-wrap items-center justify-between gap-2">
                    <p id={`${draftId}-hint`} className="text-sm text-muted-foreground">
                        Enter sends. Shift and Enter starts a new line.
                    </p>
                    <div className="flex flex-wrap gap-2">
                        <Button
                            type="button"
                            variant="ghost"
                            className="h-11 gap-2 px-4"
                            disabled={streaming || messages.length === 0}
                            onClick={clear}
                        >
                            <Trash2 className="h-4 w-4" aria-hidden="true" />
                            Clear chat
                        </Button>
                        {streaming ? (
                            <Button
                                type="button"
                                variant="outline"
                                className="h-11 gap-2 px-4"
                                onClick={() => abortRef.current?.abort()}
                            >
                                <Square className="h-4 w-4" aria-hidden="true" />
                                Stop
                            </Button>
                        ) : (
                            <Button type="submit" className="h-11 gap-2 px-4" disabled={draft.trim() === ""}>
                                <Send className="h-4 w-4" aria-hidden="true" />
                                Send
                            </Button>
                        )}
                    </div>
                </div>
            </form>
        </div>
    );
}

export default ChatPanel;
