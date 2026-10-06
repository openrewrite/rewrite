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
import {fromVisitor, RecipeSpec, SourceSpec} from '../../../src/test';
import {capture, javascript, JavaScriptVisitor, JS, pattern, template, typescript, withDetectedStyle} from '../../../src/javascript';
import {Expression, J} from '../../../src/java';
import {ExecutionContext} from '../../../src';

describe('a substituted value keeps the layout the source gave it', () => {
    const spec = new RecipeSpec();
    const detected = (source: SourceSpec<JS.CompilationUnit>) => ({...source, beforeRecipe: withDetectedStyle});

    /** `Array(...)` becomes `[...]`, the same arguments under a different pair of delimiters. */
    const arrayLiteral = () => spec.recipe = fromVisitor(new class extends JavaScriptVisitor<ExecutionContext> {
        override async visitMethodInvocation(method: J.MethodInvocation, p: ExecutionContext): Promise<J | undefined> {
            const args: J.Container<Expression> = method.arguments;
            return await template`[${args}]`.apply(method, this.cursor);
        }
    });

    test('a container keeps its line breaks and indent', () => {
        arrayLiteral();
        return spec.rewriteRun(detected(
            //language=javascript
            javascript(
                `
                    function f() {
                      return Array(
                        1,
                        2
                      );
                    }
                `,
                `
                    function f() {
                      return [
                        1,
                        2
                      ];
                    }
                `)));
    });

    test('an argument carrying a trailing comment', () => {
        arrayLiteral();
        return spec.rewriteRun(
            //language=javascript
            javascript(`Array(1 /* one */, 2 /* two */,);`, `[1 /* one */, 2 /* two */,];`));
    });

    test('captured arguments re-emitted in place keep their layout', () => {
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<ExecutionContext> {
            override async visitMethodInvocation(method: J.MethodInvocation, p: ExecutionContext): Promise<J | undefined> {
                const [http, first, second, third] = [capture('http'), capture('first'), capture('second'), capture('third')];
                const values = method.typeParameters ? undefined :
                    await pattern`${http}.put(${first}, ${second}, ${third})`.match(method, this.cursor);
                return values ?
                    await template`${http}.put<any>(${first}, ${second}, ${third})`.apply(method, this.cursor, {values}) :
                    method;
            }
        });
        return spec.rewriteRun(detected(
            //language=typescript
            typescript(
                `
                    function put(path: string, body: object) {
                      return this.http.put(
                        path,
                        JSON.stringify(body),
                        { headers: this.headers() }
                      );
                    }
                `,
                `
                    function put(path: string, body: object) {
                      return this.http.put<any>(
                        path,
                        JSON.stringify(body),
                        { headers: this.headers() }
                      );
                    }
                `)));
    });

    test('a statement moved into a block is re-indented as a whole', () => {
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<ExecutionContext> {
            override async visitMethodInvocation(method: J.MethodInvocation): Promise<J | undefined> {
                return this.cursor.parentTree()?.value.kind !== JS.Kind.CompilationUnit ? method :
                    await template`if (ready) {
                        ${method}
                    }`.apply(method, this.cursor);
            }
        });
        return spec.rewriteRun(detected(
            //language=javascript
            javascript(
                `
                    run(() => {
                      /*
                       * steady
                       */
                      go();
                    })
                `,
                `
                    if (ready) {
                      run(() => {
                        /*
                         * steady
                         */
                        go();
                      })
                    }
                `)));
    });

    test('a value with an empty prefix takes the spacing the template gives it', () => {
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<ExecutionContext> {
            override async visitMethodInvocation(method: J.MethodInvocation, p: ExecutionContext): Promise<J | undefined> {
                const [a, b] = [capture('a'), capture('b')];
                const values = await pattern`sum(${a}, ${b})`.match(method, this.cursor);
                return values ? await template`${a}+${b}`.apply(method, this.cursor, {values}) : method;
            }
        });
        return spec.rewriteRun(
            //language=javascript
            javascript(`const c = sum(a,b);`, `const c = a + b;`));
    });
});
