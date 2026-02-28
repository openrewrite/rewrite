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

import {Option, Recipe} from "../../recipe";
import {TreeVisitor} from "../../visitor";
import {ExecutionContext} from "../../execution";
import {JavaScriptVisitor, JS} from "../index";
import {maybeAddImport} from "../add-import";
import {Expression, J, isIdentifier, Statement, Type} from "../../java";
import {create as produce, Draft} from "mutative";

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
 * // Change a named import to a different name
 * const recipe = new ChangeImport({
 *     oldModule: "lodash",
 *     oldMember: "extend",
 *     newModule: "lodash",
 *     newMember: "assign"
 * });
 * // Before: import { extend } from 'lodash';
 * // After:  import { assign } from 'lodash';
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
        description: "Optional alias for the new import. Required when newMember is 'default' or '*'.",
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
            private oldAlias?: string;
            private transformedImport = false;

            override async visitJsCompilationUnit(cu: JS.CompilationUnit, ctx: ExecutionContext): Promise<J | undefined> {
                // Reset tracking for each file
                this.hasOldImport = false;
                this.oldAlias = undefined;
                this.transformedImport = false;

                // First pass: check if the old import exists and capture any alias
                for (const statement of cu.statements) {
                    const stmt = statement as Statement;
                    if (stmt.kind === JS.Kind.Import) {
                        const jsImport = stmt as JS.Import;
                        const aliasInfo = this.checkForOldImport(jsImport);
                        if (aliasInfo.found) {
                            this.hasOldImport = true;
                            this.oldAlias = aliasInfo.alias;
                            break;
                        }
                    }
                }

                // Visit the compilation unit (this will transform imports via visitJsImport)
                let result = await super.visitJsCompilationUnit(cu, ctx) as JS.CompilationUnit;

                // If we transformed an import but need to add to existing import from new module,
                // or if we only removed a member from a multi-import, use maybeAddImport
                if (this.hasOldImport && !this.transformedImport) {
                    const aliasToUse = newAlias ?? this.oldAlias;

                    if (newMember === 'default') {
                        maybeAddImport(this, {
                            module: newModule,
                            member: 'default',
                            alias: aliasToUse,
                            onlyIfReferenced: false
                        });
                    } else if (newMember === '*') {
                        maybeAddImport(this, {
                            module: newModule,
                            member: '*',
                            alias: aliasToUse,
                            onlyIfReferenced: false
                        });
                    } else if (aliasToUse && aliasToUse !== newMember) {
                        maybeAddImport(this, {
                            module: newModule,
                            member: newMember,
                            alias: aliasToUse,
                            onlyIfReferenced: false
                        });
                    } else {
                        maybeAddImport(this, {
                            module: newModule,
                            member: newMember,
                            onlyIfReferenced: false
                        });
                    }
                }

                return result;
            }

            override async visitImportDeclaration(jsImport: JS.Import, ctx: ExecutionContext): Promise<J | undefined> {
                let imp = await super.visitImportDeclaration(jsImport, ctx) as JS.Import;

                if (!this.hasOldImport) {
                    return imp;
                }

                const aliasInfo = this.checkForOldImport(imp);
                if (!aliasInfo.found) {
                    return imp;
                }

                // Check if this is the only import from the old module
                const namedImports = this.getNamedImports(imp);
                const isOnlyImport = namedImports.length === 1 ||
                    (oldMember === 'default' && !imp.importClause?.namedBindings) ||
                    (oldMember === '*');

                if (isOnlyImport) {
                    // Transform the module specifier in place
                    this.transformedImport = true;
                    return produce(imp, draft => {
                        if (draft.moduleSpecifier) {
                            // For tree types, the padded value IS the element (intersection type)
                            const literal = draft.moduleSpecifier as unknown as Draft<J.Literal>;
                            literal.value = newModule;
                            // Update valueSource to preserve quote style
                            const originalSource = literal.valueSource || `"${oldModule}"`;
                            const quoteChar = originalSource.startsWith("'") ? "'" : '"';
                            literal.valueSource = `${quoteChar}${newModule}${quoteChar}`;
                        }
                        // If we're also renaming the member, update the import specifier
                        if (newMember !== oldMember && oldMember !== 'default' && oldMember !== '*') {
                            const importClause = draft.importClause;
                            if (importClause?.namedBindings?.kind === JS.Kind.NamedImports) {
                                const namedImports = importClause.namedBindings as Draft<JS.NamedImports>;
                                for (const elem of namedImports.elements.elements) {
                                    // For tree types, elem IS the specifier with padding mixed in
                                    const specifier = elem as unknown as Draft<JS.ImportSpecifier>;
                                    if (specifier.specifier.kind === J.Kind.Identifier &&
                                        specifier.specifier.simpleName === oldMember) {
                                        specifier.specifier.simpleName = newMember;
                                    }
                                }
                            }
                        }
                    });
                } else {
                    // Remove just the specific member from the import
                    // maybeAddImport will add the new import
                    return this.removeNamedImportMember(imp, oldMember, ctx);
                }
            }

            private async removeNamedImportMember(imp: JS.Import, memberToRemove: string, _ctx: ExecutionContext): Promise<JS.Import> {
                return produce(imp, draft => {
                    const importClause = draft.importClause;
                    if (!importClause?.namedBindings) return;
                    if (importClause.namedBindings.kind !== JS.Kind.NamedImports) return;

                    const namedImports = importClause.namedBindings as Draft<JS.NamedImports>;
                    const elements = namedImports.elements.elements;
                    const filteredElements = elements.filter(elem => {
                        // For tree types, elem IS the specifier with padding mixed in
                        const specifier = elem as unknown as Draft<JS.ImportSpecifier>;
                        const specifierNode = specifier.specifier;

                        if (specifierNode.kind === J.Kind.Identifier) {
                            return specifierNode.simpleName !== memberToRemove;
                        }

                        if (specifierNode.kind === JS.Kind.Alias) {
                            const alias = specifierNode as JS.Alias;
                            const propertyName = alias.propertyName;
                            if (propertyName.kind === J.Kind.Identifier) {
                                return propertyName.simpleName !== memberToRemove;
                            }
                        }

                        return true;
                    });

                    namedImports.elements.elements = filteredElements;
                });
            }

            private getNamedImports(imp: JS.Import): string[] {
                const imports: string[] = [];
                const importClause = imp.importClause;
                if (!importClause) return imports;

                const namedBindings = importClause.namedBindings;
                if (!namedBindings || namedBindings.kind !== JS.Kind.NamedImports) return imports;

                const namedImports = namedBindings as JS.NamedImports;
                for (const elem of namedImports.elements.elements) {
                    const specifier = elem;
                    const specifierNode = specifier.specifier;

                    if (isIdentifier(specifierNode)) {
                        imports.push(specifierNode.simpleName);
                    } else if (specifierNode.kind === JS.Kind.Alias) {
                        const alias = specifierNode as JS.Alias;
                        const propertyName = alias.propertyName;
                        if (isIdentifier(propertyName)) {
                            imports.push(propertyName.simpleName);
                        }
                    }
                }

                return imports;
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

            private checkForOldImport(jsImport: JS.Import): { found: boolean; alias?: string } {
                // Check if this import is from the old module
                const moduleSpecifier = jsImport.moduleSpecifier;
                if (!moduleSpecifier) return { found: false };

                const literal = moduleSpecifier;
                if (literal.kind !== J.Kind.Literal) return { found: false };

                const value = (literal as J.Literal).value;
                if (value !== oldModule) return { found: false };

                const importClause = jsImport.importClause;
                if (!importClause) {
                    // Side-effect import - not what we're looking for
                    return { found: false };
                }

                // Check for default import
                if (oldMember === 'default') {
                    if (importClause.name) {
                        const nameElem = importClause.name;
                        if (isIdentifier(nameElem)) {
                            return { found: true, alias: nameElem.simpleName };
                        }
                    }
                    return { found: false };
                }

                // Check for namespace import
                if (oldMember === '*') {
                    const namedBindings = importClause.namedBindings;
                    if (namedBindings?.kind === JS.Kind.Alias) {
                        const alias = namedBindings as JS.Alias;
                        if (isIdentifier(alias.alias)) {
                            return { found: true, alias: alias.alias.simpleName };
                        }
                    }
                    return { found: false };
                }

                // Check for named imports
                const namedBindings = importClause.namedBindings;
                if (!namedBindings) return { found: false };

                if (namedBindings.kind !== JS.Kind.NamedImports) return { found: false };

                const namedImports = namedBindings as JS.NamedImports;
                const elements = namedImports.elements.elements;

                for (const elem of elements) {
                    const specifier = elem;
                    const specifierNode = specifier.specifier;

                    // Handle direct import: import { act }
                    if (isIdentifier(specifierNode) && specifierNode.simpleName === oldMember) {
                        return { found: true };
                    }

                    // Handle aliased import: import { act as something }
                    if (specifierNode.kind === JS.Kind.Alias) {
                        const alias = specifierNode as JS.Alias;
                        const propertyName = alias.propertyName;
                        if (isIdentifier(propertyName) && propertyName.simpleName === oldMember) {
                            if (isIdentifier(alias.alias)) {
                                return { found: true, alias: alias.alias.simpleName };
                            }
                        }
                    }
                }

                return { found: false };
            }
        }();
    }
}
