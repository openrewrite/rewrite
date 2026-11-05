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
import {fromVisitor, RecipeSpec} from "../../../src/test";
import {capture, javascript, JavaScriptVisitor, JS, pattern, rewrite, template, typescript} from "../../../src/javascript";
import {Expression, J} from "../../../src/java";

describe('template token separation', () => {
    const spec = new RecipeSpec();

    const doubleNegationToBoolean = () => fromVisitor(new class extends JavaScriptVisitor<any> {
        override async visitUnary(unary: J.Unary, p: any): Promise<J | undefined> {
            const visited = await super.visitUnary(unary, p) as J.Unary;
            const x = capture();
            return await rewrite(() => ({
                before: pattern`!!${x}`,
                after: template`Boolean(${x})`
            })).tryOn(this.cursor, visited) || visited;
        }
    });

    const dotNotation = () => fromVisitor(new class extends JavaScriptVisitor<any> {
        override async visitArrayAccess(arrayAccess: J.ArrayAccess, p: any): Promise<J | undefined> {
            const visited = await super.visitArrayAccess(arrayAccess, p) as J.ArrayAccess;
            const o = capture();
            return await rewrite(() => ({
                before: pattern`${o}["bar"]`,
                after: template`${o}.bar`
            })).tryOn(this.cursor, visited) || visited;
        }
    });

    test('after `typeof`', () => {
        spec.recipe = doubleNegationToBoolean();
        //language=javascript
        return spec.rewriteRun(javascript(
            `const t = typeof!!foo;`,
            `const t = typeof Boolean(foo);`
        ));
    });

    test('after `return`', () => {
        spec.recipe = doubleNegationToBoolean();
        //language=javascript
        return spec.rewriteRun(javascript(
            `function f() { return!!foo; }`,
            `function f() { return Boolean(foo); }`
        ));
    });

    test('before `in`', () => {
        spec.recipe = dotNotation();
        //language=javascript
        return spec.rewriteRun(javascript(
            `if (foo["bar"]in x) {}`,
            `if (foo.bar in x) {}`
        ));
    });

    test('unwrapped parentheses after `return`', () => {
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitParentheses<T extends J>(parens: J.Parentheses<T>, p: any): Promise<J | undefined> {
                const visited = await super.visitParentheses(parens, p) as J.Parentheses<T>;
                const x = capture();
                return await rewrite(() => ({
                    before: pattern`(${x})`,
                    after: template`${x}`
                })).tryOn(this.cursor, visited) || visited;
            }
        });
        //language=javascript
        return spec.rewriteRun(javascript(
            `function f(x) { return(x); }`,
            `function f(x) { return x; }`
        ));
    });

    test('before `as`', () => {
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitBinary(binary: J.Binary, p: any): Promise<J | undefined> {
                const visited = await super.visitBinary(binary, p) as J.Binary;
                const a = capture(), b = capture();
                return await rewrite(() => ({
                    before: pattern`${a} < ${b}`,
                    after: template`${b} > ${a}`
                })).tryOn(this.cursor, visited) || visited;
            }
        });
        //language=typescript
        return spec.rewriteRun(typescript(
            `const a = 1 < f()as any;`,
            `const a = f() > 1 as any;`
        ));
    });

    test('before `instanceof`, `of`, `satisfies` and `as const`', () => {
        spec.recipe = dotNotation();
        //language=typescript
        return spec.rewriteRun(typescript(
            `
                const a = foo["bar"]instanceof X;
                for (foo["bar"]of xs) {}
                const b = foo["bar"]satisfies T;
                const c = foo["bar"]as const;
            `,
            `
                const a = foo.bar instanceof X;
                for (foo.bar of xs) {}
                const b = foo.bar satisfies T;
                const c = foo.bar as const;
            `
        ));
    });

    test('at the end of an operand that a keyword follows', () => {
        spec.recipe = dotNotation();
        //language=javascript
        return spec.rewriteRun(javascript(
            `if (a + foo["bar"]in x) {}`,
            `if (a + foo.bar in x) {}`
        ));
    });

    test('applied to a child from the visit of its parent', () => {
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitTypeOf(typeOf: JS.TypeOf, p: any): Promise<J | undefined> {
                const visited = await super.visitTypeOf(typeOf, p) as JS.TypeOf;
                const x = capture();
                const replaced = await rewrite(() => ({
                    before: pattern`!!${x}`,
                    after: template`Boolean(${x})`
                })).tryOn(this.cursor, visited.expression);
                return replaced ? {...visited, expression: replaced as Expression} as JS.TypeOf : visited;
            }

            override async visitBinaryExtensions(binary: JS.Binary, p: any): Promise<J | undefined> {
                const visited = await super.visitBinaryExtensions(binary, p) as JS.Binary;
                const o = capture();
                const replaced = await rewrite(() => ({
                    before: pattern`${o}["bar"]`,
                    after: template`${o}.bar`
                })).tryOn(this.cursor, visited.left);
                return replaced ? {...visited, left: replaced as Expression} as JS.Binary : visited;
            }
        });
        //language=javascript
        return spec.rewriteRun(javascript(
            `
                const t = typeof!!foo;
                const u = foo["bar"]in x;
            `,
            `
                const t = typeof Boolean(foo);
                const u = foo.bar in x;
            `
        ));
    });

    test('before `in` where a visitor replaced the tree printing it', () => {
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitExpression(expression: Expression, p: any): Promise<J | undefined> {
                return expression.kind === JS.Kind.Binary && (expression as JS.Binary).left.kind === J.Kind.ArrayAccess ?
                    {...expression} : expression;
            }

            override async visitArrayAccess(arrayAccess: J.ArrayAccess, p: any): Promise<J | undefined> {
                const visited = await super.visitArrayAccess(arrayAccess, p) as J.ArrayAccess;
                const o = capture();
                return await rewrite(() => ({
                    before: pattern`${o}["bar"]`,
                    after: template`${o}.bar`
                })).tryOn(this.cursor, visited) || visited;
            }
        });
        //language=javascript
        return spec.rewriteRun(javascript(
            `if (foo["bar"]in x) {}`,
            `if (foo.bar in x) {}`
        ));
    });

    test('leaves tokens that do not fuse as they are', () => {
        spec.recipe = doubleNegationToBoolean();
        //language=javascript
        return spec.rewriteRun(javascript(
            `
                const a = typeof !!foo;
                const b = typeof/* c */!!foo;
                const c = [!!foo];
                const d = !!foo+1;
            `,
            `
                const a = typeof Boolean(foo);
                const b = typeof/* c */Boolean(foo);
                const c = [Boolean(foo)];
                const d = Boolean(foo)+1;
            `
        ));
    });
});
