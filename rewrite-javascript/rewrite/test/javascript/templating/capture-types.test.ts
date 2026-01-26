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
import {capture, JavaScriptVisitor, JS, pattern, template, typescript} from "../../../src/javascript";
import {J, Type} from "../../../src/java";

describe('capture types', () => {
    const spec = new RecipeSpec();

    test('capture with string type annotation', () => {
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitBinary(binary: J.Binary, p: any): Promise<J | undefined> {
                // Create a capture with a type annotation
                const x = capture({type: 'boolean'});
                const pat = pattern`${x} || false`;
                const match = await pat.match(binary, this.cursor);

                if (match) {
                    // Replace with a simpler expression
                    return template`${x}`.apply(binary, this.cursor, { values: match });
                }
                return binary;
            }
        });
        return spec.rewriteRun(
            //language=typescript
            typescript('const a = true || false', 'const a = true'),
        );
    });

    test('template with capture type generates properly typed AST', () => {
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitBinary(binary: J.Binary, p: any): Promise<J | undefined> {
                // Create a capture with a type annotation
                const condition = capture({type: 'boolean'});
                const pat = pattern`${condition} && true`;
                const match = await pat.match(binary, this.cursor);

                if (match) {
                    // Use the capture in a template - type should be preserved
                    return template`!${condition}`.apply(binary, this.cursor, { values: match });
                }
                return binary;
            }
        });
        return spec.rewriteRun(
            //language=typescript
            typescript('const a = flag && true', 'const a = !flag'),
        );
    });

    test('multiple captures with different types', () => {
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitBinary(binary: J.Binary, p: any): Promise<J | undefined> {
                // Create captures with different type annotations
                const x = capture({type: 'number'});
                const y = capture({type: 'number'});
                const pat = pattern`${x} + ${y}`;
                const match = await pat.match(binary, this.cursor);

                if (match) {
                    // Swap the operands
                    return template`${y} + ${x}`.apply(binary, this.cursor, { values: match });
                }
                return binary;
            }
        });
        return spec.rewriteRun(
            //language=typescript
            typescript('const a = 1 + 2', 'const a = 2 + 1'),
        );
    });

    test('capture without type still works', () => {
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitBinary(binary: J.Binary, p: any): Promise<J | undefined> {
                // Create a capture without a type annotation
                const x = capture();
                const pat = pattern`${x} + 1`;
                const match = await pat.match(binary, this.cursor);

                if (match) {
                    return template`${x} * 2`.apply(binary, this.cursor, { values: match });
                }
                return binary;
            }
        });
        return spec.rewriteRun(
            //language=typescript
            typescript('const a = 5 + 1', 'const a = 5 * 2'),
        );
    });

    test('type derivation from J element in template', () => {
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(method: J.MethodInvocation, p: any): Promise<J | undefined> {
                const methodName = method.name as J.Identifier;
                if (methodName.simpleName === 'toUpperCase' && method.select) {
                    // The select element is a string literal with natural type attribution from the parser
                    // Pass J element with type directly to template - type should be derived from the string type
                    return template`${method.select}.toLowerCase()`.apply(method, this.cursor);
                }
                return method;
            }
        });
        return spec.rewriteRun({
            //language=typescript
            ...typescript('"hello".toUpperCase()', '"hello".toLowerCase()'),
            afterRecipe: (cu: JS.CompilationUnit) => {
                const methodType = (cu.statements[0] as unknown as J.MethodInvocation).methodType!;
                expect((methodType.declaringType as Type.Class).fullyQualifiedName).toBe('String');
                expect(methodType.name).toBe('toLowerCase');
            }
        });
    });

    test('type derivation from multiple J elements', () => {
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitBinary(binary: J.Binary, p: any): Promise<J | undefined> {
                if (binary.operator.element === J.Binary.Type.Addition) {
                    // Both operands are number literals with natural type attribution from the parser
                    // Pass both operands as J elements - types should be derived from the number types
                    return template`${binary.right} + ${binary.left}`.apply(binary, this.cursor);
                }
                return binary;
            }
        });
        return spec.rewriteRun(
            //language=typescript
            typescript('const a = 1 + 2', 'const a = 2 + 1'),
        );
    });

    test('Type.Primitive.Boolean maps to boolean', () => {
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitBinary(binary: J.Binary, p: any): Promise<J | undefined> {
                // Create a capture with Type.Primitive.Boolean
                const condition = capture({type: Type.Primitive.Boolean});
                const pat = pattern`${condition} && true`;
                const match = await pat.match(binary, this.cursor);

                if (match) {
                    return template`!${condition}`.apply(binary, this.cursor, { values: match });
                }
                return binary;
            }
        });
        return spec.rewriteRun(
            //language=typescript
            typescript('const a = flag && true', 'const a = !flag'),
        );
    });

    test('Type.Primitive.String maps to string', () => {
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitBinary(binary: J.Binary, p: any): Promise<J | undefined> {
                // Create a capture with Type.Primitive.String
                const str = capture({type: Type.Primitive.String});
                const pat = pattern`${str} + ""`;
                const match = await pat.match(binary, this.cursor);

                if (match) {
                    return template`${str}`.apply(binary, this.cursor, { values: match });
                }
                return binary;
            }
        });
        return spec.rewriteRun(
            //language=typescript
            typescript('const a = "hello" + ""', 'const a = "hello"'),
        );
    });

    test('Type.Primitive.Double maps to number', () => {
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitBinary(binary: J.Binary, p: any): Promise<J | undefined> {
                // Create a capture with Type.Primitive.Double
                const num = capture({type: Type.Primitive.Double});
                const pat = pattern`${num} + 0`;
                const match = await pat.match(binary, this.cursor);

                if (match) {
                    return template`${num}`.apply(binary, this.cursor, { values: match });
                }
                return binary;
            }
        });
        return spec.rewriteRun(
            //language=typescript
            typescript('const a = 42 + 0', 'const a = 42'),
        );
    });

    test('Type.Array renders component type plus []', () => {
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(method: J.MethodInvocation, p: any): Promise<J | undefined> {
                const methodName = method.name as J.Identifier;
                if (methodName.simpleName === 'oldMethod') {
                    // Create an array type: string[]
                    const arrayType: Type.Array = {
                        kind: Type.Kind.Array,
                        elemType: Type.Primitive.String,
                        annotations: []
                    } as Type.Array;

                    // Use the array type in a capture within a pattern
                    const arr = capture({type: arrayType});
                    const pat = pattern`oldMethod(${arr})`;
                    const match = await pat.match(method, this.cursor);

                    if (match) {
                        return template`newMethod(${arr})`.apply(method, this.cursor, { values: match });
                    }
                }
                return method;
            }
        });
        return spec.rewriteRun(
            //language=typescript
            typescript('oldMethod(items)', 'newMethod(items)'),
        );
    });
});
