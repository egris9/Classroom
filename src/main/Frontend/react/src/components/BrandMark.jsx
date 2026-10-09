import { BookOpen } from "lucide-react";
import { cn } from "@/lib/utils";

/** The ClassHub mark: an open book on the action colour. Drawn in code, so it needs no image file. */
export function BrandMark({ className }) {
    return (
        <span
            aria-hidden="true"
            className={cn(
                "grid h-8 w-8 shrink-0 place-items-center rounded-lg bg-[image:var(--gradient-primary)] text-primary-foreground shadow",
                className,
            )}
        >
            <BookOpen className="h-4 w-4" strokeWidth={2.25} />
        </span>
    );
}

export default BrandMark;
