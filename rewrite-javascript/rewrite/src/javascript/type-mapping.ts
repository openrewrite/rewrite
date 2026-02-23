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
import FUNCTION_TYPE_NAME = Type.FUNCTION_TYPE_NAME;

export class JavaScriptTypeMapping {
    // Primary cache: Use type signatures (preferring type.id) as cache keys
    // TypeScript assigns stable IDs to all types, so we don't need secondary caches
    private readonly typeCache: Map<string | number, Type> = new Map();
    private readonly methodCache: Map<string, Type.Method> = new Map();

    private readonly regExpSymbol: ts.Symbol | undefined;
    private readonly stringWrapperType: ts.Type | undefined;
    private readonly numberWrapperType: ts.Type | undefined;
    private readonly booleanWrapperType: ts.Type | undefined;


    constructor(
        private readonly checker: ts.TypeChecker
    ) {
        this.regExpSymbol = checker.resolveName(
            "RegExp",
            undefined,
            ts.SymbolFlags.Type,
            false
        );

        // Resolve global wrapper types for primitives from TypeScript's lib
        const stringSymbol = checker.resolveName("String", undefined, ts.SymbolFlags.Type, false);
        const numberSymbol = checker.resolveName("Number", undefined, ts.SymbolFlags.Type, false);
        const booleanSymbol = checker.resolveName("Boolean", undefined, ts.SymbolFlags.Type, false);

        // Store the TypeScript types; conversion to Type happens on-demand
        if (stringSymbol) {
            this.stringWrapperType = checker.getDeclaredTypeOfSymbol(stringSymbol);
        }

        if (numberSymbol) {
            this.numberWrapperType = checker.getDeclaredTypeOfSymbol(numberSymbol);
        }

        if (booleanSymbol) {
            this.booleanWrapperType = checker.getDeclaredTypeOfSymbol(booleanSymbol);
        }
    }

    type(node: ts.Node): Type | undefined {
        // For identifiers, check if this references a variable
        // This enables fieldType attribution for variable references
        if (ts.isIdentifier(node)) {
            const variableType = this.variableType(node);
            if (variableType) {
                return variableType;
            }
            // Fall through to regular type checking if not a variable
        }

        let type: ts.Type | undefined;
        if (ts.isExpression(node)) {
            type = this.checker.getTypeAtLocation(node);
        } else if (ts.isTypeNode(node)) {
            type = this.checker.getTypeFromTypeNode(node);
        }
        return type && this.getType(type);
    }

