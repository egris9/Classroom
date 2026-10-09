import { useEffect, useState } from "react";
import { ExternalLink } from "lucide-react";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Skeleton } from "@/components/ui/skeleton";
import { fetchFileContent } from "../api/courses.js";

// On a desktop the frame fills the window below the header and the link under it; on a phone it is 60% of the window.
const FRAME_HEIGHT = "h-[60vh] min-h-[24rem] lg:h-[calc(100dvh-11rem)]";

/** Shows a course PDF in the page. It is fetched through the client, so the token is sent. */
export function PdfViewer({ fileId, title }) {
    const [url, setUrl] = useState(null);
    const [error, setError] = useState(null);

    useEffect(() => {
        let objectUrl = null;
        let cancelled = false;
        setUrl(null);
        setError(null);
        fetchFileContent(fileId)
            .then((blob) => {
                if (cancelled) {
                    return;
                }
                objectUrl = URL.createObjectURL(new Blob([blob], { type: "application/pdf" }));
                setUrl(objectUrl);
            })
            .catch((err) => !cancelled && setError(err.message));
        return () => {
            cancelled = true;
            if (objectUrl) {
                URL.revokeObjectURL(objectUrl);
            }
        };
    }, [fileId]);

    if (error) {
        return (
            <Alert variant="destructive">
                <AlertDescription>{error}</AlertDescription>
            </Alert>
        );
    }

    if (!url) {
        return <Skeleton className={`${FRAME_HEIGHT} w-full`} />;
    }

    return (
        <div className="space-y-1">
            {/* The browser's own viewer reads these open parameters: no thumbnail rail, page fitted to the width. */}
            <iframe
                title={title}
                src={`${url}#navpanes=0&view=FitH`}
                className={`${FRAME_HEIGHT} w-full rounded-xl border bg-card shadow`}
            />
            <a
                href={url}
                target="_blank"
                rel="noreferrer"
                className="inline-flex min-h-11 items-center gap-1 rounded-md text-sm text-primary underline-offset-4 hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
            >
                Open in a new tab
                <ExternalLink className="h-4 w-4" aria-hidden="true" />
            </a>
        </div>
    );
}

export default PdfViewer;
