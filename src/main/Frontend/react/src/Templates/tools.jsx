import React from "react";
import { NavBar } from "./components/navbar.jsx";
import {ProfileProvider} from "./components/courses/ProfileContext.jsx";
import FileUpload from "../api/AI_summarization.jsx";
import QuestionGenerator from "../api/AI_test.jsx";

const Tools1 = () => (
    <ProfileProvider>
        <div className="bg-purple-50 min-h-screen ">
            <NavBar />
            <div className="flex flex-col  lg:flex-row gap-6 justify-center items-start pt-16 p-8">
                {/* Left Section - File Upload and Summarization */}
                <div className="w-full lg:w-1/2">
                    <FileUpload />
                </div>

                {/* Right Section - Question Generation */}
                <div className="w-full lg:w-1/2">
                    <QuestionGenerator />
                </div>
            </div>
        </div>
    </ProfileProvider>
);

export default Tools1;