import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "sonner";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { joinCourseByCode } from "../api/courses.js";

function JoinCourseForm({ onDone }) {
    const navigate = useNavigate();
    const [accessCode, setAccessCode] = useState("");
    const [error, setError] = useState(null);
    const [submitting, setSubmitting] = useState(false);

    const submit = async (event) => {
        event.preventDefault();
        setError(null);
        setSubmitting(true);
        const result = await joinCourseByCode(accessCode.trim());
        if (result.success) {
            toast.success(`You joined ${result.course.courseName}.`);
            onDone();
            navigate(`/courses/${result.course.id}`);
        } else {
            setError(result.error);
            setSubmitting(false);
        }
    };

    return (
        <form onSubmit={submit} className="space-y-4">
            <div className="space-y-2">
                <Label htmlFor="join-code">Access code</Label>
                <Input
                    id="join-code"
                    value={accessCode}
                    onChange={(event) => setAccessCode(event.target.value)}
                    autoComplete="off"
                    className="font-mono"
                    required
                />
            </div>
            {error && (
                <Alert variant="destructive">
                    <AlertDescription>{error}</AlertDescription>
                </Alert>
            )}
            <Button type="submit" className="h-11 w-full" disabled={submitting}>
                {submitting ? "Joining..." : "Join course"}
            </Button>
        </form>
    );
}

/** A dialog that joins a course with the code the Teacher shared, then opens it. */
export function JoinCourseDialog({ open, onOpenChange }) {
    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent>
                <DialogHeader>
                    <DialogTitle>Join with a code</DialogTitle>
                    <DialogDescription>Ask your Teacher for the access code of the course.</DialogDescription>
                </DialogHeader>
                <JoinCourseForm onDone={() => onOpenChange(false)} />
            </DialogContent>
        </Dialog>
    );
}

export default JoinCourseDialog;
