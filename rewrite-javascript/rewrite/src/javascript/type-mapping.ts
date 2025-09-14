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

        // Check for class/interface/enum types first (they may also have Object flag)
        const symbol = type.getSymbol?.();
        if (symbol) {
            if (symbol.flags & (ts.SymbolFlags.Class | ts.SymbolFlags.Interface | ts.SymbolFlags.Enum | ts.SymbolFlags.TypeAlias)) {
                // Create and cache shell first to handle circular references
                const classType = this.createEmptyClassType(type);
                this.typeCache.set(signature, classType);
                this.populateClassType(classType, type);
                return classType;
            }
        }

        // For anonymous object types that could have circular references
        if (type.flags & ts.TypeFlags.Object) {
            const objectFlags = (type as ts.ObjectType).objectFlags;
            if (objectFlags & ts.ObjectFlags.Anonymous) {
                // Anonymous object type - create and cache shell, then populate
                const classType = this.createEmptyClassType(type);
                this.typeCache.set(signature, classType);
                this.populateClassType(classType, type);
                return classType;
            }
        }

        // For non-object types, we can create them directly without recursion concerns
        const result = this.createPrimitiveOrUnknownType(type);
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
        if (Type.isClass(type) && type.fullyQualifiedName === 'lib.RegExp') {
            return Type.Primitive.String;
        }
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
        let signature: ts.Signature | undefined;
        let methodName: string;
        let declaringType: Type.FullyQualified;
        let declaredFormalTypeNames: string[] = [];

        // Handle different kinds of nodes that represent methods or method invocations
        if (ts.isCallExpression(node)) {
            // For method invocations (e.g., _.map(...))
            signature = this.checker.getResolvedSignature(node);
            if (!signature) {
                return undefined;
            }

            const symbol = this.checker.getSymbolAtLocation(node.expression);
            if (!symbol) {
                return undefined;
            }

            // Get the method name
            if (ts.isPropertyAccessExpression(node.expression)) {
                methodName = node.expression.name.getText();
                const exprType = this.checker.getTypeAtLocation(node.expression.expression);
                declaringType = this.getType(exprType) as Type.FullyQualified;
            } else if (ts.isIdentifier(node.expression)) {
                methodName = node.expression.getText();
                // For standalone functions, use the symbol's parent or module
                const parent = (symbol as any).parent;
                if (parent) {
                    const parentType = this.checker.getDeclaredTypeOfSymbol(parent);
                    declaringType = this.getType(parentType) as Type.FullyQualified;
                } else {
                    declaringType = Type.unknownType as Type.FullyQualified;
                }
            } else {
                methodName = symbol.getName();
                declaringType = Type.unknownType as Type.FullyQualified;
            }

            // Get type parameters from signature
            const typeParameters = signature.getTypeParameters();
            if (typeParameters) {
                for (const tp of typeParameters) {
                    declaredFormalTypeNames.push(tp.symbol.getName());
                }
            }
        } else if (ts.isMethodDeclaration(node) || ts.isMethodSignature(node)) {
            // For method declarations
            const symbol = this.checker.getSymbolAtLocation(node.name!);
            if (!symbol) {
                return undefined;
            }

            signature = this.checker.getSignatureFromDeclaration(node);
            if (!signature) {
                return undefined;
            }

            methodName = symbol.getName();

            // Get the declaring type (the class/interface that contains this method)
            const parent = node.parent;
            if (ts.isClassDeclaration(parent) || ts.isInterfaceDeclaration(parent) || ts.isObjectLiteralExpression(parent)) {
                const parentType = this.checker.getTypeAtLocation(parent);
                declaringType = this.getType(parentType) as Type.FullyQualified;
            } else {
                declaringType = Type.unknownType as Type.FullyQualified;
            }

            // Get type parameters from node
            if (node.typeParameters) {
                for (const tp of node.typeParameters) {
                    declaredFormalTypeNames.push(tp.name.getText());
                }
            }
        } else if (ts.isFunctionDeclaration(node) || ts.isFunctionExpression(node)) {
            // For function declarations/expressions
            signature = this.checker.getSignatureFromDeclaration(node);
            if (!signature) {
                return undefined;
            }

            methodName = node.name ? node.name.getText() : "<anonymous>";
            declaringType = Type.unknownType as Type.FullyQualified;

            // Get type parameters from node
            if (node.typeParameters) {
                for (const tp of node.typeParameters) {
                    declaredFormalTypeNames.push(tp.name.getText());
                }
            }
        } else {
            // For other node types, return undefined
            return undefined;
        }

        // Common logic for all method types
        const returnType = signature.getReturnType();
        const parameters = signature.getParameters();
        const parameterTypes: Type[] = [];
        const parameterNames: string[] = [];

        for (const param of parameters) {
            parameterNames.push(param.getName());
            const paramType = this.checker.getTypeOfSymbolAtLocation(param, node);
            parameterTypes.push(this.getType(paramType));
        }

        // Create the Type.Method object
        return Object.assign(new NonDraftableType(), {
            kind: Type.Kind.Method,
            declaringType: declaringType,
            name: methodName,
            returnType: this.getType(returnType),
            parameterNames: parameterNames,
            parameterTypes: parameterTypes,
            thrownExceptions: [], // JavaScript doesn't have checked exceptions
            annotations: [],
            defaultValue: undefined,
            declaredFormalTypeNames: declaredFormalTypeNames,
            toJSON: function () {
                return Type.signature(this);
            }
        }) as Type.Method;
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
     * Create an empty JavaType.Class shell from a TypeScript type.
     * The shell will be populated later to handle circular references.
     */
    private createEmptyClassType(type: ts.Type): Type.Class {
        // Use our custom getFullyQualifiedName method for consistent naming
        let fullyQualifiedName = this.getFullyQualifiedName(type);

        // If getFullyQualifiedName returned unknown, fall back to TypeScript's method
        if (fullyQualifiedName === "unknown") {
            const symbol = type.symbol;
            fullyQualifiedName = symbol ? this.checker.getFullyQualifiedName(symbol) : `<anonymous>${this.checker.typeToString(type)}`;

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
        }

        // Determine the class kind based on symbol flags
        let classKind = Type.Class.Kind.Interface; // Default to interface
        const symbol = type.getSymbol?.();
        if (symbol) {
            if (symbol.flags & ts.SymbolFlags.Class) {
                classKind = Type.Class.Kind.Class;
            } else if (symbol.flags & ts.SymbolFlags.Enum) {
                classKind = Type.Class.Kind.Enum;
            } else if (symbol.flags & ts.SymbolFlags.Interface) {
                classKind = Type.Class.Kind.Interface;
            }
        }

        // Create empty class type shell (no members yet to avoid recursion)
        return Object.assign(new NonDraftableType(), {
            kind: Type.Kind.Class,
            classKind: classKind,
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

    /**
     * Populates the class type with members, methods, heritage, and type parameters
     * Since the shell is already in the cache, any recursive references will find it
     */
    private populateClassType(classType: Type.Class, type: ts.Type): void {
        const symbol = type.getSymbol?.();

        // Try to get base types using TypeScript's getBaseTypes API
        // This works for both local and external types (from node_modules)
        if (type.flags & ts.TypeFlags.Object) {
            let baseTypes: ts.Type[] | undefined;

            // Check if this is a class or interface type that supports getBaseTypes
            const objectType = type as ts.ObjectType;
            if (objectType.objectFlags & (ts.ObjectFlags.Class | ts.ObjectFlags.Interface)) {
                try {
                    baseTypes = (this.checker as any).getBaseTypes?.(type as ts.InterfaceType);
                } catch (e) {
                    // getBaseTypes might fail for some types, fall back to declaration-based extraction
                }
            } else if (symbol) {
                // For constructor functions or type references, we need to get the actual class type
                // Try to get the type of the class itself (not the constructor or instance)
                const classSymbol = symbol.flags & ts.SymbolFlags.Alias ?
                    this.checker.getAliasedSymbol(symbol) : symbol;

                if (classSymbol && classSymbol.flags & (ts.SymbolFlags.Class | ts.SymbolFlags.Interface)) {
                    // Get the type of the class declaration itself
                    const declaredType = this.checker.getDeclaredTypeOfSymbol(classSymbol);
                    if (declaredType && declaredType !== type) {
                        try {
                            baseTypes = (this.checker as any).getBaseTypes?.(declaredType as ts.InterfaceType);
                        } catch (e) {
                            // getBaseTypes might fail, fall back to declaration-based extraction
                        }
                    }
                } else if (classSymbol && classSymbol.valueDeclaration && ts.isClassDeclaration(classSymbol.valueDeclaration)) {
                    // Handle the case where the symbol is for a class value (constructor function)
                    // Get the instance type of the class
                    const instanceType = this.checker.getDeclaredTypeOfSymbol(classSymbol);
                    if (instanceType && instanceType !== type) {
                        try {
                            baseTypes = (this.checker as any).getBaseTypes?.(instanceType as ts.InterfaceType);
                        } catch (e) {
                            // getBaseTypes might fail, fall back to declaration-based extraction
                        }
                    }
                }
            }

            if (baseTypes && baseTypes.length > 0) {
                // For classes, the first base type is usually the superclass
                // Additional base types are interfaces
                if (classType.classKind === Type.Class.Kind.Class) {
                    const firstBase = this.getType(baseTypes[0]);
                    if (Type.isClass(firstBase)) {
                        (classType as any).supertype = firstBase;
                    }
                    // Rest are interfaces
                    for (let i = 1; i < baseTypes.length; i++) {
                        const interfaceType = this.getType(baseTypes[i]);
                        if (Type.isClass(interfaceType)) {
                            classType.interfaces.push(interfaceType);
                        }
                    }
                } else {
                    // For interfaces, all base types are extended interfaces
                    for (const baseType of baseTypes) {
                        const interfaceType = this.getType(baseType);
                        if (Type.isClass(interfaceType)) {
                            classType.interfaces.push(interfaceType);
                        }
                    }
                }
            }
        }

        // Extract type parameters from declarations (not provided by getBaseTypes)
        if (symbol?.declarations) {
            for (const declaration of symbol.declarations) {
                if (ts.isClassDeclaration(declaration) || ts.isInterfaceDeclaration(declaration)) {
                    // Extract type parameters
                    if (declaration.typeParameters) {
                        for (const tp of declaration.typeParameters) {
                            const tpType = this.checker.getTypeAtLocation(tp);
                            classType.typeParameters.push(this.getType(tpType));
                        }
                    }
                    break; // Only process the first declaration
                }
            }
        }

        // Get properties and methods
        const properties = this.checker.getPropertiesOfType(type);
        for (const prop of properties) {
            const declaration = prop.valueDeclaration || prop.declarations?.[0];
            if (!declaration) {
                // Skip properties without declarations (synthetic/built-in properties)
                continue;
            }

            if (prop.flags & ts.SymbolFlags.Method) {
                // TODO: Create Type.Method when method support is added
                // For now, skip methods
                continue;
            } else {
                // Create Type.Variable for fields/properties
                const propType = this.checker.getTypeOfSymbolAtLocation(prop, declaration);
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


    /**
     * Note: Object/Class/Interface types are handled in getType() to properly manage circular references
     * This method should only be called for primitive and unknown types
     */
    private createPrimitiveOrUnknownType(type: ts.Type): Type {
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

        // Check for type aliases that may resolve to primitives
        const symbol = type.getSymbol?.();
        if (symbol && symbol.flags & ts.SymbolFlags.TypeAlias) {
            // Type aliases may resolve to primitive types
            const aliasedType = this.checker.getDeclaredTypeOfSymbol(symbol);
            if (aliasedType !== type) {
                return this.getType(aliasedType);
            }
        }

        return Type.unknownType;
    }
}
