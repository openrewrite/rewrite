import {JavaScriptVisitor} from "./visitor";
import {J, emptySpace, rightPadded, space, Statement, singleSpace, Type} from "../java";
import {JS} from "./tree";
import {randomId} from "../uuid";
import {emptyMarkers, markers} from "../markers";
import {ExecutionContext} from "../execution";

export enum ImportStyle {
    ES6Named,      // import { x } from 'module'
    ES6Namespace,  // import * as x from 'module'
    ES6Default,    // import x from 'module'
    CommonJS       // const x = require('module')
}

export interface AddImportOptions {
    /** The module name (e.g., 'fs') to import from */
    target: string;

    /** Optionally, the specific member to import from the module.
     * If not specified, adds a default import or namespace import */
    member?: string;

    /** Optional alias for the imported member */
    alias?: string;

    /** If true, only add the import if the member is actually used in the file. Default: true */
    onlyIfReferenced?: boolean;

    /** Optional import style to use. If not specified, auto-detects from file and existing imports */
    style?: ImportStyle;
}

/**
 * Register an AddImport visitor to add an import statement to a JavaScript/TypeScript file
 * @param visitor The visitor to add the import addition to
 * @param options Configuration options for the import to add
 */
export function maybeAddImport(
    visitor: JavaScriptVisitor<any>,
    options: AddImportOptions
) {
    for (const v of visitor.afterVisit || []) {
        if (v instanceof AddImport &&
            v.target === options.target &&
            v.member === options.member &&
            v.alias === options.alias) {
            return;
        }
    }
    visitor.afterVisit.push(new AddImport(options));
}

export class AddImport<P> extends JavaScriptVisitor<P> {
    readonly target: string;
    readonly member?: string;
    readonly alias?: string;
    readonly onlyIfReferenced: boolean;
    readonly style?: ImportStyle;

    constructor(options: AddImportOptions) {
        super();
        this.target = options.target;
        this.member = options.member;
        this.alias = options.alias;
        this.onlyIfReferenced = options.onlyIfReferenced ?? true;
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
            // If we're importing a member, use named imports
            if (this.member !== undefined) {
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

        // If importing a member, prefer named imports if they exist in the file
        if (this.member !== undefined) {
            if (hasNamedImports) {
                return ImportStyle.ES6Named;
            }
            if (hasNamespaceImports) {
                return ImportStyle.ES6Namespace;
            }
        }

        // For default/whole module imports
        if (this.member === undefined) {
            if (hasNamespaceImports) {
                return ImportStyle.ES6Namespace;
            }
            if (hasDefaultImports) {
                return ImportStyle.ES6Default;
            }
        }

        // Default to named imports for members, default imports for modules
        return this.member !== undefined ? ImportStyle.ES6Named : ImportStyle.ES6Default;
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

                    if (moduleName === this.target) {
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
                        if (moduleName === this.target) {
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
        if (this.onlyIfReferenced) {
            const isReferenced = await this.checkIdentifierReferenced(compilationUnit);
            if (!isReferenced) {
                return compilationUnit;
            }
        }

        // Determine the appropriate import style
        const importStyle = this.determineImportStyle(compilationUnit);

        // For ES6 named imports, check if we can merge into an existing import from the same module
        if (importStyle === ImportStyle.ES6Named && this.member !== undefined) {
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
                if (moduleName !== this.target) {
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

                    // Add the new specifier to the elements
                    const updatedNamedImports: JS.NamedImports = await this.produceJavaScript<JS.NamedImports>(
                        namedImports, p, async namedDraft => {
                            namedDraft.elements = {
                                ...namedImports.elements,
                                elements: [
                                    ...namedImports.elements.elements,
                                    rightPadded(newSpecifier, emptySpace)
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
        if (moduleName !== this.target) {
            return false;
        }

        const importClause = jsImport.importClause;
        if (!importClause) {
            return false;
        }

        // Check if the specific member or default import already exists
        if (this.member === undefined) {
            // We're adding a default import, check if one exists
            return importClause.name !== undefined;
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
        if (moduleName !== this.target) {
            return false;
        }

        // Check if the variable name matches what we're trying to add
        const pattern = namedVar.name;
        if (this.member === undefined && pattern?.kind === J.Kind.Identifier) {
            // Default import style: const fs = require('fs')
            return true;
        } else if (this.member !== undefined && pattern?.kind === JS.Kind.ObjectBindingPattern) {
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
        // Use type attribution to detect if the identifier is referenced
        // Map of module name -> Set of member names used from that module
        const usedImports = new Map<string, Set<string>>();

        // Helper to record usage of a method from a module
        const recordMethodUsage = (methodType: Type.Method) => {
            const moduleName = Type.FullyQualified.getFullyQualifiedName(methodType.declaringType);
            if (moduleName) {
                if (!usedImports.has(moduleName)) {
                    usedImports.set(moduleName, new Set());
                }
                usedImports.get(moduleName)!.add(methodType.name);
            }
        };

        // Create a visitor to collect used identifiers with their type attribution
        const collector = new class extends JavaScriptVisitor<ExecutionContext> {
            override async visitIdentifier(identifier: J.Identifier, p: ExecutionContext): Promise<J | undefined> {
                const type = identifier.type;
                if (type && Type.isMethod(type)) {
                    recordMethodUsage(type as Type.Method);
                }
                return super.visitIdentifier(identifier, p);
            }

            override async visitMethodInvocation(methodInvocation: J.MethodInvocation, p: ExecutionContext): Promise<J | undefined> {
                if (methodInvocation.methodType) {
                    recordMethodUsage(methodInvocation.methodType);
                }
                return super.visitMethodInvocation(methodInvocation, p);
            }

            override async visitFunctionCall(functionCall: JS.FunctionCall, p: ExecutionContext): Promise<J | undefined> {
                if (functionCall.methodType) {
                    recordMethodUsage(functionCall.methodType);
                }
                return super.visitFunctionCall(functionCall, p);
            }

            override async visitFieldAccess(fieldAccess: J.FieldAccess, p: ExecutionContext): Promise<J | undefined> {
                const type = fieldAccess.type;
                if (type && Type.isMethod(type)) {
                    recordMethodUsage(type as Type.Method);
                }
                return super.visitFieldAccess(fieldAccess, p);
            }
        };

        await collector.visit(compilationUnit, new ExecutionContext());

        // Check if our target import is used based on type attribution
        const moduleMembers = usedImports.get(this.target);
        if (!moduleMembers) {
            return false;
        }

        // For specific members, check if that member is used; otherwise check if any member is used
        return this.member ? moduleMembers.has(this.member) : moduleMembers.size > 0;
    }

    /**
     * Create a new import statement
     */
    private async createImportStatement(compilationUnit: JS.CompilationUnit, insertionIndex: number, p: P): Promise<JS.Import> {
        // Determine the appropriate prefix (spacing before the import)
        const prefix = this.determineImportPrefix(compilationUnit, insertionIndex);

        // Create the module specifier
        const moduleSpecifier: J.Literal = {
            id: randomId(),
            kind: J.Kind.Literal,
            prefix: singleSpace,
            markers: emptyMarkers,
            value: `'${this.target}'`,
            valueSource: `'${this.target}'`,
            unicodeEscapes: [],
            type: undefined
        };

        let importClause: JS.ImportClause | undefined;

        if (this.member === undefined) {
            // Default import: import target from 'module'
            const defaultName: J.Identifier = {
                id: randomId(),
                kind: J.Kind.Identifier,
                prefix: singleSpace,
                markers: emptyMarkers,
                annotations: [],
                simpleName: this.alias || this.target,
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
