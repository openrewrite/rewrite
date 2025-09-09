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
import {asRef, RpcCodecs, RpcReceiveQueue, RpcSendQueue} from "../rpc";
import {immerable} from "immer";

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
        export interface ElementValue {
            readonly kind: typeof Kind.AnnotationElementValue;
            element: Type;
            value: any;
        }
    }

    export interface Method extends Type {
        readonly kind: typeof Kind.Method;
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

    /**
     * Creates a simple string representation for debugging/testing purposes.
     * This is automatically used by JSON.stringify when serializing Type objects.
     * This function is non-recursive to avoid stack overflow issues.
     */
    export function toDebugString(type: Type): string {
        switch (type.kind) {
            case Type.Kind.Class:
            case Type.Kind.ShallowClass:
                return `Type.Class{fullyQualifiedName=${(type as Type.Class).fullyQualifiedName}}`;
            case Type.Kind.Variable:
                return `Type.Variable{name=${(type as Type.Variable).name}}`;
            case Type.Kind.Method:
                const method = type as Type.Method;
                return `Type.Method{name=${method.name}}`;
            case Type.Kind.Primitive:
                return `Type.Primitive{keyword=${(type as Type.Primitive).keyword}}`;
            case Type.Kind.Parameterized:
                return `Type.Parameterized`;
            case Type.Kind.Array:
                return `Type.Array`;
            case Type.Kind.GenericTypeVariable:
                return `Type.GenericTypeVariable{name=${(type as Type.GenericTypeVariable).name}}`;
            case Type.Kind.Annotation:
                return `Type.Annotation`;
            case Type.Kind.Union:
                return `Type.Union{bounds=${(type as Type.Union).bounds.length}}`;
            case Type.Kind.Intersection:
                return `Type.Intersection{bounds=${(type as Type.Intersection).bounds.length}}`;
            case Type.Kind.Unknown:
                return `Type.Unknown`;
            default:
                return `Type{kind=${type.kind}}`;
        }
    }

    export function signature(type: Type): string {
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
                if (generic.bounds.length === 0) {
                    return "Generic{" + generic.name + "}";
                }
                const bounds = generic.bounds.map(b => signature(b)).join(" & ");
                return "Generic{" + generic.name + " extends " + bounds + "}";
            }
            case Type.Kind.Intersection: {
                const intersection = type as Type.Intersection;
                return intersection.bounds.map(b => signature(b)).join(" & ");
            }
            case Type.Kind.Method: {
                const method = type as Type.Method;
                const declaringType = signature(method.declaringType);
                const params = method.parameterTypes.map(p => signature(p)).join(", ");
                return declaringType + "{name=" + method.name + ",return=" + signature(method.returnType) + ",parameters=[" + params + "]}";
            }
            case Type.Kind.Parameterized: {
                const parameterized = type as Type.Parameterized;
                const baseType = signature(parameterized.type);
                if (parameterized.typeParameters.length === 0) {
                    return baseType;
                }
                const params = parameterized.typeParameters.map(p => signature(p)).join(", ");
                return baseType + "<" + params + ">";
            }
            case Type.Kind.Primitive: {
                const primitive = type as Type.Primitive;
                return primitive.keyword;
            }
            case Type.Kind.Union: {
                const union = type as Type.Union;
                return union.bounds.map(b => signature(b)).join(" | ");
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

RpcCodecs.registerCodec(Type.Kind.Primitive, {
    async rpcSend(after: Type.Primitive, q: RpcSendQueue): Promise<void> {
        await q.getAndSend(after, p => p.keyword);
    },
    async rpcReceive(before: Type.Primitive, q: RpcReceiveQueue): Promise<Type.Primitive> {
        const keyword: string = await q.receive(before.keyword);
        return Type.Primitive.fromKeyword(keyword)!;
    }
});

RpcCodecs.registerCodec(Type.Kind.Class, {
    async rpcSend(after: Type.Class, q: RpcSendQueue): Promise<void> {
        await q.getAndSend(after, c => c.classKind);
        await q.getAndSend(after, c => c.fullyQualifiedName);
        await q.getAndSendList(after, c => (c.typeParameters || []).map(t => asRef(t)), t => Type.signature(t));
        await q.getAndSend(after, c => asRef(c.supertype));
        await q.getAndSendList(after, c => (c.annotations || []).map(a => asRef(a)), t => Type.signature(t));
        await q.getAndSendList(after, c => (c.interfaces || []).map(i => asRef(i)), t => Type.signature(t));
        await q.getAndSendList(after, c => (c.members || []).map(m => asRef(m)), t => Type.signature(t));
        await q.getAndSendList(after, c => (c.methods || []).map(m => asRef(m)), t => Type.signature(t));
    },
    async rpcReceive(before: Type.Class, q: RpcReceiveQueue): Promise<Type.Class> {
        // Mutate the before object in place to preserve reference identity
        before.classKind = await q.receive(before.classKind);
        before.fullyQualifiedName = await q.receive(before.fullyQualifiedName);
        before.typeParameters = await q.receiveList(before.typeParameters) || [];
        before.supertype = await q.receive(before.supertype);
        before.annotations = await q.receiveList(before.annotations) || [];
        before.interfaces = await q.receiveList(before.interfaces) || [];
        before.members = await q.receiveList(before.members) || [];
        before.methods = await q.receiveList(before.methods) || [];

        // Add toJSON method to avoid circular reference issues during serialization
        (before as any).toJSON = function() {
            return Type.toDebugString(this);
        };
        // Mark as non-draftable to prevent immer from processing circular references
        (before as any)[immerable] = false;

        return before;
    }
});

RpcCodecs.registerCodec(Type.Kind.Variable, {
    async rpcSend(after: Type.Variable, q: RpcSendQueue): Promise<void> {
        await q.getAndSend(after, v => asRef(v.name));
        await q.getAndSend(after, v => v.owner ? asRef(v.owner) : undefined);
        await q.getAndSend(after, v => asRef(v.type));
        await q.getAndSendList(after, v => v.annotations.map(v2 => asRef(v2)), t => Type.signature(t));
    },
    async rpcReceive(before: Type.Variable, q: RpcReceiveQueue): Promise<Type.Variable> {
        const name = await q.receive(before.name);
        const owner = await q.receive(before.owner);
        const type = await q.receive(before.type);
        const annotations = await q.receiveList(before.annotations);
        const variable = {
            kind: Type.Kind.Variable,
            name,
            owner,
            type,
            annotations: annotations!,
            toJSON: function() {
                return Type.toDebugString(this);
            },
            [immerable]: false  // Mark as non-draftable
        };
        return variable;
    }
});

RpcCodecs.registerCodec(Type.Kind.Annotation, {
    async rpcSend(after: Type.Annotation, q: RpcSendQueue): Promise<void> {
        await q.getAndSend(after, a => asRef(a.type));
        await q.getAndSendList(after, a => a.values.map(v => asRef(v)), v => `${v.element ? Type.signature(v.element) : 'null'}:${v.value}`);
    },
    async rpcReceive(before: Type.Annotation, q: RpcReceiveQueue): Promise<Type.Annotation> {
        const type = await q.receive(before.type);
        const values = await q.receiveList(before.values);

        return {
            kind: Type.Kind.Annotation,
            type,
            values: values!
        };
    }
});
