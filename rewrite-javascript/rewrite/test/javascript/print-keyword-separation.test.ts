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
import {fromVisitor, RecipeSpec} from "../../src/test";
import {javascript, JavaScriptVisitor, typescript} from "../../src/javascript";
import {J} from "../../src/java";

describe('printing a word keyword', () => {
    const spec = new RecipeSpec();

    const unwrapParentheses = () => fromVisitor(new class extends JavaScriptVisitor<any> {
        override async visitParentheses<T extends J>(parens: J.Parentheses<T>, p: any): Promise<J | undefined> {
            const visited = await super.visitParentheses(parens, p) as J.Parentheses<T>;
            return {...visited.tree.element, prefix: visited.prefix} as J;
        }

        override async visitParenthesizedTypeTree(parens: J.ParenthesizedTypeTree, p: any): Promise<J | undefined> {
            return {...parens.parenthesizedType.tree.element, prefix: parens.prefix} as J;
        }
    });

    test('separates it from an operand a recipe put right after it', () => {
        spec.recipe = unwrapParentheses();
        //language=javascript
        return spec.rewriteRun(javascript(
            `
                async function* f(a, b, xs) {
                    typeof(a);
                    void(a);
                    delete(a.b);
                    await(a);
                    yield(a);
                    new(A);
                    a in(b);
                    a instanceof(B);
                    for (const x of(xs)) {}
                    for (const x in(xs)) {}
                    switch (a) { case(1): break; }
                    if (a) {} else(a);
                    do(a); while (b);
                    typeof(\u{1D465});
                    typeof(𝑥);
                    throw(a);
                }
            `,
            `
                async function* f(a, b, xs) {
                    typeof a;
                    void a;
                    delete a.b;
                    await a;
                    yield a;
                    new A;
                    a in b;
                    a instanceof B;
                    for (const x of xs) {}
                    for (const x in xs) {}
                    switch (a) { case 1: break; }
                    if (a) {} else a;
                    do a; while (b);
                    typeof \u{1D465};
                    typeof 𝑥;
                    throw a;
                }
            `
        ));
    });

    test('separates it from an operand a recipe put right before it', () => {
        spec.recipe = unwrapParentheses();
        //language=typescript
        return spec.rewriteRun(typescript(
            `
                const a = (b)in c;
                const d = (e)instanceof F;
                const g = (h)as any;
                const i = (j)satisfies K;
                for ((l)of ms) {}
                type N<O> = (O)extends string ? 1 : 2;
                type P<Q> = keyof(Q);
            `,
            `
                const a = b in c;
                const d = e instanceof F;
                const g = h as any;
                const i = j satisfies K;
                for (l of ms) {}
                type N<O> = O extends string ? 1 : 2;
                type P<Q> = keyof Q;
            `
        ));
    });

    test('leaves code that parses as it is', () =>
        //language=typescript
        spec.rewriteRun(typescript(
            `
                function* f(a) {
                    yield*a;
                    switch (a) {default:}
                    const b = typeof(a), c = a in{}, d = new(A)(), e = a as(any);
                    return(a);
                }
                export default(1);
            `
        )));
});
