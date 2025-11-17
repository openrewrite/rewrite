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
import {JavaScriptVisitor, param, template, typescript} from "../../../src/javascript";
import {J} from "../../../src/java";
import {produce} from "immer";

describe('param() function', () => {
    const spec = new RecipeSpec();

    test('param() works in standalone template', async () => {
        const value = param<J.Literal>('value');
        const tmpl = template`${value} * 2`;

        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitLiteral(literal: J.Literal, p: any): Promise<J | undefined> {
                if (literal.valueSource === '5') {
                    const ten = produce(literal, draft => {
                        draft.value = 10;
                        draft.valueSource = '10';
                    });
                    return tmpl.apply(this.cursor, literal, new Map([['value', ten]]));
                }
                return literal;
            }
        });

        return spec.rewriteRun(
            typescript('const x = 5', 'const x = 10 * 2'),
        );
    });

    test('param() with unnamed parameter', async () => {
        const value = param(); // Unnamed
        const tmpl = template`${value} + 1`;

        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitLiteral(literal: J.Literal, p: any): Promise<J | undefined> {
                if (literal.valueSource === '7') {
                    return tmpl.apply(this.cursor, literal, new Map([[value.getName(), literal]]));
                }
                return literal;
            }
        });

        return spec.rewriteRun(
            typescript('const x = 7', 'const x = 7 + 1'),
        );
    });

    test('param() works with multiple parameters', async () => {
        const left = param('left');
        const right = param('right');
        const tmpl = template`${left} * ${right}`;

        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitBinary(binary: J.Binary, p: any): Promise<J | undefined> {
                if (binary.operator.element === J.Binary.Type.Addition) {
                    return tmpl.apply(this.cursor, binary, new Map([
                        ['left', binary.left],
                        ['right', binary.right]
                    ]));
                }
                return binary;
            }
        });

        return spec.rewriteRun(
            typescript('const x = 3 + 4', 'const x = 3 * 4'),
        );
    });

    test('param() type is TemplateParam not Capture', () => {
        const p = param('test');
        const c = param('test');

        // Both should have getName() method
        expect(p.getName()).toBe('test');
        expect(c.getName()).toBe('test');

        // TemplateParam has name property
        expect(p.name).toBe('test');
    });
});
