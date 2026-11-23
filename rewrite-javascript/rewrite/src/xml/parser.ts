/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import {emptyMarkers, markers, Markers} from "../markers";
import {Parser, ParserInput, parserInputRead, Parsers} from "../parser";
import {SourceFile} from "../tree";
import {randomId} from "../uuid";
import {Xml} from "./tree";

const ACCEPTED_FILE_EXTENSIONS = new Set([
    "xml", "wsdl", "xhtml", "xsd", "xsl", "xslt", "xmi", "tld", "jxb", "xjb", "jsp", "jspx", "tagx",
    "mapxmi", "tblxmi", "dbxmi", "schxmi",
    "det", "pjb", "qjb", "sjb", "prt", "srt", "psc", "ssc", "tbd", "tfm", "dqs", "stp", "dcn", "pst",
    "csproj", "vbproj", "fsproj", "props", "targets", "slnx",
    "projitems", "shproj", "vcxproj", "sqlproj", "njsproj", "pyproj", "wapproj", "ruleset", "dotsettings",
    "jrxml"
]);

const HTML_VOID_ELEMENTS = new Set([
    "area", "base", "br", "col", "embed", "hr", "img", "input", "link", "meta", "param", "source", "track", "wbr"
]);

/**
 * Parses XML into an {@link Xml.Document} that prints back byte for byte. The tree has the shape
 * `org.openrewrite.xml.XmlParser` gives the same source, down to which element each run of whitespace
 * is the prefix of, so that a recipe sees the same tree whichever side parsed the file.
 */
export class XmlParser extends Parser {

    /**
     * Whether `org.openrewrite.xml.XmlParser` takes the file at this path.
     */
    static accept(sourcePath: string): boolean {
        const fileName = sourcePath.substring(Math.max(sourcePath.lastIndexOf("/"), sourcePath.lastIndexOf("\\")) + 1);
        const dot = fileName.lastIndexOf(".");
        if (0 < dot && dot < fileName.length - 1 &&
            ACCEPTED_FILE_EXTENSIONS.has(fileName.substring(dot + 1).toLowerCase())) {
            return true;
        }
        const lower = fileName.toLowerCase();
        return lower === "nuget.config" || lower === "packages.config" ||
            ((lower.startsWith("app.") || lower.startsWith("web.")) && lower.endsWith(".config"));
    }

    async *parse(...sourcePaths: ParserInput[]): AsyncGenerator<SourceFile> {
        for (const input of sourcePaths) {
            const sourcePath = this.relativePath(input);
            try {
                const text = parserInputRead(input);
                const bom = text.startsWith("\uFEFF");
                const document: Xml.Document = {
                    ...new XmlParserReader(bom ? text.substring(1) : text, isHtmlLike(sourcePath)).parse(),
                    sourcePath,
                    charsetName: "UTF-8",
                    charsetBomMarked: bom
                };
                yield await this.requirePrintEqualsInput(document, input, text);
            } catch (e: any) {
                yield this.error(input, e instanceof Error ? e : new Error(String(e)));
            }
        }
    }
}

function isHtmlLike(sourcePath: string): boolean {
    const p = sourcePath.toLowerCase();
    return p.endsWith(".jsp") || p.endsWith(".html") || p.endsWith(".htm");
}

const NAME_START_CHAR = ":A-Z_a-z\\u00C0-\\u00D6\\u00D8-\\u00F6\\u00F8-\\u02FF\\u0370-\\u037D\\u037F-\\u1FFF" +
    "\\u200C-\\u200D\\u2070-\\u218F\\u3001-\\uD7FF\\uF900-\\uFDCF\\uFDF0-\\uFFFD\\u{10000}-\\u{EFFFF}";
const NAME_CHAR = NAME_START_CHAR + "\\-.0-9\\u00B7\\u0300-\\u036F\\u203F-\\u2040";
const NAME = `[${NAME_START_CHAR}][${NAME_CHAR}]*`;

/**
 * The whitespace the lexer skips between the parts of a tag, `S` in the grammar.
 */
const TAG_WHITESPACE = /[ \t\r\n]*/y;
/**
 * The whitespace the lexer skips between markup, `WS` in the grammar.
 */
