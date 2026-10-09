import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader } from "@/components/ui/card";
import { cn } from "@/lib/utils";
import { deleteCourse, leaveCourse, removeCoursePicture, setCoursePicture } from "../api/courses.js";
import ConfirmAction from "./ConfirmAction.jsx";
import CourseCover from "./CourseCover.jsx";
import FileDropzone from "./FileDropzone.jsx";

/** One titled card of the Settings tab. The heading is an h2 so the tab reads as a list of sections. */
function SettingsCard({ id, title, description, danger = false, children }) {
    return (
        <section aria-labelledby={id}>
            <Card className={cn(danger && "border-destructive/50")}>
                <CardHeader className="pb-3">
                    <h2 id={id} className="text-heading font-semibold">
                        {title}
                    </h2>
                    {description && <CardDescription>{description}</CardDescription>}
                </CardHeader>
                <CardContent>{children}</CardContent>
            </Card>
        </section>
    );
}

/** Teacher only: shows the current cover, replaces it with a new picture, or removes it. */
function CoverSection({ course, onCourseChange }) {
    const [file, setFile] = useState(null);
    const [saving, setSaving] = useState(false);

    const save = async () => {
        setSaving(true);
        try {
            const updated = await setCoursePicture(course.id, file);
            onCourseChange({ ...course, ...updated, pictureVersion: Date.now() });
            setFile(null);
            toast.success("Cover picture saved.");
        } catch (err) {
            toast.error(err.message);
        } finally {
            setSaving(false);
        }
    };

    const remove = async () => {
        try {
            await removeCoursePicture(course.id);
            onCourseChange({ ...course, picture: null, pictureVersion: undefined });
            toast.success("Cover picture removed.");
        } catch (err) {
            toast.error(err.message);
        }
    };

    return (
        <SettingsCard
            id="settings-cover"
            title="Cover picture"
            description="Shown on the course list and at the top of this page."
        >
            <div className="max-w-xl space-y-4">
                <CourseCover course={course} aspect="aspect-[3/1]" className="rounded-lg border" />
                <FileDropzone
                    id="settings-cover-file"
                    label={course.picture ? "Replace the picture" : "Add a picture"}
                    accept="image/png,image/jpeg,image/gif,image/webp"
                    maxBytes={2 * 1024 * 1024}
                    hint="JPEG, PNG, GIF or WebP, up to 2 MB"
                    file={file}
                    onFile={setFile}
                />
                <div className="flex flex-wrap gap-2">
                    <Button type="button" className="h-11" disabled={!file || saving} onClick={save}>
                        {saving ? "Saving..." : "Save picture"}
                    </Button>
                    {course.picture && (
                        <ConfirmAction
                            label="Remove picture"
                            size="default"
                            tone="quiet"
                            className="h-11"
                            title="Remove the cover picture?"
                            description="The course goes back to a coloured cover."
                            confirmLabel="Remove picture"
                            onConfirm={remove}
                        />
                    )}
                </div>
            </div>
        </SettingsCard>
    );
}

/** The details of a course, the Teacher's cover, and the one destructive action each role has. */
export function CourseSettings({ course, onCourseChange }) {
    const navigate = useNavigate();
    const isTeacher = course.role === "TEACHER";

    const finish = async (action, done) => {
        try {
            await action(course.id);
            toast.success(done);
            navigate("/courses");
        } catch (err) {
            toast.error(err.message);
        }
    };

    return (
        <div className="max-w-3xl space-y-6">
            <SettingsCard id="settings-details" title="Details">
                <dl className="grid gap-4 sm:grid-cols-3">
                    <div>
                        <dt className="text-sm text-muted-foreground">Subject</dt>
                        <dd className="font-medium">{course.subject}</dd>
                    </div>
                    <div>
                        <dt className="text-sm text-muted-foreground">Section</dt>
                        <dd className="font-medium">{course.section}</dd>
                    </div>
                    <div>
                        <dt className="text-sm text-muted-foreground">Room</dt>
                        <dd className="font-medium">{course.room}</dd>
                    </div>
                </dl>
            </SettingsCard>

            {isTeacher && <CoverSection course={course} onCourseChange={onCourseChange} />}

            <SettingsCard
                id="settings-danger"
                danger
                title={isTeacher ? "Delete this course" : "Leave this course"}
                description={
                    isTeacher
                        ? "The course, its PDFs, their summaries and exercise sets, and every enrolment are removed."
                        : "You lose access to its PDFs and to the results you asked for. You can join again with the access code."
                }
            >
                {isTeacher ? (
                    <ConfirmAction
                        label="Delete course"
                        size="default"
                        className="h-11"
                        title="Delete this course?"
                        description="The course, its PDFs, their summaries and exercise sets, and every student's enrolment are removed. This cannot be undone."
                        confirmLabel="Delete course"
                        onConfirm={() => finish(deleteCourse, "Course deleted.")}
                    />
                ) : (
                    <ConfirmAction
                        label="Leave course"
                        size="default"
                        className="h-11"
                        title="Leave this course?"
                        description="You lose access to its PDFs and to the results you asked for. You can join again with the access code."
                        confirmLabel="Leave course"
                        onConfirm={() => finish(leaveCourse, "You left the course.")}
                    />
                )}
            </SettingsCard>
        </div>
    );
}

export default CourseSettings;
