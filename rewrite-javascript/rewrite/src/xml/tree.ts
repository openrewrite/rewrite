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
import {emptyMarkers, Markers} from "../markers";
import {SourceFile, Tree} from "../tree";
import {randomId} from "../uuid";

/**
 * An XML LST element, mirroring `org.openrewrite.xml.tree.Xml`. Every element keeps the
 * whitespace before it in `prefix`, so that a document prints back exactly as it was parsed.
 */
export interface Xml extends Tree {
    readonly prefix: string
}

export namespace Xml {
    export const Kind = {
        Document: "org.openrewrite.xml.tree.Xml$Document",
        Prolog: "org.openrewrite.xml.tree.Xml$Prolog",
        XmlDecl: "org.openrewrite.xml.tree.Xml$XmlDecl",
        ProcessingInstruction: "org.openrewrite.xml.tree.Xml$ProcessingInstruction",
        Tag: "org.openrewrite.xml.tree.Xml$Tag",
        TagClosing: "org.openrewrite.xml.tree.Xml$Tag$Closing",
        Attribute: "org.openrewrite.xml.tree.Xml$Attribute",
        AttributeValue: "org.openrewrite.xml.tree.Xml$Attribute$Value",
        CharData: "org.openrewrite.xml.tree.Xml$CharData",
        Comment: "org.openrewrite.xml.tree.Xml$Comment",
        DocTypeDecl: "org.openrewrite.xml.tree.Xml$DocTypeDecl",
        DocTypeDeclExternalSubsets: "org.openrewrite.xml.tree.Xml$DocTypeDecl$ExternalSubsets",
        Element: "org.openrewrite.xml.tree.Xml$Element",
        Ident: "org.openrewrite.xml.tree.Xml$Ident",
        JspDirective: "org.openrewrite.xml.tree.Xml$JspDirective",
        JspScriptlet: "org.openrewrite.xml.tree.Xml$JspScriptlet",
        JspExpression: "org.openrewrite.xml.tree.Xml$JspExpression",
        JspDeclaration: "org.openrewrite.xml.tree.Xml$JspDeclaration",
        JspComment: "org.openrewrite.xml.tree.Xml$JspComment",
    } as const;

    export const MarkersKind = {
        /**
         * An HTML void element such as `<br>`, which prints with a bare `>` and has neither content nor a closing tag.
         */
        HtmlVoidElement: "org.openrewrite.xml.marker.HtmlVoidElement",
    } as const;

    /**
     * What may appear between a tag's start and end, mirroring `org.openrewrite.xml.tree.Content`.
     */
    export type Content = Tag | CharData | Comment | ProcessingInstruction |
        JspDirective | JspScriptlet | JspExpression | JspDeclaration | JspComment;

    /**
     * What may appear in the prolog, mirroring `org.openrewrite.xml.tree.Misc`.
     */
    export type Misc = Comment | DocTypeDecl | ProcessingInstruction | XmlDecl | JspDeclaration | JspComment;

    export interface Document extends SourceFile, Xml {
        readonly kind: typeof Kind.Document
        readonly prolog: Prolog
        /**
         * Absent from a document without a root element, such as an empty file or one holding only a prolog,
         * which the Java parser accepts as well.
         */
        readonly root?: Tag
        readonly eof: string
    }

    export interface Prolog extends Xml {
        readonly kind: typeof Kind.Prolog
        readonly xmlDecl?: XmlDecl
        readonly misc: Misc[]
        readonly jspDirectives: JspDirective[]
    }

    export interface XmlDecl extends Xml {
        readonly kind: typeof Kind.XmlDecl
        readonly name: string
        readonly attributes: Attribute[]
        /**
         * The space before `?>`.
         */
        readonly beforeTagDelimiterPrefix: string
    }

    export interface ProcessingInstruction extends Xml {
        readonly kind: typeof Kind.ProcessingInstruction
        readonly name: string
        /**
         * The instruction's data, absent from `<?target?>`.
         */
        readonly processingInstructions?: CharData
        /**
         * The space before `?>`.
         */
        readonly beforeTagDelimiterPrefix: string
    }

    export interface Tag extends Xml {
        readonly kind: typeof Kind.Tag
        readonly name: string
        readonly attributes: Attribute[]
        /**
         * Absent from a self-closing tag such as `<a/>`, and empty for `<a></a>`.
         */
        readonly content?: Content[]
        /**
         * Absent from a self-closing tag.
         */
        readonly closing?: TagClosing
        /**
         * The space before `>` or `/>`.
         */
        readonly beforeTagDelimiterPrefix: string
    }

    export interface TagClosing extends Xml {
        readonly kind: typeof Kind.TagClosing
        readonly name: string
        /**
         * The space before `>`.
         */
        readonly beforeTagDelimiterPrefix: string
    }

    export interface Attribute extends Xml {
        readonly kind: typeof Kind.Attribute
        readonly key: Ident
        readonly beforeEquals: string
        readonly value: AttributeValue
    }

    export type Quote = "Double" | "Single";

