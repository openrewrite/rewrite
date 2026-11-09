/*
 * Copyright 2026 the original author or authors.
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
import ts from "typescript";
import * as path from "path";
import {randomId} from "../uuid";
import {JS} from "./tree";
import {ResolvedNames} from "./markers";
import {JavaScriptTypeMapping} from "./type-mapping";
import {moduleName} from "./package-name";

/** A lone surrogate prints as a six character `\uXXXX` escape in place of its one code unit. */
const ESCAPED_SURROGATE_GROWTH = 5;

type DeclarationKind = "class" | "interface" | "enum" | "enum-member" | "method" | "constructor" | "field" | "module";

/** A declaration as every reference to it names it. */
interface Target {
    readonly name: string;
    /** What the names of its members start from, which a module's file is not. */
    readonly owner: string;
    readonly pkg: string | null;
    readonly kind: DeclarationKind;
}

/** What a file's declarations are named from. */
interface Place {
    /** The file's module, from the root of its repository or its package. */
    readonly module: string;
    /** The file, from the same root, which is what names the module. */
    readonly file: string;
    /** The npm package holding the file, null for the repository and for the default library. */
    readonly pkg: string | null;
    /** Whether the file was indexed apart from the repository, so that no declaration row of ours stands in it. */
    readonly external: boolean;
    /** The parser's own naming, which a package's files and a declaration file go by. */
    readonly mapping?: JavaScriptTypeMapping;
}

/** Each row of a file, with the names and the kinds they share. */
class Rows {
    readonly names: string[] = [];
    readonly packages: (string | null)[] = [];
    readonly kinds: string[] = [];
    readonly spans: number[] = [];
    private readonly nameIndices = new Map<string, number>();
    private readonly kindIndices = new Map<string, number>();
    private readonly printed: (offset: number) => number;

    constructor(private readonly sourceFile: ts.SourceFile) {
        this.printed = printedOffsets(sourceFile.text);
    }

    add(node: ts.Node, name: string, pkg: string | null, kind: string): void {
        const key = `${pkg}\0${name}`;
        let nameIndex = this.nameIndices.get(key);
        if (nameIndex === undefined) {
            this.nameIndices.set(key, nameIndex = this.names.length);
            this.names.push(name);
            this.packages.push(pkg);
        }
        let kindIndex = this.kindIndices.get(kind);
        if (kindIndex === undefined) {
            this.kindIndices.set(kind, kindIndex = this.kinds.length);
            this.kinds.push(kind);
        }
        this.spans.push(this.printed(node.getStart(this.sourceFile)), this.printed(node.getEnd()), nameIndex, kindIndex);
    }
}

/** Gives a reference and its declaration one canonical name, from what the checker resolves each name to. */
export class JavaScriptNameResolver {
    private readonly checker: ts.TypeChecker;
    private readonly roots: string[];
    private readonly targets = new Map<ts.Symbol, Target | undefined>();
    private readonly places = new Map<string, Place | undefined>();
    private readonly mappings = new Map<string, JavaScriptTypeMapping>();
    private readonly packageNames = new Map<string, string | undefined>();

    /**
     * @param resolveModule resolves a specifier as the program did, for one it did not load
     * @param repositoryRoot what a path in the repository is relative to
     */
    constructor(private readonly program: ts.Program,
                private readonly resolveModule: (specifier: ts.StringLiteralLike) => string | undefined,
                repositoryRoot: string) {
        this.checker = program.getTypeChecker();
        // Module resolution reports a symlinked path by what it links to.
        this.roots = [...new Set([repositoryRoot, ts.sys.realpath?.(repositoryRoot) ?? repositoryRoot])];
    }

    resolvedNames(sourceFile: ts.SourceFile): ResolvedNames {
        const rows = new Rows(sourceFile);
        const visit = (node: ts.Node): void => {
            this.rowsAt(node, rows);
            ts.forEachChild(node, visit);
        };
        visit(sourceFile);
        const {names, packages, kinds, spans} = rows;
        return {kind: JS.Markers.ResolvedNames, id: randomId(), names, packages, kinds, spans};
    }

    private rowsAt(node: ts.Node, rows: Rows): void {
        try {
            if (ts.isIdentifier(node) || ts.isPrivateIdentifier(node)) {
                this.nameRows(node, rows);
            } else if (ts.isStringLiteralLike(node) && isModuleSpecifier(node)) {
                this.add(rows, node, this.specifiedModule(node), "import-ref");
            } else if (ts.isStringLiteralLike(node) || ts.isNumericLiteral(node)) {
                this.literalNameRow(node, rows);
            } else if (ts.isConstructorDeclaration(node)) {
                this.keywordRow(node, ts.SyntaxKind.ConstructorKeyword, rows);
            } else if (isNamelessDefaultExport(node)) {
                this.keywordRow(node, ts.SyntaxKind.DefaultKeyword, rows);
            }
        } catch {
            // The checker asserts on some code it cannot place, and one name must not cost the file its LST.
        }
    }

