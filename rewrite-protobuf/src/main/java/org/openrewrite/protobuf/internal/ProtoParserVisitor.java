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

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.jspecify.annotations.Nullable;
import org.openrewrite.FileAttributes;
import org.openrewrite.marker.Markers;
import org.openrewrite.protobuf.internal.grammar.Protobuf2Parser;
import org.openrewrite.protobuf.internal.grammar.Protobuf2ParserBaseVisitor;
import org.openrewrite.protobuf.tree.*;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import static org.openrewrite.Tree.randomId;

public class ProtoParserVisitor extends Protobuf2ParserBaseVisitor<Proto> {
    private final Path path;

    @Nullable
    private final FileAttributes fileAttributes;

    private final String source;
    private final Charset charset;
    private final boolean charsetBomMarked;

    /**
     * Track position within the file by character (UTF-16 code units)
     */
    private int cursor = 0;
    /**
     * Track parsing position within the file by Unicode code point
     */
    private int codePointCursor = 0;

    public ProtoParserVisitor(Path path, @Nullable FileAttributes fileAttributes, String source, Charset charset, boolean charsetBomMarked) {
        this.path = path;
        this.fileAttributes = fileAttributes;
        this.source = source;
        this.charset = charset;
        this.charsetBomMarked = charsetBomMarked;
    }

    public Proto.Block visitBlock(List<ParseTree> statementTrees) {
        Space bodyPrefix = sourceBefore("{");
        List<ProtoRightPadded<Proto>> statements = new ArrayList<>(statementTrees.size() - 2);
        for (int i = 1; i < statementTrees.size() - 1; i++) {
            Proto s = visit(statementTrees.get(i));
            statements.add(ProtoRightPadded.build(s).withAfter(
                    (s instanceof Proto.Empty ||
                            s instanceof Proto.Field ||
                            s instanceof Proto.EnumField ||
                            s instanceof Proto.Import ||
                            s instanceof Proto.MapField ||
                            s instanceof Proto.OptionDeclaration ||
                            s instanceof Proto.Package ||
                            s instanceof Proto.Reserved ||
                            (s instanceof Proto.Rpc && ((Proto.Rpc) s).getBody() == null) ||
                            s instanceof Proto.Syntax
                    ) ? sourceBefore(";") : Space.EMPTY
            ));
        }
        return new Proto.Block(randomId(), bodyPrefix, Markers.EMPTY, statements, sourceBefore("}"));
    }

    @Override
    public Proto.Constant visitConstant(Protobuf2Parser.ConstantContext ctx) {
        String sourceValue = ctx.getChild(0).getText();
        String source = sourceValue;
        if (ctx.StringLiteral() != null) {
            source = source.substring(1, source.length() - 1);
        }
        return new Proto.Constant(randomId(), sourceBefore(sourceValue), Markers.EMPTY, source, sourceValue);
    }

    @Override
    public Proto.Empty visitEmptyStatement(Protobuf2Parser.EmptyStatementContext ctx) {
        return new Proto.Empty(randomId(), Space.EMPTY, Markers.EMPTY);
    }

    @Override
    public Proto.Enum visitEnumDefinition(Protobuf2Parser.EnumDefinitionContext ctx) {
        return new Proto.Enum(randomId(), sourceBefore("enum"), Markers.EMPTY,
                visitIdent(ctx.ident()),
                visitBlock(ctx.enumBody().children));
    }

    @Override
    public Proto.EnumField visitEnumField(Protobuf2Parser.EnumFieldContext ctx) {
        Proto.Identifier name = visitIdent(ctx.ident());
        return new Proto.EnumField(randomId(), name.getPrefix(), Markers.EMPTY,
                ProtoRightPadded.build(name.withPrefix(Space.EMPTY)).withAfter(sourceBefore("=")),
                mapConstant(ctx.IntegerLiteral()),
                mapOptionList(ctx.optionList())
        );
    }