const MARKUP_WHITESPACE = /^[ \t\n\r\f]+$/;
const NAME_PATTERN = new RegExp(NAME, "uy");
const NAME_START_PATTERN = new RegExp(`[${NAME_START_CHAR}]`, "uy");
const NAME_CHAR_PATTERN = new RegExp(`[${NAME_CHAR}]`, "uy");
const REFERENCE_PATTERN = new RegExp(`&(?:#[0-9]+|#x[0-9a-fA-F]+|${NAME});`, "uy");
const PARAMETER_ENTITY_REFERENCE_PATTERN = new RegExp(`%${NAME};`, "uy");
const TEXT_END = /[<&]/g;

/**
 * `java.lang.Character.isWhitespace(char)`, by which the Java parser splits the whitespace off character data.
 */
function isJavaWhitespace(c: number): boolean {
    return (c >= 0x09 && c <= 0x0D) || (c >= 0x1C && c <= 0x20) || c === 0x1680 ||
        (c >= 0x2000 && c <= 0x2006) || (c >= 0x2008 && c <= 0x200A) ||
        c === 0x2028 || c === 0x2029 || c === 0x205F || c === 0x3000;
}

class XmlParserReader {
    private cursor = 0;

    constructor(private readonly source: string, private readonly htmlMode: boolean) {
    }

    parse(): Omit<Xml.Document, "sourcePath"> {
        const prefix = this.markupWhitespace();
        const prolog = this.parseProlog();
        const beforeRoot = this.cursor;
        const rootPrefix = this.markupWhitespace();
        let root: Xml.Tag | undefined;
        if (this.startsWithTag()) {
            root = this.parseTag(rootPrefix);
        } else if (this.cursor === this.source.length) {
            this.cursor = beforeRoot;
        } else {
            throw this.error("Expected the root element");
        }
        return {
            kind: Xml.Kind.Document,
            id: randomId(),
            prefix,
            markers: emptyMarkers,
            prolog,
            root,
            eof: this.source.substring(this.cursor)
        };
    }

    private parseProlog(): Xml.Prolog {
        const xmlDecl = this.startsWithXmlDecl() ? this.parseXmlDecl() : undefined;

        const misc: Xml.Misc[] = [];
        while (true) {
            const start = this.cursor;
            const prefix = this.markupWhitespace();
            if (this.startsWith("<!--")) {
                misc.push(this.parseComment(prefix));
            } else if (this.startsWithDocTypeDecl()) {
                misc.push(this.parseDocTypeDecl(prefix));
            } else if (this.startsWith("<?") && !this.startsWithXmlDecl()) {
                misc.push(this.parseProcessingInstruction(prefix));
            } else if (this.startsWith("<%--")) {
                misc.push(this.parseJspComment(prefix));
            } else if (this.startsWith("<%!")) {
                misc.push(this.parseDelimited(prefix, Xml.Kind.JspDeclaration, "<%!", "%>"));
            } else {
                this.cursor = start;
                break;
            }
        }

        const jspDirectives: Xml.JspDirective[] = [];
        while (true) {
            const start = this.cursor;
            const prefix = this.markupWhitespace();
            if (this.startsWith("<%@")) {
                jspDirectives.push(this.parseJspDirective(prefix));
            } else {
                this.cursor = start;
                break;
            }
        }

        return {
            kind: Xml.Kind.Prolog,
            id: randomId(),
            prefix: "",
            markers: emptyMarkers,
            xmlDecl,
            misc,
            jspDirectives
        };
    }

    private parseXmlDecl(): Xml.XmlDecl {
        this.cursor += "<?".length;
        const name = this.name();
        const {attributes, before} = this.parseAttributes();
        this.expect("?>");
        return {
            kind: Xml.Kind.XmlDecl,
            id: randomId(),
            prefix: "",
            markers: emptyMarkers,
            name,
            attributes,
            beforeTagDelimiterPrefix: before
        };
    }

