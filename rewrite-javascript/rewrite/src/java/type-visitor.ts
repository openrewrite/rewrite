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
import {mapAsync, mapSync, updateIfChanged} from "../util";
import {Type} from "./type";

export class AsyncTypeVisitor<P> {
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
            const newElement = await this.visit(v.element, p);
            if (v.kind === Type.Kind.SingleElementValue) {
                const single = v as Type.Annotation.SingleElementValue;
                const newReferenceValue = single.referenceValue
                    ? await this.visit(single.referenceValue, p)
                    : undefined;
                return updateIfChanged(v, {
                    element: newElement,
                    referenceValue: newReferenceValue,
                });
            } else if (v.kind === Type.Kind.ArrayElementValue) {
                const array = v as Type.Annotation.ArrayElementValue;
                const newReferenceValues = array.referenceValues
                    ? await mapAsync(array.referenceValues, rv => this.visit(rv, p)) as Type[]
                    : undefined;
                return updateIfChanged(v, {
                    element: newElement,
                    referenceValues: newReferenceValues,
                });
            }
            return updateIfChanged(v, {element: newElement});
        });
        return updateIfChanged(annotation, {
            type: newType,
            values: newValues,
        });
    }

    protected async visitArray(array: Type.Array, p: P): Promise<Type | undefined> {
        return updateIfChanged(array, {
            elemType: await this.visit(array.elemType, p),
            annotations: await this.visitList(array.annotations, p) || [],
        });
    }

    protected async visitClass(aClass: Type.Class, p: P): Promise<Type | undefined> {
        return updateIfChanged(aClass, {
            supertype: await this.visit(aClass.supertype, p),
            owningClass: await this.visit(aClass.owningClass, p),
            annotations: await mapAsync(aClass.annotations || [], a => this.visit(a, p)),
            interfaces: await mapAsync(aClass.interfaces || [], i => this.visit(i, p)),
            members: await mapAsync(aClass.members || [], m => this.visit(m, p)),
            methods: await mapAsync(aClass.methods || [], m => this.visit(m, p)),
            typeParameters: await this.visitList(aClass.typeParameters, p) || [],
        });
    }

    protected async visitGenericTypeVariable(generic: Type.GenericTypeVariable, p: P): Promise<Type | undefined> {
        return updateIfChanged(generic, {
            bounds: await mapAsync(generic.bounds || [], b => this.visit(b, p)),
        });
    }

    protected async visitIntersection(intersection: Type.Intersection, p: P): Promise<Type | undefined> {
        return updateIfChanged(intersection, {
            bounds: await mapAsync(intersection.bounds || [], b => this.visit(b, p)),
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
            declaringType: await this.visit(method.declaringType, p),
            returnType: await this.visit(method.returnType, p),
            parameterTypes: await mapAsync(method.parameterTypes || [], pt => this.visit(pt, p)),
            thrownExceptions: await mapAsync(method.thrownExceptions || [], t => this.visit(t, p)),
            annotations: await mapAsync(method.annotations || [], a => this.visit(a, p)),
        });
    }

    protected async visitParameterized(parameterized: Type.Parameterized, p: P): Promise<Type | undefined> {
        return updateIfChanged(parameterized, {
            type: await this.visit(parameterized.type, p),
            typeParameters: await mapAsync(parameterized.typeParameters || [], t => this.visit(t, p)),
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
            type: await this.visit(variable.type, p),
            annotations: await mapAsync(variable.annotations || [], a => this.visit(a, p)),
        });
    }

    protected async visitUnknown(unknown: Type, p: P): Promise<Type | undefined> {
        // Unknown types have no properties to visit
        return unknown;
    }
}

export class TypeVisitor<P> {
    protected cursor: Cursor = rootCursor();

    visitList<T extends Type>(types: T[] | undefined, p: P): T[] | undefined {
        if (!types) {
            return undefined;
        }
        return mapSync(types, t => this.visit(t, p) as T);
    }

    preVisit(type: Type, p: P): Type | undefined {
        return type;
    }

