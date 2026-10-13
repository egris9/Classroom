import { ChevronDown } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from "@/components/ui/collapsible";
import { cn } from "@/lib/utils";

const HEADER = "flex min-h-11 w-full items-center gap-2 px-4 py-2 text-left";
// The heading carries the card's whole header, so only the title inside it is set like a heading.
const HEADING = "font-sans text-sm font-normal tracking-normal";
const BODY = "space-y-2 px-4 pb-4 pt-0";

function TitleAndMeta({ title, meta }) {
    return (
        <span className="flex flex-wrap items-baseline gap-x-2 gap-y-0.5">
            <span className="font-display text-base font-semibold tracking-tight">{title}</span>
            {meta && <span className="flex flex-wrap items-baseline gap-x-2 text-muted-foreground">{meta}</span>}
        </span>
    );
}

/**
 * A Card with a title row (`title`, then `meta` nodes, then a `badge` at the far side) that folds its content away.
 * The whole title row is one button. With `collapsible={false}` it is a plain card that is always open.
 */
export function CollapsibleCard({ title, meta, badge, defaultOpen = true, collapsible = true, className, children }) {
    if (!collapsible) {
        return (
            <Card className={className}>
                <div className={HEADER}>
                    <h2 className={cn(HEADING, "min-w-0 flex-1")}>
                        <TitleAndMeta title={title} meta={meta} />
                    </h2>
                    {badge}
                </div>
                <CardContent className={BODY}>{children}</CardContent>
            </Card>
        );
    }
    return (
        <Card className={className}>
            <Collapsible defaultOpen={defaultOpen}>
                <h2 className={HEADING}>
                    <CollapsibleTrigger
                        className={cn(
                            HEADER,
                            "group rounded-xl transition-colors hover:bg-muted/60",
                            "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-ring",
                        )}
                    >
                        <ChevronDown
                            aria-hidden="true"
                            className="size-4 shrink-0 text-muted-foreground transition-transform duration-200 [transition-timing-function:cubic-bezier(0.2,0,0,1)] group-data-[state=open]:rotate-180"
                        />
                        <span className="min-w-0 flex-1">
                            <TitleAndMeta title={title} meta={meta} />
                        </span>
                        {badge}
                    </CollapsibleTrigger>
                </h2>
                <CollapsibleContent className="collapsible-content">
                    <CardContent className={BODY}>{children}</CardContent>
                </CollapsibleContent>
            </Collapsible>
        </Card>
    );
}

export default CollapsibleCard;
