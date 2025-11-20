import {JavaScriptVisitor} from "./visitor";
import {emptySpace, J, rightPadded, singleSpace, space, Statement, Type} from "../java";
import {JS} from "./tree";
import {randomId} from "../uuid";
import {emptyMarkers, markers} from "../markers";

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
            v.sideEffectOnly === (options.sideEffectOnly ?? false)) {
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
        }

        this.module = options.module;
        this.member = options.member;
        this.alias = options.alias;
        this.onlyIfReferenced = options.onlyIfReferenced ?? true;
        this.sideEffectOnly = options.sideEffectOnly ?? false;
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
            const statement = stmt.element;

            // Check for ES6 imports
            if (statement?.kind === JS.Kind.Import) {
                const jsImport = statement as JS.Import;
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
            if (statement?.kind === J.Kind.VariableDeclarations) {
                const varDecl = statement as J.VariableDeclarations;
                if (varDecl.variables.length === 1) {
                    const namedVar = varDecl.variables[0].element;
                    const initializer = namedVar?.initializer?.element;
                    if (initializer?.kind === J.Kind.MethodInvocation &&
                        this.isRequireCall(initializer as J.MethodInvocation)) {
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
            const statement = stmt.element;

            // Check ES6 imports
            if (statement?.kind === JS.Kind.Import) {
                const jsImport = statement as JS.Import;
                const moduleSpecifier = jsImport.moduleSpecifier?.element;

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
            if (statement?.kind === J.Kind.VariableDeclarations) {
                const varDecl = statement as J.VariableDeclarations;
                if (varDecl.variables.length === 1) {
                    const namedVar = varDecl.variables[0].element;
                    const initializer = namedVar?.initializer?.element;

                    if (initializer?.kind === J.Kind.MethodInvocation &&
                        this.isRequireCall(initializer as J.MethodInvocation)) {
                        const moduleName = this.getModuleNameFromRequire(initializer as J.MethodInvocation);
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

        // For ES6 named imports, check if we can merge into an existing import from the same module
        // Don't try to merge default imports (member === 'default'), side-effect imports, or namespace imports (member === '*')
        if (!this.sideEffectOnly && importStyle === ImportStyle.ES6Named && this.member !== undefined && this.member !== 'default' && this.member !== '*') {
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
        return this.produceJavaScript<JS.CompilationUnit>(compilationUnit, p, async draft => {
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
                const updatedStatements = compilationUnit.statements.length > 0
                    ? [
                        rightPadded(newImport, emptySpace, semicolonMarkers),
                        {
                            ...compilationUnit.statements[0],
                            element: compilationUnit.statements[0].element
                                ? {...compilationUnit.statements[0].element, prefix: space("\n\n")}
                                : undefined
                        } as J.RightPadded<Statement>,
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
                if (after.length > 0 && after[0].element) {
                    const currentPrefix = after[0].element.prefix;
                    const needsNewline = !currentPrefix.whitespace.includes('\n');

                    const updatedNextStatement = needsNewline ? {
                        ...after[0],
                        element: {...after[0].element, prefix: space("\n" + currentPrefix.whitespace)}
                    } : after[0];

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
            const statement = stmt.element;

            if (statement?.kind === JS.Kind.Import) {
                const jsImport = statement as JS.Import;
                const moduleSpecifier = jsImport.moduleSpecifier?.element;

                if (!moduleSpecifier) {
                    continue;
                }

                const moduleName = this.getModuleName(moduleSpecifier);

                // Check if this is an import from our target module
                if (moduleName !== this.module) {
                    continue;
                }

                const importClause = jsImport.importClause;
                if (!importClause || !importClause.namedBindings) {
                    continue;
                }

                // Only merge into NamedImports, not namespace imports
                if (importClause.namedBindings.kind !== JS.Kind.NamedImports) {
                    continue;
                }

                // We found a matching import with named bindings - merge into it
                return this.produceJavaScript<JS.CompilationUnit>(compilationUnit, p, async draft => {
                    const namedImports = importClause.namedBindings as JS.NamedImports;

                    // Create the new specifier with a space prefix (since it's not the first element)
                    const newSpecifierBase = this.createImportSpecifier();
                    const newSpecifier = {...newSpecifierBase, prefix: singleSpace};

                    // Transfer the right padding from the element before the insertion point to the new element
                    // Since we're appending, this is the last existing element
                    const existingElements = namedImports.elements.elements;
                    const elementBeforeInsertion = existingElements[existingElements.length - 1];
                    const paddingToTransfer = elementBeforeInsertion.after;

                    // Add the new specifier to the elements
                    const updatedNamedImports: JS.NamedImports = await this.produceJavaScript<JS.NamedImports>(
                        namedImports, p, async namedDraft => {
                            // Update the element before insertion to have emptySpace as its right padding (before the comma)
                            const updatedExistingElements = existingElements.slice(0, -1).concat({
                                ...elementBeforeInsertion,
                                after: emptySpace
                            });

                            namedDraft.elements = {
                                ...namedImports.elements,
                                elements: [
                                    ...updatedExistingElements,
                                    // Transfer the padding to the new element (after the comma, before the closing brace)
                                    rightPadded(newSpecifier, paddingToTransfer)
                                ]
                            };
                        }
                    );

                    // Update the import with the new named imports
                    const updatedImport: JS.Import = await this.produceJavaScript<JS.Import>(
                        jsImport, p, async importDraft => {
                            importDraft.importClause = await this.produceJavaScript<JS.ImportClause>(
                                importClause, p, async clauseDraft => {
                                    clauseDraft.namedBindings = updatedNamedImports;
                                }
                            );
                        }
                    );

                    // Replace the statement in the compilation unit
                    draft.statements = compilationUnit.statements.map((s, idx) =>
                        idx === i ? {...s, element: updatedImport} : s
                    );
                });
            }
        }

        return compilationUnit;
    }

    /**
     * Check if the import already exists in the compilation unit
     */
    private async checkImportExists(compilationUnit: JS.CompilationUnit): Promise<boolean> {
        for (const stmt of compilationUnit.statements) {
            const statement = stmt.element;

            // Check ES6 imports
            if (statement?.kind === JS.Kind.Import) {
                const jsImport = statement as JS.Import;
                if (this.isMatchingImport(jsImport)) {
                    return true;
                }
            }

            // Check CommonJS require statements
            if (statement?.kind === J.Kind.VariableDeclarations) {
                const varDecl = statement as J.VariableDeclarations;
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
        const moduleSpecifier = jsImport.moduleSpecifier?.element;
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
            if (this.alias && importClause.name.element?.kind === J.Kind.Identifier) {
                const existingName = (importClause.name.element as J.Identifier).simpleName;
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
                    if (elem.element?.kind === JS.Kind.ImportSpecifier) {
                        const specifier = elem.element as JS.ImportSpecifier;
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

        const namedVar = varDecl.variables[0].element;
        if (!namedVar) {
            return false;
        }

        const initializer = namedVar.initializer?.element;
        if (!initializer || initializer.kind !== J.Kind.MethodInvocation) {
            return false;
        }

        const methodInv = initializer as J.MethodInvocation;
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
                if (elem.element?.kind === JS.Kind.BindingElement) {
                    const bindingElem = elem.element as JS.BindingElement;
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
            const statement = stmt.element;

            if (statement?.kind === JS.Kind.Import) {
                const jsImport = statement as JS.Import;
                const moduleSpecifier = jsImport.moduleSpecifier?.element;

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
                        const specifier = elem.element;
                        if (specifier?.kind === JS.Kind.ImportSpecifier) {
                            const importSpec = specifier as JS.ImportSpecifier;
                            let identifier: J.Identifier | undefined;
                            if (importSpec.specifier?.kind === J.Kind.Identifier) {
                                identifier = importSpec.specifier as J.Identifier;
                            } else if (importSpec.specifier?.kind === JS.Kind.Alias) {
                                const aliasSpec = importSpec.specifier as JS.Alias;
                                if (aliasSpec.alias?.kind === J.Kind.Identifier) {
                                    identifier = aliasSpec.alias as J.Identifier;
                                }
                            }

                            if (identifier?.type && Type.isMethod(identifier.type)) {
                                const methodType = identifier.type as Type.Method;
                                expectedDeclaringType = Type.FullyQualified.getFullyQualifiedName(methodType.declaringType);
                                if (expectedDeclaringType) {
                                    break;  // Found it!
                                }
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
        let found = false;

        // If no existing imports from this module, look for unresolved references
        // If there ARE existing imports, look for references with the expected declaring type

        const collector = new class extends JavaScriptVisitor<void> {
            override async visitIdentifier(identifier: J.Identifier, p: void): Promise<J | undefined> {
                if (identifier.simpleName === targetName) {
                    const type = identifier.type;
                    if (expectedDeclaringType) {
                        // We have an expected declaring type - check for exact match
                        if (type && Type.isMethod(type)) {
                            const methodType = type as Type.Method;
                            const declaringTypeName = Type.FullyQualified.getFullyQualifiedName(methodType.declaringType);
                            if (declaringTypeName === expectedDeclaringType) {
                                found = true;
                            }
                        }
                    } else {
                        // No existing imports - look for unresolved references (no type)
                        if (!type) {
                            found = true;
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
        const moduleSpecifier: J.Literal = {
            id: randomId(),
            kind: J.Kind.Literal,
            prefix: this.sideEffectOnly ? emptySpace : singleSpace,
            markers: emptyMarkers,
            value: `'${this.module}'`,
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
                prefix: emptySpace,
                markers: emptyMarkers,
                typeOnly: false,
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
                prefix: emptySpace,
                markers: emptyMarkers,
                typeOnly: false,
                name: rightPadded(defaultName, emptySpace),
                namedBindings: undefined
            };
        } else {
            // Named import: import { member } from 'module'
            const importSpec = this.createImportSpecifier();

            const namedImports: JS.NamedImports = {
                id: randomId(),
                kind: JS.Kind.NamedImports,
                prefix: singleSpace,
                markers: emptyMarkers,
                elements: {
                    kind: J.Kind.Container,
                    before: emptySpace,
                    elements: [rightPadded(importSpec, emptySpace)],
                    markers: emptyMarkers
                }
            };

            importClause = {
                id: randomId(),
                kind: JS.Kind.ImportClause,
                prefix: emptySpace,
                markers: emptyMarkers,
                typeOnly: false,
                name: undefined,
                namedBindings: namedImports
            };
        }

        const jsImport: JS.Import = {
            id: randomId(),
            kind: JS.Kind.Import,
            prefix,
            markers: emptyMarkers,
            modifiers: [],
            importClause,
            moduleSpecifier: {
                kind: J.Kind.LeftPadded,
                before: singleSpace,
                element: moduleSpecifier,
                markers: emptyMarkers
            },
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
                kind: J.Kind.LeftPadded,
                before: emptySpace,
                element: false,
                markers: emptyMarkers
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
        if (insertionIndex === 0 && compilationUnit.statements.length > 0) {
            const firstPrefix = compilationUnit.statements[0].element?.prefix;
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
            const statement = compilationUnit.statements[i].element;
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

        const firstArg = args[0].element;
        if (!firstArg || firstArg.kind !== J.Kind.Literal || typeof (firstArg as J.Literal).value !== 'string') {
            return undefined;
        }

        return (firstArg as J.Literal).value?.toString();
    }

    /**
     * Get the import name from an import specifier
     */
    private getImportName(specifier: JS.ImportSpecifier): string {
        const spec = specifier.specifier;
        if (spec?.kind === JS.Kind.Alias) {
            const alias = spec as JS.Alias;
            const propertyName = alias.propertyName.element;
            if (propertyName?.kind === J.Kind.Identifier) {
                return (propertyName as J.Identifier).simpleName;
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
