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
package org.openrewrite.protobuf.internal;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.Markers;
import org.openrewrite.protobuf.ProtoVisitor;
import org.openrewrite.protobuf.marker.ImplicitProto2Syntax;
import org.openrewrite.protobuf.tree.*;

import java.util.List;
import java.util.function.UnaryOperator;

public class ProtoPrinter<P> extends ProtoVisitor<PrintOutputCapture<P>> {

    @Override
    public Proto visitBlock(Proto.Block block, PrintOutputCapture<P> p) {
        beforeSyntax(block, p);
        p.append('{');
        visitStatements(block.getPadding().getStatements(), p);
        visitSpace(block.getEnd(), p);
        p.append('}');
        afterSyntax(block, p);
        return block;
    }

    @Override
    public Proto visitConstant(Proto.Constant constant, PrintOutputCapture<P> p) {
        beforeSyntax(constant, p);
        p.append(constant.getValueSource());
        afterSyntax(constant, p);
        return constant;
    }

    @Override
    public Proto visitDocument(Proto.Document document, PrintOutputCapture<P> p) {
        beforeSyntax(document, p);
        visit(document.getSyntax(), p);
        visitStatements(document.getPadding().getBody(), p);
        visitSpace(document.getEof(), p);
        afterSyntax(document, p);
        return document;
    }

    @Override
    public Proto visitEmpty(Proto.Empty empty, PrintOutputCapture<P> p) {
        beforeSyntax(empty, p);
        afterSyntax(empty, p);
        return empty;
    }

    @Override
    public Proto visitEnum(Proto.Enum anEnum, PrintOutputCapture<P> p) {
        beforeSyntax(anEnum, p);
        p.append("enum");
        visit(anEnum.getName(), p);
        visit(anEnum.getBody(), p);
        afterSyntax(anEnum, p);
        return anEnum;
    }

    @Override
    public Proto visitEnumField(Proto.EnumField enumField, PrintOutputCapture<P> p) {
        beforeSyntax(enumField, p);
        visitRightPadded(enumField.getPadding().getName(), p);
        p.append('=');
        visit(enumField.getNumber(), p);
        visitContainer("[", enumField.getPadding().getOptions(), "]", p);
        afterSyntax(enumField, p);
        return enumField;
    }

    @Override
    public Proto visitExtend(Proto.Extend extend, PrintOutputCapture<P> p) {
        beforeSyntax(extend, p);
        p.append("extend");
        visitFullIdentifier(extend.getName(), p);
        visitBlock(extend.getBody(), p);
        afterSyntax(extend, p);
        return extend;
    }

    @Override
    public Proto visitExtensionName(Proto.ExtensionName extensionName, PrintOutputCapture<P> p) {
        beforeSyntax(extensionName, p);
        p.append('(');
        visitRightPadded(extensionName.getPadding().getExtension(), p);
        p.append(')');
        afterSyntax(extensionName, p);
        return extensionName;
    }

    @Override
    public Proto visitField(Proto.Field field, PrintOutputCapture<P> p) {
        beforeSyntax(field, p);
        visit(field.getLabel(), p);
        visit(field.getType(), p);
        visitRightPadded(field.getPadding().getName(), p);
        p.append('=');
        visit(field.getNumber(), p);
        visitContainer("[", field.getPadding().getOptions(), "]", p);
        afterSyntax(field, p);
        return field;
    }

    @Override
    public Proto visitFullIdentifier(Proto.FullIdentifier identifier, PrintOutputCapture<P> p) {
        beforeSyntax(identifier, p);
        visitRightPadded(identifier.getPadding().getTarget(), p);
        if (identifier.getTarget() != null) {
            p.append('.');
        }
        visit(identifier.getName(), p);
        afterSyntax(identifier, p);
        return identifier;
    }

    @Override
    public Proto visitIdentifier(Proto.Identifier identifier, PrintOutputCapture<P> p) {
        beforeSyntax(identifier, p);
        p.append(identifier.getName());
        afterSyntax(identifier, p);
        return identifier;
    }

    @Override
    public Proto visitImport(Proto.Import anImport, PrintOutputCapture<P> p) {
        beforeSyntax(anImport, p);
        p.append("import");
        visit(anImport.getModifier(), p);
        visitRightPadded(anImport.getPadding().getName(), p);
        afterSyntax(anImport, p);
        return anImport;
    }

    @Override
    public Proto visitKeyword(Proto.Keyword keyword, PrintOutputCapture<P> p) {
        beforeSyntax(keyword, p);
        p.append(keyword.getKeyword());
        afterSyntax(keyword, p);
        return keyword;
    }

