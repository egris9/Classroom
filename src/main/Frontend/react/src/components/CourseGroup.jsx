import CourseCard from "./CourseCard.jsx";

/** A titled grid of courses. Renders nothing for an empty list. */
export function CourseGroup({ title, courses }) {
    if (courses.length === 0) {
        return null;
    }
    return (
        <section aria-labelledby={`group-${title}`} className="space-y-4">
            <h2 id={`group-${title}`} className="text-heading font-semibold">
                {title}
            </h2>
            <ul className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                {courses.map((course) => (
                    <li key={course.id}>
                        <CourseCard course={course} />
                    </li>
                ))}
            </ul>
        </section>
    );
}

export default CourseGroup;
