// noinspection TypeScriptUnresolvedReference,TypeScriptValidateTypes,JSUnusedLocalSymbols

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
import {JavaScriptParser, JS, typescript} from "../../../src/javascript";
import {J} from "../../../src/java";
import {ExecutionContext} from "../../../src/execution";
import {findMarker, MarkersKind, ParseExceptionResult} from "../../../src/markers";
import {ParseError, ParseErrorKind} from "../../../src/parse-error";
import {SourceFile} from "../../../src/tree";


describe('class decorator mapping', () => {
    const spec = new RecipeSpec();

    test('unqualified', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('@foo class A {}')
        ));

    test('unqualified parens', () =>
        spec.rewriteRun({
            //language=typescript
            ...typescript('@foo( ) class A {}'),
            afterRecipe: (cu: JS.CompilationUnit) => {
                const classDecl = cu.statements[0].element as J.ClassDeclaration;
                const annotation = classDecl.leadingAnnotations[0];
                expect(annotation.annotationType.kind).toBe(J.Kind.Identifier);
            }
        }));

    test('qualified', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('@foo . bar class A {}')
        ));

    test('qualified parens', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('@foo . bar ( ) class A {}')
        ));

    test('parameter decorator with params', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                export class WorkspaceMemberWorkspaceEntity extends BaseWorkspaceEntity {
                    @WorkspaceField({
                        standardId: WORKSPACE_MEMBER_STANDARD_FIELD_IDS.name,
                        type: FieldMetadataType.FULL_NAME,
                        label: 'Name',
                        description: 'Workspace member name',
                        icon: 'IconCircleUser',
                    })
                    [NAME_FIELD_NAME]: FullNameMetadata;
                }
            `)
        ));

    test('decorator with type params', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                @StaticInterfaceImplement/*a*/<ISpriteAssembler>/*b*/()
                export class SimpleSpriteAssembler {
                }
            `)
        ));

    test('decorator with parenthesized expression', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                class SimpleSpriteAssembler {
                    @/*a*/(/*b*/Ember.computed('fullName').readOnly()/*c*/)/*d*/
                    get fullNameReadonly() {
                        return 'fullName';
                    }
                }
            `)
        ));

    test('decorator on class expression', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                const Foo = (x => x)(
                    @dec('')
                    class {
                    })
            `)
        ));

    test('class / method / params / properties decorators', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                @UseGuards(WorkspaceAuthGuard)
                @Resolver()
                export class RelationMetadataResolver {
                    constructor(
                        @Args('input')
                        private readonly relationMetadataService: RelationMetadataService,
                    ) {
                    }

                    @Args('input') input: DeleteOneRelationInput;

                    @Mutation(() => RelationMetadataDTO)
                    async deleteOneRelation(
                        @Args('input') input: DeleteOneRelationInput,
                        @AuthWorkspace() {id: workspaceId}: Workspace,
                    ) {
                        try {
                            return await this.relationMetadataService.deleteOneRelation(
                                input.id,
                                workspaceId,
                            );
                        } catch (error) {
                            relationMetadataGraphqlApiExceptionHandler(error);
                        }
                    }
                }
            `)
        ));

    test('decorator after modifiers', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                export
                @decorator()
                class Foo {
                }

                export default
                @decorator()
                class {
                }
            `)
        ));

    test('decorators stay where they are written among the modifiers', () =>
        spec.rewriteRun({
            //language=typescript
            ...typescript('@a /*1*/ export /*2*/ @b /*3*/ abstract /*4*/ class A {}\nexport @c class B {}'),
            afterRecipe: (cu: JS.CompilationUnit) => {
                const names = (annotations: J.Annotation[]) => annotations.map(a => (a.annotationType as J.Identifier).simpleName);
                const a = cu.statements[0].element as J.ClassDeclaration;
                expect(names(a.leadingAnnotations)).toEqual(["a"]);
                expect(a.modifiers.map(m => names(m.annotations))).toEqual([[], ["b"]]);
                expect(a.classKind.annotations).toEqual([]);

                const b = cu.statements[1].element as J.ClassDeclaration;
                expect(b.leadingAnnotations).toEqual([]);
                expect(names(b.classKind.annotations)).toEqual(["c"]);
            }
        }));

    test.for([
        "@dec enum E { A }",
        "export @dec const enum E { A }",
        "@dec export interface I { }",
        "export @dec interface I { }",
        "@inline function f() {}",
        "// @ts-ignore: decorator\n@inline\nexport function f() {}",
    ])('decorator on %s', (code) =>
        spec.rewriteRun(
            //language=typescript
            typescript(code)
        ));
});