    private nameRows(name: ts.Identifier | ts.PrivateIdentifier, rows: Rows): void {
        const parent = name.parent;
        if (this.isOwnModule(name)) {
            const place = this.placeOf(name.getSourceFile().fileName);
            this.add(rows, name, place && moduleTarget(place), undefined);
            return;
        }
        if (ts.isShorthandPropertyAssignment(parent) && parent.name === name) {
            this.shorthandRows(name, parent, rows);
            return;
        }
        if (ts.isParameter(parent) && parent.name === name && ts.isParameterPropertyDeclaration(parent, parent.parent)) {
            const [, property] = this.checker.getSymbolsOfParameterPropertyDeclaration(parent, name.text);
            this.declarationRow(name, property, rows);
            return;
        }
        const symbol = this.checker.getSymbolAtLocation(name);
        if (isBoundProperty(name)) {
            const element = name.parent as ts.BindingElement;
            if (!(symbol && symbol.flags & ts.SymbolFlags.Alias)) {
                const read = propertyOf(this.checker.getTypeAtLocation(element.parent), name.text);
                if (read) {
                    this.referenceRow(name, read, rows, moduleSpecifierOf(element) ? "import-ref" : undefined);
                }
            }
            if (element.propertyName === name) {
                return;
            }
        }
        if (!symbol) {
            return;
        }
        if (isBinding(name, symbol)) {
            const target = this.targetOf(symbol);
            if (target && this.isUnbound(symbol, target)) {
                rows.add(name, target.name, target.pkg, target.kind);
            } else {
                this.add(rows, name, target, "import-ref");
            }
            return;
        }
        // The binding it is assigned to declares it. A reference here would be a call on the declaration's own line.
        const assigned = (ts.isFunctionExpression(parent) || ts.isClassExpression(parent)) && parent.name === name &&
            !isExportedValue(parent) ? this.assignedTarget(parent) : undefined;
        if (assigned) {
            return;
        }
        const expected = this.contextualProperty(name);
        const fills = expected !== undefined && !expected.declarations?.includes(parent as ts.Declaration);
        if (fills) {
            this.referenceRow(name, expected, rows);
        }
        if (ts.isJsxAttribute(parent)) {
            return;
        }
        if (declares(symbol, name)) {
            this.declarationRow(name, symbol, rows);
        } else if (!fills) {
            this.referenceRow(name, symbol, rows);
        }
    }

    /** How a CommonJS file names itself, which the checker answers with whatever is being assigned. */
    private isOwnModule(name: ts.Identifier | ts.PrivateIdentifier): boolean {
        if (name.text !== "module" && name.text !== "exports") {
            return false;
        }
        const file = name.getSourceFile() as ts.SourceFile & { commonJsModuleIndicator?: ts.Node, symbol?: ts.Symbol };
        if (!file.commonJsModuleIndicator || ts.getNameOfDeclaration(name.parent as ts.Declaration) === name) {
            return false;
        }
        const bound = this.checker.resolveName(name.text, name, ts.SymbolFlags.Value, true);
        return bound === file.symbol || bound !== undefined && (bound.flags & ts.SymbolFlags.ModuleExports) !== 0;
    }

    /** `{a}` reads the value `a` and declares a property of the same name, in that order, as a click on it does. */
    private shorthandRows(name: ts.Identifier | ts.PrivateIdentifier, element: ts.ShorthandPropertyAssignment, rows: Rows): void {
        const value = this.checker.getShorthandAssignmentValueSymbol(element);
        if (value) {
            this.referenceRow(name, value, rows);
        }
        const property = this.checker.getSymbolAtLocation(name);
        if (property && !(property.flags & ts.SymbolFlags.Alias) && this.declared(property) !== (value && this.targetOf(value))) {
            this.declarationRow(name, property, rows);
        }
    }

    /** A declaration named by a string or a number, which a reference by name still reaches. */
    private literalNameRow(literal: ts.StringLiteralLike | ts.NumericLiteral, rows: Rows): void {
        const declaration = literal.parent as ts.Declaration;
        if (ts.getNameOfDeclaration(declaration) === literal) {
            const symbol = this.symbolOfDeclaration(declaration);
            if (symbol) {
                this.declarationRow(literal, symbol, rows);
            }
        }
    }

