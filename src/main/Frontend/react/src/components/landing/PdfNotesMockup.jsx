import { StatusBadge } from "@/components/StatusBadge";
import { buttonVariants } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { cn } from "@/lib/utils";

// Sample content. Each bullet is [before, key term, after].
const BULLETS = [
    ["", "Light reactions", " split water and store energy as ATP."],
    ["The ", "Calvin cycle", " uses that energy to fix CO₂ into sugar."],
    ["More light speeds it up until CO₂ becomes the ", "limiting factor", "."],
];

function Lines({ widths }) {
    return widths.map((width, index) => (
        <span key={index} className="block h-1.5 rounded-full bg-paper-line" style={{ width: `${width}%` }} />
    ));
}

function Marked({ children }) {
    // Narrow enough to stay clear of the summary card that overlaps the page.
    return (
        <span className="block w-fit max-w-[9.5rem] rounded-sm bg-paper-mark px-1 py-0.5 text-[0.6875rem] font-medium leading-snug">
            {children}
        </span>
    );
}

/** A drawing of the product: a PDF page with the summary written from it. Sample data, nothing in it can be used. */
export function PdfNotesMockup({ className }) {
    return (
        <figure className={cn("w-full max-w-lg select-none", className)}>
            {/* The image role sits on this inner div, not on the figure: a figcaption inside an img is not valid ARIA.
                pointer-events-none keeps the drawn buttons from reacting to the mouse. */}
            <div
                role="img"
                aria-label="Sample: a PDF page about photosynthesis, with the summary ClassHub wrote from it"
                className="pointer-events-none sm:grid sm:grid-cols-12"
            >
                <div className="h-60 space-y-2 overflow-hidden rounded-lg border bg-paper p-5 text-paper-foreground shadow-raised sm:col-start-1 sm:col-end-9 sm:row-start-1 sm:aspect-[3/4] sm:h-auto sm:p-6">
                    <p className="text-[0.6875rem] font-medium">Week 3</p>
                    <p className="pb-2 font-display text-xl font-semibold tracking-tight">Photosynthesis</p>
                    <Lines widths={[96, 90]} />
                    <Marked>Light reactions happen in the thylakoid membrane.</Marked>
                    <Lines widths={[94, 70]} />
                    <svg viewBox="0 0 120 56" className="my-3 h-16 w-3/5" fill="none" stroke="currentColor" strokeLinecap="round">
                        <path d="M8 4v44h106" strokeWidth="1" opacity="0.5" />
                        <path d="M8 48C30 20 50 12 112 10" strokeWidth="2" />
                    </svg>
                    <Lines widths={[92]} />
                    <Marked>The Calvin cycle fixes CO₂ into sugar.</Marked>
                    <Lines widths={[96, 88, 93, 60]} />
                </div>

                <div className="relative -mt-16 ml-6 space-y-2 sm:col-start-6 sm:col-end-13 sm:row-start-1 sm:ml-0 sm:mt-28 lg:mt-24 xl:mt-28">
                    <span className={cn(buttonVariants({ size: "sm" }), "h-9 bg-[image:var(--gradient-primary)] px-3 text-sm")}>
                        Summarise
                    </span>
                    <Card className="bg-surface-raised shadow-raised">
                        <CardContent className="space-y-3 p-4">
                            <div className="flex items-center justify-between gap-2">
                                <span className="text-sm font-medium">Summary</span>
                                <StatusBadge status="success">Published</StatusBadge>
                            </div>
                            <p className="text-sm leading-reading">
                                Photosynthesis turns light, water and CO₂ into sugar, in two linked stages.
                            </p>
                            <ul className="space-y-2 text-sm leading-reading">
                                {BULLETS.map(([before, term, after], index) => (
                                    <li
                                        key={term}
                                        className="flex gap-2 motion-safe:animate-in motion-safe:fade-in-0 motion-safe:slide-in-from-bottom-2 motion-safe:duration-500 motion-safe:fill-mode-both"
                                        style={{ animationDelay: `${400 + index * 250}ms` }}
                                    >
                                        <span className="mt-[0.6em] h-1.5 w-1.5 shrink-0 rounded-full bg-primary" />
                                        <span>
                                            {before}
                                            <strong className="font-semibold">{term}</strong>
                                            {after}
                                        </span>
                                    </li>
                                ))}
                            </ul>
                        </CardContent>
                    </Card>
                </div>
            </div>
            <figcaption className="mt-4 text-sm text-muted-foreground">Sample: Photosynthesis, week 3</figcaption>
        </figure>
    );
}

export default PdfNotesMockup;
