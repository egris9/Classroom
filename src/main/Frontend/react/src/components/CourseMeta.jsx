import { DoorOpen, Layers } from "lucide-react";
import { cn } from "@/lib/utils";

/** Subject, section and room of a course. Each is its own item, so nothing relies on punctuation to tell them apart. */
export function CourseMeta({ course, className }) {
    return (
        <ul className={cn("flex flex-wrap items-center gap-x-4 gap-y-1 text-sm text-muted-foreground", className)}>
            {course.subject && <li className="font-medium text-foreground">{course.subject}</li>}
            {course.section && (
                <li className="inline-flex items-center gap-1.5">
                    <Layers className="h-4 w-4" aria-hidden="true" />
                    Section {course.section}
                </li>
            )}
            {course.room && (
                <li className="inline-flex items-center gap-1.5">
                    <DoorOpen className="h-4 w-4" aria-hidden="true" />
                    Room {course.room}
                </li>
            )}
        </ul>
    );
}

export default CourseMeta;
