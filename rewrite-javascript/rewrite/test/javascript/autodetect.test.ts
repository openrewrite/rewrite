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
import {Autodetect, StyleKind, TabsAndIndentsStyle, SpacesStyle, WrappingAndBracesStyle, JavaScriptParser} from "../../src/javascript";
import {dedent} from "../../src/test";

describe('Autodetect', () => {
    const parser = new JavaScriptParser();

    async function parseAndDetect(...sources: string[]): Promise<Autodetect> {
        const detector = Autodetect.detector();
        for (let i = 0; i < sources.length; i++) {
            for await (const cu of parser.parse({text: dedent(sources[i]), sourcePath: `test${i}.ts`})) {
                await detector.sample(cu);
            }
        }
        return detector.build();
    }

    describe('TabsAndIndentsStyle', () => {
        test('detects 4-space indentation', async () => {
            const autodetect = await parseAndDetect(`
                function test() {
                    const x = 1;
                    if (x > 0) {
                        console.log(x);
                    }
                }
                `);
            const styles = autodetect.styles;
            const tabsAndIndents = styles.find(s => s.kind === StyleKind.TabsAndIndentsStyle) as TabsAndIndentsStyle | undefined;

            expect(tabsAndIndents).toBeDefined();
            expect(tabsAndIndents!.useTabCharacter).toBe(false);
            expect(tabsAndIndents!.indentSize).toBe(4);
        });

        test('detects 2-space indentation', async () => {
            const autodetect = await parseAndDetect(`
                function test() {
                  const x = 1;
                  if (x > 0) {
                    console.log(x);
                  }
                }
                `);
            const styles = autodetect.styles;
            const tabsAndIndents = styles.find(s => s.kind === StyleKind.TabsAndIndentsStyle) as TabsAndIndentsStyle | undefined;

            expect(tabsAndIndents).toBeDefined();
            expect(tabsAndIndents!.useTabCharacter).toBe(false);
            expect(tabsAndIndents!.indentSize).toBe(2);
        });

        test('detects tab indentation', async () => {
            const autodetect = await parseAndDetect(`
                function test() {
                \tconst x = 1;
                \tif (x > 0) {
                \t\tconsole.log(x);
                \t}
                }
                `);
            const styles = autodetect.styles;
            const tabsAndIndents = styles.find(s => s.kind === StyleKind.TabsAndIndentsStyle) as TabsAndIndentsStyle | undefined;

            expect(tabsAndIndents).toBeDefined();
            expect(tabsAndIndents!.useTabCharacter).toBe(true);
        });

        test('spaces win over tabs when more common', async () => {
            const autodetect = await parseAndDetect(`
                function test1() {
                    const a = 1;
                    const b = 2;
                    const c = 3;
                }
                function test2() {
                    const d = 4;
                    const e = 5;
                }
                function test3() {
                \tconst f = 6;
                }
                `);
            const styles = autodetect.styles;
            const tabsAndIndents = styles.find(s => s.kind === StyleKind.TabsAndIndentsStyle) as TabsAndIndentsStyle | undefined;

            expect(tabsAndIndents).toBeDefined();
            expect(tabsAndIndents!.useTabCharacter).toBe(false);
        });
    });

    describe('SpacesStyle', () => {
        test('detects spaces within ES6 import braces', async () => {
            const autodetect = await parseAndDetect(`
                import { foo } from 'bar';
                import { baz, qux } from 'quux';
                `);
            const styles = autodetect.styles;
            const spacesStyle = styles.find(s => s.kind === StyleKind.SpacesStyle) as SpacesStyle | undefined;

            expect(spacesStyle).toBeDefined();
            expect(spacesStyle!.within.es6ImportExportBraces).toBe(true);
        });

        test('detects no spaces within ES6 import braces', async () => {
            const autodetect = await parseAndDetect(`
                import {foo} from 'bar';
                import {baz, qux} from 'quux';
                `);
            const styles = autodetect.styles;
            const spacesStyle = styles.find(s => s.kind === StyleKind.SpacesStyle) as SpacesStyle | undefined;

            expect(spacesStyle).toBeDefined();
            expect(spacesStyle!.within.es6ImportExportBraces).toBe(false);
        });

        test('detects spaces within ES6 export braces', async () => {
            const autodetect = await parseAndDetect(`
                const foo = 1;
                const bar = 2;
                export { foo, bar };
                `);
            const styles = autodetect.styles;
            const spacesStyle = styles.find(s => s.kind === StyleKind.SpacesStyle) as SpacesStyle | undefined;

            expect(spacesStyle).toBeDefined();
            expect(spacesStyle!.within.es6ImportExportBraces).toBe(true);
        });

        test('majority wins for mixed brace spacing', async () => {
            const autodetect = await parseAndDetect(`
                import {foo} from 'a';
                import {bar} from 'b';
                import {baz} from 'c';
                import { qux } from 'd';
                `);
            const styles = autodetect.styles;
            const spacesStyle = styles.find(s => s.kind === StyleKind.SpacesStyle) as SpacesStyle | undefined;

            expect(spacesStyle).toBeDefined();
            expect(spacesStyle!.within.es6ImportExportBraces).toBe(false);
        });
    });

    describe('WrappingAndBracesStyle', () => {
        test('detects simple blocks on one line', async () => {
            const autodetect = await parseAndDetect(`
                if (true) {}
                while (true) {}
                for (let i = 0; i < 10; i++) {}
                `);
            const styles = autodetect.styles;
            const wrappingStyle = styles.find(s => s.kind === StyleKind.WrappingAndBracesStyle) as WrappingAndBracesStyle | undefined;

            expect(wrappingStyle).toBeDefined();
            expect(wrappingStyle!.keepWhenReformatting.simpleBlocksInOneLine).toBe(true);
        });

        test('detects simple blocks on multiple lines', async () => {
            const autodetect = await parseAndDetect(`
                if (true) {
                }
                while (true) {
                }
                for (let i = 0; i < 10; i++) {
                }
                `);
            const styles = autodetect.styles;
            const wrappingStyle = styles.find(s => s.kind === StyleKind.WrappingAndBracesStyle) as WrappingAndBracesStyle | undefined;

            expect(wrappingStyle).toBeDefined();
            expect(wrappingStyle!.keepWhenReformatting.simpleBlocksInOneLine).toBe(false);
        });

        test('detects simple methods on one line', async () => {
            const autodetect = await parseAndDetect(`
                function foo() {}
                const bar = () => {};
                class A {
                    method() {}
                }
                `);
            const styles = autodetect.styles;
            const wrappingStyle = styles.find(s => s.kind === StyleKind.WrappingAndBracesStyle) as WrappingAndBracesStyle | undefined;

            expect(wrappingStyle).toBeDefined();
            expect(wrappingStyle!.keepWhenReformatting.simpleMethodsInOneLine).toBe(true);
        });

        test('detects simple methods on multiple lines', async () => {
            const autodetect = await parseAndDetect(`
                function foo() {
                }
                const bar = () => {
                };
                class A {
                    method() {
                    }
                }
                `);
            const styles = autodetect.styles;
            const wrappingStyle = styles.find(s => s.kind === StyleKind.WrappingAndBracesStyle) as WrappingAndBracesStyle | undefined;

            expect(wrappingStyle).toBeDefined();
            expect(wrappingStyle!.keepWhenReformatting.simpleMethodsInOneLine).toBe(false);
        });

        test('majority wins for mixed simple block styles', async () => {
            const autodetect = await parseAndDetect(`
                if (a) {}
                if (b) {}
                if (c) {}
                if (d) {
                }
                `);
            const styles = autodetect.styles;
            const wrappingStyle = styles.find(s => s.kind === StyleKind.WrappingAndBracesStyle) as WrappingAndBracesStyle | undefined;

            expect(wrappingStyle).toBeDefined();
            expect(wrappingStyle!.keepWhenReformatting.simpleBlocksInOneLine).toBe(true);
        });
    });

    describe('multiple files', () => {
        test('aggregates statistics from multiple files', async () => {
            const autodetect = await parseAndDetect(
                `
                function foo() {
                    return 1;
                }
                `,
                                `
                function bar() {
                    return 2;
                }
                `,
                                `
                function baz() {
                    return 3;
                }
                `
            );
            const styles = autodetect.styles;
            const tabsAndIndents = styles.find(s => s.kind === StyleKind.TabsAndIndentsStyle) as TabsAndIndentsStyle | undefined;

            expect(tabsAndIndents).toBeDefined();
            expect(tabsAndIndents!.useTabCharacter).toBe(false);
            expect(tabsAndIndents!.indentSize).toBe(4);
        });
    });

    describe('Autodetect class', () => {
        test('has correct metadata', async () => {
            const autodetect = await parseAndDetect(`const x = 1;`);

            expect(autodetect.name).toBe('org.openrewrite.javascript.Autodetect');
            expect(autodetect.displayName).toBe('Auto-detected');
            expect(autodetect.kind).toBe('org.openrewrite.javascript.style.Autodetect');
            expect(autodetect.id).toBeDefined();
            expect(autodetect.styles.length).toBeGreaterThan(0);
        });

        test('detector can be created via static method', () => {
            const detector = Autodetect.detector();
            expect(detector).toBeDefined();
        });
    });
});
