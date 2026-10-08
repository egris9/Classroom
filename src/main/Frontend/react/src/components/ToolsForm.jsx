import { useId, useState } from "react";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Skeleton } from "@/components/ui/skeleton";
import { Textarea } from "@/components/ui/textarea";
import FileDropzone from "@/components/FileDropzone";
import { formatBytes } from "../lib/files.js";
import { nextStep } from "../lib/toolErrors.js";

const count = (n) => n.toLocaleString("en-US");

/**
 * Pasted text or a PDF, one button, and the result under it. Shared by the Summary and Exercises tabs.
 * `fromText(text)` and `fromPdf(file)` are the API calls; `attempt` runs one for the visitor and resolves to
 * `{ gated: true }` when the sign-up gate was shown instead; `limits` is `{ chars, pdfBytes }`.
 */
export function ToolsForm({ action, busyLabel, fromText, fromPdf, attempt, limits, children }) {
    const textId = useId();
    const [text, setText] = useState("");
    const [file, setFile] = useState(null);
    const [busy, setBusy] = useState(false);
    const [result, setResult] = useState(null);
    const [error, setError] = useState(null);

    const tooLong = !file && text.length > limits.chars;
    const empty = !file && text.trim() === "";

    const submit = async (event) => {
        event.preventDefault();
        setBusy(true);
        setResult(null);
        setError(null);
        try {
            const outcome = await attempt(() => (file ? fromPdf(file) : fromText(text)));
            if (!outcome.gated) {
                setResult(outcome.value);
            }
        } catch (err) {
            setError(`${err.message} (${err.code}) ${nextStep(err.code, err.spentTrial)}`);
        } finally {
            setBusy(false);
        }
    };

    return (
        <div className="space-y-6">
            <form onSubmit={submit} className="space-y-4">
                <div className="space-y-2">
                    <Label htmlFor={textId}>Paste text</Label>
                    <Textarea
                        id={textId}
                        dir="auto"
                        className="min-h-40"
                        placeholder="Paste a chapter, your notes or an article."
                        value={text}
                        onChange={(event) => setText(event.target.value)}
                        disabled={busy || Boolean(file)}
                        aria-describedby={`${textId}-count`}
                        aria-invalid={tooLong || undefined}
                    />
                    <p
                        id={`${textId}-count`}
                        className={tooLong ? "text-sm text-destructive" : "text-sm text-muted-foreground"}
                    >
                        {file
                            ? "The PDF will be used. Remove it to use pasted text instead."
                            : `${count(text.length)} of ${count(limits.chars)} characters${tooLong ? ". Shorten the text to continue." : ""}`}
                    </p>
                </div>

                <p className="flex items-center gap-3 text-sm text-muted-foreground before:h-px before:flex-1 before:bg-border after:h-px after:flex-1 after:bg-border">
                    or
                </p>

                <FileDropzone
                    label="Choose a PDF"
                    accept="application/pdf"
                    maxBytes={limits.pdfBytes}
                    hint={`PDF, up to ${formatBytes(limits.pdfBytes)}, with at most ${count(limits.chars)} characters of text`}
                    file={file}
                    onFile={setFile}
                />

                <Button type="submit" className="h-11 w-full px-6 sm:w-auto" disabled={busy || empty || tooLong}>
                    {busy ? busyLabel : action}
                </Button>
            </form>

            <div aria-live="polite" aria-busy={busy} className="space-y-4">
                {busy && (
                    <div className="space-y-2">
                        <p className="text-sm text-muted-foreground">
                            The model is working. This can take up to two minutes.
                        </p>
                        <Skeleton className="h-40 w-full" />
                    </div>
                )}
                {!busy && error && (
                    <Alert variant="destructive">
                        <AlertDescription>{error}</AlertDescription>
                    </Alert>
                )}
                {!busy && result && children(result)}
            </div>
        </div>
    );
}

export default ToolsForm;
