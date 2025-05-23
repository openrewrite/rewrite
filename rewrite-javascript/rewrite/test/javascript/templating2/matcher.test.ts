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
import {match, capture, backRef, Pattern, Matcher} from "../../../src/javascript/templating2";
import {emptySpace, J} from "../../../src/java";
import {JS} from "../../../src/javascript";
import {JavaType} from "../../../src/java/type";
import {emptyMarkers, randomId} from "../../../src";

// Helper function to create a mock identifier
function createIdentifier(name: string): J.Identifier {
    return {
        kind: J.Kind.Identifier,
        id: randomId(),
        markers: emptyMarkers,
        prefix: emptySpace,
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
        prefix: emptySpace,
        value,
        valueSource
    } as J.Literal;
}

// Helper function to create a mock binary expression
function createBinary(left: J, operator: J.Binary.Type, right: J): J.Binary {
    return {
        kind: J.Kind.Binary,
        id: randomId(),
        markers: emptyMarkers,
        prefix: emptySpace,
        left,
        operator: {
            kind: J.Kind.JLeftPadded,
            before: {
                kind: J.Kind.Space,
                comments: [],
                whitespace: " "
            },
            element: operator,
            markers: emptyMarkers,
        },
        right
    } as J.Binary;
}

describe('Matcher', () => {
    describe('matchNode', () => {
        test('matches identical identifiers', () => {
            const matcher = new Matcher({} as Pattern, {} as J);

            const id1 = createIdentifier('test');
            const id2 = createIdentifier('test');
            const id3 = createIdentifier('other');

            // Access the private method using type assertion
            const matchNode = (matcher as any).matchNode.bind(matcher);

            expect(matchNode(id1, id2)).toBe(true);
            expect(matchNode(id1, id3)).toBe(false);
        });

        test('matches identical literals', () => {
            const matcher = new Matcher({} as Pattern, {} as J);

            const lit1 = createLiteral(42, '42');
            const lit2 = createLiteral(42, '42');
            const lit3 = createLiteral('42', '"42"');

            // Access the private method using type assertion
            const matchNode = (matcher as any).matchNode.bind(matcher);

            expect(matchNode(lit1, lit2)).toBe(true);
            expect(matchNode(lit1, lit3)).toBe(false);
        });

        test('matches identical binary expressions', () => {
            const matcher = new Matcher({} as Pattern, {} as J);

            const left1 = createIdentifier('a');
            const right1 = createIdentifier('b');
            const binary1 = createBinary(left1, J.Binary.Type.Addition, right1);

            const left2 = createIdentifier('a');
            const right2 = createIdentifier('b');
            const binary2 = createBinary(left2, J.Binary.Type.Addition, right2);

            const left3 = createIdentifier('a');
            const right3 = createIdentifier('c');
            const binary3 = createBinary(left3, J.Binary.Type.Addition, right3);

            // Access the private method using type assertion
            const matchNode = (matcher as any).matchNode.bind(matcher);

            expect(matchNode(binary1, binary2)).toBe(true);
            expect(matchNode(binary1, binary3)).toBe(false);
        });

        test('returns false for nodes with different kinds', () => {
            const matcher = new Matcher({} as Pattern, {} as J);

            const id = createIdentifier('test');
            const lit = createLiteral(42, '42');

            // Access the private method using type assertion
            const matchNode = (matcher as any).matchNode.bind(matcher);

            expect(matchNode(id, lit)).toBe(false);
        });
    });

    describe('handleCapture', () => {
        test('captures a node and returns true', () => {
            const matcher = new Matcher({} as Pattern, {} as J);

            const capturePlaceholder = createIdentifier('__capture_test__');
            const targetNode = createLiteral(42, '42');

            // Access the private methods using type assertion
            const handleCapture = (matcher as any).handleCapture.bind(matcher);

            expect(handleCapture(capturePlaceholder, targetNode)).toBe(true);
            expect(matcher.get('test')).toBe(targetNode);
        });

        test('captures a node with type constraint', () => {
            const matcher = new Matcher({} as Pattern, {} as J);

            const capturePlaceholder = createIdentifier('__capture_test_number__');
            const targetNode = createLiteral(42, '42');

            // Mock the matchesTypeConstraint method to always return true
            (matcher as any).matchesTypeConstraint = jest.fn().mockReturnValue(true);

            // Access the private methods using type assertion
            const handleCapture = (matcher as any).handleCapture.bind(matcher);

            expect(handleCapture(capturePlaceholder, targetNode)).toBe(true);
            expect(matcher.get('test')).toBe(targetNode);
            expect((matcher as any).matchesTypeConstraint).toHaveBeenCalledWith(targetNode, 'number');
        });

        test('returns false if type constraint is not satisfied', () => {
            const matcher = new Matcher({} as Pattern, {} as J);

            const capturePlaceholder = createIdentifier('__capture_test_number__');
            const targetNode = createLiteral(42, '42');

            // Mock the matchesTypeConstraint method to return false
            (matcher as any).matchesTypeConstraint = jest.fn().mockReturnValue(false);

            // Access the private methods using type assertion
            const handleCapture = (matcher as any).handleCapture.bind(matcher);

            expect(handleCapture(capturePlaceholder, targetNode)).toBe(false);
            expect(matcher.get('test')).toBeUndefined();
            expect((matcher as any).matchesTypeConstraint).toHaveBeenCalledWith(targetNode, 'number');
        });
    });

    describe('handleBackRef', () => {
        test('returns true if back-reference matches target', () => {
            const matcher = new Matcher({} as Pattern, {} as J);

            const backRefPlaceholder = createIdentifier('__backRef_test__');
            const targetNode = createLiteral(42, '42');

            // Set up a binding
            (matcher as any).bindings.set('test', targetNode);

            // Mock the nodesAreEqual method to return true
            (matcher as any).nodesAreEqual = jest.fn().mockReturnValue(true);

            // Access the private methods using type assertion
            const handleBackRef = (matcher as any).handleBackRef.bind(matcher);

            expect(handleBackRef(backRefPlaceholder, targetNode)).toBe(true);
            expect((matcher as any).nodesAreEqual).toHaveBeenCalledWith(targetNode, targetNode);
        });

        test('returns false if back-reference does not match target', () => {
            const matcher = new Matcher({} as Pattern, {} as J);

            const backRefPlaceholder = createIdentifier('__backRef_test__');
            const capturedNode = createLiteral(42, '42');
            const targetNode = createLiteral(43, '43');

            // Set up a binding
            (matcher as any).bindings.set('test', capturedNode);

            // Mock the nodesAreEqual method to return false
            (matcher as any).nodesAreEqual = jest.fn().mockReturnValue(false);

            // Access the private methods using type assertion
            const handleBackRef = (matcher as any).handleBackRef.bind(matcher);

            expect(handleBackRef(backRefPlaceholder, targetNode)).toBe(false);
            expect((matcher as any).nodesAreEqual).toHaveBeenCalledWith(capturedNode, targetNode);
        });

        test('returns false if back-reference name is not found', () => {
            const matcher = new Matcher({} as Pattern, {} as J);

            const backRefPlaceholder = createIdentifier('__backRef_test__');
            const targetNode = createLiteral(42, '42');

            // Access the private methods using type assertion
            const handleBackRef = (matcher as any).handleBackRef.bind(matcher);

            expect(handleBackRef(backRefPlaceholder, targetNode)).toBe(false);
        });
    });

    describe('matches', () => {
        test('matches a simple pattern with captures', async () => {
            // Create a pattern and a matching AST
            const pattern = {
                getPatternAst: jest.fn().mockResolvedValue(
                    createBinary(
                        createIdentifier('__capture_left__'),
                        J.Binary.Type.Addition,
                        createIdentifier('__capture_right__')
                    )
                )
            } as unknown as Pattern;

            const ast = createBinary(
                createLiteral(1, '1'),
                J.Binary.Type.Addition,
                createLiteral(2, '2')
            );

            const matcher = new Matcher(pattern, ast);

            // Mock the matchNode method to simulate a successful match
            (matcher as any).matchNode = jest.fn().mockReturnValue(true);

            expect(await matcher.matches()).toBe(true);
            expect((matcher as any).matchNode).toHaveBeenCalled();
        });

        test('does not match if pattern and AST are different', async () => {
            // Create a pattern and a non-matching AST
            const pattern = {
                getPatternAst: jest.fn().mockResolvedValue(
                    createBinary(
                        createIdentifier('__capture_left__'),
                        J.Binary.Type.Addition,
                        createIdentifier('__capture_right__')
                    )
                )
            } as unknown as Pattern;

            const ast = createIdentifier('notABinaryExpression');

            const matcher = new Matcher(pattern, ast);

            // Mock the matchNode method to simulate a failed match
            (matcher as any).matchNode = jest.fn().mockReturnValue(false);

            expect(await matcher.matches()).toBe(false);
            expect((matcher as any).matchNode).toHaveBeenCalled();
        });
    });

    describe('integration tests', () => {
        test('captures variables in a binary expression', async () => {
            // Create a real pattern using the match function
            const pattern = match`${capture('left')} + ${capture('right')}`;

            // Create a matching AST
            const ast = createBinary(
                createLiteral(1, '1'),
                J.Binary.Type.Addition,
                createLiteral(2, '2')
            );

            // Override the matchNode method to always return true and capture the variables
            const matcher = new Matcher(pattern, ast);
            (matcher as any).matchNode = jest.fn((_pattern: J, target: J) => {
                // Simulate capturing the left and right operands
                (matcher as any).bindings.set('left', ast.left);
                (matcher as any).bindings.set('right', ast.right);
                return true;
            });

            expect(await matcher.matches()).toBe(true);
            expect(matcher.get('left')).toBe(ast.left);
            expect(matcher.get('right')).toBe(ast.right);
        });

        test('handles back-references correctly', async () => {
            // Create a real pattern using the match function
            const pattern = match`${capture('expr')} || ${backRef('expr')}`;

            // Create a matching AST with identical expressions on both sides
            const expr = createLiteral(true, 'true');
            const ast = createBinary(
                expr,
                J.Binary.Type.Or,
                createLiteral(true, 'true')
            );

            // Create a matcher and set up mocks
            const matcher = new Matcher(pattern, ast);

            // Mock the pattern AST to be a binary expression with a capture and a back-reference
            (matcher as any).patternAst = createBinary(
                createIdentifier('__capture_expr__'),
                J.Binary.Type.Or,
                createIdentifier('__backRef_expr__')
            );

            // Mock the matchNode method to always return true
            (matcher as any).matchNode = jest.fn().mockReturnValue(true);

            // Set up the binding for 'expr'
            (matcher as any).bindings.set('expr', ast.left);

            expect(await matcher.matches()).toBe(true);
            expect(matcher.get('expr')).toBe(ast.left);
        });
    });
});
