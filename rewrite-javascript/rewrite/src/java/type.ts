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
import {asRef} from "../rpc";

export interface Type {
    readonly kind: string;
}

export namespace Type {
    export const Kind = {
        Annotation: "org.openrewrite.java.tree.JavaType$Annotation",
        AnnotationElementValue: "org.openrewrite.java.tree.JavaType$Annotation$ElementValue",
        SingleElementValue: "org.openrewrite.java.tree.JavaType$Annotation$SingleElementValue",
        ArrayElementValue: "org.openrewrite.java.tree.JavaType$Annotation$ArrayElementValue",
        Array: "org.openrewrite.java.tree.JavaType$Array",
        Class: "org.openrewrite.java.tree.JavaType$Class",
        GenericTypeVariable: "org.openrewrite.java.tree.JavaType$GenericTypeVariable",
        Intersection: "org.openrewrite.java.tree.JavaType$Intersection",
        Method: "org.openrewrite.java.tree.JavaType$Method",
        Parameterized: "org.openrewrite.java.tree.JavaType$Parameterized",
        Primitive: "org.openrewrite.java.tree.JavaType$Primitive",
        ShallowClass: "org.openrewrite.java.tree.JavaType$ShallowClass",
        Union: "org.openrewrite.java.tree.JavaType$MultiCatch",
        Unknown: "org.openrewrite.java.tree.JavaType$Unknown",
        Variable: "org.openrewrite.java.tree.JavaType$Variable",
    }

    export interface Class extends Type, FullyQualified {
        readonly kind: typeof Kind.Class,
        flags: number;
        classKind: Class.Kind;
        fullyQualifiedName: string;
        typeParameters: Type[];
        supertype?: Type.Class;
        owningClass?: Type.Class;
        annotations: Type.Annotation[];
        interfaces: Type.Class[];
        members: Type.Variable[];
        methods: Type.Method[];

        toJSON?(): string;
    }

    export namespace Class {
        export const enum Kind {
            Class = "Class",
            Enum = "Enum",
            Interface = "Interface",
            Annotation = "Annotation",
            Record = "Record",
            Value = "Value"
        }
    }

    export interface Annotation extends Type, FullyQualified {
        readonly kind: typeof Kind.Annotation,
        type: Type.FullyQualified;
        values: Annotation.ElementValue[];
    }

    export namespace Annotation {
        export type ElementValue = SingleElementValue | ArrayElementValue;

        export interface SingleElementValue {
            readonly kind: typeof Kind.SingleElementValue;
            element: Type;
            constantValue?: any;
            referenceValue?: Type;
        }

        export interface ArrayElementValue {
            readonly kind: typeof Kind.ArrayElementValue;
            element: Type;
            constantValues?: any[];
            referenceValues?: Type[];
        }
    }

    export interface Method extends Type {
        readonly kind: typeof Kind.Method;
        flags: number;
        declaringType: Type.FullyQualified;
        name: string;
        returnType: Type;
        parameterNames: string[];
        parameterTypes: Type[];
        thrownExceptions: Type[];
        annotations: Type.Annotation[];
        defaultValue?: string[];
        declaredFormalTypeNames: string[];
    }

    export interface Variable extends Type {
        readonly kind: typeof Kind.Variable;
        name: string;
        owner?: Type;
        type: Type;
        annotations: Type.Annotation[];

        toJSON?(): string;
    }

    export interface Parameterized extends Type, FullyQualified {
        readonly kind: typeof Kind.Parameterized;
        type: Type.FullyQualified;
        typeParameters: Type[];
    }

    export interface GenericTypeVariable extends Type {
        readonly kind: typeof Kind.GenericTypeVariable;
        name: string;
        variance: GenericTypeVariable.Variance;
        bounds: Type[];
    }

    export namespace GenericTypeVariable {
        export const enum Variance {
            Covariant,
            Contravariant,
            Invariant
        }
    }