describe('decorators and modifiers before a declaration', () => {
    const spec = new RecipeSpec();

    const names = (annotations: J.Annotation[]) => annotations.map(a => (a.annotationType as J.Identifier).simpleName);
    const keywords = (modifiers: J.Modifier[]) => modifiers.map(m => m.keyword ?? m.type);
    const modifierAnnotations = (modifiers: J.Modifier[]) => modifiers.map(m => names(m.annotations));
    const members = (cu: JS.CompilationUnit) => {
        const first = cu.statements[0].element;
        const body = first.kind === JS.Kind.TypeDeclaration ?
            ((first as JS.TypeDeclaration).initializer.element as JS.TypeLiteral).members :
            (first as J.ClassDeclaration).body;
        return body.statements.map(s => s.element);
    };

    test.for([
        "export @dec function f() {}",
        "@a export @b function f() {}",
        "export default @dec function f() {}",
        "export @dec async function f() {}",
        "@a export @b declare type T = string;",
        "@dec declare module 'm' { }",
        "@dec declare global { }",
        "declare import a from 'a';",
        "export import a from 'a';",
        "declare export * from 'x';",
        "@dec export as namespace N;",
        "declare export as namespace N;",
        "class A { @dec static [key: string]: any }",
        "interface I { public x: number; static m(): void }",
        "interface I { static [Symbol.iterator](): void }",
        "type L = { readonly x: number; public m(): void }",
        "({ export @dec m() {}, export @dec get x() { return 1 }, export @dec set x(v) {} });",
        "({ export @dec get [k]() { return 1 } });",
        "({ export @dec public x: 1 });",
        "/*0*/ @a /*1*/ export /*2*/ @b /*3*/ function /*4*/ f() {}",
        "/*0*/ @a /*1*/ declare /*2*/ import /*3*/ a from 'a';",
        "/*0*/ @a /*1*/ declare /*2*/ export /*3*/ * from 'x';",
        "/*0*/ @a /*1*/ declare /*2*/ export /*3*/ as /*4*/ namespace /*5*/ N;",
        "class A { /*0*/ @a /*1*/ static /*2*/ [key: string]: any }",
        "interface I { /*0*/ public /*1*/ static /*2*/ m(): void }",
        "function f(export @dec x) {}",
        "function f(export @dec x: number, y = 1) {}",
        "function f(export @dec x?: number) {}",
        "function f(export @dec x = 1) {}",
        "function f(export @dec {x}) {}",
        "function f(export @dec {x}: Foo = {}) {}",
        "function f(@a export @b x) {}",
        "function f(@a export @b @c x) {}",
        "function f(export @dec public x) {}",
        "function f(export @dec public ...x) {}",
        "function f(/*0*/ @a /*1*/ export /*2*/ @b /*3*/ x /*4*/) {}",
        "function f(/*0*/ export /*1*/ @b /*2*/ {x} /*3*/) {}",
        "class A { constructor(export @dec x) {} }",
        "class A { m(@a export @b x) {} }",
        "class A { set x(export @dec v) {} }",
        "class A { [k](export @dec v) {} }",
        "(function (export @dec x) {});",
        "interface I { m(export @dec x): void }",
        "interface I { (export @dec x): void }",
        "interface I { new(export @dec x): I }",
        "type F = new (export @dec x) => void;",
        "type L = { m(export @dec x): void }",
        "({ m(export @dec x) {} });",
        "({ set x(export @dec v) {} });",
        "class A { m(@dec ...x: any[]) {} }",
        "class A { m(@dec [a, b]: any[]) {} }",
        "class A { m(@dec {a}: any) {} }",
        "class A { @dec [k]() {} }",
        "const A = @a export @b class {};",
        "const A = @dec abstract class {};",
        "const A = @a export @b abstract class B {};",
        "(@a export default @b class {});",
    ])('%s', (code) =>
        spec.rewriteRun(
            //language=typescript
            typescript(code)
        ));

    // What the compiler rejects and the model has no place for must not come back as a tree that has lost it.
    test.for([
        ["@dec type T = string;", "Decorators are not valid here."],
        ["export @dec type T = string;", "Decorators are not valid here."],
        ["@a export @b type T = string;", "Decorators are not valid here."],
        ["@dec namespace N { }", "Decorators are not valid here."],
        ["export @dec namespace A.B { }", "Decorators are not valid here."],
        ["@dec import a = b.c;", "Decorators are not valid here."],
        ["export @dec import a = b.c;", "Decorators are not valid here."],
        ["@dec import a from 'a';", "Decorators are not valid here."],
        ["@dec import 'a';", "Decorators are not valid here."],
        ["@dec export * from 'x';", "Decorators are not valid here."],
        ["@dec export { a };", "Decorators are not valid here."],
        ["@dec export default a;", "Decorators are not valid here."],
        ["@dec export = a;", "Decorators are not valid here."],
        ["declare export default a;", "Modifiers cannot appear here."],
        ["declare export = a;", "Modifiers cannot appear here."],
        ["class A { @dec [key: string]: any }", "Decorators are not valid here."],
        ["class A { @dec static { } }", "Decorators are not valid here."],
        ["class A { public static { } }", "Modifiers cannot appear here."],
        ["class A { @dec public static { } }", "Decorators are not valid here."],
        ["({ export @dec x: 1 });", "Decorators are not valid here."],
        ["({ export @dec x });", "Decorators are not valid here."],
        ["({ export @dec [k]: 1 });", "Decorators are not valid here."],
        ["({ export @dec [k]() {} });", "Decorators are not valid here."],
        ["({ export @dec *[k]() {} });", "Decorators are not valid here."],
        ["function f(export @dec [x]) {}", "Decorators are not valid here."],
        ["function f(export @dec ...x) {}", "Decorators are not valid here."],
        ["function f(export @dec ...[x]) {}", "Decorators are not valid here."],
        ["class A { m(export @dec ...x) {} }", "Decorators are not valid here."],
        ["interface I { m(export @dec [x]): void }", "Decorators are not valid here."],
    ] as const)('rejects %s', async ([code, reason]) => {
        for (const requirePrintEqualsInput of [true, false]) {
            const ctx = new ExecutionContext({[ExecutionContext.REQUIRE_PRINT_EQUALS_INPUT]: requirePrintEqualsInput});
            const parsed: SourceFile[] = [];
            for await (const sourceFile of new JavaScriptParser({ctx}).parse(
                {text: code, sourcePath: "rejected.ts"},
                {text: "const valid = 1;", sourcePath: "valid.ts"})) {
                parsed.push(sourceFile);
            }

            const [rejected, valid] = parsed;
            expect(rejected.kind).toEqual(ParseErrorKind);
            expect((rejected as ParseError).text).toEqual(code);
            expect(findMarker<ParseExceptionResult>(rejected, MarkersKind.ParseExceptionResult)!.message).toContain(reason);
            expect(valid.kind).toEqual(JS.Kind.CompilationUnit);
        }
    });

    test('function declaration', () =>
        spec.rewriteRun({
            //language=typescript
            ...typescript('@a export @b function f() {}\nexport default @c function g() {}\nexport @d async function h() {}'),
            afterRecipe: (cu: JS.CompilationUnit) => {
                const [f, g, h] = cu.statements.map(s => s.element as J.MethodDeclaration);
                expect(names(f.leadingAnnotations)).toEqual(["a"]);
                expect(keywords(f.modifiers)).toEqual(["export"]);
                expect(names(f.nameAnnotations)).toEqual(["b"]);

                expect(keywords(g.modifiers)).toEqual(["export", "default"]);
                expect(names(g.nameAnnotations)).toEqual(["c"]);

                expect(modifierAnnotations(h.modifiers)).toEqual([[], ["d"]]);
                expect(h.nameAnnotations).toEqual([]);
            }
        }));

    test('a decorator is an annotation of the modifier written after it', () =>
        spec.rewriteRun({
            //language=typescript
            ...typescript("@a export @b declare type T = string;\n@c declare module 'm' { }\nconst o = { export @d public x: 1 };\nclass A { @e static [key: string]: any }"),
            afterRecipe: (cu: JS.CompilationUnit) => {
                const [type, module, declarations, clazz] = cu.statements.map(s => s.element);
                expect(keywords((type as JS.TypeDeclaration).modifiers)).toEqual(["export", "declare"]);
                expect(modifierAnnotations((type as JS.TypeDeclaration).modifiers)).toEqual([["a"], ["b"]]);
                expect(modifierAnnotations((module as JS.NamespaceDeclaration).modifiers)).toEqual([["c"]]);

                const literal = (declarations as J.VariableDeclarations).variables[0].element.initializer!.element as J.NewClass;
                const property = literal.body!.statements[0].element as JS.PropertyAssignment;
                expect(keywords(property.modifiers)).toEqual(["export", J.ModifierType.Public]);
                expect(modifierAnnotations(property.modifiers)).toEqual([[], ["d"]]);

                const signature = (clazz as J.ClassDeclaration).body.statements[0].element as JS.IndexSignatureDeclaration;
                expect(keywords(signature.modifiers)).toEqual([J.ModifierType.Static]);
                expect(modifierAnnotations(signature.modifiers)).toEqual([["e"]]);
            }
        }));

    test('export as namespace', () =>
        spec.rewriteRun({
            //language=typescript
            ...typescript('@a export as namespace N;\n@b declare export as namespace M;'),
            afterRecipe: (cu: JS.CompilationUnit) => {
                const [n, m] = cu.statements.map(s => s.element as JS.NamespaceDeclaration);
                expect(keywords(n.modifiers)).toEqual(["export", "as"]);
                expect(modifierAnnotations(n.modifiers)).toEqual([["a"], []]);

                expect(keywords(m.modifiers)).toEqual(["declare", "export", "as"]);
                expect(modifierAnnotations(m.modifiers)).toEqual([["b"], [], []]);
            }
        }));

    test('import and export declaration', () =>
        spec.rewriteRun({
            //language=typescript
            ...typescript("const first = 1;\n@a  declare   import b from 'b';\nexport import c from 'c';\n@d declare  export * from 'x';"),
            afterRecipe: (cu: JS.CompilationUnit) => {
                const [, b, c, star] = cu.statements.map(s => s.element as JS.Import | JS.ExportDeclaration);
                expect(keywords(b.modifiers)).toEqual(["declare"]);
                expect(modifierAnnotations(b.modifiers)).toEqual([["a"]]);
                // the first thing written carries the leading whitespace, and the import what is left before its keyword
                expect(b.modifiers[0].annotations[0].prefix.whitespace).toEqual("\n");
                expect(b.modifiers[0].prefix.whitespace).toEqual("  ");
                expect(b.prefix.whitespace).toEqual("   ");

                expect(keywords(c.modifiers)).toEqual(["export"]);
                expect(c.prefix.whitespace).toEqual(" ");

                expect(star.kind).toEqual(JS.Kind.ExportDeclaration);
                expect(keywords(star.modifiers)).toEqual(["declare"]);
                expect(modifierAnnotations(star.modifiers)).toEqual([["d"]]);
                expect(star.prefix.whitespace).toEqual("  ");
            }
        }));

    test('method signature', async () => {
        await spec.rewriteRun({
            //language=typescript
            ...typescript('interface I { public x: number; static m(): void; static [Symbol.iterator](): void }'),
            afterRecipe: (cu: JS.CompilationUnit) => {
                const [x, m, computed] = members(cu);
                expect(keywords((x as J.VariableDeclarations).modifiers)).toEqual([J.ModifierType.Public]);
                expect(keywords((m as J.MethodDeclaration).modifiers)).toEqual([J.ModifierType.Static]);
                expect(keywords((computed as JS.ComputedPropertyMethodDeclaration).modifiers)).toEqual([J.ModifierType.Static]);
            }
        });
        await spec.rewriteRun({
            //language=typescript
            ...typescript('type L = { readonly x: number; public m(): void }'),
            afterRecipe: (cu: JS.CompilationUnit) => {
                const [x, m] = members(cu);
                expect(keywords((x as J.VariableDeclarations).modifiers)).toEqual(["readonly"]);
                expect(keywords((m as J.MethodDeclaration).modifiers)).toEqual([J.ModifierType.Public]);
            }
        });
    });

    test('object literal method and accessor', () =>
        spec.rewriteRun({
            //language=typescript
            ...typescript('const o = { export @a m() {}, export @b get x() { return 1 }, export @c get [k]() { return 1 } };'),
            afterRecipe: (cu: JS.CompilationUnit) => {
                const declarations = cu.statements[0].element as J.VariableDeclarations;
                const literal = declarations.variables[0].element.initializer!.element as J.NewClass;
                const [m, x, computed] = literal.body!.statements.map(s => s.element as J.MethodDeclaration);
                expect(keywords(m.modifiers)).toEqual(["export"]);
                expect(names(m.nameAnnotations)).toEqual(["a"]);

                expect(keywords(x.modifiers)).toEqual(["export", "get"]);
                expect(modifierAnnotations(x.modifiers)).toEqual([[], ["b"]]);
                expect(x.nameAnnotations).toEqual([]);

                expect(computed.kind).toEqual(JS.Kind.ComputedPropertyMethodDeclaration);
                expect(modifierAnnotations(computed.modifiers)).toEqual([[], ["c"]]);
            }
        }));

    test('parameter', () =>
        spec.rewriteRun({
            //language=typescript
            ...typescript('function f(@a export @b x, export  @c   {y}, export @d public w, @e ...rest) {}'),
            afterRecipe: (cu: JS.CompilationUnit) => {
                const method = cu.statements[0].element as J.MethodDeclaration;
                const [x, y, w, rest] = method.parameters.elements.map(p => p.element as J.VariableDeclarations);
                const name = (parameter: J.VariableDeclarations) => parameter.variables[0].element.name;

                expect(names(x.leadingAnnotations)).toEqual(["a"]);
                expect(keywords(x.modifiers)).toEqual(["export"]);
                expect(names((name(x) as J.Identifier).annotations)).toEqual(["b"]);

                const object = name(y) as JS.ObjectBindingPattern;
                expect(object.kind).toEqual(JS.Kind.ObjectBindingPattern);
                expect(y.leadingAnnotations).toEqual([]);
                expect(names(object.leadingAnnotations)).toEqual(["c"]);
                // a pattern prints its annotations after its prefix, so they are followed by the space before the brace
                expect(object.prefix.whitespace).toEqual("");
                expect(object.leadingAnnotations[0].prefix.whitespace).toEqual("  ");
                expect(object.bindings.before.whitespace).toEqual("   ");

                expect(keywords(w.modifiers)).toEqual(["export", J.ModifierType.Public]);
                expect(modifierAnnotations(w.modifiers)).toEqual([[], ["d"]]);
                expect((name(w) as J.Identifier).annotations).toEqual([]);

                expect(names(rest.leadingAnnotations)).toEqual(["e"]);
                expect(name(rest).kind).toEqual(JS.Kind.Spread);
            }
        }));

    test('class expression', () =>
        spec.rewriteRun({
            //language=typescript
            ...typescript('const A = @a export @b abstract class {};\nconst B = @c export @d class {};\nconst C = @e abstract class {};'),
            afterRecipe: (cu: JS.CompilationUnit) => {
                const [a, b, c] = cu.statements.map(s => {
                    const declarations = s.element as J.VariableDeclarations;
                    return (declarations.variables[0].element.initializer!.element as JS.StatementExpression).statement as J.ClassDeclaration;
                });
                expect(names(a.leadingAnnotations)).toEqual(["a"]);
                expect(keywords(a.modifiers)).toEqual(["export", J.ModifierType.Abstract]);
                expect(modifierAnnotations(a.modifiers)).toEqual([[], ["b"]]);
                expect(a.classKind.annotations).toEqual([]);

                expect(names(b.leadingAnnotations)).toEqual(["c"]);
                expect(keywords(b.modifiers)).toEqual(["export"]);
                expect(names(b.classKind.annotations)).toEqual(["d"]);

                expect(names(c.leadingAnnotations)).toEqual(["e"]);
                expect(keywords(c.modifiers)).toEqual([J.ModifierType.Abstract]);
            }
        }));
});
