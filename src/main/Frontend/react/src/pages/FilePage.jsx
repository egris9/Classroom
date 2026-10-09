import { useCallback, useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { ArrowLeft, Eye, EyeOff } from "lucide-react";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { cn } from "@/lib/utils";
import FileResults from "@/components/FileResults";
import PageContainer from "@/components/PageContainer";
import PdfViewer from "@/components/PdfViewer";
import { getCourse, listFiles } from "../api/courses.js";

export default function FilePage() {
    const { courseId, fileId } = useParams();
    const [state, setState] = useState({ course: null, file: null });
    const [error, setError] = useState(null);
    const [showPdf, setShowPdf] = useState(false);

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

    // Results come first in the page and in the reading order. On a desktop the PDF sits beside them and stays in
    // view while a long result scrolls; on a phone it is behind a toggle so Summarise is on the first screen.
    return (
        <PageContainer className="space-y-4">
            {back}
            <h1 className="break-words text-title font-semibold">{file.fileName}</h1>
            <div className="grid gap-6 lg:grid-cols-2 lg:items-start">
                <FileResults file={file} role={course.role} />
                <div className="flex flex-col gap-3 lg:sticky lg:top-20">
                    <Button
                        type="button"
                        variant="outline"
                        className="h-11 gap-2 lg:hidden"
                        aria-expanded={showPdf}
                        aria-controls="pdf-panel"
                        onClick={() => setShowPdf((open) => !open)}
                    >
                        {showPdf ? <EyeOff aria-hidden="true" /> : <Eye aria-hidden="true" />}
                        {showPdf ? "Hide PDF" : "Show PDF"}
                    </Button>
                    <div id="pdf-panel" className={cn(!showPdf && "hidden", "lg:block")}>
                        <PdfViewer fileId={file.id} title={file.fileName} />
                    </div>
                </div>
            </div>
        </PageContainer>
    );
}
