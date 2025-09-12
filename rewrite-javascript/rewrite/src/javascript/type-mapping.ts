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
import ts from "typescript";
import {Type} from "../java";
import {immerable} from "immer";

// Helper class to create Type objects that immer won't traverse
class NonDraftableType {
    [immerable] = false;
}

const builtInTypes = new Set([
    'Array', 'Object', 'Function', 'String', 'Number', 'Boolean',
    'Date', 'RegExp', 'Error', 'Promise', 'Map', 'Set', 'WeakMap',
    'WeakSet', 'Symbol', 'BigInt', 'HTMLElement', 'Document',
    'Window', 'Console', 'JSON', 'Math', 'Reflect', 'Proxy'
]);

export class JavaScriptTypeMapping {
    private readonly typeCache: Map<string | number, Type> = new Map();
    private readonly regExpSymbol: ts.Symbol | undefined;

    constructor(
        private readonly checker: ts.TypeChecker,
        private readonly projectRoot: string = process.cwd()
    ) {
        this.regExpSymbol = checker.resolveName(
            "RegExp",
            undefined,
            ts.SymbolFlags.Type,
            false
        );
    }

    type(node: ts.Node): Type | undefined {
        let type: ts.Type | undefined;
        if (ts.isExpression(node)) {
            type = this.checker.getTypeAtLocation(node);

        } else if (ts.isTypeNode(node)) {
            type = this.checker.getTypeFromTypeNode(node);
        }
        return type && this.getType(type);
    }

    private getType(type: ts.Type): Type {
        const signature = this.getSignature(type);
        const existing = this.typeCache.get(signature);
        if (existing) {
            return existing;
        }

        // For object types that could have circular references, create the shell first
        // and put it in the cache, then populate it. This way circular references
        // will point to the actual object being built, not Type.Unknown
        if (type.flags & ts.TypeFlags.Object) {
            const objectFlags = (type as ts.ObjectType).objectFlags;
            if (objectFlags & ts.ObjectFlags.Anonymous) {
                // Anonymous object type - create and cache shell, then populate
                const classType = this.createEmptyClassType(type);
                this.typeCache.set(signature, classType);
                this.populateClassType(classType, type);
                return classType;
            } else if (type.symbol) {
                // Named type with symbol - create and cache shell, then populate
                const classType = this.createEmptyClassType(type);
                this.typeCache.set(signature, classType);
                this.populateClassType(classType, type);
                return classType;
            }
        }

        // For non-object types, we can create them directly without recursion concerns
        const result = this.createType(type);
        this.typeCache.set(signature, result);
        return result;
    }

    private getSignature(type: ts.Type): string | number {
        // Try to use TypeScript's internal id if available
        if ("id" in type && type.id !== undefined) {
            return type.id as number;
        }

        // Fallback: Generate a string signature based on type characteristics
        const typeString = this.checker.typeToString(type);
        const symbol = type.getSymbol?.();

        if (symbol) {
            const declaration = symbol.valueDeclaration || symbol.declarations?.[0];
            if (declaration) {
                const sourceFile = declaration.getSourceFile();
                const fileName = sourceFile.fileName;
                const pos = declaration.pos;
                // Create unique signature from file + position + type string
                // This ensures types from different modules are distinguished
                return `${fileName}:${pos}:${typeString}`;
            }
        }

        // Last resort: use type string with a prefix to distinguish from numeric IDs
        // This might happen for synthetic types or types without declarations
        return `synthetic:${typeString}`;
    }

    primitiveType(node: ts.Node): Type.Primitive {
        const type = this.type(node);
        return Type.isPrimitive(type) ? type : Type.Primitive.None;
    }

    variableType(node: ts.NamedDeclaration): Type.Variable | undefined {
        if (ts.isVariableDeclaration(node)) {
            const symbol = this.checker.getSymbolAtLocation(node.name);
            if (symbol) {
                // TODO: Implement in Phase 6
                // const type = this.checker.getTypeOfSymbolAtLocation(symbol, node);
                // return JavaType.Variable with proper mapping
            }
        }
        return undefined;
    }

    methodType(node: ts.Node): Type.Method | undefined {
        return undefined;
    }

