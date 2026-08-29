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

import { Option, Recipe } from "../../recipe";
import { TreeVisitor } from "../../visitor";
import { ExecutionContext } from "../../execution";
import { JavaScriptVisitor, JS } from "../index";
import { maybeRebind } from "../binding";
import { J, Type } from "../../java";
import { create as produce } from "mutative";

/**
 * Changes an import from one module to another, updating all type attributions.
 *
 * This recipe is useful for:
 * - Library migrations (e.g., moving `act` from `react-dom/test-utils` to `react`)
 * - Module restructuring (e.g., split packages)
 * - Renaming exported members
 *
 * @example
 * // Migrate act import from react-dom/test-utils to react
 * const recipe = new ChangeImport({
 *     oldModule: "react-dom/test-utils",
 *     oldMember: "act",
 *     newModule: "react"
 * });
 * // Before: import { act } from 'react-dom/test-utils';
 * // After:  import { act } from 'react';
 *
 * @example
 * // Rename a member, carrying the references that resolve to it
 * const recipe = new ChangeImport({
 *     oldModule: "lodash",
 *     oldMember: "extend",
 *     newModule: "lodash",
 *     newMember: "assign"
 * });
 * // Before: import { extend } from 'lodash';  extend({}, {});
 * // After:  import { assign } from 'lodash';  assign({}, {});
 * // Adding `newAlias: "extend"` instead binds it as `import { assign as extend }`.
 */
export class ChangeImport extends Recipe {
    readonly name = "org.openrewrite.javascript.change-import";
    readonly displayName = "Change import";
    readonly description = "Changes an import from one module/member to another, updating all type attributions.";

    @Option({
        displayName: "Old module",
        description: "The module to change imports from",
        example: "react-dom/test-utils"
    })
    oldModule!: string;

    @Option({
        displayName: "Old member",
        description: "The member to change (or 'default' for default imports, '*' for namespace imports)",
        example: "act"
    })
    oldMember!: string;

    @Option({
        displayName: "New module",
        description: "The module to change imports to",
        example: "react"
    })
    newModule!: string;

    @Option({
        displayName: "New member",
        description: "The new member name. If not specified, keeps the same member name.",
        example: "act",
        required: false
    })
    newMember?: string;

    @Option({
        displayName: "New alias",
        description: "The local name to bind the new member under. Defaults to the alias the import " +
            "already had, or to the new member name where it had none.",
        example: "act",
        required: false
    })
    newAlias?: string;

    constructor(options?: {
        oldModule?: string;
        oldMember?: string;
        newModule?: string;
        newMember?: string;
        newAlias?: string;
    }) {
        super(options);
    }

