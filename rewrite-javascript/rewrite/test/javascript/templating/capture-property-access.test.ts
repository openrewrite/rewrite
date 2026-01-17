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
import {capture, JavaScriptVisitor, pattern, template, typescript} from "../../../src/javascript";
import {J} from "../../../src/java";

describe('forwardRef pattern with replacement', () => {
    const spec = new RecipeSpec();

    test('capture with property access', () => {
        // Test replacing forwardRef(Type) with Type
        // This captures the argument to forwardRef and replaces it with just the argument
        const arg = capture();
        const pat = pattern`forwardRef(${arg})`;
        const tmpl = template`${arg}`;

        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(methodInvocation: J.MethodInvocation, _p: any): Promise<J | undefined> {
                const m = await pat.match(methodInvocation, this.cursor);
                if (m) {
                    const result = await tmpl.apply(methodInvocation, this.cursor, { values: m });
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
                    const forwardRef = (x: any) => x;
                    type ComponentType = any;
                    const MyComponent: ComponentType = forwardRef(() => { return null; });
                `,
                `
                    const forwardRef = (x: any) => x;
                    type ComponentType = any;
                    const MyComponent: ComponentType = () => {
                        return null;
                    };
                `
            )
        );
    });

    test('capture with nested property access in template', () => {
        // Test: forwardRef((props) => ...) becomes (props) => ...
        // This tests a more complex case where we're matching the full forwardRef pattern
        const componentDef = capture();
        const pat = pattern`forwardRef(${componentDef})`;
        const tmpl = template`${componentDef}`;

        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(methodInvocation: J.MethodInvocation, _p: any): Promise<J | undefined> {
                if ((methodInvocation.name as J.Identifier).simpleName === 'forwardRef') {
                    const m = await pat.match(methodInvocation, this.cursor);
                    if (m) {
                        const result = await tmpl.apply(methodInvocation, this.cursor, { values: m });
                        if (result) {
                            return result;
                        }
                    }
                }
                return methodInvocation;
            }
        });

        return spec.rewriteRun(
            //language=typescript
            typescript(
                `
                    const forwardRef = (x: any) => x;
                    type ComponentType = any;
                    const MyComponent: ComponentType = forwardRef((props) => {
                        return props.name;
                    });
                `,
                `
                    const forwardRef = (x: any) => x;
                    type ComponentType = any;
                    const MyComponent: ComponentType = (props) => {
                        return props.name;
                    };
                `
            )
        );
    });

    test('capture with property access in template', () => {
        // Test accessing properties of captured nodes in templates
        const method = capture<J.MethodInvocation>('method');
        const pat = pattern`foo(${method})`;

        // Access the name property of the captured method invocation
        const methodName = method.name;
        const tmpl = template`bar(${methodName})`;

        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(methodInvocation: J.MethodInvocation, _p: any): Promise<J | undefined> {
                if ((methodInvocation.name as J.Identifier).simpleName === 'foo') {
                    const m = await pat.match(methodInvocation, this.cursor);
                    if (m) {
                        // Replace foo(baz()) with bar(baz)
                        const result = await tmpl.apply(methodInvocation, this.cursor, { values: m });
                        if (result) {
                            return result;
                        }
                    }
                }
                return methodInvocation;
            }
        });

        return spec.rewriteRun(
            //language=typescript
            typescript(
                `
                    const foo = (x: any) => x;
                    const bar = (x: any) => x;
                    const baz = () => 42;
                    const result = foo(baz());
                `,
                `
                    const foo = (x: any) => x;
                    const bar = (x: any) => x;
                    const baz = () => 42;
                    const result = bar(baz);
                `
            )
        );
    });

    test('capture with array access in template', () => {
        // Test accessing array elements via bracket notation
        const invocation = capture<J.MethodInvocation>('invocation');
        const pat = pattern`${invocation}`;

        // Access the first argument via array index
        // invocation.arguments.elements[0] accesses the first argument (no .element needed with intersection types)
        const tmpl = template`bar(${(invocation.arguments.elements[0])})`;


        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(methodInvocation: J.MethodInvocation, _p: any): Promise<J | undefined> {
                if ((methodInvocation.name as J.Identifier).simpleName === 'foo') {
                    const m = await pat.match(methodInvocation, this.cursor);
                    if (m) {
                        // Replace foo(baz()) with bar(baz())
                        const result = await tmpl.apply(methodInvocation, this.cursor, { values: m });
                        if (result) {
                            return result;
                        }
                    }
                }
                return methodInvocation;
            }
        });

        return spec.rewriteRun(
            //language=typescript
            typescript(
                `
                    const foo = (x: any) => x;
                    const bar = (x: any) => x;
                    const baz = () => 42;
                    const result = foo(baz());
                    const result2 = bar(baz());
                `,
                `
                    const foo = (x: any) => x;
                    const bar = (x: any) => x;
                    const baz = () => 42;
                    const result = bar(baz());
                    const result2 = bar(baz());
                `
            )
        );
    });
});
