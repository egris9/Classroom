import React, { useState } from "react";
import {Link} from "react-router";
import { MdAccountCircle } from "react-icons/md";
import { MdOutlineExpandMore } from "react-icons/md";



const Navbar = () => {
    const [isDropdownVisible, setDropdownVisible] = useState(false);

    const toggleDropdown = () => {
        setDropdownVisible(!isDropdownVisible);
    };

    return (
        <nav className="nav flex gap-x-40 fixed w-full bg-emerald-950/80 backdrop-blur-md top-2 z-10 rounded-full max-w-4xl left-1/2 -translate-x-1/2 px-4 py-4 shadow-md">
            {/* Logo */}
            <div className="text-red-500 font-medium self-center">
                <Link to="/">
                    <div id="logo" className="text-emerald-300 thick-font">
                        <p className="capitalize">Conquer</p>
                    </div>
                </Link>
            </div>

            {/* Navigation Links */}
            <div className="flex text-lg text-white items-center list-none gap-x-4">
                <li>
                    <Link to="/" className="here hover:text-emerald-300">
                        accessory
                    </Link>
                </li>
                <li>
                    <Link to="/" className="here hover:text-emerald-300">
                        sessions
                    </Link>
                </li>
                <li>
                    <Link to="/" className="here hover:text-emerald-300">
                        dashboard
                    </Link>
                </li>
            </div>

            {/* Account Section */}
            <div className="text-slate-50">
                <ul className="nav_links items-center flex justify-between">
                    {/* Dropdown Menu */}
                    <li className="relative">
                        <button
                            onClick={toggleDropdown}
                            className="group font-semibold inline-flex justify-center items-center gap-x-1 bg-white/5 p-1 py-2 rounded-lg">

                              <span className="group-hover:text-emerald-300 transition-colors duration-300 text-lg leading-none">
                                <MdAccountCircle />
                              </span>
                              <span className="material-symbols-outlined group-hover:text-emerald-300 transition-colors duration-300 text-lg leading-none">
                                <MdOutlineExpandMore />
                              </span>
                        </button>

                        {isDropdownVisible && (
                            <ul className="w-fit transition-opacity duration-200 top-[calc(100%+1.5rem)] rounded-lg left-1/2 -translate-x-1/2 text-neutral-50 absolute bg-emerald-950/80">
                                <li className="w-36 m-2 p-2 transition-colors duration-300 rounded-lg cursor-pointer font-medium">
                                    <Link
                                        className="flex items-center justify-between gap-x-2 w-full"
                                        to="/"
                                    >
                                        cart
                                        <span className="material-symbols-outlined text-lg leading-none">
                                          <MdAccountCircle />
                                        </span>
                                    </Link>
                                </li>
                                <li className="w-36 m-2 p-2 transition-colors duration-300 rounded-lg cursor-pointer font-medium">
                                    <Link
                                        className="flex items-center justify-between gap-x-2 w-full"
                                        to="/"
                                    >
                                        profile
                                        <span className="material-symbols-outlined text-lg leading-none">
                                          <MdAccountCircle />
                                        </span>
                                    </Link>
                                </li>
                                <li className="w-36 m-2 p-2 transition-colors duration-300 rounded-lg cursor-pointer font-medium">
                                    <Link
                                        className="flex items-center justify-between gap-x-2 w-full" to="/">
                                        sign out
                                        <span className="material-symbols-outlined text-lg leading-none">
                                          <MdAccountCircle />
                                        </span>
                                    </Link>
                                </li>
                            </ul>
                        )}
                    </li>


                </ul>
            </div>
        </nav>

    );
};

export default Navbar;


function App() {
  return (
    <>
        <div>
            <h1 className="text-3xl text-red-800 font-bold underline">
                Hello world!
            </h1>


        </div>

    </>
  )
}