    export interface Array extends Type, FullyQualified {
        readonly kind: typeof Kind.Array;
        elemType: Type;
        annotations: Type.Annotation[];
    }


    export class Primitive implements Type {
        private constructor(
            public readonly keyword: string,
            public readonly kind = Type.Kind.Primitive
        ) {
        }

        static readonly Boolean = new Primitive('boolean');
        static readonly Byte = new Primitive('byte');
        static readonly Char = new Primitive('char');
        static readonly Double = new Primitive('double');
        static readonly Float = new Primitive('float');
        static readonly Int = new Primitive('int');
        static readonly BigInt = new Primitive('long');
        static readonly Short = new Primitive('short');
        static readonly String = new Primitive('String');
        static readonly Void = new Primitive('void');
        static readonly Null = new Primitive('null');
        static readonly None = new Primitive('');

        private static _all = [
            Primitive.Boolean,
            Primitive.Byte,
            Primitive.Char,
            Primitive.Double,
            Primitive.Float,
            Primitive.Int,
            Primitive.BigInt,
            Primitive.Short,
            Primitive.String,
            Primitive.Void,
            Primitive.Null,
            Primitive.None
        ];

        static values(): Primitive[] {
            return Primitive._all.slice();
        }

        static fromKeyword(keyword: string): Primitive | undefined {
            switch (keyword) {
                case 'boolean':
                    return Primitive.Boolean;
                case 'byte':
                    return Primitive.Byte;
                case 'char':
                    return Primitive.Char;
                case 'double':
                    return Primitive.Double;
                case 'float':
                    return Primitive.Float;
                case 'int':
                    return Primitive.Int;
                case 'long':
                    return Primitive.BigInt;
                case 'short':
                    return Primitive.Short;
                case 'String':
                    return Primitive.String;
                case 'void':
                    return Primitive.Void;
                case 'null':
                    return Primitive.Null;
                case '':
                    return Primitive.None;
                default:
                    return undefined;
            }
        }
    }

    export interface Union extends Type {
        readonly kind: typeof Kind.Union;
        bounds: Type[];
    }

    export interface Intersection extends Type {
        readonly kind: typeof Kind.Intersection;
        bounds: Type[];
    }

    export interface ShallowClass extends Type.Class {
        readonly kind: typeof Kind.ShallowClass;
    }

    export const unknownType: Type = asRef({
        kind: Type.Kind.Unknown
    });

    export function isPrimitive(type?: Type): type is Type.Primitive {
        return type?.kind === Type.Kind.Primitive;
    }

    export function isClass(type?: Type): type is Type.Class {
        return type?.kind === Type.Kind.Class;
    }

    export function isMethod(type?: Type): type is Type.Method {
        return type?.kind === Type.Kind.Method;
    }

    export function isArray(type?: Type): type is Type.Array {
        return type?.kind === Type.Kind.Array;
    }

    export function isParameterized(type?: Type): type is Type.Parameterized {
        return type?.kind === Type.Kind.Parameterized;
    }

    export function isFullyQualified(type?: Type): type is Type.FullyQualified {
        return type != null && (
            type.kind === Type.Kind.Class ||
            type.kind === Type.Kind.Annotation ||
            type.kind === Type.Kind.Parameterized ||
            type.kind === Type.Kind.Array ||
            type.kind === Type.Kind.ShallowClass
        );
    }

    export interface FullyQualified extends Type {
    }

    export namespace FullyQualified {
        export function getFullyQualifiedName(javaType: FullyQualified): string {
            switch (javaType.kind) {
                case Type.Kind.Class:
                    return (javaType as Type.Class).fullyQualifiedName;
                case Type.Kind.Parameterized:
                    return getFullyQualifiedName((javaType as Type.Parameterized).type);
                case Type.Kind.Annotation:
                    return getFullyQualifiedName((javaType as Type.Annotation).type);
                case Type.Kind.ShallowClass:
                    return (javaType as Type.ShallowClass).fullyQualifiedName;
                case Type.Kind.Unknown:
                    return "<unknown>";
            }
            throw new Error("Cannot get fully qualified name of type: " + JSON.stringify(javaType));
        }
    }