    /** A declaration with no name of its own stands on its keyword, as `constructor` and `export default` do. */
    private keywordRow(declaration: ts.Node, keyword: ts.SyntaxKind, rows: Rows): void {
        const token = declaration.getChildren().flatMap(child => child.kind === ts.SyntaxKind.SyntaxList ? child.getChildren() : [child])
            .find(child => child.kind === keyword);
        const symbol = this.symbolOfDeclaration(declaration);
        if (token && symbol) {
            this.declarationRow(token, symbol, rows);
        }
    }

    private declarationRow(node: ts.Node, symbol: ts.Symbol, rows: Rows): void {
        const target = this.declared(symbol);
        const declaration = symbol.declarations?.find(d => nameOf(d) === node) ?? symbol.declarations?.[0];
        if (target && declaration && target === this.valueTarget(declaration)) {
            this.add(rows, node, target, undefined);
        } else if (target && declaration) {
            rows.add(node, target.name, target.pkg, this.kindOf(symbol, declaration));
        }
    }

    private referenceRow(node: ts.Node, symbol: ts.Symbol, rows: Rows, kind?: string): void {
        this.add(rows, node, this.targetOf(symbol), kind);
    }

    /** A row for a name that refers to `target`, in the kind given, else the one its target asks for. */
    private add(rows: Rows, node: ts.Node, target: Target | undefined, kind: string | undefined): void {
        if (!target) {
            return;
        }
        if (kind) {
            rows.add(node, target.name, target.pkg, kind);
        } else if (isConstructed(node) && (target.kind === "class" || target.kind === "interface")) {
            rows.add(node, `${target.name}.<init>`, target.pkg, "constructor-call");
        } else {
            rows.add(node, target.name, target.pkg, referenceKind(target.kind));
        }
    }

    /** The property an object literal's key fills in the type the literal is expected to have. */
    private contextualProperty(name: ts.Identifier | ts.PrivateIdentifier): ts.Symbol | undefined {
        const element = name.parent;
        const inLiteral = ts.isObjectLiteralElementLike(element) && ts.isObjectLiteralExpression(element.parent) ||
            ts.isJsxAttribute(element);
        if (!inLiteral || element.name !== name) {
            return undefined;
        }
        const expected = this.checker.getContextualType(element.parent as ts.Expression);
        return expected && propertyOf(expected, name.text);
    }

    private targetOf(symbol: ts.Symbol): Target | undefined {
        if (!(symbol.flags & ts.SymbolFlags.Alias)) {
            return this.declared(symbol);
        }
        const aliased = this.checker.getAliasedSymbol(symbol);
        if (aliased.declarations?.length) {
            return this.declared(aliased);
        }
        const binding = this.moduleBinding(symbol, new Set());
        return binding && (this.resolvedModule(binding.specifier) ?? this.unbound(binding.declaration));
    }

    /** The declaration that binds a symbol to a module, through whatever assignments and exports stand between them. */
    private moduleBinding(symbol: ts.Symbol, seen: Set<ts.Symbol>): { declaration: ts.Declaration, specifier: ts.StringLiteralLike } | undefined {
        if (seen.has(symbol)) {
            return undefined;
        }
        seen.add(symbol);
        for (const declaration of symbol.declarations ?? []) {
            const specifier = moduleSpecifierOf(declaration);
            if (specifier) {
                return ts.isStringLiteralLike(specifier) ? {declaration, specifier} : undefined;
            }
            const source = this.assignedSymbol(declaration);
            const bound = source && this.moduleBinding(source, seen);
            if (bound) {
                return bound;
            }
        }
        return undefined;
    }

    /** A binding to a module that does not resolve, which is all there is of what it binds. A re-export binds nothing here. */
    private unbound(binding: ts.Declaration): Target | undefined {
        const symbol = this.symbolOfDeclaration(binding);
        return symbol && !ts.isExportSpecifier(binding) && !ts.isNamespaceExport(binding) ? this.declared(symbol) : undefined;
    }

    /** Whether a binding is the target of its own uses, which only {@link unbound} makes an alias. */
    private isUnbound(binding: ts.Symbol, target: Target): boolean {
        return (binding.flags & ts.SymbolFlags.Alias) !== 0 && this.targets.get(binding) === target;
    }

    private assignedSymbol(declaration: ts.Node): ts.Symbol | undefined {
        if (ts.isExportSpecifier(declaration)) {
            return this.checker.getExportSpecifierLocalTargetSymbol(declaration);
        }
        if (ts.isShorthandPropertyAssignment(declaration)) {
            return this.checker.getShorthandAssignmentValueSymbol(declaration);
        }
        const parent = declaration.parent as ts.Node | undefined;
        const assigned = ts.isExportAssignment(declaration) ? declaration.expression :
            ts.isBinaryExpression(declaration) ? declaration.right :
                parent && ts.isBinaryExpression(parent) && parent.left === declaration ? parent.right :
                    undefined;
        return assigned && this.checker.getSymbolAtLocation(leftmost(assigned));
    }

