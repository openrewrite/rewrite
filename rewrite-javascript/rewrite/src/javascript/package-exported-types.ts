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
import {Type} from "../java";
import {JavaScriptTypeMapping} from "./type-mapping";

// Free functions are not exported-type entries; only nominal types (and value modules, which are
// descended into for nested types) qualify.
const TYPE_SYMBOL_FLAGS =
    ts.SymbolFlags.Class |
    ts.SymbolFlags.Interface |
    ts.SymbolFlags.Enum |
    ts.SymbolFlags.TypeAlias |
    ts.SymbolFlags.ValueModule;

/**
 * Enumerate the OWN public exported types of one or more npm packages into the engine's
 * {@link Type} model. The JS analog of the C# {@code AssemblyTypeEnumerator.Enumerate}:
 * a {@code ts.Program} is rooted at each package's declaration entry, the {@code references}
 * closure supplies node_modules resolution, and the types each own package exports are
 * mapped with full members and methods. Types reached transitively come back shallow.
 *
 * @param ownArtifacts package directories whose public types should be defined (full bodies)
 * @param references node_modules roots / sibling packages used only for symbol resolution
 */
export function exportedTypes(ownArtifacts: string[], references: string[]): Type.FullyQualified[] {
    const entries: string[] = [];
    for (const artifact of ownArtifacts) {
        const entry = declarationFileEntry(artifact) ?? resolvePackageEntry(artifact);
        if (entry) {
            entries.push(path.normalize(entry));
        }
    }
    if (entries.length === 0) {
        return [];
    }

    const compilerOptions: ts.CompilerOptions = {
        target: ts.ScriptTarget.Latest,
        module: ts.ModuleKind.CommonJS,
        moduleResolution: ts.ModuleResolutionKind.Node10,
        noEmit: true,
        allowJs: true,
        checkJs: false,
        esModuleInterop: true,
        allowSyntheticDefaultImports: true,
        skipLibCheck: true,
        forceConsistentCasingInFileNames: false,
    };

    const host = ts.createCompilerHost(compilerOptions);
    // Default node resolution from each file's on-disk location, falling back to the
    // explicit reference roots when a dependency isn't co-located with the own package.
    host.resolveModuleNameLiterals = (moduleLiterals, containingFile) =>
        moduleLiterals.map(literal => {
            let resolved = ts.resolveModuleName(literal.text, containingFile, compilerOptions, host);
            if (!resolved.resolvedModule) {
                for (const ref of references) {
                    const probe = ts.resolveModuleName(literal.text, path.join(ref, "__probe__.ts"), compilerOptions, host);
                    if (probe.resolvedModule) {
                        resolved = probe;
                        break;
                    }
                }
            }
            return resolved;
        });

    const program = ts.createProgram(entries, compilerOptions, host);
    const checker = program.getTypeChecker();
    const typeMapping = new JavaScriptTypeMapping(checker);

    // Lowest arity wins for a given FQN, mirroring the C# byFqn dedup.
    const byFqn = new Map<string, Type.Class>();
    const collect = (cls: Type.Class): void => {
        const fqn = cls.fullyQualifiedName;
        if (!fqn || fqn === "unknown" || fqn === "<unknown>" || fqn === Type.FUNCTION_TYPE_NAME) {
            return;
        }
        const existing = byFqn.get(fqn);
        if (!existing || arity(cls) < arity(existing)) {
            byFqn.set(fqn, cls);
        }
    };

    const mapSymbols = (symbols: ts.Symbol[]): void => {
        for (const symbol of symbols) {
            // Isolate per-symbol mapping: one throwing export must not abort the coordinate.
            try {
                const declNodes = classLikeDeclarations(symbol);
                if (declNodes.length > 0) {
                    const cls = typeMapping.declarationType(declNodes[0]);
                    if (Type.isClass(cls)) {
                        // Walk every declaration so merged interfaces / class+namespace merges keep
                        // all their methods; populateMethods dedups by signature across the calls.
                        for (const declNode of declNodes) {
                            populateMethods(typeMapping, declNode, cls);
                        }
                        collect(cls);
                    }
                } else {
                    // Type aliases / namespaces: keep only those that resolve to a nominal class.
                    const mapped = typeMapping.exportedType(symbol);
                    if (Type.isClass(mapped)) {
                        collect(mapped);
                    }
                }
            } catch (e) {
                console.warn(`exportedTypes: skipping export '${symbol.getName()}': ${e}`);
            }
        }
    };

    for (const entry of entries) {
        const sourceFile = program.getSourceFile(entry);
        if (!sourceFile) {
            continue;
        }
        if (ts.isExternalModule(sourceFile)) {
            const moduleSymbol = checker.getSymbolAtLocation(sourceFile) ?? (sourceFile as { symbol?: ts.Symbol }).symbol;
            if (moduleSymbol) {
                mapSymbols(enumerateTypeSymbols(checker, moduleSymbol));
            }
            continue;
        }
        // A global script (lib.*.d.ts / ambient .d.ts): globals live in global scope, not on a
        // module symbol. Enumerate top-level ambient declarations + `declare module` ambients.
        mapSymbols(globalTypeSymbols(checker, sourceFile));
        for (const ambient of checker.getAmbientModules()) {
            if (ambient.declarations?.some(d => d.getSourceFile() === sourceFile)) {
                mapSymbols(enumerateTypeSymbols(checker, ambient));
            }
        }
    }

    return [...byFqn.values()];
}

