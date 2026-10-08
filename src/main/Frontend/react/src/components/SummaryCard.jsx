import { Badge } from "@/components/ui/badge";
import { Card, CardContent } from "@/components/ui/card";
import { Markdown } from "@/components/Markdown";

/** Says that a result came from the demo adapter, which is the case when its model is "fake". */
export function DemoNotice({ item }) {
    if (item.model !== "fake") {
        return null;
    }
    return (
        <div className="flex flex-wrap items-center gap-2">
            <Badge variant="outline">Demo output</Badge>
            <span className="text-sm text-muted-foreground">No model is connected, so this only quotes the PDF.</span>
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

/** One Summary as a card. `badge` is an optional node shown beside the title, such as the result's status. */
export function SummaryCard({ summary, badge }) {
    return (
        <Card>
            <CardContent className="space-y-2 p-4">
                <div className="flex items-center justify-between gap-2">
                    <span className="text-sm font-medium">Summary</span>
                    {badge}
                </div>
                <DemoNotice item={summary} />
                {summary.text && <Markdown>{summary.text}</Markdown>}
                <ResultFailure item={summary} />
            </CardContent>
        </Card>
    );
}

export default SummaryCard;
