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

import org.openrewrite.PrintOutputCapture;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.protobuf.ProtoVisitor;
import org.openrewrite.protobuf.tree.*;

import java.util.List;

public class ProtoPrinter<P> extends ProtoVisitor<PrintOutputCapture<P>> {

    @Override
    public Proto visitBlock(Proto.Block block, PrintOutputCapture<P> p) {
        visitSpace(block.getPrefix(), p);
        visitMarkers(block.getMarkers(), p);
        p.out.append('{');
        visitStatements(block.getPadding().getStatements(), p);
        visitSpace(block.getEnd(), p);
        p.out.append('}');
        return block;
    }

    @Override
    public Proto visitConstant(Proto.Constant constant, PrintOutputCapture<P> p) {
        visitSpace(constant.getPrefix(), p);
        visitMarkers(constant.getMarkers(), p);
        p.out.append(constant.getValueSource());
        return constant;
    }

    @Override
    public Proto visitDocument(Proto.Document document, PrintOutputCapture<P> p) {
        visitSpace(document.getPrefix(), p);
        visitMarkers(document.getMarkers(), p);
        visit(document.getSyntax(), p);
        visitStatements(document.getPadding().getBody(), p);
        visitSpace(document.getEof(), p);
        return document;
    }

    @Override
    public Proto visitEmpty(Proto.Empty empty, PrintOutputCapture<P> p) {
        visitSpace(empty.getPrefix(), p);
        visitMarkers(empty.getMarkers(), p);
        return empty;
    }

    @Override
    public Proto visitEnum(Proto.Enum anEnum, PrintOutputCapture<P> p) {
        visitSpace(anEnum.getPrefix(), p);
        visitMarkers(anEnum.getMarkers(), p);
        p.out.append("enum");
        visit(anEnum.getName(), p);
        visit(anEnum.getBody(), p);
        return anEnum;
    }

    @Override
    public Proto visitEnumField(Proto.EnumField enumField, PrintOutputCapture<P> p) {
        visitSpace(enumField.getPrefix(), p);
        visitMarkers(enumField.getMarkers(), p);
        visitRightPadded(enumField.getPadding().getName(), p);
        p.out.append('=');
        visit(enumField.getNumber(), p);
        visitContainer("[", enumField.getPadding().getOptions(), ",", "]", p);
        return enumField;
    }

    @Override
    public Proto visitExtend(Proto.Extend extend, PrintOutputCapture<P> p) {
        visitSpace(extend.getPrefix(), p);
        visitMarkers(extend.getMarkers(), p);
        p.out.append("extend");
        visitFullIdentifier(extend.getName(), p);
        visitBlock(extend.getBody(), p);
        return extend;
    }

    @Override
    public Proto visitExtensionName(Proto.ExtensionName extensionName, PrintOutputCapture<P> p) {
        visitSpace(extensionName.getPrefix(), p);
        visitMarkers(extensionName.getMarkers(), p);
        p.out.append('(');
        visitRightPadded(extensionName.getPadding().getExtension(), p);
        p.out.append(')');
        return extensionName;
    }

    @Override
    public Proto visitField(Proto.Field field, PrintOutputCapture<P> p) {
        visitSpace(field.getPrefix(), p);
        visitMarkers(field.getMarkers(), p);
        visit(field.getLabel(), p);
        visit(field.getType(), p);
        visitRightPadded(field.getPadding().getName(), p);
        p.out.append('=');
        visit(field.getNumber(), p);
        visitContainer("[", field.getPadding().getOptions(), ",", "]", p);
        return field;
    }

    @Override
    public Proto visitFullIdentifier(Proto.FullIdentifier identifier, PrintOutputCapture<P> p) {
        visitSpace(identifier.getPrefix(), p);
        visitMarkers(identifier.getMarkers(), p);
        visitRightPadded(identifier.getPadding().getTarget(), p);
        if (identifier.getTarget() != null) {
            p.out.append('.');
        }
        visit(identifier.getName(), p);
        return identifier;
    }

    @Override
    public Proto visitIdentifier(Proto.Identifier identifier, PrintOutputCapture<P> p) {
        visitSpace(identifier.getPrefix(), p);
        visitMarkers(identifier.getMarkers(), p);
        p.out.append(identifier.getName());
        return identifier;
    }

