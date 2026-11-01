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
import {isTree, TreePrinters} from "../src";
import {J} from "../src/java";
import {JavaScriptParser, JavaScriptVisitor, JS, sourceFileCache} from "../src/javascript";

export function tap<T>(obj: T, fn: (x: T) => void) {
    fn(obj);
}

/** Expects `cu` to hold the nodes that a fresh parse of the text it prints makes. */
export async function expectParserShaped(cu: JS.CompilationUnit): Promise<void> {
    const text = await TreePrinters.print(cu);
    const reparsed = await new JavaScriptParser({sourceFileCache}).parseOne({text, sourcePath: cu.sourcePath});
    expect(await outline(cu)).toEqual(await outline(reparsed as JS.CompilationUnit));
}

/** The kind of each node of `cu`, indented by its depth. */
async function outline(cu: JS.CompilationUnit): Promise<string[]> {
    const lines: string[] = [];
    await new class extends JavaScriptVisitor<undefined> {
        protected override async preVisit(tree: J): Promise<J | undefined> {
            lines.push(" ".repeat(this.cursor.asArray().filter(isTree).length - 1) + tree.kind);
            return tree;
        }
    }().visit(cu, undefined);
    return lines;
}
