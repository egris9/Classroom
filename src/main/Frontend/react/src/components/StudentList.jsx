import { useEffect, useState } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { listStudents } from "../api/courses.js";

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

    return (
        <Card>
            <CardHeader>
                <CardTitle className="text-lg">Students</CardTitle>
            </CardHeader>
            <CardContent>
                {error && (
                    <p role="alert" className="text-sm text-destructive">
                        {error}
                    </p>
                )}
                {!error && students === null && <p className="text-sm text-muted-foreground">Loading...</p>}
                {students && students.length === 0 && (
                    <p className="text-sm text-muted-foreground">
                        No one has joined yet. Share the access code so students can join.
                    </p>
                )}
                {students && students.length > 0 && (
                    <ul className="divide-y">
                        {students.map((student) => (
                            <li key={student.id} className="flex flex-col py-2 sm:flex-row sm:justify-between">
                                <span className="font-medium">
                                    {student.firstName} {student.lastName}
                                </span>
                                <span className="text-sm text-muted-foreground">{student.email}</span>
                            </li>
                        ))}
                    </ul>
                )}
            </CardContent>
        </Card>
    );
}

export default StudentList;
