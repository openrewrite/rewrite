import * as ts from "typescript";
import {JavaType} from "../java";
import {Draft} from "immer";
import {asRef} from "../rpc";

export class JavaScriptTypeMapping {
    private readonly typeCache: Map<string, JavaType> = new Map();
    private readonly regExpSymbol: ts.Symbol | undefined;

    constructor(private readonly checker: ts.TypeChecker) {
        this.regExpSymbol = checker.resolveName(
            "RegExp",
            undefined,
            ts.SymbolFlags.Type,
            false
        );
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

    private getSignature(type: ts.Type) {
        // FIXME for classes we need to include the containing module / package in the signature and probably include in the qualified name
        return this.checker.typeToString(type);
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

    private createType(type: ts.Type, signature: string): JavaType {
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

        if (type.isUnion()) {
            let result: Draft<JavaType.Union> = asRef({
                kind: JavaType.Kind.Union,
                bounds: []
            });
            this.typeCache.set(signature, result);

            result.bounds = type.types.map(t => this.getType(t));
            return result;
        } else if (type.isClass()) {
            // FIXME flags
            let result = asRef({
                kind: JavaType.Kind.Class,
                flags: 0,
                name: type.symbol.name
            } as JavaType);
            this.typeCache.set(signature, result);
            // FIXME unsafeSet
            return result;
        } else if (type.flags & ts.TypeFlags.UniqueESSymbol) {
            let result = {
                kind: JavaType.Kind.UniqueSymbol,
            } as JavaType.UniqueSymbol;
            this.typeCache.set(signature, result);
            return result;
        }

        // if (ts.isRegularExpressionLiteral(node)) {
        //     return JavaType.Primitive.String;
        // }

        return JavaType.unknownType;
    }
}