    @Override
    public Proto visitMapField(Proto.MapField mapField, PrintOutputCapture<P> p) {
        beforeSyntax(mapField, p);
        p.append("map");
        visitSpace(mapField.getPadding().getMap().getAfter(), p);
        p.append('<');
        visitRightPadded(mapField.getPadding().getKeyType(), p);
        p.append(',');
        visitRightPadded(mapField.getPadding().getValueType(), p);
        p.append('>');
        visitRightPadded(mapField.getPadding().getName(), p);
        p.append('=');
        visit(mapField.getNumber(), p);
        visitContainer("[", mapField.getPadding().getOptions(), "]", p);
        afterSyntax(mapField, p);
        return mapField;
    }

    @Override
    public Proto visitMessage(Proto.Message message, PrintOutputCapture<P> p) {
        beforeSyntax(message, p);
        p.append("message");
        visit(message.getName(), p);
        visit(message.getBody(), p);
        afterSyntax(message, p);
        return message;
    }

    @Override
    public Proto visitOneOf(Proto.OneOf oneOf, PrintOutputCapture<P> p) {
        beforeSyntax(oneOf, p);
        p.append("oneof");
        visit(oneOf.getName(), p);
        visit(oneOf.getFields(), p);
        afterSyntax(oneOf, p);
        return oneOf;
    }

    @Override
    public Proto visitOption(Proto.Option option, PrintOutputCapture<P> p) {
        beforeSyntax(option, p);
        visitRightPadded(option.getPadding().getName(), p);
        p.append('=');
        visit(option.getAssignment(), p);
        afterSyntax(option, p);
        return option;
    }

    @Override
    public Proto visitOptionDeclaration(Proto.OptionDeclaration optionDeclaration, PrintOutputCapture<P> p) {
        beforeSyntax(optionDeclaration, p);
        p.append("option");
        visitRightPadded(optionDeclaration.getPadding().getName(), p);
        p.append('=');
        visit(optionDeclaration.getAssignment(), p);
        afterSyntax(optionDeclaration, p);
        return optionDeclaration;
    }

    @Override
    public Proto visitPackage(Proto.Package aPackage, PrintOutputCapture<P> p) {
        beforeSyntax(aPackage, p);
        p.append("package");
        visit(aPackage.getName(), p);
        afterSyntax(aPackage, p);
        return aPackage;
    }

    @Override
    public Proto visitPrimitive(Proto.Primitive primitive, PrintOutputCapture<P> p) {
        beforeSyntax(primitive, p);
        p.append(primitive.getType().toString().toLowerCase());
        afterSyntax(primitive, p);
        return primitive;
    }

    @Override
    public Proto visitRange(Proto.Range range, PrintOutputCapture<P> p) {
        beforeSyntax(range, p);
        visitRightPadded(range.getPadding().getFrom(), p);
        if (range.getTo() != null) {
            p.append("to");
            visit(range.getTo(), p);
        }
        afterSyntax(range, p);
        return range;
    }

    @Override
    public Proto visitReserved(Proto.Reserved reserved, PrintOutputCapture<P> p) {
        beforeSyntax(reserved, p);
        p.append("reserved");
        visitContainer("", reserved.getPadding().getReservations(), "", p);
        afterSyntax(reserved, p);
        return reserved;
    }

    @Override
    public Proto visitRpc(Proto.Rpc rpc, PrintOutputCapture<P> p) {
        beforeSyntax(rpc, p);
        p.append("rpc");
        visit(rpc.getName(), p);
        visit(rpc.getRequest(), p);
        visit(rpc.getReturns(), p);
        visit(rpc.getResponse(), p);
        visit(rpc.getBody(), p);
        afterSyntax(rpc, p);
        return rpc;
    }

    @Override
    public Proto visitRpcInOut(Proto.RpcInOut rpcInOut, PrintOutputCapture<P> p) {
        beforeSyntax(rpcInOut, p);
        p.append('(');
        if (rpcInOut.getStream() != null) {
            visitSpace(rpcInOut.getStream().getPrefix(), p);
            p.append("stream");
        }
        visitRightPadded(rpcInOut.getPadding().getType(), p);
        p.append(')');
        afterSyntax(rpcInOut, p);
        return rpcInOut;
    }

    @Override
    public Proto visitService(Proto.Service service, PrintOutputCapture<P> p) {
        beforeSyntax(service, p);
        p.append("service");
        visit(service.getName(), p);
        visit(service.getBody(), p);
        afterSyntax(service, p);
        return service;
    }

