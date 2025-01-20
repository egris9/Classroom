import { Button, Input, Typography } from "@material-tailwind/react";
import { EyeIcon, EyeSlashIcon } from "@heroicons/react/24/solid/index.js";
import { useState } from "react";
import {Link} from "react-router";
import {signUp} from "../../../../api/auth.js";


export function Form({ handleSignUp }) {
    const [formData, setFormData] = useState({
        firstName: "",
        lastName: "",
        email: "",
        password: "",
        profilePicture: null,
    });
    const [error, setError] = useState(null);
    const [passwordShown, setPasswordShown] = useState(false);

    const handleChange = (e) => {
        if (e.target.name === "profilePicture") {
            setFormData({
                ...formData,
                profilePicture: e.target.files[0], // Pour l'image, on prend le fichier sélectionné
            });
        } else {
            setFormData({ ...formData, [e.target.name]: e.target.value });
        }
    };

    const togglePasswordVisibility = () => {
        setPasswordShown((prev) => !prev);
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        const formDataToSend = new FormData();  // FormData for handling files
        formDataToSend.append("firstName", formData.firstName);
        formDataToSend.append("lastName", formData.lastName);
        formDataToSend.append("email", formData.email);
        formDataToSend.append("password", formData.password);
        if (formData.profilePicture) {
            formDataToSend.append("profilePicture", formData.profilePicture);  // Add profile picture
        }

        try {
            const response = await signUp(formDataToSend);  // Use formDataToSend, not formData
            console.log("Sign Up Successful:", response);
            window.location.href = "/signing";  // Redirect to sign-in page
        } catch (err) {
            console.error("Error Details:", err);
            setError(err.message || "Sign Up Failed");
        }
    };


    return (
        <form onSubmit={handleSubmit} className="text-left w-full max-w-2xl">
            <div className="flex md:flex-row gap-6 w-full">
                {/* Left Column: First Name and Last Name */}
                <div className="w-full">
                    {/* First Name Field */}
                    <div className="mb-6 w-full">
                        <Input
                            value={formData.firstName}
                            onChange={handleChange}
                            id="first-name"
                            color="gray"
                            size="lg"
                            type="text"
                            name="firstName"
                            placeholder="First Name"
                            variant="outlined"
                            label="First Name"
                            className="w-full"
                        />
                    </div>

                    {/* Last Name Field */}
                    <div className="mb-6 w-full">
                        <Input
                            value={formData.lastName}
                            onChange={handleChange}
                            id="last-name"
                            color="gray"
                            size="lg"
                            type="text"
                            name="lastName"
                            placeholder="Last Name"
                            variant="outlined"
                            label="Last Name"
                            className="w-full"
                        />
                    </div>
                </div>

                {/* Right Column: Email and Password */}
                <div className="w-full">
                    {/* Email Field */}
                    <div className="mb-6 w-full">
                        <Input
                            value={formData.email}
                            onChange={handleChange}
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

                    {/* Password Field */}
                    <div className="mb-6 w-full">
                        <Input
                            value={formData.password}
                            onChange={handleChange}
                            size="lg"
                            label="Password"
                            variant="outlined"
                            className="w-full placeholder:opacity-100 focus:border-t-primary border-t-blue-gray-200"
                            type={passwordShown ? "text" : "password"}
                            name="password"  // Ajouter l'attribut name pour que handleChange fonctionne correctement
                            icon={
                                <i onClick={togglePasswordVisibility}>
                                    {passwordShown ? (
                                        <EyeIcon className="h-5 w-5"/>
                                    ) : (
                                        <EyeSlashIcon className="h-5 w-5"/>
                                    )}
                                </i>
                            }
                        />
                    </div>
                </div>
            </div>
            {/* Profile Picture Upload */}
            <div className="mb-6 ml-40 w-96">
                <Input
                    onChange={handleChange}
                    id="profilePicture"
                    color="gray"
                    size="lg"
                    type="file"
                    name="profilePicture"
                    variant="outlined"
                    label="Profile Picture"
                    className="w-full"
                />
            </div>
            {/* Error Handling */}
            {error && (
                <Typography color="red" className="text-center mb-4">
                    {error}
                </Typography>
            )}
            {/* Submit Button */}
            <Button color="purple" size="lg" className="mt-6 mx-auto block px-24" type="submit">
                Sign Up
            </Button>
            <Typography variant="small" color="gray" className="!mt-4 text-center font-normal">
                Already have an account?{" "}
                <Link to="/signing" className="font-medium text-gray-900">
                    Sign In
                </Link>
            </Typography>
        </form>


    );
}
