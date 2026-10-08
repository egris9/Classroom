import ReactMarkdown from "react-markdown";

// react-markdown passes the syntax tree node to every component; it is not a DOM attribute, so each element
// is built from the props without it.
// dir="auto" gives each block the direction of its own first strong letter, so Arabic reads right to left and
// French or English left to right. Lists keep their markers inside the item, so the marker follows the item's side.
function element(Tag, className, dir) {
    return function MarkdownElement(props) {
        const rest = { ...props };
        delete rest.node;
        return <Tag className={className} dir={dir} {...rest} />;
    };
}

const components = {
    p: element("p", "text-sm", "auto"),
    ul: element("ul", "list-inside list-disc space-y-2 text-sm"),
    ol: element("ol", "list-inside list-decimal space-y-2 text-sm"),
    li: element("li", undefined, "auto"),
    strong: element("strong", "font-semibold"),
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