    @Override
    public Proto visitImport(Proto.Import anImport, PrintOutputCapture<P> p) {
        visitSpace(anImport.getPrefix(), p);
        visitMarkers(anImport.getMarkers(), p);
        p.out.append("import");
        visit(anImport.getModifier(), p);
        visitRightPadded(anImport.getPadding().getName(), p);
        return anImport;
    }

    @Override
    public Proto visitKeyword(Proto.Keyword keyword, PrintOutputCapture<P> p) {
        visitSpace(keyword.getPrefix(), p);
        visitMarkers(keyword.getMarkers(), p);
        p.out.append(keyword.getKeyword());
        return keyword;
    }

    @Override
    public Proto visitMapField(Proto.MapField mapField, PrintOutputCapture<P> p) {
        visitSpace(mapField.getPrefix(), p);
        visitMarkers(mapField.getMarkers(), p);
        p.out.append("map");
        visitSpace(mapField.getPadding().getMap().getAfter(), p);
        p.out.append('<');
        visitRightPadded(mapField.getPadding().getKeyType(), p);
        p.out.append(',');
        visitRightPadded(mapField.getPadding().getValueType(), p);
        p.out.append('>');
        visitRightPadded(mapField.getPadding().getName(), p);
        p.out.append('=');
        visit(mapField.getNumber(), p);
        visitContainer("[", mapField.getPadding().getOptions(), ",", "]", p);
        return mapField;
    }

    @Override
    public Proto visitMessage(Proto.Message message, PrintOutputCapture<P> p) {
        visitSpace(message.getPrefix(), p);
        visitMarkers(message.getMarkers(), p);
        p.out.append("message");
        visit(message.getName(), p);
        visit(message.getBody(), p);
        return message;
    }

    @Override
    public Proto visitOneOf(Proto.OneOf oneOf, PrintOutputCapture<P> p) {
        visitSpace(oneOf.getPrefix(), p);
        visitMarkers(oneOf.getMarkers(), p);
        p.out.append("oneof");
        visit(oneOf.getName(), p);
        visit(oneOf.getFields(), p);
        return oneOf;
    }

    @Override
    public Proto visitOption(Proto.Option option, PrintOutputCapture<P> p) {
        visitSpace(option.getPrefix(), p);
        visitMarkers(option.getMarkers(), p);
        visitRightPadded(option.getPadding().getName(), p);
        p.out.append('=');
        visit(option.getAssignment(), p);
        return option;
    }

    @Override
    public Proto visitOptionDeclaration(Proto.OptionDeclaration optionDeclaration, PrintOutputCapture<P> p) {
        visitSpace(optionDeclaration.getPrefix(), p);
        visitMarkers(optionDeclaration.getMarkers(), p);
        p.out.append("option");
        visitRightPadded(optionDeclaration.getPadding().getName(), p);
        p.out.append('=');
        visit(optionDeclaration.getAssignment(), p);
        return optionDeclaration;
    }

    @Override
    public Proto visitPackage(Proto.Package aPackage, PrintOutputCapture<P> p) {
        visitSpace(aPackage.getPrefix(), p);
        visitMarkers(aPackage.getMarkers(), p);
        p.out.append("package");
        visit(aPackage.getName(), p);
        return aPackage;
    }

    @Override
    public Proto visitPrimitive(Proto.Primitive primitive, PrintOutputCapture<P> p) {
        visitSpace(primitive.getPrefix(), p);
        visitMarkers(primitive.getMarkers(), p);
        p.out.append(primitive.getType().toString().toLowerCase());
        return primitive;
    }

    @Override
    public Proto visitRange(Proto.Range range, PrintOutputCapture<P> p) {
        visitSpace(range.getPrefix(), p);
        visitMarkers(range.getMarkers(), p);
        visitRightPadded(range.getPadding().getFrom(), p);
        if (range.getTo() != null) {
            p.out.append("to");
            visit(range.getTo(), p);
        }
        return range;
    }

    @Override
    public Proto visitReserved(Proto.Reserved reserved, PrintOutputCapture<P> p) {
        visitSpace(reserved.getPrefix(), p);
        visitMarkers(reserved.getMarkers(), p);
        p.out.append("reserved");
        visitContainer("", reserved.getPadding().getReservations(), ",", "", p);
        return reserved;
    }

