import { Typography } from "@material-tailwind/react";
import { useState, useEffect } from "react";
import { useParams, Link } from "react-router-dom";
import { fetchFileContent, getCourse, listFiles } from "../api/courses.js";

export function PDFDisplay() {
    const { courseid } = useParams();
    const [pdfList, setPdfList] = useState([]);
    const [role, setRole] = useState(null);
    const [error, setError] = useState(null);
    const [isLoading, setIsLoading] = useState(true);

    useEffect(() => {
        const load = async () => {
            try {
                const [course, files] = await Promise.all([getCourse(courseid), listFiles(courseid)]);
                setRole(course.role);
                setPdfList(files);
            } catch (error) {
                setError(error.message);
            } finally {
                setIsLoading(false);
            }
        };
        if (courseid) {
            load();
        }
    }, [courseid]);

    // The window opens before the request so the browser does not block it as a popup.
    const openPdf = async (pdf) => {
        const viewer = window.open("", "_blank");
        if (!viewer) {
            setError("Your browser blocked the PDF window. Allow popups for this site and try again.");
            return;
        }
        try {
            const blob = await fetchFileContent(pdf.id);
            const url = URL.createObjectURL(new Blob([blob], { type: "application/pdf" }));
            viewer.location = url;
        } catch (error) {
            viewer.close();
            setError(error.message);
        }
    };

    return (
        <section className="min-h-screen bg-purple-50">
            <div className="flex items-center justify-center mt-24 px-4">
                <div className="flex flex-col w-full gap-6 mx-auto max-w-4xl bg-gray-50 p-6 md:p-8 rounded-2xl">
                    {/* Title and subtitle centered */}
                    <div className="flex flex-col gap-3 text-center">
                        <Typography variant="h2" color="blue-gray">
                            PDF Files for Course
                        </Typography>
                        <Typography className="text-gray-600 font-normal text-xl">
                            View all the PDFs for this course.
                        </Typography>
                    </div>

                    {/* PDF List */}
                    <div className="mt-8 w-full">
                        {error && <div className="text-red-500 mt-4 text-center">{error}</div>}
                        {isLoading ? (
                            <Typography variant="h5" color="blue-gray" className="text-center">
                                Loading...
                            </Typography>
                        ) : (
                            <>
                                <Typography variant="h5" color="blue-gray" className="mb-4 text-center">
                                    PDFs for Course {courseid}:
                                </Typography>
                                <ul className="space-y-2">
                                    {pdfList.length > 0 ? (
                                        pdfList.map((pdf) => (
                                            <li key={pdf.id} className="flex justify-between items-center bg-gray-100 p-3 rounded-md">
                                                <span className="text-gray-800">{pdf.fileName}</span>
                                                <div className="flex gap-3">
                                                    <button
                                                        onClick={() => openPdf(pdf)}
                                                        className="text-blue-500 hover:underline"
                                                    >
                                                        view PDF
                                                    </button>
                                                </div>
                                            </li>
                                        ))
                                    ) : (
                                        <Typography variant="paragraph" color="blue-gray" className="text-center">
                                            No PDFs available for this course.
                                        </Typography>
                                    )}
                                </ul>
                            </>
                        )}

                        {/* Button to add new PDF: Teacher only */}
                        {role === "TEACHER" && (
                            <div className="mt-8 flex justify-center">
                                <Link
                                    to={`/pdfupload/${courseid}`}
                                    className="px-4 py-2 bg-purple-500 text-white rounded-md hover:bg-purple-600 transition-colors"
                                >
                                    Add a New PDF
                                </Link>
                            </div>
                        )}
                    </div>
                </div>
            </div>
        </section>
    );
}

export default PDFDisplay;
