import {JavaScriptVisitor} from "./visitor";
import {isIdentifier, J, TextComment} from "../java";
import {bindingNames, namesReferencedAsTypesWithin, namesReferencedWithin, valuesDeclaredAtTopLevel, walk} from "./scope";
import {sameModule} from "./package-name";
import {isObjectBindingPattern, JS, JSX, moduleExportName} from "./tree";
import {bindingsOf, ModuleBinding, moduleNameOf, requiredModule} from "./binding-table";
import {bindsMember, memberName} from "./add-import";
import {mapAsync, updateIfChanged} from "../util";
import {ElementRemovalFormatter} from "../java";
import {randomId} from "../uuid";
import {JavaScriptParser} from "./parser";

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
        // transfer its trailing layout (space and markers, which carry TrailingComma)
        // to the new last element, so the closing brace keeps its position.
        if (filtered.length > 0 && originalLastElement?.element && !shouldKeep(originalLastElement.element)) {
            const lastIdx = filtered.length - 1;
            filtered[lastIdx] = {...filtered[lastIdx], after: originalTrailingSpace, markers: originalLastElement.markers};
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
        const memberScoped = this.member !== undefined && this.member !== '*' && this.member !== 'default';
        const usedNames = collectUsedNames(compilationUnit, memberScoped ? jsxFactory(compilationUnit) : undefined);
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
                    const result = await this.processRequireFromVarDecls(varDecl, usedNames.all, p);
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
                            const result = await this.processRequireFromVarDecls(varDecl as J.VariableDeclarations, usedNames.all, p);
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

            const filtered = newStatements.some(s => s === undefined)
                ? newStatements.filter(s => s !== undefined)
                : newStatements;

            const eof = await this.visitSpace(compilationUnit.eof, p);
            if (isTypeScriptSource(compilationUnit.sourcePath) && hasModuleSyntax(compilationUnit.statements) &&
                !hasModuleSyntax(filtered) && !hasJsx(compilationUnit)) {
                // The marker goes after the file's trailing comments, which belong to the code above them.
                const marker = await parseModuleMarker();
                const last = eof.comments[eof.comments.length - 1];
                const prefix = last
                    ? {...eof, comments: [...eof.comments.slice(0, -1), {...last, suffix: '\n\n'}]}
                    : {...marker.element.prefix, whitespace: filtered.length ? '\n\n' : ''};
                draft.statements = [...filtered, {...marker, element: {...marker.element, id: randomId(), prefix}}];
                draft.eof = last ? {...eof, whitespace: last.suffix, comments: []} : eof;
            } else {
                draft.statements = filtered;
                draft.eof = eof;
            }
        });
    }

    private async processImport(
        jsImport: JS.Import,
        used: UsedNames,
        p: P
    ): Promise<JS.Import | undefined> {
        // Handle import-equals-require syntax: import util = require("util");
        if (jsImport.initializer) {
            return this.processImportEqualsRequire(jsImport, used.all, p);
        }

        // Check if this import is from the target module
        if (!this.isTargetModule(jsImport)) {
            return jsImport;
        }

        let importClause = jsImport.importClause;
        if (!importClause) {
            return jsImport;
        }
        const usedNames = importClause.typeOnly ? used.types : used.all;

        // Process default import
        if (importClause.name) {
            const defaultName = importClause.name.element;
            if (defaultName && defaultName.kind === J.Kind.Identifier) {
                const identifier = defaultName as J.Identifier;
                const name = identifier.simpleName;

                // `'default'` or no member asks for it, whatever its local name
                const shouldRemove = this.asksFor(bindingsOf(jsImport).find(b => b.form === "default")) &&
                    !usedNames.has(name);

                if (shouldRemove) {
                    // If there are no named imports, remove the entire import
                    if (!importClause.namedBindings) {
                        return undefined;
                    }
                    // Otherwise, just remove the default import and fix spacing
                    jsImport = await this.updateImportClause(jsImport, importClause, async draft => {
                        draft.name = undefined;
                        // When removing the default import, we need to fix up spacing on the
                        // namedBindings. The space between "import" and the clause content is
                        // already in importClause.prefix, so we clear both namedBindings.prefix
                        // and elements.before (the space before "{") to avoid a double space.
                        if (draft.namedBindings) {
                            draft.namedBindings = await this.produceJava(
                                draft.namedBindings, p, async bindingsDraft => {
                                    bindingsDraft.prefix = identifier.prefix;
                                    if (bindingsDraft.elements) {
                                        bindingsDraft.elements = {
                                            ...bindingsDraft.elements,
                                            before: identifier.prefix
                                        };
                                    }
                                }
                            );
                        }
                    }, p);
                    importClause = jsImport.importClause!;
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

                // A member-scoped removal removes the whole namespace when it's unused
                if (this.member !== undefined) {
                    if (!usedNames.has(name)) {
                        if (!importClause.name) {
                            return undefined;
                        }
                        return this.updateImportClause(jsImport, importClause, draft => {
                            draft.namedBindings = undefined;
                        }, p);
                    }
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

                // Same as above: member-scoped removal removes unused namespace
                if (this.member !== undefined) {
                    if (!usedNames.has(aliasName)) {
                        if (!importClause.name) {
                            return undefined;
                        }
                        return this.updateImportClause(jsImport, importClause, draft => {
                            draft.namedBindings = undefined;
                        }, p);
                    }
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
                const updatedImports = await this.processNamedImports(namedImports, importClause.typeOnly, used, p);

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
        usedNames: ReadonlySet<string>,
        p: P
    ): Promise<JS.Import | undefined> {
        const moduleName = requiredModule(jsImport.initializer?.element);
        if (moduleName === undefined || !sameModule(moduleName, this.module)) {
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
     * Whether this removal asks for `binding`, as the binding table reads a request: every binding of the module
     * when no member is given, else the one {@link bindsMember} reads for the member. `'default'` asks for `D` of
     * `import D from "m"`, while `"D"` does not.
     */
    private asksFor(binding: ModuleBinding | undefined): boolean {
        return binding !== undefined && (this.member === undefined || bindsMember(binding, memberName(this.member)));
    }

    /**
     * Check if an identifier should be removed based on usage
     */
    private shouldRemoveIdentifier(name: string, usedNames: ReadonlySet<string>): boolean {
        // For CommonJS and import-equals-require, we're removing the entire import
        // if the identifier is not used (member is typically undefined for these cases,
        // or we're checking if a specific binding is used)
        return !usedNames.has(name);
    }

    private async processNamedImports(
        namedImports: JS.NamedImports,
        typeOnly: boolean,
        used: UsedNames,
        p: P
    ): Promise<JS.NamedImports | undefined> {
        const {filtered, allRemoved} = await this.filterElementsWithPrefixPreservation(
            namedImports.elements.elements,
            (elem: J) => {
                if (elem.kind === JS.Kind.ImportSpecifier) {
                    const specifier = elem as JS.ImportSpecifier;
                    const usedNames = typeOnly || specifier.importType.element ? used.types : used.all;
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
                        return !this.shouldRemoveImport(nameToCheck, usedNames, importName);
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
        usedNames: ReadonlySet<string>,
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

        // As the binding table reads one: `container.require("m")` calls a method rather than loading a module
        const moduleName = requiredModule(namedVar.initializer?.element);
        const pattern = namedVar.name;
        if (moduleName === undefined || !sameModule(moduleName, this.module) || !pattern) {
            return varDecls;
        }

        // Handle: const fs = require('fs')
        if (isIdentifier(pattern) && !usedNames.has(pattern.simpleName)) {
            return undefined; // Remove the entire require statement
        }

        // Handle: const { readFile } = require('fs')
        if (isObjectBindingPattern(pattern)) {
            const updatedPattern = await this.processObjectBindingPattern(pattern, usedNames, p);

            if (updatedPattern === undefined) {
                return undefined; // Remove entire require
            } else if (updatedPattern !== pattern) {
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

    private async processObjectBindingPattern(
        pattern: JS.ObjectBindingPattern,
        usedNames: ReadonlySet<string>,
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
            return moduleExportName((spec as JS.Alias).propertyName.element);
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
        usedNames: ReadonlySet<string>,
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

        // As the binding table reads it, so `"./author's-notes"` keeps its quote
        return sameModule(moduleNameOf(moduleSpecifier as J.Literal), this.module);
    }

}

/** The names the file references, and of those, the ones a type-only import answers for. */
interface UsedNames {
    all: ReadonlySet<string>;
    types: ReadonlySet<string>;
}

/**
 * The names the file references, less its own imports: a specifier names what it binds, so counting
 * one would read an import as a use of itself. A value read reaches a type-only import, as `typeof X`
 * does, only where the file declares no value of that name itself. The JSX factory is read as a value.
 */
function collectUsedNames(cu: JS.CompilationUnit, jsxFactory: string | undefined): UsedNames {
    const all = new Set<string>();
    const types = new Set<string>();
    for (const statement of cu.statements) {
        const element = statement.element;
        if (!element || element.kind === JS.Kind.Import) {
            continue;
        }
        namesReferencedWithin(element).forEach(name => all.add(name));
        namesReferencedAsTypesWithin(element).forEach(name => types.add(name));
    }
    const values = valuesDeclaredAtTopLevel(cu);
    all.forEach(name => !values.has(name) && types.add(name));
    if (jsxFactory) {
        all.add(jsxFactory);
    }
    return {all, types};
}

/**
 * The name classic JSX reads its factory through, `React` for `React.createElement`, unless a leading
 * `@jsx` pragma names another. No reference in the file spells it.
 */
function jsxFactory(cu: JS.CompilationUnit): string | undefined {
    if (!hasJsx(cu)) {
        return undefined;
    }
    const first = cu.statements.find(s => s.element.kind !== JS.Kind.Shebang)?.element;
    for (const comment of [...cu.prefix.comments, ...(first?.prefix.comments ?? [])]) {
        const pragma = comment.kind === J.Kind.TextComment && /@jsx\s+([\w$]+)/.exec((comment as TextComment).text);
        if (pragma) {
            return pragma[1];
        }
    }
    return 'React';
}

function hasJsx(cu: JS.CompilationUnit): boolean {
    let found = false;
    walk(cu.statements, node => {
        found ||= node.kind === JS.Kind.JsxTag;
        return !found;
    });
    return found;
}

function hasModuleSyntax(statements: J.RightPadded<J>[]): boolean {
    return statements.some(({element: s}) =>
        s.kind === JS.Kind.Import || s.kind === JS.Kind.ExportDeclaration || s.kind === JS.Kind.ExportAssignment ||
        ((s as {modifiers?: J.Modifier[]}).modifiers ?? []).some(m => m.keyword === 'export'));
}

// `.mts` and `.cts` files are modules whatever they hold.
const TS_EXT = /\.tsx?$/;
function isTypeScriptSource(sourcePath: string): boolean {
    return TS_EXT.test(sourcePath);
}

let moduleMarkerCache: J.RightPadded<J> | undefined;
async function parseModuleMarker(): Promise<J.RightPadded<J>> {
    if (moduleMarkerCache) return moduleMarkerCache;
    const parser = new JavaScriptParser();
    for await (const sf of parser.parse({text: 'export {};', sourcePath: 'module-marker.ts'})) {
        moduleMarkerCache = (sf as JS.CompilationUnit).statements[0];
    }
    return moduleMarkerCache!;
}

