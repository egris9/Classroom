import { useEffect, useState } from "react";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Skeleton } from "@/components/ui/skeleton";
import { listStudents } from "../api/courses.js";
import UserAvatar from "./UserAvatar.jsx";

/** The Students enrolled in a course. Only the Teacher is allowed to load it. */
export function StudentList({ courseId }) {
    const [students, setStudents] = useState(null);
    const [error, setError] = useState(null);

    useEffect(() => {
        let cancelled = false;
        listStudents(courseId)
            .then((loaded) => !cancelled && setStudents(loaded))
            .catch((err) => !cancelled && setError(err.message));
        return () => {
            cancelled = true;
        };
    }, [courseId]);

    if (error) {
        return (
            <Alert variant="destructive">
                <AlertDescription>{error}</AlertDescription>
            </Alert>
        );
    }

    if (students === null) {
        return <Skeleton className="h-24 w-full" />;
    }

    if (students.length === 0) {
        return (
            <p className="rounded-lg border border-dashed p-6 text-center text-sm text-muted-foreground">
                No one has joined yet. Share the access code above so students can join.
            </p>
        );
    }

    return (
        <ul className="divide-y rounded-lg border bg-card">
            {students.map((student) => (
                <li key={student.id} className="flex items-center gap-3 p-3">
                    <UserAvatar name={`${student.firstName} ${student.lastName}`} picture={student.picture} />
                    <div className="min-w-0">
                        <p className="truncate font-medium">
                            {student.firstName} {student.lastName}
                        </p>
                        <p className="truncate text-sm text-muted-foreground">{student.email}</p>
                    </div>
                </li>
            ))}
        </ul>
    );
}

export default StudentList;
