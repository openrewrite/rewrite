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
import {fromVisitor, RecipeSpec} from '../../../src/test';
import {javascript, JavaScriptVisitor, template} from '../../../src/javascript';
import {Expression, J} from '../../../src/java';
import {ExecutionContext} from '../../../src';

describe('a substituted container keeps the line breaks the source wrote', () => {
    const spec = new RecipeSpec();

    /** `Array(...)` becomes `[...]`, the same arguments under a different pair of delimiters. */
    spec.recipe = fromVisitor(new class extends JavaScriptVisitor<ExecutionContext> {
        override async visitMethodInvocation(method: J.MethodInvocation, p: ExecutionContext): Promise<J | undefined> {
            const args: J.Container<Expression> = method.arguments;
            return await template`[${args}]`.apply(method, this.cursor);
        }
    });

    /** The line breaks are the source's; the indent between them is the formatter's, as on the JVM. */
    test('an argument per line', () => spec.rewriteRun(
        //language=javascript
        javascript(
            `
                Array(
                  1,
                  2
                );
            `,
            `
                [
                    1,
                    2
                ];
            `)));
});