function arity(cls: Type.Class): number {
    return cls.typeParameters?.length ?? 0;
}

/** A TypeScript declaration/source FILE (e.g. a `lib.*.d.ts` of ambient globals), used directly as a program entry. */
function declarationFileEntry(artifact: string): string | undefined {
    return ts.sys.fileExists(artifact) && /\.[cm]?tsx?$/.test(artifact) ? artifact : undefined;
}

/** Top-level ambient type declarations of a script (non-module) file — a `lib.*.d.ts`'s globals; namespaces descended for nested types. */
function globalTypeSymbols(checker: ts.TypeChecker, sourceFile: ts.SourceFile): ts.Symbol[] {
    const out: ts.Symbol[] = [];
    const seen = new Set<ts.Symbol>();
    const push = (name: ts.Node | undefined): void => {
        const symbol = name ? checker.getSymbolAtLocation(name) : undefined;
        if (symbol && !seen.has(symbol)) {
            seen.add(symbol);
            out.push(symbol);
        }
    };
    const visit = (node: ts.Node): void => {
        if (ts.isInterfaceDeclaration(node) || ts.isClassDeclaration(node) ||
            ts.isEnumDeclaration(node) || ts.isTypeAliasDeclaration(node)) {
            push(node.name);
        } else if (ts.isModuleDeclaration(node)) {
            push(node.name);
            if (node.body && ts.isModuleBlock(node.body)) {
                node.body.statements.forEach(visit);
            }
        }
    };
    sourceFile.statements.forEach(visit);
    return out;
}

/**
 * Resolve a package directory to its declaration entry: the package.json `types`/`typings`
 * field, else the `exports["."]` types condition (modern packages declare types only there),
 * else a top-level `index.d.ts` (then `index.ts`).
 */
function resolvePackageEntry(dir: string): string | undefined {
    const packageJson = path.join(dir, "package.json");
    if (ts.sys.fileExists(packageJson)) {
        try {
            const pkg = JSON.parse(ts.sys.readFile(packageJson) || "{}");
            const typesField: unknown = pkg.types ?? pkg.typings;
            const candidate = typeof typesField === "string"
                ? typesField
                : exportsTypesEntry(pkg.exports);
            if (typeof candidate === "string") {
                const resolved = path.resolve(dir, candidate);
                if (ts.sys.fileExists(resolved)) {
                    return resolved;
                }
                if (ts.sys.fileExists(resolved + ".d.ts")) {
                    return resolved + ".d.ts";
                }
            }
        } catch {
            // Malformed package.json; fall through to conventional entry points.
        }
    }
    for (const candidate of ["index.d.ts", "index.ts", "index.tsx"]) {
        const resolved = path.join(dir, candidate);
        if (ts.sys.fileExists(resolved)) {
            return resolved;
        }
    }
    return undefined;
}

/** The declaration file for the package's `.` subpath from its `exports` map, if any. */
function exportsTypesEntry(exports: unknown): string | undefined {
    if (typeof exports === "string") {
        return typesFromCondition(exports);
    }
    if (!exports || typeof exports !== "object") {
        return undefined;
    }
    const map = exports as Record<string, unknown>;
    // `exports["."]`, or the whole object when it is condition-only sugar (no subpath keys).
    const dot = "." in map
        ? map["."]
        : (Object.keys(map).some(k => k.startsWith(".")) ? undefined : map);
    return dot === undefined ? undefined : typesFromCondition(dot);
}

