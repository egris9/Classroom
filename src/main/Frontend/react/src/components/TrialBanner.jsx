import { Link } from "react-router-dom";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { GateActions } from "@/components/SignUpGate";

/**
 * Tells a visitor with no account where their free try stands. `trial` is what GET /api/tools/trial answered,
 * or null while it is not known. A signed-in user sees nothing.
 */
export function TrialBanner({ trial }) {
    if (!trial || trial.signedIn) {
        return null;
    }
    // role="status": the shadcn Alert defaults to role="alert", which would interrupt a screen reader on load.
    if (trial.trialAvailable) {
        return (
            <Alert role="status">
                <AlertDescription className="flex flex-wrap items-center justify-between gap-3">
                    <span>You have 1 free try. It is free to sign up for more.</span>
                    <Button asChild variant="outline" className="h-11 px-4">
                        <Link to="/signup">Sign up</Link>
                    </Button>
                </AlertDescription>
            </Alert>
        );
    }
    return (
        <Alert role="status">
            <AlertDescription className="flex flex-wrap items-center justify-between gap-3">
                <span>
                    <strong className="font-semibold">That was your free try.</strong> Sign up to keep using the AI
                    tools. It is free.
                </span>
                <GateActions className="flex flex-wrap gap-2" />
            </AlertDescription>
        </Alert>
    );
}

export default TrialBanner;
