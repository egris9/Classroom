import { NavBar } from "./components/navbar.jsx";
import ImClass from "../static/imgs/AIgen.png";


export function Courses() {
return(
    <div className="bg-purple-50 min-h-screen">
        <NavBar/>
    <div className="max-w-[90rem] mx-auto px-4 py-10 md:px-10 lg:px-6 ">
        <div className="text-center mb-16 lg:mb-20">
            <h2 className="text-2xl md:text-4xl text-purple-400 font-bold md:leading-tight">Your classes</h2>
        </div>
        {/* cards */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
            <div className="flex flex-col border rounded-xl p-4 md:p-6 max-w-[382px] max-h-[235px] bg-purple-50 border-purple-500">
                <div className="flex items-center gap-x-4">
                    <img className="rounded-full size-20" src={ImClass} alt="Avatar"/>
                    <div className="grow">
                        <h3 className="font-medium text-purple-800">
                            David go
                        </h3>
                        <p className="text-xs uppercase text-purple-200">
                            maths
                        </p>
                    </div>

                </div>
                <p className="mt-3 text-purple-300">
                    I am an ambitious workaholic, but apart from that, pretty simple person
                </p>
            </div>

        </div>

    </div>
    </div>
);
}

export default Courses;