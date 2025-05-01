import {connect, disconnect, rewriteRun, typeScript} from '../testHarness';

describe('export keyword tests', () => {
    beforeAll(() => connect());
    afterAll(() => disconnect());

    test('module.export', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                const nxPreset = require('@nx/jest/preset').default;

                module.exports = { ...nxPreset };
            `)
        );
    });

    test('type export', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                export type ObjectMetadataItemWithFieldMaps = ObjectMetadataInterface & {
                    fieldsById: FieldMetadataMap;
                    fieldsByName: FieldMetadataMap;
                };
            `),
            //language=typescript
            typeScript(`
                import type { SomeThing } from "./some-module.js";
                export type { SomeThing };
            `)
        );
    });

    test('class export', () => {
        rewriteRun(
            //language=typescript
            typeScript(`

                export class RelationMetadataResolver {

                }

                export default class RelationMetadataResolver {

                }
            `)
        );
    });

    test('namespace export', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                export namespace MyNamespace {
                    export const x = 10;
                    export function greet() {
                        return 'Hello';
                    }
                }
            `)
        );
    });

    test('enum export', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                export enum RemoteServerType {
                    POSTGRES_FDW = 'postgres_fdw',
                    STRIPE_FDW = 'stripe_fdw',
                }
            `)
        );
    });

    test('object export', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                function foo() {};
                function bar() {};
                export {foo, bar};
            `),
            //language=typescript
            typeScript(`
                function foo() {}
                function bar() {};
                export default {foo, bar};
            `),
            //language=typescript
            typeScript(`
                // Default export of a variable
                export default 42;
            `)
        );
    });

    test('re-export', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                // Re-exporting everything from another module
                export * from './accessibility';
                export * as name1 from "module-name"    ;
                // Re-exporting specific members from another module
                export { foo, bar } from './anotherModule';
                export { foo as myFoo, bar as myBar } from './anotherModule';
            `)
        );
    });

    test('single statement export', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                // Exporting a single item as default using \`export =\`
                export = MyClass;
            `)
        );
    });

    test('e2e', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                // Exporting declarations
                export let name1, name2/*, … */; // also var
                export const name1 = 1, name2 = 2/*, … */; // also var, let
                export function functionName() { /* … */ }
                export class ClassName { /* … */ }
                export function* generatorFunctionName() { /* … */ }
                export const { name1, name2: bar } = {} ;
                export const [ name1, name2 ] = array;

                // Export list
                export { name1, /* …, */ nameN } ;
                export { variable1 as name1, variable2 as name2, /* …, */ nameN };
                export { variable1 as "string name" };
                export { name1 as default /*, … */ };

                // Default exports
                export default expression;
                export default function functionName() { /* … */ }
                export default class ClassName { /* … */ }
                export default function* generatorFunctionName() { /* … */ }
                export default function () { /* … */ }
                export default class { /* … */ }
                export default function* () { /* … */ }

                // Aggregating modules
                export * from "module-name";
                export * as name1 from "module-name"    ;
                export { name1, /* …, */ nameN } from "module-name";
                export { import1 as name1, import2 as name2, /* …, */ nameN } from "module-name" ;
                export { default, /* …, */ } from "module-name";
                export { default as name1 } from "module-name";
            `)
        );
    });

    test('empty named export', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                export {/*a*/}
            `)
        );
    });

    test('export with attributes', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                export type { A } from "A" with { type: "json" };
                export type * as B from "B" /*a*/ with /*b*/ { type: "json" }/*c*/;
                export { E, type F } from "C" assert { type: "json" };
            `)
        );
    });

    test('export/import with empty attributes', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                export * as foo from "foo.json"
                export * as bar from "bar.json" assert { }
                export * as baz from "baz.json" assert { /* comment */ }

                import * as foo from "foo.json"
                import * as bar from "bar.json" assert { }
                import * as baz from "baz.json" assert { /* comment */ }
            `)
        );
    });
});


