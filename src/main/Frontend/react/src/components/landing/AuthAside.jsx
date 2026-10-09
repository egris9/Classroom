import PdfNotesMockup from "@/components/landing/PdfNotesMockup";
import { cn } from "@/lib/utils";

/**
 * What the account is for, beside the sign-in and sign-up forms: the landing page's drawing of a PDF and the
 * summary written from it. It repeats nothing the form needs, so it is hidden below `lg`.
 */
export function AuthAside({ className }) {
    return (
        <aside className={cn("hidden space-y-6 lg:block", className)}>
            <p className="max-w-md font-display text-heading font-semibold">
                Upload a PDF and get a summary and exercises written from it, shared with the whole course.
            </p>
            <PdfNotesMockup />
        </aside>
    );
}

export default AuthAside;