    private specifiedModule(specifier: ts.StringLiteralLike): Target | undefined {
        const symbol = this.checker.getSymbolAtLocation(specifier);
        const declaration = symbol?.declarations?.[0];
        if (!declaration || isAugmentation(declaration)) {
            return this.resolvedModule(specifier);
        }
        if (ts.isModuleDeclaration(declaration)) {
            return this.declared(symbol!);
        }
        const place = this.placeOf(declaration.getSourceFile().fileName);
        return place && moduleTarget(place);
    }

    private resolvedModule(specifier: ts.StringLiteralLike): Target | undefined {
        const file = this.resolveModule(specifier);
        const place = file === undefined ? undefined : this.placeOf(file);
        return place && moduleTarget(place);
    }

    private symbolOfDeclaration(declaration: ts.Node): ts.Symbol | undefined {
        return (declaration as { symbol?: ts.Symbol }).symbol;
    }

    private declared(symbol: ts.Symbol): Target | undefined {
        const declaration = primaryDeclaration(symbol);
        if (!declaration || ts.isIndexSignatureDeclaration(declaration)) {
            return undefined;
        }
        // A transient symbol, as a union's property is, has no owner, where the symbol it stands for does.
        const own = symbol.flags & ts.SymbolFlags.Transient ? this.symbolOfDeclaration(declaration) ?? symbol : symbol;
        if (!this.targets.has(own)) {
            this.targets.set(own, undefined);
            this.targets.set(own, this.isLocal(own, declaration) ? undefined : this.target(own, declaration));
        }
        return this.targets.get(own);
    }

    private target(symbol: ts.Symbol, declaration: ts.Declaration): Target | undefined {
        const place = this.placeOf(declaration.getSourceFile().fileName);
        if (!place) {
            return undefined;
        }
        const name = this.nameIn(place, symbol, declaration);
        if (isNamedByFile(declaration, place)) {
            return {...moduleTarget(place), owner: name};
        }
        const value = this.valueTarget(declaration);
        return value?.name === name ? value : {name, owner: name, pkg: place.pkg, kind: this.kindOf(symbol, declaration)};
    }

    /** What a property holds by name, which is the one declaration when it restates it, as `module.exports = {f}` does `f`. */
    private valueTarget(declaration: ts.Declaration): Target | undefined {
        const value = ts.isShorthandPropertyAssignment(declaration) ? this.checker.getShorthandAssignmentValueSymbol(declaration) :
            ts.isPropertyAssignment(declaration) && ts.isIdentifier(declaration.initializer) ?
                this.checker.getSymbolAtLocation(declaration.initializer) : undefined;
        return value && this.targetOf(value);
    }

    private isLocal(symbol: ts.Symbol, declaration: ts.Declaration): boolean {
        if (isMember(symbol, declaration)) {
            const owner = (symbol as { parent?: ts.Symbol }).parent;
            const ownerDeclaration = owner?.valueDeclaration ?? owner?.declarations?.[0];
            if (owner && ownerDeclaration) {
                return this.isLocal(owner, ownerDeclaration);
            }
        }
        let child: ts.Node = ts.getNameOfDeclaration(declaration) ?? declaration;
        for (let node = child.parent; node && !ts.isSourceFile(node); child = node, node = node.parent) {
            if (ts.isFunctionLike(node) && node.name !== child || ts.isMappedTypeNode(node) || ts.isInferTypeNode(node)) {
                return true;
            }
        }
        return false;
    }

    private nameIn(place: Place, symbol: ts.Symbol, declaration: ts.Declaration): string {
        if (ts.isSourceFile(declaration) || isModuleExportsObject(declaration)) {
            return place.module;
        }
        if (ts.isModuleDeclaration(declaration) && ts.isStringLiteral(declaration.name)) {
            const augmented = isAugmentation(declaration) ? this.specifiedModule(declaration.name) : undefined;
            return augmented?.owner ?? moduleName(declaration.name.text);
        }
        if (place.mapping && isNamedByType(declaration)) {
            return place.mapping.fullyQualifiedName(symbol);
        }
        if (ts.isObjectLiteralExpression(declaration)) {
            return this.objectName(declaration, place);
        }
        if (ts.isTypeLiteralNode(declaration)) {
            return this.literalName(declaration, place);
        }
        if (isExpression(declaration)) {
            return this.expressionName(declaration, place);
        }
        if (ts.isBinaryExpression(declaration) && isModuleExports(declaration.left)) {
            return this.exportedValueName(declaration.right, place);
        }
        return `${this.ownerName(place, symbol, declaration)}.${lastSegment(declaration)}`;
    }

