import { Link } from "react-router-dom";
import { BookOpenCheck, Copy, KeyRound, Lock, Sparkles } from "lucide-react";
import { Button, buttonVariants } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import ExerciseMockup from "@/components/landing/ExerciseMockup";
import HowItWorks from "@/components/landing/HowItWorks";
import PdfNotesMockup from "@/components/landing/PdfNotesMockup";
import { cn } from "@/lib/utils";

const SECTION = "mx-auto w-full max-w-screen-xl px-4 py-16 sm:px-6 md:py-24 lg:px-8";
const ROLE_LIST = "list-disc space-y-2 pl-5 text-muted-foreground";

/** A drawing of a course's access code with its copy button. Sample data, nothing in it can be used. */
function AccessCodeMockup() {
    return (
        <figure role="img" aria-label="Sample: an access code with a Copy button" className="select-none">
            <div className="flex flex-wrap items-center gap-3 rounded-lg border bg-muted/60 p-4">
                <span className="text-sm text-muted-foreground">Access code</span>
                <code className="rounded-md border bg-card px-3 py-1.5 font-mono text-xl tracking-[0.2em] sm:text-2xl">K7M2QX9P</code>
                <span className={cn(buttonVariants({ variant: "outline", size: "sm" }), "h-9 gap-1")}>
                    <Copy className="h-4 w-4" />
                    Copy
                </span>
            </div>
            <figcaption className="mt-3 text-sm text-muted-foreground">Sample code</figcaption>
        </figure>
    );
}

export default function Landing() {
    return (
        <>
            <section className={cn(SECTION, "grid items-center gap-x-10 gap-y-12 lg:grid-cols-12")}>
                <div className="space-y-6 lg:col-span-7">
                    <h1 className="text-balance text-display font-semibold">Course PDFs, summarised and turned into exercises</h1>
                    <p className="max-w-xl text-lg text-muted-foreground">
                        Teachers upload a PDF and share one code, and the whole course reads it next to its summary.
                    </p>
                    <div className="flex flex-wrap items-center gap-3">
                        <Button asChild className="h-12 px-6 text-base">
                            <Link to="/signup">Sign up</Link>
                        </Button>
                        <Button asChild variant="outline" className="h-12 px-6 text-base">
                            <Link to="/tools">Try it free</Link>
                        </Button>
                    </div>
                </div>
                <div className="lg:col-span-5">
                    <PdfNotesMockup />
                </div>
            </section>

            <HowItWorks />

            <section aria-labelledby="who" className={cn(SECTION, "grid gap-x-10 gap-y-12 lg:grid-cols-12")}>
                <h2 id="who" className="sr-only">
                    For Teachers and for Students
                </h2>
                <div className="grid items-start gap-8 sm:grid-cols-2 lg:col-span-8">
                    <div className="space-y-4">
                        <h3 className="text-title font-semibold">For Teachers</h3>
                        <ul className={ROLE_LIST}>
                            <li>Create a course and get its access code.</li>
                            <li>Upload PDFs and delete them when they are out of date.</li>
                            <li>Generate a set of exercises with model answers for the class.</li>
                            <li>See who has joined.</li>
                        </ul>
                    </div>
                    <ExerciseMockup />
                </div>
                <div className="space-y-4 lg:col-span-4 lg:border-l lg:pl-10">
                    <h3 className="text-title font-semibold">For Students</h3>
                    <ul className={ROLE_LIST}>
                        <li>Join a course with the code your Teacher gave you.</li>
                        <li>Read every PDF next to its summary.</li>
                        <li>Ask for your own summary and keep it to yourself.</li>
                        <li>Leave a course whenever you want.</li>
                    </ul>
                </div>
            </section>

            <section aria-labelledby="features" className="border-t">
                <div className={SECTION}>
                    <h2 id="features" className="mb-10 text-title font-semibold">
                        What you get
                    </h2>
                    <div className="grid gap-4 md:grid-cols-12">
                        <Card className="md:col-span-7 md:row-span-2">
                            <CardContent className="flex h-full flex-col justify-between gap-8 p-6 sm:p-8">
                                <div className="space-y-3">
                                    <KeyRound className="h-7 w-7 text-primary" aria-hidden="true" />
                                    <h3 className="text-heading font-semibold">One code, one course</h3>
                                    <p className="max-w-md text-muted-foreground">
                                        Share the access code of your course. Students type it once and every PDF in the course opens.
                                    </p>
                                </div>
                                <AccessCodeMockup />
                            </CardContent>
                        </Card>
                        <Card className="md:col-span-5">
                            <CardContent className="space-y-3 p-6">
                                <Sparkles className="h-6 w-6 text-primary" aria-hidden="true" />
                                <h3 className="text-heading font-semibold">AI tools without a course</h3>
                                <p className="text-sm text-muted-foreground">
                                    Paste a text or drop a PDF and get a summary, exercises or an answer from the study assistant. The
                                    first try needs no account.
                                </p>
                                <Button asChild variant="outline" className="h-11 px-4">
                                    <Link to="/tools">Try it free</Link>
                                </Button>
                            </CardContent>
                        </Card>
                        <Card className="md:col-span-5">
                            <CardContent className="space-y-3 p-6">
                                <BookOpenCheck className="h-6 w-6 text-primary" aria-hidden="true" />
                                <h3 className="text-heading font-semibold">Exercises with answers</h3>
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
                </div>
            </section>

            <section aria-labelledby="start" className="border-t bg-card/60">
                <div className={cn(SECTION, "flex flex-col items-start gap-6 md:flex-row md:items-center md:justify-between")}>
                    <div className="space-y-2">
                        <h2 id="start" className="text-title font-semibold">
                            Start with one PDF
                        </h2>
                        <p className="max-w-xl text-muted-foreground">
                            Create a course, upload the PDF and share the code. Signing up is free.
                        </p>
                    </div>
                    <Button asChild className="h-12 shrink-0 px-8 text-base">
                        <Link to="/signup">Sign up</Link>
                    </Button>
                </div>
            </section>
        </>
    );
}
