import {JavaScriptVisitor} from "./visitor";
import {J, Statement} from "../java";
import {JS, JSX} from "./tree";
import {mapAsync} from "../util";
import {ElementRemovalFormatter} from "../java";

/**
 * @param visitor The visitor to add the import removal to
 * @param module The module name (e.g., 'fs', 'react') to remove imports from
 * @param member Optionally, the specific member to remove from the import.
 *               If not specified, removes all unused imports from the module.
 *               Special values:
 *               - 'default': Removes the default import from the module if unused,
 *                 regardless of its local name (e.g., `import React from 'react'`)
 *               - '*': Removes the namespace import if unused (e.g., `import * as fs from 'fs'`)
 *
 * @example
 * // Remove a specific named import if unused
 * maybeRemoveImport(visitor, 'fs', 'readFile');
 *
 * @example
 * // Remove the default import from 'react' if unused (regardless of local name)
 * maybeRemoveImport(visitor, 'react', 'default');
 *
 * @example
 * // Remove all unused imports from 'react' module
 * maybeRemoveImport(visitor, 'react');
 *
 * @example
 * // Remove namespace import if unused
 * maybeRemoveImport(visitor, 'fs', '*');
 */
export function maybeRemoveImport(visitor: JavaScriptVisitor<any>, module: string, member?: string) {
    for (const v of visitor.afterVisit || []) {
        if (v instanceof RemoveImport && v.module === module && v.member === member) {
            return;
        }
    }
    visitor.afterVisit.push(new RemoveImport(module, member));
}

// Type alias for RightPadded elements to simplify type signatures
// With the new intersection type for RightPadded<T extends J>, padding is nested under `padding` property
type RightPaddedElement<T extends J> = T & J.RightPaddingMixin;

export class RemoveImport<P> extends JavaScriptVisitor<P> {
    /**
     * @param module The module name (e.g., 'fs', 'react') to remove imports from
     * @param member Optionally, the specific member to remove from the import.
     *               If not specified, removes all unused imports from the module.
     *               Special values:
     *               - 'default': Removes the default import from the module if unused,
     *                 regardless of its local name
     *               - '*': Removes the namespace import if unused
     */
    constructor(readonly module: string,
                readonly member?: string) {
        super();
    }

    /**
     * Generic helper to filter elements from a RightPadded array while preserving formatting.
     * When removing elements, the prefix from the first removed element is applied to the
     * first remaining element to maintain proper spacing. Also preserves trailing space
     * from the last element if it's removed.
     */
    private async filterElementsWithPrefixPreservation<T extends J>(
        elements: RightPaddedElement<T>[],
        shouldKeep: (elem: T) => boolean,
        updatePrefix: (elem: T, prefix: J.Space) => Promise<T>,
        _p: P
    ): Promise<{ filtered: RightPaddedElement<T>[], allRemoved: boolean }> {
        const filtered: RightPaddedElement<T>[] = [];
        let removedPrefix: J.Space | undefined;

        // Track the trailing space of the original last element
        const originalLastElement = elements[elements.length - 1];
        const originalTrailingSpace = originalLastElement?.padding.after;

        for (const elem of elements) {
            // With intersection types, elem IS the element with padding mixed in
            if (elem && shouldKeep(elem as T)) {
                // If we removed the previous element and this is the first kept element,
                // apply the removed element's prefix to maintain formatting
                if (removedPrefix && filtered.length === 0) {
                    const updatedElement = await updatePrefix(elem as T, removedPrefix);
                    filtered.push({...updatedElement, padding: elem.padding} as RightPaddedElement<T>);
                    removedPrefix = undefined;
                } else {
                    filtered.push(elem);
                }
            } else if (elem) {
                // Store the prefix of the first removed element
                if (filtered.length === 0 && !removedPrefix) {
                    removedPrefix = elem.prefix;
                }
            }
        }

        // If the original last element was removed and we have remaining elements,
        // transfer its trailing space to the new last element
        if (filtered.length > 0 && originalLastElement && !shouldKeep(originalLastElement as T)) {
            const lastIdx = filtered.length - 1;
            filtered[lastIdx] = {...filtered[lastIdx], after: originalTrailingSpace};
        }

        return {
            filtered,
            allRemoved: filtered.length === 0
        };
    }

    /**
     * Helper to update an import clause by removing specific bindings
     */
    private async updateImportClause(
        jsImport: JS.Import,
        importClause: JS.ImportClause,
        updateFn: (draft: any) => void | Promise<void>,
        p: P
    ): Promise<JS.Import> {
        return this.produceJavaScript(jsImport, p, async draft => {
            if (draft.importClause) {
                draft.importClause = await this.produceJavaScript(
                    importClause, p, async (clauseDraft: any) => await updateFn(clauseDraft)
                );
            }
        });
    }