    @Override
    public Proto.Extend visitExtend(Protobuf2Parser.ExtendContext ctx) {
        Space prefix = sourceBefore("extend");
        Proto.FullIdentifier name = visitFullIdent(ctx.fullIdent());
        Space blockPrefix = sourceBefore("{");

        List<ProtoRightPadded<Proto>> statements = new ArrayList<>(ctx.messageField().size());
        for (Protobuf2Parser.MessageFieldContext mfc : ctx.messageField()) {
            statements.add(new ProtoRightPadded<>(visitMessageField(mfc), sourceBefore(";"), Markers.EMPTY));
        }

        return new Proto.Extend(randomId(), prefix, Markers.EMPTY, name,
                new Proto.Block(randomId(), blockPrefix, Markers.EMPTY, statements, sourceBefore("}")));
    }

    @Override
    public Proto.Field visitField(Protobuf2Parser.FieldContext ctx) {
        TypeTree type = (TypeTree) visit(ctx.type());
        return new Proto.Field(randomId(), type.getPrefix(), Markers.EMPTY,
                null,
                type.withPrefix(Space.EMPTY),
                ProtoRightPadded.build(visitIdentOrReserved(ctx.fieldName)).withAfter(sourceBefore("=")),
                mapConstant(ctx.IntegerLiteral()),
                mapOptionList(ctx.optionList())
        );
    }

    @Override
    public Proto visitFullyQualifiedType(Protobuf2Parser.FullyQualifiedTypeContext ctx) {
        return visitFullIdent(ctx.fullIdent());
    }

    @Override
    public Proto.Import visitImportStatement(Protobuf2Parser.ImportStatementContext ctx) {
        Space prefix = sourceBefore("import");
        Proto.Keyword modifier = null;
        if (ctx.WEAK() != null) {
            modifier = new Proto.Keyword(randomId(), sourceBefore(ctx.WEAK().getText()), Markers.EMPTY, ctx.WEAK().getText());
        } else if (ctx.PUBLIC() != null) {
            modifier = new Proto.Keyword(randomId(), sourceBefore(ctx.PUBLIC().getText()), Markers.EMPTY, ctx.PUBLIC().getText());
        }

        Protobuf2Parser.StringLiteralContext s = ctx.stringLiteral();
        String lit = s.getText();
        return new Proto.Import(randomId(), prefix, Markers.EMPTY, modifier,
                ProtoRightPadded.build(new Proto.StringLiteral(randomId(), sourceBefore(lit), Markers.EMPTY, lit.startsWith("'"), lit.substring(1, lit.length() - 1))));
    }

    @Override
    public Proto.Primitive visitPrimitiveType(Protobuf2Parser.PrimitiveTypeContext ctx) {
        return new Proto.Primitive(randomId(), sourceBefore(ctx.getText()), Markers.EMPTY,
                Proto.Primitive.Type.valueOf(ctx.getText().toUpperCase()));
    }

    @Override
    public Proto.FullIdentifier visitFullIdent(Protobuf2Parser.FullIdentContext ctx) {
        Space prefix = prefix(ctx.identOrReserved(0));
        return visitFullIdent(ctx.identOrReserved(), 0, null).withPrefix(prefix);
    }

    private FullName visitFullIdent(List<? extends ParseTree> idents, int start, @Nullable FullName prefix) {
        FullName fi = prefix;
        for (int j = start; j < idents.size(); j++) {
            ParseTree i = idents.get(j);
            if (!(i instanceof Protobuf2Parser.IdentOrReservedContext || i instanceof Protobuf2Parser.ReservedWordContext)) {
                continue;
            }
            ProtoRightPadded<FullName> previous = fi == null ?
                    null :
                    ProtoRightPadded.build(fi).withAfter(sourceBefore("."));
            fi = new Proto.FullIdentifier(randomId(), Space.EMPTY, Markers.EMPTY, previous,
                    (Proto.Identifier) visit(i));
        }
        assert fi != null;
        return fi;
    }

