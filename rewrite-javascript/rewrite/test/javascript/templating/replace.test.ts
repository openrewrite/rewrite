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
import {JavaScriptVisitor, capture, pattern, template, typescript} from "../../../src/javascript";
import {Expression, J} from "../../../src/java";
import {create as produce} from "mutative";

describe('template2 replace', () => {
    const spec = new RecipeSpec();

    test('raw string', () => {
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override visitLiteral(literal: J.Literal, p: any): J | undefined {
                if (literal.valueSource === '1') {
                    // Use the new template API with tagged template literals
                    return template`2`.apply(literal, this.cursor);
                }
                return literal;
            }
        });
        return spec.rewriteRun(
            //language=typescript
            typescript('const a = 1', 'const a = 2'),
        );
    });

    test('tree replacement', () => {
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override visitLiteral(literal: J.Literal, p: any): J | undefined {
                if (literal.valueSource === '1') {
                    // Create a new AST node for the number 2
                    const two = produce(literal, draft => {
                        draft.value = 2;
                        draft.valueSource = '2';
                    });

                    // Use the new template API with tagged template literals and AST node substitution
                    return template`${two}`.apply(literal, this.cursor);
                }
                return literal;
            }
        });
        return spec.rewriteRun(
            //language=typescript
            typescript('const a = 1', 'const a = 2'),
        );
    });

    test('binary expression replacement', () => {
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override visitBinary(binary: J.Binary, p: any): J | undefined {
                if (binary.operator.element === J.Binary.Type.Equal) {
                    return produce(binary, draft => {
                        draft.left = (template`${binary.right}`.apply(
                            binary,
                            this.cursor
                        )) as Expression;
                    });
                }
                return binary;
            }
        });
        return spec.rewriteRun(
            //language=typescript
            typescript('(1 + 2) == 3', '3 == 3'),
        );
    });

    test('late binding with capture', () => {
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override visitLiteral(literal: J.Literal, p: any): J | undefined {
                if (literal.valueSource === '1') {
                    // Create a replacement value
                    const replacement = produce(literal, draft => {
                        draft.value = 42;
                        draft.valueSource = '42';
                    });

                    // Use capture for late binding - myValue capture is looked up in the values map
                    const myValue = capture();
                    return template`${myValue}`.apply(literal, this.cursor, {values: new Map([[myValue, replacement]])});
                }
                return literal;
            }
        });
        return spec.rewriteRun(
            //language=typescript
            typescript('const a = 1', 'const a = 42'),
        );
    });

    test('scalar capture preserves trailing semicolon', () => {
        const arg = capture();
        const pat = pattern`foo(${arg})`;
        const tmpl = template`bar(${arg})`;

        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override visitMethodInvocation(method: J.MethodInvocation, p: any): J | undefined {
                const match = pat.match(method, this.cursor);
                if (match) {
                    return tmpl.apply(method, this.cursor, {values: match});
                }
                return method;
            }
        });

        return spec.rewriteRun(
            typescript(
                'foo(123);',
                'bar(123);'
            )
        );
    });

    test('scalar capture preserves comments', () => {
        const arg = capture();
        const pat = pattern`oldFunc(${arg})`;
        const tmpl = template`newFunc(${arg})`;

        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override visitMethodInvocation(method: J.MethodInvocation, p: any): J | undefined {
                const match = pat.match(method, this.cursor);
                if (match) {
                    return tmpl.apply(method, this.cursor, {values: match});
                }
                return method;
            }
        });

        return spec.rewriteRun(
            typescript(
                'oldFunc(x); // comment',
                'newFunc(x); // comment'
            )
        );
    });

    test('capture binding in method select position', () => {
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override visitMethodInvocation(method: J.MethodInvocation, p: any): J | undefined {
                if ((method.name as J.Identifier).simpleName === 'oldMethod' && method.select) {
                    const select = capture();
                    return template`${select}.newMethod()`.apply(
                        method,
                        this.cursor,
                        {values: new Map([[select, method.select.element]])}
                    );
                }
                return method;
            }
        });

        return spec.rewriteRun(
            typescript(
                'obj.oldMethod();',
                'obj.newMethod();'
            )
        );
    });
});
