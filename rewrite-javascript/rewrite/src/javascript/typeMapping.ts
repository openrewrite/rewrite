import * as ts from "typescript";
import { JavaType } from "../java";

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

  type(node: ts.Node): JavaType | null {
    let type: ts.Type | undefined;
    if (ts.isExpression(node)) {
      type = this.checker.getTypeAtLocation(node);
    } else if (ts.isTypeNode(node)) {
      type = this.checker.getTypeFromTypeNode(node);
    }

    return type ? this.getType(type) : null;
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
    return type instanceof JavaType.Primitive
      ? type
      : JavaType.Primitive.of(JavaType.PrimitiveKind.None);
  }

  variableType(node: ts.NamedDeclaration): JavaType.Variable | null {
    if (ts.isVariableDeclaration(node)) {
      const symbol = this.checker.getSymbolAtLocation(node.name);
      if (symbol) {
        const type = this.checker.getTypeOfSymbolAtLocation(symbol, node);
      }
    }
    return null;
  }

  methodType(node: ts.Node): JavaType.Method | null {
    return null;
  }

  private createType(type: ts.Type, signature: string): JavaType {
    if (type.isLiteral()) {
      if (type.isNumberLiteral()) {
        return JavaType.Primitive.of(JavaType.PrimitiveKind.Double);
      } else if (type.isStringLiteral()) {
        return JavaType.Primitive.of(JavaType.PrimitiveKind.String);
      }
    }

    if (type.flags === ts.TypeFlags.Null) {
      return JavaType.Primitive.of(JavaType.PrimitiveKind.Null);
    } else if (type.flags === ts.TypeFlags.Undefined) {
      return JavaType.Primitive.of(JavaType.PrimitiveKind.None);
    } else if (
      type.flags === ts.TypeFlags.Number ||
      type.flags === ts.TypeFlags.NumberLiteral ||
      type.flags === ts.TypeFlags.NumberLike
    ) {
      return JavaType.Primitive.of(JavaType.PrimitiveKind.Double);
    } else if (
      type.flags === ts.TypeFlags.String ||
      type.flags === ts.TypeFlags.StringLiteral ||
      type.flags === ts.TypeFlags.StringLike
    ) {
      return JavaType.Primitive.of(JavaType.PrimitiveKind.String);
    } else if (type.flags === ts.TypeFlags.Void) {
      return JavaType.Primitive.of(JavaType.PrimitiveKind.Void);
    } else if (
      type.flags === ts.TypeFlags.BigInt ||
      type.flags === ts.TypeFlags.BigIntLiteral ||
      type.flags === ts.TypeFlags.BigIntLike
    ) {
      return JavaType.Primitive.of(JavaType.PrimitiveKind.Long);
    } else if (
      (type.symbol !== undefined && type.symbol === this.regExpSymbol) ||
      this.checker.typeToString(type) === "RegExp"
    ) {
      return JavaType.Primitive.of(JavaType.PrimitiveKind.String);
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
      return JavaType.Primitive.of(JavaType.PrimitiveKind.Boolean);
    }

    if (type.isUnion()) {
      let result = new JavaType.Union();
      this.typeCache.set(signature, result);

      const types = type.types.map((t) => this.getType(t));
      result.unsafeSet(types);
      return result;
    } else if (type.isClass()) {
      // FIXME flags
      let result = new JavaType.Class(
        0,
        type.symbol.name,
        JavaType.Class.Kind.Class
      );
      this.typeCache.set(signature, result);
      // FIXME unsafeSet
      return result;
    }

    // if (ts.isRegularExpressionLiteral(node)) {
    //     return JavaType.Primitive.of(JavaType.PrimitiveKind.String);
    // }

    return JavaType.Unknown.INSTANCE;
  }
}
