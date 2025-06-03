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
import {Marker} from "./markers";
import {Cursor, isSourceFile, SourceFile, Tree} from "./tree";
import {TreeVisitor} from "./visitor";

type CommentWrapper = (input: string) => string;

export interface MarkerPrinter {
    beforeSyntax(marker: Marker, cursor: Cursor, commentWrapper: CommentWrapper): string;

    beforePrefix(marker: Marker, cursor: Cursor, commentWrapper: CommentWrapper): string;

    afterSyntax(marker: Marker, cursor: Cursor, commentWrapper: CommentWrapper): string;
}

const defaultMarkerPrinter: MarkerPrinter = {
    beforeSyntax(): string {
        return "";
    },
    beforePrefix(): string {
        return "";
    },
    afterSyntax(): string {
        return "";
    },
}

export class PrintOutputCapture {
    private _out: string = "";

    constructor(public readonly markerPrinter: MarkerPrinter = defaultMarkerPrinter) {
    }

    get out(): string {
        return this._out;
    }

    append(text: string | undefined): PrintOutputCapture {
        if (text && text.length > 0) {
            this._out += text;
        }
        return this;
    }
}

interface TreePrinter {
    /**
     * Add a dependency on `ts-dedent` and use `dedent` to normalize indentation if you
     * wish to print subtrees without indentation.
     *
     * @param tree Helps to determine what kind of language we are dealing with when
     * printing a subtree whose LST type is shared between multiple languages in a language family.
     * @param out Accumulates printing output.
     */
    print(tree: Tree, out?: PrintOutputCapture): Promise<string>;
}

export class TreePrinters {
    private static _registry =
        new Map<string, TreePrinter>();

    static register(kind: string, printer: () => TreeVisitor<any, PrintOutputCapture>): void {
        this._registry.set(kind, {
            async print(tree: Tree, out?: PrintOutputCapture): Promise<string> {
                const p = out || new PrintOutputCapture();
                await printer().visit(tree, p);
                return p.out;
            }})
    }

    /**
     * Retrieve the printer for a given tree kind.
     *
     * @param target Helps to determine what kind of language we are dealing with when
     * printing a subtree whose LST type is shared between multiple languages in a language family.
     */
    static printer(target: Cursor | SourceFile): TreePrinter {
        const sourceFileKind = (isSourceFile(target) ?
                target as SourceFile :
                target.firstEnclosing(isSourceFile)
        )!.kind

        if (!this._registry.has(sourceFileKind)) {
            throw new Error(`No printer registered for ${sourceFileKind}`)
        }
        return this._registry.get(sourceFileKind)!;
    }

    static print(sourceFile: SourceFile): Promise<string> {
        return this.printer(sourceFile).print(sourceFile);
    }
}

export function printer(target: Cursor | SourceFile): TreePrinter {
    return TreePrinters.printer(target);
}