    private getType(type: ts.Type): Type {
        // Check for error types first - these indicate type-checking failures
        // and should not be processed further
        if (type.flags & ts.TypeFlags.Any) {
            const intrinsicName = (type as any).intrinsicName;
            if (intrinsicName === 'error') {
                return Type.unknownType;
            }
        }

        // Skip problematic type constructs EARLY - before any caching or recursion
        // These can cause deep recursion and are often computed/structural types
        if (type.flags & ts.TypeFlags.Object) {
            const objectFlags = (type as ts.ObjectType).objectFlags;
            // Always skip mapped types (e.g., { [K in keyof T]: ... })
            // These are structural/computed types that can cause infinite recursion
            if (objectFlags & ts.ObjectFlags.Mapped) {
                return Type.unknownType;
            }
            // Skip instantiated types ONLY if they're not type references
            // Type references like Array<string>, Promise<T> are instantiated but should be mapped
            // Other instantiated types (like object literals) should return unknown
            if (objectFlags & ts.ObjectFlags.Instantiated) {
                const isTypeReference = objectFlags & ts.ObjectFlags.Reference;
                if (!isTypeReference) {
                    return Type.unknownType;
                }
            }
        }
        // Always skip conditional types (T extends U ? X : Y)
        if (type.flags & ts.TypeFlags.Conditional) {
            return Type.unknownType;
        }

        // Check cache using signature (type.id when available)
        const signature = this.getSignature(type);
        const existing = this.typeCache.get(signature);
        if (existing) {
            return existing;
        }

        // TypeScript represents `boolean` as a union of `false | true`, but the union
        // type still has the Boolean flag set. Check this early to return Primitive.Boolean
        // before we process it as a generic union type.
        if (type.flags & ts.TypeFlags.Boolean) {
            this.typeCache.set(signature, Type.Primitive.Boolean);
            return Type.Primitive.Boolean;
        }

        // Get symbol for later use in type detection
        const symbol = type.getSymbol?.();

        // IMPORTANT: Check if this is a type reference to a parameterized type FIRST
        if (type.flags & ts.TypeFlags.Object) {
            const objectType = type as ts.ObjectType;
            const isTypeReference = objectType.objectFlags & ts.ObjectFlags.Reference;

            if (isTypeReference) {
                const typeRef = type as ts.TypeReference;
                const hasTypeArgs = typeRef.typeArguments && typeRef.typeArguments.length > 0;

                if (hasTypeArgs) {
                    // This is a parameterized type reference (e.g., RefObject<HTMLButtonElement>)
                    // Extract the base class type and type arguments to create a Parameterized type

                    // IMPORTANT: Check if the type arguments are actually type parameters (unsubstituted)
                    // This happens when TypeScript expands type aliases but doesn't substitute the type parameters
                    // For example, React.Ref<HTMLButtonElement> expands to RefObject<T> | RefCallback<T> | null
                    // instead of RefObject<HTMLButtonElement> | RefCallback<HTMLButtonElement> | null
                    const hasUnsubstitutedTypeParams = typeRef.typeArguments!.some((arg: any) =>
                        (arg as ts.Type).flags & ts.TypeFlags.TypeParameter
                    );

                    if (!hasUnsubstitutedTypeParams) {
                        // Only create parameterized type if type arguments are actual types, not type parameters

                        if (symbol && (symbol.flags & (ts.SymbolFlags.Class | ts.SymbolFlags.Interface | ts.SymbolFlags.TypeAlias))) {
                            const declaredType = this.checker.getDeclaredTypeOfSymbol(symbol);

                            // Get or create the base class type (this gets cached)
                            const declaredSig = this.getSignature(declaredType);
                            let classType = this.typeCache.get(declaredSig) as Type.Class | undefined;
                            if (!classType) {
                                classType = this.createEmptyClassType(declaredType);
                                this.typeCache.set(declaredSig, classType);
                                this.populateClassType(classType, declaredType);
                            }

                            // Resolve type arguments
                            const typeParameters: Type[] = [];
                            for (const typeArg of typeRef.typeArguments!) {
                                const resolvedArg = this.getType(typeArg as ts.Type);
                                typeParameters.push(resolvedArg);
                            }

                            // Create the parameterized type wrapper
                            const parameterized = {
                                kind: Type.Kind.Parameterized,
                                type: classType,
                                typeParameters: typeParameters,
                                fullyQualifiedName: classType.fullyQualifiedName,
                                toJSON: function () {
                                    return Type.signature(this);
                                }
                            } as Type.Parameterized;

                            // Cache the parameterized type
                            this.typeCache.set(signature, parameterized);
                            return parameterized;
                        }
                    }
                }
            }
        }

        // Check for class/interface/enum types (including arrays)
        // Arrays in JavaScript are objects with methods, so we treat them as class types
        if (symbol) {
            // Check for function symbols
            if (symbol.flags & ts.SymbolFlags.Function) {
                const callSignatures = type.getCallSignatures();
                if (callSignatures.length > 0) {
                    // Shell-cache: Create stub, cache it, then populate (prevents cycles)
                    const functionType = this.createEmptyFunctionType();
                    this.typeCache.set(signature, functionType);
                    this.populateFunctionType(functionType, callSignatures[0]);
                    return functionType;
                }
            }

            // Check for function-scoped or block-scoped variables that might be functions
            if (symbol.flags & (ts.SymbolFlags.FunctionScopedVariable | ts.SymbolFlags.BlockScopedVariable)) {
                const callSignatures = type.getCallSignatures();
                if (callSignatures.length > 0) {
                    // Shell-cache: Create stub, cache it, then populate (prevents cycles)
                    const functionType = this.createEmptyFunctionType();
                    this.typeCache.set(signature, functionType);
                    this.populateFunctionType(functionType, callSignatures[0]);
                    return functionType;
                }
            }

            if (symbol.flags & ts.SymbolFlags.ValueModule) {
                const classType = this.createEmptyClassType(type);
                this.typeCache.set(signature, classType);
                this.populateClassType(classType, type);
                return classType;
            }
            if (symbol.flags & (ts.SymbolFlags.Class | ts.SymbolFlags.Interface | ts.SymbolFlags.Enum | ts.SymbolFlags.TypeAlias | ts.SymbolFlags.ValueModule)) {
                // Get the declared type from the symbol (analogous to symType = sym.type in Java)
                // This is the base class type without specific type arguments
                const declaredType = this.checker.getDeclaredTypeOfSymbol(symbol);
                const declaredSig = this.getSignature(declaredType);

                let classType = this.typeCache.get(declaredSig) as Type.Class | undefined;

                if (!classType) {
                    // Shell-cache: Create stub, cache it, then populate (prevents cycles)
                    classType = this.createEmptyClassType(declaredType);
                    this.typeCache.set(declaredSig, classType);
                    this.populateClassType(classType, declaredType);
                }

                // Return the base class type (parameterized types are handled at the beginning of getType)
                return classType;
            }

            if (symbol.flags & ts.SymbolFlags.TypeParameter) {
                return this.createGenericTypeVariable(type as ts.TypeParameter, signature);
            }
        }

        // Note on type aliases with type arguments (e.g., React.Ref<HTMLButtonElement>):
        // TypeScript automatically substitutes type parameters when resolving type aliases.
        // For example, React.Ref<T> = RefCallback<T> | RefObject<T> | null
        // When we encounter Ref<HTMLButtonElement>, TypeScript gives us:
        //   RefCallback<HTMLButtonElement> | RefObject<HTMLButtonElement> | null
        // The type parameters are already correctly substituted in the constituent types,
        // so we can just process the resolved type normally (falling through to union handling below).

        // Check for union types (e.g., string | number)
        if (type.flags & ts.TypeFlags.Union) {
            return this.createUnionType(type as ts.UnionType, signature);
        }

        // Check for intersection types (e.g., A & B)
        if (type.flags & ts.TypeFlags.Intersection) {
            const intersectionType = type as ts.IntersectionType;
            return this.createIntersectionType(intersectionType, signature);
        }

        // Check for function types without symbols (anonymous functions, function types)
        const callSignatures = type.getCallSignatures();
        if (callSignatures && callSignatures.length > 0) {
            // Shell-cache: Create stub, cache it, then populate (prevents cycles)
            const functionType = this.createEmptyFunctionType();
            this.typeCache.set(signature, functionType);
            this.populateFunctionType(functionType, callSignatures[0]);
            return functionType;
        }

        // For anonymous object types that could have circular references
        if (type.flags & ts.TypeFlags.Object) {
            const objectFlags = (type as ts.ObjectType).objectFlags;
            if (objectFlags & ts.ObjectFlags.Anonymous) {
                return Type.unknownType;
            }
        }

        // For non-object types, we can create them directly without recursion concerns
        const result = this.createPrimitiveOrUnknownType(type);
        this.typeCache.set(signature, result);
        return result;
    }

