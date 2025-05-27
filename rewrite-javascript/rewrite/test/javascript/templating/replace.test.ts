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
import {JavaScriptVisitor, template, typescript} from "../../../src/javascript";
import {Expression, J} from "../../../src/java";
import {produce} from "immer";
import {produceAsync} from "../../../src";

describe('template2 replace', () => {
    const spec = new RecipeSpec();

    test('raw string', () => {
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitLiteral(literal: J.Literal, p: any): Promise<J | undefined> {
                if (literal.valueSource === '1') {
                    // Use the new template API with tagged template literals
                    return template`2`.apply(this.cursor, literal);
                }
                return literal;
            }
        });
        return spec.rewriteRun(
            //language=typescript
            typescript('const a = 1', 'const a = 2'),
        );
    });

    test('parameter replacement', () => {
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitLiteral(literal: J.Literal, p: any): Promise<J | undefined> {
                if (literal.valueSource === '1') {
                    // Use the new template API with tagged template literals and parameter substitution
                    return template`${2}`.apply(this.cursor, literal);
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
            override async visitLiteral(literal: J.Literal, p: any): Promise<J | undefined> {
                if (literal.valueSource === '1') {
                    // Create a new AST node for the number 2
                    const two = produce(literal, draft => {
                        draft.value = 2;
                        draft.valueSource = '2';
                    });

                    // Use the new template API with tagged template literals and AST node substitution
                    return template`${two}`.apply(this.cursor, literal);
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
            override async visitBinary(binary: J.Binary, p: any): Promise<J | undefined> {
                if (binary.operator.element === J.Binary.Type.Equal) {
                    return await produceAsync(binary, async draft => {

                        draft.left = (await template`${binary.right}`.apply(
                            this.cursor,
                            binary
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
});