    override async visitJsCompilationUnit(compilationUnit: JS.CompilationUnit, p: P): Promise<J | undefined> {
        // First, collect all used identifiers in the file
        const usedIdentifiers = new Set<string>();
        const usedTypes = new Set<string>();

        // Traverse the AST to collect used identifiers
        await this.collectUsedIdentifiers(compilationUnit, usedIdentifiers, usedTypes);

        // Now process imports with knowledge of what's used
        return this.produceJavaScript(compilationUnit, p, async draft => {
            const formatter = new ElementRemovalFormatter<J>(true); // Preserve file headers from first import

            draft.statements = await mapAsync(compilationUnit.statements, async (stmt) => {
                // Handle ES6 imports
                if (stmt?.kind === JS.Kind.Import) {
                    const jsImport = stmt as JS.Import & J.RightPaddingMixin;
                    const result = await this.processImport(jsImport, usedIdentifiers, usedTypes, p);
                    if (result === undefined) {
                        formatter.markRemoved(stmt);
                        return undefined;
                    }

                    const finalResult = formatter.processKept(result) as JS.Import;
                    return {...finalResult, padding: stmt.padding} as J.RightPadded<Statement>;
                }

                // Handle CommonJS require statements
                // Note: const fs = require() comes as J.VariableDeclarations
                // Multi-variable declarations might come as JS.ScopedVariableDeclarations
                if (stmt?.kind === J.Kind.VariableDeclarations) {
                    const varDecl = stmt as J.VariableDeclarations & J.RightPaddingMixin;
                    const result = await this.processRequireFromVarDecls(varDecl, usedIdentifiers, p);
                    if (result === undefined) {
                        formatter.markRemoved(stmt);
                        return undefined;
                    }

                    const finalResult = formatter.processKept(result) as J.VariableDeclarations;
                    return {...finalResult, padding: stmt.padding} as J.RightPadded<Statement>;
                }

                // Handle JS.ScopedVariableDeclarations (multi-variable var/let/const)
                if (stmt?.kind === JS.Kind.ScopedVariableDeclarations) {
                    const scopedVarDecl = stmt as any;
                    // Scoped variable declarations contain a variables array where each element is a single-variable J.VariableDeclarations
                    const filteredVariables: any[] = [];
                    let hasChanges = false;
                    const varFormatter = new ElementRemovalFormatter<J.VariableDeclarations>(true); // Preserve file headers

                    for (const v of scopedVarDecl.variables) {
                        if (v?.kind === J.Kind.VariableDeclarations) {
                            const result = await this.processRequireFromVarDecls(v as J.VariableDeclarations, usedIdentifiers, p);
                            if (result === undefined) {
                                hasChanges = true;
                                varFormatter.markRemoved(v);
                            } else {
                                const formattedVarDecl = varFormatter.processKept(result as J.VariableDeclarations);
                                filteredVariables.push({...formattedVarDecl, padding: v.padding});
                            }
                        } else {
                            filteredVariables.push(v);
                        }
                    }

                    if (filteredVariables.length === 0) {
                        formatter.markRemoved(stmt);
                        return undefined;
                    }

                    const finalElement: any = hasChanges
                        ? formatter.processKept({...scopedVarDecl, variables: filteredVariables})
                        : formatter.processKept(stmt);

                    return {...finalElement, padding: stmt.padding};
                }

                // For any other statement type, apply prefix from removed elements
                if (stmt) {
                    const finalStatement = formatter.processKept(stmt);
                    return {...finalStatement, padding: stmt.padding} as J.RightPadded<Statement>;
                }

                return stmt;
            });

            // Filter out undefined (removed) statements
            draft.statements = draft.statements.filter(s => s !== undefined);
            draft.eof = await this.visitSpace(compilationUnit.eof, p);
        });
    }

