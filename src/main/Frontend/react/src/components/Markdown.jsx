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

// The card that holds the text has the h2, so whatever level the model writes its headings at, they are h3 here
// and a heading below those an h4. The padding, not a margin, keeps a heading clear of the block above it.
const HEADING = "pt-3 text-base font-semibold first:pt-0";
const SUBHEADING = "pt-1 text-sm font-semibold first:pt-0";

const components = {
    h1: element("h3", HEADING, "auto"),
    h2: element("h3", HEADING, "auto"),
    h3: element("h3", HEADING, "auto"),
    h4: element("h4", SUBHEADING, "auto"),
    h5: element("h4", SUBHEADING, "auto"),
    h6: element("h4", SUBHEADING, "auto"),
    p: element("p", "text-sm leading-reading", "auto"),
    ul: element("ul", "list-inside list-disc space-y-2 text-sm leading-reading"),
    ol: element("ol", "list-inside list-decimal space-y-2 text-sm leading-reading"),
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
