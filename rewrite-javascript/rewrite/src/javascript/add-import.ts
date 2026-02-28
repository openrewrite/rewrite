import {JavaScriptVisitor} from "./visitor";
import {emptySpace, J, rightPadded, singleSpace, space, Statement, Type} from "../java";
import {JS} from "./tree";
import {randomId} from "../uuid";
import {emptyMarkers, markers} from "../markers";
import {getStyle, SpacesStyle, StyleKind} from "./style";

export enum ImportStyle {
    ES6Named,      // import { x } from 'module'
    ES6Namespace,  // import * as x from 'module'
    ES6Default,    // import x from 'module'
    CommonJS       // const x = require('module')
}

export interface AddImportOptions {
    /** The module name (e.g., 'fs', 'react') to import from */
    module: string;

    /** Optionally, the specific member to import from the module.
     * If not specified, adds a default import or namespace import.
     * Special values:
     * - 'default': Adds a default import from the module.
     *   When using 'default', the `alias` parameter is required.
     * - '*': Adds a namespace import (import * as alias from 'module').
     *   When using '*', the `alias` parameter is required.
     * Cannot be combined with `sideEffectOnly`. */
    member?: string;

    /** Optional alias for the imported member.
     * Required when member is 'default' or '*'.
     * Cannot be combined with `sideEffectOnly`. */
    alias?: string;

    /** If true, only add the import if the member is actually used in the file. Default: true
     * Cannot be combined with `sideEffectOnly`. */
    onlyIfReferenced?: boolean;

    /** If true, adds a side-effect import without bindings (e.g., `import 'module'` or `require('module')`).
     * Cannot be combined with `member`, `alias`, or `onlyIfReferenced`. */
    sideEffectOnly?: boolean;

    /** If true, adds a type-only import (e.g., `import type { Foo } from 'module'`).
     * Type-only imports are erased at compile time and do not generate runtime code.
     * Cannot be combined with `sideEffectOnly`. */
    typeOnly?: boolean;

    /** Optional import style to use. If not specified, auto-detects from file and existing imports */
    style?: ImportStyle;
}

/**
 * Register an AddImport visitor to add an import statement to a JavaScript/TypeScript file
 * @param visitor The visitor to add the import addition to
 * @param options Configuration options for the import to add
 *
 * @example
 * // Add a named import
 * maybeAddImport(visitor, { module: 'fs', member: 'readFile' });
 *
 * @example
 * // Add a default import using the 'default' member specifier
 * maybeAddImport(visitor, { module: 'react', member: 'default', alias: 'React' });
 *
 * @example
 * // Add a default import (legacy way, without specifying member)
 * maybeAddImport(visitor, { module: 'react', alias: 'React' });
 *
 * @example
 * // Add a namespace import
 * maybeAddImport(visitor, { module: 'crypto', member: '*', alias: 'crypto' });
 *
 * @example
 * // Add a side-effect import
 * maybeAddImport(visitor, { module: 'core-js/stable', sideEffectOnly: true });
 */
export function maybeAddImport(
    visitor: JavaScriptVisitor<any>,
    options: AddImportOptions
) {
    for (const v of visitor.afterVisit || []) {
        if (v instanceof AddImport &&
            v.module === options.module &&
            v.member === options.member &&
            v.alias === options.alias &&
            v.sideEffectOnly === (options.sideEffectOnly ?? false) &&
            v.typeOnly === (options.typeOnly ?? false)) {
            return;
        }
    }
    visitor.afterVisit.push(new AddImport(options));
}

export class AddImport<P> extends JavaScriptVisitor<P> {
    readonly module: string;
    readonly member?: string;
    readonly alias?: string;
    readonly onlyIfReferenced: boolean;
    readonly sideEffectOnly: boolean;
    readonly typeOnly: boolean;
    readonly style?: ImportStyle;

    constructor(options: AddImportOptions) {
        super();

        // Validate that alias is provided when member is 'default'
        if (options.member === 'default' && !options.alias) {
            throw new Error("When member is 'default', the alias parameter is required");
        }

        // Validate that alias is provided when member is '*' (namespace import)
        if (options.member === '*' && !options.alias) {
            throw new Error("When member is '*', the alias parameter is required");
        }

        // Validate that sideEffectOnly is not combined with incompatible options
        if (options.sideEffectOnly) {
            if (options.member !== undefined) {
                throw new Error("Cannot combine sideEffectOnly with member");
            }
            if (options.alias !== undefined) {
                throw new Error("Cannot combine sideEffectOnly with alias");
            }
            if (options.onlyIfReferenced !== undefined) {
                throw new Error("Cannot combine sideEffectOnly with onlyIfReferenced");
            }
            if (options.typeOnly) {
                throw new Error("Cannot combine sideEffectOnly with typeOnly");
            }
        }

        this.module = options.module;
        this.member = options.member;
        this.alias = options.alias;
        this.onlyIfReferenced = options.onlyIfReferenced ?? true;
        this.sideEffectOnly = options.sideEffectOnly ?? false;
        this.typeOnly = options.typeOnly ?? false;
        this.style = options.style;
    }

    /**
     * Extract module name from a module specifier literal
     */
    private getModuleName(moduleSpecifier: J): string | undefined {
        if (moduleSpecifier.kind !== J.Kind.Literal) {
            return undefined;
        }
        return (moduleSpecifier as J.Literal).value?.toString();
    }

    /**
     * Check if a method invocation is a require() call
     */
    private isRequireCall(methodInv: J.MethodInvocation): boolean {
        return methodInv.name?.kind === J.Kind.Identifier &&
               (methodInv.name as J.Identifier).simpleName === 'require';
    }

