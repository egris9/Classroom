import { useCallback, useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { ArrowLeft } from "lucide-react";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import FileResults from "@/components/FileResults";
import PageContainer from "@/components/PageContainer";
import PdfViewer from "@/components/PdfViewer";
import { getCourse, listFiles } from "../api/courses.js";

export default function FilePage() {
    const { courseId, fileId } = useParams();
    const [state, setState] = useState({ course: null, file: null });
    const [error, setError] = useState(null);

    const load = useCallback(() => {
        setError(null);
        setState({ course: null, file: null });
        Promise.all([getCourse(courseId), listFiles(courseId)])
            .then(([course, files]) => {
                const file = files.find((item) => String(item.id) === fileId);
                if (file) {
                    setState({ course, file });
                } else {
                    setError("This PDF is not in the course. It may have been deleted.");
                }
            })
            .catch((err) => setError(err.message));
    }, [courseId, fileId]);

    useEffect(() => {
        load();
    }, [load]);

    const { course, file } = state;

    const back = (
        <Link
            to={`/courses/${courseId}`}
            className="inline-flex min-h-11 items-center gap-1 rounded-md text-sm text-muted-foreground hover:text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
        >
            <ArrowLeft className="h-4 w-4" aria-hidden="true" />
            {course ? course.courseName : "Back to the course"}
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

    if (!file) {
        return (
            <PageContainer className="space-y-4" aria-busy="true">
                {back}
                <Skeleton className="h-10 w-1/2" />
                <Skeleton className="h-96 w-full" />
            </PageContainer>
        );
    }

    return (
        <PageContainer className="space-y-4">
            {back}
            <h1 className="break-words text-3xl font-semibold">{file.fileName}</h1>
            <div className="grid gap-6 lg:grid-cols-2">
                <PdfViewer fileId={file.id} title={file.fileName} />
                <FileResults file={file} role={course.role} />
            </div>
        </PageContainer>
    );
}
