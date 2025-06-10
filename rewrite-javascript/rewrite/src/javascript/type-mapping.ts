import * as ts from "typescript";
import {JavaType} from "../java";
import {Draft} from "immer";

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
            let result: Draft<JavaType.Union> = {
                kind: JavaType.Kind.Union,
                bounds: []
            };
            this.typeCache.set(signature, result);

            result.bounds = type.types.map(t => this.getType(t));
            return result;
        } else if (type.flags & ts.TypeFlags.UniqueESSymbol) {
            let result = {
                kind: JavaType.Kind.UniqueSymbol,
            } as JavaType.UniqueSymbol;
            this.typeCache.set(signature, result);
            return result;
        } else if (type.flags & ts.TypeFlags.Object) {
            const objectType = type as ts.ObjectType;
            if (objectType.isClassOrInterface()) {
                let result = {
                    kind: JavaType.Kind.Class,
                    classKind: type.isClass() ? JavaType.Class.Kind.Class : JavaType.Class.Kind.Interface, // TODO there are other options, no?
                    fullyQualifiedName: "missing", // TODO
                    typeParameters: [], // TODO
                    annotations: [], // TODO
                    interfaces: [], // TODO
                    members: [],
                    methods: []
                } as Draft<JavaType.Class>;
                this.typeCache.set(signature, result);
                objectType.getProperties().forEach(symbol => {
                    const memberType = this.checker.getTypeOfSymbol(symbol);
                    const callSignatures = memberType.getCallSignatures();
                    if ((memberType.flags & ts.TypeFlags.Object) && callSignatures.length > 0) {
                        const signature = callSignatures[0]; // TODO understand multiple signatures, maybe all signatures should be added as separate methods?
                        result.methods.push({
                            kind: JavaType.Kind.Method,
                            declaringType: result,
                            name: symbol.getName(),
                            returnType: this.getType(signature.getReturnType()),
                            parameterNames: signature.parameters.map(s => s.getName()),
                            parameterTypes: signature.parameters.map(s => this.getType(this.checker.getTypeOfSymbol(s))),
                            thrownExceptions: [],
                            annotations: [],
                            defaultValue: [], // TODO
                            declaredFormalTypeNames: [] // TODO
                        } as JavaType.Method);
                    } else {
                        result.members.push({
                            kind: JavaType.Kind.Variable,
                            name: symbol.getName(),
                            owner: result,
                            type: this.getType(memberType),
                            annotations: []
                        } as JavaType.Variable);
                    }
                });
                return result;
            } else if (objectType.getCallSignatures().length > 0) {
                const callSignature = objectType.getCallSignatures()[0]; // TODO handle multiple signatures
                const result = {
                    kind: JavaType.Kind.Method,
                    declaringType: JavaType.unknownType, // TODO
                    name: objectType.getSymbol()?.getName(),
                    returnType: this.getType(callSignature.getReturnType()),
                    parameterNames: callSignature.parameters.map(s => s.getName()),
                    parameterTypes: callSignature.parameters.map(s => this.getType(this.checker.getTypeOfSymbol(s))),
                    thrownExceptions: [],
                    annotations: [],
                    defaultValue: undefined, // TODO
                    declaredFormalTypeNames: []
                } as JavaType.Method;
                this.typeCache.set(signature, result);
                return result;
            }
        }

        // if (ts.isRegularExpressionLiteral(node)) {
        //     return JavaType.Primitive.String;
        // }

        return JavaType.unknownType;
    }
}