    export interface AttributeValue extends Xml {
        readonly kind: typeof Kind.AttributeValue
        readonly quote: Quote
        /**
         * The text between the quotes, with any entity references left as written.
         */
        readonly value: string
    }

    export interface CharData extends Xml {
        readonly kind: typeof Kind.CharData
        readonly cdata: boolean
        readonly text: string
        readonly afterText: string
    }

    export interface Comment extends Xml {
        readonly kind: typeof Kind.Comment
        readonly text: string
    }

    export interface DocTypeDecl extends Xml {
        readonly kind: typeof Kind.DocTypeDecl
        readonly name: Ident
        /**
         * The `DOCTYPE` keyword as written, which may be in any case.
         */
        readonly documentDeclaration: string
        readonly externalId?: Ident
        readonly internalSubset: Ident[]
        readonly externalSubsets?: DocTypeDeclExternalSubsets
        /**
         * The space before `>`.
         */
        readonly beforeTagDelimiterPrefix: string
    }

    export interface DocTypeDeclExternalSubsets extends Xml {
        readonly kind: typeof Kind.DocTypeDeclExternalSubsets
        readonly elements: Element[]
    }

    export interface Element extends Xml {
        readonly kind: typeof Kind.Element
        readonly subset: Ident[]
        /**
         * The space before `]` on the last element of a subset.
         */
        readonly beforeTagDelimiterPrefix: string
    }

    export interface Ident extends Xml {
        readonly kind: typeof Kind.Ident
        readonly name: string
    }

    export interface JspDirective extends Xml {
        readonly kind: typeof Kind.JspDirective
        readonly beforeTypePrefix: string
        readonly type: string
        readonly attributes: Attribute[]
        /**
         * The space before `%>`.
         */
        readonly beforeDirectiveEndPrefix: string
    }

    export interface JspScriptlet extends Xml {
        readonly kind: typeof Kind.JspScriptlet
        readonly content: string
    }

    export interface JspExpression extends Xml {
        readonly kind: typeof Kind.JspExpression
        readonly content: string
    }

    export interface JspDeclaration extends Xml {
        readonly kind: typeof Kind.JspDeclaration
        readonly content: string
    }

    export interface JspComment extends Xml {
        readonly kind: typeof Kind.JspComment
        readonly content: string
    }
}

const xmlKindValues = new Set<string>(Object.values(Xml.Kind));

export function isXml(tree: any): tree is Xml {
    return xmlKindValues.has(tree?.["kind"]);
}

export function isDocument(xml: Xml): xml is Xml.Document {
    return xml.kind === Xml.Kind.Document;
}

export function isTag(xml: Xml): xml is Xml.Tag {
    return xml.kind === Xml.Kind.Tag;
}

export function isAttribute(xml: Xml): xml is Xml.Attribute {
    return xml.kind === Xml.Kind.Attribute;
}

export function isCharData(xml: Xml): xml is Xml.CharData {
    return xml.kind === Xml.Kind.CharData;
}

export function isComment(xml: Xml): xml is Xml.Comment {
    return xml.kind === Xml.Kind.Comment;
}

/**
 * The child tags of a tag, optionally only those with the given name.
 */
export function getChildren(tag: Xml.Tag, name?: string): Xml.Tag[] {
    return (tag.content ?? []).filter((c): c is Xml.Tag => isTag(c) && (name === undefined || c.name === name));
}

export function getChild(tag: Xml.Tag, name: string): Xml.Tag | undefined {
    return tag.content?.find((c): c is Xml.Tag => isTag(c) && c.name === name);
}

/**
 * The character data of a tag whose content is only character data, as `Xml.Tag.getValue()` returns it.
 */
export function getValue(tag: Xml.Tag): string | undefined {
    if (!tag.content) {
        return undefined;
    }
    if (tag.content.length === 1 && isCharData(tag.content[0])) {
        return tag.content[0].text;
    }
    if (tag.content.every(isCharData)) {
        return tag.content.map(c => unescapeXml((c as Xml.CharData).text)).join("");
    }
    return undefined;
}

export function getChildValue(tag: Xml.Tag, name: string): string | undefined {
    const child = getChild(tag, name);
    return child && getValue(child);
}

export function getAttribute(tag: Xml.Tag, key: string): Xml.Attribute | undefined {
    return tag.attributes.find(a => a.key.name === key);
}

/**
 * The value of an attribute as written between its quotes, with entity references left as they are.
 */
export function getAttributeValue(tag: Xml.Tag, key: string): string | undefined {
    return getAttribute(tag, key)?.value.value;
}

/**
 * The tag renamed, together with its closing tag, like `Xml.Tag.withName(String)`.
 */
export function withName(tag: Xml.Tag, name: string): Xml.Tag {
    if (name !== name.trim()) {
        throw new Error("Tag name must not contain leading or trailing whitespace");
    }
    if (tag.name === name) {
        return tag;
    }
    return {
        ...tag,
        name,
        closing: tag.closing && {...tag.closing, name}
    };
}

/**
 * The tag with the value of an existing attribute replaced, or the tag itself when it has no such attribute.
 * The value is plain text, escaped here for the quotes the attribute already uses.
 */
