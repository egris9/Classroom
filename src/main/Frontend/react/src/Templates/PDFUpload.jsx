import { Button, Typography, Input } from "@material-tailwind/react";
import { useState } from "react";
import { useParams } from "react-router-dom";
import {  pdfjs } from "react-pdf";
import { uploadFile } from "../api/courses.js";


pdfjs.GlobalWorkerOptions.workerSrc = `https://cdnjs.cloudflare.com/ajax/libs/pdf.js/${pdfjs.version}/pdf.worker.min.js`;

export function PDFUpload() {
    const { courseid } = useParams();
    const [pdfFile, setPdfFile] = useState(null);
    const [error, setError] = useState(null);
    const [successMessage, setSuccessMessage] = useState("");
    const [isSubmitting, setIsSubmitting] = useState(false);

    const handleFileChange = (event) => {
        setPdfFile(event.target.files[0]);
    };


    const handleSubmit =
        async (event) => {
            event.preventDefault();
            setError(null);
            setSuccessMessage("");
            setIsSubmitting(true);

            if (!pdfFile) {
                setError("Choose a PDF file to upload");
                setIsSubmitting(false);
                return;
            }

            try {
                await uploadFile(courseid, pdfFile);

                setSuccessMessage("PDF successfully uploaded!");
                setPdfFile(null); // Reset file input
            } catch (error) {
                setError(error.message);
            } finally {
                setIsSubmitting(false);
            }
        }


    return (
        <section className="min-h-screen bg-purple-50">
            <div className="flex items-center justify-center mt-48 px-4">
                <div className="flex flex-col md:flex-row w-full gap-6 mx-auto max-w-6xl justify-between bg-gray-50 p-6 md:p-10 rounded-2xl">
                    <div className="flex flex-col gap-3 flex-1 items-center md:items-start text-center md:text-left mt-20">
                        <Typography variant="h2" color="blue-gray" className=" ml-16">
                            Upload PDF for Course
                        </Typography>
                        <Typography className="mb-8 md:mb-10 text-gray-600 font-normal text-xl ml-20">
                            Add a PDF to this course
                        </Typography>
                    </div>
                    <form onSubmit={handleSubmit} className="space-y-8 w-full max-w-md">
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
