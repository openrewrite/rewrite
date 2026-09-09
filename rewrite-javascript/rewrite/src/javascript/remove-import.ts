import {JavaScriptVisitor} from "./visitor";
import {J} from "../java";
import {bindingNames, namesReferencedWithin} from "./scope";
import {JS, JSX} from "./tree";
import {mapAsync, updateIfChanged} from "../util";
import {ElementRemovalFormatter} from "../java";

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
        const originalTrailingSpace = originalLastElement?.after;

        for (const elem of elements) {
            if (elem.element && shouldKeep(elem.element)) {
                // If we removed the previous element and this is the first kept element,
                // apply the removed element's prefix to maintain formatting
                if (removedPrefix && filtered.length === 0) {
                    const updatedElement = await updatePrefix(elem.element, removedPrefix);
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
        const usedNames = collectUsedNames(compilationUnit);
        return this.produceJavaScript(compilationUnit, p, async draft => {
            const formatter = new ElementRemovalFormatter<J>(true); // Preserve file headers from first import

            const newStatements = await mapAsync(compilationUnit.statements, async (stmt) => {
                const statement = stmt.element;

                // Handle ES6 imports
                if (statement?.kind === JS.Kind.Import) {
                    const jsImport = statement as JS.Import;
                    const result = await this.processImport(jsImport, usedNames, p);
                    if (result === undefined) {
                        formatter.markRemoved(statement);
                        return undefined;
                    }

                    const finalResult = formatter.processKept(result) as JS.Import;
                    return updateIfChanged(stmt, {element: finalResult});
                }

                // Handle CommonJS require statements
                // Note: const fs = require() comes as J.VariableDeclarations
                // Multi-variable declarations might come as JS.ScopedVariableDeclarations
                if (statement?.kind === J.Kind.VariableDeclarations) {
                    const varDecl = statement as J.VariableDeclarations;
                    const result = await this.processRequireFromVarDecls(varDecl, usedNames, p);
                    if (result === undefined) {
                        formatter.markRemoved(statement);
                        return undefined;
                    }

                    const finalResult = formatter.processKept(result) as J.VariableDeclarations;
                    return updateIfChanged(stmt, {element: finalResult});
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
                            const result = await this.processRequireFromVarDecls(varDecl as J.VariableDeclarations, usedNames, p);
                            if (result === undefined) {
                                hasChanges = true;
                                varFormatter.markRemoved(varDecl);
                            } else {
                                const formattedVarDecl = varFormatter.processKept(result as J.VariableDeclarations);
                                filteredVariables.push(updateIfChanged(v, {element: formattedVarDecl}));
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

                    return updateIfChanged(stmt, {element: finalElement});
                }

                // For any other statement type, apply prefix from removed elements
                if (statement) {
                    const finalStatement = formatter.processKept(statement);
                    return updateIfChanged(stmt, {element: finalStatement});
                }

                return stmt;
            });

            draft.statements = newStatements.some(s => s === undefined)
                ? newStatements.filter(s => s !== undefined)
                : newStatements;
            draft.eof = await this.visitSpace(compilationUnit.eof, p);
        });
    }

    private async processImport(
        jsImport: JS.Import,
        usedNames: Set<string>,
        p: P
    ): Promise<JS.Import | undefined> {
        // Handle import-equals-require syntax: import util = require("util");
        if (jsImport.initializer) {
            return this.processImportEqualsRequire(jsImport, usedNames, p);
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
                    shouldRemove = !usedNames.has(name);
                } else {
                    // Regular case: check if the import name matches the removal criteria
                    shouldRemove = this.shouldRemoveImport(name, usedNames, name);
                }

                if (shouldRemove) {
                    // If there are no named imports, remove the entire import
                    if (!importClause.namedBindings) {
                        return undefined;
                    }
                    // Otherwise, just remove the default import and fix spacing
                    return this.updateImportClause(jsImport, importClause, async draft => {
                        draft.name = undefined;
                        // When removing the default import, we need to fix up spacing on the
                        // namedBindings. The space between "import" and the clause content is
                        // already in importClause.prefix, so we clear both namedBindings.prefix
                        // and elements.before (the space before "{") to avoid a double space.
                        if (draft.namedBindings && importClause.name?.element) {
                            draft.namedBindings = await this.produceJava(
                                draft.namedBindings, p, async bindingsDraft => {
                                    bindingsDraft.prefix = importClause.name!.element!.prefix;
                                    if (bindingsDraft.elements) {
                                        bindingsDraft.elements = {
                                            ...bindingsDraft.elements,
                                            before: importClause.name!.element!.prefix
                                        };
                                    }
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
                    if (!usedNames.has(name)) {
                        // Namespace is not used, remove the entire import
                        if (!importClause.name) {
                            return undefined;
                        }
                        return this.updateImportClause(jsImport, importClause, draft => {
                            draft.namedBindings = undefined;
                        }, p);
                    }
                    // Namespace is used, we can't remove individual members from it
                } else if (this.shouldRemoveImport(name, usedNames, name)) {
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
                    if (!usedNames.has(aliasName)) {
                        // Namespace is not used, remove the entire import
                        if (!importClause.name) {
                            return undefined;
                        }
                        return this.updateImportClause(jsImport, importClause, draft => {
                            draft.namedBindings = undefined;
                        }, p);
                    }
                    // Namespace is used, we can't remove individual members from it
                } else if (this.shouldRemoveImport(aliasName, usedNames, aliasName)) {
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
                const updatedImports = await this.processNamedImports(namedImports, usedNames, p);

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
        usedNames: Set<string>,
        p: P
    ): Promise<JS.Import | undefined> {
        const initializer = jsImport.initializer?.element;
        if (!initializer || !isRequireCall(initializer)) {
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
        if (this.shouldRemoveIdentifier(importedName, usedNames)) {
            return undefined;
        }

        return jsImport;
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
    private shouldRemoveIdentifier(name: string, usedNames: Set<string>): boolean {
        // For CommonJS and import-equals-require, we're removing the entire import
        // if the identifier is not used (member is typically undefined for these cases,
        // or we're checking if a specific binding is used)
        return !usedNames.has(name);
    }

    private async processNamedImports(
        namedImports: JS.NamedImports,
        usedNames: Set<string>,
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
                            return usedNames.has(nameToCheck);
                        }
                        return true; // Keep imports that don't match the member
                    } else {
                        // We're removing based on the import name itself
                        return !this.shouldRemoveImport(importName, usedNames, importName);
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
        usedNames: Set<string>,
        p: P
    ): Promise<J.VariableDeclarations | undefined> {
        // Check if this is a require() call
        if (varDecls.variables.length !== 1) {
            return varDecls;
        }

        const namedVar = varDecls.variables[0].element;
        if (!namedVar) {
            return varDecls;
        }

        const initializer = namedVar.initializer?.element;
        if (!initializer || !isRequireCall(initializer)) {
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
            if (moduleName && this.matchesTargetModule(moduleName) && !usedNames.has(varName)) {
                return undefined; // Remove the entire require statement
            }
        }

        // Handle: const { readFile } = require('fs')
        if (pattern.kind === JS.Kind.ObjectBindingPattern && this.member !== undefined) {
            const objectPattern = pattern as JS.ObjectBindingPattern;
            const updatedPattern = await this.processObjectBindingPattern(objectPattern, usedNames, p);

            if (updatedPattern === undefined) {
                return undefined; // Remove entire require
            } else if (updatedPattern !== objectPattern) {
                // Update with filtered bindings
                return this.produceJava(varDecls, p, async draft => {
                    const updatedNamedVar = await this.produceJava(
                        namedVar, p, async namedDraft => {
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

    private async processObjectBindingPattern(
        pattern: JS.ObjectBindingPattern,
        usedNames: Set<string>,
        p: P
    ): Promise<JS.ObjectBindingPattern | undefined> {
        const {filtered, allRemoved} = await this.filterElementsWithPrefixPreservation(
            pattern.bindings.elements,
            (elem: J) => {
                const bound = this.boundByPatternElement(elem);
                // An element whose names cannot be read is left alone.
                return bound.length === 0 ||
                    bound.some(b => !this.shouldRemoveImport(b.name, usedNames, b.member));
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

    /** The names a pattern element binds, and for each the member of the module it reads. */
    private boundByPatternElement(elem: J): { name: string; member?: string }[] {
        // Shorthand, so the name it binds is the member it reads.
        return elem.kind === J.Kind.Identifier
            ? [{name: (elem as J.Identifier).simpleName, member: (elem as J.Identifier).simpleName}]
            : bindingNames(elem);
    }

    private shouldRemoveImport(
        name: string,
        usedNames: Set<string>,
        member: string | undefined
    ): boolean {
        // If member is specified, we're removing a specific member from the module
        if (this.member !== undefined) {
            // A name bound under an alias reads one member and is referenced by another, and a name
            // a nested pattern binds reads a property of a property, so it reads no member at all.
            if (this.member !== member) {
                return false;
            }
        }
        // If no member specified, we're removing all unused imports from the module
        // So we check if this particular import is unused

        // Check if it's used
        return !usedNames.has(name);
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

}

/**
 * The names the file references, less its own imports: a specifier names what it binds, so counting
 * one would read an import as a use of itself.
 */
function collectUsedNames(cu: JS.CompilationUnit): Set<string> {
    const used = new Set<string>();
    for (const statement of cu.statements) {
        const element = statement.element;
        if (!element || element.kind === JS.Kind.Import) {
            continue;
        }
        namesReferencedWithin(element).forEach(name => used.add(name));
    }
    return used;
}

function isRequireCall(node: J): boolean {
    if (node.kind !== J.Kind.MethodInvocation) {
        return false;
    }
    const name = (node as J.MethodInvocation).name;
    return name?.kind === J.Kind.Identifier && (name as J.Identifier).simpleName === 'require';
}
