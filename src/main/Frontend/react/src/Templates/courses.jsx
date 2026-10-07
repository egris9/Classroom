import React from "react";
import {ProfileProvider} from "./components/courses/ProfileContext.jsx";
import UserCourses from "./components/courses/UserCourses.jsx";


const CoursesApp = () => (
    <ProfileProvider>
        <div className="">
            <UserCourses />

        </div>
    </ProfileProvider>
);

export default CoursesApp;
