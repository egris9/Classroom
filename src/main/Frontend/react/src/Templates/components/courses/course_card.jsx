import React from "react";
import { Link } from "react-router-dom";
import { API_URL } from "../../../api/client.js";

const CourseCard = ({ course }) => {
    const {  name, teacher } = course;

    // Add your base URL here. This might be different based on your deployment environment.
    const BASE_URL = `${API_URL}/api/auth/profile-picture/`;

    return (
        <Link to={`/coursepdfs/${course.id}`} className="block">
            <div className="flex flex-col border rounded-xl p-4 md:p-6 max-w-[382px] max-h-[235px] bg-purple-50 border-purple-500 hover:shadow-lg transition-shadow">
                <div className="flex items-center gap-x-4">
                    <img
                        className="rounded-full size-10"
                        src={teacher && teacher.profilePicture ? `${BASE_URL}${teacher.profilePicture}` : "/default-avatar.png"}
                        alt={teacher && teacher.name ? `${teacher.name}'s Profile` : "Default Profile"}
                    />
                    <div className="grow">
                        <h3 className="font-medium text-xl text-purple-800">{name}</h3>
                        <p className="text-purple-500">By {teacher.name}</p>
                    </div>
                </div>
            </div>
        </Link>
    );
};

export default CourseCard;
