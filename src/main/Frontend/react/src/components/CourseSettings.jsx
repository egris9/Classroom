import { useNavigate } from "react-router-dom";
import { toast } from "sonner";
import { deleteCourse, leaveCourse } from "../api/courses.js";
import ConfirmAction from "./ConfirmAction.jsx";

/** The details of a course, and the one destructive action each role has. */
export function CourseSettings({ course }) {
    const navigate = useNavigate();

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
        <div className="space-y-8">
            <section aria-labelledby="settings-details" className="space-y-3">
                <h2 id="settings-details" className="text-lg font-semibold">
                    Details
                </h2>
                <dl className="grid gap-4 rounded-lg border bg-card p-4 sm:grid-cols-3">
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
            </section>

            <section aria-labelledby="settings-danger" className="space-y-3">
                <h2 id="settings-danger" className="text-lg font-semibold">
                    {course.role === "TEACHER" ? "Delete this course" : "Leave this course"}
                </h2>
                {course.role === "TEACHER" ? (
                    <>
                        <p className="text-sm text-muted-foreground">
                            The course, its PDFs, their summaries and exercise sets, and every enrolment are removed.
                        </p>
                        <ConfirmAction
                            label="Delete course"
                            size="default"
                            title="Delete this course?"
                            description="The course, its PDFs, their summaries and exercise sets, and every student's enrolment are removed. This cannot be undone."
                            confirmLabel="Delete course"
                            onConfirm={() => finish(deleteCourse, "Course deleted.")}
                        />
                    </>
                ) : (
                    <>
                        <p className="text-sm text-muted-foreground">
                            You lose access to its PDFs and to the results you asked for. You can join again with the access code.
                        </p>
                        <ConfirmAction
                            label="Leave course"
                            size="default"
                            title="Leave this course?"
                            description="You lose access to its PDFs and to the results you asked for. You can join again with the access code."
                            confirmLabel="Leave course"
                            onConfirm={() => finish(leaveCourse, "You left the course.")}
                        />
                    </>
                )}
            </section>
        </div>
    );
}

export default CourseSettings;
