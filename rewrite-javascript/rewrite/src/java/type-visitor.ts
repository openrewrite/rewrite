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
import {Cursor, rootCursor} from "../tree";
import {mapAsync, updateIfChanged} from "../util";
import {Type} from "./type";

export class TypeVisitor<P> {
    protected cursor: Cursor = rootCursor();

    async visitList<T extends Type>(types: T[] | undefined, p: P): Promise<T[] | undefined> {
        if (!types) {
            return undefined;
        }
        return mapAsync(types, t => this.visit(t, p) as Promise<T>);
    }

    async preVisit(type: Type, p: P): Promise<Type | undefined> {
        return type;
    }

    async postVisit(type: Type, p: P): Promise<Type | undefined> {
        return type;
    }

    /**
     * By calling this method, you are asserting that you know that the outcome will be non-null
     * when the compiler couldn't otherwise prove this to be the case. This method is a shortcut
     * for having to assert the non-nullability of the returned type.
     *
     * @param type A non-null type.
     * @param p A state object that passes through the visitor.
     * @return A non-null type.
     */
    async visitNonNull(type: Type, p: P): Promise<Type> {
        const t = await this.visit(type, p);
        if (!t) {
            throw new Error("Expected non-null type");
        }
        return t;
    }

    async visit<T extends Type>(type: T | undefined, p: P): Promise<T | undefined> {
        if (!type) {
            return undefined;
        }

        this.cursor = new Cursor(type, this.cursor);

        let result = await this.preVisit(type, p);
        if (!result) {
            this.cursor = this.cursor.parent!;
            return undefined;
        }

        switch (result.kind) {
            case Type.Kind.Array:
                result = await this.visitArray(result as Type.Array, p);
                break;
            case Type.Kind.Annotation:
                result = await this.visitAnnotation(result as Type.Annotation, p);
                break;
            case Type.Kind.Class:
                result = await this.visitClass(result as Type.Class, p);
                break;
            case Type.Kind.GenericTypeVariable:
                result = await this.visitGenericTypeVariable(result as Type.GenericTypeVariable, p);
                break;
            case Type.Kind.Intersection:
                result = await this.visitIntersection(result as Type.Intersection, p);
                break;
            case Type.Kind.Union:
                result = await this.visitUnion(result as Type.Union, p);
                break;
            case Type.Kind.Parameterized:
                result = await this.visitParameterized(result as Type.Parameterized, p);
                break;
            case Type.Kind.Primitive:
                result = await this.visitPrimitive(result as Type.Primitive, p);
                break;
            case Type.Kind.Method:
                result = await this.visitMethod(result as Type.Method, p);
                break;
            case Type.Kind.Variable:
                result = await this.visitVariable(result as Type.Variable, p);
                break;
            case Type.Kind.ShallowClass:
                result = await this.visitClass(result as Type.ShallowClass, p);
                break;
            case Type.Kind.Unknown:
                result = await this.visitUnknown(result, p);
                break;
        }

        if (result) {
            result = await this.postVisit(result, p);
        }

        this.cursor = this.cursor.parent!;
        return result as T;
    }

    protected async visitUnion(union: Type.Union, p: P): Promise<Type | undefined> {
        return updateIfChanged(union, {
            bounds: await mapAsync(union.bounds || [], b => this.visit(b, p)) as Type[],
        });
    }

    protected async visitAnnotation(annotation: Type.Annotation, p: P): Promise<Type | undefined> {
        const newType = await this.visit(annotation.type, p) as Type.FullyQualified;
        // Note: values contain element values which themselves contain Type references
        const newValues = await mapAsync(annotation.values || [], async v => {
            const newElement = await this.visit(v.element, p) as Type;
            if (v.kind === Type.Kind.SingleElementValue) {
                const single = v as Type.Annotation.SingleElementValue;
                const newReferenceValue = single.referenceValue
                    ? await this.visit(single.referenceValue, p)
                    : undefined;
                return updateIfChanged(v, {
                    element: newElement,
                    referenceValue: newReferenceValue,
                } as Partial<Type.Annotation.SingleElementValue>);
            } else if (v.kind === Type.Kind.ArrayElementValue) {
                const array = v as Type.Annotation.ArrayElementValue;
                const newReferenceValues = array.referenceValues
                    ? await mapAsync(array.referenceValues, rv => this.visit(rv, p)) as Type[]
                    : undefined;
                return updateIfChanged(v, {
                    element: newElement,
                    referenceValues: newReferenceValues,
                } as Partial<Type.Annotation.ArrayElementValue>);
            }
            return updateIfChanged(v, { element: newElement });
        });
        return updateIfChanged(annotation, {
            type: newType,
            values: newValues,
        });
    }

