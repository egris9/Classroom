import { Link } from "react-router-dom";
import { ChevronRight, FileText } from "lucide-react";
import { toast } from "sonner";
import { deleteFile } from "../api/courses.js";
import ConfirmAction from "./ConfirmAction.jsx";
import UploadPdfDialog from "./UploadPdfDialog.jsx";

/** The PDFs of a course. The Teacher uploads and deletes; everyone opens. */
export function CourseMaterials({ courseId, role, files, onFilesChange }) {
    const isTeacher = role === "TEACHER";

    const remove = async (file) => {
        try {
            await deleteFile(file.id);
            onFilesChange(files.filter((item) => item.id !== file.id));
            toast.success(`${file.fileName} deleted.`);
        } catch (err) {
            toast.error(err.message);
        }
    };

    return (
        <div className="space-y-4">
            {isTeacher && (
                <UploadPdfDialog courseId={courseId} onUploaded={(file) => onFilesChange([...files, file])} />
            )}

            {files.length === 0 ? (
                <p className="rounded-lg border border-dashed p-8 text-center text-sm text-muted-foreground">
                    {isTeacher
                        ? "No PDFs yet. Choose Upload PDF to add the first one."
                        : "Your Teacher has not uploaded any PDFs yet."}
                </p>
            ) : (
                <ul className="divide-y rounded-lg border bg-card">
                    {files.map((file) => (
                        <li key={file.id} className="flex items-center gap-2 pr-2">
                            <Link
                                to={`/courses/${courseId}/files/${file.id}`}
                                className="flex min-h-14 min-w-0 flex-1 items-center gap-3 rounded-lg p-3 hover:bg-accent focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                            >
                                <FileText className="h-5 w-5 shrink-0 text-muted-foreground" aria-hidden="true" />
                                <span className="truncate font-medium">{file.fileName}</span>
                                <ChevronRight className="ml-auto h-4 w-4 shrink-0 text-muted-foreground" aria-hidden="true" />
                            </Link>
                            {isTeacher && (
                                <ConfirmAction
                                    label="Delete"
                                    title={`Delete ${file.fileName}?`}
                                    description="The PDF and every summary and exercise set made from it are removed for everyone in the course. This cannot be undone."
                                    confirmLabel="Delete PDF"
                                    onConfirm={() => remove(file)}
                                />
                            )}
                        </li>
                    ))}
                </ul>
            )}
        </div>
    );
}

export default CourseMaterials;
