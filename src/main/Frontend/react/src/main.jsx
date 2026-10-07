import React from "react";
import ReactDOM from "react-dom/client";
import {BrowserRouter, Route, Routes} from "react-router-dom";
import "./main.css"
import SignIn from "./Templates/sign_in.jsx";
import SignUp from "./Templates/sign_up.jsx";
import Home from "./Templates/home.jsx";
import JoinPage from "./Templates/join.jsx";
import Courses from "./Templates/courses.jsx";
import PDFUpload from "./Templates/PDFUpload.jsx"
import PDFDisplay from "./Templates/PDFDisplay.jsx"
import Tools1 from "./Templates/tools.jsx"
import Styleguide from "./Templates/styleguide.jsx";
import { ThemeProvider as MaterialThemeProvider } from "@material-tailwind/react";
import { ThemeProvider } from "next-themes";
import Creation from "./Templates/creation.jsx";
import RequireAuth from "./RequireAuth.jsx";
import AppShell from "./components/AppShell.jsx";

const root = document.getElementById("root");

ReactDOM.createRoot(root).render(
    <React.StrictMode>
        <BrowserRouter>
            <ThemeProvider attribute="class" defaultTheme="system" enableSystem>
                <MaterialThemeProvider>
                    <Routes>
                        <Route element={<AppShell />}>
                            <Route path="/signing" element={<SignIn />} />
                            <Route path="/signup" element={<SignUp />} />
                            <Route path="/" element={<Home />} />
                            <Route path="/styleguide" element={<Styleguide />} />
                            <Route element={<RequireAuth />}>
                                <Route path="/creation" element={<Creation />} />
                                <Route path="/join" element={<JoinPage />} />
                                <Route path="/courses" element={<Courses />} />
                                <Route path="/pdfupload/:courseid" element={<PDFUpload />} />
                                <Route path="/coursepdfs/:courseid" element={<PDFDisplay />} />
                                <Route path="/tools" element={<Tools1 />} />
                            </Route>
                        </Route>
                    </Routes>
                </MaterialThemeProvider>
            </ThemeProvider>
        </BrowserRouter>
    </React.StrictMode>
);
