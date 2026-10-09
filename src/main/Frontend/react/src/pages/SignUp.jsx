import { useRef, useState } from "react";
import { Link, Navigate, useNavigate } from "react-router-dom";
import { Loader2 } from "lucide-react";
import { toast } from "sonner";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import AuthLayout from "@/components/AuthLayout";
import FileDropzone from "@/components/FileDropzone";
import FormField from "@/components/FormField";
import PasswordInput from "@/components/PasswordInput";
import { isSignedIn, signIn, signUp } from "../api/auth.js";
import { EMAIL_PATTERN, showFieldErrors } from "../lib/validation.js";

const FIELDS = ["firstName", "lastName", "email", "password"];

const validate = ({ firstName, lastName, email, password }) => {
    const errors = {};
    if (!firstName.trim()) {
        errors.firstName = "Enter your first name.";
    }
    if (!lastName.trim()) {
        errors.lastName = "Enter your last name.";
    }
    if (!email.trim()) {
        errors.email = "Enter your email.";
    } else if (!EMAIL_PATTERN.test(email.trim())) {
        errors.email = "Enter an email like name@example.com.";
    }
    if (!password) {
        errors.password = "Choose a password.";
    }
    return errors;
};

export default function SignUp() {
    const navigate = useNavigate();
    const [values, setValues] = useState({ firstName: "", lastName: "", email: "", password: "" });
    const [picture, setPicture] = useState(null);
    const [fieldErrors, setFieldErrors] = useState({});
    const [error, setError] = useState(null);
    const [submitting, setSubmitting] = useState(false);
    const inFlight = useRef(false);

    if (isSignedIn()) {
        return <Navigate to="/courses" replace />;
    }

    const change = (event) => {
        const { name, value } = event.target;
        setValues({ ...values, [name]: value });
        setFieldErrors((current) => ({ ...current, [name]: undefined }));
    };

    const submit = async (event) => {
        event.preventDefault();
        // A ref, not the state: two submits in the same tick both see the state as false, and the second would
        // meet EMAIL_TAKEN for the account the first just made.
        if (inFlight.current) {
            return;
        }
        setError(null);
        const errors = validate(values);
        if (Object.keys(errors).length > 0) {
            showFieldErrors(setFieldErrors, errors, FIELDS);
            return;
        }
        setFieldErrors({});
        inFlight.current = true;
        setSubmitting(true);
        const body = new FormData();
        Object.entries(values).forEach(([key, value]) => body.append(key, key === "email" ? value.trim() : value));
        if (picture) {
            body.append("profilePicture", picture);
        }
        try {
            await signUp(body);
        } catch (err) {
            inFlight.current = false;
            setSubmitting(false);
            if (err.response?.data?.code === "EMAIL_TAKEN") {
                // The server's one field-specific answer: show it at the field it is about.
                const taken = { email: "An account with this email already exists. Sign in instead, or use another email." };
                showFieldErrors(setFieldErrors, taken, FIELDS);
            } else {
                setError(err.response?.data?.message || "Sign up failed. Try again.");
            }
            return;
        }
        try {
            await signIn(values.email.trim(), values.password);
            toast.success(`Welcome, ${values.firstName}.`);
            navigate("/courses");
        } catch {
            toast.success("Account created. Sign in to continue.");
            navigate("/signin");
        }
    };

    return (
        <AuthLayout className="lg:max-w-lg">
            <Card>
                <CardHeader>
                    <h1 className="text-title font-semibold">Create your account</h1>
                    <CardDescription>One account works as a Teacher in your courses and a Student in others.</CardDescription>
                </CardHeader>
                <CardContent>
                    <form onSubmit={submit} noValidate aria-busy={submitting} className="space-y-4">
                        <div className="grid gap-4 sm:grid-cols-2">
                            <FormField id="firstName" label="First name" error={fieldErrors.firstName}>
                                {(field) => (
                                    <Input
                                        {...field}
                                        name="firstName"
                                        autoComplete="given-name"
                                        value={values.firstName}
                                        onChange={change}
                                        required
                                    />
                                )}
                            </FormField>
                            <FormField id="lastName" label="Last name" error={fieldErrors.lastName}>
                                {(field) => (
                                    <Input
                                        {...field}
                                        name="lastName"
                                        autoComplete="family-name"
                                        value={values.lastName}
                                        onChange={change}
                                        required
                                    />
                                )}
                            </FormField>
                        </div>
                        <FormField id="email" label="Email" error={fieldErrors.email}>
                            {(field) => (
                                <Input
                                    {...field}
                                    name="email"
                                    type="email"
                                    autoComplete="email"
                                    value={values.email}
                                    onChange={change}
                                    required
                                />
                            )}
                        </FormField>
                        <FormField id="password" label="Password" error={fieldErrors.password}>
                            {(field) => (
                                <PasswordInput
                                    {...field}
                                    name="password"
                                    autoComplete="new-password"
                                    value={values.password}
                                    onChange={change}
                                    required
                                />
                            )}
                        </FormField>
                        <FileDropzone
                            id="profilePicture"
                            label="Profile picture (optional)"
                            accept="image/png,image/jpeg,image/gif,image/webp"
                            maxBytes={2 * 1024 * 1024}
                            hint="JPEG, PNG, GIF or WebP, up to 2 MB"
                            file={picture}
                            onFile={setPicture}
                        />
                        {error && (
                            <Alert variant="destructive">
                                <AlertDescription>{error}</AlertDescription>
                            </Alert>
                        )}
                        <Button type="submit" className="h-11 w-full gap-2" disabled={submitting}>
                            {submitting && <Loader2 className="h-4 w-4 motion-safe:animate-spin" aria-hidden="true" />}
                            {submitting ? "Creating account..." : "Create account"}
                        </Button>
                    </form>
                    <p className="mt-4 text-center text-sm text-muted-foreground">
                        Already have an account?{" "}
                        <Link to="/signin" className="inline-flex min-h-11 items-center font-medium text-primary underline-offset-4 hover:underline">
                            Sign in
                        </Link>
                    </p>
                </CardContent>
            </Card>
        </AuthLayout>
    );
}
