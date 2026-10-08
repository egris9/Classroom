import { useState } from "react";
import { Upload } from "lucide-react";
import { toast } from "sonner";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogHeader,
    DialogTitle,
    DialogTrigger,
} from "@/components/ui/dialog";
import FileDropzone from "@/components/FileDropzone";
import { uploadFile } from "../api/courses.js";

function UploadForm({ courseId, onUploaded }) {
    const [file, setFile] = useState(null);
    const [error, setError] = useState(null);
    const [submitting, setSubmitting] = useState(false);

    const submit = async (event) => {
        event.preventDefault();
        setError(null);
        setSubmitting(true);
        try {
            const uploaded = await uploadFile(courseId, file);
            toast.success(`${uploaded.fileName} uploaded.`);
            onUploaded(uploaded);
        } catch (err) {
            setError(err.message);
            setSubmitting(false);
        }
    };

    return (
        <form onSubmit={submit} className="space-y-4">
            <FileDropzone
                id="pdf-file"
                label="PDF file"
                accept="application/pdf"
                maxBytes={20 * 1024 * 1024}
                hint="PDF, up to 20 MB"
                file={file}
                onFile={setFile}
            />
            {error && (
                <Alert variant="destructive">
                    <AlertDescription>{error}</AlertDescription>
                </Alert>
            )}
            <Button type="submit" className="h-11 w-full" disabled={submitting || !file}>
                {submitting ? "Uploading..." : "Upload PDF"}
            </Button>
        </form>
    );
}

/** Teacher only: a button that opens a dialog to upload a PDF into the course. */
export function UploadPdfDialog({ courseId, onUploaded }) {
    const [open, setOpen] = useState(false);

    return (
        <Dialog open={open} onOpenChange={setOpen}>
            <DialogTrigger asChild>
                <Button className="h-11 gap-2 px-4">
                    <Upload className="h-4 w-4" aria-hidden="true" />
                    Upload PDF
                </Button>
            </DialogTrigger>
            <DialogContent>
                <DialogHeader>
                    <DialogTitle>Upload a PDF</DialogTitle>
                    <DialogDescription>Everyone in the course can open it. The limit is 20 MB.</DialogDescription>
                </DialogHeader>
                <UploadForm
                    courseId={courseId}
                    onUploaded={(file) => {
                        setOpen(false);
                        onUploaded(file);
                    }}
                />
            </DialogContent>
        </Dialog>
    );
}

export default UploadPdfDialog;
