// noinspection TypeScriptUnresolvedReference,TypeScriptCheckImport,JSDuplicatedDeclaration

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
import {RecipeSpec} from "../../../src/test";
import {typescript} from "../../../src/javascript";

describe('export keyword tests', () => {
    const spec = new RecipeSpec();

    test('module.export', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                const nxPreset = require('@nx/jest/preset').default;

                module.exports = {...nxPreset};
            `)
        ));

    test('export declaration', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 export type ObjectMetadataItemWithFieldMaps = ObjectMetadataInterface & {
                     fieldsById: FieldMetadataMap;
                     fieldsByName: FieldMetadataMap;
                 };
             `)
        ));

    test('export specifier', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 import type { SomeThing } from "./some-module.js";
                 export type { SomeThing };
             `)
        ));

    test('class export', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
 
                 export class RelationMetadataResolver {
 
                 }
 
                 export default class RelationMetadataResolver {
 
                 }
             `)
        ));

    test('namespace export', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 export namespace MyNamespace {
                     export const x = 10;
                     export function greet() {
                         return 'Hello';
                     }
                 }
             `)
        ));

    test('enum export', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 export enum RemoteServerType {
                     POSTGRES_FDW = 'postgres_fdw',
                     STRIPE_FDW = 'stripe_fdw',
                 }
             `)
        ));

    // noinspection JSUnnecessarySemicolon
    test('object export', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 function foo() {};
                 function bar() {};
                 export {foo, bar};
             `),
            //language=typescript
            typescript(`
                 function foo() {}
                 function bar() {};
                 export default {foo, bar};
             `),
            //language=typescript
            typescript(`
                 // Default export of a variable
                 export default 42;
             `)
        ));

    test('re-export', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 // Re-exporting everything from another module
                 export * from './accessibility';
                 export * as name1 from "module-name"    ;
                 // Re-exporting specific members from another module
                 export { foo, bar } from './anotherModule';
                 export { foo as myFoo, bar as myBar } from './anotherModule';
             `)
        ));

    test('single statement export', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 // Exporting a single item as default using \`export =\`
                 export = MyClass;
             `)
        ));

    test('e2e', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                // Exporting declarations
                // noinspection JSAnnotator

                export let name1, name2/*, … */; // also var
                export const name1 = 1, name2 = 2/*, … */; // also var, let
                export function functionName() { /* … */
                }

                export class ClassName { /* … */
                }

                export function* generatorFunctionName() { /* … */
                }

                export const {name1, name2: bar} = {};
                export const [name1, name2] = array;

                // Export list
                export {name1, /* …, */ nameN} ;
                export {variable1 as name1, variable2 as name2, /* …, */ nameN};
                export {name1 as default /*, … */};

                // Default exports
                export default expression;
                export default function functionName() { /* … */
                }
                export default class ClassName { /* … */
                }
                export default function* generatorFunctionName() { /* … */
                }
                export default function () { /* … */
                }
                export default class { /* … */
                }
                export default function* () { /* … */
                }

                // Aggregating modules
                export * from "module-name";
                export * as name1 from "module-name"    ;
                export {name1, /* …, */ nameN} from "module-name";
                export {import1 as name1, import2 as name2, /* …, */ nameN} from "module-name" ;
                export {default, /* …, */} from "module-name";
                export {default as name1} from "module-name";
            `)
        ));

    test('empty named export', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 export {/*a*/}
             `)
        ));

    test('export with attributes', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 export type { A } from "A" with { type: "json" };
                 export type * as B from "B" /*a*/ with /*b*/ { type: "json" }/*c*/;
                 export { E, type F } from "C" assert { type: "json" };
             `)
        ));

    // noinspection ES6UnusedImports
    test('export/import with empty attributes', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 export * as foo from "foo.json"
                 export * as bar from "bar.json" assert { }
                 export * as baz from "baz.json" assert { /* comment */ }
 
                 import * as foo from "foo.json"
                 import * as bar from "bar.json" assert { }
                 import * as baz from "baz.json" assert { /* comment */ }
             `)
        ));
});
