import { useCallback, useEffect, useState } from "react";
import { toast } from "sonner";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { ExerciseList } from "@/components/ExerciseList";
import { StatusBadge } from "@/components/StatusBadge";
import { SummaryCard } from "@/components/SummaryCard";
import { listExerciseSets, listSummaries, requestExerciseSet, requestSummary } from "../api/generation.js";

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

function Empty({ children }) {
    return <p className="rounded-xl border border-dashed p-6 text-center text-sm text-muted-foreground">{children}</p>;
}

/** The Summaries and Exercise sets of one file, with the buttons that ask for new ones. */
export function FileResults({ file, role }) {
    const [summaries, setSummaries] = useState(null);
    const [exerciseSets, setExerciseSets] = useState(null);
    const [error, setError] = useState(null);
    const [busy, setBusy] = useState(false);
    const isTeacher = role === "TEACHER";

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
        refresh();
    }, [refresh]);

    const pending = [...(summaries ?? []), ...(exerciseSets ?? [])].some((item) => item.status === "PENDING");
    useEffect(() => {
        if (!pending) {
            return undefined;
        }
        const timer = setTimeout(refresh, POLL_MS);
        return () => clearTimeout(timer);
    }, [pending, summaries, exerciseSets, refresh]);

    const request = async (action, label) => {
        setBusy(true);
        try {
            await action(file.id);
            toast.success(`${label} requested. It appears here when it is ready.`);
            await refresh();
        } catch (err) {
            toast.error(err.message);
        } finally {
            setBusy(false);
        }
    };

    if (error && summaries === null) {
        return (
            <Alert variant="destructive">
                <AlertDescription className="flex flex-wrap items-center justify-between gap-2">
                    {error}
                    <Button variant="outline" size="sm" onClick={refresh}>
                        Try again
                    </Button>
                </AlertDescription>
            </Alert>
        );
    }

    if (summaries === null) {
        return <Skeleton className="h-64 w-full" />;
    }

    return (
        <Tabs defaultValue="summary">
            <TabsList className="h-14">
                <TabsTrigger value="summary" className="h-11 px-5">
                    Summary
                </TabsTrigger>
                <TabsTrigger value="exercises" className="h-11 px-5">
                    Exercises
                </TabsTrigger>
            </TabsList>

            <TabsContent value="summary" className="mt-4 space-y-4">
                <Button
                    variant={summaries.length === 0 ? "default" : "outline"}
                    className="h-11"
                    disabled={busy}
                    onClick={() => request(requestSummary, "Summary")}
                >
                    {summaries.length === 0 ? "Summarise" : "Summarise again"}
                </Button>
                {error && (
                    <Alert variant="destructive">
                        <AlertDescription>{error}</AlertDescription>
                    </Alert>
                )}
                {summaries.length === 0 && <Empty>No summary yet. Choose Summarise to make one.</Empty>}
                {summaries.map((summary) => (
                    <SummaryCard key={summary.id} summary={summary} badge={<ResultBadge item={summary} />} />
                ))}
            </TabsContent>

            <TabsContent value="exercises" className="mt-4 space-y-4">
                {isTeacher && (
                    <Button
                        variant="outline"
                        className="h-11"
                        disabled={busy}
                        onClick={() => request(requestExerciseSet, "Exercises")}
                    >
                        Generate exercises
                    </Button>
                )}
                {exerciseSets.length === 0 && (
                    <Empty>
                        {isTeacher
                            ? "No exercises yet. Choose Generate exercises to make a set for your students."
                            : "Your Teacher has not published exercises for this PDF yet."}
                    </Empty>
                )}
                {exerciseSets.map((set) => (
                    <ExerciseList key={set.id} set={set} badge={<ResultBadge item={set} />} />
                ))}
            </TabsContent>
        </Tabs>
    );
}

export default FileResults;
