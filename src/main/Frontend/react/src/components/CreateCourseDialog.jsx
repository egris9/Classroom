import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "sonner";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { createCourse } from "../api/courses.js";

function CreateCourseForm({ onDone }) {
    const navigate = useNavigate();
    const [values, setValues] = useState({ courseName: "", section: "", subject: "", room: "" });
    const [error, setError] = useState(null);
    const [submitting, setSubmitting] = useState(false);

    const change = (event) => setValues({ ...values, [event.target.name]: event.target.value });

    const submit = async (event) => {
        event.preventDefault();
        setError(null);
        setSubmitting(true);
        try {
            const course = await createCourse({ ...values, room: parseInt(values.room, 10) });
            toast.success(`${course.courseName} created. Upload your first PDF next.`);
            onDone();
            navigate(`/courses/${course.id}`);
        } catch (err) {
            setError(err.message);
            setSubmitting(false);
        }
    };

    return (
        <form onSubmit={submit} className="space-y-4">
            <div className="space-y-2">
                <Label htmlFor="course-name">Course name</Label>
                <Input id="course-name" name="courseName" value={values.courseName} onChange={change} required />
            </div>
            <div className="grid gap-4 sm:grid-cols-2">
                <div className="space-y-2">
                    <Label htmlFor="course-subject">Subject</Label>
                    <Input id="course-subject" name="subject" value={values.subject} onChange={change} required />
                </div>
                <div className="space-y-2">
                    <Label htmlFor="course-section">Section</Label>
                    <Input id="course-section" name="section" value={values.section} onChange={change} required />
                </div>
            </div>
            <div className="space-y-2">
                <Label htmlFor="course-room">Room</Label>
                <Input id="course-room" name="room" type="number" min="1" value={values.room} onChange={change} required />
            </div>
            {error && (
                <Alert variant="destructive">
                    <AlertDescription>{error}</AlertDescription>
                </Alert>
            )}
            <Button type="submit" className="h-11 w-full" disabled={submitting}>
                {submitting ? "Creating..." : "Create course"}
            </Button>
        </form>
    );
}

/** A dialog that creates a course, then opens it. */
export function CreateCourseDialog({ open, onOpenChange }) {
    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent>
                <DialogHeader>
                    <DialogTitle>Create a course</DialogTitle>
                    <DialogDescription>You get an access code to share with your students.</DialogDescription>
                </DialogHeader>
                <CreateCourseForm onDone={() => onOpenChange(false)} />
            </DialogContent>
        </Dialog>
    );
}

export default CreateCourseDialog;
