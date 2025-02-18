import React from "react";
import ReactDOM from "react-dom/client";
import {BrowserRouter, Route, Routes} from "react-router-dom";
import App from "./app";
import "./main.css"
import SignIn from "./Templates/sign_in.jsx";
import SignUp from "./Templates/sign_up.jsx";
import Home from "./Templates/home.jsx";
import JoinPage from "./Templates/join.jsx";
import Courses from "./Templates/courses.jsx";
import PDFUpload from "./Templates/PDFUpload.jsx"
import CoursePDFs from "./Templates/CoursePDFs.jsx"

import { ThemeProvider } from "@material-tailwind/react";
import Creation from "./Templates/creation.jsx";

const root = document.getElementById("root");

ReactDOM.createRoot(root).render(
    <React.StrictMode>
        <BrowserRouter>
            <ThemeProvider>
                <Routes>
                        <Route path="/signing" element={<SignIn />} />
                        <Route path="/signup" element={<SignUp />} />
                        <Route path="/" element={<Home />} />
                        <Route path="/creation" element={<Creation />} />
                        <Route path="/join" element={<JoinPage />} />
                        <Route path="/courses" element={<Courses />} />
                        <Route path="/pdfupload" element={<PDFUpload />} />
                        <Route path="/coursepdfs" element={<CoursePDFs />} />

                </Routes>
            </ThemeProvider>
        </BrowserRouter>
    </React.StrictMode>
);
