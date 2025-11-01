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

        test('should not remove require from multi-variable assignment if it is used', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new RemoveImport("util"));

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        var util = require('util'),
                            _ = require('underscore');

                        function example() {
                            return util.promisify(fs.readFile);
                        }
                    `
                )
            );
        });

        test('should not remove import when used in initializer of typed variable', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new RemoveImport("util"));

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        import * as util from 'util';

                        const eachLine: any = util.promisify(LineReader.eachLine);
                    `
                )
            );
        });

        test('should preserve blank line when removing middle require statement', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new RemoveImport("util"));

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        const http = require('http');
                        const util = require('util');

                        describe("nographql", () => {
                        })
                    `,
                    `
                        const http = require('http');

                        describe("nographql", () => {
                        })
                    `
                )
            );
        });

        test('should preserve var keyword when removing first variable from multi-variable assignment', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new RemoveImport("underscore"));

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        var _ = require('underscore'),
                            model = require('./model'),
                            util = require('util');

                        function example() {
                            model.save();
                            util.inspect({});
                        }
                    `,
                    `
                        var model = require('./model'),
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

    describe('import-equals-require syntax', () => {
        test('should remove unused import-equals-require', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new RemoveImport("util"));

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        import util = require("util");

                        console.log("no util usage");
                    `,
                    `
                        console.log("no util usage");
                    `
                )
            );
        });

        test('should not remove used import-equals-require', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new RemoveImport("util"));

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        import util = require("util");

                        console.log(util.isArray([]));
                    `
                )
            );
        });

        test('should remove unused import-equals-require with member specified', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new RemoveImport("util", "isArray"));

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        import util = require("util");

                        console.log("no util usage");
                    `,
                    `
                        console.log("no util usage");
                    `
                )
            );
        });

        test('should remove unused import-equals-require from multiple imports', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new RemoveImport("fs"));

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        import util = require("util");
                        import fs = require("fs");
                        import path = require("path");

                        console.log(util.isArray([]));
                        console.log(path.join("a", "b"));
                    `,
                    `
                        import util = require("util");
                        import path = require("path");

                        console.log(util.isArray([]));
                        console.log(path.join("a", "b"));
                    `
                )
            );
        });

        test('should preserve comments when removing import-equals-require', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new RemoveImport("fs"));

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        import util = require("util");
                        // This is a comment about fs
                        import fs = require("fs");
                        import path = require("path");

                        console.log(util.isArray([]));
                        console.log(path.join("a", "b"));
                    `,
                    `
                        import util = require("util");
                        import path = require("path");

                        console.log(util.isArray([]));
                        console.log(path.join("a", "b"));
                    `
                )
            );
        });

        test('should remove import-equals-require while keeping ES6 imports', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new RemoveImport("fs"));

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        import {readFile} from "fs";
                        import util = require("util");
                        import fs = require("fs");

                        console.log(util.isArray([]));
                        readFile("test.txt", () => {});
                    `,
                    `
                        import {readFile} from "fs";
                        import util = require("util");
                        
                        console.log(util.isArray([]));
                        readFile("test.txt", () => {});
                    `
                )
            );
        });

        test('should not remove import-equals-require used as type', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new RemoveImport("util"));

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        import util = require("util");

                        const x: typeof util = {} as any;
                    `
                )
            );
        });

        test('should remove import-equals-require used in member access', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new RemoveImport("path"));

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        import util = require("util");
                        import path = require("path");

                        console.log(util.isArray([]));
                    `,
                    `
                        import util = require("util");

                        console.log(util.isArray([]));
                    `
                )
            );
        });
    });

    describe('comment preservation', () => {
        test('should remove trailing line comment when removing first import', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new RemoveImport("fs"));

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        import fs from 'fs'; // unused import
                        import path from 'path';

                        console.log(path.join('a', 'b'));
                    `,
                    `
                        import path from 'path';

                        console.log(path.join('a', 'b'));
                    `
                )
            );
        });

        test('should preserve leading comment on second element when removing first import', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new RemoveImport("fs"));

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        import fs from 'fs';
                        // This is about path
                        import path from 'path';

                        console.log(path.join('a', 'b'));
                    `,
                    `
                        // This is about path
                        import path from 'path';

                        console.log(path.join('a', 'b'));
                    `
                )
            );
        });

        test('should preserve file header comment when removing first import', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new RemoveImport("fs"));

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        // Copyright 2025
                        // File header comment
                        import fs from 'fs';
                        import path from 'path';

                        console.log(path.join('a', 'b'));
                    `,
                    `
                        // Copyright 2025
                        // File header comment
                        import path from 'path';

                        console.log(path.join('a', 'b'));
                    `
                )
            );
        });

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

    describe('leading empty lines', () => {
        test('should remove leading empty lines when removing only import', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new RemoveImport("fs", "readFile"));

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        import {readFile} from "fs";

                        console.log('test');
                    `,
                    `console.log('test');`
                )
            );
        });

        test('should preserve leading empty lines when imports remain', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new RemoveImport("fs", "readFile"));

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        import {readFile} from "fs";
                        import {join} from "path";

                        console.log(join("a", "b"));
                    `,
                    `
                        import {join} from "path";

                        console.log(join("a", "b"));
                    `
                )
            );
        });

        test('should preserve leading comment when removing only import', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new RemoveImport("fs", "readFile"));

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        // File comment
                        import {readFile} from "fs";

                        console.log('test');
                    `,
                    `
                        // File comment
                        console.log('test');
                    `
                )
            );
        });

        test('should remove leading empty lines with variable declaration', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new RemoveImport("fs", "readFile"));

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        import {readFile} from "fs";

                        const foo = 1;
                        console.log(foo);
                    `,
                    `
                        const foo = 1;
                        console.log(foo);`
                )
            );
        });

        test('should remove leading empty lines with function declaration', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new RemoveImport("fs", "readFile"));

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        import {readFile} from "fs";

                        function foo() {
                            return 42;
                        }
                    `,
                    `
                        function foo() {
                            return 42;
                        }
                    `
                )
            );
        });
    });
});
