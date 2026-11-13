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
import {javascript, JavaScriptVisitor, JS, typescript} from "../../../src/javascript";
import {UseObjectPropertyShorthand} from "../../../src/javascript/cleanup";
import {J, Type} from "../../../src/java";

describe("UseObjectPropertyShorthand", () => {
    const spec = new RecipeSpec();
    spec.recipe = new UseObjectPropertyShorthand();

    test("collapses a destructured property whose name matches its binding", () => spec.rewriteRun(
        //language=javascript
        javascript(
            `const { foo: foo, bar: baz, qux: qux, ...props } = obj;`,
            `const { foo, bar: baz, qux, ...props } = obj;`
        )
    ));

    test("leaves a destructured property alone when it renames or is already shorthand", () => spec.rewriteRun(
        //language=javascript
        javascript(
            `const { x: y, z } = obj;`
        )
    ));

    test("collapses a destructured property wherever the binding appears", () => spec.rewriteRun(
        //language=javascript
        javascript(
            `function foo({ x: x }) { const { outer: { inner: inner } } = x; return inner; }`,
            `function foo({ x }) { const { outer: { inner } } = x; return inner; }`
        )
    ));

    test("collapses an object literal property whose value is its own name", () => spec.rewriteRun(
        //language=javascript
        javascript(
            `const obj = { foo: foo, bar: baz, qux: qux };`,
            `const obj = { foo, bar: baz, qux };`
        )
    ));

    test("leaves an object literal property alone when it renames or is already shorthand", () => spec.rewriteRun(
        //language=javascript
        javascript(
            `const obj = { x: y, z };`
        )
    ));

    test("collapses an object literal property wherever the literal appears", () => spec.rewriteRun(
        //language=javascript
        javascript(
            `function getData() { return doSomething({ name: name, value: value }); }`,
            `function getData() { return doSomething({ name, value }); }`
        )
    ));

    test("preserves type attribution after simplification", async () => spec.rewriteRun({
        //language=typescript
        ...typescript(
            `let foo: boolean; const obj = { foo: foo };`,
            `let foo: boolean; const obj = { foo };`
        ),
        afterRecipe: async (cu: JS.CompilationUnit) => {
            let foundShorthandProperty = false;

            await new class extends JavaScriptVisitor<void> {
                protected async visitPropertyAssignment(prop: JS.PropertyAssignment, _: void): Promise<J | undefined> {
                    // Find the shorthand property (no initializer means shorthand)
                    if (!prop.initializer) {
                        foundShorthandProperty = true;
                        const nameIdent = prop.name.element as J.Identifier;
                        expect(nameIdent.simpleName).toBe('foo');
                        // The identifier should retain type information as Primitive.Boolean
                        expect(nameIdent.type).toBe(Type.Primitive.Boolean);
                    }
                    return prop;
                }
            }().visit(cu, undefined);

            expect(foundShorthandProperty).toBe(true);
        }
    }));

    test("does not simplify object literal with non-null assertion", () => spec.rewriteRun(
        //language=typescript
        typescript(
            `const icon: string | null = null; const obj = { icon: icon! };`
        )
    ));

    test("does not simplify destructuring with non-null assertion in default value", () => spec.rewriteRun(
        //language=typescript
        typescript(
            `const obj = { x: null }; const { x = x! } = obj;`
        )
    ));
});