    // Track type variable names and parameterized types to prevent infinite recursion
    let typeVariableNameStack: Set<string> | null = null;
    let parameterizedStack: Set<Type> | null = null;

    export function signature(type: Type | undefined | null): string {
        if (!type) {
            return "<null>";
        }
        switch (type.kind) {
            case Type.Kind.Array: {
                const arr = type as Type.Array;
                return signature(arr.elemType) + "[]";
            }
            case Type.Kind.Class:
            case Type.Kind.ShallowClass: {
                const clazz = type as Type.Class;
                return clazz.fullyQualifiedName;
            }
            case Type.Kind.GenericTypeVariable: {
                const generic = type as Type.GenericTypeVariable;
                let result = "Generic{" + generic.name;

                // Initialize stack if needed
                if (typeVariableNameStack === null) {
                    typeVariableNameStack = new Set<string>();
                }

                // Check for recursion in type variable names
                if (generic.name !== "?" && typeVariableNameStack.has(generic.name)) {
                    return result + "}";
                }

                // Add to stack to track
                if (generic.name !== "?") {
                    typeVariableNameStack.add(generic.name);
                }

                try {
                    if (!generic.bounds || generic.bounds.length === 0) {
                        return result + "}";
                    }

                    // Filter out bounds that would cause cycles through parameterized types
                    const safeBounds = generic.bounds.filter(b => {
                        return !parameterizedStack || !parameterizedStack.has(b);
                    });

                    if (safeBounds.length > 0) {
                        const bounds = safeBounds.map(b => signature(b)).join(" & ");
                        result += " extends " + bounds;
                    }

                    return result + "}";
                } finally {
                    // Remove from stack when done
                    if (generic.name !== "?") {
                        typeVariableNameStack.delete(generic.name);
                    }
                }
            }
            case Type.Kind.Intersection: {
                const intersection = type as Type.Intersection;
                return (intersection.bounds || []).map(b => signature(b)).join(" & ");
            }
            case Type.Kind.Method: {
                const method = type as Type.Method;
                const declaringType = signature(method.declaringType);
                const params = (method.parameterTypes || []).map(p => signature(p)).join(", ");
                return declaringType + "{name=" + method.name + ",return=" + signature(method.returnType) + ",parameters=[" + params + "]}";
            }
            case Type.Kind.Parameterized: {
                const parameterized = type as Type.Parameterized;

                // Initialize stack if needed
                if (parameterizedStack === null) {
                    parameterizedStack = new Set<Type>();
                }

                // Add to stack to track cycles
                parameterizedStack.add(parameterized);

                try {
                    const baseType = signature(parameterized.type);
                    if (!parameterized.typeParameters || parameterized.typeParameters.length === 0) {
                        return baseType;
                    }
                    const params = parameterized.typeParameters.map(p => signature(p)).join(", ");
                    return baseType + "<" + params + ">";
                } finally {
                    // Remove from stack when done
                    parameterizedStack.delete(parameterized);
                }
            }
            case Type.Kind.Primitive: {
                const primitive = type as Type.Primitive;
                return primitive.keyword;
            }
            case Type.Kind.Union: {
                const union = type as Type.Union;
                return (union.bounds || []).map(b => signature(b)).join(" | ");
            }
            case Type.Kind.Unknown: {
                return "<unknown>";
            }
            case Type.Kind.Variable: {
                const variable = type as Type.Variable;
                const ownerSig = variable.owner ? signature(variable.owner) + "{" : "";
                const closeBrace = variable.owner ? "}" : "";
                return ownerSig + "name=" + variable.name + ",type=" + signature(variable.type) + closeBrace;
            }
            case Type.Kind.Annotation: {
                const annotation = type as Type.Annotation;
                return "@" + signature(annotation.type);
            }
            default:
                return "<unknown>";
        }
    }
}
