import { useId, useState } from "react";
import { FileText, ImageIcon, UploadCloud, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { cn } from "@/lib/utils";
import { checkFile, formatBytes } from "../lib/files.js";

/**
 * A dashed card that is a label for a visually hidden file input, so the OS picker, Enter and Space work.
 * Files can also be dropped on it. A wrong type or a file over `maxBytes` shows an error and calls onFile(null).
 */
export default function FileDropzone({ accept, maxBytes, file, onFile, label, hint, id, required = false }) {
    const generatedId = useId();
    const inputId = id ?? generatedId;
    const errorId = `${inputId}-error`;
    const [error, setError] = useState(null);
    const [dragging, setDragging] = useState(false);
    const [inputKey, setInputKey] = useState(0);

    const choose = (picked) => {
        if (!picked) {
            return;
        }
        const problem = checkFile(picked, { accept, maxBytes });
        setError(problem);
        onFile(problem ? null : picked);
        if (problem) {
            setInputKey((key) => key + 1);
        }
    };

    const remove = () => {
        setError(null);
        setInputKey((key) => key + 1);
        onFile(null);
    };

    const onDrop = (event) => {
        event.preventDefault();
        setDragging(false);
        choose(event.dataTransfer.files?.[0]);
    };

    const isImage = file?.type?.startsWith("image/");
    const Icon = isImage ? ImageIcon : FileText;

    return (
        <div className="space-y-2">
            {label && <Label htmlFor={inputId}>{label}</Label>}
            {file ? (
                <Card className="flex items-center gap-3 p-3">
                    <Icon className="h-6 w-6 shrink-0 text-muted-foreground" aria-hidden="true" />
                    <div className="min-w-0 flex-1">
                        <p className="truncate text-sm font-medium">{file.name}</p>
                        <p className="text-xs text-muted-foreground">{formatBytes(file.size)}</p>
                    </div>
                    <Button type="button" variant="ghost" className="h-11 gap-1 px-3" onClick={remove}>
                        <X className="h-4 w-4" aria-hidden="true" />
                        Remove
                    </Button>
                </Card>
            ) : (
                <label
                    htmlFor={inputId}
                    onDragOver={(event) => {
                        event.preventDefault();
                        setDragging(true);
                    }}
                    onDragLeave={() => setDragging(false)}
                    onDrop={onDrop}
                    className={cn(
                        "flex min-h-28 cursor-pointer flex-col items-center justify-center gap-1 rounded-lg border-2 border-dashed border-input bg-muted/40 px-4 py-6 text-center transition-colors hover:bg-muted focus-within:ring-2 focus-within:ring-ring focus-within:ring-offset-2 focus-within:ring-offset-background",
                        dragging && "border-primary bg-muted",
                        error && "border-destructive"
                    )}
                >
                    <UploadCloud className="h-6 w-6 text-muted-foreground" aria-hidden="true" />
                    <span className="text-sm font-medium">Drop a file here or choose one</span>
                    {hint && <span className="text-xs text-muted-foreground">{hint}</span>}
                    <input
                        key={inputKey}
                        id={inputId}
                        type="file"
                        accept={accept}
                        required={required}
                        aria-describedby={error ? errorId : undefined}
                        className="sr-only"
                        onChange={(event) => choose(event.target.files?.[0])}
                    />
                </label>
            )}
            {error && (
                <p id={errorId} role="alert" className="text-sm text-destructive">
                    {error}
                </p>
            )}
        </div>
    );
}
