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
import {asRef, RpcCodecs, RpcReceiveQueue, RpcSendQueue} from "../rpc";

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

        // Set a placeholder to prevent infinite recursion during type creation
        // This is crucial for types with cyclic references (e.g., DOM types)
        this.typeCache.set(signature, Type.unknownType);

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

        const classType: Type.Class = {
            kind: Type.Kind.Class,
            classKind: kind,
            fullyQualifiedName: fullyQualifiedName,
            typeParameters: typeParameters,
            supertype: supertype,
            annotations: [],
            interfaces: interfaces,
            members: [],  // Will be populated below
            methods: methods
        };

        // Get members (properties)
        const properties = this.checker.getPropertiesOfType(type);
        for (const prop of properties) {
            // Skip methods for now (will be handled in Phase 3)
            if (!(prop.flags & ts.SymbolFlags.Method)) {
                const propType = this.checker.getTypeOfSymbolAtLocation(prop, prop.valueDeclaration || prop.declarations![0]);
                const variable: Type.Variable = {
                    kind: Type.Kind.Variable,
                    name: prop.getName(),
                    owner: classType,  // Cyclic reference to the containing class
                    type: this.getType(propType),
                    annotations: []
                };
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

    /**
     * Create a JavaType.Class for anonymous object types.
     */
    private createAnonymousClassType(type: ts.Type): Type.Class {
        // Generate a name for the anonymous type
        const typeString = this.checker.typeToString(type);
        const fullyQualifiedName = `<anonymous>${typeString}`;

        // Create initial class type with asRef for cyclic references
        const classType: Type.Class = asRef({
            kind: Type.Kind.Class,
            classKind: Type.Class.Kind.Interface, // Treat anonymous objects as interfaces
            fullyQualifiedName: fullyQualifiedName,
            typeParameters: [],
            annotations: [],
            interfaces: [],
            members: [],
            methods: []
        });

        // Get properties of the anonymous type
        const properties = this.checker.getPropertiesOfType(type);
        for (const prop of properties) {
            if (!(prop.flags & ts.SymbolFlags.Method)) {
                const propType = this.checker.getTypeOfSymbolAtLocation(prop, prop.valueDeclaration || prop.declarations![0]);
                const variable: Type.Variable = asRef({
                    kind: Type.Kind.Variable,
                    name: prop.getName(),
                    owner: classType,  // Cyclic reference to the containing class
                    type: this.getType(propType),
                    annotations: []
                });
                classType.members.push(variable);
            }
        }

        return classType;
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

        // Check for object literal types
        // noinspection JSBitwiseOperatorUsage
        if (type.flags & ts.TypeFlags.Object) {
            const objectFlags = (type as ts.ObjectType).objectFlags;
            if (objectFlags & ts.ObjectFlags.Anonymous) {
                // Anonymous object type - create a synthetic class
                return this.createAnonymousClassType(type);
            }
        }

        return Type.unknownType;
    }
}

RpcCodecs.registerCodec(Type.Kind.Primitive, {
    async rpcSend(after: Type.Primitive, q: RpcSendQueue): Promise<void> {
        await q.getAndSend(after, p => p.keyword);
    },
    async rpcReceive(before: Type.Primitive, q: RpcReceiveQueue): Promise<Type.Primitive> {
        const keyword: string = await q.receive(before.keyword);
        return Type.Primitive.fromKeyword(keyword)!;
    }
});

RpcCodecs.registerCodec(Type.Kind.Class, {
    async rpcSend(after: Type.Class, q: RpcSendQueue): Promise<void> {
        await q.getAndSend(after, c => c.classKind);
        await q.getAndSend(after, c => c.fullyQualifiedName);
        await q.getAndSend(after, c => c.typeParameters);
        await q.getAndSend(after, c => c.supertype);
        await q.getAndSend(after, c => c.annotations);
        await q.getAndSend(after, c => c.interfaces);
        await q.getAndSend(after, c => c.members);
        await q.getAndSend(after, c => c.methods);
    },
    async rpcReceive(before: Type.Class, q: RpcReceiveQueue): Promise<Type.Class> {
        const classKind = await q.receive(before.classKind);
        const fullyQualifiedName = await q.receive(before.fullyQualifiedName);
        const typeParameters = await q.receive(before.typeParameters);
        const supertype = await q.receive(before.supertype);
        const annotations = await q.receive(before.annotations);
        const interfaces = await q.receive(before.interfaces);
        const members = await q.receive(before.members);
        const methods = await q.receive(before.methods);

        return asRef({
            kind: Type.Kind.Class,
            classKind,
            fullyQualifiedName,
            typeParameters,
            supertype,
            annotations,
            interfaces,
            members,
            methods
        });
    }
});

RpcCodecs.registerCodec(Type.Kind.Variable, {
    async rpcSend(after: Type.Variable, q: RpcSendQueue): Promise<void> {
        await q.getAndSend(after, v => v.name);
        await q.getAndSend(after, v => v.owner);
        await q.getAndSend(after, v => v.type);
        // FIXME we need to use getAndSendList here but need to think about what to use for the ID
        await q.getAndSend(after, v => v.annotations);
    },
    async rpcReceive(before: Type.Variable, q: RpcReceiveQueue): Promise<Type.Variable> {
        const name = await q.receive(before.name);
        const owner = await q.receive(before.owner);
        const type = await q.receive(before.type);
        // FIXME we need to use receiveList here but need to think about what to use for the ID
        const annotations = await q.receive(before.annotations);

        return asRef({
            kind: Type.Kind.Variable,
            name,
            owner,
            type,
            annotations
        });
    }
});

RpcCodecs.registerCodec(Type.Kind.Annotation, {
    async rpcSend(after: Type.Annotation, q: RpcSendQueue): Promise<void> {
        await q.getAndSend(after, a => a.type);
        await q.getAndSend(after, a => a.values);
    },
    async rpcReceive(before: Type.Annotation, q: RpcReceiveQueue): Promise<Type.Annotation> {
        const type = await q.receive(before.type);
        const values = await q.receive(before.values);

        return asRef({
            kind: Type.Kind.Annotation,
            type,
            values
        });
    }
});
