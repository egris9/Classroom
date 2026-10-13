import { Check, Lightbulb, X } from "lucide-react";
import { cn } from "@/lib/utils";

// The mark is an icon and a word, never a colour alone.
const TONES = {
    success: { Icon: Check, box: "border-success bg-success/10", title: "text-success" },
    destructive: { Icon: X, box: "border-destructive bg-destructive/10", title: "text-destructive" },
    neutral: { Icon: Lightbulb, box: "bg-muted/60", title: "text-foreground" },
};

/**
 * What a Student sees under a checked question: a `title` ("Correct", "Not quite", "Model answer"), the `answer`
 * text, the `explanation` when there is one, and `children` for anything below (the self-marking buttons).
 */
export function AnswerCard({ title, tone = "neutral", answer, explanation, children }) {
    const { Icon, box, title: titleClass } = TONES[tone];
    return (
        <div className={cn("answer-reveal space-y-2 rounded-lg border p-3", box)}>
            <p className={cn("flex items-center gap-2 text-sm font-semibold", titleClass)}>
                <Icon aria-hidden="true" className="size-4 shrink-0" />
                {title}
            </p>
            {answer && (
                <p dir="auto" className="break-words text-sm leading-reading">
                    {answer}
                </p>
            )}
            {explanation && (
                <p dir="auto" className="break-words text-sm leading-reading text-muted-foreground">
                    {explanation}
                </p>
            )}
            {children}
        </div>
    );
}

export default AnswerCard;
