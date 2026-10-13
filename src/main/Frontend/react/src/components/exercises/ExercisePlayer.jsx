import { useEffect, useId, useRef, useState } from "react";
import { Button } from "@/components/ui/button";
import { Progress } from "@/components/ui/progress";
import { ExerciseStep } from "@/components/exercises/ExerciseStep";
import { ScoreScreen } from "@/components/exercises/ScoreScreen";
import { isCorrect, typeOf } from "@/lib/exercises";

/** The first question without an answer, or the number of questions when each one has one. */
const firstOpenStep = (exercises, answers) => {
    const index = exercises.findIndex((_, i) => answers[i] === undefined);
    return index === -1 ? exercises.length : index;
};

/**
 * An Exercise set played one question at a time: choose or write, check, read the answer card, go on, see the score.
 * `marking` is "self" for now: multiple choice and true or false are marked by comparing, and an open answer by the
 * Student ("I had it", "Not yet"). `initialAnswers` is an array of `{ given, correct }` with a hole for a question
 * not answered yet. `onAnswer(index, answer)` fires when a step is checked, `onFinish(answers)` when the score is
 * asked for, and `onRetry()` when the Student starts again. A set with no exercises renders nothing. Key the player by the set, so a new set starts afresh.
 */
export function ExercisePlayer({ exercises, marking = "self", initialAnswers, onAnswer, onFinish, onRetry }) {
    const idPrefix = useId();
    const headingRef = useRef(null);
    const moved = useRef(false);
    const primaryRef = useRef(null);
    const focusPrimary = useRef(false);
    const [answers, setAnswers] = useState(() => exercises.map((_, i) => initialAnswers?.[i]));
    const [drafts, setDrafts] = useState({});
    const [revealed, setRevealed] = useState({});
    const [index, setIndex] = useState(() => firstOpenStep(exercises, answers));
    // Which way the last move went, so the new step comes in from the right side. Null until the first move.
    const [direction, setDirection] = useState(null);

    const total = exercises.length;
    const onScore = index >= total;

    // On a step change focus moves to the question, so a keyboard or screen reader person starts reading there.
    useEffect(() => {
        if (moved.current) {
            headingRef.current?.focus();
        }
    }, [index]);

    // Marking an open answer removes the button that was pressed, so focus goes to the next action instead of the page.
    useEffect(() => {
        if (focusPrimary.current) {
            focusPrimary.current = false;
            primaryRef.current?.focus();
        }
    });

    if (total === 0) {
        return null;
    }

    const go = (next) => {
        moved.current = true;
        setDirection(next > index ? "next" : "back");
        setIndex(next);
    };

    const exercise = exercises[index];
    const type = exercise ? typeOf(exercise) : null;
    const answer = answers[index];
    const checked = answer !== undefined;
    const draft = drafts[index];
    const isRevealed = revealed[index] === true;

    const record = (given, correct) => {
        const entry = { given, correct };
        setAnswers((current) => current.map((existing, i) => (i === index ? entry : existing)));
        onAnswer?.(index, entry);
    };

    const check = () => record(draft, isCorrect(exercise, draft));

    const finish = () => {
        onFinish?.(answers);
        go(total);
    };

    // The one primary action of the step, as the button below names it.
    let primary;
    if (type === "OPEN" && !checked && !isRevealed) {
        primary = { label: "Show answer", disabled: false, run: () => setRevealed((r) => ({ ...r, [index]: true })) };
    } else if (!checked && type === "OPEN") {
        primary = { label: "Next", disabled: true, run: () => {} };
    } else if (!checked) {
        primary = { label: "Check", disabled: !draft, run: check };
    } else if (index === total - 1) {
        primary = { label: "See my score", disabled: false, run: finish };
    } else {
        primary = { label: "Next", disabled: false, run: () => go(index + 1) };
    }

    // Enter does the primary action from a radio card or from the question itself. In a text box it is a new line, so
    // there Ctrl or Cmd with Enter does it; on a button Enter already presses that button.
    const onKeyDown = (event) => {
        if (event.key !== "Enter" || event.shiftKey || primary.disabled) {
            return;
        }
        const target = event.target;
        const fromTextBox = target.tagName === "TEXTAREA";
        if (fromTextBox ? event.ctrlKey || event.metaKey : target.getAttribute("role") === "radio" || target === headingRef.current) {
            event.preventDefault();
            primary.run();
        }
    };

    if (onScore) {
        return (
            <ScoreScreen
                exercises={exercises}
                answers={answers}
                onReview={go}
                headingRef={headingRef}
                onRetry={() => {
                    onRetry?.();
                    setAnswers(exercises.map(() => undefined));
                    setDrafts({});
                    setRevealed({});
                    go(0);
                }}
            />
        );
    }

    return (
        // Enter from the controls inside bubbles up to onKeyDown here.
        <div className="space-y-4" data-marking={marking} onKeyDown={onKeyDown}>
            <div className="space-y-2">
                <p className="text-sm text-muted-foreground">
                    Question {index + 1} of {total}
                </p>
                <Progress
                    aria-label={`Question ${index + 1} of ${total}`}
                    value={((index + 1) / total) * 100}
                    className="h-1.5"
                />
            </div>

            <div
                key={index}
                className={direction === "next" ? "step-in-next" : direction === "back" ? "step-in-back" : undefined}
            >
                <ExerciseStep
                    exercise={exercise}
                    number={index + 1}
                    idPrefix={`${idPrefix}-${index}`}
                    headingRef={headingRef}
                    draft={draft}
                    onDraft={(value) => setDrafts((d) => ({ ...d, [index]: value }))}
                    answer={answer}
                    revealed={isRevealed}
                    onMark={(correct) => {
                        focusPrimary.current = true;
                        record(draft ?? "", correct);
                    }}
                />
            </div>

            <div className="flex flex-wrap items-center justify-between gap-2">
                <Button type="button" variant="outline" className="h-11" disabled={index === 0} onClick={() => go(index - 1)}>
                    Back
                </Button>
                <Button ref={primaryRef} type="button" className="h-11 min-w-28" disabled={primary.disabled} onClick={primary.run}>
                    {primary.label}
                </Button>
            </div>
        </div>
    );
}

export default ExercisePlayer;
