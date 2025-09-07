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
        readonly classKind: Class.Kind;
        readonly fullyQualifiedName: string;
        readonly typeParameters: Type[];
        readonly supertype?: Type.Class;
        readonly owningClass?: Type.Class;
        readonly annotations: Type.Annotation[];
        readonly interfaces: Type.Class[];
        readonly members: Type.Variable[];
        readonly methods: Type.Method[];
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
        readonly type: Type.FullyQualified;
        readonly values: Annotation.ElementValue[];
    }

    export namespace Annotation {
        export interface ElementValue {
            readonly kind: typeof Kind.AnnotationElementValue;
            readonly element: Type;
            readonly value: any;
        }
    }

    export interface Method extends Type {
        readonly kind: typeof Kind.Method;
        readonly declaringType: Type.FullyQualified;
        readonly name: string;
        readonly returnType: Type;
        readonly parameterNames: string[];
        readonly parameterTypes: Type[];
        readonly thrownExceptions: Type[];
        readonly annotations: Type.Annotation[];
        readonly defaultValue?: string[];
        readonly declaredFormalTypeNames: string[];
    }

    export interface Variable extends Type {
        readonly kind: typeof Kind.Variable;
        readonly name: string;
        readonly owner?: Type;
        readonly type: Type;
        readonly annotations: Type.Annotation[];
    }

    export interface Parameterized extends Type, FullyQualified {
        readonly kind: typeof Kind.Parameterized;
        readonly type: Type.FullyQualified;
        readonly typeParameters: Type[];
    }

    export interface GenericTypeVariable extends Type {
        readonly kind: typeof Kind.GenericTypeVariable;
        readonly name: string;
        readonly variance: GenericTypeVariable.Variance;
        readonly bounds: Type[];
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
        readonly elemType: Type;
        readonly annotations: Type.Annotation[];
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
        static readonly Long = new Primitive('long');
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
            Primitive.Long,
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
                    return Primitive.Long;
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
        readonly bounds: Type[];
    }

    export interface Intersection extends Type {
        readonly kind: typeof Kind.Intersection;
        readonly bounds: Type[];
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

    export function isArray(type?: Type): type is Type.Array {
        return type?.kind === Type.Kind.Array;
    }

    export function isParameterized(type?: Type): type is Type.Parameterized {
        return type?.kind === Type.Kind.Parameterized;
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
}
