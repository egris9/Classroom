import { Badge } from "@/components/ui/badge";
import { Progress } from "@/components/ui/progress";
import { CollapsibleCard } from "@/components/CollapsibleCard";
import { Markdown } from "@/components/Markdown";
import { summaryLevelLabel } from "@/lib/summaryLevels";

/** Says that a result came from the demo adapter, which is the case when its model is "fake". */
export function DemoNotice({ item }) {
    if (item.model !== "fake") {
        return null;
    }
    return (
        <div className="flex flex-wrap items-center gap-2">
            <Badge variant="outline">Demo output</Badge>
            <span className="text-sm text-muted-foreground">No model is connected, so this only quotes its source.</span>
        </div>
    );
}

/**
 * A PENDING result has no content yet. The API reports only that it is pending, so this shows one bar that says
 * "busy" and names no stage.
 */
export function PendingNotice({ item }) {
    if (item.status !== "PENDING") {
        return null;
    }
    return (
        <div role="status" className="space-y-2 py-1">
            <p className="text-sm font-medium">Working on it</p>
            <Progress aria-label="Working on it" className="h-1.5" />
            <p className="text-sm text-muted-foreground">It appears here when it is ready. You can leave this page.</p>
        </div>
    );
}

/** Why a FAILED result has no content. */
export function ResultFailure({ item }) {
    if (item.status !== "FAILED") {
        return null;
    }
    return (
        <p role="alert" className="text-sm text-destructive">
            {item.failureMessage} ({item.failureCode})
        </p>
    );
}

const DAY = { day: "numeric", month: "short", year: "numeric" };

/** The level and the day of a Summary, for the title row, or null: a summary from the AI tools has neither. */
function metaOf(summary) {
    const level = summaryLevelLabel(summary.level);
    const created = summary.createdAt ? new Date(summary.createdAt) : null;
    const day = created && !Number.isNaN(created.getTime()) ? created : null;
    if (!level && !day) {
        return null;
    }
    return (
        <>
            {level && <span>{level}</span>}
            {day && <time dateTime={day.toISOString()}>{day.toLocaleDateString(undefined, DAY)}</time>}
        </>
    );
}

/**
 * One Summary as a card. `badge` is an optional node shown in the title row, such as the result's status. With
 * `collapsible` the card folds, and `defaultOpen` says how it starts; without it the card is always open.
 */
export function SummaryCard({ summary, badge, collapsible = false, defaultOpen = true, className }) {
    return (
        <CollapsibleCard
            title="Summary"
            meta={metaOf(summary)}
            badge={badge}
            collapsible={collapsible}
            defaultOpen={defaultOpen}
            className={className}
        >
            <DemoNotice item={summary} />
            <PendingNotice item={summary} />
            {summary.text && <Markdown>{summary.text}</Markdown>}
            <ResultFailure item={summary} />
        </CollapsibleCard>
    );
}

export default SummaryCard;
