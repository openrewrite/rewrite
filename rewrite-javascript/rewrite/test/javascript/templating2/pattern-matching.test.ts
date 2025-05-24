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
import {match, capture, backRef, Pattern, Matcher, TypeConstraintChecker, TemplateProcessor} from "../../../src/javascript/templating2";
import {J} from "../../../src/java";
import {JS} from "../../../src/javascript";
import {JavaType} from "../../../src/java/type";
import {emptyMarkers, randomId} from "../../../src";

// Helper function to create a mock identifier
function createIdentifier(name: string): J.Identifier {
    return {
        kind: J.Kind.Identifier,
        id: randomId(),
        markers: emptyMarkers,
        prefix: {
            kind: J.Kind.Space,
            comments: [],
            whitespace: ""
        },
        annotations: [],
        simpleName: name
    } as J.Identifier;
}

// Helper function to create a mock literal
function createLiteral(value: any, valueSource: string): J.Literal {
    return {
        kind: J.Kind.Literal,
        id: randomId(),
        markers: emptyMarkers,
        prefix: {
            kind: J.Kind.Space,
            comments: [],
            whitespace: ""
        },
        value,
        valueSource
    } as J.Literal;
}


describe('Pattern Matching', () => {
    describe('TemplateProcessor', () => {
        test('buildTemplateString creates correct string with captures', () => {
            const templateParts = ['', ' + ', ''];
            const captures = [
                capture('left'),
                capture('right', 'number')
            ];

            const processor = new TemplateProcessor(templateParts as unknown as TemplateStringsArray, captures);

            // Access the private method using type assertion
            const buildTemplateString = (processor as any).buildTemplateString.bind(processor);

            const result = buildTemplateString();
            expect(result).toBe('__capture_left__ + __capture_right_number__');
        });

        test('buildTemplateString creates correct string with back-references', () => {
            const templateParts = ['', ' || ', ''];
            const captures = [
                capture('expr'),
                backRef('expr')
            ];

            const processor = new TemplateProcessor(templateParts as unknown as TemplateStringsArray, captures);

            // Access the private method using type assertion
            const buildTemplateString = (processor as any).buildTemplateString.bind(processor);

            const result = buildTemplateString();
            expect(result).toBe('__capture_expr__ || __backRef_expr__');
        });
    });

    describe('Pattern', () => {
        test('getPatternAst returns an AST representation of the pattern', async () => {
            const pattern = match`${capture('left')} + ${capture('right')}`;
            const ast = await pattern.getPatternAst();

            // Verify that we got an AST node back
            expect(ast).toBeDefined();
            expect(ast.kind).toBeDefined();
        });

        test('getPatternAst caches the result', async () => {
            const pattern = match`${capture('left')} + ${capture('right')}`;

            // Call twice and verify we get the same object
            const ast1 = await pattern.getPatternAst();
            const ast2 = await pattern.getPatternAst();

            expect(ast1).toBe(ast2);
        });
    });

    describe('Matcher', () => {
        describe('isCapturePlaceholder', () => {
            test('identifies capture placeholders correctly', () => {
                const matcher = new Matcher({} as Pattern, {} as J);

                // Create a mock identifier with the capture placeholder format
                const capturePlaceholder = createIdentifier('__capture_test__');
                const regularIdentifier = createIdentifier('test');

                // Access the private method using type assertion
                const isCapturePlaceholder = (matcher as any).isCapturePlaceholder.bind(matcher);

                expect(isCapturePlaceholder(capturePlaceholder)).toBe(true);
                expect(isCapturePlaceholder(regularIdentifier)).toBe(false);
            });
        });

        describe('isBackRefPlaceholder', () => {
            test('identifies back-reference placeholders correctly', () => {
                const matcher = new Matcher({} as Pattern, {} as J);

                // Create a mock identifier with the back-reference placeholder format
                const backRefPlaceholder = createIdentifier('__backRef_test__');
                const regularIdentifier = createIdentifier('test');

                // Access the private method using type assertion
                const isBackRefPlaceholder = (matcher as any).isBackRefPlaceholder.bind(matcher);

                expect(isBackRefPlaceholder(backRefPlaceholder)).toBe(true);
                expect(isBackRefPlaceholder(regularIdentifier)).toBe(false);
            });
        });

        describe('extractCaptureName', () => {
            test('extracts capture name correctly', () => {
                const matcher = new Matcher({} as Pattern, {} as J);

                // Access the private method using type assertion
                const extractCaptureName = (matcher as any).extractCaptureName.bind(matcher);

                expect(extractCaptureName('__capture_test__')).toBe('test');
                expect(extractCaptureName('__capture_test_number__')).toBe('test');
            });
        });

        describe('extractTypeConstraint', () => {
            test('extracts type constraint correctly', () => {
                const matcher = new Matcher({} as Pattern, {} as J);

                // Access the private method using type assertion
                const extractTypeConstraint = (matcher as any).extractTypeConstraint.bind(matcher);

                expect(extractTypeConstraint('__capture_test__')).toBeUndefined();
                expect(extractTypeConstraint('__capture_test_number__')).toBe('number');
            });
        });

        describe('extractBackRefName', () => {
            test('extracts back-reference name correctly', () => {
                const matcher = new Matcher({} as Pattern, {} as J);

                // Access the private method using type assertion
                const extractBackRefName = (matcher as any).extractBackRefName.bind(matcher);

                expect(extractBackRefName('__backRef_test__')).toBe('test');
            });
        });

        describe('matchIdentifier', () => {
            test('matches identifiers with the same name', () => {
                const matcher = new Matcher({} as Pattern, {} as J);

                const id1 = createIdentifier('test');
                const id2 = createIdentifier('test');
                const id3 = createIdentifier('other');

                // Access the private method using type assertion
                const matchIdentifier = (matcher as any).matchIdentifier.bind(matcher);

                expect(matchIdentifier(id1, id2)).toBe(true);
                expect(matchIdentifier(id1, id3)).toBe(false);
            });
        });

        describe('matchLiteral', () => {
            test('matches literals with the same value source', () => {
                const matcher = new Matcher({} as Pattern, {} as J);

                const lit1 = createLiteral(42, '42');
                const lit2 = createLiteral(42, '42');
                const lit3 = createLiteral('42', '"42"');

                // Access the private method using type assertion
                const matchLiteral = (matcher as any).matchLiteral.bind(matcher);

                expect(matchLiteral(lit1, lit2)).toBe(true);
                expect(matchLiteral(lit1, lit3)).toBe(false);
            });
        });

        describe('nodesAreEqual', () => {
            test('compares identifiers correctly', () => {
                const matcher = new Matcher({} as Pattern, {} as J);

                const id1 = createIdentifier('test');
                const id2 = createIdentifier('test');
                const id3 = createIdentifier('other');

                // Access the private method using type assertion
                const nodesAreEqual = (matcher as any).nodesAreEqual.bind(matcher);

                expect(nodesAreEqual(id1, id2)).toBe(true);
                expect(nodesAreEqual(id1, id3)).toBe(false);
            });

            test('compares literals correctly', () => {
                const matcher = new Matcher({} as Pattern, {} as J);

                const lit1 = createLiteral(42, '42');
                const lit2 = createLiteral(42, '42');
                const lit3 = createLiteral('42', '"42"');

                // Access the private method using type assertion
                const nodesAreEqual = (matcher as any).nodesAreEqual.bind(matcher);

                expect(nodesAreEqual(lit1, lit2)).toBe(true);
                expect(nodesAreEqual(lit1, lit3)).toBe(false);
            });
        });
    });

    describe('TypeConstraintChecker', () => {
        describe('isPrimitiveTypeConstraint', () => {
            test('identifies primitive type constraints correctly', () => {
                // Access the private static method using type assertion
                const isPrimitiveTypeConstraint = (TypeConstraintChecker as any).isPrimitiveTypeConstraint;

                expect(isPrimitiveTypeConstraint('number')).toBe(true);
                expect(isPrimitiveTypeConstraint('string')).toBe(true);
                expect(isPrimitiveTypeConstraint('boolean')).toBe(true);
                expect(isPrimitiveTypeConstraint('CustomClass')).toBe(false);
            });
        });

        describe('isClassTypeConstraint', () => {
            test('identifies class type constraints correctly', () => {
                // Access the private static method using type assertion
                const isClassTypeConstraint = (TypeConstraintChecker as any).isClassTypeConstraint;

                expect(isClassTypeConstraint('Number')).toBe(true);
                expect(isClassTypeConstraint('String')).toBe(true);
                expect(isClassTypeConstraint('CustomClass')).toBe(true);
                expect(isClassTypeConstraint('number')).toBe(false);
            });
        });

        describe('isUnionTypeConstraint', () => {
            test('identifies union type constraints correctly', () => {
                // Access the private static method using type assertion
                const isUnionTypeConstraint = (TypeConstraintChecker as any).isUnionTypeConstraint;

                expect(isUnionTypeConstraint('number|string')).toBe(true);
                expect(isUnionTypeConstraint('number | string')).toBe(true);
                expect(isUnionTypeConstraint('number')).toBe(false);
            });
        });
    });
});
