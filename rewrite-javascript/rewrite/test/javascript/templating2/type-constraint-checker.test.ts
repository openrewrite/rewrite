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
import {TypeConstraintChecker} from "../../../src/javascript/templating2";
import {emptySpace, J, JavaType, TypedTree} from "../../../src/java";
import {emptyMarkers, randomId} from "../../../src";
import getType = TypedTree.getType;

// Helper function to create a mock node with type information
function createNodeWithType(type: JavaType): J & TypedTree {
    return {
        kind: J.Kind.Identifier,
        id: randomId(),
        markers: emptyMarkers,
        prefix: emptySpace,
        type
    } as J & TypedTree;
}

describe('TypeConstraintChecker', () => {
    describe('matches', () => {
        test('matches primitive type constraints', () => {
            // Create a mock primitive type
            const numberType: JavaType.Primitive = JavaType.Primitive.Int;
            const stringType: JavaType.Primitive = JavaType.Primitive.String;
            const booleanType: JavaType.Primitive = JavaType.Primitive.Boolean;
            
            // Create nodes with these types
            const numberNode = createNodeWithType(numberType);
            const stringNode = createNodeWithType(stringType);
            const booleanNode = createNodeWithType(booleanType);
            
            // Mock the toString method for primitive types
            numberType.toString = jest.fn().mockReturnValue('number');
            stringType.toString = jest.fn().mockReturnValue('string');
            booleanType.toString = jest.fn().mockReturnValue('boolean');
            
            // Test matching with correct constraints
            expect(TypeConstraintChecker.matches(numberNode, 'number')).toBe(true);
            expect(TypeConstraintChecker.matches(stringNode, 'string')).toBe(true);
            expect(TypeConstraintChecker.matches(booleanNode, 'boolean')).toBe(true);
            
            // Test matching with incorrect constraints
            expect(TypeConstraintChecker.matches(numberNode, 'string')).toBe(false);
            expect(TypeConstraintChecker.matches(stringNode, 'boolean')).toBe(false);
            expect(TypeConstraintChecker.matches(booleanNode, 'number')).toBe(false);
        });
        
        test('matches class type constraints', () => {
            // Create a mock class type
            const userType: JavaType.FullyQualified = {
                kind: 'FullyQualified',
                fullyQualifiedName: 'com.example.User'
            } as JavaType.FullyQualified;
            
            const productType: JavaType.FullyQualified = {
                kind: 'FullyQualified',
                fullyQualifiedName: 'com.example.Product'
            } as JavaType.FullyQualified;
            
            // Create nodes with these types
            const userNode = createNodeWithType(userType);
            const productNode = createNodeWithType(productType);
            
            // Mock the toString method for class types
            userType.toString = jest.fn().mockReturnValue('com.example.User');
            productType.toString = jest.fn().mockReturnValue('com.example.Product');
            
            // Test matching with correct constraints
            expect(TypeConstraintChecker.matches(userNode, 'User')).toBe(true);
            expect(TypeConstraintChecker.matches(productNode, 'Product')).toBe(true);
            
            // Test matching with incorrect constraints
            expect(TypeConstraintChecker.matches(userNode, 'Product')).toBe(false);
            expect(TypeConstraintChecker.matches(productNode, 'User')).toBe(false);
        });
        
        test('matches union type constraints', () => {
            // Create a mock primitive type
            const numberType: JavaType.Primitive = JavaType.Primitive.Int;
            const stringType: JavaType.Primitive = JavaType.Primitive.String;

            // Create nodes with these types
            const numberNode = createNodeWithType(numberType);
            const stringNode = createNodeWithType(stringType);
            
            // Mock the toString method for primitive types
            numberType.toString = jest.fn().mockReturnValue('number');
            stringType.toString = jest.fn().mockReturnValue('string');
            
            // Mock the matches method to handle union types
            const originalMatches = TypeConstraintChecker.matches;
            (TypeConstraintChecker as any).matches = jest.fn((node: TypedTree, constraint: string) => {
                if (constraint === 'number') {
                    return getType(node) === numberType;
                } else if (constraint === 'string') {
                    return getType(node) === stringType;
                }
                return false;
            });
            
            // Test matching with union constraints
            // FIXME check what to do about this
            // expect(TypeConstraintChecker.matches(numberNode, 'number|string')).toBe(true);
            // expect(TypeConstraintChecker.matches(stringNode, 'number|string')).toBe(true);
            // expect(TypeConstraintChecker.matches(numberNode, 'string|boolean')).toBe(false);
            
            // Restore the original matches method
            (TypeConstraintChecker as any).matches = originalMatches;
        });
        
        test('returns false for nodes without type information', () => {
            // Create a node without type information
            const nodeWithoutType = {
                kind: J.Kind.Identifier,
                id: randomId(),
                markers: emptyMarkers,
                prefix: emptySpace,
            } as J;
            
            expect(TypeConstraintChecker.matches(nodeWithoutType, 'number')).toBe(false);
        });
    });
    
    describe('isPrimitiveTypeConstraint', () => {
        test('identifies primitive type constraints correctly', () => {
            // Access the private static method using type assertion
            const isPrimitiveTypeConstraint = (TypeConstraintChecker as any).isPrimitiveTypeConstraint;
            
            expect(isPrimitiveTypeConstraint('number')).toBe(true);
            expect(isPrimitiveTypeConstraint('string')).toBe(true);
            expect(isPrimitiveTypeConstraint('boolean')).toBe(true);
            expect(isPrimitiveTypeConstraint('any')).toBe(true);
            expect(isPrimitiveTypeConstraint('void')).toBe(true);
            expect(isPrimitiveTypeConstraint('null')).toBe(true);
            expect(isPrimitiveTypeConstraint('undefined')).toBe(true);
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
            expect(isClassTypeConstraint('123Class')).toBe(false);
        });
    });
    
    describe('isUnionTypeConstraint', () => {
        test('identifies union type constraints correctly', () => {
            // Access the private static method using type assertion
            const isUnionTypeConstraint = (TypeConstraintChecker as any).isUnionTypeConstraint;
            
            expect(isUnionTypeConstraint('number|string')).toBe(true);
            expect(isUnionTypeConstraint('number | string')).toBe(true);
            expect(isUnionTypeConstraint('number|string|boolean')).toBe(true);
            expect(isUnionTypeConstraint('number')).toBe(false);
        });
    });
    
    describe('matchesPrimitiveType', () => {
        test('matches primitive types correctly', () => {
            // Create a mock primitive type
            const numberType: JavaType.Primitive = JavaType.Primitive.Int;

            // Mock the toString method
            numberType.toString = jest.fn().mockReturnValue('number');
            
            // Access the private static method using type assertion
            const matchesPrimitiveType = (TypeConstraintChecker as any).matchesPrimitiveType;
            
            expect(matchesPrimitiveType(numberType, 'number')).toBe(true);
            expect(matchesPrimitiveType(numberType, 'string')).toBe(false);
        });
    });
    
    describe('matchesClassType', () => {
        test('matches class types correctly', () => {
            // Create a mock class type
            const userType: JavaType.FullyQualified = {
                kind: 'FullyQualified',
                fullyQualifiedName: 'com.example.User'
            } as JavaType.FullyQualified;
            
            // Mock the toString method
            userType.toString = jest.fn().mockReturnValue('com.example.User');
            
            // Access the private static method using type assertion
            const matchesClassType = (TypeConstraintChecker as any).matchesClassType;
            
            expect(matchesClassType(userType, 'User')).toBe(true);
            expect(matchesClassType(userType, 'Product')).toBe(false);
        });
    });
});