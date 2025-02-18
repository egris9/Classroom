import { Typography } from "@material-tailwind/react";
import { useState, useEffect } from "react";
import { NavBar } from "./components/navbar.jsx";
import {useParams, Link} from "react-router-dom"; // Pour récupérer l'ID du cours et rediriger

export function CoursePDFs() {
    const { courseId } = useParams(); // On récupère l'ID du cours à partir de l'URL
    const [pdfList, setPdfList] = useState([]);
    const [error, setError] = useState(null);
    const [isLoading, setIsLoading] = useState(true); // Ajout d'un état de chargement

    useEffect(() => {
        // Simuler la récupération des fichiers PDF pour un cours spécifique
        const fetchPDFs = async () => {
            try {
                // Remplacer ceci par un appel API pour obtenir les fichiers PDF associés au cours
                const response = [
                    { name: "Course_Introduction.pdf", file: "course_intro.pdf" },
                    { name: "Chapter_1_Basics.pdf", file: "chapter_1_basics.pdf" },
                    { name: "Chapter_2_Advanced.pdf", file: "chapter_2_advanced.pdf" },
                    { name: "Exercises_and_Solutions.pdf", file: "exercises_solutions.pdf" },
                    { name: "Final_Exam_Preparation.pdf", file: "final_exam_prep.pdf" },
                ];
                setPdfList(response);
            } catch (error) {
                setError("An error occurred while fetching PDFs.");
            } finally {
                setIsLoading(false); // Arrêter le chargement une fois la requête terminée
            }
        };

        fetchPDFs();
    }, [courseId]); // Mettre à jour la liste des PDFs chaque fois que l'ID du cours change

    return (
        <section className="min-h-screen bg-purple-50">
            <NavBar />
            <div className="flex items-center justify-center mt-24 px-4">
                <div className="flex flex-col w-full gap-6 mx-auto max-w-4xl bg-gray-50 p-6 md:p-8 rounded-2xl">
                    {/* Titre et sous-titre centrés */}
                    <div className="flex flex-col gap-3 text-center">
                        <Typography variant="h2" color="blue-gray">
                            PDF Files for Course
                        </Typography>
                        <Typography className="text-gray-600 font-normal text-xl">
                            View all the PDFs for this course.
                        </Typography>
                    </div>

                    {/* Liste des PDFs */}
                    <div className="mt-8 w-full">
                        {error && <div className="text-red-500 mt-4 text-center">{error}</div>}
                        {isLoading ? (
                            <Typography variant="h5" color="blue-gray" className="text-center">
                                Loading...
                            </Typography>
                        ) : (
                            <>
                                <Typography variant="h5" color="blue-gray" className="mb-4 text-center">
                                    PDFs for Course {courseId}:
                                </Typography>
                                <ul className="space-y-2">
                                    {pdfList.length > 0 ? (
                                        pdfList.map((pdf, index) => (
                                            <li key={index} className="flex justify-between items-center bg-gray-100 p-3 rounded-md">
                                                <span className="text-gray-800">{pdf.name}</span>
                                                <a
                                                    href={`/uploads/${pdf.file}`}
                                                    target="_blank"
                                                    rel="noopener noreferrer"
                                                    className="text-blue-500 hover:underline"
                                                >
                                                    View PDF
                                                </a>
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

                        {/* Bouton pour ajouter un nouveau PDF */}
                        <div className="mt-8 flex justify-center">
                            <Link to="/pdfupload"
                                className="px-4 py-2 bg-purple-500 text-white rounded-md"
                            >
                                Add a New PDF
                            </Link>
                        </div>
                    </div>
                </div>
            </div>
        </section>
    );
}

export default CoursePDFs;