    private parseProcessingInstruction(prefix: string): Xml.ProcessingInstruction {
        this.cursor += "<?".length;
        const name = this.name();
        const end = this.indexOf("?>", this.cursor, "<?" + name);
        const data = this.source.substring(this.cursor, end);
        this.cursor = end + "?>".length;
        // As the lexer has it, a lone whitespace character is skipped, while any longer run is data.
        const isData = data.length > 1 || (data.length === 1 && !/[ \t\r\n]/.test(data));
        return {
            kind: Xml.Kind.ProcessingInstruction,
            id: randomId(),
            prefix,
            markers: emptyMarkers,
            name,
            processingInstructions: isData ? this.charData(data, "") : undefined,
            beforeTagDelimiterPrefix: isData ? "" : data
        };
    }

    private parseTag(prefix: string): Xml.Tag {
        const start = this.cursor;
        this.cursor += "<".length;
        const name = this.name();
        const {attributes, before} = this.parseAttributes();

        if (this.startsWith("/>")) {
            this.cursor += "/>".length;
            return this.tag(prefix, emptyMarkers, name, attributes, undefined, undefined, before);
        }
        this.expect(">");
        if (this.htmlMode && HTML_VOID_ELEMENTS.has(name.toLowerCase())) {
            return this.tag(prefix, markers({kind: Xml.MarkersKind.HtmlVoidElement, id: randomId()}),
                name, attributes, undefined, undefined, before);
        }

        const content: Xml.Content[] = [];
        while (true) {
            const textStart = this.cursor;
            TEXT_END.lastIndex = textStart;
            const textEnd = TEXT_END.exec(this.source)?.index ?? this.source.length;
            this.cursor = textEnd;
            let itemPrefix = "";
            if (textEnd > textStart) {
                const text = this.source.substring(textStart, textEnd);
                if (MARKUP_WHITESPACE.test(text)) {
                    itemPrefix = text;
                } else {
                    content.push(this.charData(text, ""));
                }
            }
            if (this.cursor >= this.source.length) {
                throw this.error(`Unclosed element <${name}>`, start);
            }
            if (this.startsWith("</")) {
                const closing = this.parseClosing(itemPrefix, name);
                return this.tag(prefix, emptyMarkers, name, attributes, content, closing, before);
            }
            content.push(this.parseContent(itemPrefix));
        }
    }

    private parseClosing(prefix: string, openName: string): Xml.TagClosing {
        const start = this.cursor;
        this.cursor += "</".length;
        const name = this.name();
        const before = this.tagWhitespace();
        this.expect(">");
        if (!this.htmlMode && name !== openName) {
            throw this.error(`Mismatched closing tag </${name}> for <${openName}>`, start);
        }
        return {
            kind: Xml.Kind.TagClosing,
            id: randomId(),
            prefix,
            markers: emptyMarkers,
            name,
            beforeTagDelimiterPrefix: before
        };
    }

    private tag(prefix: string, tagMarkers: Markers, name: string, attributes: Xml.Attribute[],
                content: Xml.Content[] | undefined, closing: Xml.TagClosing | undefined, before: string): Xml.Tag {
        return {
            kind: Xml.Kind.Tag,
            id: randomId(),
            prefix,
            markers: tagMarkers,
            name,
            attributes,
            content,
            closing,
            beforeTagDelimiterPrefix: before
        };
    }