    private ownerName(place: Place, symbol: ts.Symbol, declaration: ts.Declaration): string {
        if (place.mapping && ts.isFunctionDeclaration(declaration)) {
            return place.mapping.functionOwnerName(declaration);
        }
        const owner = this.ownerOf(symbol, declaration);
        return owner && !isGlobalScope(owner) ? this.declared(owner)?.owner ?? place.module : place.module;
    }

    private ownerOf(symbol: ts.Symbol, declaration: ts.Declaration): ts.Symbol | undefined {
        if (ts.isTypeParameterDeclaration(declaration)) {
            return this.symbolOfDeclaration(declaration.parent);
        }
        const namespace = ts.findAncestor(declaration.parent, ts.isModuleDeclaration);
        return (symbol as { parent?: ts.Symbol }).parent ?? (namespace && this.symbolOfDeclaration(namespace));
    }

    /** The name an object literal's members hang from: what it is assigned to. */
    private objectName(object: ts.ObjectLiteralExpression, place: Place): string {
        return isExportedValue(object) ? place.module : this.literalName(object, place);
    }

    /** A function or a class is named by what it is assigned to, as its members are. */
    private expressionName(expression: ts.FunctionExpression | ts.ArrowFunction | ts.ClassExpression, place: Place): string {
        if (isExportedValue(expression)) {
            return this.exportedValueName(expression, place);
        }
        const own = expression.name?.text;
        if (!own || this.assignedTarget(expression)) {
            return this.literalName(expression, place);
        }
        return assignedName(outermost(expression)) ? `${place.module}.${own}` : `${this.literalName(expression, place)}.${own}`;
    }

    /** What `module.exports =` gives the value it is assigned, which `export default` gives `default`. */
    private exportedValueName(value: ts.Expression, place: Place): string {
        const named = ts.isFunctionExpression(value) || ts.isClassExpression(value) ? value.name : undefined;
        return `${place.module}.${named?.text ?? "default"}`;
    }

    private literalName(literal: ts.Node, place: Place): string {
        if (ts.isExportAssignment(outermost(literal).parent)) {
            return `${place.module}.default`;
        }
        const assigned = this.assignedTarget(literal);
        if (assigned) {
            return assigned.owner;
        }
        const {line, column} = lineAndColumn(literal.getSourceFile(), literal.getStart());
        return `${place.module}.<object:${line}:${column}>`;
    }

    private assignedTarget(literal: ts.Node): Target | undefined {
        const assigned = assignedName(outermost(literal));
        const symbol = assigned && this.checker.getSymbolAtLocation(assigned);
        return symbol && this.targetOf(symbol);
    }

    private kindOf(symbol: ts.Symbol, declaration: ts.Declaration): DeclarationKind {
        if (ts.isModuleDeclaration(declaration)) {
            return "module";
        }
        if (ts.isClassLike(declaration)) {
            return "class";
        }
        if (ts.isInterfaceDeclaration(declaration) || ts.isTypeAliasDeclaration(declaration) || ts.isTypeParameterDeclaration(declaration)) {
            return "interface";
        }
        if (ts.isEnumDeclaration(declaration)) {
            return "enum";
        }
        if (ts.isEnumMember(declaration)) {
            return "enum-member";
        }
        if (ts.isConstructorDeclaration(declaration)) {
            return "constructor";
        }
        if (ts.isAccessor(declaration)) {
            return "field";
        }
        if (ts.isFunctionLike(declaration)) {
            return "method";
        }
        const value = assignedValue(declaration);
        if (value && ts.isClassExpression(unwrapped(value))) {
            return "class";
        }
        return this.checker.getTypeOfSymbolAtLocation(symbol, declaration).getCallSignatures().length > 0 ? "method" : "field";
    }

    private placeOf(fileName: string): Place | undefined {
        if (!this.places.has(fileName)) {
            this.places.set(fileName, this.place(fileName));
        }
        return this.places.get(fileName);
    }

