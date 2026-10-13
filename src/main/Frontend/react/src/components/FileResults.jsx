import { useCallback, useEffect, useState } from "react";
import { toast } from "sonner";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Skeleton } from "@/components/ui/skeleton";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { ExerciseOptionsForm } from "@/components/exercises/ExerciseOptionsForm";
import { ExerciseSetCard } from "@/components/exercises/ExerciseSetCard";
import { StatusBadge } from "@/components/StatusBadge";
import { SummaryCard } from "@/components/SummaryCard";
import { getPref, setPref } from "@/lib/prefs";
import { DEFAULT_SUMMARY_LEVEL, isSummaryLevel, SUMMARY_LEVELS } from "@/lib/summaryLevels";
import { listExerciseSets, listSummaries, requestExerciseSet, requestSummary } from "../api/generation.js";

const POLL_MS = 2000;
// A result card that was not there before fades in and rises 4px (main.css).
const APPEAR = "result-appear";

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
    const [optionsOpen, setOptionsOpen] = useState(false);
    const [level, setLevel] = useState(() => {
        const stored = getPref("summaryLevel", DEFAULT_SUMMARY_LEVEL);
        return isSummaryLevel(stored) ? stored : DEFAULT_SUMMARY_LEVEL;
    });
    // The summaries that were here when the page loaded. One that comes later is new, and only a new one fades in.
    const [loadedIds, setLoadedIds] = useState(null);
    const [loadedSetIds, setLoadedSetIds] = useState(null);
    const isTeacher = role === "TEACHER";

    const refresh = useCallback(async () => {
        try {
            const [loadedSummaries, loadedSets] = await Promise.all([listSummaries(file.id), listExerciseSets(file.id)]);
            setSummaries(loadedSummaries);
            setLoadedIds((known) => known ?? new Set(loadedSummaries.map((summary) => summary.id)));
            setExerciseSets(loadedSets);
            setLoadedSetIds((known) => known ?? new Set(loadedSets.map((set) => set.id)));
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

    // The server refuses a second request of a kind while one is pending (409), so the button waits too.
    const exercisesPending = (exerciseSets ?? []).some((set) => set.status === "PENDING");

    /** Resolves true when the request was accepted; a refusal is shown as a toast. */
    const request = async (action, label) => {
        setBusy(true);
        try {
            await action(file.id);
            toast.success(`${label} requested. It appears here when it is ready.`);
            await refresh();
            return true;
        } catch (err) {
            toast.error(err.message);
            return false;
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

            {/* Kept mounted while the other tab shows, so each card stays folded or open as it was left. */}
            <TabsContent value="summary" forceMount className="mt-4 space-y-4 data-[state=inactive]:hidden">
                <div className="flex flex-wrap items-end gap-3">
                    <div className="grid gap-1.5">
                        <Label htmlFor="summary-level">Length</Label>
                        <Select value={level} onValueChange={setLevel} disabled={busy}>
                            <SelectTrigger id="summary-level" className="h-11 w-40 bg-card">
                                <SelectValue />
                            </SelectTrigger>
                            <SelectContent>
                                {SUMMARY_LEVELS.map((option) => (
                                    <SelectItem key={option.value} value={option.value} className="min-h-11">
                                        {option.label}
                                    </SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                    </div>
                    <Button
                        variant={summaries.length === 0 ? "default" : "outline"}
                        className="h-11"
                        disabled={busy}
                        onClick={() => {
                            setPref("summaryLevel", level);
                            request((fileId) => requestSummary(fileId, level), "Summary");
                        }}
                    >
                        {summaries.length === 0 ? "Summarise" : "Summarise again"}
                    </Button>
                </div>
                {error && (
                    <Alert variant="destructive">
                        <AlertDescription>{error}</AlertDescription>
                    </Alert>
                )}
                {summaries.length === 0 && <Empty>No summary yet. Choose Summarise to make one.</Empty>}
                {summaries.map((summary, index) => (
                    <SummaryCard
                        key={summary.id}
                        summary={summary}
                        badge={<ResultBadge item={summary} />}
                        collapsible
                        defaultOpen={index === 0}
                        className={loadedIds?.has(summary.id) ? undefined : APPEAR}
                    />
                ))}
            </TabsContent>

            {/* Kept mounted too, so a set being played is not lost when the Summary tab is visited. */}
            <TabsContent value="exercises" forceMount className="mt-4 space-y-4 data-[state=inactive]:hidden">
                <Button
                    variant="outline"
                    className="h-11"
                    disabled={busy || exercisesPending}
                    onClick={() => setOptionsOpen(true)}
                >
                    Generate exercises
                </Button>
                <ExerciseOptionsForm
                    open={optionsOpen}
                    onOpenChange={setOptionsOpen}
                    busy={busy}
                    onSubmit={async (options) => {
                        const accepted = await request((fileId) => requestExerciseSet(fileId, options), "Exercises");
                        if (accepted) {
                            setOptionsOpen(false);
                        }
                    }}
                />
                {exerciseSets.length === 0 && (
                    <Empty>
                        {isTeacher
                            ? "No exercises yet. Choose Generate exercises to make a set for your students."
                            : "No exercises yet. Choose Generate exercises to make a set only you can see."}
                    </Empty>
                )}
                {exerciseSets.map((set, index) => (
                    <ExerciseSetCard
                        key={set.id}
                        set={set}
                        badge={<ResultBadge item={set} />}
                        collapsible
                        defaultOpen={index === 0}
                        className={loadedSetIds?.has(set.id) ? undefined : APPEAR}
                    />
                ))}
            </TabsContent>
        </Tabs>
    );
}

export default FileResults;
