import { NavBar } from "./components/navbar.jsx";
import {Button, Typography} from "@material-tailwind/react";
import { motion } from "motion/react"
import ImClass from "../static/imgs/AIgen.png";



export function Home() {
    const scrollToSection = (sectionId) => {
        const section = document.getElementById(sectionId);
        if (section) {
            window.scrollTo({
                top: section.offsetTop - 150,  // Adjust this value to control the scroll offset
                behavior: "smooth",
            });
        }
    };
    const box = {
        width: "100%", // Adjust width to fill the container
        height: 150, // Adjust the height as needed
        borderRadius: 5,
    };
    return(
        <div className="bg-purple-50 min-h-screen">
            {/* Navbar at the top */}
            <NavBar/>
            {/* Welcome Section */}
            <div
                className="grid grid-cols-5 grid-rows-[500px,1Fr] w-full max-w-[90%] mx-auto justify-center mt-14 gap-8">
                <motion.div
                    initial={{opacity: 0, y: 50}}
                    animate={{opacity: 1, y: 0}}
                    transition={{duration: 1, ease: "easeInOut"}}
                    className="text-center p-10 bg-white shadow-lg rounded-xl max-w-4xl w-full col-span-2 row-span-2 flex flex-col justify-center"
                >
                    {/* Welcome Header */}
                    <Typography
                        variant="h2"
                        className="text-purple-500 font-bold mb-4"
                    >
                        Welcome to ClassHub!
                    </Typography>

                    {/* Welcome Message */}
                    <Typography className="text-gray-600 mb-6 text-lg">
                        Your one-stop platform for managing and organizing your
                        classroom activities. Let's make learning fun and
                        engaging!
                    </Typography>

                    {/* Navigation Buttons */}
                    <div className="flex justify-center gap-4">
                        <Button
                            variant="gradient"
                            color="purple"
                            size="lg"
                            className="rounded-md"
                        >
                            Get Started
                        </Button>
                        <Button
                            variant="outlined"
                            color="purple"
                            size="lg"
                            className="rounded-md"
                            onClick={() => scrollToSection("why-choose")}
                        >
                            Learn More
                        </Button>
                    </div>
                </motion.div>
                {/* Image Box */}
                <motion.div
                    initial={{opacity: 0, x: 50}}
                    animate={{opacity: 1, x: 0}}
                    transition={{duration: 1, ease: "easeInOut"}}
                    className="col-span-3 row-span-2 flex items-center justify-center"
                >
                    <img
                        src={ImClass}
                        alt="Classroom illustration"
                        className="w-[930px] h-[532px] rounded-xl shadow-lg"
                    />
                </motion.div>
                {/* Additional Image Under the Existing Two Sections
                <motion.div
                    initial={{opacity: 0, y: 50}}
                    animate={{opacity: 1, y: 0}}
                    transition={{duration: 1, ease: "easeInOut"}}
                    className="col-span-5 flex items-center justify-center "
                >
                    <img
                        src={ImClass2} // Replace with the path to your second image
                        alt="Second Illustration"
                        className="w-[1557px] h-[350px] rounded-xl shadow-lg"
                    />
                </motion.div>
                */}
            </div>
            <div className="mt-10 flex flex-col items-center">
                <motion.div
                    initial={{opacity: 0, y: 50}}
                    animate={{opacity: 1, y: 0}}
                    transition={{duration: 1, ease: "easeInOut"}}
                    className="text-center p-10 bg-white shadow-lg rounded-xl w-[1057px] h-[400px] flex flex-col justify-center"
                >
                    {/* Welcome Header */}
                    <Typography
                        variant="h2"
                        className="text-purple-500 font-bold mb-4"
                    >
                        What is ClassHub?
                    </Typography>

                    {/* Welcome Message */}
                    <Typography className="text-gray-600 text-lg justify-center">
                        ClassHub is a user-friendly platform designed to simplify classroom management and foster collaborative learning.
                        It offers tools for lesson planning, progress tracking, resource sharing, and real-time discussions, making education efficient and engaging.
                        By enhancing communication among educators, students, and parents, ClassHub creates a supportive learning environment.
                        <br/>
                        <br/>
                        It also provides analytics to identify student strengths and areas for improvement, ensuring no one is left behind.
                        With features like multimedia sharing, interactive quizzes, and gamified learning, ClassHub keeps students motivated and inspired.
                        Join ClassHub today to transform classroom management and embrace the future of education!
                    </Typography>
                </motion.div>
            </div>
            <div id="why-choose" className="mt-10">
                <motion.div
                    initial={{opacity: 0, y: -20}}
                    animate={{opacity: 1, y: 0}}
                    transition={{duration: 0.8, ease: "easeInOut"}}
                >
                    <Typography
                        variant="h3"
                        className="text-center text-purple-500 font-bold mb-8"
                    >
                        Why Choose ClassHub?
                    </Typography>
                </motion.div>
                <div className="grid grid-cols-3 gap-6 mx-auto max-w-[90%]">
                    <motion.div
                        initial={{opacity: 0, y: 50}}
                        animate={{opacity: 1, y: 0}}
                        transition={{duration: 0.8, ease: "easeInOut"}}
                        whileHover={{scale: 1.05}}  // Reduced scale for hover
                        whileTap={{scale: 0.95}}    // Reduced scale for tap
                        className="p-6 bg-white shadow-md rounded-lg text-center"
                        style={box}
                    >
                        <Typography variant="h5" className="font-bold mb-2">Lesson Planning</Typography>
                        <Typography className="text-gray-600 text-md">
                            Plan and organize lessons efficiently with a user-friendly interface.
                        </Typography>
                    </motion.div>

                    <motion.div
                        initial={{opacity: 0, y: 50}}
                        animate={{opacity: 1, y: 0}}
                        transition={{duration: 0.8, ease: "easeInOut"}}
                        whileHover={{scale: 1.05}}  // Reduced scale for hover
                        whileTap={{scale: 0.95}}    // Reduced scale for tap
                        className="p-6 bg-white shadow-md rounded-lg text-center"
                        style={box}
                    >
                        <Typography variant="h5" className="font-bold mb-2">Progress Tracking</Typography>
                        <Typography className="text-gray-600 text-md">
                            Monitor student performance with detailed analytics and insights.
                        </Typography>
                    </motion.div>

                    <motion.div
                        initial={{opacity: 0, y: 50}}
                        animate={{opacity: 1, y: 0}}
                        transition={{duration: 0.8, ease: "easeInOut"}}
                        whileHover={{scale: 1.05}}  // Reduced scale for hover
                        whileTap={{scale: 0.95}}    // Reduced scale for tap
                        className="p-6 bg-white shadow-md rounded-lg text-center"
                        style={box}
                    >
                        <Typography variant="h5" className="font-bold mb-2">Collaborative Learning</Typography>
                        <Typography className="text-gray-600 text-md">
                            Facilitate real-time communication between students, teachers, and parents.
                        </Typography>
                    </motion.div>
                </div>
            </div>

            <div id="faq" className="mt-8">
                <motion.div
                    initial={{opacity: 0, y: -20}}
                    animate={{opacity: 1, y: 0}}
                    transition={{duration: 1, ease: "easeInOut"}}
                >
                    <Typography
                        variant="h3"
                        className="text-center text-purple-500 font-bold mb-8"
                    >
                        Frequently Asked Questions
                    </Typography>
                </motion.div>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-6 max-w-[90%] mx-auto">
                    <motion.div
                        initial={{opacity: 0, scale: 0.8}}  // Reduced scale to 0.8
                        animate={{opacity: 1, scale: 1}}  // Final scale to 1
                        transition={{
                            duration: 0.6,  // Slower transition
                            scale: {type: "spring", stiffness: 100, damping: 20},  // Adjusted spring settings for smoother scaling
                        }}
                        className="p-4 bg-white shadow-md rounded-lg"
                    >
                        <Typography variant="h6" className="font-bold mb-2">
                            Is ClassHub free to use?
                        </Typography>
                        <Typography className="text-gray-600">
                            Yes! ClassHub offers a free plan with essential features. Additional premium features are
                            available through our paid plans.
                        </Typography>
                    </motion.div>

                    <motion.div
                        initial={{opacity: 0, scale: 0.8}}  // Reduced scale to 0.8
                        animate={{opacity: 1, scale: 1}}  // Final scale to 1
                        transition={{
                            duration: 0.6,  // Slower transition
                            scale: {type: "spring", stiffness: 100, damping: 20},  // Adjusted spring settings for smoother scaling
                        }}
                        className="p-4 bg-white shadow-md rounded-lg"
                    >
                        <Typography variant="h6" className="font-bold mb-2">
                            Can parents access ClassHub?
                        </Typography>
                        <Typography className="text-gray-600">
                            Absolutely! ClassHub includes a parent portal to stay updated on their child's progress.
                        </Typography>
                    </motion.div>

                    <motion.div
                        initial={{opacity: 0, scale: 0.8}}  // Reduced scale to 0.8
                        animate={{opacity: 1, scale: 1}}  // Final scale to 1
                        transition={{
                            duration: 0.6,  // Slower transition
                            scale: {type: "spring", stiffness: 100, damping: 20},  // Adjusted spring settings for smoother scaling
                        }}
                        className="p-4 bg-white shadow-md rounded-lg"
                    >
                        <Typography variant="h6" className="font-bold mb-2">
                            What devices does ClassHub support?
                        </Typography>
                        <Typography className="text-gray-600">
                            ClassHub is accessible on desktops, tablets, and smartphones, ensuring flexibility for
                            users.
                        </Typography>
                    </motion.div>

                    <motion.div
                        initial={{opacity: 0, scale: 0.8}}  // Reduced scale to 0.8
                        animate={{opacity: 1, scale: 1}}  // Final scale to 1
                        transition={{
                            duration: 0.6,  // Slower transition
                            scale: {type: "spring", stiffness: 100, damping: 20},  // Adjusted spring settings for smoother scaling
                        }}
                        className="p-4 bg-white shadow-md rounded-lg"
                    >
                        <Typography variant="h6" className="font-bold mb-2">
                            How secure is my data on ClassHub?
                        </Typography>
                        <Typography className="text-gray-600">
                            We take security seriously! ClassHub employs robust encryption and data protection measures
                            to safeguard your information.
                        </Typography>
                    </motion.div>

                    <motion.div
                        initial={{opacity: 0, scale: 0.8}}  // Reduced scale to 0.8
                        animate={{opacity: 1, scale: 1}}  // Final scale to 1
                        transition={{
                            duration: 0.6,  // Slower transition
                            scale: {type: "spring", stiffness: 100, damping: 20},  // Adjusted spring settings for smoother scaling
                        }}
                        className="p-4 bg-white shadow-md rounded-lg"
                    >
                        <Typography variant="h6" className="font-bold mb-2">
                            Can I customize ClassHub for my needs?
                        </Typography>
                        <Typography className="text-gray-600">
                            Yes, ClassHub offers customization options for educators and institutions to suit their
                            unique requirements.
                        </Typography>
                    </motion.div>

                    <motion.div
                        initial={{opacity: 0, scale: 0.8}}  // Reduced scale to 0.8
                        animate={{opacity: 1, scale: 1}}  // Final scale to 1
                        transition={{
                            duration: 0.6,  // Slower transition
                            scale: {type: "spring", stiffness: 100, damping: 20},  // Adjusted spring settings for smoother scaling
                        }}
                        className="p-4 bg-white shadow-md rounded-lg"
                    >
                        <Typography variant="h6" className="font-bold mb-2">
                            What support is available for users?
                        </Typography>
                        <Typography className="text-gray-600">
                            ClassHub provides 24/7 customer support, a comprehensive help center, and onboarding
                            tutorials for new users.
                        </Typography>
                    </motion.div>
                </div>
            </div>

            <footer className="bg-purple-500 text-white py-8 mt-14">
                <div className="max-w-screen-xl mx-auto text-center">
                    <Typography className="font-bold text-2xl mb-4">
                        Stay Connected with ClassHub
                    </Typography>
                    <Typography className="text-sm text-gray-200">
                        &copy; {new Date().getFullYear()} ClassHub. All Rights Reserved.
                    </Typography>
                </div>
            </footer>


        </div>


    );
}

export default Home;