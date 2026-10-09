import { useCallback, useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { ArrowLeft } from "lucide-react";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import CopyCode from "@/components/CopyCode";
import CourseMaterials from "@/components/CourseMaterials";
import CourseCover from "@/components/CourseCover";
import CourseMeta from "@/components/CourseMeta";
import CoursePeople from "@/components/CoursePeople";
import CourseSettings from "@/components/CourseSettings";
import PageContainer from "@/components/PageContainer";
import UserAvatar from "@/components/UserAvatar";
import { getCourse, listFiles } from "../api/courses.js";

export default function CoursePage() {
    const { courseId } = useParams();
    const [course, setCourse] = useState(null);
    const [files, setFiles] = useState([]);
    const [error, setError] = useState(null);

    const load = useCallback(() => {
        setError(null);
        setCourse(null);
        Promise.all([getCourse(courseId), listFiles(courseId)])
            .then(([loadedCourse, loadedFiles]) => {
                setCourse(loadedCourse);
                setFiles(loadedFiles);
            })
            .catch((err) => setError(err.message));
    }, [courseId]);

    useEffect(() => {
        load();
    }, [load]);

    const back = (
        <Link
            to="/courses"
            className="inline-flex min-h-11 items-center gap-1 rounded-md text-sm text-muted-foreground hover:text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
        >
            <ArrowLeft className="h-4 w-4" aria-hidden="true" />
            My courses
        </Link>
    );

    if (error) {
        return (
            <PageContainer className="space-y-4">
                {back}
                <Alert variant="destructive">
                    <AlertDescription className="flex flex-wrap items-center justify-between gap-2">
                        {error}
                        <Button variant="outline" size="sm" onClick={load}>
                            Try again
                        </Button>
                    </AlertDescription>
                </Alert>
            </PageContainer>
        );
    }

    if (!course) {
        return (
            <PageContainer className="space-y-4" aria-busy="true">
                {back}
                <Skeleton className="h-32 w-full sm:h-40" />
                <Skeleton className="h-10 w-2/3" />
                <Skeleton className="h-6 w-1/3" />
                <Skeleton className="h-64 w-full" />
            </PageContainer>
        );
    }

    const isTeacher = course.role === "TEACHER";

    return (
        <PageContainer className="space-y-6">
            {back}
            <header className="space-y-4">
                <CourseCover course={course} aspect="aspect-auto" className="h-32 rounded-xl border sm:h-40" />
                <div className="space-y-3">
                    <h1 className="text-title font-semibold">{course.courseName}</h1>
                    <CourseMeta course={course} />
                    <div className="flex flex-wrap items-center gap-3">
                        <span className="inline-flex items-center gap-2 rounded-full border bg-card py-1 pl-1 pr-3 text-sm shadow">
                            <UserAvatar name={course.teacher?.name} picture={course.teacher?.picture} className="h-8 w-8" />
                            <span className="sr-only">Teacher: </span>
                            {course.teacher?.name}
                        </span>
                        {isTeacher && course.accessCode && <CopyCode code={course.accessCode} />}
                    </div>
                </div>
            </header>

            <Tabs defaultValue="materials">
                <TabsList className="h-14">
                    <TabsTrigger value="materials" className="h-11 px-3 sm:px-5">
                        Materials
                    </TabsTrigger>
                    <TabsTrigger value="people" className="h-11 px-3 sm:px-5">
                        People
                    </TabsTrigger>
                    <TabsTrigger value="settings" className="h-11 px-3 sm:px-5">
                        Settings
                    </TabsTrigger>
                </TabsList>
                <TabsContent value="materials" className="mt-6">
                    <CourseMaterials courseId={course.id} role={course.role} files={files} onFilesChange={setFiles} />
                </TabsContent>
                <TabsContent value="people" className="mt-6">
                    <CoursePeople course={course} />
                </TabsContent>
                <TabsContent value="settings" className="mt-6">
                    <CourseSettings course={course} onCourseChange={setCourse} />
                </TabsContent>
            </Tabs>
        </PageContainer>
    );
}
