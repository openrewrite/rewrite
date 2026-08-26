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
import {capture, javascript, JavaScriptVisitor, JS, Pattern, pattern} from "../../../src/javascript";
import {J} from "../../../src/java";

// The parser represents the empty parameter list of `() => 1` as a single J.Empty, so an
// arrow pattern with one parameter and a zero-parameter arrow have lists of the same length.
describe('arrow patterns against a zero-parameter arrow', () => {
    const spec = new RecipeSpec();

    // Records the name of the variable each matching arrow function is assigned to.
    function probe(pat: Pattern, matched: string[]) {
        return fromVisitor(new class extends JavaScriptVisitor<any> {
            protected override async visitArrowFunction(arrow: JS.ArrowFunction, p: any): Promise<J | undefined> {
                if (await pat.match(arrow, this.cursor)) {
                    const variable = this.cursor.firstEnclosing(
                        (v): v is J.VariableDeclarations.NamedVariable => (v as J)?.kind === J.Kind.NamedVariable);
                    matched.push(variable ? (variable.name as J.Identifier).simpleName : '?');
                }
                return super.visitArrowFunction(arrow, p);
            }
        });
    }

    const blockBodied = () =>
        //language=javascript
        javascript(`
            const zeroParameters = () => { return 1; };
            const oneParameter = (a) => { return 1; };
        `);

    const expressionBodied = () =>
        //language=javascript
        javascript(`
            const zeroParameters = () => 1;
            const oneParameter = (a) => 1;
        `);

    test('a variadic parameter capture', async () => {
        const matched: string[] = [];
        spec.recipe = probe(pattern`(${capture({variadic: true})}) => { return ${capture()}; }`, matched);

        await spec.rewriteRun(blockBodied());

        expect(matched).toEqual(['oneParameter']);
    });

    test('a plain parameter capture', async () => {
        const matched: string[] = [];
        spec.recipe = probe(pattern`(${capture()}) => { return ${capture()}; }`, matched);

        await spec.rewriteRun(blockBodied());

        expect(matched).toEqual(['oneParameter']);
    });

    test('a literal parameter, with no capture in the parameter list', async () => {
        const matched: string[] = [];
        spec.recipe = probe(pattern`(a) => { return ${capture()}; }`, matched);

        await spec.rewriteRun(blockBodied());

        expect(matched).toEqual(['oneParameter']);
    });

    test('an expression-bodied arrow pattern', async () => {
        const matched: string[] = [];
        spec.recipe = probe(pattern`(${capture()}) => ${capture()}`, matched);

        await spec.rewriteRun(expressionBodied());

        expect(matched).toEqual(['oneParameter']);
    });

    test('a zero-parameter pattern matches only the zero-parameter arrow', async () => {
        const matched: string[] = [];
        spec.recipe = probe(pattern`() => ${capture()}`, matched);

        await spec.rewriteRun(expressionBodied());

        expect(matched).toEqual(['zeroParameters']);
    });
});
