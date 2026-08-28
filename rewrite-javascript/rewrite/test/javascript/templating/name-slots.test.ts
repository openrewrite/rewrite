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
import {capture, javascript, JavaScriptVisitor, pattern, rewrite, RewriteRule, template} from "../../../src/javascript";
import {J} from "../../../src/java";

function onCall(rule: RewriteRule) {
    return fromVisitor(new class extends JavaScriptVisitor<any> {
        override async visitMethodInvocation(method: J.MethodInvocation, p: any): Promise<J | undefined> {
            const visited = await super.visitMethodInvocation(method, p) as J.MethodInvocation;
            return await rule.tryOn(this.cursor, visited) || visited;
        }
    });
}

describe('a slot that spells out a name', () => {
    const spec = new RecipeSpec();

    test('a member name takes a name, not an expression', async () => {
        const x = capture();
        spec.recipe = onCall(rewrite(() => ({
            before: pattern`read(${x})`,
            after: template`obj.${x}`
        })));
        //language=javascript
        await expect(spec.rewriteRun(javascript(`read(a || b);`)))
            .rejects.toThrow(/cannot be a member name: write `\$\{a}\[\$\{b}]`/);

        spec.recipe = onCall(rewrite(() => ({
            before: pattern`read(${x})`,
            after: template`obj.${x}()`
        })));
        //language=javascript
        await expect(spec.rewriteRun(javascript(`read(a || b);`)))
            .rejects.toThrow(/cannot be a member name: write `\$\{a}\[\$\{b}]\(\)`/);
    });

    test('a property name takes a name, not an expression', () => {
        const x = capture();
        spec.recipe = onCall(rewrite(() => ({
            before: pattern`read(${x})`,
            after: template`({${x}: 1})`
        })));
        //language=javascript
        return expect(spec.rewriteRun(javascript(`read(a || b);`)))
            .rejects.toThrow(/cannot be a property name: write `\{\[\$\{k}]: v}`/);
    });

    test('the forms a property key may take keep working', () => {
        const x = capture();
        spec.recipe = onCall(rewrite(() => ({
            before: pattern`read(${x})`,
            after: template`({${x}: 1, [${x}]: 2})`
        })));
        //language=javascript
        return spec.rewriteRun(javascript(
            `
                read(key);
                read("a-b");
            `,
            `
                ({key: 1, [key]: 2});
                ({"a-b": 1, ["a-b"]: 2});
            `));
    });
});