    @Override
    public Proto.Identifier visitIdentOrReserved(Protobuf2Parser.IdentOrReservedContext ctx) {
        String name = ctx.ident() == null ? ctx.reservedWord().getText() : ctx.ident().getText();
        return new Proto.Identifier(randomId(), sourceBefore(name), Markers.EMPTY, name);
    }

    @Override
    public Proto.Identifier visitIdent(Protobuf2Parser.IdentContext ctx) {
        String name = ctx.Ident().getText();
        return new Proto.Identifier(randomId(), sourceBefore(name), Markers.EMPTY, name);
    }

    @Override
    public Proto.MapField visitMapField(Protobuf2Parser.MapFieldContext ctx) {
        return new Proto.MapField(randomId(), sourceBefore("map"), Markers.EMPTY,
                ProtoRightPadded.build(new Proto.Keyword(randomId(), Space.EMPTY, Markers.EMPTY, "map")).withAfter(sourceBefore("<")),
                ProtoRightPadded.build(new Proto.Keyword(randomId(), sourceBefore(ctx.keyType().getText()), Markers.EMPTY, ctx.keyType().getText())).withAfter(sourceBefore(",")),
                ProtoRightPadded.build((TypeTree) visit(ctx.type())).withAfter(sourceBefore(">")),
                ProtoRightPadded.build(visitIdent(ctx.ident())).withAfter(sourceBefore("=")),
                mapConstant(ctx.IntegerLiteral()),
                mapOptionList(ctx.optionList()));
    }

    private Proto.Constant mapConstant(TerminalNode integerLiteral) {
        String number = integerLiteral.getText();
        Integer numberValue = number.contains("x") ? Integer.parseInt(number, 16) :
                number.startsWith("0") ? Integer.parseInt(number, 8) :
                        Integer.parseInt(number);
        return new Proto.Constant(randomId(), sourceBefore(number), Markers.EMPTY, numberValue, number);
    }

    @Override
    public Proto.Message visitMessage(Protobuf2Parser.MessageContext ctx) {
        return new Proto.Message(randomId(), sourceBefore("message"), Markers.EMPTY,
                visitIdent(ctx.ident()),
                visitBlock(ctx.messageBody().children));
    }

    @Override
    public Proto.Field visitMessageField(Protobuf2Parser.MessageFieldContext ctx) {
        String label = ctx.getChild(0).getText();
        Space labelPrefix = sourceBefore(label);
        Proto.Field field = visitField(ctx.field());
        return field
                .withType(field.getType().withPrefix(field.getPrefix()))
                .withLabel(new Proto.Keyword(randomId(), Space.EMPTY, Markers.EMPTY, label))
                .withPrefix(labelPrefix);
    }

    @Override
    public Proto visitOneOf(Protobuf2Parser.OneOfContext ctx) {
        Space prefix = sourceBefore("oneof");
        Proto.Identifier ident = visitIdent(ctx.ident());

        Space fieldsPrefix = sourceBefore("{");
        List<ProtoRightPadded<Proto>> fields = new ArrayList<>(ctx.field().size());
        List<Protobuf2Parser.FieldContext> fieldContexts = ctx.field();
        for (Protobuf2Parser.FieldContext field : fieldContexts) {
            fields.add(new ProtoRightPadded<>(visitField(field), sourceBefore(";"), Markers.EMPTY));
        }

        return new Proto.OneOf(randomId(), prefix, Markers.EMPTY,
                ident, new Proto.Block(randomId(), fieldsPrefix, Markers.EMPTY, fields, sourceBefore("}")));
    }

    @Override
    public Proto visitOptionDef(Protobuf2Parser.OptionDefContext ctx) {
        return new Proto.OptionDeclaration(randomId(), sourceBefore("option"), Markers.EMPTY,
                ProtoRightPadded.build(visitOptionName(ctx.option().optionName())).withAfter(sourceBefore("=")),
                visitConstant(ctx.option().constant()));
    }

