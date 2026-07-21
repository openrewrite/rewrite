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
import {RecipeSpec} from "../../../src/test";
import {JS, typescript, javascript} from "../../../src/javascript";
import {J, Type} from "../../../src/java";
import Literal = J.Literal;

const spec = new RecipeSpec();

describe.each([
    ['1', Type.Primitive.Double],
    ['0o777', Type.Primitive.Double],
    ['1.0', Type.Primitive.Double],
    ['123n', Type.Primitive.BigInt],
    ['"1"', Type.Primitive.String],
    ['true', Type.Primitive.Boolean],
    ['null', Type.Primitive.Null],
    //skipped since undefined is a valid identifier in some scenarios.
    // see https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/undefined#description
    // ['undefined', JavaType.Primitive.None],
    ['/hello/gi', Type.Primitive.String],
    ['`hello!`', Type.Primitive.String]
])(`primitive types`, (expectedValueSource: string, expectedType: Type.Primitive) => {
    test(`${expectedValueSource} should have primitive type ${expectedType.keyword}`, () => spec.rewriteRun({
        ...typescript(` ${expectedValueSource}`),
        afterRecipe: (cu: JS.CompilationUnit) => {
            expect(cu).toBeDefined();
            expect(cu.statements).toHaveLength(1);
            const lit = (cu.statements[0].element as JS.ExpressionStatement).expression as Literal;
            expect(lit.valueSource).toBe(expectedValueSource);
            expect(lit.type).toBe(expectedType);
        }
    }));
});

describe('property key literal types', () => {
    // A property name literal's type must match the literal's own lexical category,
    // not the property value's type. Regression for a string key over a numeric
    // value ({'a"b': 1}) being typed `double`: the JVM deserializer is type-directed
    // and ran Double.valueOf("a\"b"), throwing NumberFormatException mid-stream.
    function keyLiteral(cu: JS.CompilationUnit): Literal {
        const decl = cu.statements[0].element as J.VariableDeclarations;
        const newClass = decl.variables[0].element.initializer!.element as J.NewClass;
        const prop = newClass.body!.statements[0].element as JS.PropertyAssignment;
        return prop.name.element as Literal;
    }

    test.each([
        [`{'a"b': 1}`, 'a"b', Type.Primitive.String],
        [`{'ab': 1}`, 'ab', Type.Primitive.String],
        [`{"foo": 2}`, 'foo', Type.Primitive.String],
        [`{1: 2}`, 1, Type.Primitive.Double],
    ])('%s key is typed by its own literal', (obj, expectedValue, expectedType) =>
        spec.rewriteRun({
            ...typescript(`const o = ${obj};`),
            afterRecipe: (cu: JS.CompilationUnit) => {
                const key = keyLiteral(cu);
                expect(key.value).toBe(expectedValue);
                expect(key.type).toBe(expectedType);
            }
        }));
});

describe('Old-style octal literals (error 1121)', () => {
    test('should parse in .js files', () => spec.rewriteRun({
        ...javascript('0777'),
        afterRecipe: (cu: JS.CompilationUnit) => {
            expect(cu).toBeDefined();
            expect(cu.statements).toHaveLength(1);
            const lit = (cu.statements[0].element as JS.ExpressionStatement).expression as Literal;
            expect(lit.valueSource).toBe('0777');
            expect(lit.type).toBe(Type.Primitive.Double);
        }
    }));

    test('should NOT parse in .ts files', () => {
        return expect(spec.rewriteRun(typescript('0777'))).rejects.toThrow(/Octal literals are not allowed/);
    });
});

describe('Old-style octal escapes (error 1487)', () => {
    test('should parse in .js files', () => spec.rewriteRun({
        ...javascript("'\\033[2J'"),
        afterRecipe: (cu: JS.CompilationUnit) => {
            expect(cu).toBeDefined();
            expect(cu.statements).toHaveLength(1);
            const lit = (cu.statements[0].element as JS.ExpressionStatement).expression as Literal;
            expect(lit.valueSource).toBe("'\\033[2J'");
            expect(lit.type).toBe(Type.Primitive.String);
        }
    }));

    test('should NOT parse in .ts files', () => {
        return expect(spec.rewriteRun(typescript("'\\033[2J'"))).rejects.toThrow(/Octal escape sequences are not allowed/);
    });
});

describe('Malformed hex escape sequences (error 1125)', () => {
    test('should parse in .js files', () => spec.rewriteRun({
        ...javascript('/\\x-.*/'),
        afterRecipe: (cu: JS.CompilationUnit) => {
            expect(cu).toBeDefined();
            expect(cu.statements).toHaveLength(1);
            const lit = (cu.statements[0].element as JS.ExpressionStatement).expression as Literal;
            expect(lit.valueSource).toBe('/\\x-.*/');
            expect(lit.type).toBe(Type.Primitive.String);
        }
    }));

    test('should NOT parse in .ts files', () => {
        return expect(spec.rewriteRun(typescript('/\\x-.*/'))).rejects.toThrow(/Hexadecimal digit expected/);
    });
});
