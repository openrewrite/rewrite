import {Markers, SourceFile, Tree, TreeKind} from "../";

export interface Xml extends Tree {
    readonly prefix: string;
}

export namespace Xml {
    export const Kind = {
        ...TreeKind,
        Document: "org.openrewrite.xml.tree.Xml$Document",
        Prolog: "org.openrewrite.xml.tree.Xml$Prolog",
        XmlDecl: "org.openrewrite.xml.tree.Xml$XmlDecl",
        ProcessingInstruction: "org.openrewrite.xml.tree.Xml$ProcessingInstruction",
        Tag: "org.openrewrite.xml.tree.Xml$Tag",
        Closing: "org.openrewrite.xml.tree.Xml$Tag$Closing",
        Attribute: "org.openrewrite.xml.tree.Xml$Attribute",
        AttributeValue: "org.openrewrite.xml.tree.Xml$Attribute$Value",
        CharData: "org.openrewrite.xml.tree.Xml$CharData",
        Comment: "org.openrewrite.xml.tree.Xml$Comment",
        DocTypeDecl: "org.openrewrite.xml.tree.Xml$DocTypeDecl",
        ExternalSubsets: "org.openrewrite.xml.tree.Xml$DocTypeDecl$ExternalSubsets",
        Element: "org.openrewrite.xml.tree.Xml$Element",
        Ident: "org.openrewrite.xml.tree.Xml$Ident",
        JspDirective: "org.openrewrite.xml.tree.JspDirective"
    } as const;

    export interface Document extends SourceFile, Xml {
        readonly kind: typeof Kind.Document;
        readonly id: string;
        readonly prefix: string;
        readonly markers: Markers;
        readonly prolog: Prolog;
        readonly root: Tag;
        readonly eof: string;
    }

    export interface Prolog extends Xml {
        readonly kind: typeof Kind.Prolog;
        readonly id: string;
        readonly prefix: string;
        readonly xmlDecl?: XmlDecl;
        readonly misc: Xml[];
        readonly jspDirectives: JspDirective[];
    }

    export interface XmlDecl extends Xml {
        readonly kind: typeof Kind.XmlDecl;
        readonly id: string;
        readonly prefix: string;
        readonly markers: Markers;
        readonly name: string;
        readonly attributes: Attribute[];
        readonly beforeTagDelimiterPrefix: string;
    }

    export interface ProcessingInstruction extends Xml {
        readonly kind: typeof Kind.ProcessingInstruction;
        readonly id: string;
        readonly prefix: string;
        readonly markers: Markers;
        readonly name: string;
        readonly processingInstructions: string;
        readonly beforeTagDelimiterPrefix: string;
    }

    export interface Tag extends Xml {
        readonly kind: typeof Kind.Tag;
        readonly id: string;
        readonly prefix: string;
        readonly markers: Markers;
        readonly name: string;
        readonly attributes: Attribute[];
        readonly content?: Content[];
        readonly closing?: Closing;
        readonly beforeTagDelimiterPrefix: string;
    }

    export interface Closing extends Xml {
        readonly kind: typeof Kind.Closing;
        readonly id: string;
        readonly prefix: string;
        readonly markers: Markers;
        readonly name: string;
        readonly beforeTagDelimiterPrefix: string;
    }

    export interface Attribute extends Xml {
        readonly kind: typeof Kind.Attribute;
        readonly id: string;
        readonly prefix: string;
        readonly markers: Markers;
        readonly key: Ident;
        readonly beforeEquals: string;
        readonly value: AttributeValue;
    }

    export interface AttributeValue extends Xml {
        readonly kind: typeof Kind.AttributeValue;
        readonly id: string;
        readonly prefix: string;
        readonly markers: Markers;
        readonly quote: "Double" | "Single";
        readonly value: string;
    }

    export interface CharData extends Xml {
        readonly kind: typeof Kind.CharData;
        readonly id: string;
        readonly prefix: string;
        readonly markers: Markers;
        readonly cdata: boolean;
        readonly text: string;
        readonly afterText: string;
    }

    export interface Comment extends Xml {
        readonly kind: typeof Kind.Comment;
        readonly id: string;
        readonly prefix: string;
        readonly markers: Markers;
        readonly text: string;
    }

    export interface DocTypeDecl extends Xml {
        readonly kind: typeof Kind.DocTypeDecl;
        readonly id: string;
        readonly prefix: string;
        readonly markers: Markers;
        readonly name: Ident;
        readonly documentDeclaration: string;
        readonly externalId?: Ident;
        readonly internalSubset: Ident[];
        readonly externalSubsets?: ExternalSubsets;
        readonly beforeTagDelimiterPrefix: string;
    }

    export interface ExternalSubsets extends Xml {
        readonly kind: typeof Kind.ExternalSubsets;
        readonly id: string;
        readonly prefix: string;
        readonly markers: Markers;
        readonly elements: Element[];
    }

    export interface Element extends Xml {
        readonly kind: typeof Kind.Element;
        readonly id: string;
        readonly prefix: string;
        readonly markers: Markers;
        readonly subset: Ident[];
        readonly beforeTagDelimiterPrefix: string;
    }

    export interface Ident extends Xml {
        readonly kind: typeof Kind.Ident;
        readonly id: string;
        readonly prefix: string;
        readonly markers: Markers;
        readonly name: string;
    }

    export interface JspDirective extends Xml {
        readonly kind: typeof Kind.JspDirective;
        readonly id: string;
        readonly prefix: string;
        readonly markers: Markers;
        readonly beforeTypePrefix: string;
        readonly type: string;
        readonly attributes: Attribute[];
        readonly beforeDirectiveEndPrefix: string;
    }

    export type Content =
        | Tag
        | CharData
        | Comment
        | ProcessingInstruction
        | JspDirective;
}

export function isXml(tree: any): tree is Xml {
    return Object.values(Xml.Kind).includes(tree.kind);
}
