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
import {JavaScriptVisitor, pattern, rewrite, template, typescript} from "../../../src/javascript";
import {J} from "../../../src/java";

describe('unnamed capture', () => {
    const spec = new RecipeSpec();

    test('more complex example', () => {

        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitTernary(ternary: J.Ternary, p: any): Promise<J | undefined> {

                let m = await pattern`${"obj"} === null || ${"obj"} === undefined ? ${"defaultValue"} : ${"obj"}.${"property"}`
                    .match(ternary);
                if (m) {
                    return await template`${"obj"}?.${"property"} ?? ${"defaultValue"}`.apply(this.cursor, {tree: ternary}, m);
                }

                return await super.visitTernary(ternary, p);
            }
        });

        return spec.rewriteRun(
            //language=typescript
            typescript(`
                function getName(user) {
                    return user === null || user === undefined ? "default" : user.name;
                }
            `, `
                function getName(user) {
                    return user?.name ?? "default";
                }
            `),
        );
    });

    test('more complex example using replace', () => {

        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitTernary(ternary: J.Ternary, p: any): Promise<J | undefined> {

                return await rewrite(() => ({
                    before: pattern`${"obj"} === null || ${"obj"} === undefined ? ${"defaultValue"} : ${"obj"}.${"property"}`,
                    after: template`${"obj"}?.${"property"} ?? ${"defaultValue"}`
                }))
                    .tryOn(ternary, this.cursor) || super.visitTernary(ternary, p);
            }
        });

        return spec.rewriteRun(
            //language=typescript
            typescript(`
                function getName(user) {
                    return user === null || user === undefined ? "default" : user.name;
                }
            `, `
                function getName(user) {
                    return user?.name ?? "default";
                }
            `),
        );
    });
});
