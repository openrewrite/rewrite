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
import {typescript} from "../../../src/javascript";

describe('identifier mapping', () => {
    const spec = new RecipeSpec();

    test('number', () => {
        rewriteRunWithOptions(
          {normalizeIndent: false},
          typescript(' 1', undefined, sourceFile => {
              assertLiteralLst(sourceFile, '1', JavaType.PrimitiveKind.Double);
          }));
    });
    test('string', () => {
        rewriteRunWithOptions(
          {normalizeIndent: false},
          typescript('"1"', undefined, sourceFile => {
              assertLiteralLst(sourceFile, '"1"', JavaType.PrimitiveKind.String);
          }));
    });
    test('boolean', () => {
        rewriteRunWithOptions(
          {normalizeIndent: false},
          typescript('true', undefined, sourceFile => {
              assertLiteralLst(sourceFile, 'true', JavaType.PrimitiveKind.Boolean);
          }));
    });
    test('null', () => {
        rewriteRunWithOptions(
          {normalizeIndent: false},
          typescript('null', undefined, sourceFile => {
              assertLiteralLst(sourceFile, 'null', JavaType.PrimitiveKind.Null);
          }));
    });
    test.skip('undefined', () => {
        //skipped since undefined is a valid identifier in some scenarios.
        // see https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/undefined#description
        rewriteRunWithOptions(
          {normalizeIndent: false},
          typescript('undefined', undefined, sourceFile => {
              assertLiteralLst(sourceFile, 'undefined', JavaType.PrimitiveKind.None);
          }));
    });
    test('regex', () => {
        rewriteRunWithOptions(
          {normalizeIndent: false},
          typescript('/hello/gi', undefined, sourceFile => {
              assertLiteralLst(sourceFile, '/hello/gi', JavaType.PrimitiveKind.String);
          }));
    });
    test('template without substitutions', () => {
        rewriteRunWithOptions(
          {normalizeIndent: false},
          typescript('`hello!`', undefined, sourceFile => {
              assertLiteralLst(sourceFile, '`hello!`', JavaType.PrimitiveKind.String);
          }));
    });

    function assertLiteralLst(sourceFile: JS.CompilationUnit, expectedValueSource: string, expectedType: JavaType.PrimitiveKind) {
        expect(sourceFile).toBeDefined();
        expect(sourceFile.statements).toHaveLength(1);
        let statement = sourceFile.statements[0];
        expect(statement).toBeInstanceOf(JS.ExpressionStatement);
        let expression = (statement as JS.ExpressionStatement).expression;
        expect(expression).toBeInstanceOf(J.Literal);
        expect((expression as J.Literal).valueSource).toBe(expectedValueSource);
        expect((expression as J.Literal).type.kind).toBe(expectedType);
    }
});
