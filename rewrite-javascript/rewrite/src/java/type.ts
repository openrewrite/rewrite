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
        await q.getAndSend(after, c => asRef(c.owningClass));
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
        before.owningClass = await q.receive(before.owningClass);
        before.annotations = await q.receiveList(before.annotations) || [];
        before.interfaces = await q.receiveList(before.interfaces) || [];
        before.members = await q.receiveList(before.members) || [];
        before.methods = await q.receiveList(before.methods) || [];
        (before as any).toJSON = function () {
            return Type.signature(this);
        };
        // Mark as non-draftable to prevent immer from processing circular references
        (before as any)[immerable] = false;

        return before;
    }
});

RpcCodecs.registerCodec(Type.Kind.Variable, {
    async rpcSend(after: Type.Variable, q: RpcSendQueue): Promise<void> {
        await q.getAndSend(after, v => v.name);
        await q.getAndSend(after, v => v.owner ? asRef(v.owner) : undefined);
        await q.getAndSend(after, v => asRef(v.type));
        await q.getAndSendList(after, v => (v.annotations || []).map(v2 => asRef(v2)), t => Type.signature(t));
    },
    async rpcReceive(before: Type.Variable, q: RpcReceiveQueue): Promise<Type.Variable> {
        // Mutate the before object in place to preserve reference identity
        before.name = await q.receive(before.name);
        before.owner = await q.receive(before.owner);
        before.type = await q.receive(before.type);
        before.annotations = await q.receiveList(before.annotations) || [];
        return before;
    }
});

RpcCodecs.registerCodec(Type.Kind.Annotation, {
    async rpcSend(after: Type.Annotation, q: RpcSendQueue): Promise<void> {
        await q.getAndSend(after, a => asRef(a.type));
        await q.getAndSendList(after, a => (a.values || []).map(v => asRef(v)), v => {
            let value: any;
            if (v.kind === Type.Kind.SingleElementValue) {
                const single = v as Type.Annotation.SingleElementValue;
                value = single.constantValue !== undefined ? single.constantValue : single.referenceValue;
            } else {
                const array = v as Type.Annotation.ArrayElementValue;
                value = array.constantValues || array.referenceValues;
            }
            return `${Type.signature(v.element)}:${value == null ? "null" : value.toString()}`;
        });
    },
    async rpcReceive(before: Type.Annotation, q: RpcReceiveQueue): Promise<Type.Annotation> {
        // Mutate the before object in place to preserve reference identity
        before.type = await q.receive(before.type);
        before.values = await q.receiveList(before.values) || [];
        return before;
    }
});

RpcCodecs.registerCodec(Type.Kind.SingleElementValue, {
    async rpcSend(after: Type.Annotation.SingleElementValue, q: RpcSendQueue): Promise<void> {
        await q.getAndSend(after, e => asRef(e.element));
        await q.getAndSend(after, e => e.constantValue);
        await q.getAndSend(after, e => asRef(e.referenceValue));
    },
    async rpcReceive(before: Type.Annotation.SingleElementValue | undefined, q: RpcReceiveQueue): Promise<Type.Annotation.SingleElementValue> {
        if (!before) {
            return {
                kind: Type.Kind.SingleElementValue,
                element: (await q.receive(undefined) as unknown) as Type,
                constantValue: await q.receive(undefined),
                referenceValue: await q.receive(undefined)
            };
        }
        // Mutate the before object in place to preserve reference identity
        before.element = await q.receive(before.element);
        before.constantValue = await q.receive(before.constantValue);
        before.referenceValue = await q.receive(before.referenceValue);
        return before;
    }
});

RpcCodecs.registerCodec(Type.Kind.ArrayElementValue, {
    async rpcSend(after: Type.Annotation.ArrayElementValue, q: RpcSendQueue): Promise<void> {
        await q.getAndSend(after, e => asRef(e.element));
        await q.getAndSendList(after, e => e.constantValues || [], v => v == null ? "null" : v.toString());
        await q.getAndSendList(after, e => (e.referenceValues || []).map(r => asRef(r)), t => Type.signature(t));
    },
    async rpcReceive(before: Type.Annotation.ArrayElementValue | undefined, q: RpcReceiveQueue): Promise<Type.Annotation.ArrayElementValue> {
        if (!before) {
            return {
                kind: Type.Kind.ArrayElementValue,
                element: (await q.receive(undefined) as unknown) as Type,
                constantValues: await q.receiveList(undefined),
                referenceValues: await q.receiveList(undefined)
            };
        }
        // Mutate the before object in place to preserve reference identity
        before.element = await q.receive(before.element);
        before.constantValues = await q.receiveList(before.constantValues);
        before.referenceValues = await q.receiveList(before.referenceValues);
        return before;
    }
});

RpcCodecs.registerCodec(Type.Kind.Method, {
    async rpcSend(after: Type.Method, q: RpcSendQueue): Promise<void> {
        await q.getAndSend(after, m => asRef(m.declaringType));
        await q.getAndSend(after, m => m.name);
        await q.getAndSend(after, m => asRef(m.returnType));
        await q.getAndSendList(after, m => m.parameterNames || [], v => v);
        await q.getAndSendList(after, m => (m.parameterTypes || []).map(t => asRef(t)), t => Type.signature(t));
        await q.getAndSendList(after, m => (m.thrownExceptions || []).map(t => asRef(t)), t => Type.signature(t));
        await q.getAndSendList(after, m => (m.annotations || []).map(a => asRef(a)), t => Type.signature(t));
        await q.getAndSendList(after, m => m.defaultValue || undefined, v => v);
        await q.getAndSendList(after, m => m.declaredFormalTypeNames || [], v => v);
    },
    async rpcReceive(before: Type.Method, q: RpcReceiveQueue): Promise<Type.Method> {
        // Mutate the before object in place to preserve reference identity
        before.declaringType = await q.receive(before.declaringType);
        before.name = await q.receive(before.name);
        before.returnType = await q.receive(before.returnType);
        before.parameterNames = await q.receiveList(before.parameterNames) || [];
        before.parameterTypes = await q.receiveList(before.parameterTypes) || [];
        before.thrownExceptions = await q.receiveList(before.thrownExceptions) || [];
        before.annotations = await q.receiveList(before.annotations) || [];
        before.defaultValue = await q.receiveList(before.defaultValue);
        before.declaredFormalTypeNames = await q.receiveList(before.declaredFormalTypeNames) || [];
        return before;
    }
});

