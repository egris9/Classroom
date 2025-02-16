import { useEffect, useState } from 'react';
import { getCourses } from '../../../api/courses';
import CourseCard from './Course_card';

const UserCourses = () => {
    const [createdCourses, setCreatedCourses] = useState([]);
    const [joinedCourses, setJoinedCourses] = useState([]);
    const [error, setError] = useState(null);

    useEffect(() => {
        const fetchCourses = async () => {
            try {
                const token = localStorage.getItem("jwt_token");
                console.log("JWT Token:", token);

                const coursesData = await getCourses("user");
                console.log("Courses Data:", coursesData);

                if (Array.isArray(coursesData)) {
                    const created = coursesData.filter(course => course.type === "created");
                    const joined = coursesData.filter(course => course.type === "joined");

                    setCreatedCourses(created);
                    setJoinedCourses(joined);
                } else {
                    console.error("Unexpected response format:", coursesData);
                    setError("Unexpected response format.");
                }
            } catch (error) {
                console.error("Error fetching user courses:", error);
                setError("Failed to fetch courses. Please try again.");
            }
        };

        fetchCourses();
    }, []);

    if (error) {
        return <div>{error}</div>;
    }

    return (
        <div className="bg-purple-50 min-h-screen">
            <div className="max-w-[90rem] mx-auto px-4 py-10 md:px-10 lg:px-6">
                <div className="text-center mb-16 lg:mb-20">
                    <h2 className="text-2xl md:text-4xl text-purple-400 font-bold md:leading-tight">Your Classes</h2>
                </div>
                {/* Created Courses */}
                <div>
                    <h3 className="text-xl text-purple-800 font-semibold mb-4">Your Created Courses</h3>
                    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
                        {createdCourses.length > 0 ? (
                            createdCourses.map((course) => (
                                <CourseCard key={course.id} course={course} />
                            ))
                        ) : (
                            <p className="text-purple-600">No courses created yet.</p>
                        )}
                    </div>
                </div>
                {/* Joined Courses */}
                <div className="mt-10">
                    <h3 className="text-xl text-purple-800 font-semibold mb-4">Your Joined Courses</h3>
                    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
                        {joinedCourses.length > 0 ? (
                            joinedCourses.map((course) => (
                                <CourseCard key={course.id} course={course} />
                            ))
                        ) : (
                            <p className="text-purple-600">No courses joined yet.</p>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
};

export default UserCourses;
