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
import {capture, JavaScriptVisitor, pattern, raw, rewrite, template, typescript} from "../../../src/javascript";
import {J} from "../../../src/java";

describe('a template whose code does not parse', () => {
    const spec = new RecipeSpec();

    /** `raw()` splices verbatim, which is the way to build a template the parser rejects. */
    function recipeApplying(tmpl: ReturnType<typeof template>) {
        const rule = rewrite(() => ({before: pattern`use(${capture()})`, after: tmpl}));
        return fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(method: J.MethodInvocation, p: any): Promise<J | undefined> {
                method = await super.visitMethodInvocation(method, p) as J.MethodInvocation;
                return await rule.tryOn(this.cursor, method, {visitor: this}) || method;
            }
        });
    }

    test('reports the engine error, naming the syntax problem, where the bindings are read', async () => {
        spec.recipe = recipeApplying(template`new ${raw('() => 1')}()`);

        await expect(spec.rewriteRun(
            //language=typescript
            typescript(`use(z);`)
        )).rejects.toThrow(/Failed to parse template code \(.*expected.*\):/);
    });
});
