import { useState } from "react";
import { Link, Navigate, useNavigate } from "react-router-dom";
import { Loader2 } from "lucide-react";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import AuthLayout from "@/components/AuthLayout";
import FormField from "@/components/FormField";
import PasswordInput from "@/components/PasswordInput";
import { isSignedIn, signIn } from "../api/auth.js";
import { EMAIL_PATTERN, focusFirstError } from "../lib/validation.js";

const FIELDS = ["email", "password"];

const validate = ({ email, password }) => {
    const errors = {};
    if (!email.trim()) {
        errors.email = "Enter your email.";
    } else if (!EMAIL_PATTERN.test(email.trim())) {
        errors.email = "Enter an email like name@example.com.";
    }
    if (!password) {
        errors.password = "Enter your password.";
    }
    return errors;
};

export default function SignIn() {
    const navigate = useNavigate();
    const [values, setValues] = useState({ email: "", password: "" });
    const [fieldErrors, setFieldErrors] = useState({});
    const [error, setError] = useState(null);
    const [submitting, setSubmitting] = useState(false);

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
        setError(null);
        const errors = validate(values);
        setFieldErrors(errors);
        if (Object.keys(errors).length > 0) {
            focusFirstError(errors, FIELDS);
            return;
        }
        setSubmitting(true);
        try {
            await signIn(values.email.trim(), values.password);
            navigate("/courses");
        } catch (err) {
            setError(err.response?.data?.message || "Sign in failed. Check your email and password.");
            setSubmitting(false);
        }
    };

    return (
        <AuthLayout>
            <Card>
                <CardHeader>
                    <h1 className="text-title font-semibold">Sign in</h1>
                    <CardDescription>Welcome back. Your courses are waiting.</CardDescription>
                </CardHeader>
                <CardContent>
                    <form onSubmit={submit} noValidate aria-busy={submitting} className="space-y-4">
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
                                    autoComplete="current-password"
                                    value={values.password}
                                    onChange={change}
                                    required
                                />
                            )}
                        </FormField>
                        {error && (
                            <Alert variant="destructive">
                                <AlertDescription>{error}</AlertDescription>
                            </Alert>
                        )}
                        <Button type="submit" className="h-11 w-full gap-2" disabled={submitting}>
                            {submitting && <Loader2 className="h-4 w-4 motion-safe:animate-spin" aria-hidden="true" />}
                            {submitting ? "Signing in..." : "Sign in"}
                        </Button>
                    </form>
                    <p className="mt-4 text-center text-sm text-muted-foreground">
                        New here?{" "}
                        <Link to="/signup" className="inline-flex min-h-11 items-center font-medium text-primary underline-offset-4 hover:underline">
                            Create an account
                        </Link>
                    </p>
                </CardContent>
            </Card>
        </AuthLayout>
    );
}
