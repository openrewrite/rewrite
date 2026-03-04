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
import {Type} from "../../src/java";

describe('Type.signature', () => {
    test('cyclic parameterized types with shared references (shell-caching pattern)', () => {
        // Simulates the shell-caching pattern from type-mapping:
        // P1 and P2 share the same object references via typeCache
        const classA: Type.ShallowClass = {
            kind: Type.Kind.ShallowClass,
            fullyQualifiedName: "com.example.A",
        };
        const classB: Type.ShallowClass = {
            kind: Type.Kind.ShallowClass,
            fullyQualifiedName: "com.example.B",
        };

        // Shell-cache pattern: create with empty typeParameters first
        const paramA: Type.Parameterized = {
            kind: Type.Kind.Parameterized,
            type: classA,
            typeParameters: [],
        };
        const paramB: Type.Parameterized = {
            kind: Type.Kind.Parameterized,
            type: classB,
            typeParameters: [],
        };

        // Then fill in type parameters (creating a cycle through shared references)
        // A<B<A>> where the inner A IS the same paramA object
        paramB.typeParameters = [paramA];
        paramA.typeParameters = [paramB];

        // This should NOT throw RangeError: Maximum call stack size exceeded
        const result = Type.signature(paramA);
        expect(result).toBeDefined();
        expect(result).toContain("com.example.A");
    });

    test('deeply nested but non-cyclic parameterized types work correctly', () => {
        const classA: Type.ShallowClass = {
            kind: Type.Kind.ShallowClass,
            fullyQualifiedName: "com.example.List",
        };
        const classB: Type.ShallowClass = {
            kind: Type.Kind.ShallowClass,
            fullyQualifiedName: "com.example.Map",
        };

        const inner: Type.Parameterized = {
            kind: Type.Kind.Parameterized,
            type: classA,
            typeParameters: [Type.Primitive.String],
        };
        const outer: Type.Parameterized = {
            kind: Type.Kind.Parameterized,
            type: classB,
            typeParameters: [Type.Primitive.String, inner],
        };

        const result = Type.signature(outer);
        expect(result).toBe("com.example.Map<String, com.example.List<String>>");
    });
});
