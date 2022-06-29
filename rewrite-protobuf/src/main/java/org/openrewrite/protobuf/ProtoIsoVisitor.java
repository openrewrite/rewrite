/*
 * Copyright 2021 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.protobuf;

import org.openrewrite.protobuf.tree.Proto;

public class ProtoIsoVisitor<P> extends ProtoVisitor<P> {

    @Override
    public Proto.Block visitBlock(Proto.Block block, P p) {
        return (Proto.Block) super.visitBlock(block, p);
    }

    @Override
    public Proto.Constant visitConstant(Proto.Constant constant, P p) {
        return (Proto.Constant) super.visitConstant(constant, p);
    }

    @Override
    public Proto.Document visitDocument(Proto.Document document, P p) {
        return (Proto.Document) super.visitDocument(document, p);
    }

    @Override
    public Proto.Empty visitEmpty(Proto.Empty empty, P p) {
        return (Proto.Empty) super.visitEmpty(empty, p);
    }

    @Override
    public Proto.Enum visitEnum(Proto.Enum anEnum, P p) {
        return (Proto.Enum) super.visitEnum(anEnum, p);
    }

    @Override
    public Proto.EnumField visitEnumField(Proto.EnumField enumField, P p) {
        return (Proto.EnumField) super.visitEnumField(enumField, p);
    }

    @Override
    public Proto.ExtensionName visitExtensionName(Proto.ExtensionName extensionName, P p) {
        return (Proto.ExtensionName) super.visitExtensionName(extensionName, p);
    }

    @Override
    public Proto.Field visitField(Proto.Field field, P p) {
        return (Proto.Field) super.visitField(field, p);
    }

    @Override
    public Proto.FullIdentifier visitFullIdentifier(Proto.FullIdentifier fullIdentifier, P p) {
        return (Proto.FullIdentifier) super.visitFullIdentifier(fullIdentifier, p);
    }

    @Override
    public Proto.Identifier visitIdentifier(Proto.Identifier identifier, P p) {
        return (Proto.Identifier) super.visitIdentifier(identifier, p);
    }

    @Override
    public Proto.Import visitImport(Proto.Import anImport, P p) {
        return (Proto.Import) super.visitImport(anImport, p);
    }

    @Override
    public Proto.Keyword visitKeyword(Proto.Keyword keyword, P p) {
        return (Proto.Keyword) super.visitKeyword(keyword, p);
    }

    @Override
    public Proto.MapField visitMapField(Proto.MapField mapField, P p) {
        return (Proto.MapField) super.visitMapField(mapField, p);
    }

    @Override
    public Proto.Message visitMessage(Proto.Message message, P p) {
        return (Proto.Message) super.visitMessage(message, p);
    }

    @Override
    public Proto.OneOf visitOneOf(Proto.OneOf oneOf, P p) {
        return (Proto.OneOf) super.visitOneOf(oneOf, p);
    }

    @Override
    public Proto.Option visitOption(Proto.Option option, P p) {
        return (Proto.Option) super.visitOption(option, p);
    }

    @Override
    public Proto.OptionDeclaration visitOptionDeclaration(Proto.OptionDeclaration optionDeclaration, P p) {
        return (Proto.OptionDeclaration) super.visitOptionDeclaration(optionDeclaration, p);
    }

    @Override
    public Proto.Package visitPackage(Proto.Package aPackage, P p) {
        return (Proto.Package) super.visitPackage(aPackage, p);
    }

    @Override
    public Proto.Primitive visitPrimitive(Proto.Primitive primitive, P p) {
        return (Proto.Primitive) super.visitPrimitive(primitive, p);
    }

    @Override
    public Proto.Range visitRange(Proto.Range range, P p) {
        return (Proto.Range) super.visitRange(range, p);
    }

    @Override
    public Proto.Reserved visitReserved(Proto.Reserved reserved, P p) {
        return (Proto.Reserved) super.visitReserved(reserved, p);
    }

    @Override
    public Proto.Rpc visitRpc(Proto.Rpc rpc, P p) {
        return (Proto.Rpc) super.visitRpc(rpc, p);
    }

    @Override
    public Proto.RpcInOut visitRpcInOut(Proto.RpcInOut rpcInOut, P p) {
        return (Proto.RpcInOut) super.visitRpcInOut(rpcInOut, p);
    }

    @Override
    public Proto.Service visitService(Proto.Service service, P p) {
        return (Proto.Service) super.visitService(service, p);
    }

    @Override
    public Proto.StringLiteral visitStringLiteral(Proto.StringLiteral stringLiteral, P p) {
        return (Proto.StringLiteral) super.visitStringLiteral(stringLiteral, p);
    }

    @Override
    public Proto.Syntax visitSyntax(Proto.Syntax syntax, P p) {
        return (Proto.Syntax) super.visitSyntax(syntax, p);
    }
}