    private async processImport(
        jsImport: JS.Import,
        usedIdentifiers: Set<string>,
        usedTypes: Set<string>,
        p: P
    ): Promise<JS.Import | undefined> {
        // Handle import-equals-require syntax: import util = require("util");
        if (jsImport.initializer) {
            return this.processImportEqualsRequire(jsImport, usedIdentifiers, usedTypes, p);
        }

        // Check if this import is from the target module
        if (!this.isTargetModule(jsImport)) {
            return jsImport;
        }

        const importClause = jsImport.importClause;
        if (!importClause) {
            // Side-effect import like: import 'module'
            if (this.member === '*') {
                return undefined; // Remove the entire import
            }
            return jsImport;
        }

        // Process default import
        if (importClause.name) {
            // With intersection types, name IS the Identifier with padding mixed in
            const defaultName = importClause.name;
            if (defaultName && defaultName.kind === J.Kind.Identifier) {
                const identifier = defaultName as J.Identifier & J.RightPaddingMixin;
                const name = identifier.simpleName;

                // Check if we should remove this default import
                let shouldRemove: boolean;
                if (this.member === 'default') {
                    // Special case: member 'default' means remove any default import from the target module if unused
                    shouldRemove = !usedIdentifiers.has(name) && !usedTypes.has(name);
                } else {
                    // Regular case: check if the import name matches the removal criteria
                    shouldRemove = this.shouldRemoveImport(name, usedIdentifiers, usedTypes);
                }

                if (shouldRemove) {
                    // If there are no named imports, remove the entire import
                    if (!importClause.namedBindings) {
                        return undefined;
                    }
                    // Otherwise, just remove the default import and fix spacing
                    return this.updateImportClause(jsImport, importClause, async draft => {
                        draft.name = undefined;
                        // When removing the default import, we need to transfer its prefix to namedBindings
                        // to maintain proper spacing (the default import's prefix is typically empty)
                        if (draft.namedBindings && importClause.name) {
                            draft.namedBindings = await this.produceJava(
                                draft.namedBindings, p, async bindingsDraft => {
                                    bindingsDraft.prefix = importClause.name!.prefix;
                                }
                            );
                        }
                    }, p);
                }
            }
        }

        // Process named imports
        if (importClause.namedBindings) {
            const namedBindings = importClause.namedBindings;

            // Handle namespace import: import * as X from 'module'
            if (namedBindings.kind === J.Kind.Identifier) {
                const identifier = namedBindings as J.Identifier;
                const name = identifier.simpleName;

                // When removing a specific member from a namespace import,
                // we can only remove the entire namespace if it's not used
                if (this.member !== undefined) {
                    // We're trying to remove a specific member from this namespace
                    // Check if the namespace itself is used
                    if (!usedIdentifiers.has(name) && !usedTypes.has(name)) {
                        // Namespace is not used, remove the entire import
                        if (!importClause.name) {
                            return undefined;
                        }
                        return this.updateImportClause(jsImport, importClause, draft => {
                            draft.namedBindings = undefined;
                        }, p);
                    }
                    // Namespace is used, we can't remove individual members from it
                } else if (this.shouldRemoveImport(name, usedIdentifiers, usedTypes)) {
                    // If there's no default import, remove the entire import
                    if (!importClause.name) {
                        return undefined;
                    }
                    // Otherwise, just remove the namespace import
                    return this.updateImportClause(jsImport, importClause, draft => {
                        draft.namedBindings = undefined;
                    }, p);
                }
            } else if (namedBindings.kind === JS.Kind.Alias) {
                // Handle import * as X from 'module' - represented as Alias with propertyName = "*"
                const alias = namedBindings as JS.Alias;
                const aliasName = (alias.alias as J.Identifier).simpleName;

                // When removing a specific member from a namespace import,
                // we can only remove the entire namespace if it's not used
                if (this.member !== undefined) {
                    // We're trying to remove a specific member from this namespace
                    // Check if the namespace itself is used
                    if (!usedIdentifiers.has(aliasName) && !usedTypes.has(aliasName)) {
                        // Namespace is not used, remove the entire import
                        if (!importClause.name) {
                            return undefined;
                        }
                        return this.updateImportClause(jsImport, importClause, draft => {
                            draft.namedBindings = undefined;
                        }, p);
                    }
                    // Namespace is used, we can't remove individual members from it
                } else if (this.shouldRemoveImport(aliasName, usedIdentifiers, usedTypes)) {
                    // If there's no default import, remove the entire import
                    if (!importClause.name) {
                        return undefined;
                    }
                    // Otherwise, just remove the namespace import
                    return this.updateImportClause(jsImport, importClause, draft => {
                        draft.namedBindings = undefined;
                    }, p);
                }
            }

            // Handle named imports: import { a, b } from 'module'
            if (namedBindings.kind === JS.Kind.NamedImports) {
                const namedImports = namedBindings as JS.NamedImports;
                const updatedImports = await this.processNamedImports(namedImports, usedIdentifiers, usedTypes, p);

                if (updatedImports === undefined) {
                    // All named imports were removed
                    if (!importClause.name) {
                        // No default import either, remove the entire import
                        return undefined;
                    }
                    // Keep the import with just the default import
                    return this.updateImportClause(jsImport, importClause, draft => {
                        draft.namedBindings = undefined;
                    }, p);
                } else if (updatedImports !== namedImports) {
                    // Some named imports were removed
                    return this.updateImportClause(jsImport, importClause, draft => {
                        draft.namedBindings = updatedImports;
                    }, p);
                }
            }
        }

        return jsImport;
    }

    /**
     * Process TypeScript import-equals-require syntax: import util = require("util");
     * This is represented as a JS.Import with an initializer containing the require() call.
     */
    private async processImportEqualsRequire(
        jsImport: JS.Import,
        usedIdentifiers: Set<string>,
        usedTypes: Set<string>,
        p: P
    ): Promise<JS.Import | undefined> {
        // With intersection types, initializer IS the expression with padding mixed in
        const initializer = jsImport.initializer;
        if (!initializer || !this.isRequireCall(initializer)) {
            return jsImport;
        }

        const methodInv = initializer as J.MethodInvocation & J.LeftPaddingMixin;
        const moduleName = this.getModuleNameFromRequire(methodInv);
        if (!moduleName || !this.matchesTargetModule(moduleName)) {
            return jsImport;
        }

        // Get the import name from the importClause
        const importClause = jsImport.importClause;
        if (!importClause || !importClause.name) {
            // No name, this is unusual for import-equals-require
            return jsImport;
        }

        // With intersection types, name IS the Identifier with padding mixed in
        const importedName = (importClause.name as J.Identifier & J.RightPaddingMixin).simpleName;

        // For import-equals-require, we can only remove the entire import since
        // it imports the whole module as a single identifier
        if (this.shouldRemoveIdentifier(importedName, usedIdentifiers, usedTypes)) {
            return undefined;
        }

        return jsImport;
    }

    /**
     * Check if a node is a require() method invocation
     */
    private isRequireCall(node: J): boolean {
        if (node.kind !== J.Kind.MethodInvocation) {
            return false;
        }
        const methodInv = node as J.MethodInvocation;
        return methodInv.name?.kind === J.Kind.Identifier &&
               (methodInv.name as J.Identifier).simpleName === 'require';
    }

