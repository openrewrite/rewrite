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
import {javascript, JavaScriptVisitor, RewriteRule} from "../../../src/javascript";
import {capture, pattern, rewrite, template} from "../../../src/javascript";
import {J} from "../../../src/java";

/** Applies `rule` to every `marker(...)` call, keeping each test's rewrite to one trigger point. */
function onCall(rule: RewriteRule) {
    return fromVisitor(new class extends JavaScriptVisitor<any> {
        override async visitMethodInvocation(method: J.MethodInvocation, p: any): Promise<J | undefined> {
            const visited = await super.visitMethodInvocation(method, p) as J.MethodInvocation;
            return await rule.tryOn(this.cursor, visited) || visited;
        }
    });
}

describe('template precedence', () => {
    const spec = new RecipeSpec();

    describe('a substituted capture that binds more loosely than its slot', () => {
        test('under a prefix operator', () => {
            spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
                override async visitTernary(ternary: J.Ternary, p: any): Promise<J | undefined> {
                    const visited = await super.visitTernary(ternary, p) as J.Ternary;
                    const t = capture();
                    return await rewrite(() => ({
                        before: pattern`${t} ? false : true`,
                        after: template`!${t}`
                    })).tryOn(this.cursor, visited) || visited;
                }
            });
            //language=javascript
            return spec.rewriteRun(javascript(
                `
                    const x = a || b ? false : true;
                    const y = a === b ? false : true;
                    const z = foo() ? false : true;
                    const w = a.b ? false : true;
                `,
                `
                    const x = !(a || b);
                    const y = !(a === b);
                    const z = !foo();
                    const w = !a.b;
                `));
        });

        test('as the left operand of a tighter-binding operator', () => {
            const x = capture();
            spec.recipe = onCall(rewrite(() => ({
                before: pattern`square(${x})`,
                after: template`${x} * ${x}`
            })));
            //language=javascript
            return spec.rewriteRun(javascript(
                `
                    const a = square(n + 1);
                    const b = square(n / 2);
                `,
                `
                    const a = (n + 1) * (n + 1);
                    const b = n / 2 * (n / 2);
                `));
        });

        test('as the right operand of an equally binding operator', () => {
            const x = capture();
            spec.recipe = onCall(rewrite(() => ({
                before: pattern`below(${x})`,
                after: template`10 - ${x}`
            })));
            //language=javascript
            return spec.rewriteRun(javascript(
                `
                    const a = below(n - 1);
                    const b = below(n * 2);
                `,
                `
                    const a = 10 - (n - 1);
                    const b = 10 - n * 2;
                `));
        });

        test('as the operand of typeof', () => {
            const x = capture();
            spec.recipe = onCall(rewrite(() => ({
                before: pattern`isBool(${x})`,
                after: template`typeof ${x} === 'boolean'`
            })));
            //language=javascript
            return spec.rewriteRun(javascript(
                `
                    const a = isBool(p || q);
                    const b = isBool(flag);
                `,
                `
                    const a = typeof (p || q) === 'boolean';
                    const b = typeof flag === 'boolean';
                `));
        });

        test('as the target of a member access', () => {
            const x = capture();
            spec.recipe = onCall(rewrite(() => ({
                before: pattern`size(${x})`,
                after: template`${x}.length`
            })));
            //language=javascript
            return spec.rewriteRun(javascript(
                `
                    const a = size(p || q);
                    const b = size(items);
                `,
                `
                    const a = (p || q).length;
                    const b = items.length;
                `));
        });

        test('as the select of a method call', () => {
            const x = capture();
            spec.recipe = onCall(rewrite(() => ({
                before: pattern`str(${x})`,
                after: template`${x}.toString()`
            })));
            //language=javascript
            return spec.rewriteRun(javascript(
                `
                    const a = str(p ? q : r);
                    const b = str(value);
                `,
                `
                    const a = (p ? q : r).toString();
                    const b = value.toString();
                `));
        });

        test('as the callee of a call, which only `JS.FunctionCall` can hold', () => {
            const x = capture();
            spec.recipe = onCall(rewrite(() => ({
                before: pattern`register(${x})`,
                after: template`${x}()`
            })));
            //language=javascript
            return spec.rewriteRun(javascript(
                `
                    register(() => 1);
                    register(handlers.init);
                    register(initAuth);
                `,
                `
                    (() => 1)();
                    handlers.init();
                    initAuth();
                `));
        });

        test('as the condition of a ternary', () => {
            const c = capture(), t = capture(), f = capture();
            spec.recipe = onCall(rewrite(() => ({
                before: pattern`pick(${c}, ${t}, ${f})`,
                after: template`${c} ? ${t} : ${f}`
            })));
            //language=javascript
            return spec.rewriteRun(javascript(
                `
                    const a = pick(p ? q : r, one, two);
                    const b = pick(p && q, one, two);
                `,
                `
                    const a = (p ? q : r) ? one : two;
                    const b = p && q ? one : two;
                `));
        });

        test('as an argument, where nothing binds it', () => {
            const x = capture();
            spec.recipe = onCall(rewrite(() => ({
                before: pattern`old(${x})`,
                after: template`renamed(${x})`
            })));
            //language=javascript
            return spec.rewriteRun(javascript(
                `const a = old(p || q);`,
                `const a = renamed(p || q);`));
        });

        test('does not double up on parentheses the template already writes', () => {
            const x = capture();
            spec.recipe = onCall(rewrite(() => ({
                before: pattern`group(${x})`,
                after: template`(${x})`
            })));
            //language=javascript
            return spec.rewriteRun(javascript(
                `const a = group(p || q);`,
                `const a = (p || q);`));
        });

        test('keeps a trailing `!` or `?.` outside the parentheses', () => {
            const x = capture(), y = capture();
            spec.recipe = onCall(
                rewrite(() => ({before: pattern`nonNull(${x})`, after: template`${x}!.length`}))
                    .orElse(rewrite(() => ({before: pattern`optional(${y})`, after: template`${y}?.length`}))));
            //language=typescript
            return spec.rewriteRun(javascript(
                `
                    const a = nonNull(p || q);
                    const b = optional(p || q);
                `,
                `
                    const a = (p || q)!.length;
                    const b = (p || q)?.length;
                `));
        });

        test('does not double up on parentheses the capture already carries', () => {
            const x = capture();
            spec.recipe = onCall(rewrite(() => ({
                before: pattern`not(${x})`,
                after: template`!${x}`
            })));
            //language=javascript
            return spec.rewriteRun(javascript(
                `const a = not((p || q));`,
                `const a = !(p || q);`));
        });
    });

    describe('operand restrictions that precedence alone does not express', () => {
        test('`??` may not sit next to `||` or `&&`', () => {
            const x = capture(), y = capture();
            spec.recipe = onCall(rewrite(() => ({
                before: pattern`orDefault(${x}, ${y})`,
                after: template`${x} ?? ${y}`
            })));
            //language=javascript
            return spec.rewriteRun(javascript(
                `
                    const a = orDefault(p || q, fallback);
                    const b = orDefault(p & q, fallback);
                `,
                `
                    const a = (p || q) ?? fallback;
                    const b = p & q ?? fallback;
                `));
        });

        test('`||` may not sit next to `??`', () => {
            const x = capture(), y = capture();
            spec.recipe = onCall(rewrite(() => ({
                before: pattern`either(${x}, ${y})`,
                after: template`${x} || ${y}`
            })));
            //language=javascript
            return spec.rewriteRun(javascript(
                `const a = either(p ?? q, r);`,
                `const a = (p ?? q) || r;`));
        });

        test('the base of `**` may not be a prefix expression', () => {
            const x = capture(), y = capture();
            spec.recipe = onCall(rewrite(() => ({
                before: pattern`pow(${x}, ${y})`,
                after: template`${x} ** ${y}`
            })));
            //language=javascript
            return spec.rewriteRun(javascript(
                `
                    const a = pow(-n, 2);
                    const b = pow(n, m ** 2);
                    const c = pow(n ** m, 2);
                `,
                `
                    const a = (-n) ** 2;
                    const b = n ** m ** 2;
                    const c = (n ** m) ** 2;
                `));
        });

        test('a sign may not fuse with the sign that precedes it', () => {
            const x = capture();
            spec.recipe = onCall(rewrite(() => ({
                before: pattern`negate(${x})`,
                after: template`-${x}`
            })));
            //language=javascript
            return spec.rewriteRun(javascript(
                `
                    const a = -negate(n);
                    const b = 1 -negate(n);
                    const c = 1 - negate(n);
                    const d = 1 +negate(n);
                `,
                `
                    const a = -(-n);
                    const b = 1 -(-n);
                    const c = 1 - -n;
                    const d = 1 +-n;
                `));
        });

        test('a prefix operator may not fuse with the one below it', () => {
            const x = capture();
            spec.recipe = onCall(rewrite(() => ({
                before: pattern`negate(${x})`,
                after: template`-${x}`
            })));
            //language=javascript
            return spec.rewriteRun(javascript(
                `
                    const a = negate(-n);
                    const b = negate(+n);
                    const c = negate(n);
                `,
                `
                    const a = -(-n);
                    const b = -+n;
                    const c = -n;
                `));
        });
    });

    describe('slots that restrict the shape of their operand, not just its precedence', () => {
        test('an object literal as an arrow function body reads as a block', () => {
            const x = capture();
            spec.recipe = onCall(rewrite(() => ({
                before: pattern`thunk(${x})`,
                after: template`() => ${x}`
            })));
            //language=javascript
            return spec.rewriteRun(javascript(
                `
                    const a = thunk({k: 1});
                    const b = thunk({k: 1}.k);
                    const c = thunk(n + 1);
                `,
                `
                    const a = () => ({k: 1});
                    const b = () => ({k: 1}.k);
                    const c = () => n + 1;
                `));
        });

        test('the callee of `new` may not be a call', () => {
            const x = capture();
            spec.recipe = onCall(rewrite(() => ({
                before: pattern`mk(${x})`,
                after: template`new ${x}()`
            })));
            //language=javascript
            return spec.rewriteRun(javascript(
                `
                    const a = mk(factory());
                    const b = mk(registry().Cls);
                    const c = mk(ns.Cls);
                `,
                `
                    const a = new (factory())();
                    const b = new (registry().Cls)();
                    const c = new ns.Cls();
                `));
        });

        test('the callee of `new` may not be an optional chain', () => {
            const x = capture();
            spec.recipe = onCall(rewrite(() => ({
                before: pattern`mk(${x})`,
                after: template`new ${x}()`
            })));
            //language=javascript
            return spec.rewriteRun(javascript(
                `const a = mk(ns?.Cls);`,
                `const a = new (ns?.Cls)();`));
        });

        test('the tag of a tagged template may not be an optional chain', () => {
            const x = capture();
            spec.recipe = onCall(rewrite(() => ({
                before: pattern`t(${x})`,
                after: template`${x}\`hi\``
            })));
            //language=javascript
            return spec.rewriteRun(javascript(
                `
                    const a = t(o?.tag);
                    const b = t(o.tag);
                `,
                `
                    const a = (o?.tag)\`hi\`;
                    const b = o.tag\`hi\`;
                `));
        });

        test('an integer literal followed by `.` would lex as a decimal point', () => {
            const x = capture();
            spec.recipe = onCall(rewrite(() => ({
                before: pattern`str(${x})`,
                after: template`${x}.toString()`
            })));
            //language=javascript
            return spec.rewriteRun(javascript(
                `
                    const a = str(1);
                    const b = str(1.5);
                    const c = str(0x10);
                `,
                `
                    const a = (1).toString();
                    const b = 1.5.toString();
                    const c = 0x10.toString();
                `));
        });

        test('an optional access ends the number, so no parentheses are needed', () => {
            const x = capture();
            spec.recipe = onCall(rewrite(() => ({
                before: pattern`str(${x})`,
                after: template`${x}?.length`
            })));
            //language=javascript
            return spec.rewriteRun(javascript(
                `const a = str(1);`,
                `const a = 1?.length;`));
        });

        test('an object literal at the start of a statement reads as a block', () => {
            const x = capture();
            spec.recipe = onCall(rewrite(() => ({
                before: pattern`use(${x})`,
                after: template`${x}`
            })));
            //language=javascript
            return spec.rewriteRun(javascript(
                `
                    use({k: 1});
                    use({k: 1}.k);
                    use({k: 1} && f());
                    use([1, 2]);
                `,
                `
                    ({k: 1});
                    ({k: 1}.k);
                    ({k: 1} && f());
                    [1, 2];
                `));
        });

        test('a function or class expression at the start of a statement reads as a declaration', () => {
            const x = capture();
            spec.recipe = onCall(rewrite(() => ({
                before: pattern`use(${x})`,
                after: template`${x}`
            })));
            //language=javascript
            return spec.rewriteRun(javascript(
                `
                    function h() {
                        use(function () {});
                        use(class {});
                    }
                `,
                `
                    function h() {
                        (function () {
                        });
                        (class {
                        });
                    }
                `));
        });

        test('a function expression as a callee reads as a declaration', () => {
            const x = capture();
            spec.recipe = onCall(rewrite(() => ({
                before: pattern`register(${x})`,
                after: template`${x}()`
            })));
            //language=javascript
            return spec.rewriteRun(javascript(
                `register(function () { return 1; });`,
                `(function () {\n    return 1;\n})();`));
        });

        test('a class heritage clause takes a left-hand-side expression', () => {
            const x = capture();
            spec.recipe = onCall(rewrite(() => ({
                before: pattern`base(${x})`,
                after: template`class A extends ${x} {}`
            })));
            //language=javascript
            return spec.rewriteRun(javascript(
                `const a = base(p || q);`,
                `const a = class A extends (p || q) {\n};`));
        });
    });

    describe('a template result that binds more loosely than the tree it replaces', () => {
        const notEquals = () => {
            const a = capture(), b = capture();
            return rewrite(() => ({
                before: pattern`!(${a} === ${b})`,
                after: template`${a} !== ${b}`
            }));
        };

        test('under a tighter-binding parent', () => {
            const rule = notEquals();
            spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
                override async visitUnary(unary: J.Unary, p: any): Promise<J | undefined> {
                    const visited = await super.visitUnary(unary, p) as J.Unary;
                    return await rule.tryOn(this.cursor, visited) || visited;
                }
            });
            //language=javascript
            return spec.rewriteRun(javascript(
                `
                    const a = !(x === y) + 1;
                    const b = !(x === y) * 2;
                    const c = -!(x === y);
                `,
                `
                    const a = (x !== y) + 1;
                    const b = (x !== y) * 2;
                    const c = -(x !== y);
                `));
        });

        test('under a parent that binds it loosely enough', () => {
            const rule = notEquals();
            spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
                override async visitUnary(unary: J.Unary, p: any): Promise<J | undefined> {
                    const visited = await super.visitUnary(unary, p) as J.Unary;
                    return await rule.tryOn(this.cursor, visited) || visited;
                }
            });
            //language=javascript
            return spec.rewriteRun(javascript(
                `
                    const a = !(x === y);
                    const b = !(x === y) && z;
                    const c = foo(!(x === y));
                    const d = !(x === y) ? 1 : 2;
                    const e = [!(x === y)];
                `,
                `
                    const a = x !== y;
                    const b = x !== y && z;
                    const c = foo(x !== y);
                    const d = x !== y ? 1 : 2;
                    const e = [x !== y];
                `));
        });

        test('when the result becomes the select of a member access', () => {
            const x = capture();
            spec.recipe = onCall(rewrite(() => ({
                before: pattern`inc(${x})`,
                after: template`${x} + 1`
            })));
            //language=javascript
            return spec.rewriteRun(javascript(
                `const a = inc(n).toString();`,
                `const a = (n + 1).toString();`));
        });

        test('when the result comes from a chained rule', () => {
            const x = capture(), y = capture();
            spec.recipe = onCall(
                rewrite(() => ({before: pattern`outer(${x})`, after: template`inner(${x})`}))
                    .andThen(rewrite(() => ({before: pattern`inner(${y})`, after: template`${y} + 1`}))));
            //language=javascript
            return spec.rewriteRun(javascript(
                `const a = outer(n).toString();`,
                `const a = (n + 1).toString();`));
        });

        test('the parentheses the source already had are kept', () => {
            spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
                override async visitTernary(ternary: J.Ternary, p: any): Promise<J | undefined> {
                    const visited = await super.visitTernary(ternary, p) as J.Ternary;
                    const t = capture();
                    return await rewrite(() => ({
                        before: pattern`${t} ? false : true`,
                        after: template`!${t}`
                    })).tryOn(this.cursor, visited) || visited;
                }
            });
            // The parentheses came from the source and are not the engine's to remove

            //language=javascript
            return spec.rewriteRun(javascript(
                `const y = 1 + (a === b ? false : true);`,
                `const y = 1 + (!(a === b));`));
        });
    });
});
