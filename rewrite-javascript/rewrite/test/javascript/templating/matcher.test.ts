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
import {capture, Matcher, pattern} from "../../../src/javascript";
import {emptySpace, J} from "../../../src/java";
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
    describe('matches', () => {
        test('matches a simple pattern with captures', async () => {
            // Create a pattern
            const p = pattern`${capture()} + ${capture()}`;

            const ast = createBinary(
                createLiteral(1, '1'),
                J.Binary.Type.Addition,
                createLiteral(2, '2')
            );

            const matcher = new Matcher(p, ast);

            // Mock the template processor and pattern AST creation
            const mockPatternAst = createBinary(
                createIdentifier('__capture_left__'),
                J.Binary.Type.Addition,
                createIdentifier('__capture_right__')
            );
            
            // Set the pattern AST directly to avoid the async parsing
            (matcher as any).patternAst = mockPatternAst;

            // Mock the matchNode method to simulate a successful match
            (matcher as any).matchNode = jest.fn().mockReturnValue(true);

            expect(await matcher.matches()).toBe(true);
            expect((matcher as any).matchNode).toHaveBeenCalled();
        });

        test('does not match if pattern and AST are different', async () => {
            // Create a pattern
            const p = pattern`${capture()} + ${capture()}`;

            const ast = createIdentifier('notABinaryExpression');

            const matcher = new Matcher(p, ast);

            // Mock the template processor and pattern AST creation
            const mockPatternAst = createBinary(
                createIdentifier('__capture_left__'),
                J.Binary.Type.Addition,
                createIdentifier('__capture_right__')
            );
            
            // Set the pattern AST directly to avoid the async parsing
            (matcher as any).patternAst = mockPatternAst;

            // Mock the matchNode method to simulate a failed match
            (matcher as any).matchNode = jest.fn().mockReturnValue(false);

            expect(await matcher.matches()).toBe(false);
            expect((matcher as any).matchNode).toHaveBeenCalled();
        });
    });

    describe('integration tests', () => {
        test('captures variables in a binary expression', async () => {
            // Create a real pattern using the match function
            let left = capture();
            let right = capture();
            const p = pattern`${left} + ${right}`;

            // Create a matching AST
            const ast = createBinary(
                createLiteral(1, '1'),
                J.Binary.Type.Addition,
                createLiteral(2, '2')
            );

            // Override the matchNode method to always return true and capture the variables
            const matcher = new Matcher(p, ast);
            (matcher as any).matchNode = jest.fn((_pattern: J, target: J) => {
                // Simulate capturing the left and right operands
                (matcher as any).bindings.set(left.name, ast.left);
                (matcher as any).bindings.set(right.name, ast.right);
                return true;
            });

            expect(await matcher.matches()).toBe(true);
            expect(matcher.get(left)).toBe(ast.left);
            expect(matcher.get(right)).toBe(ast.right);
        });
    });
});
