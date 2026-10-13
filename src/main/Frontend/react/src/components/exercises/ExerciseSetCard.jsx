import { useState } from "react";
import { badgeVariants } from "@/components/ui/badge";
import { CollapsibleCard } from "@/components/CollapsibleCard";
import { DemoNotice, PendingNotice, ResultFailure } from "@/components/SummaryCard";
import { ExercisePlayer } from "@/components/exercises/ExercisePlayer";
import { DIFFICULTY_LABELS, setTypeLabel, typeOf } from "@/lib/exercises";

const META_BADGE = badgeVariants({ variant: "secondary" });

/**
 * The type or types of a set. A set from the course API names them; one from the AI tools does not, so they are read
 * off its exercises.
 */
const typesOf = (set) => set.types ?? [...new Set((set.exercises ?? []).map(typeOf))];

/** The size, type and difficulty of a set, as separate small badges for the title row. */
function metaOf(set) {
    const questions = (set.exercises ?? []).length || set.count || 0;
    return (
        <>
            {questions > 0 && <span className={META_BADGE}>{questions === 1 ? "1 question" : `${questions} questions`}</span>}
            <span className={META_BADGE}>{setTypeLabel(typesOf(set))}</span>
            {DIFFICULTY_LABELS[set.difficulty] && <span className={META_BADGE}>{DIFFICULTY_LABELS[set.difficulty]}</span>}
        </>
    );
}

/**
 * One Exercise set as a card titled "Exercises": its notices, then the player. With `collapsible` the card folds, and
 * `defaultOpen` says how it starts. `badge` is an optional node for the title row, such as the status. A set with no
 * status (from the AI tools) is a finished one.
 */
export function ExerciseSetCard({ set, badge, collapsible = true, defaultOpen = true, className }) {
    // Folding the card unmounts the player, so the answers given so far are kept here and handed back when it opens.
    const [saved, setSaved] = useState([]);
    const playable = (set.status === undefined || set.status === "DONE") && (set.exercises ?? []).length > 0;
    return (
        <CollapsibleCard
            title="Exercises"
            meta={metaOf(set)}
            badge={badge}
            collapsible={collapsible}
            defaultOpen={defaultOpen}
            className={className}
        >
            <DemoNotice item={set} />
            <PendingNotice item={set} />
            {playable && (
                <ExercisePlayer
                    key={set.id}
                    exercises={set.exercises}
                    marking="self"
                    initialAnswers={saved}
                    onAnswer={(index, answer) => setSaved((current) => Object.assign([...current], { [index]: answer }))}
                    onRetry={() => setSaved([])}
                />
            )}
            <ResultFailure item={set} />
        </CollapsibleCard>
    );
}

export default ExerciseSetCard;
