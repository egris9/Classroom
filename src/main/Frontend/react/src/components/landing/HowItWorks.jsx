const STEPS = [
    {
        title: "Upload a PDF",
        text: "Create a course and add your PDFs. Each one can be up to 20 MB.",
    },
    {
        title: "Share the code",
        text: "Every course has one access code. Students type it once and the course opens.",
    },
    {
        title: "Summarise",
        text: "Anyone in the course opens a PDF and asks for its summary. Teachers can add exercises with answers.",
    },
];

/** The order of use, as three numbered steps. */
export function HowItWorks() {
    return (
        <section aria-labelledby="how" className="border-y bg-card/60">
            <div className="mx-auto w-full max-w-screen-xl px-4 py-16 sm:px-6 md:py-24 lg:px-8">
                <h2 id="how" className="text-title font-semibold">
                    How it works
                </h2>
                <ol className="mt-10 grid gap-x-10 gap-y-8 md:grid-cols-3">
                    {STEPS.map((step, index) => (
                        <li key={step.title} className="border-t pt-6">
                            <span aria-hidden="true" className="block font-display text-title font-semibold text-primary">
                                {index + 1}
                            </span>
                            <h3 className="mt-2 text-heading font-semibold">{step.title}</h3>
                            <p className="mt-2 max-w-sm text-muted-foreground">{step.text}</p>
                        </li>
                    ))}
                </ol>
            </div>
        </section>
    );
}

export default HowItWorks;