    protected async visitArray(array: Type.Array, p: P): Promise<Type | undefined> {
        return updateIfChanged(array, {
            elemType: await this.visit(array.elemType, p) as Type,
            annotations: await this.visitList(array.annotations, p) as Type.Annotation[] || [],
        });
    }

    protected async visitClass(aClass: Type.Class, p: P): Promise<Type | undefined> {
        return updateIfChanged(aClass, {
            supertype: await this.visit(aClass.supertype, p) as Type.Class | undefined,
            owningClass: await this.visit(aClass.owningClass, p) as Type.Class | undefined,
            annotations: await mapAsync(aClass.annotations || [], a => this.visit(a, p) as Promise<Type.Annotation>),
            interfaces: await mapAsync(aClass.interfaces || [], i => this.visit(i, p) as Promise<Type.Class>),
            members: await mapAsync(aClass.members || [], m => this.visit(m, p) as Promise<Type.Variable>),
            methods: await mapAsync(aClass.methods || [], m => this.visit(m, p) as Promise<Type.Method>),
            typeParameters: await this.visitList(aClass.typeParameters, p) as Type[] || [],
        });
    }

    protected async visitGenericTypeVariable(generic: Type.GenericTypeVariable, p: P): Promise<Type | undefined> {
        return updateIfChanged(generic, {
            bounds: await mapAsync(generic.bounds || [], b => this.visit(b, p)) as Type[],
        });
    }

    protected async visitIntersection(intersection: Type.Intersection, p: P): Promise<Type | undefined> {
        return updateIfChanged(intersection, {
            bounds: await mapAsync(intersection.bounds || [], b => this.visit(b, p)) as Type[],
        });
    }

    /**
     * This does not visit the declaring type to avoid a visitor cycle.
     *
     * @param method The method to visit
     * @param p Visit context
     * @return A method
     */
    protected async visitMethod(method: Type.Method, p: P): Promise<Type | undefined> {
        return updateIfChanged(method, {
            declaringType: await this.visit(method.declaringType, p) as Type.FullyQualified,
            returnType: await this.visit(method.returnType, p) as Type,
            parameterTypes: await mapAsync(method.parameterTypes || [], pt => this.visit(pt, p)) as Type[],
            thrownExceptions: await mapAsync(method.thrownExceptions || [], t => this.visit(t, p)) as Type[],
            annotations: await mapAsync(method.annotations || [], a => this.visit(a, p) as Promise<Type.Annotation>),
        });
    }

    protected async visitParameterized(parameterized: Type.Parameterized, p: P): Promise<Type | undefined> {
        return updateIfChanged(parameterized, {
            type: await this.visit(parameterized.type, p) as Type.FullyQualified,
            typeParameters: await mapAsync(parameterized.typeParameters || [], t => this.visit(t, p)) as Type[],
        });
    }

    protected async visitPrimitive(primitive: Type.Primitive, p: P): Promise<Type | undefined> {
        // Primitives are immutable singletons, just return as-is
        return primitive;
    }

    /**
     * This does not visit the owner to avoid a visitor cycle.
     *
     * @param variable The variable to visit
     * @param p Visit context
     * @return A variable
     */
    protected async visitVariable(variable: Type.Variable, p: P): Promise<Type | undefined> {
        return updateIfChanged(variable, {
            owner: await this.visit(variable.owner, p),
            type: await this.visit(variable.type, p) as Type,
            annotations: await mapAsync(variable.annotations || [], a => this.visit(a, p) as Promise<Type.Annotation>),
        });
    }

    protected async visitUnknown(unknown: Type, p: P): Promise<Type | undefined> {
        // Unknown types have no properties to visit
        return unknown;
    }
}
