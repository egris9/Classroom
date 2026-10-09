import { cn } from "@/lib/utils";

// Both drawings use the page tokens only (fill-card, stroke-primary, stroke-border), so they follow the theme.

function Courses() {
    return (
        <>
            <ellipse cx="120" cy="140" rx="92" ry="9" className="fill-primary/10" />
            <path
                d="M28 112 120 126 120 52 28 38Z"
                className="fill-card stroke-primary"
                strokeWidth="2.5"
                strokeLinejoin="round"
            />
            <path
                d="M212 112 120 126 120 52 212 38Z"
                className="fill-card stroke-primary"
                strokeWidth="2.5"
                strokeLinejoin="round"
            />
            <g className="stroke-primary/35" strokeWidth="3" strokeLinecap="round">
                <path d="M42 56 106 66" />
                <path d="M42 70 106 80" />
                <path d="M42 84 88 91" />
                <path d="M134 66 198 56" />
                <path d="M134 80 198 70" />
            </g>
            <g strokeLinejoin="round">
                <path d="M170 98 214 30 222 35 178 103Z" className="fill-primary stroke-primary" strokeWidth="2" />
                <path d="M170 98 178 103 166 110Z" className="fill-card stroke-primary" strokeWidth="2" />
            </g>
        </>
    );
}

function Files() {
    return (
        <>
            <ellipse cx="120" cy="140" rx="84" ry="9" className="fill-primary/10" />
            <rect x="62" y="42" width="92" height="94" rx="8" className="fill-card stroke-border" strokeWidth="2.5" />
            <rect x="78" y="30" width="92" height="94" rx="8" className="fill-card stroke-border" strokeWidth="2.5" />
            <path
                d="M94 18h56l22 22v86a8 8 0 0 1-8 8H94a8 8 0 0 1-8-8V26a8 8 0 0 1 8-8Z"
                className="fill-card stroke-primary"
                strokeWidth="2.5"
                strokeLinejoin="round"
            />
            <path d="M150 18v22h22" className="stroke-primary" strokeWidth="2.5" strokeLinejoin="round" fill="none" />
            <g className="stroke-primary/35" strokeWidth="3" strokeLinecap="round">
                <path d="M100 62h56" />
                <path d="M100 76h56" />
                <path d="M100 90h36" />
            </g>
        </>
    );
}

const DRAWINGS = { courses: Courses, files: Files };

/** A small drawing for an empty list. `kind` is "courses" or "files". It carries no meaning of its own: the text beside it does. */
export function EmptyArt({ kind = "courses", className }) {
    const Drawing = DRAWINGS[kind] ?? Courses;
    return (
        <svg viewBox="0 0 240 160" fill="none" aria-hidden="true" className={cn("h-auto w-full max-w-[15rem]", className)}>
            <Drawing />
        </svg>
    );
}

export default EmptyArt;
