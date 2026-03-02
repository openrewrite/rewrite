// noinspection TypeScriptUnresolvedReference,JSUnusedLocalSymbols

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
import {JS, typescript} from "../../../src/javascript";
import {J} from "../../../src/java";

describe('variable declaration mapping', () => {
    const spec = new RecipeSpec();

    test('const', () =>
        spec.rewriteRun({
            //language=javascript
            ...typescript(`
                const c = 1;
                /* c1*/  /*c2 */
                const d = 1;
            `),
            afterRecipe: (cu: JS.CompilationUnit) => {
                expect(cu).toBeDefined();
                expect(cu.statements).toHaveLength(2);
                cu.statements.forEach(statement => expect(statement.kind).toBe(J.Kind.VariableDeclarations));
                cu.statements.forEach(statement => {
                    const varDecl = statement as unknown as J.VariableDeclarations;
                    expect(statement.padding.after.comments).toHaveLength(0);
                    expect(statement.padding.after.whitespace).toBe('');
                    expect(varDecl.variables.length).toBe(1);
                });
            }
        }));

    test('typed', () => spec.rewriteRun(
        //language=typescript
        typescript('let a : number =2')
    ));

    test('typed unknown', () => spec.rewriteRun(
        //language=typescript
        typescript('const a : unknown = 2')
    ));

    test('typed any', () => spec.rewriteRun(
        //language=typescript
        typescript('let a : any = 2;')
    ));

    test('multi', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('let a=2, b=2 ')
        ));

    test('multi variable declaration AST structure', () =>
        spec.rewriteRun({
            //language=typescript
            ...typescript(`
                var a = 1,
                    b = 2,
                    c = 3;
            `),
            afterRecipe: (cu: JS.CompilationUnit) => {
                expect(cu).toBeDefined();
                expect(cu.statements).toHaveLength(1);

                const stmt = cu.statements[0];
                expect(stmt.kind).toBe(JS.Kind.ScopedVariableDeclarations);

                const scopedVarDecl = stmt as unknown as JS.ScopedVariableDeclarations;

                // Modifiers (var/let/const) should be on the ScopedVariableDeclarations
                expect(scopedVarDecl.modifiers).toHaveLength(1);
                expect(scopedVarDecl.modifiers[0].keyword).toBe('var');

                // Should have 3 variable declarations
                expect(scopedVarDecl.variables).toHaveLength(3);

                // First variable: prefix should have whitespace (space after 'var'), no modifiers
                const firstVar = scopedVarDecl.variables[0] as unknown as J.VariableDeclarations;
                expect(firstVar.kind).toBe(J.Kind.VariableDeclarations);
                expect(firstVar.modifiers).toHaveLength(0); // No modifiers on individual variables
                expect(firstVar.prefix.whitespace).toBe(' '); // Space after 'var'
                expect(firstVar.variables).toHaveLength(1);
                expect(firstVar.variables[0].prefix.whitespace).toBe(''); // NamedVariable has empty prefix

                // Second variable: prefix should have newline + indentation, no modifiers
                const secondVar = scopedVarDecl.variables[1] as unknown as J.VariableDeclarations;
                expect(secondVar.kind).toBe(J.Kind.VariableDeclarations);
                expect(secondVar.modifiers).toHaveLength(0);
                expect(secondVar.prefix.whitespace).toMatch(/\n\s+/); // Newline + spaces
                expect(secondVar.variables[0].prefix.whitespace).toBe(''); // NamedVariable has empty prefix

                // Third variable: similar to second
                const thirdVar = scopedVarDecl.variables[2] as unknown as J.VariableDeclarations;
                expect(thirdVar.kind).toBe(J.Kind.VariableDeclarations);
                expect(thirdVar.modifiers).toHaveLength(0);
                expect(thirdVar.prefix.whitespace).toMatch(/\n\s+/);
                expect(thirdVar.variables[0].prefix.whitespace).toBe('');
            }
        }));

    test('multi typed', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('  /*0.1*/  let  /*0.2*/    a   /*1*/ :      /*2*/  number =2    /*3*/ , /*4*/   b   /*5*/:/*6*/    /*7*/string  /*8*/   =/*9*/    "2" /*10*/  ; //11')
        ));

    test('a b c', () =>
        spec.rewriteRun(
            //language=typescript

            typescript(`
                const obj: any | undefined = {}
                obj?.a?.b?.c
            `)
        ));

    test('exported variables', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(' export /*0.1*/  let  /*0.2*/    a   /*1*/ :      /*2*/  number =2    /*3*/ , /*4*/   b   /*5*/:/*6*/    /*7*/string  /*8*/   =/*9*/    "2" /*10*/  ; //11')
        ));

    test('unique symbol', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('declare const unset: unique symbol;')
        ));

    test('bigint', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('type res3 = Call<Objects.PartialDeep, bigint>;\n')
        ));

    test('property signature as an array', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                export type Apply<fn extends Fn, args extends unknown[]> = (fn & {
                    [rawArgs]: args;
                })["return"];
            `)
        ));

    test('declaration with destructuring', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                /*0*/
                const /*1*/  {Client, Status}  /*2*/ =/*3*/  require("../src");
            `),
            //language=typescript
            typescript(`
                const obj = {a: 1, b: {c: 2}};
                const {a} = obj; // const 'a' is a constant
                let {
                    /*1*/ b /*2*/:/*3*/ { /*4*/c  /*5*/: /*6*/ d /*7*/} /***/,
                } = obj; // d is re-assignable
            `),
            //language=typescript
            typescript(`
                const numbers = [];
                const obj = {a: 1, b: 2};
                ({a: numbers[0], b: numbers[1]} = obj);
            `),
            //language=typescript
            typescript(`
                const {
                    size = "big",
                    coords = {x: 0, y: 0},
                    radius = 25,
                } = {}
            `),
            //language=typescript
            typescript(`
                const key = "test";
                const obj = {};
                const aDefault = {};
                const bDefault = {};
                const {x, y} = obj;
                const {a: a1, b: b1} = obj;
                const {z: a2 = aDefault, f = bDefault} = obj;
                const {c, k, ...rest} = obj;
                const {re: a12, rb: b12, ...rest1} = obj;
                const {[key]: a} = obj;
            `),
            //language=typescript
            typescript(`
                /*1*/
                const /*2*/  {  /*3*/
                    a/*4*/:/*5*/  aa /*6*/ = /*7*/  10 /*8*/, /*9*/
                    b /*10*/:  /*11*/ bb = {} /*12*/,  /*13*/
                } = {a: 3};
            `),
        ));

    test('variable with exclamation token', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                let schema/*a*/!/*b*/: number;
            `)
        ));

    test('variable with using keyword', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                using unrefTimer = stub(Deno, 'unrefTimer');
            `)
        ));

    test('variable with await using keyword', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                const getDb = async () => {
                    return {
                        async createUser(data) {
                            const user = to<AdapterUser>(data)
                            /*a*/
                                await /*b*/ using db = await getDb()
                            await db.U.insertOne(user)
                            return from<AdapterUser>(user)
                        }
                    }
                }
            `)
        ));

    test.skip('variable declaration with decorator', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                export namespace process {
                    // @ts-ignore: decorator
                    @lazy export const platform = "wasm";
                }
            `)
        ));

    test.each([
        "const c =  function(): number { return 116; };",
        "const c =  136;",
        "const c =  (1 > 0) ? 116 : 119;"
    ])('double space: %s', (code) =>
        spec.rewriteRun({
            //language=javascript
            ...typescript(code),
            afterRecipe: (cu: JS.CompilationUnit) => {
                expect(cu.statements).toHaveLength(1);
                cu.statements.forEach(statement => {
                    const varDecl = statement as unknown as J.VariableDeclarations;
                    const initializer = varDecl.variables[0].initializer!;
                    expect(initializer.padding.before.whitespace).toBe(" ");
                    expect(initializer.prefix.whitespace).toBe("  ");
                });
            }
        }));
});
