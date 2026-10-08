import { useState } from "react";
import { ImageIcon } from "lucide-react";
import { cn } from "@/lib/utils";

/**
 * A picture slot with a fixed aspect ratio. It shows the file at `src` (a path under
 * `public/images/`) when that file exists, and a captioned block otherwise. Every use
 * has a row in the Picture brief (docs/IMPLEMENTATION_PLAN.md, U2 Notes).
 * `compact` is for slots too small to print the caption: the caption becomes the label.
 */
export function ImagePlaceholder({ src, caption, alt, aspect = "16 / 9", compact = false, priority = false, className }) {
    const [missing, setMissing] = useState(false);

    return (
        <div
            className={cn("relative w-full overflow-hidden rounded-lg border bg-muted", className)}
            style={{ aspectRatio: aspect }}
        >
            {!missing && (
                <img
                    src={src}
                    alt={alt ?? caption}
                    loading={priority ? "eager" : "lazy"}
                    decoding="async"
                    className="absolute inset-0 h-full w-full object-cover"
                    onError={() => setMissing(true)}
                />
            )}
            {missing && (
                <div
                    role="img"
                    aria-label={alt ?? caption}
                    className="absolute inset-0 flex flex-col items-center justify-center gap-2 border-2 border-dashed border-border p-3 text-center text-muted-foreground"
                >
                    <ImageIcon className={compact ? "h-4 w-4" : "h-8 w-8"} aria-hidden="true" />
                    {!compact && <span className="text-sm">{caption}</span>}
                </div>
            )}
        </div>
    );
}

export default ImagePlaceholder;
