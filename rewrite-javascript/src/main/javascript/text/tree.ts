import {SourceFile, Tree, TreeKind} from "../tree";

export const PlainTextKind = {
    ...TreeKind,
    PlainText: "org.openrewrite.text.PlainText",
    Snippet: "org.openrewrite.text.PlainText$Snippet"
} as const

const textKindValues = new Set(Object.values(PlainTextKind));

export function isPlainText(tree: any): tree is PlainText {
    return textKindValues.has(tree["kind"]);
}

export interface PlainText extends SourceFile {
    readonly kind: typeof PlainTextKind.PlainText
    readonly text: string
    readonly snippets: Snippet[]
}

export interface Snippet extends Tree {
    readonly kind: typeof PlainTextKind.Snippet
    readonly text: string
}
