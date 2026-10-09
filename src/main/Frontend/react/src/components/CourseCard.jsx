import { Link } from "react-router-dom";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent } from "@/components/ui/card";
import CourseCover from "./CourseCover.jsx";
import CourseMeta from "./CourseMeta.jsx";
import UserAvatar from "./UserAvatar.jsx";

/** One course in a list. The whole card opens the course. */
export function CourseCard({ course }) {
    return (
        <Link
            to={`/courses/${course.id}`}
            className="block rounded-xl focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
        >
            <Card className="h-full overflow-hidden transition-shadow hover:shadow-raised">
                <CourseCover course={course} aspect="aspect-video" />
                <CardContent className="flex h-full flex-col gap-4 p-5">
                    <div className="space-y-2">
                        <h3 className="text-heading font-semibold">{course.courseName}</h3>
                        <CourseMeta course={course} />
                    </div>
                    <div className="mt-auto flex items-center justify-between gap-2">
                        <span className="flex min-w-0 items-center gap-2">
                            <UserAvatar name={course.teacher?.name} picture={course.teacher?.picture} className="h-8 w-8" />
                            <span className="truncate text-sm">{course.teacher?.name}</span>
                        </span>
                        <Badge variant="outline">{course.role === "TEACHER" ? "Teacher" : "Student"}</Badge>
                    </div>
                </CardContent>
            </Card>
        </Link>
    );
}

export default CourseCard;
