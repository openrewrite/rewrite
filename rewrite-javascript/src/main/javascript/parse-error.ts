import {Cursor, SourceFile, Tree, TreeKind} from "./tree";
import {TreeVisitor} from "./visitor";
import {PrintOutputCapture, TreePrinters} from "./print";

const ParseErrorKind = {
    ...TreeKind,
    ParseError: "org.openrewrite.tree.ParseError"
} as const

export interface ParseError extends SourceFile {
    readonly kind: typeof ParseErrorKind.ParseError
    readonly text: string
    readonly erroneous?: SourceFile
}

export function isParseError(tree: any): tree is ParseError {
    return tree["kind"] in ParseErrorKind;
}

export class ParseErrorVisitor<P> extends TreeVisitor<Tree, P> {
    public isAcceptable(sourceFile: SourceFile, p: P): boolean {
        return isParseError(sourceFile);
    }

    protected async accept(t: Tree, p: P): Promise<Tree | undefined> {
        if (t.kind === ParseErrorKind.ParseError) {
            return this.visitParseError(t as ParseError, p);
        }
        throw new Error("Unexpected tree kind: " + t.kind);
    }

    protected async visitParseError(e: ParseError, p: P): Promise<ParseError | undefined> {
        return this.produceTree(e, p);
    }
}

TreePrinters.register(ParseErrorKind.ParseError, new class extends ParseErrorVisitor<PrintOutputCapture> {
    protected async visitParseError(e: ParseError, p: PrintOutputCapture): Promise<ParseError | undefined> {
        for (let marker of e.markers.markers) {
            p.append(p.markerPrinter.beforePrefix(marker, new Cursor(marker, this.cursor), it => it))
        }
        await this.visitMarkers(e.markers, p);
        for (let marker of e.markers.markers) {
            p.append(p.markerPrinter.beforeSyntax(marker, new Cursor(marker, this.cursor), it => it))
        }
        p.append(e.text);
        for (let marker of e.markers.markers) {
            p.append(p.markerPrinter.afterSyntax(marker, new Cursor(marker, this.cursor), it => it))
        }
        return e;
    }
})