    @Override
    public Proto.Option visitOption(Protobuf2Parser.OptionContext ctx) {
        ProtoRightPadded<FullName> name = ProtoRightPadded.build(visitOptionName(ctx.optionName())).withAfter(sourceBefore("="));
        return new Proto.Option(randomId(), name.getElement().getPrefix(), Markers.EMPTY,
                name.withElement(name.getElement().withPrefix(Space.EMPTY)),
                visitConstant(ctx.constant()));
    }

    @Override
    public FullName visitOptionName(Protobuf2Parser.OptionNameContext ctx) {
        FullName name;
        if (ctx.fullIdent() != null) {
            name = new Proto.ExtensionName(randomId(), sourceBefore("("), Markers.EMPTY,
                    ProtoRightPadded.build(visitFullIdent(ctx.fullIdent())).withAfter(sourceBefore(")")));
        } else {
            Proto.Identifier ident = visitIdent(ctx.ident());
            name = new Proto.FullIdentifier(randomId(), ident.getPrefix(), Markers.EMPTY,
                    null, ident.withPrefix(Space.EMPTY));
        }

        if (ctx.children.size() > 1) {
            return visitFullIdent(ctx.children, 1, name);
        }
        return name;
    }

    private @Nullable ProtoContainer<Proto.Option> mapOptionList(Protobuf2Parser.@Nullable OptionListContext ctx) {
        if (ctx == null) {
            return null;
        }

        Space optionsPrefix = sourceBefore("[");
        List<ProtoRightPadded<Proto.Field.Option>> fieldOptions = new ArrayList<>(ctx.option().size());
        List<Protobuf2Parser.OptionContext> fieldOption = ctx.option();
        for (int i = 0; i < fieldOption.size(); i++) {
            Protobuf2Parser.OptionContext o = fieldOption.get(i);
            FullName name = visitOptionName(o.optionName());
            Proto.Field.Option opt = new Proto.Field.Option(randomId(), name.getPrefix(), Markers.EMPTY,
                    ProtoRightPadded.build((FullName) name.withPrefix(Space.EMPTY)).withAfter(sourceBefore("=")),
                    visitConstant(o.constant()));
            fieldOptions.add(ProtoRightPadded.build(opt).withAfter(sourceBefore(i == fieldOption.size() - 1 ? "]" : ",")));
        }
        return ProtoContainer.build(fieldOptions).withBefore(optionsPrefix);
    }

    @Override
    public Proto visitPackageStatement(Protobuf2Parser.PackageStatementContext ctx) {
        return new Proto.Package(randomId(), sourceBefore("package"), Markers.EMPTY,
                visitFullIdent(ctx.fullIdent()));
    }

    @Override
    public Proto.Document visitProto(Protobuf2Parser.ProtoContext ctx) {
        Proto.Syntax syntax = visitSyntax(ctx.syntax());
        List<ProtoRightPadded<Proto>> list = new ArrayList<>();
        // The first element is the syntax, which we've already parsed
        // The last element is a "TerminalNode" which we are uninterested in
        for (int i = 1; i < ctx.children.size() - 1; i++) {
            Proto s = visit(ctx.children.get(i));
            ProtoRightPadded<Proto> protoProtoRightPadded = ProtoRightPadded.build(s).withAfter(
                    (s instanceof Proto.Empty ||
                            s instanceof Proto.Import ||
                            s instanceof Proto.MapField ||
                            s instanceof Proto.OptionDeclaration ||
                            s instanceof Proto.Package ||
                            s instanceof Proto.Syntax
                    ) ? sourceBefore(";") : Space.EMPTY
            );
            list.add(protoProtoRightPadded);
        }

        return new Proto.Document(
                randomId(),
                path,
                fileAttributes,
                syntax.getPrefix(),
                Markers.EMPTY,
                charset.name(),
                charsetBomMarked,
                null,
                syntax.withPrefix(Space.EMPTY),
                list,
                Space.format(source.substring(cursor))
        );
    }

