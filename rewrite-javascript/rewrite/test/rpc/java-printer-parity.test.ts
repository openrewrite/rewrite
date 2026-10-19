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
import * as fs from "fs";
import * as path from "path";
import * as rpc from "vscode-jsonrpc/node";
import {describeJavaRpc, testJavaRpc} from "../../src/test/java-rpc";
import {JavaScriptParser, JavaScriptVisitor, JS} from "../../src/javascript";
import {J} from "../../src/java";
import {Cursor, isSourceFile} from "../../src/tree";
import {foundSearchResult, Markers, markupDebug, markupInfo} from "../../src/markers";
import {MarkerPrinter, PrintOutputCapture, TreePrinters} from "../../src/print";
import {MarkerPrinter as RequestedMarkerPrinter, Print} from "../../src/rpc/request/print";

const moduleDir = path.resolve(__dirname, "../..");
// Files that between them hold every type of tree the printer prints, shared with JavaScriptPrinterParityTest.
const syntaxDir = path.resolve(moduleDir, "../src/integTest/resources/printer-parity");

interface Source {
    cu: JS.CompilationUnit;
    text: string;
}

async function parse(dir: string): Promise<Source[]> {
    const files = (fs.readdirSync(dir, {recursive: true}) as string[])
        .filter(file => /\.(ts|tsx|js|jsx|mjs)$/.test(file))
        .sort();
    expect(files).not.toHaveLength(0);

    const texts = files.map(file => fs.readFileSync(path.join(dir, file), "utf8"));
    const sources: Source[] = [];
    for await (const parsed of new JavaScriptParser({relativeTo: dir})
        .parse(...files.map((file, i) => ({text: texts[i], sourcePath: file})))) {
        expect(parsed.kind, files[sources.length]).toEqual(JS.Kind.CompilationUnit);
        sources.push({cu: parsed as JS.CompilationUnit, text: texts[sources.length]});
    }
    return sources;
}

/**
 * Marks every tree, and every third comment, left padding and container, as a recipe would.
 */
class Mark extends JavaScriptVisitor<number> {
    private count = 0;

    private mark<T extends { markers: Markers }>(t: T, always: boolean = false): T {
        const n = this.count++;
        if (n % 3 !== 0 && !always) {
            return t;
        }
        switch (n % 4) {
            case 0:
                return foundSearchResult(t);
            case 1:
                return foundSearchResult(t, "found");
            case 2:
                return markupInfo(t, "info", "detail");
            default:
                return markupDebug(t, "debug");
        }
    }

    protected override async preVisit(tree: J, _p: number): Promise<J | undefined> {
        return this.mark(tree, true);
    }

    override async visitSpace(space: J.Space, _p: number): Promise<J.Space> {
        return space && space.comments.length > 0 ?
            {...space, comments: space.comments.map(comment => this.mark(comment))} :
            space;
    }

    override async visitLeftPadded<T extends J | J.Space | number | string | boolean>(left: J.LeftPadded<T>, p: number): Promise<J.LeftPadded<T> | undefined> {
        const visited = await super.visitLeftPadded(left, p);
        return visited && this.mark(visited);
    }

    override async visitContainer<T extends J>(container: J.Container<T>, p: number): Promise<J.Container<T>> {
        return this.mark(await super.visitContainer(container, p));
    }
}

// `org.openrewrite.javascript.JavaScriptPrinter` is a port of the printer in `src/javascript/print.ts`,
// so a change to either has to be made to both.
describeJavaRpc("Java printer parity", () => {

    testJavaRpc("syntax files", async ({javaRpc}) => {
        for (const {cu, text} of await parse(syntaxDir)) {
            const printed = await TreePrinters.print(cu);
            expect(printed, cu.sourcePath).toEqual(text);
            expect(await javaRpc.rpc.print(cu), cu.sourcePath).toEqual(printed);
        }
    }, 120_000);

    testJavaRpc("the sources of this module", async ({javaRpc}) => {
        for (const {cu, text} of await parse(path.join(moduleDir, "src"))) {
            const printed = await TreePrinters.print(cu);
            expect(printed, cu.sourcePath).toEqual(text);
            expect(await javaRpc.rpc.print(cu), cu.sourcePath).toEqual(printed);
        }
    }, 300_000);

    testJavaRpc("markers", async ({javaRpc}) => {
        const markerPrinters: [RequestedMarkerPrinter, MarkerPrinter][] = [
            [RequestedMarkerPrinter.DEFAULT, MarkerPrinter.DEFAULT],
            [RequestedMarkerPrinter.SEARCH_MARKERS_ONLY, MarkerPrinter.SEARCH_MARKERS_ONLY],
            [RequestedMarkerPrinter.FENCED, MarkerPrinter.FENCED],
            [RequestedMarkerPrinter.SANITIZED, MarkerPrinter.SANITIZED],
        ];
        for (const {cu, text} of await parse(syntaxDir)) {
            const marked = await new Mark().visitDefined<JS.CompilationUnit>(cu, 0);
            for (const [requested, markerPrinter] of markerPrinters) {
                const printed = await TreePrinters.printer(marked).print(marked, new PrintOutputCapture(markerPrinter));
                if (requested === RequestedMarkerPrinter.SANITIZED) {
                    expect(printed, cu.sourcePath).toEqual(text);
                } else {
                    expect(printed, cu.sourcePath).not.toEqual(text);
                }
                expect(await javaRpc.rpc.print(marked, undefined, requested), `${cu.sourcePath} ${requested}`).toEqual(printed);
            }
        }
    }, 120_000);

    // What is printed for a tree can depend on what encloses it, so both sides are given its cursor.
    testJavaRpc("subtrees", async ({javaRpc}) => {
        for (const {cu} of await parse(syntaxDir)) {
            const subtrees: [J, Cursor][] = [];
            await new class extends JavaScriptVisitor<number> {
                protected override async preVisit(tree: J, _p: number): Promise<J | undefined> {
                    if (!isSourceFile(tree)) {
                        subtrees.push([tree, this.cursor.parent!]);
                    }
                    return tree;
                }
            }().visit(cu, 0);

            for (const [tree, parent] of subtrees) {
                const printed = await TreePrinters.printer(cu).print(tree, new PrintOutputCapture(), parent);
                expect(await javaRpc.rpc.print(tree, parent), `${tree.kind} in ${cu.sourcePath}`).toEqual(printed);
            }
        }
    }, 300_000);

    testJavaRpc("a subtree printed without its cursor", async ({javaRpc}) => {
        const cu = (await new JavaScriptParser()
            .parse({text: "const literal = { a: 1, b: 2 };", sourcePath: "literal.ts"}).next()).value as JS.CompilationUnit;
        const blocks: J.Block[] = [];
        await new class extends JavaScriptVisitor<number> {
            protected override async visitBlock(block: J.Block, p: number): Promise<J | undefined> {
                blocks.push(block);
                return super.visitBlock(block, p);
            }
        }().visit(cu, 0);

        const block = blocks[0];
        javaRpc.rpc.localObjects.set(block.id.toString(), block);
        const printed = await javaRpc.rpc.connection.sendRequest(
            new rpc.RequestType<Print, string, Error>("Print"),
            new Print(block.id, cu.kind)
        );
        // nothing says the block is the body of an object literal, whose members commas separate
        expect(printed).toEqual("{ a: 1 b: 2 }");
        expect(printed).toEqual(await TreePrinters.printer(cu).print(block));
    });
});