    /**
     * Check if the module name matches the target module
     */
    private matchesTargetModule(moduleName: string): boolean {
        return moduleName === this.module;
    }

    /**
     * Check if an identifier should be removed based on usage
     */
    private shouldRemoveIdentifier(name: string, usedIdentifiers: Set<string>, usedTypes: Set<string>): boolean {
        // For CommonJS and import-equals-require, we're removing the entire import
        // if the identifier is not used (member is typically undefined for these cases,
        // or we're checking if a specific binding is used)
        return !usedIdentifiers.has(name) && !usedTypes.has(name);
    }

    private async processNamedImports(
        namedImports: JS.NamedImports,
        usedIdentifiers: Set<string>,
        usedTypes: Set<string>,
        p: P
    ): Promise<JS.NamedImports | undefined> {
        const {filtered, allRemoved} = await this.filterElementsWithPrefixPreservation(
            namedImports.elements.elements,
            (elem: J) => {
                if (elem.kind === JS.Kind.ImportSpecifier) {
                    const specifier = elem as JS.ImportSpecifier;
                    const importName = this.getImportName(specifier);
                    const aliasName = this.getImportAlias(specifier);

                    // For aliased imports, check if the alias is used
                    // For non-aliased imports, check if the import name is used
                    const nameToCheck = aliasName || importName;

                    // Check if we should remove this import
                    if (this.member !== undefined) {
                        // We're removing a specific member - check if this matches
                        if (this.member === importName) {
                            // This is the member we want to remove - check if it's used
                            return usedIdentifiers.has(nameToCheck) || usedTypes.has(nameToCheck);
                        }
                        return true; // Keep imports that don't match the member
                    } else {
                        // We're removing based on the import name itself
                        return !this.shouldRemoveImport(importName, usedIdentifiers, usedTypes);
                    }
                }
                return true; // Keep non-ImportSpecifier elements
            },
            async (elem: J, prefix: J.Space) => {
                if (elem.kind === JS.Kind.ImportSpecifier) {
                    return this.produceJavaScript(
                        elem as JS.ImportSpecifier, p, async draft => {
                            draft.prefix = prefix;
                        }
                    );
                }
                return elem;
            },
            p
        );

        if (allRemoved) {
            return undefined;
        }

        if (filtered.length === namedImports.elements.elements.length) {
            return namedImports; // No changes
        }

        // Create updated named imports with filtered elements
        return this.produceJavaScript(namedImports, p, async draft => {
            draft.elements = {
                ...namedImports.elements,
                elements: filtered as any
            };
        });
    }

    private async processRequireFromVarDecls(
        varDecls: J.VariableDeclarations,
        usedIdentifiers: Set<string>,
        p: P
    ): Promise<J.VariableDeclarations | undefined> {
        // Check if this is a require() call
        if (varDecls.variables.length !== 1) {
            return varDecls;
        }

        // With intersection types, the variable IS the NamedVariable with padding mixed in
        const namedVar = varDecls.variables[0];
        if (!namedVar) {
            return varDecls;
        }

        // With intersection types, initializer IS the expression with padding mixed in
        const initializer = namedVar.initializer;
        if (!initializer || !this.isRequireCall(initializer)) {
            return varDecls;
        }

        // Cast through unknown for intersection type to specific type
        const methodInv = initializer as unknown as J.MethodInvocation;

        // This is a require() statement
        const pattern = namedVar.name;
        if (!pattern) {
            return varDecls;
        }

        // Handle: const fs = require('fs')
        if (pattern.kind === J.Kind.Identifier) {
            const varName = (pattern as J.Identifier).simpleName;

            // For require() statements, check the module name from the require call
            const moduleName = this.getModuleNameFromRequire(methodInv);
            if (moduleName && this.matchesTargetModule(moduleName) && !usedIdentifiers.has(varName)) {
                return undefined; // Remove the entire require statement
            }
        }

        // Handle: const { readFile } = require('fs')
        if (pattern.kind === JS.Kind.ObjectBindingPattern && this.member !== undefined) {
            const objectPattern = pattern as JS.ObjectBindingPattern;
            const updatedPattern = await this.processObjectBindingPattern(objectPattern, usedIdentifiers, p);

            if (updatedPattern === undefined) {
                return undefined; // Remove entire require
            } else if (updatedPattern !== objectPattern) {
                // Update with filtered bindings
                return this.produceJava(varDecls, p, async draft => {
                    const updatedNamedVar = await this.produceJava(
                        namedVar as J.VariableDeclarations.NamedVariable, p, async namedDraft => {
                            namedDraft.name = updatedPattern;
                        }
                    );
                    // Preserve padding fields when updating
                    draft.variables = [{...updatedNamedVar, padding: varDecls.variables[0].padding} as J.RightPadded<J.VariableDeclarations.NamedVariable>];
                });
            }
        }

        return varDecls;
    }

    /**
     * Get the module name from a require() call
     */
    private getModuleNameFromRequire(methodInv: J.MethodInvocation): string | undefined {
        const args = methodInv.arguments?.elements;
        if (!args || args.length === 0) {
            return undefined;
        }

        // With intersection types, the arg IS the expression with padding mixed in
        const firstArg = args[0];
        if (!firstArg || firstArg.kind !== J.Kind.Literal || typeof (firstArg as J.Literal & J.RightPaddingMixin).value !== 'string') {
            return undefined;
        }

        return (firstArg as J.Literal & J.RightPaddingMixin).value?.toString();
    }

