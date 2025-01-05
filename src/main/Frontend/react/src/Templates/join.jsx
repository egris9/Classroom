import React, { useState } from 'react';
import { Button, Input,Typography } from '@material-tailwind/react'; // Assuming you're using Material Tailwind for Typography
import { NavBar } from "./components/navbar.jsx";

export function JoinPage() {
    // State for the input field (code)
    const [joinCode, setJoinCode] = useState("");
    const [error, setError] = useState(null);


    const handleJoinSubmit = (e) => {
        e.preventDefault();

        // Basic validation
        if (!joinCode) {
            setError("All fields are required.");
            return;
        }

        // Handle course creation logic here
        console.log({
            joinCode
        });

        // Reset the form
        setJoinCode("");
    };



    return (
        <section className="min-h-screen bg-purple-50">
            <NavBar/>
            <div className="flex items-center justify-center mt-56">
                <div className="flex flex-col md:flex-row w-full max-w-6xl justify-between bg-gray-50 p-20 rounded-2xl">
                    <div
                        className="flex flex-col gap-3 flex-1 items-center md:items-start text-center md:text-left">
                        <Typography variant="h2" color="blue-gray" className="mb-2 ml-10">
                            Join Course
                        </Typography>
                        <Typography className="mb-8 md:mb-10 text-gray-600 font-normal text-xl">
                            Enter the code to join the course
                        </Typography>
                    </div>
                    {/* Added spacing here */}
                    <form onSubmit={handleJoinSubmit} className="space-y-6 w-full max-w-md ">
                        <div>
                            <Input
                                value={joinCode}
                                onChange={(e) => setJoinCode(e.target.value)}
                                id="joinCode"
                                label="Join Code"
                                size="lg"
                                variant="outlined"
                                color="gray"
                                placeholder="Enter join code"
                                className="w-full"
                            />
                        </div>
                        <Button color="purple" size="lg"  fullWidth type="submit">
                            Join Course
                        </Button>
                        {error && <div className="text-red-500 text-center">{error}</div>}
                    </form>
                </div>
            </div>
        </section>
    );
}

export default JoinPage;
