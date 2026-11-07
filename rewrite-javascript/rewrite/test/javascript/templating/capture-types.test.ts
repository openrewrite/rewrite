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
import {fromVisitor, RecipeSpec} from "../../../src/test";
import {capture, JavaScriptVisitor, JS, param, pattern, rewrite, template, typescript} from "../../../src/javascript";
import {emptySpace, J, Type} from "../../../src/java";
import {TemplateEngine} from "../../../src/javascript/templating/engine";
import {emptyMarkers} from "../../../src/markers";
import {randomId} from "../../../src/uuid";

/** A bound value carrying nothing but a type. */
function typed(type: Type): J.Identifier {
    return {kind: J.Kind.Identifier, id: randomId(), prefix: emptySpace, markers: emptyMarkers, annotations: [], simpleName: 'v', type};
}

describe('capture types', () => {
    const spec = new RecipeSpec();

    test('capture with string type annotation', () => {
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitBinary(binary: J.Binary, p: any): Promise<J | undefined> {
                // Create a capture with a type annotation
                const x = capture({type: 'boolean'});
                const pat = pattern`${x} || false`;
                const match = await pat.match(binary, this.cursor);

                if (match) {
                    // Replace with a simpler expression
                    return template`${x}`.apply(binary, this.cursor, { values: match });
                }
                return binary;
            }
        });
        return spec.rewriteRun(
            //language=typescript
            typescript('const a = true || false', 'const a = true'),
        );
    });

    test('template with capture type generates properly typed AST', () => {
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitBinary(binary: J.Binary, p: any): Promise<J | undefined> {
                // Create a capture with a type annotation
                const condition = capture({type: 'boolean'});
                const pat = pattern`${condition} && true`;
                const match = await pat.match(binary, this.cursor);

                if (match) {
                    // Use the capture in a template - type should be preserved
                    return template`!${condition}`.apply(binary, this.cursor, { values: match });
                }
                return binary;
            }
        });
        return spec.rewriteRun(
            //language=typescript
            typescript('const a = flag && true', 'const a = !flag'),
        );
    });

    test('multiple captures with different types', () => {
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitBinary(binary: J.Binary, p: any): Promise<J | undefined> {
                // Create captures with different type annotations
                const x = capture({type: 'number'});
                const y = capture({type: 'number'});
                const pat = pattern`${x} + ${y}`;
                const match = await pat.match(binary, this.cursor);

                if (match) {
                    // Swap the operands
                    return template`${y} + ${x}`.apply(binary, this.cursor, { values: match });
                }
                return binary;
            }
        });
        spec.cycles = 1;
        return spec.rewriteRun(
            //language=typescript
            typescript('const a = 1 + 2', 'const a = 2 + 1'),
        );
    });

    test('a typed capture in receiver position still binds', () => {
        // The type is what gives the pattern the target's own `methodType`, which settles the call
        const router = capture({type: 'Router'});
        const ctx = {context: ['declare class Router { getCurrentNavigation(): void; currentNavigation(): void; }']};
        const rule = rewrite(() => ({
            before: pattern`${router}.getCurrentNavigation()`.configure(ctx),
            after: template`${router}.currentNavigation()`.configure(ctx)
        }));
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(method: J.MethodInvocation, p: any): Promise<J | undefined> {
                const m = await super.visitMethodInvocation(method, p) as J.MethodInvocation;
                return await rule.tryOn(this.cursor, m, {visitor: this}) ?? m;
            }
        });
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `
                class Router {
                    getCurrentNavigation() {}

                    currentNavigation() {}
                }

                new Router().getCurrentNavigation();`,
                `
                class Router {
                    getCurrentNavigation() {}

                    currentNavigation() {}
                }

                new Router().currentNavigation();`
            )
        );
    });

    test('an untyped capture is typed by the value bound to it', () => {
        // `Buffer.from` is overloaded and generic, so the overload it resolves to depends on the argument's type
        const s = capture();
        const rule = rewrite(() => ({
            before: pattern`new Buffer(${s})`,
            after: template`Buffer.from(${s})`
        }));
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitNewClass(newClass: J.NewClass, p: any): Promise<J | undefined> {
                return await rule.tryOn(this.cursor, newClass) ?? newClass;
            }
        });
        return spec.rewriteRun({
            //language=typescript
            ...typescript(`
                const s = "abc";
                const b = new Buffer(s);`,
                `
                const s = "abc";
                const b = Buffer.from(s);`),
            afterRecipe: (cu: JS.CompilationUnit) => {
                const b = (cu.statements[1].element as J.VariableDeclarations).variables[0].element;
                const returnType = (b.initializer!.element as J.MethodInvocation).methodType!.returnType as Type.Parameterized;
                expect((returnType.typeParameters[0] as Type.Class).fullyQualifiedName).toBe('ArrayBuffer');
            }
        });
    });

    test('a parameter without a type of its own takes the type of the expression bound to it, which a list or a statement lacks', () => {
        const node = capture({name: 'node', type: 'Foo'});
        const values = new Map<string, J | J[]>([
            ['node', {...typed(Type.Primitive.String), name: typed(Type.Primitive.Double)} as J],
            ['flag', typed(Type.Primitive.Boolean)],
            ['rest', [typed(Type.Primitive.String)]]
        ]);
        expect(TemplateEngine.parameterPreamble([
            {value: (node as any).name},
            {value: param('flag')},
            {value: capture({name: 'rest', variadic: true})},
            {value: {...typed(Type.Primitive.String), kind: J.Kind.ClassDeclaration}},
            {value: {...typed(Type.Primitive.String), kind: JS.Kind.As}}
        ], values)).toEqual(['let __PLACEHOLDER_0__: number;', 'let __PLACEHOLDER_1__: boolean;', 'let __PLACEHOLDER_4__: string;']);
    });

    test('a parameter of a generic type is declared with its type arguments', () => {
        const x = capture('x');
        const rule = rewrite(() => ({before: pattern`has(${x})`, after: template`${x}.includes(1)`}))
            .orElse(rewrite(() => ({before: pattern`lookup(${x})`, after: template`${x}.get("k")`})));
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(method: J.MethodInvocation, p: any): Promise<J | undefined> {
                const m = await super.visitMethodInvocation(method, p) as J.MethodInvocation;
                return await rule.tryOn(this.cursor, m) ?? m;
            }
        });
        return spec.rewriteRun({
            //language=typescript
            ...typescript(
                `
                const arr: number[] = [1];
                const map = new Map<string, number>();
                has(arr);
                lookup(map);`,
                `
                const arr: number[] = [1];
                const map = new Map<string, number>();
                arr.includes(1);
                map.get("k");`
            ),
            afterRecipe: (cu: JS.CompilationUnit) => {
                const [includes, get] = [2, 3].map(i => (cu.statements[i].element as J.MethodInvocation).methodType!);
                expect((includes.declaringType as Type.Class).fullyQualifiedName).toBe('Array');
                expect(includes.returnType).toBe(Type.Primitive.Boolean);
                expect((get.declaringType as Type.Class).fullyQualifiedName).toBe('Map');
                expect(get.returnType).toBe(Type.Primitive.Double);
            }
        });
    });

    test('a parameter of a recursive generic type is declared with any where the type holds itself', () => {
        const x = capture('x');
        const rule = rewrite(() => ({before: pattern`has(${x})`, after: template`${x}.includes(1)`}));
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(method: J.MethodInvocation, p: any): Promise<J | undefined> {
                const m = await super.visitMethodInvocation(method, p) as J.MethodInvocation;
                return await rule.tryOn(this.cursor, m) ?? m;
            }
        });
        return spec.rewriteRun({
            //language=typescript
            ...typescript(
                `
                type Nested = Array<Nested>;
                const nested: Nested = [];
                has(nested);`,
                `
                type Nested = Array<Nested>;
                const nested: Nested = [];
                nested.includes(1);`
            ),
            afterRecipe: (cu: JS.CompilationUnit) => {
                const includes = (cu.statements[2].element as J.MethodInvocation).methodType!;
                expect((includes.declaringType as Type.Class).fullyQualifiedName).toBe('Array');
            }
        });
    });

    test('a union, an intersection, a type variable or a generic type left without its arguments is declared as a parse reads it', () => {
        const x = capture('x');
        const rule = rewrite(() => ({before: pattern`use(${x})`, after: template`${x}.valueOf()`}));
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(method: J.MethodInvocation, p: any): Promise<J | undefined> {
                const m = await super.visitMethodInvocation(method, p) as J.MethodInvocation;
                return await rule.tryOn(this.cursor, m) ?? m;
            }
        });
        const name = (t: Type): string => Type.isClass(t) ? t.fullyQualifiedName : Type.isPrimitive(t) ? t.keyword :
            t.kind === Type.Kind.Union ? (t as Type.Union).bounds.map(name).join(' | ') : 'unknown';
        return spec.rewriteRun({
            //language=typescript
            ...typescript(
                `
                    function f<T, D extends Date>(
                        xs: Array<T>, t: T, d: D, u: string | number, i: Date & {n: 1}, b: bigint
                    ) {
                        use(xs);
                        use(t);
                        use(d);
                        use(u);
                        use(i);
                        use(b);
                    }
                `,
                `
                    function f<T, D extends Date>(
                        xs: Array<T>, t: T, d: D, u: string | number, i: Date & {n: 1}, b: bigint
                    ) {
                        xs.valueOf();
                        t.valueOf();
                        d.valueOf();
                        u.valueOf();
                        i.valueOf();
                        b.valueOf();
                    }
                `
            ),
            afterRecipe: (cu: JS.CompilationUnit) => {
                const calls = (cu.statements[0].element as J.MethodDeclaration).body!.statements
                    .map(s => (s.element as J.MethodInvocation).methodType!);
                // What a parse of the same code gives each call
                expect(calls.map(m => `${name(m.declaringType)} ${name(m.returnType)}`)).toEqual([
                    'Object Object', 'Object Object', 'Date double',
                    'unknown String | double', 'Date double', 'BigInt long'
                ]);
            }
        });
    });

    test('a type is declared as TypeScript writes it, in parentheses where it binds less tightly than where it stands', () => {
        const variable = (name: string, ...bounds: Type[]) => ({
            kind: Type.Kind.GenericTypeVariable, name, variance: Type.GenericTypeVariable.Variance.Invariant, bounds
        } as Type.GenericTypeVariable);
        const cls = (fullyQualifiedName: string, ...typeParameters: Type[]) =>
            ({kind: Type.Kind.Class, fullyQualifiedName, typeParameters} as unknown as Type.Class);
        const union = (...bounds: Type[]) => ({kind: Type.Kind.Union, bounds} as Type.Union);
        const intersection = (...bounds: Type[]) => ({kind: Type.Kind.Intersection, bounds} as Type.Intersection);
        const array = (elemType: Type) => ({kind: Type.Kind.Array, elemType, annotations: []} as Type.Array);
        const {String: str, Double: num, Null: nul, None: undef} = Type.Primitive;
        expect(TemplateEngine.parameterPreamble([
            cls('Box', variable('T', str)),
            intersection(union(str, num), cls('Date'), cls('{}')),
            array(union(str, undef)),
            union(str, str, nul),
            // A module-qualified class can't be named, and an unconstrained type variable could be anything
            union(str, cls('src/a.Foo')),
            union(str, variable('T'))
        ].map(type => ({value: typed(type)})))).toEqual([
            'let __PLACEHOLDER_0__: Box<string>;',
            'let __PLACEHOLDER_1__: (string | number) & Date;',
            'let __PLACEHOLDER_2__: (string | undefined)[];',
            'let __PLACEHOLDER_3__: string | null;',
            'let __PLACEHOLDER_5__: unknown;'
        ]);
    });

    test('capture without type still works', () => {
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitBinary(binary: J.Binary, p: any): Promise<J | undefined> {
                // Create a capture without a type annotation
                const x = capture();
                const pat = pattern`${x} + 1`;
                const match = await pat.match(binary, this.cursor);

                if (match) {
                    return template`${x} * 2`.apply(binary, this.cursor, { values: match });
                }
                return binary;
            }
        });
        return spec.rewriteRun(
            //language=typescript
            typescript('const a = 5 + 1', 'const a = 5 * 2'),
        );
    });

    test('a parameter whose type is module-qualified still splices', () => {
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(method: J.MethodInvocation, p: any): Promise<J | undefined> {
                const m = await super.visitMethodInvocation(method, p) as J.MethodInvocation;
                return m.name.simpleName === 'use' ?
                    template`wrap(${m.arguments.elements[0].element})`.apply(m, this.cursor) : m;
            }
        });
        return spec.rewriteRun({
            //language=typescript
            ...typescript(
                `
                export class Foo {
                }

                const f = new Foo();
                use(f);`,
                `
                export class Foo {
                }

                const f = new Foo();
                wrap(f);`
            ),
            // The module path is what qualifies `Foo`, giving the parameter a type no preamble can name
            path: 'src/a.ts'
        });
    });

    test('type derivation from J element in template', () => {
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(method: J.MethodInvocation, p: any): Promise<J | undefined> {
                const methodName = method.name as J.Identifier;
                if (methodName.simpleName === 'toUpperCase' && method.select) {
                    // The select element is a string literal with natural type attribution from the parser
                    // Pass J element with type directly to template - type should be derived from the string type
                    return template`${method.select.element}.toLowerCase()`.apply(method, this.cursor);
                }
                return method;
            }
        });
        return spec.rewriteRun({
            //language=typescript
            ...typescript('"hello".toUpperCase()', '"hello".toLowerCase()'),
            afterRecipe: (cu: JS.CompilationUnit) => {
                const methodType = (cu.statements[0].element as J.MethodInvocation).methodType!;
                expect((methodType.declaringType as Type.Class).fullyQualifiedName).toBe('String');
                expect(methodType.name).toBe('toLowerCase');
            }
        });
    });

    test('type derivation from multiple J elements', () => {
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitBinary(binary: J.Binary, p: any): Promise<J | undefined> {
                if (binary.operator.element === J.Binary.Type.Addition) {
                    // Both operands are number literals with natural type attribution from the parser
                    // Pass both operands as J elements - types should be derived from the number types
                    return template`${binary.right} + ${binary.left}`.apply(binary, this.cursor);
                }
                return binary;
            }
        });
        spec.cycles = 1;
        return spec.rewriteRun(
            //language=typescript
            typescript('const a = 1 + 2', 'const a = 2 + 1'),
        );
    });

    test('Type.Primitive.Boolean maps to boolean', () => {
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitBinary(binary: J.Binary, p: any): Promise<J | undefined> {
                // Create a capture with Type.Primitive.Boolean
                const condition = capture({type: Type.Primitive.Boolean});
                const pat = pattern`${condition} && true`;
                const match = await pat.match(binary, this.cursor);

                if (match) {
                    return template`!${condition}`.apply(binary, this.cursor, { values: match });
                }
                return binary;
            }
        });
        return spec.rewriteRun(
            //language=typescript
            typescript('const a = flag && true', 'const a = !flag'),
        );
    });

    test('Type.Primitive.String maps to string', () => {
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitBinary(binary: J.Binary, p: any): Promise<J | undefined> {
                // Create a capture with Type.Primitive.String
                const str = capture({type: Type.Primitive.String});
                const pat = pattern`${str} + ""`;
                const match = await pat.match(binary, this.cursor);

                if (match) {
                    return template`${str}`.apply(binary, this.cursor, { values: match });
                }
                return binary;
            }
        });
        return spec.rewriteRun(
            //language=typescript
            typescript('const a = "hello" + ""', 'const a = "hello"'),
        );
    });

    test('Type.Primitive.Double maps to number', () => {
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitBinary(binary: J.Binary, p: any): Promise<J | undefined> {
                // Create a capture with Type.Primitive.Double
                const num = capture({type: Type.Primitive.Double});
                const pat = pattern`${num} + 0`;
                const match = await pat.match(binary, this.cursor);

                if (match) {
                    return template`${num}`.apply(binary, this.cursor, { values: match });
                }
                return binary;
            }
        });
        return spec.rewriteRun(
            //language=typescript
            typescript('const a = 42 + 0', 'const a = 42'),
        );
    });

    test('Type.Array renders component type plus []', () => {
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(method: J.MethodInvocation, p: any): Promise<J | undefined> {
                const methodName = method.name as J.Identifier;
                if (methodName.simpleName === 'oldMethod') {
                    // Create an array type: string[]
                    const arrayType: Type.Array = {
                        kind: Type.Kind.Array,
                        elemType: Type.Primitive.String,
                        annotations: []
                    } as Type.Array;

                    // Use the array type in a capture within a pattern
                    const arr = capture({type: arrayType});
                    const pat = pattern`oldMethod(${arr})`;
                    const match = await pat.match(method, this.cursor);

                    if (match) {
                        return template`newMethod(${arr})`.apply(method, this.cursor, { values: match });
                    }
                }
                return method;
            }
        });
        return spec.rewriteRun(
            //language=typescript
            typescript('oldMethod(items)', 'newMethod(items)'),
        );
    });

    test('a parameter\'s type is not borrowed from a neighbour of the same shape', async () => {
        // Each pair below agrees on parts, names and arity, so the AST cache either separates them
        // on the parameter's type or serves the first one's tree for both
        const strPattern = pattern`${capture({name: 'v', type: Type.Primitive.String})} + z`;
        expect(((await strPattern.getAstPattern()) as J.Binary).left.type).toBe(Type.Primitive.String);
        const numPattern = pattern`${capture({name: 'v', type: Type.Primitive.Double})} + z`;
        expect(((await numPattern.getAstPattern()) as J.Binary).left.type).toBe(Type.Primitive.Double);

        // Templates key on parameter position rather than name, which collides just as readily
        const strTemplate = template`${capture({name: 'v', type: Type.Primitive.String})}`;
        expect((await (strTemplate as any).getTemplateTree() as J.Identifier).type).toBe(Type.Primitive.String);
        const numTemplate = template`${capture({name: 'v', type: Type.Primitive.Double})}`;
        expect((await (numTemplate as any).getTemplateTree() as J.Identifier).type).toBe(Type.Primitive.Double);

        // One template bound to values of different types parses once for each
        const untyped = template`${capture('v')}`;
        expect((await (untyped as any).getTemplateTree(false, new Map([['v', typed(Type.Primitive.String)]])) as J.Identifier).type)
            .toBe(Type.Primitive.String);
        expect((await (untyped as any).getTemplateTree(false, new Map([['v', typed(Type.Primitive.Double)]])) as J.Identifier).type)
            .toBe(Type.Primitive.Double);
        expect(await (untyped as any).getTemplateTree(false, new Map([['v', typed(Type.Primitive.String)]])))
            .toBe(await (untyped as any).getTemplateTree(false, new Map([['v', typed(Type.Primitive.String)]])));
    });
});