    /**
     * Determine the appropriate import style based on file type and existing imports
     */
    private determineImportStyle(compilationUnit: JS.CompilationUnit): ImportStyle {
        // If style was explicitly provided, use it
        if (this.style !== undefined) {
            return this.style;
        }

        // Check the file extension from sourcePath
        const sourcePath = compilationUnit.sourcePath;
        const isTypeScript = sourcePath.endsWith('.ts') ||
                            sourcePath.endsWith('.tsx') ||
                            sourcePath.endsWith('.mts') ||
                            sourcePath.endsWith('.cts');

        // Check for .cjs extension - must use CommonJS
        if (sourcePath.endsWith('.cjs')) {
            return ImportStyle.CommonJS;
        }

        // First, check if there's already an import from the same module
        // and match that style
        const existingStyleForModule = this.findExistingImportStyleForModule(compilationUnit);
        if (existingStyleForModule !== null) {
            return existingStyleForModule;
        }

        // For .mjs or TypeScript, prefer ES6
        if (sourcePath.endsWith('.mjs') || isTypeScript) {
            // If we're importing a member (but not 'default'), use named imports
            if (this.member !== undefined && this.member !== 'default') {
                return ImportStyle.ES6Named;
            }
            // Otherwise default import
            return ImportStyle.ES6Default;
        }

        // For .js files, check what style is predominantly being used
        let hasNamedImports = false;
        let hasNamespaceImports = false;
        let hasDefaultImports = false;
        let hasCommonJSRequires = false;

        for (const stmt of compilationUnit.statements) {
            // Check for ES6 imports
            if (stmt?.kind === JS.Kind.Import) {
                const jsImport = stmt as JS.Import & J.RightPaddingMixin;
                const importClause = jsImport.importClause;

                if (importClause) {
                    // Check for named bindings
                    if (importClause.namedBindings) {
                        if (importClause.namedBindings.kind === JS.Kind.NamedImports) {
                            hasNamedImports = true;
                        } else if (importClause.namedBindings.kind === J.Kind.Identifier ||
                                   importClause.namedBindings.kind === JS.Kind.Alias) {
                            // import * as x from 'module'
                            hasNamespaceImports = true;
                        }
                    }

                    // Check for default import
                    if (importClause.name) {
                        hasDefaultImports = true;
                    }
                }
            }

            // Check for CommonJS requires
            if (stmt?.kind === J.Kind.VariableDeclarations) {
                const varDecl = stmt as J.VariableDeclarations & J.RightPaddingMixin;
                if (varDecl.variables.length === 1) {
                    // With intersection types, the variable IS the NamedVariable with padding mixed in
                    const namedVar = varDecl.variables[0];
                    // initializer IS the expression with padding mixed in
                    const initializer = namedVar?.initializer;
                    if (initializer?.kind === J.Kind.MethodInvocation &&
                        this.isRequireCall(initializer as J.MethodInvocation & J.LeftPaddingMixin)) {
                        hasCommonJSRequires = true;
                    }
                }
            }
        }

        // Prefer matching the predominant style
        // If file uses CommonJS, stick with it
        if (hasCommonJSRequires && !hasNamedImports && !hasNamespaceImports && !hasDefaultImports) {
            return ImportStyle.CommonJS;
        }

        // If importing a member (but not 'default'), prefer named imports if they exist in the file
        if (this.member !== undefined && this.member !== 'default') {
            if (hasNamedImports) {
                return ImportStyle.ES6Named;
            }
            if (hasNamespaceImports) {
                return ImportStyle.ES6Namespace;
            }
        }

        // For default/whole module imports
        if (this.member === undefined || this.member === 'default') {
            if (hasNamespaceImports) {
                return ImportStyle.ES6Namespace;
            }
            if (hasDefaultImports) {
                return ImportStyle.ES6Default;
            }
        }

        // Default to named imports for members (except 'default'), default imports for modules
        return (this.member !== undefined && this.member !== 'default')
            ? ImportStyle.ES6Named
            : ImportStyle.ES6Default;
    }

    /**
     * Find the import style used for an existing import from the same module
     */
    private findExistingImportStyleForModule(compilationUnit: JS.CompilationUnit): ImportStyle | null {
        for (const stmt of compilationUnit.statements) {
            // Check ES6 imports
            if (stmt?.kind === JS.Kind.Import) {
                const jsImport = stmt as JS.Import & J.RightPaddingMixin;
                // moduleSpecifier IS the literal with padding mixed in
                const moduleSpecifier = jsImport.moduleSpecifier;

                if (moduleSpecifier) {
                    const moduleName = this.getModuleName(moduleSpecifier);

                    if (moduleName === this.module) {
                        const importClause = jsImport.importClause;
                        if (importClause?.namedBindings) {
                            if (importClause.namedBindings.kind === JS.Kind.NamedImports) {
                                return ImportStyle.ES6Named;
                            } else {
                                return ImportStyle.ES6Namespace;
                            }
                        }
                        if (importClause?.name) {
                            return ImportStyle.ES6Default;
                        }
                    }
                }
            }

            // Check CommonJS requires
            if (stmt?.kind === J.Kind.VariableDeclarations) {
                const varDecl = stmt as J.VariableDeclarations & J.RightPaddingMixin;
                if (varDecl.variables.length === 1) {
                    // With intersection types, the variable IS the NamedVariable with padding mixed in
                    const namedVar = varDecl.variables[0];
                    // initializer IS the expression with padding mixed in
                    const initializer = namedVar?.initializer;

                    if (initializer?.kind === J.Kind.MethodInvocation &&
                        this.isRequireCall(initializer as J.MethodInvocation & J.LeftPaddingMixin)) {
                        const moduleName = this.getModuleNameFromRequire(initializer as J.MethodInvocation & J.LeftPaddingMixin);
                        if (moduleName === this.module) {
                            return ImportStyle.CommonJS;
                        }
                    }
                }
            }
        }

        return null;
    }

