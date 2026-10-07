import { cn } from "@/lib/utils";

/** Centred column with the shell's side gutters. Pages that opt in use it as their root. */
export function PageContainer({ className, ...props }) {
    return (
        <div
            className={cn("mx-auto w-full max-w-screen-xl px-4 py-8 sm:px-6 lg:px-8", className)}
            {...props}
        />
    );
}

export default PageContainer;
