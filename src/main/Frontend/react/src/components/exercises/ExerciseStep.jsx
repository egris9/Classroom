import { Check, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import { Textarea } from "@/components/ui/textarea";
import { AnswerCard } from "@/components/exercises/AnswerCard";
import { isCorrect, typeOf } from "@/lib/exercises";
import { cn } from "@/lib/utils";

const TRUE_FALSE_OPTIONS = [
    { value: "true", label: "True" },
    { value: "false", label: "False" },
];

/** The answer of an exercise as a person reads it ("True" for "true"). */
const answerText = (exercise) =>
    typeOf(exercise) === "TRUE_FALSE"
        ? (TRUE_FALSE_OPTIONS.find((option) => option.value === String(exercise.answer).toLowerCase())?.label ?? exercise.answer)
        : exercise.answer;

/** How an option looks once the step is checked: the mark says it in words, the border in colour. */
function markOf(isChosen, isRight) {
    if (isRight) {
        return isChosen
            ? { Icon: Check, word: "Correct", box: "border-success bg-success/10", text: "text-success" }
            : { Icon: Check, word: "Right answer", box: "border-success bg-success/10", text: "text-success" };
    }
    return isChosen
        ? { Icon: X, word: "Not this one", box: "border-destructive bg-destructive/10", text: "text-destructive" }
        : null;
}

function OptionCard({ id, value, label, isChosen, checked, isRight }) {
    const mark = checked ? markOf(isChosen, isRight) : null;
    return (
        <Label
            htmlFor={id}
            className={cn(
                "flex min-h-11 cursor-pointer items-start gap-3 rounded-md border p-3 transition-colors duration-150",
                "has-[:focus-visible]:ring-2 has-[:focus-visible]:ring-ring",
                mark ? mark.box : isChosen ? "border-primary bg-accent" : "hover:bg-muted",
                checked && "cursor-default",
            )}
        >
            <RadioGroupItem id={id} value={value} className="mt-0.5 shrink-0" />
            <span className="min-w-0 flex-1 space-y-1">
                <span dir="auto" className="block break-words text-sm font-normal leading-reading">
                    {label}
                </span>
                {mark && (
                    <span className={cn("mark-in flex items-center gap-1 text-xs font-semibold", mark.text)}>
                        <mark.Icon aria-hidden="true" className="size-3.5" />
                        {mark.word}
                    </span>
                )}
            </span>
        </Label>
    );
}

/**
 * One question of the player: its text, the control that fits its type, and, once it is checked, the answer card.
 * `draft` is what is chosen or typed so far; `answer` is `{ given, correct }` once the step is checked; `revealed`
 * is true once an open question's model answer is showing; `onMark(correct)` is the Student's own verdict on it.
 */
export function ExerciseStep({ exercise, number, headingRef, draft, onDraft, answer, revealed, onMark, idPrefix }) {
    const type = typeOf(exercise);
    const checked = answer !== undefined;
    const options =
        type === "TRUE_FALSE"
            ? TRUE_FALSE_OPTIONS
            : (exercise.options ?? []).map((option) => ({ value: option, label: option }));

    let card = null;
    if (type === "OPEN") {
        if (revealed || checked) {
            card = (
                <AnswerCard title="Model answer" answer={exercise.answer} explanation={exercise.explanation}>
                    {checked ? (
                        <p className="text-sm font-medium">{answer.correct ? "You counted this as right." : "You counted this as not yet."}</p>
                    ) : (
                        <div className="flex flex-wrap gap-2">
                            <Button type="button" variant="outline" className="h-11" autoFocus onClick={() => onMark(true)}>
                                I had it
                            </Button>
                            <Button type="button" variant="outline" className="h-11" onClick={() => onMark(false)}>
                                Not yet
                            </Button>
                        </div>
                    )}
                </AnswerCard>
            );
        }
    } else if (checked) {
        const right = answer.correct;
        card = (
            <AnswerCard
                title={right ? "Correct" : "Not quite"}
                tone={right ? "success" : "destructive"}
                answer={`The answer is: ${answerText(exercise)}`}
                explanation={exercise.explanation}
            />
        );
    }

    return (
        <div className="space-y-4">
            <h3
                ref={headingRef}
                tabIndex={-1}
                dir="auto"
                className="break-words rounded font-display text-heading font-semibold outline-none focus-visible:ring-2 focus-visible:ring-ring"
            >
                {exercise.question}
            </h3>

            {type === "OPEN" ? (
                <div className="grid gap-1.5">
                    <Label htmlFor={`${idPrefix}-open`} className="sr-only">
                        Your answer to question {number}
                    </Label>
                    <Textarea
                        id={`${idPrefix}-open`}
                        dir="auto"
                        rows={4}
                        placeholder="Write your answer, then check it against the model answer."
                        value={checked ? answer.given : (draft ?? "")}
                        readOnly={checked || revealed}
                        onChange={(event) => onDraft(event.target.value)}
                    />
                </div>
            ) : (
                <RadioGroup
                    aria-label={`Answer to question ${number}`}
                    aria-readonly={checked || undefined}
                    value={checked ? answer.given : (draft ?? "")}
                    // A checked step is read-only: arrow keys still move focus, but the choice cannot change.
                    onValueChange={(value) => !checked && onDraft(value)}
                >
                    {options.map((option, index) => (
                        <OptionCard
                            key={index}
                            id={`${idPrefix}-${index}`}
                            value={option.value}
                            label={option.label}
                            isChosen={(checked ? answer.given : draft) === option.value}
                            checked={checked}
                            isRight={isCorrect(exercise, option.value)}
                        />
                    ))}
                </RadioGroup>
            )}

            {/* Always on the page, so a screen reader announces the card when it appears. */}
            <div aria-live="polite" className="empty:hidden">
                {card}
            </div>
        </div>
    );
}

export default ExerciseStep;