    @Override
    public Proto visitStringLiteral(Proto.StringLiteral stringLiteral, PrintOutputCapture<P> p) {
        beforeSyntax(stringLiteral, p);
        p.append(stringLiteral.isSingleQuote() ? '\'' : '"');
        p.append(stringLiteral.getLiteral());
        p.append(stringLiteral.isSingleQuote() ? '\'' : '"');
        afterSyntax(stringLiteral, p);
        return stringLiteral;
    }

    @Override
    public Proto visitSyntax(Proto.Syntax syntax, PrintOutputCapture<P> p) {
        // Skip printing implicit proto2 syntax that was not present in the original source
        if (syntax.getMarkers().findFirst(ImplicitProto2Syntax.class).isPresent()) {
            return syntax;
        }
        beforeSyntax(syntax, p);
        p.append("syntax");
        visitSpace(syntax.getKeywordSuffix(), p);
        p.append('=');
        visitRightPadded(syntax.getPadding().getLevel(), p);
        p.append(';');
        afterSyntax(syntax, p);
        return syntax;
    }

    @Override
    public Space visitSpace(Space space, PrintOutputCapture<P> p) {
        p.append(space.getWhitespace());
        for (Comment comment : space.getComments()) {
            visitMarkers(comment.getMarkers(), p);
            if (comment.isMultiline()) {
                p.append("/*").append(comment.getText()).append("*/");
            } else {
                p.append("//").append(comment.getText());
            }
            p.append(comment.getSuffix());
        }
        return space;
    }

    protected void visitContainer(String before, @Nullable ProtoContainer<? extends Proto> container,
                                  @Nullable String after, PrintOutputCapture<P> p) {
        if (container == null) {
            return;
        }
        visitSpace(container.getBefore(), p);
        p.append(before);
        visitRightPadded(container.getPadding().getElements(), p);
        p.append(after == null ? "" : after);
    }

    protected void visitRightPadded(List<? extends ProtoRightPadded<? extends Proto>> nodes, PrintOutputCapture<P> p) {
        for (int i = 0; i < nodes.size(); i++) {
            ProtoRightPadded<? extends Proto> node = nodes.get(i);
            visit(node.getElement(), p);
            visitSpace(node.getAfter(), p);
            if (i < nodes.size() - 1) {
                p.append(',');
            }
        }
    }

    protected void visitStatements(List<ProtoRightPadded<Proto>> statements, PrintOutputCapture<P> p) {
        for (ProtoRightPadded<Proto> paddedStat : statements) {
            visitStatement(paddedStat, p);
        }
    }

    protected void visitStatement(@Nullable ProtoRightPadded<Proto> paddedStat, PrintOutputCapture<P> p) {
        if (paddedStat == null) {
            return;
        }

        visit(paddedStat.getElement(), p);
        visitSpace(paddedStat.getAfter(), p);

        Proto s = paddedStat.getElement();
        if (s instanceof Proto.Empty ||
            s instanceof Proto.Field ||
            s instanceof Proto.Import ||
            s instanceof Proto.MapField ||
            s instanceof Proto.EnumField ||
            s instanceof Proto.OptionDeclaration ||
            s instanceof Proto.Package ||
            s instanceof Proto.Reserved ||
            (s instanceof Proto.Rpc && ((Proto.Rpc) s).getBody() == null) ||
            s instanceof Proto.Syntax) {
            p.append(';');
        }
    }

    private static final UnaryOperator<String> PROTO_MARKER_WRAPPER =
            out -> "/*~~" + out + (out.isEmpty() ? "" : "~~") + ">*/";

    private void beforeSyntax(Proto proto, PrintOutputCapture<P> p) {
        beforeSyntax(proto.getPrefix(), proto.getMarkers(), p);
    }

    private void beforeSyntax(Space prefix, Markers markers, PrintOutputCapture<P> p) {
        for (Marker marker : markers.getMarkers()) {
            p.append(p.getMarkerPrinter().beforePrefix(marker, new Cursor(getCursor(), marker), PROTO_MARKER_WRAPPER));
        }
        visitSpace(prefix, p);
        visitMarkers(markers, p);
        for (Marker marker : markers.getMarkers()) {
            p.append(p.getMarkerPrinter().beforeSyntax(marker, new Cursor(getCursor(), marker), PROTO_MARKER_WRAPPER));
        }
    }

    private void afterSyntax(Proto proto, PrintOutputCapture<P> p) {
        afterSyntax(proto.getMarkers(), p);
    }

    private void afterSyntax(Markers markers, PrintOutputCapture<P> p) {
        for (Marker marker : markers.getMarkers()) {
            p.append(p.getMarkerPrinter().afterSyntax(marker, new Cursor(getCursor(), marker), PROTO_MARKER_WRAPPER));
        }
    }
}
