/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import {fromVisitor, RecipeSpec} from "../../../src/test";
import {
    capture,
    JavaScriptVisitor,
    pattern,
    template,
    typescript
} from "../../../src/javascript";
import {J} from "../../../src/java";

describe('forwardRef pattern with replacement', () => {
    const spec = new RecipeSpec();

    test('capture function name and use in template', async () => {
        // Capture the function name
        const pat = pattern`forwardRef(function ${capture('name')}(props, ref) { return null; })`
            .configure({
                imports: [`import { forwardRef } from 'react'`]
            });

        // Create a template that uses the captured name
        const tmpl = template`const ComponentName = "${capture('name')}";`;

        //language=typescript
        const testCode = `
            import { forwardRef } from 'react';
            const MyComponent = forwardRef(function MyButton(props, ref) { return null; });
        `;

        let replaced = false;

        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(methodInvocation: J.MethodInvocation, _p: any): Promise<J | undefined> {
                const m = await pat.match(methodInvocation);
                if (m) {
                    // Try to use the captured name in a template
                    const result = await tmpl.apply(this.cursor, methodInvocation, m);
                    if (result) {
                        replaced = true;
                        return result;
                    }
                }
                return methodInvocation;
            }
        });

        await spec.rewriteRun(
            typescript(testCode)
        );
    });

    test('render captured name as identifier in template', async () => {
        // Capture function name and use it as an identifier in a template
        const pat = pattern`forwardRef(function ${capture('name')}(props, ref) { return null; })`
            .configure({
                imports: [`import { forwardRef } from 'react'`]
            });

        // Template that uses the captured name as an identifier
        const tmpl = template`console.log(${capture('name')})`;

        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(methodInvocation: J.MethodInvocation, _p: any): Promise<J | undefined> {
                const m = await pat.match(methodInvocation);
                if (m) {
                    // Apply template with captured identifier
                    const result = await tmpl.apply(this.cursor, methodInvocation, m);
                    if (result) {
                        return result;
                    }
                }
                return methodInvocation;
            }
        });

        return spec.rewriteRun(
            //language=typescript
            typescript(
                `
                    import { forwardRef } from 'react';
                    const MyComponent = forwardRef(function MyButton(props, ref) { return null; });
                `,
                `
                    import { forwardRef } from 'react';
                    const MyComponent = console.log(MyButton);
                `
            )
        );
    });
});
