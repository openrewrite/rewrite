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
import * as ts from "typescript";
import {JavaType} from "../java";
import {createDraft, Draft, finishDraft} from "immer";
import {asRef, RpcCodecs, RpcReceiveQueue, RpcSendQueue} from "../rpc";
import {MarkersKind, ParseExceptionResult} from "../markers";

export class JavaScriptTypeMapping {
    private readonly typeCache: Map<string | number, JavaType> = new Map();
    private readonly regExpSymbol: ts.Symbol | undefined;
    private readonly projectRoot: string;

    constructor(
        private readonly checker: ts.TypeChecker,
        projectRoot?: string
    ) {
        this.regExpSymbol = checker.resolveName(
            "RegExp",
            undefined,
            ts.SymbolFlags.Type,
            false
        );
        // Default to current working directory if not provided
        this.projectRoot = projectRoot || process.cwd();
    }

    type(node: ts.Node): JavaType | undefined {
        let type: ts.Type | undefined;
        if (ts.isExpression(node)) {
            type = this.checker.getTypeAtLocation(node);
        } else if (ts.isTypeNode(node)) {
            type = this.checker.getTypeFromTypeNode(node);
        }
        return type && this.getType(type);
    }

    private getType(type: ts.Type) {
        const signature = this.getSignature(type);
        const existing = this.typeCache.get(signature);
        if (existing) {
            return existing;
        }
        const result = this.createType(type, signature);
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

    primitiveType(node: ts.Node): JavaType.Primitive {
        const type = this.type(node);
        return JavaType.isPrimitive(type) ? type : JavaType.Primitive.None;
    }

    variableType(node: ts.NamedDeclaration): JavaType.Variable | undefined {
        if (ts.isVariableDeclaration(node)) {
            const symbol = this.checker.getSymbolAtLocation(node.name);
            if (symbol) {
                const type = this.checker.getTypeOfSymbolAtLocation(symbol, node);
            }
        }
        return undefined;
    }

    methodType(node: ts.Node): JavaType.Method | undefined {
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

        const declaration = symbol.valueDeclaration || symbol.declarations?.[0];
        if (!declaration) {
            // No declaration - might be a built-in or synthetic type
            const typeName = symbol.getName();
            if (this.isBuiltInType(typeName)) {
                return `lib.${typeName}`;
            }
            return typeName;
        }

        const sourceFile = declaration.getSourceFile();
        const fileName = sourceFile.fileName;

        // Check if this is from an external module (node_modules or .d.ts)
        if (sourceFile.isDeclarationFile || fileName.includes("node_modules")) {
            const packageName = this.extractPackageName(fileName);
            if (packageName) {
                return `${packageName}.${symbol.getName()}`;
            }
        }

        // For local files, use relative path from project root
        const relativePath = this.getRelativeModulePath(fileName);
        return `${relativePath}.${symbol.getName()}`;
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
     * Check if a type name is a built-in TypeScript/JavaScript type.
     */
    private isBuiltInType(typeName: string): boolean {
        const builtInTypes = new Set([
            'Array', 'Object', 'Function', 'String', 'Number', 'Boolean',
            'Date', 'RegExp', 'Error', 'Promise', 'Map', 'Set', 'WeakMap',
            'WeakSet', 'Symbol', 'BigInt', 'HTMLElement', 'Document',
            'Window', 'Console', 'JSON', 'Math', 'Reflect', 'Proxy'
        ]);
        return builtInTypes.has(typeName);
    }

    private createType(type: ts.Type, cacheKey: string | number): JavaType {
        const signature = this.checker.typeToString(type);
        if (type.isLiteral()) {
            if (type.isNumberLiteral()) {
                return JavaType.Primitive.Double;
            } else if (type.isStringLiteral()) {
                return JavaType.Primitive.String;
            }
        }

        if (type.flags === ts.TypeFlags.Null) {
            return JavaType.Primitive.Null;
        } else if (type.flags === ts.TypeFlags.Undefined) {
            return JavaType.Primitive.None;
        } else if (
            type.flags === ts.TypeFlags.Number ||
            type.flags === ts.TypeFlags.NumberLiteral ||
            type.flags === ts.TypeFlags.NumberLike
        ) {
            return JavaType.Primitive.Double;
        } else if (
            type.flags === ts.TypeFlags.String ||
            type.flags === ts.TypeFlags.StringLiteral ||
            type.flags === ts.TypeFlags.StringLike
        ) {
            return JavaType.Primitive.String;
        } else if (type.flags === ts.TypeFlags.Void) {
            return JavaType.Primitive.Void;
        } else if (
            type.flags === ts.TypeFlags.BigInt ||
            type.flags === ts.TypeFlags.BigIntLiteral ||
            type.flags === ts.TypeFlags.BigIntLike
        ) {
            return JavaType.Primitive.Long;
        } else if (
            (type.symbol !== undefined && type.symbol === this.regExpSymbol) ||
            this.checker.typeToString(type) === "RegExp"
        ) {
            return JavaType.Primitive.String;
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
            return JavaType.Primitive.Boolean;
        }

        // if (ts.isRegularExpressionLiteral(node)) {
        //     return JavaType.Primitive.String;
        // }

        return JavaType.unknownType;
    }
}

RpcCodecs.registerCodec(JavaType.Kind.Primitive, {
    async rpcSend(after: JavaType.Primitive, q: RpcSendQueue): Promise<void> {
        await q.getAndSend(after, p => p.keyword);
    },
    async rpcReceive(before: JavaType.Primitive, q: RpcReceiveQueue): Promise<JavaType.Primitive> {
        const keyword: string = await q.receive(before.keyword);
        return JavaType.Primitive.fromKeyword(keyword)!;
    }
});