    /**
     * Get the fully qualified name for a TypeScript type.
     * Format: "module-specifier.TypeName" (e.g., "@mui/material.Button", "src/components/Button.Button")
     */
    private getFullyQualifiedName(type: ts.Type): string {
        const symbol = type.getSymbol?.();
        if (!symbol) {
            return "unknown";
        }

        const typeName = symbol.getName();
        const declaration = symbol.valueDeclaration || symbol.declarations?.[0];
        if (!declaration) {
            // No declaration - might be a built-in or synthetic type
            if (builtInTypes.has(typeName)) {
                return `lib.${typeName}`;
            }
            return typeName;
        }

        const sourceFile = declaration.getSourceFile();
        const fileName = sourceFile.fileName;

        // Check if this is a test file (snowflake ID as filename)
        // Test files are generated with numeric IDs like "672087069480189952.ts"
        if (/^\d+\.(ts|tsx|js|jsx)$/.test(fileName)) {
            // For test files, just return the type name without module prefix
            return typeName;
        }

        // Check if this is from TypeScript's lib files (lib.d.ts, lib.dom.d.ts, etc.)
        if (fileName.includes("/typescript/lib/lib.") || fileName.includes("\\typescript\\lib\\lib.")) {
            return `lib.${typeName}`;
        }

        // Check if this is from an external module (node_modules or .d.ts)
        if (sourceFile.isDeclarationFile || fileName.includes("node_modules")) {
            const packageName = this.extractPackageName(fileName);
            if (packageName) {
                return `${packageName}.${typeName}`;
            }
        }

        // For local files, use relative path from project root
        const relativePath = this.getRelativeModulePath(fileName);
        return `${relativePath}.${typeName}`;
    }

    /**
     * Extract package name from a node_modules path.
     * Examples:
     * - /path/to/project/node_modules/react/index.d.ts -> "react"
     * - /path/to/project/node_modules/@mui/material/Button/index.d.ts -> "@mui/material"
     */
    private extractPackageName(fileName: string): string | null {
        const match = fileName.match(/node_modules\/(@[^\/]+\/[^\/]+|[^\/]+)/);
        return match ? match[1] : null;
    }