    async editor(): Promise<TreeVisitor<any, ExecutionContext>> {
        const oldModule = this.oldModule;
        const oldMember = this.oldMember;
        const newModule = this.newModule;
        const newMember = this.newMember ?? oldMember;
        const newAlias = this.newAlias;

        // Build the old and new FQNs for type attribution updates
        const oldFqn = oldMember === 'default' || oldMember === '*'
            ? oldModule
            : `${oldModule}.${oldMember}`;
        const newFqn = newMember === 'default' || newMember === '*'
            ? newModule
            : `${newModule}.${newMember}`;

        return new class extends JavaScriptVisitor<ExecutionContext> {
            private hasOldImport = false;

            override async visitJsCompilationUnit(cu: JS.CompilationUnit, ctx: ExecutionContext): Promise<J | undefined> {
                this.hasOldImport = maybeRebind(this, {
                    from: {module: oldModule, member: oldMember},
                    to: {module: newModule, member: newMember, alias: newAlias}
                }) !== undefined;

                return super.visitJsCompilationUnit(cu, ctx);
            }

            override async visitIdentifier(identifier: J.Identifier, ctx: ExecutionContext): Promise<J | undefined> {
                let ident = await super.visitIdentifier(identifier, ctx) as J.Identifier;

                if (!this.hasOldImport) {
                    return ident;
                }

                // Check and update type attribution
                let changed = false;

                // Update type if it references the old module
                const updatedType = this.updateType(ident.type);
                if (updatedType !== ident.type) {
                    changed = true;
                }

                // Update fieldType if it references the old module
                // fieldType is specifically Type.Variable, so we need to handle it specially
                let updatedFieldType: Type.Variable | undefined = ident.fieldType;
                if (ident.fieldType) {
                    const updated = this.updateVariableType(ident.fieldType);
                    if (updated !== ident.fieldType) {
                        updatedFieldType = updated;
                        changed = true;
                    }
                }

                if (changed) {
                    return produce(ident, draft => {
                        if (updatedType !== ident.type) {
                            draft.type = updatedType;
                        }
                        if (updatedFieldType !== ident.fieldType) {
                            draft.fieldType = updatedFieldType;
                        }
                    });
                }

                return ident;
            }

            override async visitMethodInvocation(method: J.MethodInvocation, ctx: ExecutionContext): Promise<J | undefined> {
                let m = await super.visitMethodInvocation(method, ctx) as J.MethodInvocation;

                if (!this.hasOldImport) {
                    return m;
                }

                // Update methodType if it references the old module
                const updatedMethodType = this.updateMethodType(m.methodType);
                if (updatedMethodType !== m.methodType) {
                    return produce(m, draft => {
                        draft.methodType = updatedMethodType;
                    });
                }

                return m;
            }

            override async visitFieldAccess(fieldAccess: J.FieldAccess, ctx: ExecutionContext): Promise<J | undefined> {
                let fa = await super.visitFieldAccess(fieldAccess, ctx) as J.FieldAccess;

                if (!this.hasOldImport) {
                    return fa;
                }

                // Update type if it references the old module
                const updatedType = this.updateType(fa.type);
                if (updatedType !== fa.type) {
                    return produce(fa, draft => {
                        draft.type = updatedType;
                    });
                }

                return fa;
            }

            override async visitFunctionCall(functionCall: JS.FunctionCall, ctx: ExecutionContext): Promise<J | undefined> {
                let fc = await super.visitFunctionCall(functionCall, ctx) as JS.FunctionCall;

                if (!this.hasOldImport) {
                    return fc;
                }

                // Update methodType if it references the old module
                const updatedMethodType = this.updateMethodType(fc.methodType);
                if (updatedMethodType !== fc.methodType) {
                    return produce(fc, draft => {
                        draft.methodType = updatedMethodType;
                    });
                }

                return fc;
            }

            override async visitNewClass(newClass: J.NewClass, ctx: ExecutionContext): Promise<J | undefined> {
                let nc = await super.visitNewClass(newClass, ctx) as J.NewClass;

                if (!this.hasOldImport) {
                    return nc;
                }

                let changed = false;

                // Update methodType if it references the old module
                const updatedMethodType = this.updateMethodType(nc.methodType);
                if (updatedMethodType !== nc.methodType) {
                    changed = true;
                }

                // Update constructorType if it references the old module
                const updatedConstructorType = this.updateMethodType(nc.constructorType);
                if (updatedConstructorType !== nc.constructorType) {
                    changed = true;
                }

                // Update type if it references the old module
                const updatedType = this.updateType(nc.type);
                if (updatedType !== nc.type) {
                    changed = true;
                }

                if (changed) {
                    return produce(nc, draft => {
                        if (updatedMethodType !== nc.methodType) {
                            draft.methodType = updatedMethodType;
                        }
                        if (updatedConstructorType !== nc.constructorType) {
                            draft.constructorType = updatedConstructorType;
                        }
                        if (updatedType !== nc.type) {
                            draft.type = updatedType;
                        }
                    });
                }

                return nc;
            }

            /**
             * Update a type if it references the old module
             */
            private updateType(type: Type | undefined): Type | undefined {
                if (!type) return type;

                switch (type.kind) {
                    case Type.Kind.Class:
                    case Type.Kind.ShallowClass:
                        return this.updateClassType(type as Type.Class);

                    case Type.Kind.Method:
                        return this.updateMethodType(type as Type.Method);

                    case Type.Kind.Variable:
                        return this.updateVariableType(type as Type.Variable);

                    case Type.Kind.Parameterized:
                        return this.updateParameterizedType(type as Type.Parameterized);

                    case Type.Kind.Array:
                        return this.updateArrayType(type as Type.Array);

                    default:
                        return type;
                }
            }

            /**
             * Update a Class type if its FQN references the old module
             */
            private updateClassType(classType: Type.Class): Type.Class {
                let changed = false;
                let newFullyQualifiedName = classType.fullyQualifiedName;
                let newOwningClass = classType.owningClass;

                // Check if the FQN matches or starts with the old module
                if (classType.fullyQualifiedName === oldFqn) {
                    newFullyQualifiedName = newFqn;
                    changed = true;
                } else if (classType.fullyQualifiedName === oldModule) {
                    newFullyQualifiedName = newModule;
                    changed = true;
                } else if (classType.fullyQualifiedName.startsWith(oldModule + '.')) {
                    newFullyQualifiedName = newModule + classType.fullyQualifiedName.substring(oldModule.length);
                    changed = true;
                }

                // Recursively update owningClass
                if (classType.owningClass) {
                    const updatedOwningClass = this.updateClassType(classType.owningClass);
                    if (updatedOwningClass !== classType.owningClass) {
                        newOwningClass = updatedOwningClass;
                        changed = true;
                    }
                }

                if (changed) {
                    // Type objects are marked as non-draftable, so we manually create new objects
                    return {
                        ...classType,
                        fullyQualifiedName: newFullyQualifiedName,
                        owningClass: newOwningClass
                    } as Type.Class;
                }

                return classType;
            }

            /**
             * Update a Method type if its declaringType references the old module
             */
            private updateMethodType(methodType: Type.Method | undefined): Type.Method | undefined {
                if (!methodType) return methodType;

                // Update the declaring type
                if (Type.isFullyQualified(methodType.declaringType)) {
                    const declaringTypeFqn = Type.FullyQualified.getFullyQualifiedName(methodType.declaringType);

                    if (declaringTypeFqn === oldModule ||
                        declaringTypeFqn === oldFqn ||
                        declaringTypeFqn.startsWith(oldModule + '.')) {

                        // Need to update the declaring type
                        const updatedDeclaringType = this.updateType(methodType.declaringType) as Type.FullyQualified;

                        // Also update the method name if we're renaming the member
                        const updatedName = (oldMember !== 'default' && oldMember !== '*' &&
                            methodType.name === oldMember && newMember !== oldMember)
                            ? newMember
                            : methodType.name;

                        // Type objects are marked as non-draftable, so we manually create new objects
                        return {
                            ...methodType,
                            declaringType: updatedDeclaringType,
                            name: updatedName
                        } as Type.Method;
                    }
                }

                return methodType;
            }

            /**
             * Update a Variable type if its owner references the old module
             */
            private updateVariableType(variableType: Type.Variable): Type.Variable {
                let changed = false;
                let newOwner = variableType.owner;
                let newInnerType = variableType.type;

                // Update owner if it references the old module
                if (variableType.owner) {
                    const updatedOwner = this.updateType(variableType.owner);
                    if (updatedOwner !== variableType.owner) {
                        newOwner = updatedOwner;
                        changed = true;
                    }
                }

                // Update inner type if it references the old module
                const updatedInnerType = this.updateType(variableType.type);
                if (updatedInnerType !== variableType.type) {
                    newInnerType = updatedInnerType!;
                    changed = true;
                }

                if (changed) {
                    // Type objects are marked as non-draftable, so we manually create new objects
                    return {
                        ...variableType,
                        owner: newOwner,
                        type: newInnerType
                    } as Type.Variable;
                }

                return variableType;
            }

            /**
             * Update a Parameterized type if its base type references the old module
             */
            private updateParameterizedType(paramType: Type.Parameterized): Type.Parameterized {
                let changed = false;
                let newBaseType = paramType.type;
                let newTypeParams = paramType.typeParameters;

                // Update base type
                if (Type.isFullyQualified(paramType.type)) {
                    const updatedType = this.updateType(paramType.type) as Type.FullyQualified;
                    if (updatedType !== paramType.type) {
                        newBaseType = updatedType;
                        changed = true;
                    }
                }

                // Update type parameters
                const updatedParams = paramType.typeParameters.map(tp => this.updateType(tp)!);
                if (updatedParams.some((p, i) => p !== paramType.typeParameters[i])) {
                    newTypeParams = updatedParams;
                    changed = true;
                }

                if (changed) {
                    // Type objects are marked as non-draftable, so we manually create new objects
                    return {
                        ...paramType,
                        type: newBaseType,
                        typeParameters: newTypeParams
                    } as Type.Parameterized;
                }

                return paramType;
            }

            /**
             * Update an Array type if its element type references the old module
             */
            private updateArrayType(arrayType: Type.Array): Type.Array {
                const updatedElemType = this.updateType(arrayType.elemType);
                if (updatedElemType !== arrayType.elemType) {
                    // Type objects are marked as non-draftable, so we manually create new objects
                    return {
                        ...arrayType,
                        elemType: updatedElemType!
                    } as Type.Array;
                }
                return arrayType;
            }
        }();
    }
}
