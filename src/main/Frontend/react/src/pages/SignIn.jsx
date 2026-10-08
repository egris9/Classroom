import { useState } from "react";
import { Link, Navigate, useNavigate } from "react-router-dom";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import PasswordInput from "@/components/PasswordInput";
import { isSignedIn, signIn } from "../api/auth.js";

export default function SignIn() {
    const navigate = useNavigate();
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [error, setError] = useState(null);
    const [submitting, setSubmitting] = useState(false);

    if (isSignedIn()) {
        return <Navigate to="/courses" replace />;
    }

    const submit = async (event) => {
        event.preventDefault();
        setError(null);
        setSubmitting(true);
        try {
            await signIn(email, password);
            navigate("/courses");
        } catch (err) {
            setError(err.response?.data?.message || "Sign in failed. Check your email and password.");
            setSubmitting(false);
        }
    };

    return (
        <div className="mx-auto flex w-full max-w-md flex-col px-4 py-12 sm:py-20">
            <Card>
                <CardHeader>
                    <h1 className="text-2xl font-semibold leading-none">Sign in</h1>
                    <CardDescription>Welcome back. Your courses are waiting.</CardDescription>
                </CardHeader>
                <CardContent>
                    <form onSubmit={submit} className="space-y-4">
                        <div className="space-y-2">
                            <Label htmlFor="email">Email</Label>
                            <Input
                                id="email"
                                type="email"
                                autoComplete="email"
                                value={email}
                                onChange={(event) => setEmail(event.target.value)}
                                required
                            />
                        </div>
                        <div className="space-y-2">
                            <Label htmlFor="password">Password</Label>
                            <PasswordInput
                                id="password"
                                autoComplete="current-password"
                                value={password}
                                onChange={(event) => setPassword(event.target.value)}
                                required
                            />
                        </div>
                        {error && (
                            <Alert variant="destructive">
                                <AlertDescription>{error}</AlertDescription>
                            </Alert>
                        )}
                        <Button type="submit" className="h-11 w-full" disabled={submitting}>
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
        </div>
    );
}