    private async processObjectBindingPattern(
        pattern: JS.ObjectBindingPattern,
        usedIdentifiers: Set<string>,
        p: P
    ): Promise<JS.ObjectBindingPattern | undefined> {
        const {filtered, allRemoved} = await this.filterElementsWithPrefixPreservation(
            pattern.bindings.elements,
            (elem: J) => {
                if (elem.kind === JS.Kind.BindingElement) {
                    const name = this.getBindingElementName(elem as JS.BindingElement);
                    return !this.shouldRemoveImport(name, usedIdentifiers, new Set());
                } else if (elem.kind === J.Kind.Identifier) {
                    const name = (elem as J.Identifier).simpleName;
                    return !this.shouldRemoveImport(name, usedIdentifiers, new Set());
                }
                return true; // Keep other element types
            },
            async (elem: J, prefix: J.Space) => {
                if (elem.kind === J.Kind.Identifier) {
                    return this.produceJava(
                        elem as J.Identifier, p, async draft => {
                            draft.prefix = prefix;
                        }
                    );
                } else if (elem.kind === JS.Kind.BindingElement) {
                    return this.produceJavaScript(
                        elem as JS.BindingElement, p, async draft => {
                            draft.prefix = prefix;
                        }
                    );
                }
                return elem;
            },
            p
        );

        if (allRemoved) {
            return undefined;
        }

        if (filtered.length === pattern.bindings.elements.length) {
            return pattern;
        }

        return this.produceJavaScript(pattern, p, async draft => {
            draft.bindings = {
                ...pattern.bindings,
                elements: filtered as any
            };
        });
    }

    private getImportName(specifier: JS.ImportSpecifier): string {
        const spec = specifier.specifier;
        if (spec?.kind === JS.Kind.Alias) {
            // Handle aliased import: import { foo as bar }
            // Return the original name (foo)
            const alias = spec as JS.Alias;
            // With intersection types, propertyName IS the identifier with padding mixed in
            const propertyName = alias.propertyName;
            if (propertyName?.kind === J.Kind.Identifier) {
                return (propertyName as J.Identifier & J.RightPaddingMixin).simpleName;
            }
        } else if (spec?.kind === J.Kind.Identifier) {
            // Handle regular import: import { foo }
            return (spec as J.Identifier).simpleName;
        }
        return '';
    }

    private getImportAlias(specifier: JS.ImportSpecifier): string | undefined {
        const spec = specifier.specifier;
        if (spec?.kind === JS.Kind.Alias) {
            // Handle aliased import: import { foo as bar }
            // Return the alias name (bar)
            const alias = spec as JS.Alias;
            if (alias.alias?.kind === J.Kind.Identifier) {
                return (alias.alias as J.Identifier).simpleName;
            }
        }
        // No alias for regular imports
        return undefined;
    }

    private getBindingElementName(bindingElement: JS.BindingElement): string {
        const name = bindingElement.name;
        if (name?.kind === J.Kind.Identifier) {
            return (name as J.Identifier).simpleName;
        }
        return '';
    }

    private shouldRemoveImport(
        name: string,
        usedIdentifiers: Set<string>,
        usedTypes: Set<string>
    ): boolean {
        // If member is specified, we're removing a specific member from the module
        if (this.member !== undefined) {
            // Only remove if this is the specific member we're looking for
            if (this.member !== name) {
                return false;
            }
        }
        // If no member specified, we're removing all unused imports from the module
        // So we check if this particular import is unused

        // Check if it's used
        return !(usedIdentifiers.has(name) || usedTypes.has(name));
    }

