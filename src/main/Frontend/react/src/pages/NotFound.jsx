import { Link } from "react-router-dom";
import { Button } from "@/components/ui/button";
import PageContainer from "@/components/PageContainer";
import { isSignedIn } from "../api/auth.js";

export default function NotFound() {
    return (
        <PageContainer className="flex flex-col items-center gap-4 py-20 text-center">
            <h1 className="text-3xl font-semibold">Page not found</h1>
            <p className="text-muted-foreground">The address does not lead anywhere in ClassHub.</p>
            <Button asChild className="h-11 px-6">
                <Link to={isSignedIn() ? "/courses" : "/"}>{isSignedIn() ? "Go to my courses" : "Go to the home page"}</Link>
            </Button>
        </PageContainer>
    );
}
