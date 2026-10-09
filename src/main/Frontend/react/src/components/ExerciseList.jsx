import { Card, CardContent } from "@/components/ui/card";
import { DemoNotice, PendingNotice, ResultFailure } from "@/components/SummaryCard";

/** One Exercise set as a card: each question with its answer behind "Show answer". `badge` is an optional node. */
export function ExerciseList({ set, badge }) {
    return (
        <Card>
            <CardContent className="space-y-2 p-4">
                <div className="flex items-center justify-between gap-2">
                    <span className="text-sm font-medium">Exercises</span>
                    {badge}
                </div>
                <DemoNotice item={set} />
                <PendingNotice item={set} />
                <ol className="list-decimal space-y-3 pl-5 text-sm leading-reading">
                    {(set.exercises ?? []).map((exercise) => (
                        <li key={exercise.question}>
                            <p dir="auto">{exercise.question}</p>
                            <details className="text-muted-foreground">
                                <summary className="min-h-11 cursor-pointer py-2">Show answer</summary>
                                <p dir="auto">{exercise.answer}</p>
                            </details>
                        </li>
                    ))}
                </ol>
                <ResultFailure item={set} />
            </CardContent>
        </Card>
    );
}

export default ExerciseList;