RpcCodecs.registerCodec(Type.Kind.Array, {
    async rpcSend(after: Type.Array, q: RpcSendQueue): Promise<void> {
        await q.getAndSend(after, a => asRef(a.elemType));
        await q.getAndSendList(after, a => (a.annotations || []).map(ann => asRef(ann)), t => Type.signature(t));
    },
    async rpcReceive(before: Type.Array, q: RpcReceiveQueue): Promise<Type.Array> {
        // Mutate the before object in place to preserve reference identity
        before.elemType = await q.receive(before.elemType);
        before.annotations = await q.receiveList(before.annotations) || [];
        return before;
    }
});

RpcCodecs.registerCodec(Type.Kind.Parameterized, {
    async rpcSend(after: Type.Parameterized, q: RpcSendQueue): Promise<void> {
        await q.getAndSend(after, p => asRef(p.type));
        await q.getAndSendList(after, p => (p.typeParameters || []).map(t => asRef(t)), t => Type.signature(t));
    },
    async rpcReceive(before: Type.Parameterized, q: RpcReceiveQueue): Promise<Type.Parameterized> {
        // Mutate the before object in place to preserve reference identity
        before.type = await q.receive(before.type);
        before.typeParameters = await q.receiveList(before.typeParameters) || [];
        return before;
    }
});

RpcCodecs.registerCodec(Type.Kind.GenericTypeVariable, {
    async rpcSend(after: Type.GenericTypeVariable, q: RpcSendQueue): Promise<void> {
        await q.getAndSend(after, g => g.name);
        // Convert TypeScript enum to Java enum string
        await q.getAndSend(after, g => {
            switch (g.variance) {
                case Type.GenericTypeVariable.Variance.Covariant:
                    return 'COVARIANT';
                case Type.GenericTypeVariable.Variance.Contravariant:
                    return 'CONTRAVARIANT';
                case Type.GenericTypeVariable.Variance.Invariant:
                default:
                    return 'INVARIANT';
            }
        });
        await q.getAndSendList(after, g => (g.bounds || []).map(b => asRef(b)), t => Type.signature(t));
    },
    async rpcReceive(before: Type.GenericTypeVariable, q: RpcReceiveQueue): Promise<Type.GenericTypeVariable> {
        // Mutate the before object in place to preserve reference identity
        before.name = await q.receive(before.name);
        const varianceStr = await q.receive(before.variance) as any as string;
        // Convert Java enum string to TypeScript enum
        switch (varianceStr) {
            case 'COVARIANT':
                before.variance = Type.GenericTypeVariable.Variance.Covariant;
                break;
            case 'CONTRAVARIANT':
                before.variance = Type.GenericTypeVariable.Variance.Contravariant;
                break;
            case 'INVARIANT':
            default:
                before.variance = Type.GenericTypeVariable.Variance.Invariant;
                break;
        }
        before.bounds = await q.receiveList(before.bounds) || [];
        return before;
    }
});

RpcCodecs.registerCodec(Type.Kind.Union, {
    async rpcSend(after: Type.Union, q: RpcSendQueue): Promise<void> {
        await q.getAndSendList(after, u => (u.bounds || []).map(b => asRef(b)), t => Type.signature(t));
    },
    async rpcReceive(before: Type.Union, q: RpcReceiveQueue): Promise<Type.Union> {
        // Mutate the before object in place to preserve reference identity
        before.bounds = await q.receiveList(before.bounds) || [];
        return before;
    }
});

RpcCodecs.registerCodec(Type.Kind.Intersection, {
    async rpcSend(after: Type.Intersection, q: RpcSendQueue): Promise<void> {
        await q.getAndSendList(after, i => (i.bounds || []).map(b => asRef(b)), t => Type.signature(t));
    },
    async rpcReceive(before: Type.Intersection, q: RpcReceiveQueue): Promise<Type.Intersection> {
        // Mutate the before object in place to preserve reference identity
        before.bounds = await q.receiveList(before.bounds) || [];
        return before;
    }
});

RpcCodecs.registerCodec(Type.Kind.ShallowClass, {
    async rpcSend(after: Type.ShallowClass, q: RpcSendQueue): Promise<void> {
        await q.getAndSend(after, c => c.fullyQualifiedName);
        await q.getAndSend(after, c => asRef(c.owningClass));
    },
    async rpcReceive(before: Type.ShallowClass, q: RpcReceiveQueue): Promise<Type.ShallowClass> {
        before.classKind = Type.Class.Kind.Class;
        before.fullyQualifiedName = await q.receive(before.fullyQualifiedName);
        before.owningClass = await q.receive(before.owningClass);
        return before;
    }
});

RpcCodecs.registerCodec(Type.Kind.Unknown, {
    async rpcSend(_after: Type, _q: RpcSendQueue): Promise<void> {
        // Unknown type has no additional data
    },
    async rpcReceive(_before: Type, _q: RpcReceiveQueue): Promise<Type> {
        return Type.unknownType;
    }
});
