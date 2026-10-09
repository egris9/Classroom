import { Link } from "react-router-dom";
import { FileText } from "lucide-react";
import { toast } from "sonner";
import { deleteFile } from "../api/courses.js";
import ConfirmAction from "./ConfirmAction.jsx";
import EmptyArt from "./EmptyArt.jsx";
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
        <section aria-labelledby="materials-heading" className="space-y-4">
            <div className="flex flex-wrap items-center justify-between gap-3">
                <h2 id="materials-heading" className="text-heading font-semibold">
                    PDFs{files.length > 0 && <span className="ml-2 text-base font-normal text-muted-foreground">{files.length}</span>}
                </h2>
                {isTeacher && (
                    <UploadPdfDialog courseId={courseId} onUploaded={(file) => onFilesChange([...files, file])} />
                )}
            </div>

            {files.length === 0 ? (
                <div className="flex flex-col items-center gap-3 rounded-xl border border-dashed p-8 text-center">
                    <EmptyArt kind="files" className="max-w-[10rem]" />
                    <p className="max-w-sm text-sm text-muted-foreground">
                        {isTeacher
                            ? "No PDFs yet. Choose Upload PDF to add the first one."
                            : "Your Teacher has not uploaded any PDFs yet."}
                    </p>
                </div>
            ) : (
                <ul className="divide-y rounded-xl border bg-card shadow">
                    {files.map((file) => (
                        <li key={file.id} className="flex items-center gap-2 pr-3">
                            <Link
                                to={`/courses/${courseId}/files/${file.id}`}
                                className="group flex min-h-14 min-w-0 flex-1 items-center gap-3 rounded-xl p-3 hover:bg-accent focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                            >
                                <span className="grid h-10 w-10 shrink-0 place-items-center rounded-lg bg-muted text-primary">
                                    <FileText className="h-5 w-5" aria-hidden="true" />
                                </span>
                                <span className="min-w-0 flex-1 break-words font-medium">{file.fileName}</span>
                                <span className="shrink-0 text-sm font-medium text-primary group-hover:underline">Open</span>
                            </Link>
                            {isTeacher && (
                                <ConfirmAction
                                    label="Delete"
                                    triggerLabel={`Delete ${file.fileName}`}
                                    tone="quiet"
                                    size="default"
                                    className="h-11 border-transparent bg-transparent shadow-none"
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
        </section>
    );
}

export default CourseMaterials;
