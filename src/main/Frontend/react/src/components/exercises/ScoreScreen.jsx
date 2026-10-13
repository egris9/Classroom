import { useEffect, useState } from "react";
import { Check, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Progress } from "@/components/ui/progress";
import { scoreOf } from "@/lib/exercises";

const COUNT_UP_MS = 800;

const prefersReducedMotion = () =>
    typeof window !== "undefined" && window.matchMedia?.("(prefers-reduced-motion: reduce)").matches === true;

/** 0 up to `target` over 800ms, or `target` at once when the person asked for less motion (CSS cannot do this one). */
function useCountUp(target) {
    const [value, setValue] = useState(() => (prefersReducedMotion() ? target : 0));
    useEffect(() => {
        if (prefersReducedMotion()) {
            setValue(target);
            return undefined;
        }
        const start = performance.now();
        let frame = requestAnimationFrame(function tick(now) {
            const progress = Math.min(1, (now - start) / COUNT_UP_MS);
            setValue(target * (1 - (1 - progress) ** 3));
            if (progress < 1) {
                frame = requestAnimationFrame(tick);
            }
        });
        return () => cancelAnimationFrame(frame);
    }, [target]);
    return value;
}

/**
 * "7 of 10", one line per question that jumps back to that step for review, and Try again. `answers` is the
 * player's array of `{ given, correct }`, with a hole for a question that was never checked.
 */
export function ScoreScreen({ exercises, answers, headingRef, onReview, onRetry }) {
    const { correct, total } = scoreOf(exercises, answers);
    const shown = useCountUp(correct);
    const percent = total === 0 ? 0 : (shown / total) * 100;

    return (
        <div className="space-y-5">
            <div className="space-y-3">
                <h3
                    ref={headingRef}
                    tabIndex={-1}
                    className="rounded font-display text-heading font-semibold outline-none focus-visible:ring-2 focus-visible:ring-ring"
                >
                    Your score
                </h3>
                {/* The number counts up for the eye; a screen reader gets the final one at once. */}
                <p className="sr-only">{`${correct} of ${total}`}</p>
                <p aria-hidden="true" className="font-display text-title font-semibold tabular-nums">
                    {Math.round(shown)} <span className="text-heading font-normal text-muted-foreground">of {total}</span>
                </p>
                <Progress aria-hidden="true" value={percent} className="h-2" />
            </div>

            <ol className="space-y-1">
                {exercises.map((exercise, index) => {
                    const right = answers[index]?.correct === true;
                    const Icon = right ? Check : X;
                    return (
                        <li key={index}>
                            <button
                                type="button"
                                onClick={() => onReview(index)}
                                className="flex min-h-11 w-full items-start gap-3 rounded-md px-2 py-2 text-left text-sm transition-colors hover:bg-muted focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                            >
                                <span className="w-6 shrink-0 tabular-nums text-muted-foreground">{index + 1}.</span>
                                <span dir="auto" className="line-clamp-2 min-w-0 flex-1 break-words">
                                    {exercise.question}
                                </span>
                                <span
                                    className={`flex shrink-0 items-center gap-1 font-medium ${right ? "text-success" : "text-destructive"}`}
                                >
                                    <Icon aria-hidden="true" className="size-4" />
                                    {right ? "Right" : "Not yet"}
                                </span>
                            </button>
                        </li>
                    );
                })}
            </ol>

            <Button type="button" variant="outline" className="h-11" onClick={onRetry}>
                Try again
            </Button>
        </div>
    );
}

export default ScoreScreen;