    private parseContent(prefix: string): Xml.Content {
        if (this.startsWith("<!--")) {
            return this.parseComment(prefix);
        } else if (this.startsWith("<![CDATA[")) {
            const end = this.indexOf("]]>", this.cursor + "<![CDATA[".length, "<![CDATA[");
            const text = this.source.substring(this.cursor + "<![CDATA[".length, end);
            this.cursor = end + "]]>".length;
            return {
                kind: Xml.Kind.CharData,
                id: randomId(),
                prefix,
                markers: emptyMarkers,
                cdata: true,
                text,
                afterText: ""
            };
        } else if (this.startsWith("<%--")) {
            return this.parseJspComment(prefix);
        } else if (this.startsWith("<%!")) {
            return this.parseDelimited(prefix, Xml.Kind.JspDeclaration, "<%!", "%>");
        } else if (this.startsWith("<%=")) {
            return this.parseDelimited(prefix, Xml.Kind.JspExpression, "<%=", "%>");
        } else if (this.startsWith("<%") && this.cursor + 2 < this.source.length && !"@-".includes(this.source[this.cursor + 2])) {
            return this.parseDelimited(prefix, Xml.Kind.JspScriptlet, "<%", "%>");
        } else if (this.startsWith("<?") && !this.startsWithXmlDecl()) {
            return this.parseProcessingInstruction(prefix);
        } else if (this.startsWithTag()) {
            return this.parseTag(prefix);
        } else if (this.startsWith("&")) {
            REFERENCE_PATTERN.lastIndex = this.cursor;
            const reference = REFERENCE_PATTERN.exec(this.source);
            if (!reference) {
                throw this.error("Expected an entity or character reference");
            }
            this.cursor += reference[0].length;
            return {
                kind: Xml.Kind.CharData,
                id: randomId(),
                prefix,
                markers: emptyMarkers,
                cdata: false,
                text: reference[0],
                afterText: ""
            };
        }
        throw this.error("Unexpected markup");
    }

    private parseComment(prefix: string): Xml.Comment {
        const end = this.indexOf("-->", this.cursor + "<!--".length, "<!--");
        const text = this.source.substring(this.cursor + "<!--".length, end);
        this.cursor = end + "-->".length;
        return {
            kind: Xml.Kind.Comment,
            id: randomId(),
            prefix,
            markers: emptyMarkers,
            text
        };
    }

    private parseJspComment(prefix: string): Xml.JspComment {
        return this.parseDelimited(prefix, Xml.Kind.JspComment, "<%--", "--%>");
    }

    private parseDelimited<K extends typeof Xml.Kind.JspComment | typeof Xml.Kind.JspDeclaration |
        typeof Xml.Kind.JspExpression | typeof Xml.Kind.JspScriptlet>(prefix: string, kind: K, open: string, close: string):
        { kind: K, id: string, prefix: string, markers: Markers, content: string } {
        // The lexer's non-greedy match needs at least one character between a scriptlet's `<%` and `%>`.
        const contentStart = this.cursor + open.length;
        const end = this.indexOf(close, kind === Xml.Kind.JspScriptlet ? contentStart + 1 : contentStart, open);
        const content = this.source.substring(contentStart, end);
        this.cursor = end + close.length;
        return {
            kind,
            id: randomId(),
            prefix,
            markers: emptyMarkers,
            content
        };
    }

    private parseJspDirective(prefix: string): Xml.JspDirective {
        this.cursor += "<%@".length;
        const beforeTypePrefix = this.tagWhitespace();
        const type = this.name();
        const {attributes, before} = this.parseAttributes();
        this.expect("%>");
        return {
            kind: Xml.Kind.JspDirective,
            id: randomId(),
            prefix,
            markers: emptyMarkers,
            beforeTypePrefix,
            type,
            attributes,
            beforeDirectiveEndPrefix: before
        };
    }

    private parseAttributes(): { attributes: Xml.Attribute[], before: string } {
        const attributes: Xml.Attribute[] = [];
        while (true) {
            const prefix = this.tagWhitespace();
            if (!this.startsWithName()) {
                return {attributes, before: prefix};
            }
            attributes.push(this.parseAttribute(prefix));
        }
    }

    private parseAttribute(prefix: string): Xml.Attribute {
        const key: Xml.Ident = {
            kind: Xml.Kind.Ident,
            id: randomId(),
            prefix: "",
            markers: emptyMarkers,
            name: this.name()
        };
        const beforeEquals = this.tagWhitespace();
        this.expect("=");
        const valuePrefix = this.tagWhitespace();
        const valueStart = this.cursor;
        const quote = this.source[this.cursor];
        if (quote !== '"' && quote !== "'") {
            throw this.error(`Expected a quoted value for attribute ${key.name}`);
        }
        const end = this.source.indexOf(quote, this.cursor + 1);
        const value = end < 0 ? "" : this.source.substring(this.cursor + 1, end);
        if (end < 0 || value.includes("<")) {
            throw this.error(`Malformed value of attribute ${key.name}`, valueStart);
        }
        this.cursor = end + 1;
        return {
            kind: Xml.Kind.Attribute,
            id: randomId(),
            prefix,
            markers: emptyMarkers,
            key,
            beforeEquals,
            value: {
                kind: Xml.Kind.AttributeValue,
                id: randomId(),
                prefix: valuePrefix,
                markers: emptyMarkers,
                quote: quote === '"' ? "Double" : "Single",
                value
            }
        };
    }

