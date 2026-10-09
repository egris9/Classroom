import { useCallback, useEffect, useState } from "react";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import AddCourseMenu from "@/components/AddCourseMenu";
import CourseGroup from "@/components/CourseGroup";
import EmptyArt from "@/components/EmptyArt";
import PageContainer from "@/components/PageContainer";
import { getUser } from "../api/auth.js";
import { getCourses } from "../api/courses.js";

export default function MyCourses() {
    const [courses, setCourses] = useState(null);
    const [error, setError] = useState(null);

    const load = useCallback(() => {
        setError(null);
        getCourses()
            .then(setCourses)
            .catch((err) => setError(err.message));
    }, []);

    useEffect(() => {
        load();
    }, [load]);

    const empty = courses !== null && courses.length === 0;
    const teaching = courses?.filter((course) => course.role === "TEACHER") ?? [];
    const enrolled = courses?.filter((course) => course.role === "STUDENT") ?? [];
    const counts = [
        teaching.length > 0 && `${teaching.length} teaching`,
        enrolled.length > 0 && `${enrolled.length} enrolled`,
    ].filter(Boolean);
    const firstName = getUser()?.firstName;

    return (
        <PageContainer className="space-y-8">
            <div className="flex flex-wrap items-end justify-between gap-4">
                <div className="space-y-1">
                    <h1 className="text-title font-semibold">My courses</h1>
                    <p className="text-muted-foreground">
                        {courses === null || empty
                            ? `Welcome${firstName ? `, ${firstName}` : ""}.`
                            : `Welcome back${firstName ? `, ${firstName}` : ""}. ${counts.join(", ")}.`}
                    </p>
                </div>
                {courses !== null && !empty && <AddCourseMenu />}
            </div>

            {error && (
                <Alert variant="destructive">
                    <AlertDescription className="flex flex-wrap items-center justify-between gap-2">
                        {error}
                        <Button variant="outline" size="sm" onClick={load}>
                            Try again
                        </Button>
                    </AlertDescription>
                </Alert>
            )}

            {courses === null && !error && (
                <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3" aria-busy="true">
                    <Skeleton className="h-36" />
                    <Skeleton className="h-36" />
                    <Skeleton className="h-36" />
                </div>
            )}

            {empty && (
                <div className="mx-auto flex max-w-md flex-col items-center gap-4 py-8 text-center">
                    <EmptyArt kind="courses" />
                    <h2 className="text-heading font-semibold">No courses yet</h2>
                    <p className="text-muted-foreground">
                        Create a course to upload PDFs for your students, or join one with the access code your Teacher gave you.
                    </p>
                    <AddCourseMenu />
                </div>
            )}

            {courses !== null && !empty && (
                <>
                    <CourseGroup title="Teaching" courses={teaching} />
                    <CourseGroup title="Enrolled" courses={enrolled} />
                </>
            )}
        </PageContainer>
    );
}
