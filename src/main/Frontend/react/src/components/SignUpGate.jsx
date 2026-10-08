import { Link } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";

/** The two ways past the free try. */
export function GateActions({ className }) {
    return (
        <div className={className}>
            <Button asChild className="h-11 px-4">
                <Link to="/signup">Sign up</Link>
            </Button>
            <Button asChild variant="outline" className="h-11 px-4">
                <Link to="/signin">Sign in</Link>
            </Button>
        </div>
    );
}

/** Shown when a visitor with no account asks for a second use of the AI tools. */
export function SignUpGate({ open, onOpenChange }) {
    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent className="max-w-[calc(100vw-2rem)] rounded-lg sm:max-w-md">
                <DialogHeader>
                    <DialogTitle>That was your free try</DialogTitle>
                    <DialogDescription>
                        Sign up to keep using the AI tools. It is free, and you can also keep your course PDFs in one place.
                    </DialogDescription>
                </DialogHeader>
                <DialogFooter>
                    <GateActions className="flex flex-col-reverse gap-2 sm:flex-row" />
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}

export default SignUpGate;
