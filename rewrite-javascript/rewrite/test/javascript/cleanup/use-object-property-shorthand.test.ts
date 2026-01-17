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

    test("simplifies { x: x } to { x }", () => spec.rewriteRun(
        //language=javascript
        javascript(
            `const { x: x } = obj;`,
            `const { x } = obj;`
        )
    ));

    test("simplifies multiple properties", () => spec.rewriteRun(
        //language=javascript
        javascript(
            `const { foo: foo, bar: bar } = obj;`,
            `const { foo, bar } = obj;`
        )
    ));

    test("simplifies with rest element", () => spec.rewriteRun(
        //language=javascript
        javascript(
            `const { ref: ref, ...props } = obj;`,
            `const { ref, ...props } = obj;`
        )
    ));

    test("does not change when names differ", () => spec.rewriteRun(
        //language=javascript
        javascript(
            `const { x: y } = obj;`
        )
    ));

    test("handles mixed properties", () => spec.rewriteRun(
        //language=javascript
        javascript(
            `const { foo: foo, bar: baz, qux: qux } = obj;`,
            `const { foo, bar: baz, qux } = obj;`
        )
    ));

    test("preserves already shorthand properties", () => spec.rewriteRun(
        //language=javascript
        javascript(
            `const { x } = obj;`
        )
    ));

    test("handles nested destructuring", () => spec.rewriteRun(
        //language=javascript
        javascript(
            `const { outer: { inner: inner } } = obj;`,
            `const { outer: { inner } } = obj;`
        )
    ));

    test("handles function parameters", () => spec.rewriteRun(
        //language=javascript
        javascript(
            `function foo({ x: x, y: y }) { return x + y; }`,
            `function foo({ x, y }) { return x + y; }`
        )
    ));

    test("handles arrow function parameters", () => spec.rewriteRun(
        //language=javascript
        javascript(
            `const fn = ({ a: a }) => a;`,
            `const fn = ({ a }) => a;`
        )
    ));

    // Object literal tests
    test("simplifies object literal { x: x } to { x }", () => spec.rewriteRun(
        //language=javascript
        javascript(
            `const obj = { x: x };`,
            `const obj = { x };`
        )
    ));

    test("simplifies multiple object literal properties", () => spec.rewriteRun(
        //language=javascript
        javascript(
            `const obj = { foo: foo, bar: bar };`,
            `const obj = { foo, bar };`
        )
    ));

    test("handles mixed object literal properties", () => spec.rewriteRun(
        //language=javascript
        javascript(
            `const obj = { foo: foo, bar: baz, qux: qux };`,
            `const obj = { foo, bar: baz, qux };`
        )
    ));

    test("does not change object literal when names differ", () => spec.rewriteRun(
        //language=javascript
        javascript(
            `const obj = { x: y };`
        )
    ));

    test("preserves already shorthand object literal properties", () => spec.rewriteRun(
        //language=javascript
        javascript(
            `const obj = { x };`
        )
    ));

    test("simplifies object literal in function call", () => spec.rewriteRun(
        //language=javascript
        javascript(
            `doSomething({ monitorId: monitorId, token: token });`,
            `doSomething({ monitorId, token });`
        )
    ));

    test("simplifies object literal in return statement", () => spec.rewriteRun(
        //language=javascript
        javascript(
            `function getData() { return { name: name, value: value }; }`,
            `function getData() { return { name, value }; }`
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
                        const nameIdent = prop.name as unknown as J.Identifier;
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
