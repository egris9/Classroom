import { MdAccountCircle, MdOutlineExpandMore } from "react-icons/md";
import { useState, useEffect } from "react";
import React from "react";
import {
    Navbar,
    MobileNav,
    Typography,
    Button,
    IconButton,
} from "@material-tailwind/react";
import {Link} from "react-router-dom";

export function NavBar() {
    const [openNav, setOpenNav] = useState(false);
    const [dropdownOpen, setDropdownOpen] = useState(false); // State for controlling dropdown visibility
    const [isLoggedIn, setIsLoggedIn] = useState(false); // Track login state
    const [user, setUser] = useState(null); // Store user details

    useEffect(() => {
        const handleResize = () => {
            if (window.innerWidth >= 960) setOpenNav(false);
        };

        window.addEventListener("resize", handleResize);
        return () => window.removeEventListener("resize", handleResize);
    }, []);

    // Simulate fetching user details after login
    useEffect(() => {
        const fetchUser = async () => {
            try {
                if (isLoggedIn) {
                    const response = await fetch("/api/auth/user");
                    if (!response.ok) {
                        console.error(`HTTP error! Status: ${response.status}`);
                        return;
                    }
                    const userData = await response.json();
                    setUser(userData);
                }
            } catch (error) {
                console.error("Error fetching user details:", error.message);
            }
        };

        fetchUser().catch((error) => {
            console.error("Unhandled error in fetchUser:", error);
        });
    }, [isLoggedIn]);

    const handleSignOut = async () => {
        try {
            const response = await fetch("/api/auth/logout", {
                method: "POST",
                credentials: "include",
            });

            if (response.ok) {
                setIsLoggedIn(false);
                setUser(null);
            } else {
                console.error("Error during logout:", response.status);
            }
        } catch (error) {
            console.error("Error during logout:", error);
        }
    };

    const navList = (
        <ul className="flex flex-col gap-2 lg:flex-row lg:items-center lg:gap-6">
            <Typography as="li" variant="small" color="blue-gray" className="p-1 font-medium">
                <a href="/" className="flex items-center">
                    Home
                </a>
            </Typography>
            <Typography as="li" variant="small" color="blue-gray" className="p-1 font-medium">
                <a href="#" className="flex items-center">
                    Course
                </a>
            </Typography>
            {/* Creationcours Link with Dropdown */}
            <Typography as="li" variant="small" color="blue-gray" className="p-1 font-medium relative">
                <button
                    className="flex items-center text-lg hover:bg-purple-100 p-1 hover:rounded-xl"
                    onClick={() => setDropdownOpen(!dropdownOpen)} // Toggle dropdown
                >
                    +
                </button>
                {dropdownOpen && (
                    <div className="absolute right-0 mt-3 bg-purple-50 shadow-lg rounded-2xl z-10 border border-purple-100">
                        <Link to="/creation" className="block px-6 py-3 text-gray-800 hover:bg-purple-200 rounded-t-2xl whitespace-nowrap">
                            Create a Course
                        </Link>
                        <Link to="/join" className="block px-6 py-3 text-gray-800 hover:bg-purple-200 rounded-b-2xl whitespace-nowrap">
                            Join a Course
                        </Link>
                    </div>
                )}
            </Typography>
        </ul>
    );

    return (
        <Navbar
            className="sticky top-0 z-10 max-w-full px-4 lg:px-8 bg-purple-50 border rounded-3xl shadow-xl shadow-purple-200">
            <div className="flex items-center justify-between text-blue-gray-900">
                <Typography as="a" href="/"
                            className="mr-4 cursor-pointer py-1.5 font-medium rounded-md text-purple-500 text-xl">
                    ClassHub
                </Typography>
                <div className="hidden lg:flex lg:items-center lg:gap-6">
                    {navList}
                    <div className="flex items-center gap-2">
                        {!isLoggedIn ? (
                            <>
                                <Link to="/signup">
                                    <Button variant="text" size="sm" className="rounded-full">
                                        Log In
                                    </Button>
                                </Link>
                                <Link to="/signing">
                                    <Button size="sm" className="rounded-full bg-purple-500 hover:bg-purple-600">
                                        Sign In
                                    </Button>
                                </Link>
                            </>
                        ) : (
                            <>
                                <Button variant="text" size="sm" className="rounded-full">
                                    {user?.firstName || "User"}
                                </Button>
                                <Button
                                    size="sm"
                                    className="rounded-full bg-purple-500 hover:bg-purple-600"
                                    onClick={handleSignOut}
                                >
                                    Sign Out
                                </Button>
                            </>
                        )}
                    </div>
                </div>
                <IconButton
                    variant="text"
                    className="ml-auto h-10 w-10 text-inherit lg:hidden rounded-full hover:bg-gray-200"
                    onClick={() => setOpenNav(!openNav)}
                >
                    {openNav ? (
                        <svg
                            xmlns="http://www.w3.org/2000/svg"
                            fill="none"
                            className="h-6 w-6"
                            viewBox="0 0 24 24"
                            stroke="currentColor"
                            strokeWidth={2}
                        >
                            <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12"/>
                        </svg>
                    ) : (
                        <svg
                            xmlns="http://www.w3.org/2000/svg"
                            fill="none"
                            className="h-6 w-6"
                            viewBox="0 0 24 24"
                            stroke="currentColor"
                            strokeWidth={2}
                        >
                            <path strokeLinecap="round" strokeLinejoin="round" d="M4 6h16M4 12h16M4 18h16"/>
                        </svg>
                    )}
                </IconButton>
            </div>
            <MobileNav open={openNav} className="rounded-lg">
                {navList}
                <div className="mt-4 flex flex-col items-center gap-2">
                    {!isLoggedIn ? (
                        <>
                            <Button fullWidth variant="text" size="sm" className="rounded-full">
                                Log In
                            </Button>
                            <Button fullWidth size="sm" className="rounded-full bg-purple-500 hover:bg-purple-600">
                                Sign In
                            </Button>
                        </>
                    ) : (
                        <>
                            <Button fullWidth variant="text" size="sm" className="rounded-full">
                                {user?.firstName || "User"}
                            </Button>
                            <Button
                                fullWidth
                                size="sm"
                                className="rounded-full bg-purple-500 hover:bg-purple-600"
                                onClick={handleSignOut}
                            >
                                Sign Out
                            </Button>
                        </>
                    )}
                </div>
            </MobileNav>
        </Navbar>
    );
}

export default NavBar;
