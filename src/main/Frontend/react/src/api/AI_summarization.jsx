import { useState } from "react";
import axios from "axios";

const FileUploadComponent = () => {
    const [file, setFile] = useState(null); // file will be set to the selected file or null
    const [summary, setSummary] = useState("");
    const [loading, setLoading] = useState(false); // State for loading indication

    // Handle file input change
    const handleFileChange = (event) => {
        const selectedFile = event.target.files ? event.target.files[0] : null;
        if (selectedFile) {
            setFile(selectedFile);
        }
    };

    // Handle file upload
    const handleFileUpload = async () => {
        if (!file) {
            console.error("No file selected.");
            return;
        }

        const formData = new FormData();
        formData.append("file", file); // Append the file to FormData with the key "file"

        setLoading(true); // Set loading state to true when the request starts

        try {
            // Replace with your ngrok URL or local server URL
            const response = await axios.post("https://ab02-34-74-82-52.ngrok-free.app/summarize", formData, {
                headers: {
                    "Content-Type": "multipart/form-data",
                },
            });

            setSummary(response.data.summary); // Assuming the response contains the 'summary'
        } catch (error) {
            console.error("Error uploading file:", error);
        } finally {
            setLoading(false); // Set loading state to false once the request is complete
        }
    };

    return (
        <div className="bg-gray-50 min-h-[670px] rounded-2xl">
            <div className="max-w-[90rem] mx-auto px-4 py-10 md:px-10 lg:px-6">
                <div className="text-center mb-16 lg:mb-20">
                    <h2 className="text-2xl md:text-4xl text-purple-500 font-bold md:leading-tight">
                        Summarization
                    </h2>
                </div>

                {/* File Upload Section */}
                <div className="border border-purple-300 p-6 rounded-lg shadow-lg mb-10">
                    <h3 className="text-xl text-purple-800 font-semibold mb-4">Upload Your PDF</h3>

                    <div className="flex flex-col items-center">
                        {/* File Input */}
                        <input
                            type="file"
                            onChange={handleFileChange}
                            className="border border-purple-400 rounded-lg p-2 mb-4"
                        />

                        {/* Upload Button */}
                        <button
                            onClick={handleFileUpload}
                            className="bg-purple-500 text-white py-2 px-4 rounded-lg shadow-md hover:bg-purple-600 transition duration-300"
                        >
                            Upload and Summarize
                        </button>
                    </div>

                    {/* Loading Message */}
                    {loading && (
                        <div className="mt-4 text-purple-600">
                            <p>Loading...</p>
                        </div>
                    )}

                    {/* Summary Display */}
                    {summary && (
                        <div className="mt-6">
                            <h3 className="text-lg text-purple-800 font-semibold">Summary:</h3>
                            <p className="text-purple-600">{summary}</p>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
};

export default FileUploadComponent;
