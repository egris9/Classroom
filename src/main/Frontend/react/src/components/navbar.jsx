import {Link} from "react-router";
import { MdAccountCircle } from "react-icons/md";
import { MdOutlineExpandMore } from "react-icons/md";
import {useState} from "react";



const Navbar = () => {
    const [isDropdownVisible, setDropdownVisible] = useState(false);

    const toggleDropdown = () => {
        setDropdownVisible(!isDropdownVisible);
    };

    return (
        <nav className="nav flex justify-between fixed w-full bg-red-950/80 backdrop-blur-md top-2 z-10 rounded-full max-w-4xl left-1/2 -translate-x-1/2 px-4 py-4 shadow-md">
            {/* Logo */}
            <div className="text-red-500 font-medium self-center">
                <Link to="/">
                    <div id="logo" className="text-red-200 thick-font">
                        <p className="capitalize">Conquer</p>
                    </div>
                </Link>
            </div>

            {/* Navigation Links */}
            <div className="flex text-lg text-white items-center list-none gap-x-4">
                <li>
                    <Link to="/" className="here hover:text-red-300">
                        tests
                    </Link>
                </li>
                <li>
                    <Link to="/" className="here hover:text-red-300">
                        sessions
                    </Link>
                </li>
                <li>
                    <Link to="/" className="here hover:text-red-300">
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

                              <span className="group-hover:text-red-300 transition-colors duration-300 text-lg leading-none">
                                <MdAccountCircle />
                              </span>
                            <span className="material-symbols-outlined group-hover:text-red-300 transition-colors duration-300 text-lg leading-none">
                                <MdOutlineExpandMore />
                              </span>
                        </button>

                        {isDropdownVisible && (
                            <ul className="w-fit transition-opacity duration-200 top-[calc(100%+1.5rem)] rounded-lg left-1/2 -translate-x-1/2 text-neutral-50 absolute bg-red-950/80">
                                <li className="w-36 m-2 p-2 transition-colors duration-300 rounded-lg cursor-pointer font-medium">
                                    <Link
                                        className="flex items-center justify-between gap-x-2 w-full"
                                        to="/"
                                    >
                                        Join a class
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
                                        Creat a class
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
