import {AsyncJavaScriptVisitor, JavaScriptVisitor} from "./visitor";
import {J} from "../java";
import {JS, JSX} from "./tree";
import {mapSync} from "../util";
import {ElementRemovalFormatter} from "../java";

/**
 * @param visitor The visitor to add the import removal to (supports both sync and async visitors)
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
export function maybeRemoveImport(visitor: JavaScriptVisitor<any> | AsyncJavaScriptVisitor<any>, module: string, member?: string) {
    const afterVisit = visitor.afterVisit as any[];
    for (const v of afterVisit || []) {
        if (v instanceof RemoveImport && v.module === module && v.member === member) {
            return;
        }
    }
    afterVisit.push(new RemoveImport(module, member));
}

// Type alias for RightPadded elements to simplify type signatures
type RightPaddedElement<T extends J> = {
    element?: T;
    after?: J.Space;
    markers?: any;
    kind?: any;  // Add kind to match the RightPadded type structure
}

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
    private filterElementsWithPrefixPreservation<T extends J>(
        elements: RightPaddedElement<T>[],
        shouldKeep: (elem: T) => boolean,
        updatePrefix: (elem: T, prefix: J.Space) => T,
        _p: P
    ): { filtered: RightPaddedElement<T>[], allRemoved: boolean } {
        const filtered: RightPaddedElement<T>[] = [];
        let removedPrefix: J.Space | undefined;

        // Track the trailing space of the original last element
        const originalLastElement = elements[elements.length - 1];
        const originalTrailingSpace = originalLastElement?.after;

        for (const elem of elements) {
            if (elem.element && shouldKeep(elem.element)) {
                // If we removed the previous element and this is the first kept element,
                // apply the removed element's prefix to maintain formatting
                if (removedPrefix && filtered.length === 0) {
                    const updatedElement = updatePrefix(elem.element, removedPrefix);
                    filtered.push({...elem, element: updatedElement});
                    removedPrefix = undefined;
                } else {
                    filtered.push(elem);
                }
            } else if (elem.element) {
                // Store the prefix of the first removed element
                if (filtered.length === 0 && !removedPrefix) {
                    removedPrefix = elem.element.prefix;
                }
            } else {
                // Keep non-element entries (shouldn't happen but be safe)
                filtered.push(elem);
            }
        }

        // If the original last element was removed and we have remaining elements,
        // transfer its trailing space to the new last element
        if (filtered.length > 0 && originalLastElement?.element && !shouldKeep(originalLastElement.element)) {
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
    private updateImportClause(
        jsImport: JS.Import,
        importClause: JS.ImportClause,
        updateFn: (draft: any) => void,
        p: P
    ): JS.Import {
        return this.produceJavaScript(jsImport, p,  draft => {
            if (draft.importClause) {
                draft.importClause = this.produceJavaScript(
                    importClause, p,  (clauseDraft: any) => updateFn(clauseDraft)
                );
            }
        });
    }

    override visitJsCompilationUnit(compilationUnit: JS.CompilationUnit, p: P): J | undefined {
        // First, collect all used identifiers in the file
        const usedIdentifiers = new Set<string>();
        const usedTypes = new Set<string>();

        // Traverse the AST to collect used identifiers
        this.collectUsedIdentifiers(compilationUnit, usedIdentifiers, usedTypes);

        // Now process imports with knowledge of what's used
        return this.produceJavaScript(compilationUnit, p,  draft => {
            const formatter = new ElementRemovalFormatter<J>(true); // Preserve file headers from first import

            draft.statements = mapSync(compilationUnit.statements,  (stmt) => {
                const statement = stmt.element;
            
                // Handle ES6 imports
                if (statement?.kind === JS.Kind.Import) {
                    const jsImport = statement as JS.Import;
                    const result = this.processImport(jsImport, usedIdentifiers, usedTypes, p);
                    if (result === undefined) {
                        formatter.markRemoved(statement);
                        return undefined;
                    }
            
                    const finalResult = formatter.processKept(result) as JS.Import;
                    return {...stmt, element: finalResult};
                }
            
                // Handle CommonJS require statements
                // Note: const fs = require() comes as J.VariableDeclarations
                // Multi-variable declarations might come as JS.ScopedVariableDeclarations
                if (statement?.kind === J.Kind.VariableDeclarations) {
                    const varDecl = statement as J.VariableDeclarations;
                    const result = this.processRequireFromVarDecls(varDecl, usedIdentifiers, p);
                    if (result === undefined) {
                        formatter.markRemoved(statement);
                        return undefined;
                    }
            
                    const finalResult = formatter.processKept(result) as J.VariableDeclarations;
                    return {...stmt, element: finalResult};
                }
            
                // Handle JS.ScopedVariableDeclarations (multi-variable var/let/const)
                if (statement?.kind === JS.Kind.ScopedVariableDeclarations) {
                    const scopedVarDecl = statement as any;
                    // Scoped variable declarations contain a variables array where each element is a single-variable J.VariableDeclarations
                    const filteredVariables: any[] = [];
                    let hasChanges = false;
                    const varFormatter = new ElementRemovalFormatter<J.VariableDeclarations>(true); // Preserve file headers
            
                    for (const v of scopedVarDecl.variables) {
                        const varDecl = v.element;
                        if (varDecl?.kind === J.Kind.VariableDeclarations) {
                            const result = this.processRequireFromVarDecls(varDecl as J.VariableDeclarations, usedIdentifiers, p);
                            if (result === undefined) {
                                hasChanges = true;
                                varFormatter.markRemoved(varDecl);
                            } else {
                                const formattedVarDecl = varFormatter.processKept(result as J.VariableDeclarations);
                                filteredVariables.push({...v, element: formattedVarDecl});
                            }
                        } else {
                            filteredVariables.push(v);
                        }
                    }
            
                    if (filteredVariables.length === 0) {
                        formatter.markRemoved(statement);
                        return undefined;
                    }
            
                    const finalElement: any = hasChanges
                        ? formatter.processKept({...scopedVarDecl, variables: filteredVariables})
                        : formatter.processKept(statement);
            
                    return {...stmt, element: finalElement};
                }
            
                // For any other statement type, apply prefix from removed elements
                if (statement) {
                    const finalStatement = formatter.processKept(statement);
                    return {...stmt, element: finalStatement};
                }
            
                return stmt;
            });

            // Filter out undefined (removed) statements
            draft.statements = draft.statements.filter(s => s !== undefined);
            draft.eof = this.visitSpace(compilationUnit.eof, p);
        });
    }

    private processImport(
        jsImport: JS.Import,
        usedIdentifiers: Set<string>,
        usedTypes: Set<string>,
        p: P
    ): JS.Import | undefined {
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
            const defaultName = importClause.name.element;
            if (defaultName && defaultName.kind === J.Kind.Identifier) {
                const identifier = defaultName as J.Identifier;
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
                    return this.updateImportClause(jsImport, importClause,  draft => {
                        draft.name = undefined;
                        // When removing the default import, we need to transfer its prefix to namedBindings
                        // to maintain proper spacing (the default import's prefix is typically empty)
                        if (draft.namedBindings && importClause.name?.element) {
                            draft.namedBindings = this.produceJava(
                                draft.namedBindings, p,  bindingsDraft => {
                                    bindingsDraft.prefix = importClause.name!.element!.prefix;
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
                const updatedImports = this.processNamedImports(namedImports, usedIdentifiers, usedTypes, p);

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
    private processImportEqualsRequire(
        jsImport: JS.Import,
        usedIdentifiers: Set<string>,
        usedTypes: Set<string>,
        p: P
    ): JS.Import | undefined {
        const initializer = jsImport.initializer?.element;
        if (!initializer || !this.isRequireCall(initializer)) {
            return jsImport;
        }

        const methodInv = initializer as J.MethodInvocation;
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

        const importedName = (importClause.name.element as J.Identifier).simpleName;

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

    private processNamedImports(
        namedImports: JS.NamedImports,
        usedIdentifiers: Set<string>,
        usedTypes: Set<string>,
        p: P
    ): JS.NamedImports | undefined {
        const {filtered, allRemoved} = this.filterElementsWithPrefixPreservation(
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
            (elem: J, prefix: J.Space) => {
                if (elem.kind === JS.Kind.ImportSpecifier) {
                    return this.produceJavaScript(
                        elem as JS.ImportSpecifier, p,  draft => {
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
        return this.produceJavaScript(namedImports, p,  draft => {
            draft.elements = {
                ...namedImports.elements,
                elements: filtered as any
            };
        });
    }

    private processRequireFromVarDecls(
        varDecls: J.VariableDeclarations,
        usedIdentifiers: Set<string>,
        p: P
    ): J.VariableDeclarations | undefined {
        // Check if this is a require() call
        if (varDecls.variables.length !== 1) {
            return varDecls;
        }

        const namedVar = varDecls.variables[0].element;
        if (!namedVar) {
            return varDecls;
        }

        const initializer = namedVar.initializer?.element;
        if (!initializer || !this.isRequireCall(initializer)) {
            return varDecls;
        }

        const methodInv = initializer as J.MethodInvocation;

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
            const updatedPattern = this.processObjectBindingPattern(objectPattern, usedIdentifiers, p);

            if (updatedPattern === undefined) {
                return undefined; // Remove entire require
            } else if (updatedPattern !== objectPattern) {
                // Update with filtered bindings
                return this.produceJava(varDecls, p,  draft => {
                    const updatedNamedVar = this.produceJava(
                        namedVar, p,  namedDraft => {
                            namedDraft.name = updatedPattern;
                        }
                    );
                    draft.variables = [{...varDecls.variables[0], element: updatedNamedVar}];
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

        const firstArg = args[0].element;
        if (!firstArg || firstArg.kind !== J.Kind.Literal || typeof (firstArg as J.Literal).value !== 'string') {
            return undefined;
        }

        return (firstArg as J.Literal).value?.toString();
    }

    private processObjectBindingPattern(
        pattern: JS.ObjectBindingPattern,
        usedIdentifiers: Set<string>,
        p: P
    ): JS.ObjectBindingPattern | undefined {
        const {filtered, allRemoved} = this.filterElementsWithPrefixPreservation(
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
            (elem: J, prefix: J.Space) => {
                if (elem.kind === J.Kind.Identifier) {
                    return this.produceJava(
                        elem as J.Identifier, p,  draft => {
                            draft.prefix = prefix;
                        }
                    );
                } else if (elem.kind === JS.Kind.BindingElement) {
                    return this.produceJavaScript(
                        elem as JS.BindingElement, p,  draft => {
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

        return this.produceJavaScript(pattern, p,  draft => {
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
            const propertyName = alias.propertyName.element;
            if (propertyName?.kind === J.Kind.Identifier) {
                return (propertyName as J.Identifier).simpleName;
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
        const moduleSpecifier = jsImport.moduleSpecifier?.element;
        if (!moduleSpecifier || moduleSpecifier.kind !== J.Kind.Literal) {
            return false;
        }

        const literal = moduleSpecifier as J.Literal;
        const moduleName = literal.value?.toString().replace(/['"`]/g, '');

        // Match the module name
        return moduleName === this.module;
    }

    /**
     * Helper to traverse parameters from various node types
     */
    private traverseParameters(
        params: any,
        usedIdentifiers: Set<string>,
        usedTypes: Set<string>
    ): void {
        if (!params || typeof params !== 'object') return;

        if (Array.isArray(params)) {
            for (const p of params) {
                // Array elements might be RightPadded, so unwrap them
                const elem = (p as any).element || p;
                this.collectUsedIdentifiers(elem, usedIdentifiers, usedTypes);
            }
        } else if (params.elements) {
            for (const p of params.elements) {
                if (p.element) {
                    this.collectUsedIdentifiers(p.element, usedIdentifiers, usedTypes);
                }
            }
        } else if (params.parameters) {
            for (const p of params.parameters) {
                const elem = p.element || p;
                this.collectUsedIdentifiers(elem, usedIdentifiers, usedTypes);
            }
        }
    }

    /**
     * Helper to traverse statements from various node types
     */
    private traverseStatements(
        statements: any,
        usedIdentifiers: Set<string>,
        usedTypes: Set<string>
    ): void {
        if (!statements) return;

        if (Array.isArray(statements)) {
            for (const stmt of statements) {
                const element = stmt.element || stmt;
                if (element) {
                    this.collectUsedIdentifiers(element, usedIdentifiers, usedTypes);
                }
            }
        }
    }

    /**
     * Helper to check for type expressions and collect type usage
     */
    private checkTypeExpression(
        node: any,
        usedTypes: Set<string>
    ): void {
        if (node.typeExpression) {
            this.collectTypeUsage(node.typeExpression, usedTypes);
        }
    }

    private collectUsedIdentifiers(
        node: J,
        usedIdentifiers: Set<string>,
        usedTypes: Set<string>
    ): void {
        // This is a simplified version - in a real implementation,
        // we'd need to traverse the entire AST and collect all identifier usages
        // For now, we'll implement a basic traversal

        if (node.kind === J.Kind.Identifier) {
            const identifier = node as J.Identifier;
            usedIdentifiers.add(identifier.simpleName);
        } else if (node.kind === J.Kind.VariableDeclarations) {
            const varDecls = node as J.VariableDeclarations;
            // Check the type expression on the VariableDeclarations itself
            this.checkTypeExpression(varDecls, usedTypes);
            for (const v of varDecls.variables) {
                // Check the initializer
                if (v.element.initializer?.element) {
                    this.collectUsedIdentifiers(v.element.initializer.element, usedIdentifiers, usedTypes);
                }
            }
        } else if (node.kind === J.Kind.MethodInvocation) {
            const methodInv = node as J.MethodInvocation;

            // Check if this is a member access pattern like fs.readFileSync
            if (methodInv.select?.element?.kind === J.Kind.FieldAccess) {
                const fieldAccess = methodInv.select.element as J.FieldAccess;
                if (fieldAccess.target?.kind === J.Kind.Identifier) {
                    usedIdentifiers.add((fieldAccess.target as J.Identifier).simpleName);
                }
            } else if (methodInv.select?.element?.kind === J.Kind.Identifier) {
                // Direct identifier like fs in fs.method() - though this is rare
                usedIdentifiers.add((methodInv.select.element as J.Identifier).simpleName);
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
                    if (arg.element) {
                        this.collectUsedIdentifiers(arg.element, usedIdentifiers, usedTypes);
                    }
                }
            }
            // Check select (object being called on)
            if (methodInv.select?.element) {
                this.collectUsedIdentifiers(methodInv.select.element, usedIdentifiers, usedTypes);
            }
        } else if (node.kind === J.Kind.MemberReference) {
            const memberRef = node as J.MemberReference;
            if (memberRef.containing && memberRef.containing.element?.kind === J.Kind.Identifier) {
                usedIdentifiers.add((memberRef.containing.element as J.Identifier).simpleName);
            }
        } else if (node.kind === J.Kind.FieldAccess) {
            // Handle field access like fs.readFileSync
            const fieldAccess = node as J.FieldAccess;
            if (fieldAccess.target?.kind === J.Kind.Identifier) {
                usedIdentifiers.add((fieldAccess.target as J.Identifier).simpleName);
            }
            // Recursively check the target
            if (fieldAccess.target) {
                this.collectUsedIdentifiers(fieldAccess.target, usedIdentifiers, usedTypes);
            }
        } else if (node.kind === JS.Kind.CompilationUnit) {
            const cu = node as JS.CompilationUnit;
            for (const stmt of cu.statements) {
                if (stmt.element && stmt.element.kind !== JS.Kind.Import) {
                    // Skip require() statements at the top level
                    if (stmt.element.kind === J.Kind.VariableDeclarations) {
                        const varDecls = stmt.element as J.VariableDeclarations;
                        // Check if this is a require() statement
                        let isRequire = false;
                        for (const v of varDecls.variables) {
                            const namedVar = v.element;
                            if (namedVar?.initializer?.element?.kind === J.Kind.MethodInvocation) {
                                const methodInv = namedVar.initializer.element as J.MethodInvocation;
                                if (methodInv.name?.kind === J.Kind.Identifier &&
                                    (methodInv.name as J.Identifier).simpleName === 'require') {
                                    isRequire = true;
                                    break;
                                }
                            }
                        }
                        if (!isRequire) {
                            // Not a require statement, process normally
                            this.collectUsedIdentifiers(stmt.element, usedIdentifiers, usedTypes);
                        }
                    } else if (stmt.element.kind !== JS.Kind.ScopedVariableDeclarations) {
                        // Process other non-import, non-require statements normally
                        this.collectUsedIdentifiers(stmt.element, usedIdentifiers, usedTypes);
                    }
                }
            }
        } else if (node.kind === J.Kind.Return) {
            // Handle return statements
            const returnStmt = node as J.Return;
            if (returnStmt.expression) {
                this.collectUsedIdentifiers(returnStmt.expression, usedIdentifiers, usedTypes);
            }
        } else if (node.kind === J.Kind.Block) {
            const block = node as J.Block;
            for (const stmt of block.statements) {
                if (stmt.element) {
                    this.collectUsedIdentifiers(stmt.element, usedIdentifiers, usedTypes);
                }
            }
        } else if (node.kind === J.Kind.MethodDeclaration) {
            const method = node as J.MethodDeclaration;
            // Check parameters for type usage
            if (method.parameters) {
                for (const param of method.parameters.elements) {
                    // Parameters can be various types, handle them recursively
                    if (param.element) {
                        this.collectUsedIdentifiers(param.element, usedIdentifiers, usedTypes);
                    }
                }
            }
            // Check body
            if (method.body) {
                this.collectUsedIdentifiers(method.body, usedIdentifiers, usedTypes);
            }
        } else if (node.kind === JS.Kind.TypeOf) {
            // Handle typeof expressions like: typeof util
            const typeOf = node as JS.TypeOf;
            if (typeOf.expression) {
                this.collectUsedIdentifiers(typeOf.expression, usedIdentifiers, usedTypes);
            }
        } else if (node.kind === JS.Kind.TypeQuery) {
            // Handle typeof type queries like: const x: typeof util
            const typeQuery = node as JS.TypeQuery;
            if (typeQuery.typeExpression) {
                this.collectUsedIdentifiers(typeQuery.typeExpression, usedIdentifiers, usedTypes);
            }
        } else if ((node as any).typeExpression) {
            // Handle nodes with type expressions (parameters, variables, etc.)
            this.checkTypeExpression(node, usedTypes);
            // Continue traversing other parts
            if ((node as any).name) {
                this.collectUsedIdentifiers((node as any).name, usedIdentifiers, usedTypes);
            }
            if ((node as any).initializer) {
                this.collectUsedIdentifiers((node as any).initializer, usedIdentifiers, usedTypes);
            }
        } else if (node.kind === JS.Kind.ArrowFunction || node.kind === J.Kind.Lambda) {
            // Handle arrow functions and lambdas
            const func = node as any;
            if (func.parameters) {
                this.collectUsedIdentifiers(func.parameters, usedIdentifiers, usedTypes);
            }
            if (func.lambda) {
                this.collectUsedIdentifiers(func.lambda, usedIdentifiers, usedTypes);
            }
            if (func.body) {
                this.collectUsedIdentifiers(func.body, usedIdentifiers, usedTypes);
            }
        } else if (node.kind === J.Kind.Lambda) {
            const lambda = node as J.Lambda;
            if (lambda.parameters?.parameters) {
                for (const param of lambda.parameters.parameters) {
                    if (param.element) {
                        this.collectUsedIdentifiers(param.element, usedIdentifiers, usedTypes);
                    }
                }
            }
            if (lambda.body) {
                this.collectUsedIdentifiers(lambda.body, usedIdentifiers, usedTypes);
            }
        } else if (node.kind === JS.Kind.JsxTag) {
            // Handle JSX tags like <div>content</div>
            const jsxTag = node as JSX.Tag;
            // Check attributes
            if (jsxTag.attributes) {
                for (const attr of jsxTag.attributes) {
                    if (attr.element) {
                        this.collectUsedIdentifiers(attr.element, usedIdentifiers, usedTypes);
                    }
                }
            }
            // Check children
            if (jsxTag.children) {
                for (const child of jsxTag.children) {
                    if (child) {
                        this.collectUsedIdentifiers(child, usedIdentifiers, usedTypes);
                    }
                }
            }
        } else if (node.kind === JS.Kind.JsxEmbeddedExpression) {
            // Handle JSX embedded expressions like {React.version}
            const embedded = node as JSX.EmbeddedExpression;
            // The expression is wrapped in RightPadded, so unwrap it
            const expr = embedded.expression?.element || embedded.expression;
            if (expr) {
                this.collectUsedIdentifiers(expr, usedIdentifiers, usedTypes);
            }
        } else if (node.kind === JS.Kind.JsxAttribute) {
            // Handle JSX attributes like onClick={handler}
            const jsxAttr = node as any;
            if (jsxAttr.value) {
                this.collectUsedIdentifiers(jsxAttr.value, usedIdentifiers, usedTypes);
            }
        } else if (node.kind === JS.Kind.TypeDeclaration) {
            // Handle type alias declarations like: type Props = { children: React.ReactNode }
            const typeDecl = node as JS.TypeDeclaration;
            // The initializer contains the type expression (the right side of the =)
            if (typeDecl.initializer?.element) {
                this.collectTypeUsage(typeDecl.initializer.element, usedTypes);
            }
        } else if ((node as any).statements) {
            // Generic handler for nodes with statements
            this.traverseStatements((node as any).statements, usedIdentifiers, usedTypes);
        } else if ((node as any).body) {
            // Generic handler for nodes with body
            this.collectUsedIdentifiers((node as any).body, usedIdentifiers, usedTypes);
        } else if ((node as any).parameters) {
            // Handle anything with parameters (functions, methods, etc.)
            this.traverseParameters((node as any).parameters, usedIdentifiers, usedTypes);
            // Continue with the body if it exists
            if ((node as any).body) {
                this.collectUsedIdentifiers((node as any).body, usedIdentifiers, usedTypes);
            }
        }
    }

    private collectTypeUsage(typeExpr: J, usedTypes: Set<string>): void {
        if (typeExpr.kind === J.Kind.Identifier) {
            usedTypes.add((typeExpr as J.Identifier).simpleName);
        } else if (typeExpr.kind === J.Kind.ParameterizedType) {
            const paramType = typeExpr as J.ParameterizedType;
            // First, collect usage from the base type (e.g., React.Ref from React.Ref<T>)
            if (paramType.class) {
                this.collectTypeUsage(paramType.class, usedTypes);
            }
            // Then collect usage from type parameters (e.g., HTMLButtonElement from React.Ref<HTMLButtonElement>)
            if (paramType.typeParameters) {
                for (const typeParam of paramType.typeParameters.elements) {
                    if (typeParam.element) {
                        this.collectTypeUsage(typeParam.element, usedTypes);
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
                this.collectTypeUsage(fieldAccess.target, usedTypes);
            }
        } else if (typeExpr.kind === JS.Kind.Intersection) {
            // Handle intersection types like ButtonProps & { ref?: React.Ref<HTMLButtonElement> }
            const intersection = typeExpr as JS.Intersection;
            for (const typeElem of intersection.types) {
                if (typeElem.element) {
                    this.collectTypeUsage(typeElem.element, usedTypes);
                }
            }
        } else if (typeExpr.kind === JS.Kind.TypeLiteral) {
            // Handle type literals like { ref?: React.Ref<HTMLButtonElement> }
            const typeLiteral = typeExpr as JS.TypeLiteral;
            // TypeLiteral members are in a Block, which contains statements
            for (const stmt of typeLiteral.members.statements) {
                if (stmt.element) {
                    // Each statement is typically a VariableDeclarations representing a property
                    this.collectUsedIdentifiers(stmt.element, new Set(), usedTypes);
                }
            }
        } else if (typeExpr.kind === JS.Kind.TypeQuery) {
            // Handle typeof type queries like: const x: typeof util
            const typeQuery = typeExpr as JS.TypeQuery;
            if (typeQuery.typeExpression) {
                this.collectTypeUsage(typeQuery.typeExpression, usedTypes);
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
                    this.collectTypeUsage(typeOf.expression, usedTypes);
                }
            }
        } else if (typeExpr.kind === JS.Kind.TypeTreeExpression) {
            // Handle TypeTreeExpression which wraps type identifiers
            const typeTree = typeExpr as JS.TypeTreeExpression;
            if (typeTree.expression) {
                this.collectTypeUsage(typeTree.expression, usedTypes);
            }
        } else if (typeExpr.kind === JS.Kind.TypeInfo) {
            // Handle TypeInfo which contains type identifiers
            const typeInfo = typeExpr as JS.TypeInfo;
            if (typeInfo.typeIdentifier) {
                this.collectTypeUsage(typeInfo.typeIdentifier, usedTypes);
            }
        } else if ((typeExpr as any).expression) {
            // Generic handler for nodes with expression property
            this.collectTypeUsage((typeExpr as any).expression, usedTypes);
        } else if ((typeExpr as any).typeIdentifier) {
            // Generic handler for nodes with typeIdentifier property
            this.collectTypeUsage((typeExpr as any).typeIdentifier, usedTypes);
        }
        // Add more type expression handlers as needed
    }
}
