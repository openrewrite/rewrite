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
import {capture, JavaScriptVisitor, pattern, rewrite, template, typescript} from "../../../src/javascript";
import {J} from "../../../src/java";

describe('a statement replacing the body of another', () => {
    const spec = new RecipeSpec();

    function replaceCalls(build: () => ReturnType<typeof template>) {
        const x = capture('x');
        const rule = rewrite(() => ({before: pattern`foo(${x})`, after: build()}));
        return fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visit<R extends J>(tree: any, p: any, parent?: any): Promise<R | undefined> {
                const visited = await super.visit<R>(tree, p, parent);
                if (visited?.kind === J.Kind.MethodInvocation && this.cursor.parentTree()?.value.kind !== J.Kind.MethodInvocation) {
                    return ((await rule.tryOn(this.cursor, visited)) ?? visited) as R;
                }
                return visited;
            }
        });
    }

    function replaceBlocksUnder(kind: string, code: () => ReturnType<typeof template>) {
        return fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitBlock(block: J.Block, p: any): Promise<J | undefined> {
                return this.cursor.parentTree()?.value.kind === kind ? code().apply(block, this.cursor) : block;
            }
        });
    }

    test('a block in place of a call does not keep its semicolon before an else', () => {
        spec.recipe = replaceCalls(() => template`{ bar(); }`);
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `
                if (c) foo(1); else d();
                `,
                `
                if (c) {
                    bar();
                } else d();
                `
            )
        );
    });

    test('a loop in place of a labeled call does not keep its semicolon', () => {
        spec.recipe = replaceCalls(() => template`while (c) bar();`);
        return spec.rewriteRun(
            //language=typescript
            typescript(`l: foo(1);`, `l: while (c) bar();`)
        );
    });

    test('a call in place of a block gets a semicolon before an else', () => {
        spec.recipe = replaceBlocksUnder(J.Kind.If, () => template`bar()`);
        return spec.rewriteRun(
            //language=typescript
            typescript(`if (c) { a(); } else d();`, `if (c) bar(); else d();`)
        );
    });

    test('a call in place of a block gets a semicolon before the while of a do', () => {
        spec.recipe = replaceBlocksUnder(J.Kind.DoWhileLoop, () => template`bar()`);
        return spec.rewriteRun(
            //language=typescript
            typescript(`do { a(); } while (c);`, `do bar(); while (c);`)
        );
    });

    test('a call in place of a block before an else on the next line needs no semicolon', () => {
        spec.recipe = replaceBlocksUnder(J.Kind.If, () => template`bar()`);
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `
                if (c) {
                    a();
                }
                else d();
                `,
                `
                if (c) bar()
                else d();
                `
            )
        );
    });

    describe('after a statement written without its semicolon', () => {
        test('a statement starting with a parenthesis does not continue it', () => {
            spec.recipe = replaceCalls(() => template`(a || b)()`);
            return spec.rewriteRun(
                //language=typescript
                typescript(
                    `
                    const x = y
                    foo(1)
                    `,
                    `
                    const x = y;
                    (a || b)()
                    `
                )
            );
        });

        test('a statement starting with a bracket does not continue it', () => {
            spec.recipe = replaceCalls(() => template`[a, b].forEach(g)`);
            return spec.rewriteRun(
                //language=typescript
                typescript(
                    `
                    g(y)
                    foo(1)
                    `,
                    `
                    g(y);
                    [a, b].forEach(g)
                    `
                )
            );
        });

        test('a statement starting with a template literal does not continue it', () => {
            spec.recipe = replaceCalls(() => template`\`\${a}\`.trim()`);
            return spec.rewriteRun(
                //language=typescript
                typescript(
                    `
                    const x = y
                    foo(1)
                    `,
                    `
                    const x = y;
                    \`\${a}\`.trim()
                    `
                )
            );
        });

        test('a statement that cannot continue it stays as it is', () => {
            spec.recipe = replaceCalls(() => template`bar(1)`);
            return spec.rewriteRun(
                //language=typescript
                typescript(
                    `
                    const x = y
                    foo(1)
                    `,
                    `
                    const x = y
                    bar(1)
                    `
                )
            );
        });
    });
});
