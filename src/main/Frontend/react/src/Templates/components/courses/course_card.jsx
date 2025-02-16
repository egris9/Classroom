import React from "react";

const CourseCard = ({ course }) => {
    const { name, teacher } = course;

    // Add your base URL here. This might be different based on your deployment environment.
    const BASE_URL = "http://localhost:8080/api/auth/profile-picture/";  // Update this with your actual base URL

    return (
        <div className="flex flex-col border rounded-xl p-4 md:p-6 max-w-[382px] max-h-[235px] bg-purple-50 border-purple-500">
            <div className="flex items-center gap-x-4">

                <img
                    className="rounded-full size-10"
                    src={teacher && teacher.profilePicture ? `${BASE_URL}${teacher.profilePicture}` : "/default-avatar.png"}
                    alt={teacher && teacher.name ? `${teacher.name}'s Profile` : "Default Profile"}
                />
                <div className="grow">
                    <h3 className="font-medium text-xl text-purple-800">{name}</h3>
                    <p className="text-purple-500">{teacher.name}</p>
                </div>
            </div>
        </div>
    );
};


export default CourseCard;
