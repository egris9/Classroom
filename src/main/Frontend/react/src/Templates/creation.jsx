import { Button, Input, Typography } from "@material-tailwind/react";
import { useState } from "react";
import { NavBar } from "./components/navbar.jsx";

export function Creation() {
    // State for form inputs
    const [courseName, setCourseName] = useState("");
    const [section, setSection] = useState("");
    const [subject, setSubject] = useState("");
    const [room, setRoom] = useState("");
    const [error, setError] = useState(null);

    const handleSubmit = (e) => {
        e.preventDefault();

        // Basic validation
        if (!courseName || !section || !subject || !room) {
            setError("All fields are required.");
            return;
        }

        // Handle course creation logic here
        console.log({
            courseName,
            section,
            subject,
            room,
        });

        // Reset the form
        setCourseName("");
        setSection("");
        setSubject("");
        setRoom("");
        setError(null);
    };

    return (
        <section className="min-h-screen bg-purple-50">
            <NavBar/> {/* Added NavBar component */}
            <div className="flex items-center justify-center mt-48 px-4">
                <div className="flex flex-col md:flex-row w-full gap-6 mx-auto max-w-6xl justify-between bg-gray-50 p-6 md:p-10 rounded-2xl">
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
                                className="w-full"
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
                                className="w-full"
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
                                className="w-full"
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
                                className="w-full"
                            />
                        </div>
                        <Button color="purple" size="lg" className="mt-6" fullWidth type="submit">
                            Create Course
                        </Button>
                        {error && <div className="text-red-500 mt-4 text-center">{error}</div>}
                    </form>
                </div>
            </div>
        </section>

    );
}


export default Creation;