    @Override
    public Proto.Range visitRange(Protobuf2Parser.RangeContext ctx) {
        Proto.Constant from = mapConstant(ctx.IntegerLiteral(0));
        TerminalNode to = ctx.IntegerLiteral(1);
        return new Proto.Range(randomId(), from.getPrefix(), Markers.EMPTY,
                ProtoRightPadded.build(from.withPrefix(Space.EMPTY))
                        .withAfter(to != null ? sourceBefore("to") : Space.EMPTY),
                to != null ? mapConstant(to) : null);
    }

    @Override
    public Proto.Reserved visitReserved(Protobuf2Parser.ReservedContext ctx) {
        Space prefix = sourceBefore("reserved");
        List<ProtoRightPadded<Proto>> reservations;
        if (ctx.fieldNames() != null) {
            List<Protobuf2Parser.StringLiteralContext> stringLiterals = ctx.fieldNames().stringLiteral();
            reservations = new ArrayList<>(stringLiterals.size());
            for (int i = 0; i < stringLiterals.size(); i++) {
                Protobuf2Parser.StringLiteralContext s = stringLiterals.get(i);
                String lit = s.getText();
                reservations.add(ProtoRightPadded
                        .build((Proto) new Proto.Constant(randomId(), sourceBefore(lit), Markers.EMPTY, lit.substring(1, lit.length() - 1), lit))
                        .withAfter(i == stringLiterals.size() - 1 ? Space.EMPTY : sourceBefore(",")));
            }
        } else {
            List<Protobuf2Parser.RangeContext> ranges = ctx.ranges().range();
            reservations = new ArrayList<>(ranges.size());
            for (int i = 0; i < ranges.size(); i++) {
                Protobuf2Parser.RangeContext r = ranges.get(i);
                reservations.add(ProtoRightPadded.build((Proto) visitRange(r))
                        .withAfter(i == ranges.size() - 1 ? Space.EMPTY : sourceBefore(",")));
            }
        }
        return new Proto.Reserved(randomId(), prefix, Markers.EMPTY,
                ProtoContainer.build(reservations));
    }

    @Override
    public Proto visitReservedWord(Protobuf2Parser.ReservedWordContext ctx) {
        String word = ctx.getChild(0).getText();
        return new Proto.Identifier(randomId(), sourceBefore(word), Markers.EMPTY, word);
    }

    @Override
    public Proto visitRpc(Protobuf2Parser.RpcContext ctx) {
        return new Proto.Rpc(randomId(), sourceBefore("rpc"), Markers.EMPTY,
                visitIdent(ctx.ident()),
                visitRpcInOut(ctx.rpcInOut(0)),
                new Proto.Keyword(randomId(), sourceBefore("returns"), Markers.EMPTY, "returns"),
                visitRpcInOut(ctx.rpcInOut(1)),
                ctx.rpcBody() == null ? null : visitBlock(ctx.rpcBody().children));
    }

    @Override
    public Proto.RpcInOut visitRpcInOut(Protobuf2Parser.RpcInOutContext ctx) {
        return new Proto.RpcInOut(randomId(), sourceBefore("("), Markers.EMPTY,
                ctx.STREAM() == null ? null : new Proto.Keyword(randomId(), sourceBefore("stream"), Markers.EMPTY, "stream"),
                ProtoRightPadded.build((FullName) visit(ctx.fullIdent())).withAfter(sourceBefore(")")));
    }

    @Override
    public Proto visitService(Protobuf2Parser.ServiceContext ctx) {
        return new Proto.Service(randomId(), sourceBefore("service"), Markers.EMPTY,
                visitIdent(ctx.ident()),
                visitBlock(ctx.serviceBody().children));
    }

    @Override
    public Proto.Syntax visitSyntax(Protobuf2Parser.SyntaxContext ctx) {
        String level = ctx.stringLiteral().StringLiteral().getText();
        return new Proto.Syntax(randomId(), sourceBefore("syntax"), Markers.EMPTY, sourceBefore("="),
                ProtoRightPadded.build(new Proto.Constant(randomId(), sourceBefore(level), Markers.EMPTY,
                                level.substring(1, level.length() - 1), level))
                        .withAfter(sourceBefore(";")));
    }

