import { useState } from "react";
import { Link, NavLink, Outlet, useLocation, useNavigate } from "react-router-dom";
import { ChevronDown, LogOut, Menu, Plus } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
    Sheet,
    SheetContent,
    SheetDescription,
    SheetHeader,
    SheetTitle,
} from "@/components/ui/sheet";
import { Separator } from "@/components/ui/separator";
import { Toaster } from "@/components/ui/sonner";
import { ThemeToggle } from "@/components/ThemeToggle";
import { cn } from "@/lib/utils";
import { clearSession, getFirstName, isSignedIn } from "../api/auth.js";

const linkClass = ({ isActive }) =>
    cn(
        "inline-flex h-11 items-center rounded-md px-3 text-sm font-medium transition-colors hover:bg-accent focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring",
        isActive ? "text-foreground" : "text-muted-foreground",
    );

export function AppShell() {
    const [menuOpen, setMenuOpen] = useState(false);
    const navigate = useNavigate();
    // The shell stays mounted across routes, so read the session on each render.
    useLocation();
    const signedIn = isSignedIn();
    const firstName = getFirstName();

    const signOut = () => {
        clearSession();
        setMenuOpen(false);
        navigate("/signing");
    };

    const memberLinks = (
        <>
            <NavLink to="/courses" className={linkClass} onClick={() => setMenuOpen(false)}>
                Courses
            </NavLink>
            <NavLink to="/tools" className={linkClass} onClick={() => setMenuOpen(false)}>
                Tools
            </NavLink>
        </>
    );

    return (
        <div className="flex min-h-screen flex-col text-foreground">
            <a
                href="#main"
                className="sr-only focus:not-sr-only focus:absolute focus:left-4 focus:top-4 focus:z-50 focus:rounded-md focus:bg-primary focus:px-4 focus:py-2 focus:text-primary-foreground"
            >
                Skip to content
            </a>
            <header className="sticky top-0 z-40 border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/80">
                <nav
                    aria-label="Main"
                    className="mx-auto flex h-16 w-full max-w-screen-xl items-center gap-2 px-4 sm:px-6 lg:px-8"
                >
                    <Link
                        to="/"
                        className="mr-4 rounded-md font-display text-xl font-semibold tracking-tight focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                    >
                        ClassHub
                    </Link>

                    <div className="hidden items-center gap-1 md:flex">{signedIn && memberLinks}</div>

                    <div className="ml-auto flex items-center gap-1">
                        {signedIn && (
                            <DropdownMenu>
                                <DropdownMenuTrigger asChild>
                                    <Button variant="ghost" className="hidden h-11 gap-1 px-3 md:inline-flex">
                                        <Plus className="h-4 w-4" aria-hidden="true" />
                                        Add course
                                    </Button>
                                </DropdownMenuTrigger>
                                <DropdownMenuContent align="end">
                                    <DropdownMenuItem asChild>
                                        <Link to="/creation">Create a course</Link>
                                    </DropdownMenuItem>
                                    <DropdownMenuItem asChild>
                                        <Link to="/join">Join a course</Link>
                                    </DropdownMenuItem>
                                </DropdownMenuContent>
                            </DropdownMenu>
                        )}

                        <ThemeToggle />

                        {signedIn ? (
                            <DropdownMenu>
                                <DropdownMenuTrigger asChild>
                                    <Button variant="outline" className="hidden h-11 gap-1 px-3 md:inline-flex">
                                        {firstName || "Account"}
                                        <ChevronDown className="h-4 w-4" aria-hidden="true" />
                                    </Button>
                                </DropdownMenuTrigger>
                                <DropdownMenuContent align="end">
                                    <DropdownMenuItem onSelect={signOut}>
                                        <LogOut className="mr-2 h-4 w-4" aria-hidden="true" />
                                        Sign out
                                    </DropdownMenuItem>
                                </DropdownMenuContent>
                            </DropdownMenu>
                        ) : (
                            <div className="hidden items-center gap-1 md:flex">
                                <Button asChild variant="ghost" className="h-11 px-4">
                                    <Link to="/signup">Sign up</Link>
                                </Button>
                                <Button asChild className="h-11 px-4">
                                    <Link to="/signing">Sign in</Link>
                                </Button>
                            </div>
                        )}

                        <Button
                            variant="ghost"
                            size="icon"
                            className="h-11 w-11 md:hidden"
                            aria-label="Open menu"
                            onClick={() => setMenuOpen(true)}
                        >
                            <Menu className="h-5 w-5" />
                        </Button>
                    </div>
                </nav>
            </header>

            <Sheet open={menuOpen} onOpenChange={setMenuOpen}>
                <SheetContent side="right" className="w-72">
                    <SheetHeader>
                        <SheetTitle className="font-display">ClassHub</SheetTitle>
                        <SheetDescription>
                            {signedIn ? `Signed in as ${firstName || "you"}` : "Sign in to see your courses"}
                        </SheetDescription>
                    </SheetHeader>
                    <div className="mt-6 flex flex-col gap-1">
                        {signedIn ? (
                            <>
                                {memberLinks}
                                <NavLink to="/creation" className={linkClass} onClick={() => setMenuOpen(false)}>
                                    Create a course
                                </NavLink>
                                <NavLink to="/join" className={linkClass} onClick={() => setMenuOpen(false)}>
                                    Join a course
                                </NavLink>
                                <Separator className="my-3" />
                                <Button variant="outline" className="h-11 justify-start" onClick={signOut}>
                                    <LogOut className="mr-2 h-4 w-4" aria-hidden="true" />
                                    Sign out
                                </Button>
                            </>
                        ) : (
                            <>
                                <Button asChild className="h-11" onClick={() => setMenuOpen(false)}>
                                    <Link to="/signing">Sign in</Link>
                                </Button>
                                <Button asChild variant="outline" className="h-11" onClick={() => setMenuOpen(false)}>
                                    <Link to="/signup">Sign up</Link>
                                </Button>
                            </>
                        )}
                    </div>
                </SheetContent>
            </Sheet>

            <main id="main" className="flex-1">
                <Outlet />
            </main>
            <Toaster richColors />
        </div>
    );
}

export default AppShell;