    postVisit(type: Type, p: P): Type | undefined {
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
    visitNonNull(type: Type, p: P): Type {
        const t = this.visit(type, p);
        if (!t) {
            throw new Error("Expected non-null type");
        }
        return t;
    }

    visit<T extends Type>(type: T | undefined, p: P): T | undefined {
        if (!type) {
            return undefined;
        }

        this.cursor = new Cursor(type, this.cursor);

        let result = this.preVisit(type, p);
        if (!result) {
            this.cursor = this.cursor.parent!;
            return undefined;
        }

        switch (result.kind) {
            case Type.Kind.Array:
                result = this.visitArray(result as Type.Array, p);
                break;
            case Type.Kind.Annotation:
                result = this.visitAnnotation(result as Type.Annotation, p);
                break;
            case Type.Kind.Class:
                result = this.visitClass(result as Type.Class, p);
                break;
            case Type.Kind.GenericTypeVariable:
                result = this.visitGenericTypeVariable(result as Type.GenericTypeVariable, p);
                break;
            case Type.Kind.Intersection:
                result = this.visitIntersection(result as Type.Intersection, p);
                break;
            case Type.Kind.Union:
                result = this.visitUnion(result as Type.Union, p);
                break;
            case Type.Kind.Parameterized:
                result = this.visitParameterized(result as Type.Parameterized, p);
                break;
            case Type.Kind.Primitive:
                result = this.visitPrimitive(result as Type.Primitive, p);
                break;
            case Type.Kind.Method:
                result = this.visitMethod(result as Type.Method, p);
                break;
            case Type.Kind.Variable:
                result = this.visitVariable(result as Type.Variable, p);
                break;
            case Type.Kind.ShallowClass:
                result = this.visitClass(result as Type.ShallowClass, p);
                break;
            case Type.Kind.Unknown:
                result = this.visitUnknown(result, p);
                break;
        }

        if (result) {
            result = this.postVisit(result, p);
        }

        this.cursor = this.cursor.parent!;
        return result as T;
    }

    protected visitUnion(union: Type.Union, p: P): Type | undefined {
        return updateIfChanged(union, {
            bounds: mapSync(union.bounds || [], b => this.visit(b, p)) as Type[],
        });
    }

    protected visitAnnotation(annotation: Type.Annotation, p: P): Type | undefined {
        const newType = this.visit(annotation.type, p) as Type.FullyQualified;
        // Note: values contain element values which themselves contain Type references
        const newValues = mapSync((annotation.values || []), v => {
            const newElement = this.visit(v.element, p);
            if (v.kind === Type.Kind.SingleElementValue) {
                const single = v as Type.Annotation.SingleElementValue;
                const newReferenceValue = single.referenceValue
                    ? this.visit(single.referenceValue, p)
                    : undefined;
                return updateIfChanged(v, {
                    element: newElement,
                    referenceValue: newReferenceValue,
                });
            } else if (v.kind === Type.Kind.ArrayElementValue) {
                const array = v as Type.Annotation.ArrayElementValue;
                const newReferenceValues = array.referenceValues
                    ? mapSync(array.referenceValues, rv => this.visit(rv, p)) as Type[]
                    : undefined;
                return updateIfChanged(v, {
                    element: newElement,
                    referenceValues: newReferenceValues,
                });
            }
            return updateIfChanged(v, {element: newElement});
        });
        return updateIfChanged(annotation, {
            type: newType,
            values: newValues,
        });
    }

    protected visitArray(array: Type.Array, p: P): Type | undefined {
        return updateIfChanged(array, {
            elemType: this.visit(array.elemType, p),
            annotations: this.visitList(array.annotations, p) || [],
        });
    }

    protected visitClass(aClass: Type.Class, p: P): Type | undefined {
        return updateIfChanged(aClass, {
            supertype: this.visit(aClass.supertype, p),
            owningClass: this.visit(aClass.owningClass, p),
            annotations: mapSync((aClass.annotations || []), a => this.visit(a, p)).filter((a): a is Type.Annotation => a !== undefined),
            interfaces: mapSync((aClass.interfaces || []), i => this.visit(i, p)).filter((i): i is Type.Class => i !== undefined),
            members: mapSync((aClass.members || []), m => this.visit(m, p)).filter((m): m is Type.Variable => m !== undefined),
            methods: mapSync((aClass.methods || []), m => this.visit(m, p)).filter((m): m is Type.Method => m !== undefined),
            typeParameters: this.visitList(aClass.typeParameters, p) || [],
        });
    }

    protected visitGenericTypeVariable(generic: Type.GenericTypeVariable, p: P): Type | undefined {
        return updateIfChanged(generic, {
            bounds: mapSync((generic.bounds || []), b => this.visit(b, p)).filter((b): b is Type => b !== undefined),
        });
    }

    protected visitIntersection(intersection: Type.Intersection, p: P): Type | undefined {
        return updateIfChanged(intersection, {
            bounds: mapSync((intersection.bounds || []), b => this.visit(b, p)).filter((b): b is Type => b !== undefined),
        });
    }

    /**
     * This does not visit the declaring type to avoid a visitor cycle.
     *
     * @param method The method to visit
     * @param p Visit context
     * @return A method
     */
    protected visitMethod(method: Type.Method, p: P): Type | undefined {
        return updateIfChanged(method, {
            declaringType: this.visit(method.declaringType, p),
            returnType: this.visit(method.returnType, p),
            parameterTypes: mapSync((method.parameterTypes || []), pt => this.visit(pt, p)).filter((pt): pt is Type => pt !== undefined),
            thrownExceptions: mapSync((method.thrownExceptions || []), t => this.visit(t, p)).filter((t): t is Type.FullyQualified => t !== undefined),
            annotations: mapSync((method.annotations || []), a => this.visit(a, p)).filter((a): a is Type.Annotation => a !== undefined),
        });
    }

    protected visitParameterized(parameterized: Type.Parameterized, p: P): Type | undefined {
        return updateIfChanged(parameterized, {
            type: this.visit(parameterized.type, p),
            typeParameters: mapSync((parameterized.typeParameters || []), t => this.visit(t, p)).filter((t): t is Type => t !== undefined),
        });
    }

    protected visitPrimitive(primitive: Type.Primitive, p: P): Type | undefined {
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
    protected visitVariable(variable: Type.Variable, p: P): Type | undefined {
        return updateIfChanged(variable, {
            owner: this.visit(variable.owner, p),
            type: this.visit(variable.type, p),
            annotations: mapSync((variable.annotations || []), a => this.visit(a, p)).filter((a): a is Type.Annotation => a !== undefined),
        });
    }

    protected visitUnknown(unknown: Type, p: P): Type | undefined {
        // Unknown types have no properties to visit
        return unknown;
    }
}
