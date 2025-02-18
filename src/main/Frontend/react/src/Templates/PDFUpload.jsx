import { Button, Typography, Input } from "@material-tailwind/react";
import { useState, useCallback } from "react";
import { Document, Page, pdfjs } from "react-pdf";
import { NavBar } from "./components/navbar.jsx";
import { useNavigate } from "react-router-dom";


pdfjs.GlobalWorkerOptions.workerSrc = `https://cdnjs.cloudflare.com/ajax/libs/pdf.js/${pdfjs.version}/pdf.worker.min.js`;

export function PDFUpload() {
    const [pdfName, setPdfName] = useState("");
    const [pdfFile, setPdfFile] = useState(null);
    const [courseName, setCourseName] = useState("");
    const [courseCode, setCourseCode] = useState("");
    const [pdfList, setPdfList] = useState([]);
    const [error, setError] = useState(null);
    const [successMessage, setSuccessMessage] = useState("");
    const [isSubmitting, setIsSubmitting] = useState(false);
    const navigate = useNavigate();

    const handleFileChange = (event) => {
        setPdfFile(event.target.files[0]);
    };

    const handleSubmit = useCallback(
        async (event) => {
            event.preventDefault();
            setError(null);
            setSuccessMessage("");
            setIsSubmitting(true);

            if (!pdfName.trim() || !pdfFile || !courseName.trim() || !courseCode.trim()) {
                setError("All fields (course name, course code, PDF name, and file) are required");
                setIsSubmitting(false);
                return;
            }

            try {
                // Simuler un envoi du fichier et du nom via une API
                // Remplace cette partie par la logique d'upload réelle
                const newPdf = { courseName, courseCode, name: pdfName, file: pdfFile.name };
                setPdfList([...pdfList, newPdf]);
                setSuccessMessage("PDF successfully uploaded!");
                setCourseName(""); // Reset course name input
                setCourseCode(""); // Reset course code input
                setPdfName(""); // Reset pdf name input
                setPdfFile(null); // Reset file input
            } catch (error) {
                setError("An error occurred during the upload");
            } finally {
                setIsSubmitting(false);
            }
        },
        [pdfName, pdfFile, courseName, courseCode, pdfList]
    );

    return (
        <section className="min-h-screen bg-purple-50">
            <NavBar />
            <div className="flex items-center justify-center mt-48 px-4">
                <div className="flex flex-col md:flex-row w-full gap-6 mx-auto max-w-6xl justify-between bg-gray-50 p-6 md:p-10 rounded-2xl">
                    <div className="flex flex-col gap-3 flex-1 items-center md:items-start text-center md:text-left mt-20">
                        <Typography variant="h2" color="blue-gray" className=" ml-16">
                            Upload PDF for Course
                        </Typography>
                        <Typography className="mb-8 md:mb-10 text-gray-600 font-normal text-xl ml-20">
                            Upload PDF files with course details
                        </Typography>
                    </div>
                    <form onSubmit={handleSubmit} className="space-y-8 w-full max-w-md">
                        <div>
                            <Input
                                value={courseName}
                                onChange={(e) => setCourseName(e.target.value)}
                                id="courseName"
                                label="Course Name"
                                size="lg"
                                variant="outlined"
                                color="gray"
                                placeholder="Enter course name"
                                required
                            />
                        </div>
                        <div>
                            <Input
                                value={courseCode}
                                onChange={(e) => setCourseCode(e.target.value)}
                                id="courseCode"
                                label="Course Code"
                                size="lg"
                                variant="outlined"
                                color="gray"
                                placeholder="Enter course code"
                                required
                            />
                        </div>
                        <div>
                            <Input
                                value={pdfName}
                                onChange={(e) => setPdfName(e.target.value)}
                                id="pdfName"
                                label="PDF Name"
                                size="lg"
                                variant="outlined"
                                color="gray"
                                placeholder="Enter PDF name"
                                required
                            />
                        </div>
                        <div>
                            <Input
                                type="file"
                                onChange={handleFileChange}
                                id="pdfFile"
                                label="Upload PDF"
                                size="lg"
                                variant="outlined"
                                color="gray"
                                required
                            />
                        </div>
                        <Button
                            color="purple"
                            size="lg"
                            className="mt-6"
                            fullWidth
                            type="submit"
                            disabled={isSubmitting}
                        >
                            {isSubmitting ? "Uploading..." : "Upload PDF"}
                        </Button>
                        {error && <div className="text-red-500 mt-4 text-center">{error}</div>}
                        {successMessage && (
                            <div className="text-green-500 mt-4 text-center">
                                {successMessage}
                            </div>
                        )}
                    </form>
                </div>
            </div>
        </section>
    );
}

export default PDFUpload;
