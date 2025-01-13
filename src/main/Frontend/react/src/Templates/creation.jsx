import { Button, Input, Typography } from "@material-tailwind/react";
import { useState , useCallback  } from "react";
import { NavBar } from "./components/navbar.jsx";
import { createCourse } from "../api/courses.js";
import { useNavigate } from "react-router-dom"; // Import useNavigate

export function Creation() {
    // State for form inputs
    const [courseName, setCourseName] = useState("");
    const [section, setSection] = useState("");
    const [subject, setSubject] = useState("");
    const [room, setRoom] = useState("");
    const [error, setError] = useState(null);
    const [accessCode, setAccessCode] = useState("");
    const [successMessage, setSuccessMessage] = useState("");
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [showRedirectButton, setShowRedirectButton] = useState(false);

    const navigate = useNavigate(); // Initialize useNavigate

    const resetForm = () => {
        setCourseName("");
        setSection("");
        setSubject("");
        setRoom("");
    };

    const handleSubmit = useCallback(
        async (event) => {
            event.preventDefault();
            setError(null);
            setSuccessMessage("");
            setAccessCode(""); // Réinitialise le code d'accès
            setIsSubmitting(true);

            if (!courseName.trim() || !section.trim() || !subject.trim() || !room.trim()) {
                setError("All fields are required");
                setIsSubmitting(false);
                return;
            }

            try {
                const courseData = { courseName, section, subject, room: parseInt(room, 10) }; // Conversion room en entier
                const response = await createCourse(courseData);

                if (response) {
                    // Ajoutez un log pour inspecter la réponse de l'API
                    console.log("Réponse API:", response);
                    setAccessCode(response.accessCode); // Mettre à jour accessCode avec la réponse de l'API
                    setSuccessMessage("The course has been successfully created !");
                    resetForm()
                    // Redirection vers "/"
                    // Afficher le bouton de redirection immédiatement après la création du cours
                    setShowRedirectButton(true);
                }
            } catch (error) {
                setError(error.message);
            } finally {
                setIsSubmitting(false);
            }
        },
        [courseName, section, subject, room]
    );

    return (
        <section className="min-h-screen bg-purple-50">
            <NavBar/>
            <div className="flex items-center justify-center mt-48 px-4">
                <div
                    className="flex flex-col md:flex-row w-full gap-6 mx-auto max-w-6xl justify-between bg-gray-50 p-6 md:p-10 rounded-2xl">
                    <div
                        className="flex flex-col gap-3 flex-1 items-center md:items-start text-center md:text-left mt-20">
                        <Typography variant="h2" color="blue-gray" className=" ml-24">
                            Create Course
                        </Typography>
                        <Typography className="mb-8 md:mb-10 text-gray-600 font-normal text-xl ml-20">
                            Enter your info to create a course
                        </Typography>
                    </div>
                    <form onSubmit={handleSubmit} className="space-y-6 w-full max-w-md">
                        <div>
                            <Input
                                value={courseName}
                                onChange={(e) => setCourseName(e.target.value)}
                                id="courseName"
                                label="Course Name"
                                size="lg"
                                variant="outlined"
                                color="gray"
                                placeholder="Enter course name"
                                required
                            />
                        </div>
                        <div>
                            <Input
                                value={section}
                                onChange={(e) => setSection(e.target.value)}
                                id="section"
                                label="Section"
                                size="lg"
                                variant="outlined"
                                color="gray"
                                placeholder="Enter section"
                                required
                            />
                        </div>
                        <div>
                            <Input
                                value={subject}
                                onChange={(e) => setSubject(e.target.value)}
                                id="subject"
                                label="Subject"
                                size="lg"
                                variant="outlined"
                                color="gray"
                                placeholder="Enter subject"
                                required
                            />
                        </div>
                        <div>
                            <Input
                                value={room}
                                onChange={(e) => setRoom(e.target.value)}
                                id="room"
                                label="Room"
                                size="lg"
                                variant="outlined"
                                color="gray"
                                placeholder="Enter room"
                                type="number"
                                min="1"
                                required
                            />
                        </div>
                        <Button
                            color="purple"
                            size="lg"
                            className="mt-6"
                            fullWidth
                            type="submit"
                            disabled={isSubmitting}
                        >
                            {isSubmitting ? "Creating..." : "Create Course"}
                        </Button>
                        {error && <div className="text-red-500 mt-4 text-center">{error}</div>}
                        {successMessage && (
                            <div className="text-green-500 mt-4 text-center">
                                {successMessage}
                                {accessCode && (
                                    <div>
                                        <p className="text-red-900 mt-2">Remember your Code "{accessCode}"</p>
                                    </div>
                                )}
                            </div>
                        )}
                        {/* Bouton de redirection affiché immédiatement après la création du cours */}
                        {showRedirectButton && (
                            <div className="mt-2 text-center">
                                <Button
                                    color="purple"
                                    size="lg"
                                    onClick={() => navigate("/courses")}
                                >
                                    Go to Courses
                                </Button>
                            </div>
                        )}
                    </form>
                </div>
            </div>
        </section>

    );
}


export default Creation;
