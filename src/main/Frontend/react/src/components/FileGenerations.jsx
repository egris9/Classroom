import { useCallback, useEffect, useState } from "react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import { StatusBadge } from "@/components/StatusBadge";
import { Badge } from "@/components/ui/badge";
import {
    listExerciseSets,
    listSummaries,
    requestExerciseSet,
    requestSummary,
} from "../api/generation.js";

const POLL_MS = 2000;

function ResultBadge({ item }) {
    if (item.status === "PENDING") {
        return <StatusBadge status="warning">Pending</StatusBadge>;
    }
    if (item.status === "FAILED") {
        return <StatusBadge status="destructive">Failed</StatusBadge>;
    }
    return item.published ? (
        <StatusBadge status="success">Published</StatusBadge>
    ) : (
        <Badge variant="outline">Only you</Badge>
    );
}

function Failure({ item }) {
    if (item.status !== "FAILED") {
        return null;
    }
    return (
        <p role="alert" className="text-sm text-destructive">
            {item.failureMessage} ({item.failureCode})
        </p>
    );
}

/** Summarise and Generate exercises for one file, with the results already made for it. */
export function FileGenerations({ file, role }) {
    const [open, setOpen] = useState(false);
    const [summaries, setSummaries] = useState([]);
    const [exerciseSets, setExerciseSets] = useState([]);
    const [error, setError] = useState(null);
    const [busy, setBusy] = useState(false);

    const refresh = useCallback(async () => {
        try {
            const [loadedSummaries, loadedSets] = await Promise.all([listSummaries(file.id), listExerciseSets(file.id)]);
            setSummaries(loadedSummaries);
            setExerciseSets(loadedSets);
            setError(null);
        } catch (err) {
            setError(err.message);
        }
    }, [file.id]);

    useEffect(() => {
        if (open) {
            refresh();
        }
    }, [open, refresh]);

    const pending = [...summaries, ...exerciseSets].some((item) => item.status === "PENDING");
    useEffect(() => {
        if (!open || !pending) {
            return undefined;
        }
        const timer = setTimeout(refresh, POLL_MS);
        return () => clearTimeout(timer);
    }, [open, pending, summaries, exerciseSets, refresh]);

    const request = async (action, label) => {
        setBusy(true);
        try {
            await action(file.id);
            toast.success(`${label} requested.`);
            if (open) {
                await refresh();
            } else {
                setOpen(true);
            }
        } catch (err) {
            toast.error(err.message);
        } finally {
            setBusy(false);
        }
    };

    return (
        <div className="mt-3">
            <div className="flex flex-wrap gap-2">
                <Button size="sm" disabled={busy} onClick={() => request(requestSummary, "Summary")}>
                    Summarise
                </Button>
                {role === "TEACHER" && (
                    <Button size="sm" disabled={busy} onClick={() => request(requestExerciseSet, "Exercises")}>
                        Generate exercises
                    </Button>
                )}
                <Button size="sm" variant="outline" aria-expanded={open} onClick={() => setOpen(!open)}>
                    {open ? "Hide results" : "Show results"}
                </Button>
            </div>

            {open && (
                <div className="mt-3 space-y-3">
                    {error && (
                        <p role="alert" className="text-sm text-destructive">
                            {error}
                        </p>
                    )}
                    {summaries.length === 0 && exerciseSets.length === 0 && !error && (
                        <p className="text-sm text-muted-foreground">
                            Nothing yet. Choose Summarise{role === "TEACHER" ? " or Generate exercises" : ""} to start.
                        </p>
                    )}

                    {summaries.map((summary) => (
                        <Card key={`summary-${summary.id}`}>
                            <CardContent className="space-y-2 p-4">
                                <div className="flex items-center justify-between gap-2">
                                    <span className="text-sm font-medium">Summary</span>
                                    <ResultBadge item={summary} />
                                </div>
                                {summary.text && <p className="whitespace-pre-wrap text-sm">{summary.text}</p>}
                                <Failure item={summary} />
                            </CardContent>
                        </Card>
                    ))}

                    {exerciseSets.map((set) => (
                        <Card key={`exercises-${set.id}`}>
                            <CardContent className="space-y-2 p-4">
                                <div className="flex items-center justify-between gap-2">
                                    <span className="text-sm font-medium">Exercises</span>
                                    <ResultBadge item={set} />
                                </div>
                                <ol className="list-decimal space-y-2 pl-5 text-sm">
                                    {set.exercises.map((exercise) => (
                                        <li key={exercise.question}>
                                            <p>{exercise.question}</p>
                                            <details className="text-muted-foreground">
                                                <summary className="cursor-pointer">Show answer</summary>
                                                <p>{exercise.answer}</p>
                                            </details>
                                        </li>
                                    ))}
                                </ol>
                                <Failure item={set} />
                            </CardContent>
                        </Card>
                    ))}
                    <Separator />
                </div>
            )}
        </div>
    );
}

export default FileGenerations;
