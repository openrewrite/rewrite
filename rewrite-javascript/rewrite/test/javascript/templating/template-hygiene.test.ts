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
import {capture, JavaScriptVisitor, JS, pattern, rewrite, RewriteRule, template, typescript} from "../../../src/javascript";
import {J} from "../../../src/java";

describe('a name the template declares', () => {
    const spec = new RecipeSpec();

    function rewriteReturns(rule: RewriteRule) {
        return fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitReturn(ret: J.Return, p: any): Promise<J | undefined> {
                return (await rule.tryOn(this.cursor, ret)) ?? ret;
            }
        });
    }

    test('is renamed where a substituted value reads that name from further out', () => {
        const arr = capture('arr');
        const cb = capture('cb');
        spec.recipe = rewriteReturns(rewrite(() => ({
            before: pattern`return ${arr}.some(${cb})`,
            after: template`{
                for (const item of ${arr}) {
                    if (${cb}(item)) return true;
                }
                return false;
            }`
        })));
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `
                function f(xs: number[], item: number) {
                    return xs.some(x => x === item);
                }
                `,
                `
                function f(xs: number[], item: number) {
                    {
                        for (const item_1 of xs) {
                            if ((x => x === item)(item_1)) return true;
                        }
                        return false;
                    }
                }
                `
            )
        );
    });

    test('keeps its name where no substituted value reads it', () => {
        const arr = capture('arr');
        const cb = capture('cb');
        spec.recipe = rewriteReturns(rewrite(() => ({
            before: pattern`return ${arr}.some(${cb})`,
            after: template`{
                for (const item of ${arr}) {
                    if (${cb}(item)) return true;
                }
                return false;
            }`
        })));
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `
                function f(xs: number[], y: number) {
                    return xs.some(x => x === y);
                }
                `,
                `
                function f(xs: number[], y: number) {
                    {
                        for (const item of xs) {
                            if ((x => x === y)(item)) return true;
                        }
                        return false;
                    }
                }
                `
            )
        );
    });

    test('keeps its name where the replaced code declared it, so the value reads the new declaration', () => {
        const arr = capture('arr');
        const body = capture('body');
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitForOfLoop(loop: JS.ForOfLoop, p: any): Promise<J | undefined> {
                return (await rewrite(() => ({
                    before: pattern`for (const item of ${arr}) ${body}`,
                    after: template`${arr}.forEach(item => ${body})`
                })).tryOn(this.cursor, loop)) ?? loop;
            }
        });
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `
                for (const item of xs) {
                    use(item);
                }
                `,
                `
                xs.forEach(item => {
                    use(item);
                })
                `
            )
        );
    });

    test('is renamed only where it binds, not where it names a member', () => {
        const value = capture('value');
        spec.recipe = rewriteReturns(rewrite(() => ({
            before: pattern`return ${value} + 1`,
            after: template`{
                const tmp = {tmp: ${value}};
                return tmp.tmp;
            }`
        })));
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `
                function f(tmp: number) {
                    return tmp + 1;
                }
                `,
                `
                function f(tmp: number) {
                    {
                        const tmp_1 = {tmp: tmp};
                        return tmp_1.tmp;
                    }
                }
                `
            )
        );
    });
});