    override async visitJsCompilationUnit(compilationUnit: JS.CompilationUnit, p: P): Promise<J | undefined> {
        // First, check if the import already exists
        const hasImport = await this.checkImportExists(compilationUnit);
        if (hasImport) {
            return compilationUnit;
        }

        // If onlyIfReferenced is true, check if the identifier is actually used
        // Skip this check for side-effect imports
        if (!this.sideEffectOnly && this.onlyIfReferenced) {
            const isReferenced = await this.checkIdentifierReferenced(compilationUnit);
            if (!isReferenced) {
                return compilationUnit;
            }
        }

        // Determine the appropriate import style
        const importStyle = this.determineImportStyle(compilationUnit);

        // For named imports, check if we can merge into an existing import from the same module
        // This handles both:
        // - Case 1: Existing import has named bindings - merge into them
        // - Case 2: Default import without named bindings - add named bindings
        // Don't try to merge default imports (member === 'default'), side-effect imports, or namespace imports (member === '*')
        if (!this.sideEffectOnly && this.member !== undefined && this.member !== 'default' && this.member !== '*') {
            const mergedCu = await this.tryMergeIntoExistingImport(compilationUnit, p);
            if (mergedCu !== compilationUnit) {
                return mergedCu;
            }
        }

        // Add the import using the appropriate style
        if (importStyle === ImportStyle.CommonJS) {
            // TODO: Implement CommonJS require creation
            // For now, fall back to ES6 imports
            // return this.addCommonJSRequire(compilationUnit, p);
        }

        // Add ES6 import (handles ES6Named, ES6Namespace, ES6Default)
        return this.produceJavaScript(compilationUnit, p, async draft => {
            // Find the position to insert the import
            const insertionIndex = this.findImportInsertionIndex(compilationUnit);

            const newImport = await this.createImportStatement(compilationUnit, insertionIndex, p);

            // Insert the import at the appropriate position
            // Create semicolon marker for the import statement
            const semicolonMarkers = markers({
                kind: J.Markers.Semicolon,
                id: randomId()
            });

            if (insertionIndex === 0) {
                // Insert at the beginning
                // The `after` space should be empty since semicolon is printed after it
                // The spacing comes from updating the next statement's prefix
                // With intersection types, we need to create the updated statement properly
                const firstStmt = compilationUnit.statements[0];
                const updatedStatements = compilationUnit.statements.length > 0
                    ? [
                        rightPadded(newImport, emptySpace, semicolonMarkers),
                        firstStmt
                            ? {...firstStmt, prefix: space("\n\n")} as J.RightPadded<Statement>
                            : firstStmt,
                        ...compilationUnit.statements.slice(1)
                    ]
                    : [rightPadded(newImport, emptySpace, semicolonMarkers)];

                draft.statements = updatedStatements;
            } else {
                // Insert after existing imports
                const before = compilationUnit.statements.slice(0, insertionIndex);
                const after = compilationUnit.statements.slice(insertionIndex);

                //The `after` space is empty, spacing comes from next statement's prefix
                // Ensure the next statement has at least one newline in its prefix
                // With intersection types, after[0] IS the statement with padding mixed in
                if (after.length > 0 && after[0]) {
                    const currentPrefix = after[0].prefix;
                    const needsNewline = !currentPrefix.whitespace.includes('\n');

                    const updatedNextStatement = needsNewline ? {
                        ...after[0],
                        prefix: space("\n" + currentPrefix.whitespace)
                    } as J.RightPadded<Statement> : after[0];

                    draft.statements = [
                        ...before,
                        rightPadded(newImport, emptySpace, semicolonMarkers),
                        updatedNextStatement,
                        ...after.slice(1)
                    ];
                } else {
                    draft.statements = [
                        ...before,
                        rightPadded(newImport, emptySpace, semicolonMarkers),
                        ...after
                    ];
                }
            }
        });
    }