    private Space prefix(ParserRuleContext ctx) {
        return prefix(ctx.getStart());
    }

    private Space prefix(@Nullable TerminalNode terminalNode) {
        return terminalNode == null ? Space.EMPTY : prefix(terminalNode.getSymbol());
    }

    private Space prefix(Token token) {
        int start = token.getStartIndex();
        if (start < codePointCursor) {
            return Space.EMPTY;
        }
        int oldCursor = cursor;
        advanceCursor(start);
        return Space.format(source.substring(oldCursor, cursor));
    }

    private <C extends ParserRuleContext, T> @Nullable T convert(C ctx, BiFunction<C, Space, T> conversion) {
        //noinspection ConstantConditions
        if (ctx == null) {
            return null;
        }

        T t = conversion.apply(ctx, prefix(ctx));
        if (ctx.getStop() != null) {
            advanceCursor(ctx.getStop().getStopIndex() + (Character.isWhitespace(source.charAt(ctx.getStop().getStopIndex())) ? 0 : 1));
        }

        return t;
    }

    private <T> T convert(TerminalNode node, BiFunction<TerminalNode, Space, T> conversion) {
        T t = conversion.apply(node, prefix(node));
        advanceCursor(node.getSymbol().getStopIndex() + 1);
        return t;
    }

    /**
     * @return Source from <code>cursor</code> to next occurrence of <code>untilDelim</code>,
     * and if not found in the remaining source, the empty String. If <code>stop</code> is reached before
     * <code>untilDelim</code> return the empty String.
     */
    private Space sourceBefore(String untilDelim) {
        int delimIndex = positionOfNext(untilDelim, null);
        if (delimIndex < 0) {
            return Space.EMPTY; // unable to find this delimiter
        }

        String prefix = source.substring(cursor, delimIndex);
        int codePointsInPrefix = prefix.codePointCount(0, prefix.length());
        // All Protobuf delimiters are ASCII, so length == code point count
        advanceCursor(codePointCursor + codePointsInPrefix + untilDelim.length());
        return Space.format(prefix);
    }

    private int positionOfNext(String untilDelim, @Nullable Character stop) {
        boolean inMultiLineComment = false;
        boolean inSingleLineComment = false;

        int delimIndex = cursor;
        for (; delimIndex < source.length() - untilDelim.length() + 1; delimIndex++) {
            if (inSingleLineComment) {
                if (source.charAt(delimIndex) == '\n') {
                    inSingleLineComment = false;
                }
            } else {
                if (source.length() - untilDelim.length() > delimIndex + 1) {
                    switch (source.substring(delimIndex, delimIndex + 2)) {
                        case "//":
                            inSingleLineComment = !inMultiLineComment;
                            delimIndex++;
                            break;
                        case "/*":
                            inMultiLineComment = true;
                            delimIndex++;
                            break;
                        case "*/":
                            inMultiLineComment = false;
                            delimIndex = delimIndex + 2;
                            break;
                    }
                }

                if (!inMultiLineComment && !inSingleLineComment) {
                    if (stop != null && source.charAt(delimIndex) == stop) {
                        return -1; // reached stop word before finding the delimiter
                    }

                    if (source.startsWith(untilDelim, delimIndex)) {
                        break; // found it!
                    }
                }
            }
        }

        return delimIndex > source.length() - untilDelim.length() ? -1 : delimIndex;
    }

    /**
     * Advance both the cursor and the code point cursor
     */
    @SuppressWarnings("UnusedReturnValue")
    private int advanceCursor(int newCodePointIndex) {
        if (newCodePointIndex <= codePointCursor) {
            return cursor;
        }
        cursor = source.offsetByCodePoints(cursor, newCodePointIndex - codePointCursor);
        codePointCursor = newCodePointIndex;
        return cursor;
    }
}
