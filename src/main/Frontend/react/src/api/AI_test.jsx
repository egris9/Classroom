import { useState } from "react";
import axios from "axios";

const QuestionGenerator = () => {
    const [file, setFile] = useState(null);
    const [questions, setQuestions] = useState([]);
    const [error, setError] = useState("");

    const handleFileChange = (event) => {
        const selectedFile = event.target.files ? event.target.files[0] : null;
        if (selectedFile) {
            setFile(selectedFile);
            setQuestions([]); // Reset questions when a new file is selected
            setError("");
        }
    };

    const handleFileUpload = async () => {
        if (!file) {
            setError("No file selected.");
            return;
        }

        const formData = new FormData();
        formData.append("file", file);

        try {
            const response = await axios.post("https://e06c-35-237-222-125.ngrok-free.app/generate-questions", formData, {
                headers: { "Content-Type": "multipart/form-data" },
            });

            setQuestions(response.data.questions);
        } catch (error) {
            console.error("Error uploading file:", error);
            setError("Failed to generate questions. Please try again.");
        }
    };

    return (
        <div className="bg-gray-50 min-h-[670px] rounded-2xl">
            <div className="max-w-[90rem] mx-auto px-4 py-10 md:px-10 lg:px-6">
                <div className="text-center mb-16 lg:mb-20">
                    <h2 className="text-2xl md:text-4xl text-blue-500 font-bold md:leading-tight">
                        Test
                    </h2>
                </div>

                {/* File Upload Section */}
                <div className="border border-blue-300 p-6 rounded-lg shadow-lg mb-10">
                    <h3 className="text-xl text-blue-800 font-semibold mb-4">Upload Your PDF</h3>

                    <div className="flex flex-col items-center">
                        {/* File Input */}
                        <input
                            type="file"
                            accept="application/pdf"
                            onChange={handleFileChange}
                            className="border border-blue-400 rounded-lg p-2 mb-4"
                        />

                        {/* Upload Button */}
                        <button
                            onClick={handleFileUpload}
                            className="bg-blue-500 text-white py-2 px-4 rounded-lg shadow-md hover:bg-blue-600 transition duration-300"
                        >
                            Upload and Generate Questions
                        </button>
                    </div>

                    {/* Error Message */}
                    {error && <p className="text-red-500 mt-2">{error}</p>}

                    {/* Questions Display */}
                    {questions.length > 0 && (
                        <div className="mt-6">
                            <h3 className="text-lg text-blue-800 font-semibold">Generated Questions:</h3>
                            <ul className="list-disc pl-5 text-blue-600">
                                {questions.map((q, index) => (
                                    <li key={index} className="mt-1">{index + 1}. {q}</li>
                                ))}
                            </ul>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
};

export default QuestionGenerator;