    /**
     * Try to merge the new member into an existing import from the same module
     */
    private async tryMergeIntoExistingImport(compilationUnit: JS.CompilationUnit, p: P): Promise<JS.CompilationUnit> {
        for (let i = 0; i < compilationUnit.statements.length; i++) {
            const stmt = compilationUnit.statements[i];

            if (stmt?.kind === JS.Kind.Import) {
                const jsImport = stmt as JS.Import & J.RightPaddingMixin;
                // moduleSpecifier IS the literal with padding mixed in
                const moduleSpecifier = jsImport.moduleSpecifier;

                if (!moduleSpecifier) {
                    continue;
                }

                const moduleName = this.getModuleName(moduleSpecifier);

                // Check if this is an import from our target module
                if (moduleName !== this.module) {
                    continue;
                }

                const importClause = jsImport.importClause;
                if (!importClause) {
                    continue;
                }

                // Only merge into imports with matching typeOnly - don't mix type and value imports
                if (importClause.typeOnly !== this.typeOnly) {
                    continue;
                }

                // Case 1: Existing import has named bindings - merge into them
                if (importClause.namedBindings) {
                    // Only merge into NamedImports, not namespace imports
                    if (importClause.namedBindings.kind !== JS.Kind.NamedImports) {
                        continue;
                    }

                    // We found a matching import with named bindings - merge into it
                    return this.produceJavaScript(compilationUnit, p, async draft => {
                        const namedImports = importClause.namedBindings as JS.NamedImports;
                        const existingElements = namedImports.elements.elements;

                        // Find the correct insertion position (alphabetical, case-insensitive)
                        const newName = (this.alias || this.member!).toLowerCase();
                        let insertIndex = existingElements.findIndex(elem => {
                            // With intersection types, elem IS the ImportSpecifier with padding mixed in
                            if (elem?.kind === JS.Kind.ImportSpecifier) {
                                const name = this.getImportAlias(elem as JS.ImportSpecifier & J.RightPaddingMixin) || this.getImportName(elem as JS.ImportSpecifier & J.RightPaddingMixin);
                                return newName.localeCompare(name.toLowerCase()) < 0;
                            }
                            return false;
                        });
                        if (insertIndex === -1) insertIndex = existingElements.length;

                        // Detect spacing style from existing elements:
                        // - firstElementPrefix: space after { (from first element's prefix)
                        // - trailingSpace: space before } (from last element's padding.after)
                        const firstElementPrefix = existingElements[0]?.prefix ?? emptySpace;
                        const lastIndex = existingElements.length - 1;
                        const trailingSpace = existingElements[lastIndex].padding.after;

                        // Build the new elements array with proper spacing
                        const updatedNamedImports: JS.NamedImports = await this.produceJavaScript(
                            namedImports, p, async namedDraft => {
                                const newSpecifier = this.createImportSpecifier();

                                const newElements = existingElements.flatMap((elem, j) => {
                                    const results: J.RightPadded<JS.ImportSpecifier>[] = [];
                                    if (j === insertIndex) {
                                        // Insert new element here
                                        // First element gets the same prefix as the original first element
                                        // Other positions get a single space (separator after comma)
                                        const prefix = j === 0 ? firstElementPrefix : singleSpace;
                                        results.push(rightPadded({...newSpecifier, prefix}, emptySpace));
                                    }
                                    // Adjust existing element: if inserting before first, give it space prefix
                                    // For tree types, elem IS the element with padding mixed in (no .element property)
                                    let adjusted = elem;
                                    if (j === 0 && insertIndex === 0) {
                                        adjusted = {...elem, prefix: singleSpace} as J.RightPadded<JS.ImportSpecifier>;
                                    }
                                    // Last element before a new trailing element loses its trailing space
                                    if (j === lastIndex && insertIndex > lastIndex) {
                                        adjusted = {...adjusted, padding: {...adjusted.padding, after: emptySpace}};
                                    }
                                    results.push(adjusted);
                                    return results;
                                });

                                // Append at end if inserting after all existing elements
                                if (insertIndex > lastIndex) {
                                    newElements.push(rightPadded({...newSpecifier, prefix: singleSpace}, trailingSpace));
                                }

                                namedDraft.elements = {...namedImports.elements, elements: newElements};
                            }
                        );

                        // Update the import with the new named imports
                        const updatedImport: JS.Import = await this.produceJavaScript(
                            jsImport, p, async importDraft => {
                                importDraft.importClause = await this.produceJavaScript(
                                    importClause, p, async clauseDraft => {
                                        clauseDraft.namedBindings = updatedNamedImports;
                                    }
                                );
                            }
                        );

                        // Replace the statement in the compilation unit
                        // With intersection types, we need to preserve padding when updating
                        draft.statements = compilationUnit.statements.map((s, idx) =>
                            idx === i ? {...updatedImport, padding: s.padding} as J.RightPadded<Statement> : s
                        );
                    });
                }

                // Case 2: Default import without named bindings - add named bindings
                // Transform: import React from 'react' -> import React, { useState } from 'react'
                if (importClause.name && !importClause.namedBindings) {
                    return this.produceJavaScript(compilationUnit, p, async draft => {
                        const newSpecifier = this.createImportSpecifier();

                        // Get the spaces style for brace spacing
                        const spacesStyle = getStyle(StyleKind.SpacesStyle, compilationUnit) as SpacesStyle;
                        const braceSpace = spacesStyle.within.es6ImportExportBraces ? singleSpace : emptySpace;

                        // Create new NamedImports with a single element
                        // Apply brace spacing: space after { is in specifier's prefix, space before } is in after
                        const namedImports: JS.NamedImports = {
                            id: randomId(),
                            kind: JS.Kind.NamedImports,
                            prefix: singleSpace,
                            markers: emptyMarkers,
                            elements: {
                                kind: J.Kind.Container,
                                before: emptySpace,
                                elements: [rightPadded({...newSpecifier, prefix: braceSpace}, braceSpace)],
                                markers: emptyMarkers
                            }
                        };

                        // Update the import clause to include named bindings
                        // Also update name.padding.after to emptySpace since the comma goes right after the name
                        const updatedImport: JS.Import = await this.produceJavaScript(
                            jsImport, p, async importDraft => {
                                importDraft.importClause = await this.produceJavaScript(
                                    importClause, p, async clauseDraft => {
                                        // Remove space after default name (comma goes right after)
                                        if (clauseDraft.name) {
                                            clauseDraft.name = {...clauseDraft.name, padding: {...clauseDraft.name.padding, after: emptySpace}};
                                        }
                                        clauseDraft.namedBindings = namedImports;
                                    }
                                );
                                // Ensure moduleSpecifier has proper space before 'from'
                                if (importDraft.moduleSpecifier) {
                                    importDraft.moduleSpecifier = {
                                        ...importDraft.moduleSpecifier,
                                        padding: {
                                            ...importDraft.moduleSpecifier.padding,
                                            before: singleSpace
                                        }
                                    };
                                }
                            }
                        );

                        // Replace the statement in the compilation unit
                        // With intersection types, we need to preserve padding when updating
                        draft.statements = compilationUnit.statements.map((s, idx) =>
                            idx === i ? {...updatedImport, padding: s.padding} as J.RightPadded<Statement> : s
                        );
                    });
                }
            }
        }

        return compilationUnit;
    }