    private parseDocTypeDecl(prefix: string): Xml.DocTypeDecl {
        this.cursor += "<!".length;
        const documentDeclaration = this.source.substring(this.cursor, this.cursor + "DOCTYPE".length);
        this.cursor += documentDeclaration.length;

        const namePrefix = this.tagWhitespace();
        const name = this.ident(namePrefix, this.name());

        let externalId: Xml.Ident | undefined;
        let start = this.cursor;
        const externalIdPrefix = this.tagWhitespace();
        if (this.startsWithName()) {
            externalId = this.ident(externalIdPrefix, this.name());
        } else {
            this.cursor = start;
        }

        const internalSubset: Xml.Ident[] = [];
        while (true) {
            start = this.cursor;
            const stringPrefix = this.tagWhitespace();
            const quote = this.source[this.cursor];
            if (quote !== '"' && quote !== "'") {
                this.cursor = start;
                break;
            }
            const end = this.source.indexOf(quote, this.cursor + 1);
            if (end < 0 || this.source.substring(this.cursor, end).includes("<")) {
                throw this.error("Malformed string in the document type declaration");
            }
            internalSubset.push(this.ident(stringPrefix, this.source.substring(this.cursor, end + 1)));
            this.cursor = end + 1;
        }

        let externalSubsets: Xml.DocTypeDeclExternalSubsets | undefined;
        start = this.cursor;
        const subsetPrefix = this.tagWhitespace();
        if (this.startsWith("[")) {
            externalSubsets = this.parseExternalSubsets(subsetPrefix);
        } else {
            this.cursor = start;
        }

        const before = this.tagWhitespace();
        this.expect(">");
        return {
            kind: Xml.Kind.DocTypeDecl,
            id: randomId(),
            prefix,
            markers: emptyMarkers,
            name,
            documentDeclaration,
            externalId,
            internalSubset,
            externalSubsets,
            beforeTagDelimiterPrefix: before
        };
    }

    private parseExternalSubsets(prefix: string): Xml.DocTypeDeclExternalSubsets {
        this.cursor += "[".length;
        const elements: Xml.Element[] = [];
        while (true) {
            const elementPrefix = this.tagWhitespace();
            if (this.startsWith("]")) {
                if (elements.length > 0) {
                    elements[elements.length - 1] = {
                        ...elements[elements.length - 1],
                        beforeTagDelimiterPrefix: elementPrefix
                    };
                } else if (elementPrefix !== "") {
                    throw this.error("An empty internal subset has no element to hold its whitespace");
                }
                this.cursor += "]".length;
                break;
            }
            const start = this.cursor;
            this.skipMarkupDecl();
            elements.push({
                kind: Xml.Kind.Element,
                id: randomId(),
                prefix: "",
                markers: emptyMarkers,
                subset: [this.ident(elementPrefix, this.source.substring(start, this.cursor))],
                beforeTagDelimiterPrefix: ""
            });
        }
        return {
            kind: Xml.Kind.DocTypeDeclExternalSubsets,
            id: randomId(),
            prefix,
            markers: emptyMarkers,
            elements
        };
    }

