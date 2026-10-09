import AuthAside from "@/components/landing/AuthAside";
import { cn } from "@/lib/utils";

/** The shape of the sign-in and sign-up screens: the form on the left, the aside on the right from `lg`, the form alone below. */
export function AuthLayout({ className, children }) {
    return (
        <div className="mx-auto grid w-full max-w-screen-xl gap-10 px-4 py-10 sm:px-6 sm:py-16 lg:grid-cols-2 lg:items-center lg:gap-16 lg:px-8">
            <div className={cn("mx-auto w-full max-w-md lg:mx-0", className)}>{children}</div>
            <AuthAside />
        </div>
    );
}

export default AuthLayout;
