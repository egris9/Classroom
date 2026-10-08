import { Link } from "react-router-dom";
import { BookOpenCheck, KeyRound, Lock, Sparkles } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import ImagePlaceholder from "@/components/ImagePlaceholder";

export default function Landing() {
    return (
        <>
            <section className="mx-auto grid w-full max-w-screen-xl items-center gap-10 px-4 py-16 sm:px-6 md:py-24 lg:grid-cols-12 lg:px-8">
                <div className="space-y-6 lg:col-span-7">
                    <h1 className="text-[clamp(2.5rem,6vw,4.5rem)] font-semibold leading-[1.05]">
                        Course PDFs, summarised and turned into exercises
                    </h1>
                    <p className="max-w-xl text-lg text-muted-foreground">
                        Teachers upload their PDFs once and share a code. Everyone in the course reads them in one place and
                        gets a summary in a click.
                    </p>
                    <div className="flex flex-wrap items-center gap-3">
                        <Button asChild className="h-12 px-6 text-base">
                            <Link to="/signup">Sign up</Link>
                        </Button>
                        <Button asChild variant="outline" className="h-12 px-6 text-base">
                            <Link to="/tools">Try it free</Link>
                        </Button>
                        <Button asChild variant="ghost" className="h-12 px-6 text-base">
                            <Link to="/signin">I already have an account</Link>
                        </Button>
                    </div>
                </div>
                <div className="lg:col-span-5">
                    <ImagePlaceholder
                        src="/images/landing-hero.jpg"
                        caption="Students and a Teacher around a laptop reading a PDF"
                        aspect="4 / 3"
                        priority
                    />
                </div>
            </section>

            <section aria-labelledby="who" className="border-y bg-card/60">
                <div className="mx-auto grid w-full max-w-screen-xl gap-10 px-4 py-16 sm:px-6 md:py-24 lg:grid-cols-2 lg:px-8">
                    <h2 id="who" className="sr-only">
                        For Teachers and for Students
                    </h2>
                    <div className="space-y-4">
                        <ImagePlaceholder
                            src="/images/landing-teachers.jpg"
                            caption="A Teacher preparing course material at a desk"
                            aspect="16 / 9"
                        />
                        <h3 className="text-2xl font-semibold">For Teachers</h3>
                        <ul className="list-disc space-y-1 pl-5 text-muted-foreground">
                            <li>Create a course and get its access code.</li>
                            <li>Upload PDFs and delete them when they are out of date.</li>
                            <li>Generate a set of exercises with model answers for the class.</li>
                            <li>See who has joined.</li>
                        </ul>
                    </div>
                    <div className="space-y-4">
                        <ImagePlaceholder
                            src="/images/landing-students.jpg"
                            caption="A Student studying a PDF on a laptop"
                            aspect="16 / 9"
                        />
                        <h3 className="text-2xl font-semibold">For Students</h3>
                        <ul className="list-disc space-y-1 pl-5 text-muted-foreground">
                            <li>Join a course with the code your Teacher gave you.</li>
                            <li>Read every PDF next to its summary.</li>
                            <li>Ask for your own summary and keep it to yourself.</li>
                            <li>Leave a course whenever you want.</li>
                        </ul>
                    </div>
                </div>
            </section>

            <section aria-labelledby="features" className="mx-auto w-full max-w-screen-xl px-4 py-16 sm:px-6 md:py-24 lg:px-8">
                <h2 id="features" className="mb-10 text-3xl font-semibold">
                    What you get
                </h2>
                <div className="grid gap-4 md:grid-cols-12">
                    <Card className="md:col-span-7 md:row-span-2">
                        <CardContent className="flex h-full flex-col justify-center gap-4 p-8">
                            <KeyRound className="h-8 w-8 text-primary" aria-hidden="true" />
                            <h3 className="text-2xl font-semibold">One code, one course</h3>
                            <p className="text-muted-foreground">
                                Create a course, upload your PDFs and share the access code. Students type it once and the whole
                                course opens.
                            </p>
                        </CardContent>
                    </Card>
                    <Card className="md:col-span-5">
                        <CardContent className="space-y-2 p-6">
                            <Sparkles className="h-6 w-6 text-primary" aria-hidden="true" />
                            <h3 className="text-lg font-semibold">Summaries on demand</h3>
                            <p className="text-sm text-muted-foreground">
                                Ask for a summary of any PDF in the course and read it next to the original.
                            </p>
                        </CardContent>
                    </Card>
                    <Card className="md:col-span-5">
                        <CardContent className="space-y-2 p-6">
                            <BookOpenCheck className="h-6 w-6 text-primary" aria-hidden="true" />
                            <h3 className="text-lg font-semibold">Exercises with answers</h3>
                            <p className="text-sm text-muted-foreground">
                                Teachers generate open questions with model answers and publish them to the course.
                            </p>
                        </CardContent>
                    </Card>
                    <Card className="md:col-span-12">
                        <CardContent className="flex flex-col gap-3 p-6 sm:flex-row sm:items-center">
                            <Lock className="h-6 w-6 shrink-0 text-primary" aria-hidden="true" />
                            <p className="text-sm text-muted-foreground">
                                <span className="font-semibold text-foreground">Private by default for Students.</span> A
                                Teacher&apos;s summary is shared with the course. A Student&apos;s summary stays visible to that Student only.
                            </p>
                        </CardContent>
                    </Card>
                </div>
                <div className="mt-12 flex justify-center">
                    <Button asChild className="h-12 px-8 text-base">
                        <Link to="/signup">Sign up</Link>
                    </Button>
                </div>
            </section>
        </>
    );
}
