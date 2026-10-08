import { ChevronDown, ChevronRight } from "lucide-react";
import { StatusBadge } from "@/components/StatusBadge";
import { Card, CardContent } from "@/components/ui/card";
import { cn } from "@/lib/utils";

/** A drawing of an Exercise set: one answer shown, one still closed. Sample data, nothing in it can be used. */
export function ExerciseMockup({ className }) {
    return (
        <figure
            role="img"
            aria-label="Sample: two exercises written from the photosynthesis PDF, the first with its model answer shown"
            className={cn("w-full select-none", className)}
        >
            <Card>
                <CardContent className="space-y-3 p-4">
                    <div className="flex items-center justify-between gap-2">
                        <span className="text-sm font-medium">Exercises</span>
                        <StatusBadge status="success">Published</StatusBadge>
                    </div>
                    <ol className="list-decimal space-y-3 pl-5 text-sm leading-reading">
                        <li>
                            Why does the rate of photosynthesis stop rising when the light gets very strong?
                            <span className="mt-1 flex items-center gap-1 text-muted-foreground">
                                <ChevronDown className="h-4 w-4" />
                                Hide answer
                            </span>
                            <span className="mt-1 block rounded-md bg-muted p-3">
                                Another factor runs out first, usually CO₂, so more light can no longer speed up the Calvin cycle.
                            </span>
                        </li>
                        <li>
                            Name the two stages of photosynthesis and say where each one happens.
                            <span className="mt-1 flex items-center gap-1 text-muted-foreground">
                                <ChevronRight className="h-4 w-4" />
                                Show answer
                            </span>
                        </li>
                    </ol>
                </CardContent>
            </Card>
            <figcaption className="mt-3 text-sm text-muted-foreground">Sample: exercises from the same PDF</figcaption>
        </figure>
    );
}

export default ExerciseMockup;
