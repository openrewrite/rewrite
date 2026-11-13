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
import {ExecutionContext, findMarker} from "../../src";
import {Parser} from "../../src/parser";
import {Autodetect, JS, npm, StyleKind, TabsAndIndentsStyle, typescript} from "../../src/javascript";
import {RecipeSpec} from "../../src/test";
import {withDir} from "tmp-promise";
import * as fs from "fs";
import * as path from "path";

/** The parser factory `npm(dir, ...)` gives a spec. Each call looks the parser up in the pool again. */
async function parserIn(dir: string): Promise<() => Parser> {
    for await (const spec of npm(dir, typescript("const a = 1"))) {
        return () => spec.parser(new ExecutionContext());
    }
    throw new Error("npm() yielded no spec");
}

describe("npm", () => {
    test("pools a directory's parser for as long as the directory exists", async () => {
        await withDir(async live => {
            const liveParser = await parserIn(live.path);
            const pooled = liveParser();
            let removedPooled: Parser | undefined;
            const removedParser = await withDir(async removed => {
                const parser = await parserIn(removed.path);
                removedPooled = parser();
                return parser;
            }, {unsafeCleanup: true});

            // The pool drops removed directories when another directory's parser is first requested.
            await withDir(async next => (await parserIn(next.path))(), {unsafeCleanup: true});

            expect(liveParser()).toBe(pooled);
            expect(removedParser()).not.toBe(removedPooled);
        }, {unsafeCleanup: true});
    });

    test.each([false, true])("refreshes formatter selection on a pooled parser with config=%s", async configured => {
        // given
        await withDir(async repo => {
            const pooled = (await parserIn(repo.path))();
            const prettier = path.join(repo.path, "node_modules", "prettier");
            fs.mkdirSync(prettier, {recursive: true});
            fs.writeFileSync(path.join(prettier, "package.json"), `{"version": "3.7.4"}`);
            if (configured) {
                fs.writeFileSync(path.join(repo.path, ".prettierrc"), '{}');
            }

            // when
            const parser = (await parserIn(repo.path))();
            const sourceFile = await parser.parseOne({text: 'const a = 1;', sourcePath: 'source.ts'});

            // then
            expect(parser).toBe(pooled);
            expect(!!findMarker(sourceFile, StyleKind.PrettierStyle)).toBe(configured);
            expect(!!findMarker(sourceFile, StyleKind.Autodetect)).toBe(!configured);
        }, {unsafeCleanup: true});
    });

    test("detects the style of the dedented text that the recipe runs on", async () => {
        let indentSize: number | undefined;
        await withDir(async repo => {
            await new RecipeSpec().rewriteRun(
                npm(
                    repo.path,
                    {
                        ...typescript(
                            `
                            function f() {
                              g();
                            }
                            `
                        ),
                        beforeRecipe: (cu: JS.CompilationUnit) => {
                            const style = findMarker<Autodetect>(cu, StyleKind.Autodetect)?.styles
                                .find(s => s.kind === StyleKind.TabsAndIndentsStyle) as TabsAndIndentsStyle | undefined;
                            indentSize = style?.indentSize;
                            return cu;
                        }
                    }
                )
            );
        }, {unsafeCleanup: true});
        expect(indentSize).toBe(2);
    });
});