    /**
     * Get relative module path from project root.
     * Removes file extension and uses forward slashes.
     */
    private getRelativeModulePath(fileName: string): string {
        // Remove project root and normalize path
        let relativePath = fileName;
        if (fileName.startsWith(this.projectRoot)) {
            relativePath = fileName.slice(this.projectRoot.length);
        }

        // Remove leading slash and file extension
        relativePath = relativePath.replace(/^\//, '').replace(/\.[^/.]+$/, '');

        // Convert backslashes to forward slashes (for Windows)
        relativePath = relativePath.replace(/\\/g, '/');

        return relativePath;
    }

    /**
     * Create a JavaType.Class from a TypeScript type and symbol.
     */
    private createEmptyClassType(type: ts.Type): Type.Class {
        const symbol = type.symbol;
        let fullyQualifiedName = symbol ? this.checker.getFullyQualifiedName(symbol) : `<anonymous>${this.checker.typeToString(type)}`;

        // Fix FQN for types from @types packages
        // TypeScript returns "_.LoDashStatic" but we want "@types/lodash.LoDashStatic"
        if (symbol && symbol.declarations && symbol.declarations.length > 0) {
            const sourceFile = symbol.declarations[0].getSourceFile();
            const fileName = sourceFile.fileName;
            // Check if this is from @types package
            const typesMatch = fileName.match(/node_modules\/@types\/([^/]+)/);
            if (typesMatch) {
                const packageName = typesMatch[1];
                // Replace the module specifier part with @types/package
                fullyQualifiedName = fullyQualifiedName.replace(/^[^.]+\./, `@types/${packageName}.`);
            }
        }

        // Create empty class type shell (no members yet to avoid recursion)
        return Object.assign(new NonDraftableType(), {
            kind: Type.Kind.Class,
            classKind: Type.Class.Kind.Interface, // Default to interface for TypeScript types
            fullyQualifiedName: fullyQualifiedName,
            typeParameters: [],
            annotations: [],
            interfaces: [],
            members: [],
            methods: [],
            toJSON: function () {
                return Type.signature(this);
            }
        }) as Type.Class;
    }

    private populateClassType(classType: Type.Class, type: ts.Type): void {
        // Now populate the class type with members
        // Since the shell is already in the cache, any recursive references will find it
        const properties = this.checker.getPropertiesOfType(type);
        for (const prop of properties) {
            if (!(prop.flags & ts.SymbolFlags.Method)) {
                const propType = this.checker.getTypeOfSymbolAtLocation(prop, prop.valueDeclaration || prop.declarations![0]);
                const variable: Type.Variable = Object.assign(new NonDraftableType(), {
                    kind: Type.Kind.Variable,
                    name: prop.getName(),
                    owner: classType,  // Cyclic reference to the containing class (already in cache)
                    type: this.getType(propType), // This will find classType in cache if it's recursive
                    annotations: [],
                    toJSON: function () {
                        return Type.signature(this);
                    }
                }) as Type.Variable;
                classType.members.push(variable);
            }
        }
    }

    private createClassType(type: ts.Type, symbol: ts.Symbol, kind: Type.Class.Kind): Type.Class {
        const fullyQualifiedName = this.getFullyQualifiedName(type);

        // Collect all the data first
        const typeParameters: Type[] = [];
        const interfaces: Type.Class[] = [];
        const methods: Type.Method[] = [];
        let supertype: Type.Class | undefined;

        // Get type parameters and heritage information from declarations
        if (symbol.declarations) {
            for (const declaration of symbol.declarations) {
                if (ts.isClassDeclaration(declaration) || ts.isInterfaceDeclaration(declaration)) {
                    // Extract type parameters
                    if (declaration.typeParameters) {
                        for (const tp of declaration.typeParameters) {
                            const tpType = this.checker.getTypeAtLocation(tp);
                            typeParameters.push(this.getType(tpType));
                        }
                    }

                    // Extract heritage information
                    if (declaration.heritageClauses) {
                        const heritage = this.extractHeritage(declaration.heritageClauses, kind);
                        if (heritage.supertype) {
                            supertype = heritage.supertype;
                        }
                        interfaces.push(...heritage.interfaces);
                    }

                    break; // Only process the first declaration
                }
            }
        }

        const classType: Type.Class = Object.assign(new NonDraftableType(), {
            kind: Type.Kind.Class,
            classKind: kind,
            fullyQualifiedName: fullyQualifiedName,
            typeParameters: typeParameters || [],
            supertype: supertype,
            annotations: [],
            interfaces: interfaces || [],
            members: [],  // Will be populated below
            methods: methods || [],
            toJSON: function () {
                return Type.signature(this);
            }
        }) as Type.Class;

        // Get members (properties)
        const properties = this.checker.getPropertiesOfType(type);
        for (const prop of properties) {
            // Skip methods for now (will be handled in Phase 3)
            if (!(prop.flags & ts.SymbolFlags.Method)) {
                // Get a declaration to use for type checking
                const declaration = prop.valueDeclaration || prop.declarations?.[0];
                if (!declaration) {
                    // Skip properties without declarations (synthetic properties)
                    continue;
                }

                const propType = this.checker.getTypeOfSymbolAtLocation(prop, declaration);
                const variable: Type.Variable = Object.assign(new NonDraftableType(), {
                    kind: Type.Kind.Variable,
                    name: prop.getName(),
                    owner: classType,  // Cyclic reference to the containing class
                    type: this.getType(propType),
                    annotations: [],
                    toJSON: function () {
                        return Type.signature(this);
                    }
                }) as Type.Variable;
                classType.members.push(variable);
            }
        }

        return classType;
    }

    /**
     * Extract supertype and interfaces from heritage clauses.
     */
    private extractHeritage(heritageClauses: ts.NodeArray<ts.HeritageClause>, kind: Type.Class.Kind):
        { supertype?: Type.Class, interfaces: Type.Class[] } {

        let supertype: Type.Class | undefined = undefined;
        const interfaces: Type.Class[] = [];

        for (const clause of heritageClauses) {
            if (clause.token === ts.SyntaxKind.ExtendsKeyword) {
                if (kind === Type.Class.Kind.Class && clause.types.length > 0) {
                    // For classes, extends means superclass (only first one)
                    const superType = this.checker.getTypeAtLocation(clause.types[0]);
                    const superJavaType = this.getType(superType);
                    if (Type.isClass(superJavaType)) {
                        supertype = superJavaType;
                    }
                } else if (kind === Type.Class.Kind.Interface) {
                    // For interfaces, extends means extended interfaces (can be multiple)
                    for (const extendedType of clause.types) {
                        const extType = this.checker.getTypeAtLocation(extendedType);
                        const extJavaType = this.getType(extType);
                        if (Type.isClass(extJavaType)) {
                            interfaces.push(extJavaType);
                        }
                    }
                }
            } else if (clause.token === ts.SyntaxKind.ImplementsKeyword) {
                // Implements clause (only for classes)
                for (const implType of clause.types) {
                    const interfaceType = this.checker.getTypeAtLocation(implType);
                    const interfaceJavaType = this.getType(interfaceType);
                    if (Type.isClass(interfaceJavaType)) {
                        interfaces.push(interfaceJavaType);
                    }
                }
            }
        }

        return {supertype, interfaces};
    }

    private createType(type: ts.Type): Type {
        // Check for literals first
        if (type.isLiteral()) {
            if (type.isNumberLiteral()) {
                return Type.Primitive.Double;
            } else if (type.isStringLiteral()) {
                return Type.Primitive.String;
            }
        }

        // Check for primitive types
        if (type.flags === ts.TypeFlags.Null) {
            return Type.Primitive.Null;
        } else if (type.flags === ts.TypeFlags.Undefined) {
            return Type.Primitive.None;
        } else if (
            type.flags === ts.TypeFlags.Number ||
            type.flags === ts.TypeFlags.NumberLiteral ||
            type.flags === ts.TypeFlags.NumberLike
        ) {
            return Type.Primitive.Double;
        } else if (
            type.flags === ts.TypeFlags.String ||
            type.flags === ts.TypeFlags.StringLiteral ||
            type.flags === ts.TypeFlags.StringLike
        ) {
            return Type.Primitive.String;
        } else if (type.flags === ts.TypeFlags.Void) {
            return Type.Primitive.Void;
        } else if (
            type.flags === ts.TypeFlags.BigInt ||
            type.flags === ts.TypeFlags.BigIntLiteral ||
            type.flags === ts.TypeFlags.BigIntLike
        ) {
            return Type.Primitive.Long;
        } else if (
            (type.symbol !== undefined && type.symbol === this.regExpSymbol) ||
            this.checker.typeToString(type) === "RegExp"
        ) {
            return Type.Primitive.String;
        }

        /**
         * TypeScript may assign multiple flags to a single type (e.g., Boolean + Union).
         * Using a bitwise check ensures we detect Boolean even if other flags are set.
         */
        if (
            type.flags & ts.TypeFlags.Boolean ||
            type.flags & ts.TypeFlags.BooleanLiteral ||
            type.flags & ts.TypeFlags.BooleanLike
        ) {
            return Type.Primitive.Boolean;
        }

        // Check for class/interface types
        const symbol = type.getSymbol?.();
        if (symbol) {
            if (symbol.flags & ts.SymbolFlags.Class) {
                return this.createClassType(type, symbol, Type.Class.Kind.Class);
            } else if (symbol.flags & ts.SymbolFlags.Interface) {
                return this.createClassType(type, symbol, Type.Class.Kind.Interface);
            } else if (symbol.flags & ts.SymbolFlags.Enum) {
                return this.createClassType(type, symbol, Type.Class.Kind.Enum);
            } else if (symbol.flags & ts.SymbolFlags.TypeAlias) {
                // Type aliases may resolve to class-like structures
                const aliasedType = this.checker.getDeclaredTypeOfSymbol(symbol);
                if (aliasedType !== type) {
                    return this.getType(aliasedType);
                }
            }
        }

        // Note: Object types are now handled in getType() to properly manage circular references
        // This method should only be called for non-object types

        return Type.unknownType;
    }
}