    /**
     * Check if the import already exists in the compilation unit
     */
    private async checkImportExists(compilationUnit: JS.CompilationUnit): Promise<boolean> {
        for (const stmt of compilationUnit.statements) {
            // Check ES6 imports
            if (stmt?.kind === JS.Kind.Import) {
                const jsImport = stmt as JS.Import & J.RightPaddingMixin;
                if (this.isMatchingImport(jsImport)) {
                    return true;
                }
            }

            // Check CommonJS require statements
            if (stmt?.kind === J.Kind.VariableDeclarations) {
                const varDecl = stmt as J.VariableDeclarations & J.RightPaddingMixin;
                if (this.isMatchingRequire(varDecl)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Check if the import matches what we're trying to add
     */
    private isMatchingImport(jsImport: JS.Import): boolean {
        // Check module specifier
        // With intersection types, moduleSpecifier IS the literal with padding mixed in
        const moduleSpecifier = jsImport.moduleSpecifier;
        if (!moduleSpecifier) {
            return false;
        }

        const moduleName = this.getModuleName(moduleSpecifier);
        if (moduleName !== this.module) {
            return false;
        }

        const importClause = jsImport.importClause;

        // Handle side-effect imports (no import clause)
        if (!importClause) {
            // If we're trying to add a side-effect import and one already exists, it's a match
            return this.sideEffectOnly;
        }

        // If we're adding a side-effect import but there's an existing import with bindings,
        // it's not a match (side-effect import should be separate)
        if (this.sideEffectOnly) {
            return false;
        }

        // Check if the typeOnly flag matches - type-only and value imports are separate
        if (importClause.typeOnly !== this.typeOnly) {
            return false;
        }

        // Check if the specific member or default import already exists
        if (this.member === '*') {
            // We're adding a namespace import, check if one exists
            const namedBindings = importClause.namedBindings;
            if (!namedBindings) {
                return false;
            }

            // Namespace imports can be represented as J.Identifier or JS.Alias
            if (namedBindings.kind === J.Kind.Identifier) {
                const identifier = namedBindings as J.Identifier;
                return identifier.simpleName === this.alias;
            } else if (namedBindings.kind === JS.Kind.Alias) {
                const alias = namedBindings as JS.Alias;
                if (alias.alias?.kind === J.Kind.Identifier) {
                    return (alias.alias as J.Identifier).simpleName === this.alias;
                }
            }
            return false;
        } else if (this.member === undefined || this.member === 'default') {
            // We're adding a default import, check if one exists
            // For member === 'default', also verify the alias matches if specified
            if (importClause.name === undefined) {
                return false;
            }
            // If we have an alias, check that it matches
            // With intersection types, name IS the Identifier with padding mixed in
            if (this.alias && importClause.name?.kind === J.Kind.Identifier) {
                const existingName = (importClause.name as J.Identifier & J.RightPaddingMixin).simpleName;
                return existingName === this.alias;
            }
            return true;
        } else {
            // We're adding a named import, check if it exists
            const namedBindings = importClause.namedBindings;
            if (!namedBindings) {
                return false;
            }

            if (namedBindings.kind === JS.Kind.NamedImports) {
                const namedImports = namedBindings as JS.NamedImports;
                for (const elem of namedImports.elements.elements) {
                    // With intersection types, elem IS the ImportSpecifier with padding mixed in
                    if (elem?.kind === JS.Kind.ImportSpecifier) {
                        const specifier = elem as JS.ImportSpecifier & J.RightPaddingMixin;
                        const importName = this.getImportName(specifier);
                        const aliasName = this.getImportAlias(specifier);

                        if (importName === this.member && aliasName === this.alias) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    /**
     * Check if this is a matching CommonJS require statement
     */
    private isMatchingRequire(varDecl: J.VariableDeclarations): boolean {
        if (varDecl.variables.length !== 1) {
            return false;
        }

        // With intersection types, the variable IS the NamedVariable with padding mixed in
        const namedVar = varDecl.variables[0];
        if (!namedVar) {
            return false;
        }

        // initializer IS the expression with padding mixed in
        const initializer = namedVar.initializer;
        if (!initializer || initializer.kind !== J.Kind.MethodInvocation) {
            return false;
        }

        // Cast through unknown for intersection type to specific type
        const methodInv = initializer as unknown as J.MethodInvocation;
        if (!this.isRequireCall(methodInv)) {
            return false;
        }

        const moduleName = this.getModuleNameFromRequire(methodInv);
        if (moduleName !== this.module) {
            return false;
        }

        // Check if the variable name matches what we're trying to add
        const pattern = namedVar.name;
        if ((this.member === undefined || this.member === 'default') && pattern?.kind === J.Kind.Identifier) {
            // Default import style: const fs = require('fs')
            // For member === 'default', also check the alias matches if specified
            if (this.alias) {
                const varName = (pattern as J.Identifier).simpleName;
                return varName === this.alias;
            }
            return true;
        } else if (this.member !== undefined && this.member !== 'default' && pattern?.kind === JS.Kind.ObjectBindingPattern) {
            // Destructured import: const { member } = require('module')
            const objectPattern = pattern as JS.ObjectBindingPattern;
            for (const elem of objectPattern.bindings.elements) {
                // With intersection types, elem IS the BindingElement with padding mixed in
                if (elem?.kind === JS.Kind.BindingElement) {
                    const bindingElem = elem as JS.BindingElement & J.RightPaddingMixin;
                    const name = (bindingElem.name as J.Identifier)?.simpleName;
                    if (name === (this.alias || this.member)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Extract the module name from a class type by traversing the owningClass chain
     * or extracting it from the FQN.
     */
    private getModuleFromClassType(classType: Type.Class): string | undefined {
        // Traverse owningClass chain to find the root
        let current: Type.Class = classType;
        while (current.owningClass && Type.isClass(current.owningClass)) {
            current = current.owningClass as Type.Class;
        }
        // If there's still an owningClass (non-Class type), use it
        if (current.owningClass) {
            return Type.FullyQualified.getFullyQualifiedName(current.owningClass);
        }
        // For top-level classes, extract module from FQN (e.g., "zod.ZodError" -> "zod")
        const fqn = current.fullyQualifiedName;
        const dotIndex = fqn.lastIndexOf('.');
        if (dotIndex > 0) {
            return fqn.substring(0, dotIndex);
        }
        // The FQN itself might be the module (e.g., "zod" for z from zod)
        return fqn;
    }

    /**
     * Extract the module name from a type (method, class, or variable).
     */
    private getModuleFromType(type: Type | undefined, fieldType: Type | undefined): string | undefined {
        if (type && Type.isMethod(type)) {
            return Type.FullyQualified.getFullyQualifiedName((type as Type.Method).declaringType);
        }
        if (type && Type.isClass(type)) {
            return this.getModuleFromClassType(type as Type.Class);
        }
        if (fieldType?.kind === Type.Kind.Variable) {
            const variableType = fieldType as Type.Variable;
            if (variableType.owner) {
                return Type.FullyQualified.getFullyQualifiedName(variableType.owner);
            }
        }
        return undefined;
    }

    /**
     * Check if a class type matches the expected module.
     * Handles direct FQN match, owningClass chain match, and FQN prefix match.
     */
    private classTypeMatchesModule(classType: Type.Class, expectedModule: string): boolean {
        const fqn = classType.fullyQualifiedName;
        // Direct match: class FQN equals the expected module (e.g., z from zod where z's type FQN is "zod")
        if (fqn === expectedModule) {
            return true;
        }
        // Check via owningClass chain or FQN prefix
        const moduleFromType = this.getModuleFromClassType(classType);
        return moduleFromType === expectedModule;
    }

    /**
     * Check if the identifier is actually referenced in the file
     */
    private async checkIdentifierReferenced(compilationUnit: JS.CompilationUnit): Promise<boolean> {
        // For namespace imports, we cannot use type attribution to detect usage
        // because the namespace itself is used as an identifier, not individual members.
        // For simplicity, we skip the onlyIfReferenced check for namespace imports.
        if (this.member === '*') {
            // TODO: Implement proper namespace usage detection by checking if alias identifier is used
            return true;
        }

        // Step 1: Find the expected declaring type by examining existing imports from the same module
        let expectedDeclaringType: string | undefined;

        for (const stmt of compilationUnit.statements) {
            if (stmt?.kind === JS.Kind.Import) {
                const jsImport = stmt as JS.Import & J.RightPaddingMixin;
                // moduleSpecifier IS the literal with padding mixed in
                const moduleSpecifier = jsImport.moduleSpecifier;

                if (!moduleSpecifier) {
                    continue;
                }

                const moduleName = this.getModuleName(moduleSpecifier);
                if (moduleName !== this.module) {
                    continue;  // Not the module we're interested in
                }

                // Found an existing import from our target module
                // Extract the declaring type from any imported member with type attribution
                const importClause = jsImport.importClause;
                if (importClause?.namedBindings?.kind === JS.Kind.NamedImports) {
                    const namedImports = importClause.namedBindings as JS.NamedImports;
                    for (const elem of namedImports.elements.elements) {
                        if (elem?.kind === JS.Kind.ImportSpecifier) {
                            const importSpec = elem as JS.ImportSpecifier & J.RightPaddingMixin;
                            let identifier: J.Identifier | undefined;
                            if (importSpec.specifier?.kind === J.Kind.Identifier) {
                                identifier = importSpec.specifier as J.Identifier;
                            } else if (importSpec.specifier?.kind === JS.Kind.Alias) {
                                const aliasSpec = importSpec.specifier as JS.Alias;
                                if (aliasSpec.alias?.kind === J.Kind.Identifier) {
                                    identifier = aliasSpec.alias as J.Identifier;
                                }
                            }

                            expectedDeclaringType = this.getModuleFromType(identifier?.type, identifier?.fieldType);
                            if (expectedDeclaringType) {
                                break;  // Found it!
                            }
                        }
                    }
                }

                if (expectedDeclaringType) {
                    break;  // No need to scan more imports
                }
            }
        }

        // Step 2: Look for references that match
        const targetName = this.alias || this.member;
        const targetModule = this.module;
        let found = false;
        const self = this;

        // If no existing imports from this module, look for unresolved references
        // If there ARE existing imports, look for references with the expected declaring type

        const collector = new class extends JavaScriptVisitor<void> {
            override async visitIdentifier(identifier: J.Identifier, p: void): Promise<J | undefined> {
                if (identifier.simpleName === targetName) {
                    const type = identifier.type;
                    const fieldType = identifier.fieldType;
                    if (expectedDeclaringType) {
                        // We have an expected declaring type - check for exact match
                        if (type && Type.isMethod(type)) {
                            const declaringTypeName = Type.FullyQualified.getFullyQualifiedName((type as Type.Method).declaringType);
                            if (declaringTypeName === expectedDeclaringType) {
                                found = true;
                            }
                        }
                        else if (type && Type.isClass(type)) {
                            if (self.classTypeMatchesModule(type as Type.Class, expectedDeclaringType)) {
                                found = true;
                            }
                        }
                        else if (fieldType?.kind === Type.Kind.Variable) {
                            const ownerTypeName = (fieldType as Type.Variable).owner
                                ? Type.FullyQualified.getFullyQualifiedName((fieldType as Type.Variable).owner!)
                                : undefined;
                            if (ownerTypeName === expectedDeclaringType) {
                                found = true;
                            }
                        }
                        // Also check for unresolved references (member isn't imported yet)
                        else if (!type && !fieldType) {
                            found = true;
                        }
                    } else {
                        // No existing imports from this module - look for references that match
                        // 1. Unresolved references (no type/unknown type and no fieldType)
                        const isUnknownType = !type || type.kind === Type.Kind.Unknown;
                        if (isUnknownType && !fieldType) {
                            found = true;
                        }
                        // 2. References with fieldType matching the target module
                        else if (fieldType?.kind === Type.Kind.Variable) {
                            const variableType = fieldType as Type.Variable;
                            if (variableType.owner && Type.isClass(variableType.owner)) {
                                // Traverse owningClass chain to find the root module (handles nested namespaces)
                                // For example: React.forwardRef -> owner is "React" namespace -> owningClass is "react" module
                                let current: Type.Class = variableType.owner as Type.Class;

                                // Walk up the owningClass chain until we reach the root
                                while (current.owningClass && Type.isClass(current.owningClass)) {
                                    current = current.owningClass as Type.Class;
                                }

                                const moduleName = Type.FullyQualified.getFullyQualifiedName(current);
                                if (moduleName === targetModule) {
                                    found = true;
                                }
                            }
                        }
                        // 3. References with method type matching the target module
                        else if (type && Type.isMethod(type)) {
                            const methodType = type as Type.Method;
                            const declaringTypeName = Type.FullyQualified.getFullyQualifiedName(methodType.declaringType);
                            if (declaringTypeName === targetModule) {
                                found = true;
                            }
                        }
                    }
                }
                return super.visitIdentifier(identifier, p);
            }

            override async visitMethodInvocation(methodInvocation: J.MethodInvocation, p: void): Promise<J | undefined> {
                if (methodInvocation.methodType && methodInvocation.methodType.name === targetName) {
                    if (expectedDeclaringType) {
                        const declaringTypeName = Type.FullyQualified.getFullyQualifiedName(methodInvocation.methodType.declaringType);
                        if (declaringTypeName === expectedDeclaringType) {
                            found = true;
                        }
                    }
                }
                return super.visitMethodInvocation(methodInvocation, p);
            }

            override async visitFunctionCall(functionCall: JS.FunctionCall, p: void): Promise<J | undefined> {
                if (functionCall.methodType && functionCall.methodType.name === targetName) {
                    if (expectedDeclaringType) {
                        const declaringTypeName = Type.FullyQualified.getFullyQualifiedName(functionCall.methodType.declaringType);
                        if (declaringTypeName === expectedDeclaringType) {
                            found = true;
                        }
                    }
                }
                return super.visitFunctionCall(functionCall, p);
            }

            override async visitFieldAccess(fieldAccess: J.FieldAccess, p: void): Promise<J | undefined> {
                const type = fieldAccess.type;
                if (type && Type.isMethod(type)) {
                    const methodType = type as Type.Method;
                    if (methodType.name === targetName) {
                        if (expectedDeclaringType) {
                            const declaringTypeName = Type.FullyQualified.getFullyQualifiedName(methodType.declaringType);
                            if (declaringTypeName === expectedDeclaringType) {
                                found = true;
                            }
                        }
                    }
                }
                return super.visitFieldAccess(fieldAccess, p);
            }
        };

        await collector.visit(compilationUnit, undefined);

        return found;
    }

    /**
     * Create a new import statement
     */
    private async createImportStatement(compilationUnit: JS.CompilationUnit, insertionIndex: number, p: P): Promise<JS.Import> {
        // Determine the appropriate prefix (spacing before the import)
        const prefix = this.determineImportPrefix(compilationUnit, insertionIndex);

        // Create the module specifier
        // For side-effect imports, use emptySpace since space comes from LeftPadded.before
        // For regular imports with import clause, use emptySpace since space comes from LeftPadded.before
        // However, the printer expects the space after 'from' in the literal's prefix
        // Note: value contains the unquoted string, valueSource contains the quoted version for printing
        const moduleSpecifier: J.Literal = {
            id: randomId(),
            kind: J.Kind.Literal,
            prefix: this.sideEffectOnly ? emptySpace : singleSpace,
            markers: emptyMarkers,
            value: this.module,
            valueSource: `'${this.module}'`,
            unicodeEscapes: [],
            type: undefined
        };

        let importClause: JS.ImportClause | undefined;

        if (this.sideEffectOnly) {
            // Side-effect import: import 'module'
            importClause = undefined;
        } else if (this.member === '*') {
            // Namespace import: import * as alias from 'module'
            const propertyName: J.Identifier = {
                id: randomId(),
                kind: J.Kind.Identifier,
                prefix: emptySpace,
                markers: emptyMarkers,
                annotations: [],
                simpleName: '*',
                type: undefined,
                fieldType: undefined
            };

            const aliasIdentifier: J.Identifier = {
                id: randomId(),
                kind: J.Kind.Identifier,
                prefix: singleSpace,
                markers: emptyMarkers,
                annotations: [],
                simpleName: this.alias!,
                type: undefined,
                fieldType: undefined
            };

            const namespaceBinding: JS.Alias = {
                id: randomId(),
                kind: JS.Kind.Alias,
                prefix: singleSpace,
                markers: emptyMarkers,
                propertyName: rightPadded(propertyName, singleSpace),
                alias: aliasIdentifier
            };

            importClause = {
                id: randomId(),
                kind: JS.Kind.ImportClause,
                prefix: this.typeOnly ? singleSpace : emptySpace,
                markers: emptyMarkers,
                typeOnly: this.typeOnly,
                name: undefined,
                namedBindings: namespaceBinding
            };
        } else if (this.member === undefined || this.member === 'default') {
            // Default import: import target from 'module'
            // or: import alias from 'module' (when member === 'default')
            const defaultName: J.Identifier = {
                id: randomId(),
                kind: J.Kind.Identifier,
                prefix: singleSpace,
                markers: emptyMarkers,
                annotations: [],
                simpleName: this.alias || this.module,
                type: undefined,
                fieldType: undefined
            };

            importClause = {
                id: randomId(),
                kind: JS.Kind.ImportClause,
                prefix: this.typeOnly ? singleSpace : emptySpace,
                markers: emptyMarkers,
                typeOnly: this.typeOnly,
                name: rightPadded(defaultName, emptySpace),
                namedBindings: undefined
            };
        } else {
            // Named import: import { member } from 'module'

            // Get the spaces style for brace spacing
            const spacesStyle = getStyle(StyleKind.SpacesStyle, compilationUnit) as SpacesStyle;
            const braceSpace = spacesStyle.within.es6ImportExportBraces ? singleSpace : emptySpace;

            const importSpec = this.createImportSpecifier();
            // Apply brace spacing: the space after { is in the specifier's prefix,
            // and the space before } is in the rightPadded's after
            const importSpecWithSpacing = {...importSpec, prefix: braceSpace};

            const namedImports: JS.NamedImports = {
                id: randomId(),
                kind: JS.Kind.NamedImports,
                prefix: singleSpace,
                markers: emptyMarkers,
                elements: {
                    kind: J.Kind.Container,
                    before: emptySpace,
                    elements: [rightPadded(importSpecWithSpacing, braceSpace)],
                    markers: emptyMarkers
                }
            };

            importClause = {
                id: randomId(),
                kind: JS.Kind.ImportClause,
                prefix: this.typeOnly ? singleSpace : emptySpace,
                markers: emptyMarkers,
                typeOnly: this.typeOnly,
                name: undefined,
                namedBindings: namedImports
            };
        }

        // With intersection types for LeftPadded tree types, we spread the element and add padding
        const jsImport: JS.Import = {
            id: randomId(),
            kind: JS.Kind.Import,
            prefix,
            markers: emptyMarkers,
            modifiers: [],
            importClause,
            moduleSpecifier: {
                ...moduleSpecifier,
                padding: {
                    before: singleSpace,
                    markers: emptyMarkers
                }
            } as J.LeftPadded<J.Literal>,
            initializer: undefined
        };

        return jsImport;
    }

    /**
     * Create an import specifier for a named import
     */
    private createImportSpecifier(): JS.ImportSpecifier {
        let specifier: J.Identifier | JS.Alias;

        if (this.alias) {
            // Aliased import: import { member as alias } from 'module'
            const propertyName: J.Identifier = {
                id: randomId(),
                kind: J.Kind.Identifier,
                prefix: emptySpace,
                markers: emptyMarkers,
                annotations: [],
                simpleName: this.member!,
                type: undefined,
                fieldType: undefined
            };

            const aliasName: J.Identifier = {
                id: randomId(),
                kind: J.Kind.Identifier,
                prefix: singleSpace,
                markers: emptyMarkers,
                annotations: [],
                simpleName: this.alias,
                type: undefined,
                fieldType: undefined
            };

            specifier = {
                id: randomId(),
                kind: JS.Kind.Alias,
                prefix: emptySpace,
                markers: emptyMarkers,
                propertyName: rightPadded(propertyName, singleSpace),
                alias: aliasName
            };
        } else {
            // Regular import: import { member } from 'module'
            specifier = {
                id: randomId(),
                kind: J.Kind.Identifier,
                prefix: emptySpace,
                markers: emptyMarkers,
                annotations: [],
                simpleName: this.member!,
                type: undefined,
                fieldType: undefined
            };
        }

        return {
            id: randomId(),
            kind: JS.Kind.ImportSpecifier,
            prefix: emptySpace,
            markers: emptyMarkers,
            importType: {
                element: false,
                padding: {
                    before: emptySpace,
                    markers: emptyMarkers
                }
            },
            specifier
        };
    }

    /**
     * Determine the appropriate spacing before the import statement
     */
    private determineImportPrefix(compilationUnit: JS.CompilationUnit, insertionIndex: number): J.Space {
        // If inserting at the beginning (index 0), use the prefix of the first statement
        // but only the whitespace part (preserve comments on the original first statement)
        // With intersection types, statements[0] IS the statement with padding mixed in
        if (insertionIndex === 0 && compilationUnit.statements.length > 0) {
            const firstPrefix = compilationUnit.statements[0]?.prefix;
            if (firstPrefix) {
                // Keep only whitespace, not comments
                return {
                    kind: J.Kind.Space,
                    comments: [],
                    whitespace: firstPrefix.whitespace
                };
            }
            return emptySpace;
        }

        // If inserting after other statements, ensure we have at least one newline
        // to separate from the previous statement
        return space("\n");
    }

    /**
     * Find the index where the new import should be inserted
     */
    private findImportInsertionIndex(compilationUnit: JS.CompilationUnit): number {
        let lastImportIndex = -1;

        for (let i = 0; i < compilationUnit.statements.length; i++) {
            // With intersection types, the statement IS the statement with padding mixed in
            const statement = compilationUnit.statements[i];
            if (statement?.kind === JS.Kind.Import) {
                lastImportIndex = i;
            } else if (lastImportIndex >= 0) {
                // We've found a non-import after imports, insert after the last import
                return lastImportIndex + 1;
            }
        }

        // If we found imports, insert after them
        if (lastImportIndex >= 0) {
            return lastImportIndex + 1;
        }

        // No imports found, insert at the beginning
        return 0;
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

    /**
     * Get the import name from an import specifier
     */
    private getImportName(specifier: JS.ImportSpecifier): string {
        const spec = specifier.specifier;
        if (spec?.kind === JS.Kind.Alias) {
            const alias = spec as JS.Alias;
            // With intersection types, propertyName IS the Identifier with padding mixed in
            const propertyName = alias.propertyName;
            if (propertyName?.kind === J.Kind.Identifier) {
                return (propertyName as J.Identifier & J.RightPaddingMixin).simpleName;
            }
        } else if (spec?.kind === J.Kind.Identifier) {
            return (spec as J.Identifier).simpleName;
        }
        return '';
    }

    /**
     * Get the import alias from an import specifier
     */
    private getImportAlias(specifier: JS.ImportSpecifier): string | undefined {
        const spec = specifier.specifier;
        if (spec?.kind === JS.Kind.Alias) {
            const alias = spec as JS.Alias;
            if (alias.alias?.kind === J.Kind.Identifier) {
                return (alias.alias as J.Identifier).simpleName;
            }
        }
        return undefined;
    }
}
