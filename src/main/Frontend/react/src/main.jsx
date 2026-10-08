import React from "react";
import ReactDOM from "react-dom/client";
import { BrowserRouter, Route, Routes } from "react-router-dom";
import { ThemeProvider } from "next-themes";
import "./main.css";
import AppShell from "./components/AppShell.jsx";
import RequireAuth from "./RequireAuth.jsx";
import CoursePage from "./pages/CoursePage.jsx";
import FilePage from "./pages/FilePage.jsx";
import Home from "./pages/Home.jsx";
import MyCourses from "./pages/MyCourses.jsx";
import NotFound from "./pages/NotFound.jsx";
import SignIn from "./pages/SignIn.jsx";
import SignUp from "./pages/SignUp.jsx";

const root = document.getElementById("root");

ReactDOM.createRoot(root).render(
    <React.StrictMode>
        <BrowserRouter>
            <ThemeProvider attribute="class" defaultTheme="system" enableSystem>
                <Routes>
                    <Route element={<AppShell />}>
                        <Route path="/" element={<Home />} />
                        <Route path="/signin" element={<SignIn />} />
                        <Route path="/signup" element={<SignUp />} />
                        <Route element={<RequireAuth />}>
                            <Route path="/courses" element={<MyCourses />} />
                            <Route path="/courses/:courseId" element={<CoursePage />} />
                            <Route path="/courses/:courseId/files/:fileId" element={<FilePage />} />
                        </Route>
                        <Route path="*" element={<NotFound />} />
                    </Route>
                </Routes>
            </ThemeProvider>
        </BrowserRouter>
    </React.StrictMode>
);
