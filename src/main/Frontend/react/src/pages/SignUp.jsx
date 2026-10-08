import { useState } from "react";
import { Link, Navigate, useNavigate } from "react-router-dom";
import { toast } from "sonner";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import PasswordInput from "@/components/PasswordInput";
import { isSignedIn, signIn, signUp } from "../api/auth.js";

export default function SignUp() {
    const navigate = useNavigate();
    const [values, setValues] = useState({ firstName: "", lastName: "", email: "", password: "" });
    const [picture, setPicture] = useState(null);
    const [error, setError] = useState(null);
    const [submitting, setSubmitting] = useState(false);

    if (isSignedIn()) {
        return <Navigate to="/courses" replace />;
    }

    const change = (event) => setValues({ ...values, [event.target.name]: event.target.value });

    const submit = async (event) => {
        event.preventDefault();
        setError(null);
        setSubmitting(true);
        const body = new FormData();
        Object.entries(values).forEach(([key, value]) => body.append(key, value));
        if (picture) {
            body.append("profilePicture", picture);
        }
        try {
            await signUp(body);
        } catch (err) {
            setError(err.response?.data?.message || "Sign up failed. Try again.");
            setSubmitting(false);
            return;
        }
        try {
            await signIn(values.email, values.password);
            toast.success(`Welcome, ${values.firstName}.`);
            navigate("/courses");
        } catch {
            toast.success("Account created. Sign in to continue.");
            navigate("/signin");
        }
    };

    return (
        <div className="mx-auto flex w-full max-w-lg flex-col px-4 py-12 sm:py-20">
            <Card>
                <CardHeader>
                    <h1 className="text-2xl font-semibold leading-none">Create your account</h1>
                    <CardDescription>One account works as a Teacher in your courses and a Student in others.</CardDescription>
                </CardHeader>
                <CardContent>
                    <form onSubmit={submit} className="space-y-4">
                        <div className="grid gap-4 sm:grid-cols-2">
                            <div className="space-y-2">
                                <Label htmlFor="firstName">First name</Label>
                                <Input
                                    id="firstName"
                                    name="firstName"
                                    autoComplete="given-name"
                                    value={values.firstName}
                                    onChange={change}
                                    required
                                />
                            </div>
                            <div className="space-y-2">
                                <Label htmlFor="lastName">Last name</Label>
                                <Input
                                    id="lastName"
                                    name="lastName"
                                    autoComplete="family-name"
                                    value={values.lastName}
                                    onChange={change}
                                    required
                                />
                            </div>
                        </div>
                        <div className="space-y-2">
                            <Label htmlFor="email">Email</Label>
                            <Input
                                id="email"
                                name="email"
                                type="email"
                                autoComplete="email"
                                value={values.email}
                                onChange={change}
                                required
                            />
                        </div>
                        <div className="space-y-2">
                            <Label htmlFor="password">Password</Label>
                            <PasswordInput
                                id="password"
                                name="password"
                                autoComplete="new-password"
                                value={values.password}
                                onChange={change}
                                required
                            />
                        </div>
                        <div className="space-y-2">
                            <Label htmlFor="profilePicture">Profile picture (optional)</Label>
                            <Input
                                id="profilePicture"
                                type="file"
                                accept="image/png,image/jpeg,image/gif,image/webp"
                                onChange={(event) => setPicture(event.target.files[0] ?? null)}
                            />
                        </div>
                        {error && (
                            <Alert variant="destructive">
                                <AlertDescription>{error}</AlertDescription>
                            </Alert>
                        )}
                        <Button type="submit" className="h-11 w-full" disabled={submitting}>
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
        </div>
    );
}
