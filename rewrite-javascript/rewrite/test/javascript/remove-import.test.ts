// noinspection JSUnusedLocalSymbols,TypeScriptCheckImport,TypeScriptUnresolvedReference,ES6UnusedImports

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
import {describe, test} from "@jest/globals";
import {fromVisitor, RecipeSpec} from "../../src/test";
import {typescript} from "../../src/javascript";
import {RemoveImport} from "../../src/javascript/remove-import";

describe('RemoveImport visitor', () => {
    describe('named imports', () => {
        test('should remove specific named import', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new RemoveImport("fs", "readFile"));

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        import {readFile, writeFile} from 'fs';

                        function example() {
                            writeFile('test.txt', 'content', () => {
                            });
                        }
                    `,
                    `
                        import {writeFile} from 'fs';

                        function example() {
                            writeFile('test.txt', 'content', () => {
                            });
                        }
                    `
                )
            );
        });

        test('should remove entire import when only one named import', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new RemoveImport("fs", "readFile"));

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        import {readFile} from 'fs';

                        function example() {
                            console.log('test');
                        }
                    `,
                    `
                        function example() {
                            console.log('test');
                        }
                    `
                )
            );
        });

        test('should not remove import if it is used', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new RemoveImport("fs", "readFile"));

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        import {readFile} from 'fs';

                        function example() {
                            readFile('test.txt', (err, data) => {
                            });
                        }
                    `
                )
            );
        });
    });

    describe('default imports', () => {
        test('should remove default import', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new RemoveImport("fs"));

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        import fs from 'fs';

                        function example() {
                            console.log('test');
                        }
                    `,
                    `
                        function example() {
                            console.log('test');
                        }
                    `
                )
            );
        });

        test('should not remove default import if used', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new RemoveImport("fs"));

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        import fs from 'fs';

                        function example() {
                            fs.readFileSync('test.txt');
                        }
                    `
                )
            );
        });
    });

    describe('namespace imports', () => {
        test('should remove namespace import', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new RemoveImport("fs"));

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        import * as fs from 'fs';

                        function example() {
                            console.log('test');
                        }
                    `,
                    `
                        function example() {
                            console.log('test');
                        }
                    `
                )
            );
        });

        test('should not remove namespace import if used', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new RemoveImport("fs"));

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        import * as fs from 'fs';

                        function example() {
                            return fs.readFileSync('test.txt');
                        }
                    `
                )
            );
        });
    });

    describe('type imports', () => {
        test('should remove type import', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new RemoveImport("fs", "Stats"));

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        import type {Stats} from 'fs';

                        function example() {
                            return null;
                        }
                    `,
                    `
                        function example() {
                            return null;
                        }
                    `
                )
            );
        });

        test('should remove specific type from type import', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new RemoveImport("fs", "Stats"));

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        import type {Stats, Dirent} from 'fs';

                        function example(d: Dirent) {
                            return d.name;
                        }
                    `,
                    `
                        import type {Dirent} from 'fs';

                        function example(d: Dirent) {
                            return d.name;
                        }
                    `
                )
            );
        });

        test('should not remove type import if used', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new RemoveImport("fs", "Stats"));

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        import type {Stats} from 'fs';

                        function example(stats: Stats) {
                            return stats.size;
                        }
                    `
                )
            );
        });
    });

    describe('CommonJS requires', () => {
        test('should remove require statement', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new RemoveImport("fs"));

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        const fs = require('fs');

                        function example() {
                            return null;
                        }
                    `,
                    `
                        function example() {
                            return null;
                        }
                    `
                )
            );
        });

        test('should remove destructured require', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new RemoveImport("fs", "readFile"));

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        const {readFile, writeFile} = require('fs');

                        function example() {
                            writeFile('test.txt', 'content', () => {
                            });
                        }
                    `,
                    `
                        const {writeFile} = require('fs');

                        function example() {
                            writeFile('test.txt', 'content', () => {
                            });
                        }
                    `
                )
            );
        });
    });
});
