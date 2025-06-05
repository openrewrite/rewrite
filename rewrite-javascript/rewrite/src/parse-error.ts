/*
 * Copyright 2025 the original author or authors.
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
import {Cursor, SourceFile, Tree} from "./tree";
import {TreeVisitor} from "./visitor";
import {PrintOutputCapture, TreePrinters} from "./print";

export const ParseErrorKind = "org.openrewrite.tree.ParseError";

export interface ParseError extends SourceFile {
    readonly kind: typeof ParseErrorKind;
    readonly text: string;
    readonly erroneous?: SourceFile;
}

export function isParseError(tree: any): tree is ParseError {
    return tree["kind"] === ParseErrorKind;
}

export class ParseErrorVisitor<P> extends TreeVisitor<Tree, P> {
    public isAcceptable(sourceFile: SourceFile, p: P): boolean {
        return isParseError(sourceFile);
    }

    protected async accept(t: Tree, p: P): Promise<Tree | undefined> {
        if (t.kind === ParseErrorKind) {
            return this.visitParseError(t as ParseError, p);
        }
        throw new Error("Unexpected tree kind: " + t.kind);
    }

    protected async visitParseError(e: ParseError, p: P): Promise<ParseError | undefined> {
        return this.produceTree(e, p);
    }
}

TreePrinters.register(ParseErrorKind, () => new class extends ParseErrorVisitor<PrintOutputCapture> {
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
