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
import {npm, packageJson, RemoveImport, typescript} from "../../src/javascript";
import {withDir} from "tmp-promise";

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

    describe('methods with same names from different types', () => {
        test('util.isArray coming from namespace import but uses Array.isArray', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new RemoveImport("util", "isArray"));

            //language=typescript
            await withDir(async (repo) => {
                await spec.rewriteRun(
                    npm(
                        repo.path,
                        typescript(
                            `
                                import * as util from 'util';

                                Array.isArray([]);
                            `,
                            //@formatter:off
                            `
                                Array.isArray([]);
                            `
                            //@formatter:on
                        ),
                        //language=json
                        packageJson(
                            `
                              {
                                "name": "test-project",
                                "version": "1.0.0",
                                "devDependencies": {
                                  "@types/node": "^20"
                                }
                              }
                            `
                        )
                    )
                )
            }, {unsafeCleanup: true});
        });

        test('isArray imported from node but uses Array.isArray', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new RemoveImport("util", "isArray"));

            //language=typescript
            await withDir(async (repo) => {
                await spec.rewriteRun(
                    npm(
                        repo.path,
                        typescript(
                            `
                                import {isArray} from 'util';

                                Array.isArray([]);
                            `,
                            //@formatter:off
                            `
                                Array.isArray([]);
                            `
                            //@formatter:on
                        ),
                        //language=json
                        packageJson(
                            `
                              {
                                "name": "test-project",
                                "version": "1.0.0",
                                "devDependencies": {
                                  "@types/node": "^20"
                                }
                              }
                            `
                        )
                    )
                )
            }, {unsafeCleanup: true});
        });

        test('aliased isArray imported from node but uses Array.isArray', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new RemoveImport("util", "isArray"));

            //language=typescript
            await withDir(async (repo) => {
                await spec.rewriteRun(
                    npm(
                        repo.path,
                        typescript(
                            `
                                import {isArray as oldIsArray} from 'util';

                                Array.isArray([]);
                            `,
                            //@formatter:off
                            `
                                Array.isArray([]);
                            `
                            //@formatter:on
                        ),
                        //language=json
                        packageJson(
                            `
                              {
                                "name": "test-project",
                                "version": "1.0.0",
                                "devDependencies": {
                                  "@types/node": "^20"
                                }
                              }
                            `
                        )
                    )
                )
            }, {unsafeCleanup: true});
        });
    })

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

        test('should remove require from multi-variable assignment', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new RemoveImport("underscore"));

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        var BinarySearchTree = require('binary-search-tree').AVLTree,
                            model = require('./model'),
                            _ = require('underscore'),
                            util = require('util');

                        function example() {
                            model.save();
                            util.inspect({});
                        }
                    `,
                    `
                        var BinarySearchTree = require('binary-search-tree').AVLTree,
                            model = require('./model'),
                            util = require('util');

                        function example() {
                            model.save();
                            util.inspect({});
                        }
                    `
                )
            );
        });
    });

    describe('comment preservation', () => {
        test('should preserve comments on subsequent lines when removing import', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new RemoveImport("fs"));

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        import fs from 'fs';

                        // This comment belongs to the class below
                        class MyClass {
                            doSomething() {
                                return 'test';
                            }
                        }
                    `,
                    `
                        // This comment belongs to the class below
                        class MyClass {
                            doSomething() {
                                return 'test';
                            }
                        }
                    `
                )
            );
        });

        test('should preserve multiple comments after removed import', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new RemoveImport("fs", "readFile"));

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        import {readFile} from 'fs';

                        /**
                         * Documentation for the function
                         * @returns {string} A test value
                         */
                        function example() {
                            return 'test';
                        }
                    `,
                    `
                        /**
                         * Documentation for the function
                         * @returns {string} A test value
                         */
                        function example() {
                            return 'test';
                        }
                    `
                )
            );
        });

        test('should preserve inline and subsequent comments when removing import', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new RemoveImport("fs"));

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        import fs from 'fs'; // This import is unused

                        // Main application code starts here
                        function main() {
                            console.log('application');
                        }
                    `,
                    `
                        // Main application code starts here
                        function main() {
                            console.log('application');
                        }
                    `
                )
            );
        });

        test('should preserve comments after multiple removed imports', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new RemoveImport("util", "isNullOrUndefined"));

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        import * as DiffGenerators from './diffgenerators/export';
                        import { isNullOrUndefined } from 'util';

                        // Gets the xml files and passes them into diff generators
                        class Foo {
                            doSomething() {
                                return DiffGenerators;
                            }
                        }
                    `,
                    `
                        import * as DiffGenerators from './diffgenerators/export';
                        // Gets the xml files and passes them into diff generators
                        class Foo {
                            doSomething() {
                                return DiffGenerators;
                            }
                        }
                    `
                )
            );
        });
    });
});