export function withAttributeValue(tag: Xml.Tag, key: string, value: string): Xml.Tag {
    const index = tag.attributes.findIndex(a => a.key.name === key);
    if (index === -1) {
        return tag;
    }
    const attribute = tag.attributes[index];
    const escaped = escapeXml(value, attribute.value.quote);
    if (attribute.value.value === escaped) {
        return tag;
    }
    const attributes = [...tag.attributes];
    attributes[index] = {...attribute, value: {...attribute.value, value: escaped}};
    return {...tag, attributes};
}

/**
 * The tag with an attribute added after its last one, or with the value replaced when it has the attribute
 * already. The value is plain text and gets escaped. The new attribute takes the whitespace before the last
 * attribute, so that it lines up with attributes written one per line, or a single space on a tag without any.
 */
export function addAttribute(tag: Xml.Tag, key: string, value: string, quote: Xml.Quote = "Double"): Xml.Tag {
    if (tag.attributes.some(a => a.key.name === key)) {
        return withAttributeValue(tag, key, value);
    }
    const last = tag.attributes[tag.attributes.length - 1];
    const attribute: Xml.Attribute = {
        kind: Xml.Kind.Attribute,
        id: randomId(),
        prefix: last?.prefix ?? " ",
        markers: emptyMarkers,
        key: {kind: Xml.Kind.Ident, id: randomId(), prefix: "", markers: emptyMarkers, name: key},
        beforeEquals: "",
        value: {
            kind: Xml.Kind.AttributeValue,
            id: randomId(),
            prefix: "",
            markers: emptyMarkers,
            quote,
            value: escapeXml(value, quote)
        }
    };
    return {...tag, attributes: [...tag.attributes, attribute]};
}

export function removeAttribute(tag: Xml.Tag, key: string): Xml.Tag {
    const attributes = tag.attributes.filter(a => a.key.name !== key);
    return attributes.length === tag.attributes.length ? tag : {...tag, attributes};
}

/**
 * The tag with its content replaced by the given text, like `Xml.Tag.withValue(String)`. The text is
 * written as is, so a `<` or `&` in it has to be escaped first, for example with {@link escapeXml}.
 */
export function withValue(tag: Xml.Tag, value: string): Xml.Tag {
    const content = tag.content;
    if (content?.length === 1 && isCharData(content[0]) && content[0].text === value) {
        return tag;
    }
    const charData: Xml.CharData = content && content.length > 0 && isCharData(content[0]) ?
        {...content[0], text: value} :
        {
            kind: Xml.Kind.CharData,
            id: randomId(),
            prefix: "",
            markers: emptyMarkers,
            cdata: false,
            text: value,
            afterText: ""
        };
    return {
        ...tag,
        markers: tag.markers.markers.some(m => m.kind === Xml.MarkersKind.HtmlVoidElement) ? {
            ...tag.markers,
            markers: tag.markers.markers.filter(m => m.kind !== Xml.MarkersKind.HtmlVoidElement)
        } : tag.markers,
        content: [charData],
        closing: tag.closing ?? {
            kind: Xml.Kind.TagClosing,
            id: randomId(),
            prefix: "",
            markers: emptyMarkers,
            name: tag.name,
            beforeTagDelimiterPrefix: ""
        }
    };
}

const XML_ENTITIES: { [name: string]: string } = {
    amp: "&",
    lt: "<",
    gt: ">",
    quot: "\"",
    apos: "'"
};

/**
 * Escapes plain text for XML. Without a `quote`, it is escaped for character data, where `&`, `<` and `>`
 * become entity references. Given the quotes of an attribute value, `&`, `<` and that quote character are
 * escaped, and a `>` is left as it is, as in a UI5 expression binding such as `{= ${a} > 1 }`.
 */
export function escapeXml(text: string, quote?: Xml.Quote): string {
    const pattern = quote === "Double" ? /[&<"]/g : quote === "Single" ? /[&<']/g : /[&<>]/g;
    return text.replace(pattern, c => {
        switch (c) {
            case "&":
                return "&amp;";
            case "<":
                return "&lt;";
            case ">":
                return "&gt;";
            case "\"":
                return "&quot;";
            default:
                return "&apos;";
        }
    });
}

/**
 * Decodes the predefined entities and character references in XML text, the reverse of {@link escapeXml}.
 * Use it on {@link getAttributeValue}, which returns a value as written, before passing it to
 * {@link withAttributeValue} or {@link addAttribute}, which take plain text.
 */
export function unescapeXml(text: string): string {
    return text.replace(/&(#x[0-9a-fA-F]+|#[0-9]+|[A-Za-z]+);/g, (entity, ref: string) => {
        if (ref.startsWith("#")) {
            const codePoint = ref.startsWith("#x") ? parseInt(ref.substring(2), 16) : parseInt(ref.substring(1), 10);
            return codePoint <= 0x10FFFF ? String.fromCodePoint(codePoint) : entity;
        }
        return XML_ENTITIES[ref] ?? entity;
    });
}