/**
 * Pull a declaration file out of an `exports` condition value: an explicit `types` condition,
 * else the `types` nested under `import`/`require`/`default`. A bare target string is a runtime
 * (JS) entry, not types, unless it is itself a declaration file.
 */
function typesFromCondition(entry: unknown): string | undefined {
    if (typeof entry === "string") {
        return /\.d\.[cm]?ts$/.test(entry) ? entry : undefined;
    }
    if (!entry || typeof entry !== "object") {
        return undefined;
    }
    const o = entry as Record<string, unknown>;
    if (typeof o.types === "string") {
        return o.types;
    }
    const nestedTypes = typesFromCondition(o.types);
    if (nestedTypes) {
        return nestedTypes;
    }
    for (const cond of ["import", "require", "default"]) {
        const nested = typesFromCondition(o[cond]);
        if (nested) {
            return nested;
        }
    }
    return undefined;
}

/**
 * The exported symbols of a module that name a type, resolving aliases and descending one
 * level into re-exported namespaces (e.g. `export = SomeNamespace`) to reach nested types.
 */
function enumerateTypeSymbols(checker: ts.TypeChecker, moduleSymbol: ts.Symbol): ts.Symbol[] {
    const out: ts.Symbol[] = [];
    const seen = new Set<ts.Symbol>();
    const queue: ts.Symbol[] = [...moduleExports(checker, moduleSymbol)];
    while (queue.length > 0) {
        let symbol = queue.shift()!;
        if (symbol.flags & ts.SymbolFlags.Alias) {
            symbol = checker.getAliasedSymbol(symbol);
        }
        if (seen.has(symbol)) {
            continue;
        }
        seen.add(symbol);
        if (symbol.flags & TYPE_SYMBOL_FLAGS) {
            out.push(symbol);
        }
        if (symbol.flags & (ts.SymbolFlags.ValueModule | ts.SymbolFlags.NamespaceModule)) {
            for (const nested of moduleExports(checker, symbol)) {
                queue.push(nested);
            }
        }
    }
    return out;
}

function moduleExports(checker: ts.TypeChecker, symbol: ts.Symbol): ts.Symbol[] {
    try {
        return checker.getExportsOfModule(symbol);
    } catch {
        const members: ts.Symbol[] = [];
        symbol.exports?.forEach(s => members.push(s));
        return members;
    }
}

// All class/interface/enum declarations of a symbol. Returns more than one for declaration
// merging (e.g. two `interface Req {...}` blocks, or a class merged with a namespace).
function classLikeDeclarations(
    symbol: ts.Symbol
): Array<ts.ClassDeclaration | ts.InterfaceDeclaration | ts.EnumDeclaration> {
    const out: Array<ts.ClassDeclaration | ts.InterfaceDeclaration | ts.EnumDeclaration> = [];
    for (const declaration of symbol.declarations ?? []) {
        if (ts.isClassDeclaration(declaration) || ts.isInterfaceDeclaration(declaration) || ts.isEnumDeclaration(declaration)) {
            out.push(declaration);
        }
    }
    return out;
}

/**
 * Populate `classType.methods`, which the normal mapping path leaves empty. Scoped to the
 * enumerator so parser output is unchanged: walk the declaration's method/constructor nodes,
 * map each via the public {@link JavaScriptTypeMapping.methodType}, and dedup by signature.
 */
function populateMethods(
    typeMapping: JavaScriptTypeMapping,
    node: ts.ClassDeclaration | ts.InterfaceDeclaration | ts.EnumDeclaration,
    classType: Type.Class
): void {
    if (!ts.isClassDeclaration(node) && !ts.isInterfaceDeclaration(node)) {
        return;
    }
    const seen = new Set<string>(classType.methods.map(m => Type.signature(m)));
    for (const member of node.members) {
        if (ts.isMethodDeclaration(member) || ts.isMethodSignature(member) || ts.isConstructorDeclaration(member)) {
            const method = typeMapping.methodType(member);
            if (!method) {
                continue;
            }
            method.declaringType = classType;
            const sig = Type.signature(method);
            if (seen.has(sig)) {
                continue;
            }
            seen.add(sig);
            classType.methods.push(method);
        }
    }
}
