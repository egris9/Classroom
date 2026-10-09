import { Link } from "react-router-dom";
import { Button } from "@/components/ui/button";
import EmptyArt from "@/components/EmptyArt";
import PageContainer from "@/components/PageContainer";
import { isSignedIn } from "../api/auth.js";

export default function NotFound() {
    return (
        <PageContainer className="flex flex-col items-center gap-4 py-16 text-center sm:py-24">
            <EmptyArt kind="files" />
            <h1 className="text-title font-semibold">Page not found</h1>
            <p className="max-w-md text-muted-foreground">
                The address does not lead anywhere in ClassHub. It may have been typed wrong, or the page may have been removed.
            </p>
            <Button asChild className="h-11 px-6">
                <Link to={isSignedIn() ? "/courses" : "/"}>{isSignedIn() ? "Go to my courses" : "Go to the home page"}</Link>
            </Button>
        </PageContainer>
    );
}
