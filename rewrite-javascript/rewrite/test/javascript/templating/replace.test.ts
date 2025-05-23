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
import {JavaScriptVisitor, typescript} from "../../../src/javascript";
import {J} from "../../../src/java";
import {JavaCoordinates, JavaScriptTemplate} from "../../../src/javascript/templating";
import Mode = JavaCoordinates.Mode;

describe('template replace', () => {
    const spec = new RecipeSpec();

    test('raw string', () => {
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitLiteral(literal: J.Literal, p: any): Promise<J | undefined> {
                if (literal.valueSource === '1') {
                    return new JavaScriptTemplate('2').apply(this.cursor, {tree: literal, loc: "EXPRESSION_PREFIX", mode: Mode.Replace});
                }
                return literal;
            }
        });
        return spec.rewriteRun({
            //language=typescript
            ...typescript('const a = 1', 'const a = 2'),
        });
    });
});