    private place(fileName: string): Place | undefined {
        const file = fileName.replace(/\\/g, "/");
        const sourceFile = this.program.getSourceFile(fileName);
        if (sourceFile && this.program.isSourceFileDefaultLibrary(sourceFile)) {
            return this.mappedPlace(file, path.dirname(path.dirname(file)), null, true);
        }
        const nodeModules = "/node_modules/";
        const at = file.lastIndexOf(nodeModules);
        if (at >= 0) {
            const segments = file.substring(at + nodeModules.length).split("/");
            const directory = segments.slice(0, segments[0].startsWith("@") ? 2 : 1).join("/");
            const root = file.substring(0, at + nodeModules.length) + directory;
            return this.mappedPlace(file, root, this.packageNameAt(root) ?? directory, true);
        }
        for (const root of this.roots) {
            const relative = path.relative(root, path.resolve(root, file)).replace(/\\/g, "/");
            if (!relative.startsWith("../") && !path.isAbsolute(relative)) {
                return sourceFile?.isDeclarationFile ? this.mappedPlace(file, root, null, false) :
                    {module: withoutExtension(relative), file: relative, pkg: null, external: false};
            }
        }
        return undefined;
    }

    private mappedPlace(file: string, root: string, pkg: string | null, external: boolean): Place {
        let mapping = this.mappings.get(root);
        if (!mapping) {
            this.mappings.set(root, mapping = new JavaScriptTypeMapping(this.checker, root, true));
        }
        const relative = path.relative(root, file).replace(/\\/g, "/");
        return {module: withoutExtension(relative), file: relative, pkg, external, mapping};
    }

    private packageNameAt(directory: string): string | undefined {
        if (!this.packageNames.has(directory)) {
            this.packageNames.set(directory, readPackageName(directory));
        }
        return this.packageNames.get(directory);
    }
}

function withoutExtension(file: string): string {
    return file.replace(/\.[^/.]+$/, "");
}

function moduleTarget(place: Place): Target {
    return {name: place.file, owner: place.module, pkg: place.pkg, kind: "module"};
}

/** A module is its file. A namespace is too, except where a declaration row of ours stands on it. */
function isNamedByFile(declaration: ts.Declaration, place: Place): boolean {
    return ts.isSourceFile(declaration) || isModuleExportsObject(declaration) ||
        ts.isModuleDeclaration(declaration) && (ts.isStringLiteral(declaration.name) || place.external);
}

function referenceKind(kind: DeclarationKind): string {
    switch (kind) {
        case "method":
            return "method-call";
        case "field":
        case "enum-member":
            return "field-access";
        default:
            return "type-ref";
    }
}

/** The implementation over an overload signature, and a class over the interface merged with it. */
function primaryDeclaration(symbol: ts.Symbol): ts.Declaration | undefined {
    const declarations = symbol.declarations;
    if (!declarations?.length) {
        return undefined;
    }
    return declarations.find(ts.isClassLike) ||
        declarations.find(d => ts.isFunctionLike(d) && (d as ts.FunctionLikeDeclaration).body !== undefined) ||
        namingDeclarations(symbol)[0];
}

/** What declares a symbol, which the container of an expando does only when nothing else does. */
function namingDeclarations(symbol: ts.Symbol): ts.Declaration[] {
    const declarations = symbol.declarations ?? [];
    const written = declarations.filter(d => !ts.isIdentifier(d));
    return written.length > 0 ? written : declarations;
}

/** Where a declaration is named. `module.exports =` is at `exports`, unless what it assigns is named or the module's own. */
function nameOf(declaration: ts.Declaration): ts.Node | undefined {
    if (ts.isBinaryExpression(declaration) && isModuleExports(declaration.left)) {
        const value = unwrapped(declaration.right);
        const named = (ts.isFunctionExpression(value) || ts.isClassExpression(value)) && value.name;
        return !named && !ts.isObjectLiteralExpression(value) && ts.isPropertyAccessExpression(declaration.left) ?
            declaration.left.name : undefined;
    }
    return ts.getNameOfDeclaration(declaration);
}

/** Whether a value is what `module.exports =` assigns. */
function isExportedValue(value: ts.Node): boolean {
    const parent = outermost(value).parent;
    return ts.isBinaryExpression(parent) && isModuleExports(parent.left);
}

function declares(symbol: ts.Symbol, name: ts.Node): boolean {
    return namingDeclarations(symbol).some(declaration => nameOf(declaration) === name);
}

function isBinding(name: ts.Identifier | ts.PrivateIdentifier, symbol: ts.Symbol): boolean {
    const parent = name.parent;
    return ts.isImportSpecifier(parent) || ts.isImportClause(parent) || ts.isNamespaceImport(parent) ||
        ts.isImportEqualsDeclaration(parent) || ts.isExportSpecifier(parent) || ts.isNamespaceExport(parent) ||
        (symbol.flags & ts.SymbolFlags.Alias) !== 0 && declares(symbol, name) ||
        isModuleExports(parent) && ts.isBinaryExpression(parent.parent) && parent.parent.left === parent && !declares(symbol, name);
}

