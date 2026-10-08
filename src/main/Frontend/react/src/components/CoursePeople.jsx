import StudentList from "./StudentList.jsx";
import UserAvatar from "./UserAvatar.jsx";

/** The Teacher of the course, and for the Teacher the Students too. */
export function CoursePeople({ course }) {
    return (
        <div className="space-y-6">
            <section aria-labelledby="people-teacher" className="space-y-2">
                <h2 id="people-teacher" className="text-lg font-semibold">
                    Teacher
                </h2>
                <div className="flex items-center gap-3 rounded-lg border bg-card p-3">
                    <UserAvatar name={course.teacher?.name} picture={course.teacher?.picture} />
                    <span className="font-medium">{course.teacher?.name}</span>
                </div>
            </section>
            <section aria-labelledby="people-students" className="space-y-2">
                <h2 id="people-students" className="text-lg font-semibold">
                    Students
                </h2>
                {course.role === "TEACHER" ? (
                    <StudentList courseId={course.id} />
                ) : (
                    <p className="text-sm text-muted-foreground">Only the Teacher sees who is enrolled.</p>
                )}
            </section>
        </div>
    );
}

export default CoursePeople;
