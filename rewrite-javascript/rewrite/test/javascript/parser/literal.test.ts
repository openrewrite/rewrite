/*
 * Copyright 2023 the original author or authors.
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
import {RecipeSpec} from "../../../src/test";
import {JS, typescript} from "../../../src/javascript";
import {J, JavaType} from "../../../src/java";
import Literal = J.Literal;

const spec = new RecipeSpec();

describe.each([
    ['1', JavaType.Primitive.Int],
    ['1.0', JavaType.Primitive.Double],
    ['"1"', JavaType.Primitive.String],
    ['true', JavaType.Primitive.Boolean],
    ['null', JavaType.Primitive.Null],
    ['undefined', JavaType.Primitive.None],
    ['/hello/gi', JavaType.Primitive.String],
    ['`hello!`', JavaType.Primitive.String]
])(`primitive types`, (expectedValueSource: string, expectedType: JavaType.Primitive) => {
    test(`${expectedValueSource} should have primitive type ${expectedType.keyword}`, () => spec.rewriteRun({
        ...typescript(' 1'),
        afterRecipe: cu => {
            expect(cu).toBeDefined();
            expect(cu.statements).toHaveLength(1);
            const lit = (cu.statements[0] as JS.ExpressionStatement).expression as Literal;
            expect(lit.valueSource).toBe(expectedValueSource);
            expect(lit.type?.kind).toBe(expectedType);
        }
    }));
});