    private getSignature(type: ts.Type): string | number {
        // Try to use TypeScript's internal id if available
        // TypeScript assigns stable IDs to all types, including parameterized, union, and intersection types
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
        if (Type.isClass(type) && type.fullyQualifiedName === 'RegExp') {
            return Type.Primitive.String;
        }
        return Type.isPrimitive(type) ? type : Type.Primitive.None;
    }

    variableType(node: ts.Node): Type.Variable | undefined {
        let symbol: ts.Symbol | undefined;
        let location: ts.Node = node;

        // Get the symbol depending on node type
        if (ts.isVariableDeclaration(node)) {
            symbol = this.checker.getSymbolAtLocation(node.name);
        } else if (ts.isParameter(node)) {
            symbol = this.checker.getSymbolAtLocation(node.name);
        } else if (ts.isIdentifier(node)) {
            // For identifier references (like 'vi' in 'vi.fn()')
            symbol = this.checker.getSymbolAtLocation(node);
        } else if (ts.isPropertyDeclaration(node) || ts.isPropertySignature(node)) {
            symbol = this.checker.getSymbolAtLocation(node.name);
        } else {
            // Not a variable/parameter/property we can handle
            return undefined;
        }

        if (!symbol) {
            return undefined;
        }

        // Get the variable declaration (resolve aliases if needed)
        let actualSymbol = symbol;
        if (symbol.flags & ts.SymbolFlags.Alias) {
            actualSymbol = this.checker.getAliasedSymbol(symbol);
        }

        // Check if this symbol represents a variable, parameter, or property
        // Exclude functions, classes, interfaces, namespaces, type aliases
        const isExcluded = actualSymbol.flags & (
            ts.SymbolFlags.Function |
            ts.SymbolFlags.Class |
            ts.SymbolFlags.Interface |
            ts.SymbolFlags.Enum |
            ts.SymbolFlags.ValueModule |
            ts.SymbolFlags.NamespaceModule |
            ts.SymbolFlags.TypeAlias |
            ts.SymbolFlags.TypeParameter
        );

        if (isExcluded) {
            // Not a variable - it's a type, function, class, namespace, etc.
            return undefined;
        }

        const isVariable = actualSymbol.flags & (
            ts.SymbolFlags.Variable |
            ts.SymbolFlags.Property |
            ts.SymbolFlags.FunctionScopedVariable |
            ts.SymbolFlags.BlockScopedVariable
        );

        if (!isVariable) {
            // Not a variable we recognize
            return undefined;
        }

        // Get the type of the variable
        const variableType = this.checker.getTypeOfSymbolAtLocation(actualSymbol, location);
        const mappedType = this.getType(variableType);

        // Get the owner (declaring type) for the variable
        let ownerType: Type | undefined;

        // Check if the variable is imported
        if (symbol.flags & ts.SymbolFlags.Alias) {
            // For imported variables, find the module specifier
            const declarations = symbol.declarations;
            if (declarations && declarations.length > 0) {
                let importNode: ts.Node | undefined = declarations[0];

                // Traverse up to find the ImportDeclaration
                while (importNode && !ts.isImportDeclaration(importNode)) {
                    importNode = importNode.parent;
                }

                if (importNode && ts.isImportDeclaration(importNode)) {
                    const importDecl = importNode as ts.ImportDeclaration;
                    if (ts.isStringLiteral(importDecl.moduleSpecifier)) {
                        const moduleSpecifier = importDecl.moduleSpecifier.text;
                        // Create a Type.Class representing the module
                        ownerType = {
                            kind: Type.Kind.Class,
                            flags: 0,
                            classKind: Type.Class.Kind.Interface,
                            fullyQualifiedName: moduleSpecifier,
                            typeParameters: [],
                            annotations: [],
                            interfaces: [],
                            members: [],
                            methods: [],
                            toJSON: function () {
                                return Type.signature(this);
                            }
                        } as Type.Class;
                    }
                }
            }
        } else {
            // For non-imported variables, check if they belong to a class/interface/namespace
            const parentSymbol = (actualSymbol as any).parent as ts.Symbol | undefined;
            if (parentSymbol) {
                const parentType = this.checker.getDeclaredTypeOfSymbol(parentSymbol);
                if (parentType) {
                    ownerType = this.getType(parentType);

                    // If the parent is a namespace, try to find the module it came from
                    // This handles cases like React.forwardRef where the namespace is React
                    // but the module is "react"
                    if (parentSymbol.flags & ts.SymbolFlags.ValueModule ||
                        parentSymbol.flags & ts.SymbolFlags.NamespaceModule) {
                        // Check if this namespace was imported
                        const parentDeclarations = parentSymbol.declarations;
                        if (parentDeclarations && parentDeclarations.length > 0) {
                            const firstDecl = parentDeclarations[0];
                            const sourceFile = firstDecl.getSourceFile();
                            // If it's from node_modules or a .d.ts file, try to extract the module name
                            if (sourceFile.isDeclarationFile) {
                                const fileName = sourceFile.fileName;
                                const moduleName = this.extractModuleNameFromPath(fileName);
                                if (moduleName) {
                                    // Store the module as the owningClass for now
                                    // (This is a bit of a hack, but works with the current type system)
                                    if (Type.isClass(ownerType)) {
                                        (ownerType as any).owningClass = {
                                            kind: Type.Kind.Class,
                                            flags: 0,
                                            classKind: Type.Class.Kind.Interface,
                                            fullyQualifiedName: moduleName,
                                            typeParameters: [],
                                            annotations: [],
                                            interfaces: [],
                                            members: [],
                                            methods: [],
                                            toJSON: function () {
                                                return Type.signature(this);
                                            }
                                        } as Type.Class;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Create the Type.Variable
        const variable = {
            kind: Type.Kind.Variable,
            name: actualSymbol.getName(),
            owner: ownerType,
            type: mappedType,
            annotations: [],
            toJSON: function () {
                return Type.signature(this);
            }
        } as Type.Variable;

        return variable;
    }

    /**
     * Extract the npm module name from a file path.
     * Handles various package manager layouts:
     * - Standard: /path/node_modules/react/index.d.ts -> react
     * - Scoped: /path/node_modules/@types/react/index.d.ts -> react
     * - Scoped with __ encoding: /path/node_modules/@types/testing-library__react/index.d.ts -> @testing-library/react
     * - Nested node_modules: /path/node_modules/pkg/node_modules/dep/index.d.ts -> dep
     * - pnpm: /path/node_modules/.pnpm/react@18.2.0/node_modules/react/index.d.ts -> react
     *
     * @returns The module name, or undefined if not from node_modules
     */
    private extractModuleNameFromPath(fileName: string): string | undefined {
        if (!fileName.includes('node_modules/')) {
            return undefined;
        }

        // Find the last occurrence of node_modules/ to handle nested dependencies
        // This also correctly handles pnpm's .pnpm structure
        const lastNodeModulesIndex = fileName.lastIndexOf('node_modules/');
        const afterNodeModules = fileName.substring(lastNodeModulesIndex + 'node_modules/'.length);

        // Split by '/' to get path segments
        const segments = afterNodeModules.split('/');
        if (segments.length === 0) {
            return undefined;
        }

        let moduleName: string;

        // Handle scoped packages (@scope/package)
        if (segments[0].startsWith('@') && segments.length > 1) {
            moduleName = `${segments[0]}/${segments[1]}`;
        } else {
            moduleName = segments[0];
        }

        // Skip pnpm's .pnpm directory - it contains versioned package paths
        // In pnpm, the actual package is in: .pnpm/pkg@version/node_modules/pkg
        // So we already handled this by using lastIndexOf above
        if (moduleName === '.pnpm') {
            return undefined;
        }

        // Remove @types/ prefix and decode DefinitelyTyped scoped package encoding
        // DefinitelyTyped encodes scoped packages using __ instead of /
        // Example: @types/testing-library__react -> @testing-library/react
        if (moduleName.startsWith('@types/')) {
            moduleName = moduleName.substring('@types/'.length);
            // Decode __ encoding for scoped packages
            // testing-library__react -> @testing-library/react
            if (moduleName.includes('__')) {
                const parts = moduleName.split('__');
                if (parts.length === 2) {
                    moduleName = `@${parts[0]}/${parts[1]}`;
                }
            }
        }

        return moduleName;
    }

    /**
     * Helper to create a Type.Method object from common parameters
     */
    private createMethodType(
        signature: ts.Signature,
        node: ts.MethodSignature | ts.FunctionDeclaration | ts.MethodDeclaration | ts.FunctionExpression | ts.CallExpression | ts.NewExpression,
        declaringType: Type.FullyQualified,
        name: string,
        declaredFormalTypeNames: string[] = []
    ): Type.Method {
        // Create composite cache key: declaring type + method name + signature
        // This prevents cache collisions when methods with identical signatures have different names
        // (e.g., util.isString(): any vs util.isArray(): any)
        const declaringTypeSig = Type.signature(declaringType);
        const signatureStr = this.checker.signatureToString(signature);
        const cacheKey = `${declaringTypeSig}#${name}${signatureStr}`;
        const cached = this.methodCache.get(cacheKey);
        if (cached) {
            return cached;
        }

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
        const method = {
            kind: Type.Kind.Method,
            flags: 0, // FIXME - determine flags
            declaringType: declaringType,
            name: name,
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
        } as Type.Method;

        this.methodCache.set(cacheKey, method);
        return method;
    }

    private wrapperType(declaringType: (Type.FullyQualified & Type.Primitive) | Type.FullyQualified) {
        if (declaringType === Type.Primitive.String && this.stringWrapperType) {
            return this.getType(this.stringWrapperType) as Type.FullyQualified;
        } else if ((declaringType === Type.Primitive.Double || declaringType === Type.Primitive.BigInt) && this.numberWrapperType) {
            return this.getType(this.numberWrapperType) as Type.FullyQualified;
        } else if (declaringType === Type.Primitive.Boolean && this.booleanWrapperType) {
            return this.getType(this.booleanWrapperType) as Type.FullyQualified;
        } else {
            // This should not really happen, but we'll fallback to unknown if needed
            return Type.unknownType as Type.FullyQualified;
        }
    }

    methodType(node: ts.Node): Type.Method | undefined {

        let signature: ts.Signature | undefined;
        let methodName: string;
        let declaringType: Type.FullyQualified;
        let declaredFormalTypeNames: string[] = [];

        // Handle different kinds of nodes that represent methods or method invocations
        if (ts.isCallOrNewExpression(node)) {
            // For method invocations (e.g., _.map(...))
            signature = this.checker.getResolvedSignature(node);
            if (!signature) {
                return undefined;
            }

            let symbol = this.checker.getSymbolAtLocation(node.expression);


            if (!symbol && ts.isPropertyAccessExpression(node.expression)) {
                // For property access expressions where we couldn't get a symbol,
                // try to get the symbol from the signature's declaration
                const declaration = signature?.getDeclaration();
                if (declaration) {
                    symbol = this.checker.getSymbolAtLocation(declaration);
                }

                // If still no symbol but we have a signature, we can proceed with limited info
                if (!symbol && signature) {
                    // For cases like util.isArray where the module is 'any' type
                    // We'll construct a basic method type from the signature
                    methodName = node.expression.name.getText();

                    // When there's no symbol but we have a signature, we need to work harder
                    // to find the declaring type. This happens with CommonJS require() calls
                    // where the module is typed as 'any' but methods still have signatures

                    // Try to trace back through the AST to find the require() call
                    let inferredDeclaringType: Type.FullyQualified | undefined;
                    const objExpr = node.expression.expression;

                    if (ts.isIdentifier(objExpr)) {
                        // Look for the variable declaration that assigns the require() result
                        const objSymbol = this.checker.getSymbolAtLocation(objExpr);

                        if (objSymbol && objSymbol.valueDeclaration) {
                            const valueDecl = objSymbol.valueDeclaration;
                            if (ts.isVariableDeclaration(valueDecl) && valueDecl.initializer) {
                                // Check if it's a require() call
                                if (ts.isCallExpression(valueDecl.initializer)) {
                                    const callExpr = valueDecl.initializer;
                                    if (ts.isIdentifier(callExpr.expression) &&
                                        callExpr.expression.getText() === 'require' &&
                                        callExpr.arguments.length > 0) {
                                        // Extract the module name from require('module-name')
                                        const moduleArg = callExpr.arguments[0];
                                        if (ts.isStringLiteral(moduleArg)) {
                                            const moduleName = moduleArg.text;

                                            inferredDeclaringType = {
                                                kind: Type.Kind.Class,
                                                flags: 0, // TODO - determine flags
                                                fullyQualifiedName: moduleName
                                            } as Type.FullyQualified;
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Use the inferred type or fall back to unknown
                    declaringType = inferredDeclaringType || Type.unknownType as Type.FullyQualified;

                    // Create the method type using the helper
                    return this.createMethodType(signature, node, declaringType, methodName);
                }
            }

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
                                flags: 0, // TODO - determine flags
                                fullyQualifiedName: `${importName}.${typeName}`
                            } as Type.FullyQualified;
                        } else {
                            declaringType = mappedType as Type.FullyQualified;
                        }
                    } else {
                        declaringType = mappedType as Type.FullyQualified;
                    }
                } else if (mappedType && mappedType.kind === Type.Kind.Parameterized) {
                    // For parameterized types (e.g., Array<string>, number[]), use the base class type
                    declaringType = (mappedType as Type.Parameterized).type;
                } else if (mappedType && mappedType.kind === Type.Kind.Primitive) {
                    // Box the primitive to its wrapper type
                    declaringType = this.wrapperType(mappedType as Type.Primitive);
                } else {
                    // Default to unknown if we can't determine the type
                    declaringType = Type.unknownType as Type.FullyQualified;
                }

                // For string methods like 'hello'.split(), ensure we have a proper declaring type for primitives
                if (!isImport && declaringType === Type.unknownType) {
                    // If the expression type is a primitive string, use String as declaring type
                    const typeString = this.checker.typeToString(exprType);
                    if (typeString === 'string' || exprType.flags & ts.TypeFlags.String || exprType.flags & ts.TypeFlags.StringLiteral) {
                        declaringType = this.wrapperType(Type.Primitive.String);
                    } else if (typeString === 'number' || exprType.flags & ts.TypeFlags.Number || exprType.flags & ts.TypeFlags.NumberLiteral) {
                        declaringType = this.wrapperType(Type.Primitive.Double);
                    } else if (typeString === 'boolean' || exprType.flags & ts.TypeFlags.Boolean || exprType.flags & ts.TypeFlags.BooleanLiteral) {
                        declaringType = this.wrapperType(Type.Primitive.Boolean);
                    } else {
                        // Fallback for other primitive types or unknown
                        declaringType = Type.unknownType as Type.FullyQualified;
                    }
                }

            } else if (ts.isIdentifier(node.expression)) {
                methodName = node.expression.getText();

                // Check if this is an import first
                const exprSymbol = this.checker.getSymbolAtLocation(node.expression);
                let moduleSpecifier: string | undefined;
                let aliasedSymbol: ts.Symbol | undefined;

                if (exprSymbol) {
                    // Check if this is an aliased symbol (i.e., an import)
                    if (exprSymbol.flags & ts.SymbolFlags.Alias) {
                        aliasedSymbol = this.checker.getAliasedSymbol(exprSymbol);
                    }

                    // If getAliasedSymbol returns something different, it's an import
                    if (aliasedSymbol && aliasedSymbol !== exprSymbol) {
                        // This is definitely an imported symbol
                        const aliasedParentSymbol = (aliasedSymbol as any).parent as ts.Symbol | undefined;

                        if (aliasedParentSymbol && aliasedParentSymbol.declarations?.[0] &&
                            ts.isModuleDeclaration(aliasedParentSymbol.declarations[0]) &&
                            ts.isIdentifier(aliasedParentSymbol.declarations[0].name)) {
                            // For namespace imports, use the namespace symbol's `name` as the module specifier (e.g. `React` instead of `react`)
                            moduleSpecifier = aliasedParentSymbol.name;
                        } else {
                            // Now find the import declaration to get the module specifier
                            if (exprSymbol.declarations && exprSymbol.declarations.length > 0) {
                                let importNode: ts.Node = exprSymbol.declarations[0];

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
                }

                if (moduleSpecifier) {
                    // This is an imported function - use the module specifier as declaring type
                    if (moduleSpecifier.startsWith('node:')) {
                        // Node.js built-in module
                        declaringType = {
                            kind: Type.Kind.Class,
                            flags: 0, // TODO - determine flags
                            fullyQualifiedName: 'node'
                        } as Type.FullyQualified;
                        methodName = moduleSpecifier.substring(5); // Remove 'node:' prefix
                    } else {
                        // Regular module import
                        declaringType = {
                            kind: Type.Kind.Class,
                            flags: 0, // TODO - determine flags
                            fullyQualifiedName: moduleSpecifier
                        } as Type.FullyQualified;
                        // Determine the method name based on import type
                        const isDefaultImport = exprSymbol!.declarations?.some(d =>
                            ts.isImportClause(d) || ts.isNamespaceImport(d)
                        );
                        if (isDefaultImport) {
                            methodName = '<default>';
                        } else if (aliasedSymbol && aliasedSymbol.name) {
                            methodName = aliasedSymbol.name;
                        } else {
                            methodName = '<default>';
                        }
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
                                flags: 0, // TODO - determine flags
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

        // Create the method type using the helper
        return this.createMethodType(signature, node, declaringType, methodName, declaredFormalTypeNames);
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

        // First, check if this symbol is an import/alias
        // For imported types, we want to use the module specifier instead of the file path
        if (symbol.flags & ts.SymbolFlags.Alias) {
            const aliasedSymbol = this.checker.getAliasedSymbol(symbol);
            if (aliasedSymbol && aliasedSymbol !== symbol && symbol.declarations && symbol.declarations.length > 0) {
                // Try to find the import declaration to get the module specifier
                let importNode: ts.Node | undefined = symbol.declarations[0];

                // Traverse up to find the ImportDeclaration or ImportSpecifier
                while (importNode && importNode.parent && !ts.isImportDeclaration(importNode) && !ts.isImportSpecifier(importNode)) {
                    importNode = importNode.parent;
                }

                let moduleSpecifier: string | undefined;

                if (importNode && ts.isImportSpecifier(importNode)) {
                    // Named import like: import { ClipLoader } from 'react-spinners'
                    // ImportSpecifier -> NamedImports -> ImportClause -> ImportDeclaration
                    const namedImports = importNode.parent; // NamedImports
                    if (namedImports && ts.isNamedImports(namedImports)) {
                        const importClause = namedImports.parent; // ImportClause
                        if (importClause && ts.isImportClause(importClause)) {
                            const importDecl = importClause.parent; // ImportDeclaration
                            if (importDecl && ts.isImportDeclaration(importDecl) && ts.isStringLiteral(importDecl.moduleSpecifier)) {
                                moduleSpecifier = importDecl.moduleSpecifier.text;
                            }
                        }
                    }
                } else if (importNode && ts.isImportDeclaration(importNode)) {
                    // Default or namespace import
                    if (ts.isStringLiteral(importNode.moduleSpecifier)) {
                        moduleSpecifier = importNode.moduleSpecifier.text;
                    }
                }

                if (moduleSpecifier) {
                    // Build the fully qualified name from module specifier + symbol name
                    const symbolName = symbol.getName();
                    return `${moduleSpecifier}.${symbolName}`;
                }
            }
        }

        // Fall back to TypeScript's built-in getFullyQualifiedName
        // This returns names with quotes that we need to clean up
        // e.g., '"React"."Component"' -> 'React.Component'
        const tsQualifiedName = this.checker.getFullyQualifiedName(symbol);
        let cleanedName = tsQualifiedName.replace(/"/g, '');

        // Check if this is a file path from node_modules (happens with some packages)
        // TypeScript sometimes returns full paths instead of module names
        if (cleanedName.includes('node_modules/')) {
            // Extract the module name from the path
            // Example: /private/var/.../node_modules/react-spinners/src/index.ClipLoader
            // Should become: react-spinners.ClipLoader
            const nodeModulesIndex = cleanedName.indexOf('node_modules/');
            const afterNodeModules = cleanedName.substring(nodeModulesIndex + 'node_modules/'.length);

            // Split by '/' to get parts of the path
            const pathParts = afterNodeModules.split('/');

            if (pathParts.length > 0) {
                // First part is the package name (might be scoped like @types)
                let packageName = pathParts[0];

                // Handle scoped packages
                if (packageName.startsWith('@') && pathParts.length > 1) {
                    packageName = `${packageName}/${pathParts[1]}`;
                }

                // Find the symbol name (everything after the last dot in the original cleaned name)
                const lastDotIndex = cleanedName.lastIndexOf('.');
                if (lastDotIndex > 0) {
                    const symbolName = cleanedName.substring(lastDotIndex + 1);
                    cleanedName = `${packageName}.${symbolName}`;
                } else {
                    cleanedName = packageName;
                }
            }
        }

        return cleanedName.endsWith('Constructor') ?
            cleanedName.substring(0, cleanedName.length - 'Constructor'.length) :
            cleanedName;
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
        return {
            kind: Type.Kind.Class,
            flags: 0, // TODO - determine flags
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
        } as Type.Class;
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
                    // Handle both Class and Parameterized (e.g., Component<Props>)
                    if (Type.isClass(firstBase)) {
                        (classType as any).supertype = firstBase;
                    } else if (Type.isParameterized(firstBase)) {
                        // For parameterized types, use the base class as the supertype
                        (classType as any).supertype = (firstBase as Type.Parameterized).type;
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
                const variable: Type.Variable = {
                    kind: Type.Kind.Variable,
                    name: prop.getName(),
                    owner: classType,  // Cyclic reference to the containing class (already in cache)
                    type: this.getType(propType), // This will find classType in cache if it's recursive
                    annotations: [],
                    toJSON: function () {
                        return Type.signature(this);
                    }
                } as Type.Variable;
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
        // Note: Using bitwise & instead of === for robustness, as TypeScript may assign
        // multiple flags to a single type (e.g., Boolean + Union)
        if (type.flags & ts.TypeFlags.Null) {
            return Type.Primitive.Null;
        } else if (type.flags & ts.TypeFlags.Undefined) {
            return Type.Primitive.None;
        } else if (type.flags & (ts.TypeFlags.Number | ts.TypeFlags.NumberLiteral | ts.TypeFlags.NumberLike)) {
            return Type.Primitive.Double;
        } else if (type.flags & (ts.TypeFlags.String | ts.TypeFlags.StringLiteral | ts.TypeFlags.StringLike)) {
            return Type.Primitive.String;
        } else if (type.flags & ts.TypeFlags.Void) {
            return Type.Primitive.Void;
        } else if (type.flags & (ts.TypeFlags.BigInt | ts.TypeFlags.BigIntLiteral | ts.TypeFlags.BigIntLike)) {
            return Type.Primitive.BigInt;
        } else if (type.symbol !== undefined && type.symbol === this.regExpSymbol) {
            return Type.Primitive.String;
        } else if (type.flags & (ts.TypeFlags.Boolean | ts.TypeFlags.BooleanLiteral | ts.TypeFlags.BooleanLike)) {
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

    /**
     * Create a union type from TypeScript union type (e.g., string | number)
     * Note: Cache check is done in getType() before calling this method
     */
    private createUnionType(unionType: ts.UnionType, cacheKey: string | number): Type.Union {
        // Shell-cache FIRST to prevent infinite recursion (before resolving constituent types)
        const union = {
            kind: Type.Kind.Union,
            bounds: []
        } as Type.Union;

        this.typeCache.set(cacheKey, union);

        // Now map all constituent types (may recursively reference this union)
        const bounds: Type[] = [];
        for (const constituentType of unionType.types) {
            bounds.push(this.getType(constituentType));
        }

        // Update the bounds in the union we created
        (union as any).bounds = bounds;

        return union;
    }

    /**
     * Create an intersection type from TypeScript intersection type (e.g., A & B)
     * Note: Cache check is done in getType() before calling this method
     */
    private createIntersectionType(intersectionType: ts.IntersectionType, cacheKey: string | number): Type.Intersection {
        // Shell-cache FIRST to prevent infinite recursion (before resolving constituent types)
        const intersection = {
            kind: Type.Kind.Intersection,
            bounds: []
        } as Type.Intersection;

        this.typeCache.set(cacheKey, intersection);

        // Now map all constituent types (may recursively reference this intersection)
        const bounds: Type[] = [];
        for (const constituentType of intersectionType.types) {
            bounds.push(this.getType(constituentType));
        }

        // Update the bounds in the intersection we created
        (intersection as any).bounds = bounds;

        return intersection;
    }

    /**
     * Create a generic type variable from a TypeScript type parameter.
     * Examples: T, K extends string, V extends keyof T
     * Note: Cache check is done in getType() before calling this method
     */
    private createGenericTypeVariable(typeParam: ts.TypeParameter, cacheKey: string | number): Type.GenericTypeVariable {
        const symbol = typeParam.getSymbol();
        const name = symbol ? symbol.getName() : '?';

        // Shell-cache: Create stub, cache it, then populate (prevents cycles)
        const gtv = {
            kind: Type.Kind.GenericTypeVariable,
            name: name,
            variance: Type.GenericTypeVariable.Variance.Invariant,
            bounds: []
        } as Type.GenericTypeVariable;

        this.typeCache.set(cacheKey, gtv);

        // Get the constraint (upper bound) if it exists
        const constraint = typeParam.getConstraint();
        let bounds: Type[] = [];
        let variance = Type.GenericTypeVariable.Variance.Invariant;

        if (constraint) {
            const boundType = this.getType(constraint);
            // Only add bounds if it's not just "object" (the default constraint)
            if (!(Type.isClass(boundType) && boundType.fullyQualifiedName === 'object')) {
                bounds = [boundType];
                variance = Type.GenericTypeVariable.Variance.Covariant;
            }
        }

        // Update the variance and bounds
        (gtv as any).variance = variance;
        (gtv as any).bounds = bounds;

        return gtv;
    }

    /**
     * Create an empty function type shell with FQN 𝑓.
     * The shell will be populated later to handle circular references.
     */
    private createEmptyFunctionType(): Type.Class {
        return {
            kind: Type.Kind.Class,
            flags: 0,
            classKind: Type.Class.Kind.Interface,
            fullyQualifiedName: FUNCTION_TYPE_NAME,
            typeParameters: [],
            annotations: [],
            interfaces: [],
            members: [],
            methods: [],
            toJSON: function () {
                return Type.signature(this);
            }
        } as Type.Class;
    }

    /**
     * Populate a function type with signature information.
     * The function type has generic type parameters for return type (first) and parameter types (subsequent),
     * and contains an apply() method with the matching signature.
     * Since the shell is already in the cache, any recursive references will find it.
     */
    private populateFunctionType(functionClass: Type.Class, signature: ts.Signature): void {
        const returnType = this.getType(signature.getReturnType());
        const parameters = signature.getParameters();
        const parameterTypes: Type[] = [];
        const parameterNames: string[] = [];

        // Get parameter types
        for (const param of parameters) {
            const declaration = param.valueDeclaration || param.declarations?.[0];
            if (declaration) {
                const paramType = this.checker.getTypeOfSymbolAtLocation(param, declaration);
                parameterTypes.push(this.getType(paramType));
                parameterNames.push(param.getName());
            }
        }

        // Build the type parameters list with proper variance:
        // - Return type is covariant (R)
        // - Parameter types are contravariant (P1, P2, ...)
        const typeParameters: Type[] = [];

        // Return type parameter (covariant)
        typeParameters.push({
            kind: Type.Kind.GenericTypeVariable,
            name: 'R',
            variance: Type.GenericTypeVariable.Variance.Covariant,
            bounds: [returnType]
        } as Type.GenericTypeVariable);

        // Parameter type variables (contravariant)
        parameterTypes.forEach((paramType, index) => {
            typeParameters.push({
                kind: Type.Kind.GenericTypeVariable,
                name: `P${index + 1}`,
                variance: Type.GenericTypeVariable.Variance.Contravariant,
                bounds: [paramType]
            } as Type.GenericTypeVariable);
        });

        functionClass.typeParameters = typeParameters;

        // Create the apply() method
        const applyMethod = {
            kind: Type.Kind.Method,
            flags: 0,
            declaringType: functionClass,
            name: 'apply',
            returnType: returnType,
            parameterNames: parameterNames,
            parameterTypes: parameterTypes,
            thrownExceptions: [],
            annotations: [],
            defaultValue: undefined,
            declaredFormalTypeNames: [],
            toJSON: function () {
                return Type.signature(this);
            }
        } as Type.Method;

        // Add the apply method to the function class
        functionClass.methods.push(applyMethod);
    }
}
