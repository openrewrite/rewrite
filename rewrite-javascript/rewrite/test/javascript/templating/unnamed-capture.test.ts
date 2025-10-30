/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import {fromVisitor, RecipeSpec} from "../../../src/test";
import {capture, JavaScriptVisitor, pattern, rewrite, template, typescript} from "../../../src/javascript";
import {Expression, J} from "../../../src/java";

describe('unnamed capture', () => {
    const spec = new RecipeSpec();

    test('more complex example', () => {

        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {

            protected override async visitExpression(expr: Expression, p: any): Promise<J | undefined> {
                const left = capture();
                const right = capture();

                //language=typescript
                let m = await pattern`${left} + ${right}`.match(expr) ||
                    await pattern`${left} * ${right}`.match(expr);

                return m && await template`${right} + ${left}`.apply(this.cursor, expr, m) || expr;
            }
        });

        return spec.rewriteRun(
            //language=typescript
            typescript(`
                const a = 2 * 2 + 3;
            `, `
                const a = 3 + 2 + 2;
            `),
        );
    });

    test('more complex example using replace', () => {

        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitTernary(ternary: J.Ternary, p: any): Promise<J | undefined> {
                const obj = capture();
                const property = capture();
                const defaultValue = capture();

                //language=typescript
                return await rewrite(() => ({
                    before: [
                        pattern`${obj} === null || ${obj} === undefined ? ${defaultValue} : ${obj}.${property}`,
                        pattern`${obj} === undefined || ${obj} === null ? ${defaultValue} : ${obj}.${property}`
                    ],
                    after: template`${obj}?.${property} ?? ${defaultValue}`
                }))
                    .tryOn(this.cursor, ternary) || super.visitTernary(ternary, p);
            }
        });

        //language=typescript
        return spec.rewriteRun(
            typescript(`
                function getName(user) {
                    return user === null || user === undefined ? "default" : user.name;
                }
            `, `
                function getName(user) {
                    return user?.name ?? "default";
                }
            `),
            typescript(`
                function getName(user) {
                    return user === undefined || user === null ? "default" : user.name;
                }
            `, `
                function getName(user) {
                    return user?.name ?? "default";
                }
            `)
        );
    });
});
