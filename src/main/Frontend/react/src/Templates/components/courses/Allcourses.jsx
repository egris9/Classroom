import React, { useEffect, useState } from "react";
import { getCourses } from "../../../api/courses";
import CourseCard from "./course_card.jsx";

const Allcourses = () => {
    const [courses, setCourses] = useState([]);
    const [error, setError] = useState(null);

    useEffect(() => {
        const fetchCourses = async () => {
            try {
                const data = await getCourses("all");
                setCourses(data);
            } catch (err) {
                setError(err.message);
            }
        };

        fetchCourses();
    }, []);

    if (error) return <div>Error: {error}</div>;

    return (
        <div className="bg-purple-50 min-h-screen">

            <div className="max-w-[90rem] mx-auto px-4 py-10 md:px-10 lg:px-6">
                <div className="text-center mb-16 lg:mb-20">
                    <h2 className="text-2xl md:text-4xl text-purple-400 font-bold md:leading-tight">All Courses</h2>
                </div>
                {/* Course Cards */}
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
                    {courses.map((course) => (
                        <CourseCard key={course.id} course={course} />
                    ))}
                </div>
            </div>
        </div>
    );
};

export default Allcourses;
