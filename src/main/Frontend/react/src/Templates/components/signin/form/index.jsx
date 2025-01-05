import { Button, Input, Typography } from "@material-tailwind/react";
import { EyeIcon, EyeSlashIcon } from "@heroicons/react/24/solid/index.js";
import { useState } from "react";
import {Link, useNavigate} from "react-router";
import {signInn} from "../../../../api/auth.js";

export function Form({ signIn }) {
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [error, setError] = useState(null);
    const [passwordShown, setPasswordShown] = useState(false);
    const navigate = useNavigate();

    const togglePasswordVisibility = () => {
        setPasswordShown(!passwordShown);
    };

    const handleSubmit = async (e) => {
        e.preventDefault();

        // Basic validation
        if (!email || !password) {
            setError("Please fill out all fields.");
            return;
        }

        try {
            const response = await signInn(email, password); // API call
            console.log("Sign In Successful:", response);
            navigate("/"); // Redirect to home page
        } catch (err) {
            console.error("Sign In Error:", err);
            setError(err.response?.data?.message || "Sign In Failed. Please check your credentials.");
        }
    };


    return (
        <form onSubmit={handleSubmit} className="text-left w-full max-w-md">
            <div className="mb-6">
                <Input
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    id="email"
                    color="gray"
                    size="lg"
                    type="email"
                    name="email"
                    placeholder="name@mail.com"
                    variant="outlined"
                    label="Email"
                    className="w-full"
                />
            </div>
            <div className="mb-6">
                <Input
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    size="lg"
                    label="Password"
                    variant="outlined"
                    className="w-full placeholder:opacity-100 focus:border-t-primary border-t-blue-gray-200"
                    type={passwordShown ? "text" : "password"}
                    icon={
                        <i onClick={togglePasswordVisibility}>
                            {passwordShown ? (
                                <EyeIcon className="h-5 w-5" />
                            ) : (
                                <EyeSlashIcon className="h-5 w-5" />
                            )}
                        </i>
                    }
                />
            </div>
            <Button color="purple" size="lg" className="mt-6" fullWidth type="submit">
                Sign In
            </Button>
            {error && <div className="text-red-500 mt-4 text-center">{error}</div>}
            <Typography
                variant="small"
                color="gray"
                className="!mt-4 text-center font-normal"
            >
                Not registered?{" "}
                <Link to="/signup" className="font-medium text-gray-900">
                    Create account
                </Link>
            </Typography>
        </form>
    );
}
