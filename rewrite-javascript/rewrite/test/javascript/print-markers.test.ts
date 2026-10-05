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
import {JavaScriptParser, JavaScriptVisitor, JS} from "../../src/javascript";
import {J} from "../../src/java";
import {foundSearchResult, markupInfo} from "../../src/markers";
import {MarkerPrinter, PrintOutputCapture, TreePrinters} from "../../src/print";

const syntaxDir = path.resolve(__dirname, "../../../src/integTest/resources/printer-parity");

async function parse(text: string, sourcePath: string): Promise<JS.CompilationUnit> {
    const parsed = (await new JavaScriptParser().parse({text, sourcePath}).next()).value;
    expect(parsed.kind, sourcePath).toEqual(JS.Kind.CompilationUnit);
    return parsed as JS.CompilationUnit;
}

describe("marker printing", () => {

    test.each(fs.readdirSync(syntaxDir).sort())("a search result on any tree of %s is printed", async file => {
        const cu = await parse(fs.readFileSync(path.join(syntaxDir, file), "utf8"), file);
        const found: string[] = [];
        const marked = await new class extends JavaScriptVisitor<number> {
            protected override async preVisit(tree: J, _p: number): Promise<J | undefined> {
                found.push(`${found.length} ${tree.kind.replace(/.*\./, "")}`);
                return foundSearchResult(tree, found[found.length - 1]);
            }
        }().visitDefined<JS.CompilationUnit>(cu, 0);

        const printed = await TreePrinters.print(marked);
        expect(found.filter(description => !printed.includes(`/*~~(${description})~~>*/`))).toEqual([]);
    });

    test("a markup prints its message, and its detail only when asked to", async () => {
        const marked = markupInfo(await parse("a", "a.ts"), "message", "detail");
        const print = (markerPrinter: MarkerPrinter) =>
            TreePrinters.printer(marked).print(marked, new PrintOutputCapture(markerPrinter));

        expect(await print(MarkerPrinter.DEFAULT)).toEqual("/*~~(message)~~>*/a");
        expect(await print(MarkerPrinter.VERBOSE)).toEqual("/*~~(detail)~~>*/a");
        expect(await print(MarkerPrinter.SEARCH_MARKERS_ONLY)).toEqual("a");
    });

    test("a markup with no detail prints its message when asked for the detail", async () => {
        const marked = markupInfo(await parse("a", "a.ts"), "message");
        expect(await TreePrinters.printer(marked).print(marked, new PrintOutputCapture(MarkerPrinter.VERBOSE)))
            .toEqual("/*~~(message)~~>*/a");
    });
});
