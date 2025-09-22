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
        private readonly checker: ts.TypeChecker
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

        // Check if this is an array type BEFORE checking for classes
        // TypeScript represents Array<T> as a reference to the Array interface
        if (this.checker.isArrayType(type)) {
            const arrayType = this.createArrayType(type as ts.TypeReference);
            this.typeCache.set(signature, arrayType);
            return arrayType;
        }

        // Check for class/interface/enum types (but not arrays)
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

                // Check if the object is an imported symbol
                const objSymbol = this.checker.getSymbolAtLocation(node.expression.expression);
                let isImport = false;
                if (objSymbol) {
                    // Only call getAliasedSymbol if the symbol is actually an alias
                    if (objSymbol.flags & ts.SymbolFlags.Alias) {
                        const aliasedSymbol = this.checker.getAliasedSymbol(objSymbol);
                        isImport = aliasedSymbol && aliasedSymbol !== objSymbol;
                    }
                }

                const exprType = this.checker.getTypeAtLocation(node.expression.expression);
                const mappedType = this.getType(exprType);

                // Handle different types
                if (mappedType && mappedType.kind === Type.Kind.Class) {
                    // Update the declaring type with the corrected FQN
                    if (isImport && objSymbol) {
                        const importName = objSymbol.getName();
                        const origFqn = (mappedType as Type.Class).fullyQualifiedName;
                        const lastDot = origFqn.lastIndexOf('.');
                        if (lastDot > 0) {
                            const typeName = origFqn.substring(lastDot + 1);
                            declaringType = {
                                kind: Type.Kind.Class,
                                fullyQualifiedName: `${importName}.${typeName}`
                            } as Type.FullyQualified;
                        } else {
                            declaringType = mappedType as Type.FullyQualified;
                        }
                    } else {
                        declaringType = mappedType as Type.FullyQualified;
                    }
                } else {
                    // Handle primitive types and other non-class types
                    if (mappedType) {
                        if ((mappedType as any).keyword === 'String') {
                            declaringType = {
                                kind: Type.Kind.Class,
                                fullyQualifiedName: 'lib.String'
                            } as Type.FullyQualified;
                        } else if ((mappedType as any).keyword === 'Number') {
                            declaringType = {
                                kind: Type.Kind.Class,
                                fullyQualifiedName: 'lib.Number'
                            } as Type.FullyQualified;
                        } else {
                            // Fallback for other types
                            declaringType = mappedType as Type.FullyQualified;
                        }
                    } else {
                        // Default to unknown if we can't determine the type
                        declaringType = Type.unknownType as Type.FullyQualified;
                    }
                }

                // For string methods like 'hello'.split(), ensure we have a proper declaring type for primitives
                if (!isImport && declaringType === Type.unknownType) {
                    // If the expression type is a primitive string, use lib.String as declaring type
                    const typeString = this.checker.typeToString(exprType);
                    if (typeString === 'string' || exprType.flags & ts.TypeFlags.String || exprType.flags & ts.TypeFlags.StringLiteral) {
                        declaringType = {
                            kind: Type.Kind.Class,
                            fullyQualifiedName: 'lib.String'
                        } as Type.FullyQualified;
                    } else if (typeString === 'number' || exprType.flags & ts.TypeFlags.Number || exprType.flags & ts.TypeFlags.NumberLiteral) {
                        declaringType = {
                            kind: Type.Kind.Class,
                            fullyQualifiedName: 'lib.Number'
                        } as Type.FullyQualified;
                    } else {
                        // Fallback for other primitive types or unknown
                        declaringType = Type.unknownType as Type.FullyQualified;
                    }
                }

            } else if (ts.isIdentifier(node.expression)) {
                methodName = node.expression.getText();

                // Check if this is an import first
                const symbol = this.checker.getSymbolAtLocation(node.expression);
                let moduleSpecifier: string | undefined;

                if (symbol) {
                    // Check if this is an aliased symbol (i.e., an import)
                    let aliasedSymbol: ts.Symbol | undefined;
                    if (symbol.flags & ts.SymbolFlags.Alias) {
                        aliasedSymbol = this.checker.getAliasedSymbol(symbol);
                    }

                    // If getAliasedSymbol returns something different, it's an import
                    if (aliasedSymbol && aliasedSymbol !== symbol) {
                        // This is definitely an imported symbol
                        // Now find the import declaration to get the module specifier
                        if (symbol.declarations && symbol.declarations.length > 0) {
                            let importNode: ts.Node = symbol.declarations[0];

                            // Traverse up to find the ImportDeclaration
                            while (importNode && !ts.isImportDeclaration(importNode)) {
                                importNode = importNode.parent;
                            }

                            if (importNode && ts.isImportDeclaration(importNode)) {
                                const importDeclNode = importNode as ts.ImportDeclaration;
                                if (ts.isStringLiteral(importDeclNode.moduleSpecifier)) {
                                    moduleSpecifier = importDeclNode.moduleSpecifier.text;
                                }
                            }
                        }
                    }
                }

                if (moduleSpecifier) {
                    // This is an imported function - use the module specifier as declaring type
                    if (moduleSpecifier.startsWith('node:')) {
                        // Node.js built-in module
                        declaringType = {
                            kind: Type.Kind.Class,
                            fullyQualifiedName: 'node'
                        } as Type.FullyQualified;
                        methodName = moduleSpecifier.substring(5); // Remove 'node:' prefix
                    } else {
                        // Regular module import
                        declaringType = {
                            kind: Type.Kind.Class,
                            fullyQualifiedName: moduleSpecifier
                        } as Type.FullyQualified;
                        methodName = '<default>';
                    }
                } else {
                    // Fall back to the original logic for non-imported functions
                    const exprType = this.checker.getTypeAtLocation(node.expression);
                    const funcType = this.getType(exprType);

                    if (funcType && funcType.kind === Type.Kind.Class) {
                        const fqn = (funcType as Type.Class).fullyQualifiedName;
                        const lastDot = fqn.lastIndexOf('.');

                        if (lastDot > 0) {
                            // For functions from modules, use the module part as declaring type
                            declaringType = {
                                kind: Type.Kind.Class,
                                fullyQualifiedName: fqn.substring(0, lastDot)
                            } as Type.FullyQualified;
                        } else {
                            // No dots in the name - the type IS the module itself
                            declaringType = funcType as Type.FullyQualified;
                        }
                    } else {
                        // Try to use the symbol's parent or module
                        const parent = (symbol as any).parent;
                        if (parent) {
                            const parentType = this.checker.getDeclaredTypeOfSymbol(parent);
                            declaringType = this.getType(parentType) as Type.FullyQualified;
                        } else {
                            declaringType = Type.unknownType as Type.FullyQualified;
                        }
                    }
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
     * Create a JavaType.Array from a TypeScript array type
     */
    private createArrayType(type: ts.TypeReference): Type.Array {
        // Get the element type (type argument of Array<T>)
        const typeArgs = this.checker.getTypeArguments(type);
        const elemType = typeArgs.length > 0 ? this.getType(typeArgs[0]) : Type.unknownType;
        
        return Object.assign(new NonDraftableType(), {
            kind: Type.Kind.Array,
            elemType: elemType,
            annotations: [],
            toJSON: function () {
                return Type.signature(this);
            }
        }) as Type.Array;
    }

    /**
     * Get the fully qualified name for a TypeScript type.
     * Uses TypeScript's built-in resolution which properly handles things like:
     * - React.Component (not @types/react.Component)
     * - _.LoDashStatic (not @types/lodash.LoDashStatic)
     */
    private getFullyQualifiedName(type: ts.Type): string {
        const symbol = type.getSymbol?.();
        if (!symbol) {
            return "unknown";
        }

        // Use TypeScript's built-in getFullyQualifiedName
        // This returns names with quotes that we need to clean up
        // e.g., '"React"."Component"' -> 'React.Component'
        const tsQualifiedName = this.checker.getFullyQualifiedName(symbol);
        const cleanedName = tsQualifiedName.replace(/"/g, '');

        // If it's just a simple name without dots, check if it's a built-in type
        if (!cleanedName.includes('.')) {
            if (builtInTypes.has(cleanedName)) {
                return `lib.${cleanedName}`;
            }
        }

        return cleanedName;
    }


    /**
     * Create an empty JavaType.Class shell from a TypeScript type.
     * The shell will be populated later to handle circular references.
     */
    private createEmptyClassType(type: ts.Type): Type.Class {
        // Use our getFullyQualifiedName method which uses TypeScript's built-in resolution
        const fullyQualifiedName = this.getFullyQualifiedName(type);

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
