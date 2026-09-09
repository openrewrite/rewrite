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
import {capture, javascript, JavaScriptVisitor, Pattern, pattern} from "../../../src/javascript";
import {J} from "../../../src/java";

// Semantic comparison is deliberately tolerant of differing node kinds, so a pattern is
// offered every node a recipe visits and has to answer for the ones it has nothing to say about.
describe('patterns offered a node of an unrelated kind', () => {
    const spec = new RecipeSpec();

    // Counts how many of the nodes in the source the pattern matches.
    function probe(pat: Pattern, matches: { count: number }) {
        return fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visit(tree: any, p: any, parent?: any): Promise<any> {
                const node = await super.visit(tree, p, parent);
                if (node && (node as J).kind && await pat.match(node as J, this.cursor)) {
                    matches.count++;
                }
                return node;
            }
        });
    }

    test('a function declaration pattern against a declaration with no parameters', async () => {
        const matches = {count: 0};
        spec.recipe = probe(pattern`function foo(a) { return 1; }`, matches);

        //language=javascript
        await spec.rewriteRun(javascript(`function foo() { return 1; }`));

        expect(matches.count).toBe(0);
    });

    test('a function declaration pattern against the declaration it describes', async () => {
        const matches = {count: 0};
        spec.recipe = probe(pattern`function foo(a) { return 1; }`, matches);

        //language=javascript
        await spec.rewriteRun(javascript(`function foo(a) { return 1; }`));

        expect(matches.count).toBe(1);
    });

    test('a variable declaration pattern against a function declaration', async () => {
        const matches = {count: 0};
        spec.recipe = probe(pattern`const [${capture()}, ${capture()}] = ${capture()};`, matches);

        //language=javascript
        await spec.rewriteRun(javascript(`function foo() { return 1; }`));

        expect(matches.count).toBe(0);
    });

    test('a variable declaration pattern against the declaration it describes', async () => {
        const matches = {count: 0};
        spec.recipe = probe(pattern`const [${capture()}, ${capture()}] = ${capture()};`, matches);

        //language=javascript
        await spec.rewriteRun(javascript(`const [a, b] = arr;`));

        expect(matches.count).toBe(1);
    });
});
