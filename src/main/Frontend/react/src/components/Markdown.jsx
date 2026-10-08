import ReactMarkdown from "react-markdown";

const components = {
    p: (props) => <p className="text-sm" {...props} />,
    ul: (props) => <ul className="list-disc space-y-2 pl-5 text-sm" {...props} />,
    ol: (props) => <ol className="list-decimal space-y-2 pl-5 text-sm" {...props} />,
    strong: (props) => <strong className="font-semibold" {...props} />,
};

/** Model text as formatted content. Raw HTML in the text is not rendered. */
export function Markdown({ children }) {
    return (
        <div className="space-y-2">
            <ReactMarkdown components={components}>{children}</ReactMarkdown>
        </div>
    );
}

export default Markdown;
