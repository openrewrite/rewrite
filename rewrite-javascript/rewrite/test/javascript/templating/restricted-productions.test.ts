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
import {capture, JavaScriptVisitor, pattern, rewrite, Template, template, typescript} from "../../../src/javascript";
import {J} from "../../../src/java";

describe('a value substituted after a keyword that a line break ends', () => {
    const spec = new RecipeSpec();

    function replaceCallStatements(after: (x: ReturnType<typeof capture>) => Template) {
        const x = capture('x');
        const rule = rewrite(() => ({before: pattern`foo(${x})`, after: after(x)}));
        return fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(method: J.MethodInvocation, p: any): Promise<J | undefined> {
                return this.cursor.parentTree()?.value.kind === J.Kind.Block ?
                    (await rule.tryOn(this.cursor, method)) ?? method : method;
            }
        });
    }

    test('stays the value returned where a line comment comes before it', () => {
        spec.recipe = replaceCallStatements(x => template`return ${x}`);
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `
                function f() {
                    foo(// why
                        a);
                }
                `,
                `
                function f() {
                    return (// why
                        a);
                }
                `
            )
        );
    });

    test('stays the value thrown where a line comment comes before it', () => {
        spec.recipe = replaceCallStatements(x => template`throw ${x}`);
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `
                function f() {
                    foo(// why
                        a);
                }
                `,
                `
                function f() {
                    throw (// why
                        a);
                }
                `
            )
        );
    });

    test('keeps a comment on its own line where the keyword is not one of them', () => {
        spec.recipe = replaceCallStatements(x => template`bar(${x})`);
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `
                function f() {
                    foo(// why
                        a);
                }
                `,
                `
                function f() {
                    bar(// why
                        a);
                }
                `
            )
        );
    });
});
