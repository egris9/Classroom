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
    const [dropdownOpen, setDropdownOpen] = useState(false);
    const [user, setUser] = useState(null); // Stocke les informations utilisateur

    useEffect(() => {
        const handleResize = () => {
            if (window.innerWidth >= 960) setOpenNav(false);
        };

        // Vérifier si l'utilisateur est connecté au chargement
        const token = localStorage.getItem("jwt_token");
        const userFirstName = localStorage.getItem("user_firstname");
        // Vérifier si le prénom est bien récupéré depuis localStorage
        console.log("Prénom récupéré depuis localStorage:", userFirstName);

        if (token && userFirstName) {
            setUser({ firstname: userFirstName });
        }

        window.addEventListener("resize", handleResize);
        return () => window.removeEventListener("resize", handleResize);
    }, []);

    const handleSignOut = () => {
        // Déconnexion : Supprimer le jeton et réinitialiser l'état
        localStorage.removeItem('jwt_token');
        localStorage.removeItem('user_firstname');
        setUser(null);
    };



    const navList = (
        <ul className="flex flex-col gap-2 lg:flex-row lg:items-center lg:gap-6">
            <Typography as="li" variant="small" color="blue-gray" className="p-1 font-medium">
                <a href="/" className="flex items-center">
                    Home
                </a>
            </Typography>
            <Typography as="li" variant="small" color="blue-gray" className="p-1 font-medium">
                <a href="/courses" className="flex items-center">
                    Courses
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
                        {!user ? (
                            <>
                                <Link to="/signup">
                                    <Button variant="text" size="sm" className="rounded-full">
                                        Sign Up
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
                                <Typography as="span" variant="small" color="blue-gray" className="p-1 font-medium mr-4">
                                    {user.firstname}
                                </Typography>
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
                    {!user ? (
                        <>
                            <Button fullWidth variant="text" size="sm" className="rounded-full">
                                Sign Up
                            </Button>
                            <Button fullWidth size="sm" className="rounded-full bg-purple-500 hover:bg-purple-600">
                                Sign In
                            </Button>
                        </>
                    ) : (
                        <Button
                            fullWidth
                            size="sm"
                            className="rounded-full bg-purple-500 hover:bg-purple-600"
                            onClick={handleSignOut}
                        >
                            Sign Out
                        </Button>
                    )}
                </div>
            </MobileNav>
        </Navbar>
    );
}

export default NavBar;
