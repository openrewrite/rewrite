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
import {capture, JavaScriptParser, JavaScriptVisitor, JS, pattern, template, typescript} from '../../../src/javascript';
import {J} from '../../../src/java';
import {fromVisitor, RecipeSpec} from '../../../src/test';
import {ExecutionContext} from '../../../src';

describe('object literals in templates and patterns', () => {
    async function parseExpression(code: string): Promise<J> {
        const cu = (await new JavaScriptParser().parse({text: code, sourcePath: 'test.ts'}).next()).value as JS.CompilationUnit;
        return cu.statements[0].element;
    }

    test('a template opening with `{` in place of an expression is an object literal', () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<ExecutionContext> {
            override async visitNewClass(newClass: J.NewClass, p: ExecutionContext): Promise<J | undefined> {
                const spread = newClass.body?.statements[0].element;
                if (newClass.body!.statements.length === 1 && spread?.kind === JS.Kind.Spread) {
                    // Asking for bindings parses the template's context along with its code
                    return template`{responseType: 'text', ...${(spread as JS.Spread).expression}}`
                        .apply(newClass, this.cursor, {bindings: {}});
                }
                return super.visitNewClass(newClass, p);
            }
        });
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `const options = {...base};`,
                `const options = {responseType: 'text', ...base};`
            )
        );
    });

    test('a template opening with `{` in place of a function body is a block', () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<ExecutionContext> {
            override async visitBlock(block: J.Block, p: ExecutionContext): Promise<J | undefined> {
                if (this.cursor.parentTree()?.value.kind === J.Kind.Lambda && block.statements.length === 1) {
                    return template`{
                        log();
                        return 1;
                    }`.apply(block, this.cursor);
                }
                return super.visitBlock(block, p);
            }
        });
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `
                const f = () => {
                    return 1;
                };
                `,
                `
                const f = () => {
                    log();
                    return 1;
                };
                `
            )
        );
    });

    test('a captured property name is spliced into an object literal without its source punctuation', () => {
        const spec = new RecipeSpec();
        const x = capture('x');
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<ExecutionContext> {
            override async visitMethodInvocation(method: J.MethodInvocation, p: ExecutionContext): Promise<J | undefined> {
                const match = await pattern`foo(${x})`.match(method, this.cursor);
                return match ? template`bar({${x}, b: 1})`.apply(method, this.cursor, {values: match}) : method;
            }
        });
        return spec.rewriteRun(
            //language=typescript
            typescript(`foo(a,)`, `bar({a, b: 1})`)
        );
    });

    test('properties passed as a parameter are spliced into an object literal', () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<ExecutionContext> {
            override async visitMethodInvocation(method: J.MethodInvocation, p: ExecutionContext): Promise<J | undefined> {
                const options = method.arguments.elements[1]?.element;
                if (method.select && method.name.simpleName === 'get' && options?.kind === J.Kind.NewClass) {
                    const props = (options as J.NewClass).body!.statements;
                    return template`${method.select}.post(${method.arguments.elements[0]}, {${props}, responseType: 'text'})`
                        .apply(method, this.cursor);
                }
                return super.visitMethodInvocation(method, p);
            }
        });
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `http.get(url, {headers: h, withCredentials: true})`,
                `http.post(url, {headers: h, withCredentials: true, responseType: 'text'})`
            )
        );
    });

    test('a variadic capture in an object literal pattern matches its properties', async () => {
        const props = capture({variadic: true});
        const match = await pattern`${capture('http')}.get(${capture('url')}, {${props}})`
            .match(await parseExpression(`http.get(url, {headers: h, withCredentials: true})`), undefined!);
        expect(match).toBeDefined();
        expect((match!.get(props) as J[]).length).toBe(2);
    });

    test('a pattern opening with `{` matches an object literal, and a block only a block', async () => {
        const declaration = await parseExpression(`const o = {headers: h}`) as J.VariableDeclarations;
        const objectLiteral = declaration.variables[0].element.initializer!.element;
        expect(await pattern`{headers: ${capture('value')}}`.match(objectLiteral, undefined!)).toBeDefined();

        expect(await pattern`{ foo(); bar(); }`.match(objectLiteral, undefined!)).toBeUndefined();
    });
});