/** Whether `name` reads a property in an object pattern, which `const {a} = b` does as it declares `a`. */
function isBoundProperty(name: ts.Identifier | ts.PrivateIdentifier): boolean {
    const element = name.parent;
    return ts.isBindingElement(element) && ts.isObjectBindingPattern(element.parent) &&
        name === (element.propertyName ?? element.name);
}

function isConstructed(name: ts.Node): boolean {
    return ts.isNewExpression(name.parent) && name.parent.expression === name ||
        ts.isPropertyAccessExpression(name.parent) && name.parent.name === name &&
        ts.isNewExpression(name.parent.parent) && name.parent.parent.expression === name.parent;
}

function isModuleExports(node: ts.Node): boolean {
    return ts.isPropertyAccessExpression(node) && node.name.text === "exports" &&
        ts.isIdentifier(node.expression) && node.expression.text === "module" ||
        ts.isIdentifier(node) && node.text === "exports";
}

/** `module.exports = {...}`, which is the module's own value, and its members the module's. */
function isModuleExportsObject(declaration: ts.Declaration): boolean {
    return ts.isBinaryExpression(declaration) && isModuleExports(declaration.left) &&
        ts.isObjectLiteralExpression(unwrapped(declaration.right));
}

/** The value inside any parentheses or assertion, where {@link outermost} is the expression around it. */
function unwrapped(expression: ts.Expression): ts.Expression {
    let value = expression;
    while (ts.isParenthesizedExpression(value) || ts.isAssertionExpression(value) || ts.isSatisfiesExpression(value) ||
    ts.isNonNullExpression(value)) {
        value = value.expression;
    }
    return value;
}

function isExpression(declaration: ts.Declaration): declaration is ts.FunctionExpression | ts.ArrowFunction | ts.ClassExpression {
    return ts.isFunctionExpression(declaration) || ts.isArrowFunction(declaration) || ts.isClassExpression(declaration);
}

/** What the parser names by its type, which a package's own naming gives whole. */
function isNamedByType(declaration: ts.Declaration): boolean {
    return ts.isClassLike(declaration) || ts.isInterfaceDeclaration(declaration) || ts.isEnumDeclaration(declaration) ||
        ts.isTypeAliasDeclaration(declaration) || ts.isModuleDeclaration(declaration);
}

function isGlobalScope(symbol: ts.Symbol | undefined): boolean {
    return symbol?.declarations?.some(d => ts.isModuleDeclaration(d) && (d.flags & ts.NodeFlags.GlobalAugmentation) !== 0) === true;
}

function lastSegment(declaration: ts.Declaration): string {
    const name = ts.getNameOfDeclaration(declaration);
    if (!name) {
        return ts.isConstructorDeclaration(declaration) ? "<constructor>" : "default";
    }
    return ts.isComputedPropertyName(name) ? name.getText() : (name as ts.Identifier).text;
}

/** The expression in the position a literal is assigned from, past any parentheses or assertion around it. */
function outermost(literal: ts.Node): ts.Node {
    let value = literal;
    while (ts.isParenthesizedExpression(value.parent) || ts.isAssertionExpression(value.parent) ||
    ts.isSatisfiesExpression(value.parent) || ts.isNonNullExpression(value.parent)) {
        value = value.parent;
    }
    return value;
}

/** The name a value or a type literal is assigned to, as `o` in `const o = {}`, or none. */
function assignedName(value: ts.Node): ts.Node | undefined {
    const parent = value.parent;
    if ((ts.isVariableDeclaration(parent) || ts.isPropertyAssignment(parent) || ts.isPropertyDeclaration(parent) ||
        ts.isPropertySignature(parent) || ts.isTypeAliasDeclaration(parent)) &&
        (parent as { name: ts.Node }).name !== value) {
        return (parent as { name: ts.Node }).name;
    }
    if (!ts.isBinaryExpression(parent) || parent.right !== value || parent.operatorToken.kind !== ts.SyntaxKind.EqualsToken) {
        return undefined;
    }
    const left = parent.left;
    if (ts.isPropertyAccessExpression(left) && left.name.text === "prototype") {
        return leftmost(left.expression);
    }
    return ts.isPropertyAccessExpression(left) ? left.name : left;
}

function assignedValue(declaration: ts.Node): ts.Expression | undefined {
    if (ts.isVariableDeclaration(declaration) || ts.isPropertyAssignment(declaration) || ts.isPropertyDeclaration(declaration)) {
        return declaration.initializer;
    }
    if (ts.isBinaryExpression(declaration)) {
        return declaration.right;
    }
    const parent = declaration.parent as ts.Node | undefined;
    return parent && ts.isBinaryExpression(parent) && parent.left === declaration ? parent.right : undefined;
}