    private isTargetModule(jsImport: JS.Import): boolean {
        // Always check if the import is from the specified module
        // With intersection types, moduleSpecifier IS the literal with padding mixed in
        const moduleSpecifier = jsImport.moduleSpecifier;
        if (!moduleSpecifier || moduleSpecifier.kind !== J.Kind.Literal) {
            return false;
        }

        const literal = moduleSpecifier as J.Literal & J.LeftPaddingMixin;
        const moduleName = literal.value?.toString().replace(/['"`]/g, '');

        // Match the module name
        return moduleName === this.module;
    }

    /**
     * Helper to traverse parameters from various node types
     */
    private async traverseParameters(
        params: any,
        usedIdentifiers: Set<string>,
        usedTypes: Set<string>
    ): Promise<void> {
        if (!params || typeof params !== 'object') return;

        if (Array.isArray(params)) {
            for (const p of params) {
                // Array elements might be RightPadded, so unwrap them
                const elem = (p as any).element || p;
                await this.collectUsedIdentifiers(elem, usedIdentifiers, usedTypes);
            }
        } else if (params.elements) {
            for (const p of params.elements) {
                if (p.element) {
                    await this.collectUsedIdentifiers(p.element, usedIdentifiers, usedTypes);
                }
            }
        } else if (params.parameters) {
            for (const p of params.parameters) {
                const elem = p.element || p;
                await this.collectUsedIdentifiers(elem, usedIdentifiers, usedTypes);
            }
        }
    }

    /**
     * Helper to traverse statements from various node types
     */
    private async traverseStatements(
        statements: any,
        usedIdentifiers: Set<string>,
        usedTypes: Set<string>
    ): Promise<void> {
        if (!statements) return;

        if (Array.isArray(statements)) {
            for (const stmt of statements) {
                const element = stmt.element || stmt;
                if (element) {
                    await this.collectUsedIdentifiers(element, usedIdentifiers, usedTypes);
                }
            }
        }
    }

    /**
     * Helper to check for type expressions and collect type usage
     */
    private async checkTypeExpression(
        node: any,
        usedTypes: Set<string>
    ): Promise<void> {
        if (node.typeExpression) {
            await this.collectTypeUsage(node.typeExpression, usedTypes);
        }
    }

    private async collectUsedIdentifiers(
        node: J,
        usedIdentifiers: Set<string>,
        usedTypes: Set<string>
    ): Promise<void> {
        // This is a simplified version - in a real implementation,
        // we'd need to traverse the entire AST and collect all identifier usages
        // For now, we'll implement a basic traversal

        if (node.kind === J.Kind.Identifier) {
            const identifier = node as J.Identifier;
            usedIdentifiers.add(identifier.simpleName);
        } else if (node.kind === J.Kind.VariableDeclarations) {
            const varDecls = node as J.VariableDeclarations;
            // Check the type expression on the VariableDeclarations itself
            await this.checkTypeExpression(varDecls, usedTypes);
            for (const v of varDecls.variables) {
                // With intersection types, v IS the NamedVariable with padding mixed in
                // Check the initializer (which is also an intersection type for tree nodes)
                if (v.initializer) {
                    await this.collectUsedIdentifiers(v.initializer, usedIdentifiers, usedTypes);
                }
            }
        } else if (node.kind === J.Kind.MethodInvocation) {
            const methodInv = node as J.MethodInvocation;

            // Check if this is a member access pattern like fs.readFileSync
            // With intersection types, select IS the expression with padding mixed in
            if (methodInv.select?.kind === J.Kind.FieldAccess) {
                const fieldAccess = methodInv.select as J.FieldAccess & J.RightPaddingMixin;
                if (fieldAccess.target?.kind === J.Kind.Identifier) {
                    usedIdentifiers.add((fieldAccess.target as J.Identifier).simpleName);
                }
            } else if (methodInv.select?.kind === J.Kind.Identifier) {
                // Direct identifier like fs in fs.method() - though this is rare
                usedIdentifiers.add((methodInv.select as J.Identifier & J.RightPaddingMixin).simpleName);
            } else if (!methodInv.select) {
                // No select means this is a direct function call like isArray()
                // Only in this case should we add the method name as a used identifier
                if (methodInv.name && methodInv.name.kind === J.Kind.Identifier) {
                    usedIdentifiers.add((methodInv.name as J.Identifier).simpleName);
                }
            }
            // Note: We don't add method names for calls like Array.isArray() or obj.method()
            // because those are methods on objects, not standalone imported functions
            // Recursively check arguments
            if (methodInv.arguments) {
                for (const arg of methodInv.arguments.elements) {
                    // With intersection types, arg IS the expression with padding mixed in
                    if (arg) {
                        await this.collectUsedIdentifiers(arg, usedIdentifiers, usedTypes);
                    }
                }
            }
            // Check select (object being called on)
            if (methodInv.select) {
                await this.collectUsedIdentifiers(methodInv.select, usedIdentifiers, usedTypes);
            }
        } else if (node.kind === J.Kind.MemberReference) {
            const memberRef = node as J.MemberReference;
            // With intersection types, containing IS the expression with padding mixed in
            if (memberRef.containing && memberRef.containing.kind === J.Kind.Identifier) {
                usedIdentifiers.add((memberRef.containing as J.Identifier & J.RightPaddingMixin).simpleName);
            }
        } else if (node.kind === J.Kind.FieldAccess) {
            // Handle field access like fs.readFileSync
            const fieldAccess = node as J.FieldAccess;
            if (fieldAccess.target?.kind === J.Kind.Identifier) {
                usedIdentifiers.add((fieldAccess.target as J.Identifier).simpleName);
            }
            // Recursively check the target
            if (fieldAccess.target) {
                await this.collectUsedIdentifiers(fieldAccess.target, usedIdentifiers, usedTypes);
            }
        } else if (node.kind === JS.Kind.CompilationUnit) {
            const cu = node as JS.CompilationUnit;
            for (const stmt of cu.statements) {
                // With intersection types, stmt IS the statement with padding mixed in
                if (stmt && stmt.kind !== JS.Kind.Import) {
                    // Skip require() statements at the top level
                    if (stmt.kind === J.Kind.VariableDeclarations) {
                        const varDecls = stmt as J.VariableDeclarations & J.RightPaddingMixin;
                        // Check if this is a require() statement
                        let isRequire = false;
                        for (const v of varDecls.variables) {
                            // v IS the NamedVariable with padding mixed in
                            const namedVar = v;
                            // initializer IS the expression with padding mixed in
                            if (namedVar?.initializer?.kind === J.Kind.MethodInvocation) {
                                const methodInv = namedVar.initializer as J.MethodInvocation & J.LeftPaddingMixin;
                                if (methodInv.name?.kind === J.Kind.Identifier &&
                                    (methodInv.name as J.Identifier).simpleName === 'require') {
                                    isRequire = true;
                                    break;
                                }
                            }
                        }
                        if (!isRequire) {
                            // Not a require statement, process normally
                            await this.collectUsedIdentifiers(stmt, usedIdentifiers, usedTypes);
                        }
                    } else if (stmt.kind !== JS.Kind.ScopedVariableDeclarations) {
                        // Process other non-import, non-require statements normally
                        await this.collectUsedIdentifiers(stmt, usedIdentifiers, usedTypes);
                    }
                }
            }
        } else if (node.kind === J.Kind.Return) {
            // Handle return statements
            const returnStmt = node as J.Return;
            if (returnStmt.expression) {
                await this.collectUsedIdentifiers(returnStmt.expression, usedIdentifiers, usedTypes);
            }
        } else if (node.kind === J.Kind.Block) {
            const block = node as J.Block;
            for (const stmt of block.statements) {
                // With intersection types, stmt IS the statement with padding mixed in
                if (stmt) {
                    await this.collectUsedIdentifiers(stmt, usedIdentifiers, usedTypes);
                }
            }
        } else if (node.kind === J.Kind.MethodDeclaration) {
            const method = node as J.MethodDeclaration;
            // Check parameters for type usage
            if (method.parameters) {
                for (const param of method.parameters.elements) {
                    // With intersection types, param IS the parameter with padding mixed in
                    if (param) {
                        await this.collectUsedIdentifiers(param, usedIdentifiers, usedTypes);
                    }
                }
            }
            // Check body
            if (method.body) {
                await this.collectUsedIdentifiers(method.body, usedIdentifiers, usedTypes);
            }
        } else if (node.kind === JS.Kind.TypeOf) {
            // Handle typeof expressions like: typeof util
            const typeOf = node as JS.TypeOf;
            if (typeOf.expression) {
                await this.collectUsedIdentifiers(typeOf.expression, usedIdentifiers, usedTypes);
            }
        } else if (node.kind === JS.Kind.TypeQuery) {
            // Handle typeof type queries like: const x: typeof util
            const typeQuery = node as JS.TypeQuery;
            if (typeQuery.typeExpression) {
                await this.collectUsedIdentifiers(typeQuery.typeExpression, usedIdentifiers, usedTypes);
            }
        } else if ((node as any).typeExpression) {
            // Handle nodes with type expressions (parameters, variables, etc.)
            await this.checkTypeExpression(node, usedTypes);
            // Continue traversing other parts
            if ((node as any).name) {
                await this.collectUsedIdentifiers((node as any).name, usedIdentifiers, usedTypes);
            }
            if ((node as any).initializer) {
                await this.collectUsedIdentifiers((node as any).initializer, usedIdentifiers, usedTypes);
            }
        } else if (node.kind === JS.Kind.ArrowFunction || node.kind === J.Kind.Lambda) {
            // Handle arrow functions and lambdas
            const func = node as any;
            if (func.parameters) {
                await this.collectUsedIdentifiers(func.parameters, usedIdentifiers, usedTypes);
            }
            if (func.lambda) {
                await this.collectUsedIdentifiers(func.lambda, usedIdentifiers, usedTypes);
            }
            if (func.body) {
                await this.collectUsedIdentifiers(func.body, usedIdentifiers, usedTypes);
            }
        } else if (node.kind === J.Kind.Lambda) {
            const lambda = node as J.Lambda;
            if (lambda.parameters?.parameters) {
                for (const param of lambda.parameters.parameters) {
                    // With intersection types, param IS the parameter with padding mixed in
                    if (param) {
                        await this.collectUsedIdentifiers(param, usedIdentifiers, usedTypes);
                    }
                }
            }
            if (lambda.body) {
                await this.collectUsedIdentifiers(lambda.body, usedIdentifiers, usedTypes);
            }
        } else if (node.kind === JS.Kind.JsxTag) {
            // Handle JSX tags like <div>content</div>
            const jsxTag = node as JSX.Tag;
            // Check attributes
            if (jsxTag.attributes) {
                for (const attr of jsxTag.attributes) {
                    // With intersection types, attr IS the attribute with padding mixed in
                    if (attr) {
                        await this.collectUsedIdentifiers(attr, usedIdentifiers, usedTypes);
                    }
                }
            }
            // Check children
            if (jsxTag.children) {
                for (const child of jsxTag.children) {
                    if (child) {
                        await this.collectUsedIdentifiers(child, usedIdentifiers, usedTypes);
                    }
                }
            }
        } else if (node.kind === JS.Kind.JsxEmbeddedExpression) {
            // Handle JSX embedded expressions like {React.version}
            const embedded = node as JSX.EmbeddedExpression;
            // With intersection types, expression IS the expression with padding mixed in
            const expr = embedded.expression;
            if (expr) {
                await this.collectUsedIdentifiers(expr, usedIdentifiers, usedTypes);
            }
        } else if (node.kind === JS.Kind.JsxAttribute) {
            // Handle JSX attributes like onClick={handler}
            const jsxAttr = node as any;
            if (jsxAttr.value) {
                await this.collectUsedIdentifiers(jsxAttr.value, usedIdentifiers, usedTypes);
            }
        } else if (node.kind === JS.Kind.TypeDeclaration) {
            // Handle type alias declarations like: type Props = { children: React.ReactNode }
            const typeDecl = node as JS.TypeDeclaration;
            // With intersection types, initializer IS the type expression with padding mixed in
            if (typeDecl.initializer) {
                await this.collectTypeUsage(typeDecl.initializer, usedTypes);
            }
        } else if ((node as any).statements) {
            // Generic handler for nodes with statements
            await this.traverseStatements((node as any).statements, usedIdentifiers, usedTypes);
        } else if ((node as any).body) {
            // Generic handler for nodes with body
            await this.collectUsedIdentifiers((node as any).body, usedIdentifiers, usedTypes);
        } else if ((node as any).parameters) {
            // Handle anything with parameters (functions, methods, etc.)
            await this.traverseParameters((node as any).parameters, usedIdentifiers, usedTypes);
            // Continue with the body if it exists
            if ((node as any).body) {
                await this.collectUsedIdentifiers((node as any).body, usedIdentifiers, usedTypes);
            }
        }
    }

    private async collectTypeUsage(typeExpr: J, usedTypes: Set<string>): Promise<void> {
        if (typeExpr.kind === J.Kind.Identifier) {
            usedTypes.add((typeExpr as J.Identifier).simpleName);
        } else if (typeExpr.kind === J.Kind.ParameterizedType) {
            const paramType = typeExpr as J.ParameterizedType;
            // First, collect usage from the base type (e.g., React.Ref from React.Ref<T>)
            if (paramType.class) {
                await this.collectTypeUsage(paramType.class, usedTypes);
            }
            // Then collect usage from type parameters (e.g., HTMLButtonElement from React.Ref<HTMLButtonElement>)
            if (paramType.typeParameters) {
                for (const typeParam of paramType.typeParameters.elements) {
                    // With intersection types, typeParam IS the type with padding mixed in
                    if (typeParam) {
                        await this.collectTypeUsage(typeParam, usedTypes);
                    }
                }
            }
        } else if (typeExpr.kind === J.Kind.FieldAccess) {
            // Handle qualified names in type positions like React.Ref
            const fieldAccess = typeExpr as J.FieldAccess;
            if (fieldAccess.target?.kind === J.Kind.Identifier) {
                usedTypes.add((fieldAccess.target as J.Identifier).simpleName);
            } else if (fieldAccess.target) {
                // Recursively handle nested field accesses
                await this.collectTypeUsage(fieldAccess.target, usedTypes);
            }
        } else if (typeExpr.kind === JS.Kind.Intersection) {
            // Handle intersection types like ButtonProps & { ref?: React.Ref<HTMLButtonElement> }
            const intersection = typeExpr as JS.Intersection;
            for (const typeElem of intersection.types) {
                // With intersection types, typeElem IS the type with padding mixed in
                if (typeElem) {
                    await this.collectTypeUsage(typeElem, usedTypes);
                }
            }
        } else if (typeExpr.kind === JS.Kind.TypeLiteral) {
            // Handle type literals like { ref?: React.Ref<HTMLButtonElement> }
            const typeLiteral = typeExpr as JS.TypeLiteral;
            // TypeLiteral members are in a Block, which contains statements
            for (const stmt of typeLiteral.members.statements) {
                // With intersection types, stmt IS the statement with padding mixed in
                if (stmt) {
                    // Each statement is typically a VariableDeclarations representing a property
                    await this.collectUsedIdentifiers(stmt, new Set(), usedTypes);
                }
            }
        } else if (typeExpr.kind === JS.Kind.TypeQuery) {
            // Handle typeof type queries like: const x: typeof util
            const typeQuery = typeExpr as JS.TypeQuery;
            if (typeQuery.typeExpression) {
                await this.collectTypeUsage(typeQuery.typeExpression, usedTypes);
            }
        } else if (typeExpr.kind === JS.Kind.TypeOf) {
            // Handle typeof expressions in types
            const typeOf = typeExpr as JS.TypeOf;
            if (typeOf.expression) {
                // For typeof expressions, the expression contains the identifier
                // Add it to usedTypes since it's used in a type context
                if (typeOf.expression.kind === J.Kind.Identifier) {
                    usedTypes.add((typeOf.expression as J.Identifier).simpleName);
                } else {
                    await this.collectTypeUsage(typeOf.expression, usedTypes);
                }
            }
        } else if (typeExpr.kind === JS.Kind.TypeTreeExpression) {
            // Handle TypeTreeExpression which wraps type identifiers
            const typeTree = typeExpr as JS.TypeTreeExpression;
            if (typeTree.expression) {
                await this.collectTypeUsage(typeTree.expression, usedTypes);
            }
        } else if (typeExpr.kind === JS.Kind.TypeInfo) {
            // Handle TypeInfo which contains type identifiers
            const typeInfo = typeExpr as JS.TypeInfo;
            if (typeInfo.typeIdentifier) {
                await this.collectTypeUsage(typeInfo.typeIdentifier, usedTypes);
            }
        } else if ((typeExpr as any).expression) {
            // Generic handler for nodes with expression property
            await this.collectTypeUsage((typeExpr as any).expression, usedTypes);
        } else if ((typeExpr as any).typeIdentifier) {
            // Generic handler for nodes with typeIdentifier property
            await this.collectTypeUsage((typeExpr as any).typeIdentifier, usedTypes);
        }
        // Add more type expression handlers as needed
    }
}