    @Override
    public Proto visitRpc(Proto.Rpc rpc, PrintOutputCapture<P> p) {
        visitSpace(rpc.getPrefix(), p);
        visitMarkers(rpc.getMarkers(), p);
        p.out.append("rpc");
        visit(rpc.getName(), p);
        visit(rpc.getRequest(), p);
        visit(rpc.getReturns(), p);
        visit(rpc.getResponse(), p);
        visit(rpc.getBody(), p);
        return rpc;
    }

    @Override
    public Proto visitRpcInOut(Proto.RpcInOut rpcInOut, PrintOutputCapture<P> p) {
        visitSpace(rpcInOut.getPrefix(), p);
        visitMarkers(rpcInOut.getMarkers(), p);
        p.out.append('(');
        if (rpcInOut.getStream() != null) {
            visitSpace(rpcInOut.getStream().getPrefix(), p);
            p.out.append("stream");
        }
        visitRightPadded(rpcInOut.getPadding().getType(), p);
        p.out.append(')');
        return rpcInOut;
    }

    @Override
    public Proto visitService(Proto.Service service, PrintOutputCapture<P> p) {
        visitSpace(service.getPrefix(), p);
        visitMarkers(service.getMarkers(), p);
        p.out.append("service");
        visit(service.getName(), p);
        visit(service.getBody(), p);
        return service;
    }

    @Override
    public Proto visitStringLiteral(Proto.StringLiteral stringLiteral, PrintOutputCapture<P> p) {
        visitSpace(stringLiteral.getPrefix(), p);
        visitMarkers(stringLiteral.getMarkers(), p);
        p.out.append(stringLiteral.isSingleQuote() ? '\'' : '"');
        p.out.append(stringLiteral.getLiteral());
        p.out.append(stringLiteral.isSingleQuote() ? '\'' : '"');
        return stringLiteral;
    }

    @Override
    public Proto visitSyntax(Proto.Syntax syntax, PrintOutputCapture<P> p) {
        visitSpace(syntax.getPrefix(), p);
        visitMarkers(syntax.getMarkers(), p);
        p.out.append("syntax");
        visitSpace(syntax.getKeywordSuffix(), p);
        p.out.append('=');
        visitRightPadded(syntax.getPadding().getLevel(), p);
        p.out.append(';');
        return syntax;
    }

    public Space visitSpace(Space space, PrintOutputCapture<P> p) {
        p.out.append(space.getWhitespace());
        for (Comment comment : space.getComments()) {
            visitMarkers(comment.getMarkers(), p);
            if (comment.isMultiline()) {
                p.out.append("/*").append(comment.getText()).append("*/");
            } else {
                p.out.append("//").append(comment.getText());
            }
            p.out.append(comment.getSuffix());
        }
        return space;
    }

    protected void visitContainer(String before, @Nullable ProtoContainer<? extends Proto> container,
                                   String suffixBetween, @Nullable String after, PrintOutputCapture<P> p) {
        if (container == null) {
            return;
        }
        visitSpace(container.getBefore(), p);
        p.append(before);
        visitRightPadded(container.getPadding().getElements(), suffixBetween, p);
        p.append(after == null ? "" : after);
    }

    protected void visitLeftPadded(@Nullable String prefix, @Nullable ProtoLeftPadded<? extends Proto> leftPadded, PrintOutputCapture<P> p) {
        if (leftPadded != null) {
            visitSpace(leftPadded.getBefore(), p);
            if (prefix != null) {
                p.out.append(prefix);
            }
            visit(leftPadded.getElement(), p);
        }
    }

    protected void visitRightPadded(List<? extends ProtoRightPadded<? extends Proto>> nodes, String suffixBetween, PrintOutputCapture<P> p) {
        for (int i = 0; i < nodes.size(); i++) {
            ProtoRightPadded<? extends Proto> node = nodes.get(i);
            visit(node.getElement(), p);
            visitSpace(node.getAfter(), p);
            if (i < nodes.size() - 1) {
                p.out.append(suffixBetween);
            }
        }
    }

    @Override
    public <M extends Marker> M visitMarker(Marker marker, PrintOutputCapture<P> p) {
        if (marker instanceof SearchResult) {
            String description = ((SearchResult) marker).getDescription();
            p.out.append("/*~~")
                    .append(description == null ? "" : "(" + description + ")~~")
                    .append(">*/");
        }
        //noinspection unchecked
        return (M) marker;
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
}
