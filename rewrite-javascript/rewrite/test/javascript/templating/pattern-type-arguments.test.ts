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
import {capture, JavaScriptVisitor, Pattern, pattern, typescript} from "../../../src/javascript";
import {J} from "../../../src/java";

describe('pattern matching against calls with type arguments', () => {
    const spec = new RecipeSpec();

    /**
     * Collects, for every method invocation the pattern matches, the name of the
     * variable it was assigned to. That makes the assertions readable without
     * having to reason about the shape of the invocation itself.
     */
    function probe(pat: Pattern, matched: string[]) {
        return fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(method: J.MethodInvocation, p: any): Promise<J | undefined> {
                method = await super.visitMethodInvocation(method, p) as J.MethodInvocation;
                if (await pat.match(method, this.cursor)) {
                    const variable = this.cursor.firstEnclosing(
                        (v): v is J.VariableDeclarations.NamedVariable => (v as J)?.kind === J.Kind.NamedVariable);
                    matched.push(variable ? (variable.name as J.Identifier).simpleName : '?');
                }
                return method;
            }
        });
    }

    test('a pattern without type arguments matches a call that has them', async () => {
        const matched: string[] = [];
        spec.recipe = probe(pattern`${capture()}.bind(${capture()})`, matched);

        await spec.rewriteRun(
            //language=typescript
            typescript(`
                const plain = fn.bind(x);
                const parenthesized = (fn).bind((x));
                const optional = fn?.bind(x);
                const typeArguments = fn.bind<any>(x);
            `)
        );

        expect(matched).toEqual(['plain', 'parenthesized', 'optional', 'typeArguments']);
    });

    test('a pattern without type arguments matches a call that has no type attribution', async () => {
        const matched: string[] = [];
        spec.recipe = probe(pattern`identity(${capture()})`, matched);

        await spec.rewriteRun(
            //language=typescript
            typescript(`
                const plain = identity(x);
                const typeArguments = identity<string>(x);
            `)
        );

        expect(matched).toEqual(['plain', 'typeArguments']);
    });

    test('a variadic argument capture makes no difference', async () => {
        const matched: string[] = [];
        spec.recipe = probe(pattern`identity(${capture({variadic: true})})`, matched);

        await spec.rewriteRun(
            //language=typescript
            typescript(`
                const plain = identity(x);
                const typeArguments = identity<string>(x);
                const several = identity<string>(x, y);
            `)
        );

        expect(matched).toEqual(['plain', 'typeArguments', 'several']);
    });

    test('a pattern that spells out type arguments still constrains them', async () => {
        const matched: string[] = [];
        spec.recipe = probe(pattern`${capture()}.bind<string>(${capture()})`, matched);

        await spec.rewriteRun(
            //language=typescript
            typescript(`
                const plain = fn.bind(x);
                const other = fn.bind<any>(x);
                const same = fn.bind<string>(x);
            `)
        );

        expect(matched).toEqual(['same']);
    });
});

// Such a call parses as JS.FunctionCall rather than J.MethodInvocation.
describe('pattern matching against calls with a computed callee', () => {
    const spec = new RecipeSpec();

    function probe(pat: Pattern, matched: string[]) {
        return fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visit(tree: any, p: any, parent?: any): Promise<any> {
                const node = await super.visit(tree, p, parent);
                if (node && (node as J).kind && await pat.match(node as J, this.cursor)) {
                    const variable = this.cursor.firstEnclosing(
                        (v): v is J.VariableDeclarations.NamedVariable => (v as J)?.kind === J.Kind.NamedVariable);
                    matched.push(variable ? (variable.name as J.Identifier).simpleName : '?');
                }
                return node;
            }
        });
    }

    test('a pattern without type arguments matches a call that has them', async () => {
        const matched: string[] = [];
        spec.recipe = probe(pattern`getFn()(${capture()})`, matched);

        await spec.rewriteRun(
            //language=typescript
            typescript(`
                const plain = getFn()(x);
                const typeArguments = getFn()<string>(x);
            `)
        );

        expect(matched).toEqual(['plain', 'typeArguments']);
    });

    test('a pattern that spells out type arguments still constrains them', async () => {
        const matched: string[] = [];
        spec.recipe = probe(pattern`getFn()<string>(${capture()})`, matched);

        await spec.rewriteRun(
            //language=typescript
            typescript(`
                const plain = getFn()(x);
                const other = getFn()<number>(x);
                const same = getFn()<string>(x);
            `)
        );

        expect(matched).toEqual(['same']);
    });
});