function propertyOf(type: ts.Type, name: string): ts.Symbol | undefined {
    return type.isUnion() ? type.types.map(t => t.getProperty(name)).find(p => p !== undefined) : type.getProperty(name);
}

function leftmost(expression: ts.Expression): ts.Expression {
    while (ts.isPropertyAccessExpression(expression) || ts.isElementAccessExpression(expression) ||
    ts.isParenthesizedExpression(expression)) {
        expression = expression.expression;
    }
    return expression;
}

/** Counted as the search index counts: lines end at a line feed and nothing else, as TypeScript's do not. */
function lineAndColumn(file: ts.SourceFile, offset: number): { line: number, column: number } {
    const before = file.text.substring(0, offset);
    const lineStart = before.lastIndexOf("\n") + 1;
    return {line: before.split("\n").length, column: offset - lineStart + 1};
}

function printedOffsets(text: string): (offset: number) => number {
    const surrogates = [...text.matchAll(/\p{Cs}/gu)].map(match => match.index);
    return surrogates.length === 0 ? offset => offset :
        offset => offset + ESCAPED_SURROGATE_GROWTH * surrogates.filter(at => at < offset).length;
}

function isModuleSpecifier(node: ts.StringLiteralLike): boolean {
    const parent = node.parent;
    if (ts.isImportDeclaration(parent) || ts.isExportDeclaration(parent)) {
        return parent.moduleSpecifier === node;
    }
    if (ts.isExternalModuleReference(parent)) {
        return true;
    }
    if (ts.isLiteralTypeNode(parent)) {
        return ts.isImportTypeNode(parent.parent);
    }
    return ts.isModuleDeclaration(parent) || ts.isCallExpression(parent) && isModuleLoad(parent) && parent.arguments[0] === node;
}

/** In a module, `declare module` adds to the module it names, and declares none. */
function isAugmentation(node: ts.Node): boolean {
    return ts.isModuleDeclaration(node) && ts.isExternalModule(node.getSourceFile());
}

function isModuleLoad(call: ts.CallExpression): boolean {
    return call.expression.kind === ts.SyntaxKind.ImportKeyword ||
        ts.isIdentifier(call.expression) && call.expression.text === "require";
}

/** `export default` of a value with no name of its own, as `export default a.b` has in `b`. */
function isNamelessDefaultExport(node: ts.Node): boolean {
    if (ts.isExportAssignment(node)) {
        return !node.isExportEquals && !ts.isIdentifier(node.expression) && !ts.isPropertyAccessExpression(node.expression);
    }
    return (ts.isFunctionDeclaration(node) || ts.isClassDeclaration(node)) && !node.name;
}

/** Declared where a function runs, as `this.x =` is, but on the class or module that holds it. */
function isMember(symbol: ts.Symbol, declaration: ts.Declaration): boolean {
    return ts.isBinaryExpression(declaration) || ts.isPropertyAccessExpression(declaration) ||
        ts.isElementAccessExpression(declaration) || ts.isCallExpression(declaration) ||
        ts.isParameter(declaration) && (symbol.flags & ts.SymbolFlags.Property) !== 0;
}

function moduleSpecifierOf(declaration: ts.Node): ts.Expression | undefined {
    if (ts.isImportClause(declaration)) {
        return declaration.parent.moduleSpecifier;
    }
    if (ts.isNamespaceImport(declaration)) {
        return declaration.parent.parent.moduleSpecifier;
    }
    if (ts.isImportSpecifier(declaration)) {
        return declaration.parent.parent.parent.moduleSpecifier;
    }
    if (ts.isExportSpecifier(declaration)) {
        return declaration.parent.parent.moduleSpecifier;
    }
    if (ts.isNamespaceExport(declaration)) {
        return declaration.parent.moduleSpecifier;
    }
    if (ts.isImportEqualsDeclaration(declaration)) {
        return ts.isExternalModuleReference(declaration.moduleReference) ?
            declaration.moduleReference.expression : undefined;
    }
    if (ts.isBindingElement(declaration)) {
        return moduleSpecifierOf(declaration.parent.parent);
    }
    if (ts.isVariableDeclaration(declaration) && declaration.initializer) {
        return requiredModule(declaration.initializer);
    }
    return undefined;
}

function requiredModule(expression: ts.Expression): ts.Expression | undefined {
    const root = leftmost(expression);
    return ts.isCallExpression(root) && isModuleLoad(root) ? root.arguments[0] : undefined;
}

function readPackageName(directory: string): string | undefined {
    try {
        const {name} = JSON.parse(ts.sys.readFile(`${directory}/package.json`) ?? "{}");
        return typeof name === "string" ? name : undefined;
    } catch {
        return undefined;
    }
}
