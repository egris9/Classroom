import React from "react";
import { NavBar } from "./components/navbar.jsx";
import {ProfileProvider} from "./components/courses/ProfileContext.jsx";
import UserCourses from "./components/courses/UserCourses.jsx";

const CoursesApp = () => (
    <ProfileProvider>
        <div className="">
            <NavBar />
            <UserCourses />
        </div>
    </ProfileProvider>
);

export default CoursesApp;