    /**
     * Moves past a markup declaration, comment, processing instruction or parameter entity reference
     * of a document type declaration's internal subset, which the tree keeps as text.
     */
    private skipMarkupDecl(): void {
        if (this.startsWith("<!--")) {
            this.cursor = this.indexOf("-->", this.cursor + "<!--".length, "<!--") + "-->".length;
        } else if (this.startsWith("<?")) {
            this.cursor = this.indexOf("?>", this.cursor + "<?".length, "<?") + "?>".length;
        } else if (this.startsWith("<!")) {
            const start = this.cursor;
            this.cursor += "<!".length;
            while (true) {
                const c = this.source[this.cursor];
                if (c === undefined) {
                    throw this.error("Unterminated markup declaration", start);
                } else if (c === ">") {
                    this.cursor++;
                    return;
                } else if (c === '"' || c === "'" || c === "[") {
                    const end = this.source.indexOf(c === "[" ? "]" : c, this.cursor + 1);
                    if (end < 0) {
                        throw this.error("Unterminated markup declaration", start);
                    }
                    this.cursor = end + 1;
                } else {
                    this.cursor++;
                }
            }
        } else {
            PARAMETER_ENTITY_REFERENCE_PATTERN.lastIndex = this.cursor;
            const reference = PARAMETER_ENTITY_REFERENCE_PATTERN.exec(this.source);
            if (!reference) {
                throw this.error("Expected a markup declaration in the internal subset");
            }
            this.cursor += reference[0].length;
        }
    }

    private charData(text: string, prefix: string): Xml.CharData {
        let start = 0;
        while (start < text.length && isJavaWhitespace(text.charCodeAt(start))) {
            start++;
        }
        let end = text.length;
        while (end > start && isJavaWhitespace(text.charCodeAt(end - 1))) {
            end--;
        }
        return {
            kind: Xml.Kind.CharData,
            id: randomId(),
            prefix: prefix + text.substring(0, start),
            markers: emptyMarkers,
            cdata: false,
            text: text.substring(start, end),
            afterText: text.substring(end)
        };
    }

    private ident(prefix: string, name: string): Xml.Ident {
        return {
            kind: Xml.Kind.Ident,
            id: randomId(),
            prefix,
            markers: emptyMarkers,
            name
        };
    }

    private name(): string {
        NAME_PATTERN.lastIndex = this.cursor;
        const match = NAME_PATTERN.exec(this.source);
        if (!match) {
            throw this.error("Expected a name");
        }
        this.cursor += match[0].length;
        return match[0];
    }

    private startsWithName(): boolean {
        NAME_START_PATTERN.lastIndex = this.cursor;
        return NAME_START_PATTERN.test(this.source);
    }

    private startsWithTag(): boolean {
        if (this.source[this.cursor] !== "<") {
            return false;
        }
        NAME_START_PATTERN.lastIndex = this.cursor + 1;
        return NAME_START_PATTERN.test(this.source);
    }

    private startsWithXmlDecl(): boolean {
        if (!this.startsWith("<?xml")) {
            return false;
        }
        NAME_CHAR_PATTERN.lastIndex = this.cursor + "<?xml".length;
        return !NAME_CHAR_PATTERN.test(this.source);
    }

    private startsWithDocTypeDecl(): boolean {
        return this.startsWith("<!") &&
            this.source.substring(this.cursor + 2, this.cursor + 2 + "DOCTYPE".length).toUpperCase() === "DOCTYPE";
    }

    private tagWhitespace(): string {
        TAG_WHITESPACE.lastIndex = this.cursor;
        const whitespace = TAG_WHITESPACE.exec(this.source)![0];
        this.cursor += whitespace.length;
        return whitespace;
    }

    private markupWhitespace(): string {
        const start = this.cursor;
        while (this.cursor < this.source.length && " \t\n\r\f".includes(this.source[this.cursor])) {
            this.cursor++;
        }
        return this.source.substring(start, this.cursor);
    }

    private startsWith(s: string): boolean {
        return this.source.startsWith(s, this.cursor);
    }

    private expect(s: string): void {
        if (!this.startsWith(s)) {
            throw this.error(`Expected '${s}'`);
        }
        this.cursor += s.length;
    }

    private indexOf(s: string, from: number, opened: string): number {
        const index = this.source.indexOf(s, from);
        if (index < 0) {
            throw this.error(`Expected '${s}' to close '${opened}'`);
        }
        return index;
    }

    private error(message: string, at: number = this.cursor): Error {
        const before = this.source.substring(0, at);
        const line = before.split("\n").length;
        const column = at - before.lastIndexOf("\n");
        return new Error(`${message} at line ${line}, column ${column}`);
    }
}

Parsers.registerParser("xml", XmlParser);
