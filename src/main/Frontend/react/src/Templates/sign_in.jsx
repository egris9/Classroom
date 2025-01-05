import { Form } from "./components/signin/form/index.jsx";
import { Typography } from "@material-tailwind/react";
import {signInn} from "../api/auth"; // Import the signIn function

export function SignIn() {

    return (
            <section className="flex items-center justify-center h-screen bg-purple-50 px-4">
                <div className="flex flex-col md:flex-row w-full gap-6 mx-auto max-w-4xl justify-between bg-gray-50 p-6 md:p-10 rounded-2xl">
                    <div className="flex flex-col gap-3 flex-1 items-center md:items-start text-center md:text-left">
                        <Typography variant="h3" color="blue-gray" className="mb-2">
                            Sign In
                        </Typography>
                        <Typography className="mb-8 md:mb-10 text-gray-600 font-normal text-lg">
                            Enter your email and password to sign in
                        </Typography>
                    </div>
                    <div className="flex-1 flex items-center justify-center">
                        <Form signIn={signInn}/>
                    </div>
                </div>
            </section>
    );
}

export default SignIn;

