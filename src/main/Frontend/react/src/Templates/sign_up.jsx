import {Form} from "./components/signup/form/index.jsx";
import {Typography} from "@material-tailwind/react";
import { signUp } from "../api/auth"; // Import the signUp function


export function SignUp() {


    return (
        <section className="flex items-center justify-center h-screen bg-purple-50 px-4">
            <div className="flex flex-col w-full gap-6 mx-auto max-w-3xl bg-gray-50 p-6 md:p-10 rounded-2xl">
                <div className="flex flex-col gap-3 items-center text-center">
                    <Typography variant="h3" color="blue-gray" className="">
                        Sign Up
                    </Typography>
                    <Typography className=" text-gray-600 font-normal max-w-md">
                        Sign Up to Manage Your Classes and Stay Connected with Your Learning Community
                    </Typography>
                </div>
                <div className="flex items-center justify-center">
                    <Form signUp={signUp}/>
                </div>
            </div>
        </section>
    );
}

export default SignUp;