import { useState } from "react";
import { Link, NavLink, Outlet, useLocation, useNavigate } from "react-router-dom";
import { BookOpen, LogOut, Menu } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuLabel,
    DropdownMenuSeparator,
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
import { clearSession, getUser, isSignedIn } from "../api/auth.js";
import BrandMark from "./BrandMark.jsx";
import UserAvatar from "./UserAvatar.jsx";

// The current page gets a bar under its label as well as the darker text, so it does not rely on colour alone.
const linkClass = ({ isActive }) =>
    cn(
        "relative inline-flex h-11 items-center rounded-md px-3 text-sm font-medium transition-colors hover:bg-accent focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring",
        isActive
            ? "text-foreground after:absolute after:inset-x-3 after:bottom-1 after:h-0.5 after:rounded-full after:bg-primary"
            : "text-muted-foreground hover:text-foreground",
    );

export function AppShell() {
    const [menuOpen, setMenuOpen] = useState(false);
    const navigate = useNavigate();
    // The shell stays mounted across routes, so read the session on each render.
    useLocation();
    const signedIn = isSignedIn();
    const user = getUser();
    const fullName = user ? `${user.firstName} ${user.lastName}` : "";

    const signOut = () => {
        clearSession();
        setMenuOpen(false);
        navigate("/signin");
    };

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
                        className="mr-4 flex min-h-11 items-center gap-2 rounded-md font-display text-xl font-semibold tracking-tight focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                    >
                        <BrandMark />
                        ClassHub
                    </Link>

                    <div className="hidden items-center gap-1 md:flex">
                        {signedIn && (
                            <NavLink to="/courses" className={linkClass}>
                                My courses
                            </NavLink>
                        )}
                        <NavLink to="/tools" className={linkClass}>
                            AI tools
                        </NavLink>
                    </div>

                    <div className="ml-auto flex items-center gap-1">
                        <ThemeToggle />

                        {signedIn ? (
                            <DropdownMenu>
                                <DropdownMenuTrigger asChild>
                                    <Button
                                        variant="ghost"
                                        className="hidden h-11 w-11 rounded-full p-0 md:inline-flex"
                                        aria-label={`Account menu for ${fullName || "you"}`}
                                    >
                                        <UserAvatar name={fullName} picture={user?.picture} className="h-9 w-9" />
                                    </Button>
                                </DropdownMenuTrigger>
                                <DropdownMenuContent align="end" className="w-56">
                                    <DropdownMenuLabel className="font-normal">
                                        <span className="block text-xs text-muted-foreground">Signed in as</span>
                                        <span className="block truncate text-sm font-medium">{fullName || "you"}</span>
                                    </DropdownMenuLabel>
                                    <DropdownMenuSeparator />
                                    <DropdownMenuItem asChild className="min-h-11">
                                        <Link to="/courses">
                                            <BookOpen className="mr-2 h-4 w-4" aria-hidden="true" />
                                            My courses
                                        </Link>
                                    </DropdownMenuItem>
                                    <DropdownMenuItem className="min-h-11" onSelect={signOut}>
                                        <LogOut className="mr-2 h-4 w-4" aria-hidden="true" />
                                        Sign out
                                    </DropdownMenuItem>
                                </DropdownMenuContent>
                            </DropdownMenu>
                        ) : (
                            <div className="hidden items-center gap-1 md:flex">
                                <Button asChild variant="ghost" className="h-11 px-4">
                                    <Link to="/signin">Sign in</Link>
                                </Button>
                                <Button asChild className="h-11 px-4">
                                    <Link to="/signup">Sign up</Link>
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
                            {signedIn ? `Signed in as ${user?.firstName || "you"}` : "Sign in to see your courses"}
                        </SheetDescription>
                    </SheetHeader>
                    <nav aria-label="Menu" className="mt-6 flex flex-col gap-1">
                        {signedIn ? (
                            <>
                                <NavLink to="/courses" className={linkClass} onClick={() => setMenuOpen(false)}>
                                    My courses
                                </NavLink>
                                <NavLink to="/tools" className={linkClass} onClick={() => setMenuOpen(false)}>
                                    AI tools
                                </NavLink>
                                <Separator className="my-3" />
                                <Button variant="outline" className="h-11 justify-start" onClick={signOut}>
                                    <LogOut className="mr-2 h-4 w-4" aria-hidden="true" />
                                    Sign out
                                </Button>
                            </>
                        ) : (
                            <>
                                <NavLink to="/tools" className={linkClass} onClick={() => setMenuOpen(false)}>
                                    AI tools
                                </NavLink>
                                <Separator className="my-3" />
                                <Button asChild className="h-11" onClick={() => setMenuOpen(false)}>
                                    <Link to="/signup">Sign up</Link>
                                </Button>
                                <Button asChild variant="outline" className="h-11" onClick={() => setMenuOpen(false)}>
                                    <Link to="/signin">Sign in</Link>
                                </Button>
                            </>
                        )}
                    </nav>
                </SheetContent>
            </Sheet>

            <main id="main" className="flex-1 scroll-mt-16">
                <Outlet />
            </main>
            <footer className="border-t py-6">
                <p className="mx-auto w-full max-w-screen-xl px-4 text-sm text-muted-foreground sm:px-6 lg:px-8">
                    &copy; {new Date().getFullYear()} ClassHub
                </p>
            </footer>
            <Toaster richColors />
        </div>
    );
}

export default AppShell;
