/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.toml.internal;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.jspecify.annotations.Nullable;
import org.openrewrite.FileAttributes;
import org.openrewrite.internal.EncodingDetectingInputStream;
import org.openrewrite.marker.Markers;
import org.openrewrite.toml.internal.grammar.TomlParser;
import org.openrewrite.toml.internal.grammar.TomlParserBaseVisitor;
import org.openrewrite.toml.marker.ArrayTable;
import org.openrewrite.toml.marker.InlineTable;
import org.openrewrite.toml.tree.*;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.openrewrite.Tree.randomId;

public class TomlParserVisitor extends TomlParserBaseVisitor<Toml> {
    private final Path path;
    private final String source;
    private final Charset charset;
    private final boolean charsetBomMarked;

    @Nullable
    private final FileAttributes fileAttributes;

    private int cursor = 0;
    private int codePointCursor = 0;

    public TomlParserVisitor(Path path, @Nullable FileAttributes fileAttributes, EncodingDetectingInputStream source) {
        this.path = path;
        this.fileAttributes = fileAttributes;
        this.source = source.readFully();
        this.charset = source.getCharset();
        this.charsetBomMarked = source.isCharsetBomMarked();
    }

    @Override
    public Toml visitChildren(RuleNode node) {
        Toml result = defaultResult();
        int n = node.getChildCount();
        for (int i = 0; i < n; i++) {
            if (!shouldVisitNextChild(node, result)) {
                break;
            }

            ParseTree c = node.getChild(i);
            if (c instanceof TomlParser.CommentContext) {
                continue;
            }

            Toml childResult = c.accept(this);
            result = aggregateResult(result, childResult);
        }

        return result;
    }

    @Override
    public Toml.Document visitDocument(TomlParser.DocumentContext ctx) {
        if (!ctx.children.isEmpty() && ctx.children.get(0) instanceof TerminalNode && ((TerminalNode) ctx.children.get(0)).getSymbol().getType() == TomlParser.EOF) {
            new Toml.Document(
                    randomId(),
                    path,
                    Space.EMPTY,
                    Markers.EMPTY,
                    charset.name(),
                    charsetBomMarked,
                    null,
                    fileAttributes,
                    emptyList(),
                    Space.EMPTY
            );
        }

        List<TomlValue> elements = new ArrayList<>();
        // The last element is a "TerminalNode" which we are uninterested in
        for (int i = 0; i < ctx.children.size() - 1; i++) {
            TomlValue element = (TomlValue) visit(ctx.children.get(i));
            if (element != null) {
                elements.add(element);
            }
        }

        return new Toml.Document(
                randomId(),
                path,
                Space.EMPTY,
                Markers.EMPTY,
                charset.name(),
                charsetBomMarked,
                null,
                fileAttributes,
                elements,
                Space.format(source, cursor, source.length())
        );
    }

    @Override
    public Toml.Identifier visitKey(TomlParser.KeyContext ctx) {
        return (Toml.Identifier) super.visitKey(ctx);
    }

    @Override
    public Toml.Identifier visitSimpleKey(TomlParser.SimpleKeyContext ctx) {
        return convert(ctx, (c, prefix) -> new Toml.Identifier(
                randomId(),
                prefix,
                Markers.EMPTY,
                c.getText(),
                c.getText()
        ));
    }

    @Override
    public Toml.Identifier visitDottedKey(TomlParser.DottedKeyContext ctx) {
        Space prefix = prefix(ctx);
        StringBuilder text = new StringBuilder();
        StringBuilder key = new StringBuilder();
        for (ParseTree child : ctx.children) {
            Space space = sourceBefore(child.getText());
            text.append(space.getWhitespace()).append(child.getText());
            key.append(child.getText());
        }
        return new Toml.Identifier(
                randomId(),
                prefix,
                Markers.EMPTY,
                text.toString(),
                key.toString()
        );
    }

    @Override
    public Toml.KeyValue visitKeyValue(TomlParser.KeyValueContext ctx) {
        return convert(ctx, (c, prefix) -> new Toml.KeyValue(
                randomId(),
                prefix,
                Markers.EMPTY,
                TomlRightPadded.build((TomlKey) visitKey(c.key())).withAfter(sourceBefore("=")),
                visitValue(c.value())
            ));
    }

    @Override
    public Toml visitString(TomlParser.StringContext ctx) {
        return convert(ctx, (c, prefix) -> {
            String string = c.getText();
            return new Toml.Literal(
                    randomId(),
                    prefix,
                    Markers.EMPTY,
                    TomlType.Primitive.String,
                    string,
                    string.substring(1, string.length() - 1)
            );
        });
    }

    @Override
    public Toml visitInteger(TomlParser.IntegerContext ctx) {
        return convert(ctx, (c, prefix) -> {
            String rawNumber = c.getText();
            String number = rawNumber.replace("_", "");
            Long numberValue = rawNumber.startsWith("0x") ? Long.parseLong(number.substring(2), 16) :
                    rawNumber.startsWith("0o") ? Long.parseLong(number.substring(2), 8) :
                            rawNumber.startsWith("0b") ? Long.parseLong(number.substring(2), 2) :
                                Long.parseLong(number);
            return new Toml.Literal(
                    randomId(),
                    prefix,
                    Markers.EMPTY,
                    TomlType.Primitive.Integer,
                    rawNumber,
                    numberValue
            );
        });
    }

    @Override
    public Toml visitFloatingPoint(TomlParser.FloatingPointContext ctx) {
        return convert(ctx, (c, prefix) -> {
            String rawNumber = c.getText();
            if (c.NAN() != null) {
                return new Toml.Literal(
                        randomId(),
                        prefix,
                        Markers.EMPTY,
                        TomlType.Primitive.Float,
                        rawNumber,
                        Double.NaN
                );
            } else if (c.INF() != null) {
                return new Toml.Literal(
                        randomId(),
                        prefix,
                        Markers.EMPTY,
                        TomlType.Primitive.Float,
                        rawNumber,
                        source.charAt(cursor) == '-' ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY
                );
            }

            String number = rawNumber.replace("_", "");
            Double numberValue = Double.parseDouble(number);
            return new Toml.Literal(
                    randomId(),
                    prefix,
                    Markers.EMPTY,
                    TomlType.Primitive.Float,
                    rawNumber,
                    numberValue
            );
        });
    }

    @Override
    public Toml visitBool(TomlParser.BoolContext ctx) {
        return convert(ctx, (c, prefix) -> {
            String bool = c.getText();
            Boolean boolValue = Boolean.parseBoolean(bool);
            return new Toml.Literal(
                    randomId(),
                    prefix,
                    Markers.EMPTY,
                    TomlType.Primitive.Boolean,
                    bool,
                    boolValue
            );
        });
    }

    private static final DateTimeFormatter RFC3339_OFFSET_DATE_TIME;

    static {
        RFC3339_OFFSET_DATE_TIME = new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .append(DateTimeFormatter.ISO_LOCAL_DATE)
                .appendLiteral(' ')
                .append(DateTimeFormatter.ISO_LOCAL_TIME)
                .parseLenient()
                .appendOffsetId()
                .parseStrict()
                .toFormatter();
    }

    @Override
    public Toml visitDateTime(TomlParser.DateTimeContext ctx) {
        return convert(ctx, (c, prefix) -> {
            String dateTime = c.getText();
            if (c.OFFSET_DATE_TIME() != null) {
                return new Toml.Literal(
                        randomId(),
                        prefix,
                        Markers.EMPTY,
                        TomlType.Primitive.OffsetDateTime,
                        dateTime,
                        dateTime.contains("T") ? OffsetDateTime.parse(dateTime) : OffsetDateTime.parse(dateTime, RFC3339_OFFSET_DATE_TIME)
                );
            } else if (c.LOCAL_DATE_TIME() != null) {
                return new Toml.Literal(
                        randomId(),
                        prefix,
                        Markers.EMPTY,
                        TomlType.Primitive.LocalDateTime,
                        dateTime,
                        LocalDateTime.parse(dateTime)
                );
            } else if (c.LOCAL_DATE() != null) {
                return new Toml.Literal(
                        randomId(),
                        prefix,
                        Markers.EMPTY,
                        TomlType.Primitive.LocalDate,
                        dateTime,
                        LocalDate.parse(dateTime)
                );
            }

            return new Toml.Literal(
                    randomId(),
                    prefix,
                    Markers.EMPTY,
                    TomlType.Primitive.LocalTime,
                    dateTime,
                    LocalTime.parse(dateTime)
            );
        });
    }

    @Override
    public Toml visitArray(TomlParser.ArrayContext ctx) {
        return convert(ctx, (c, prefix) -> {
            sourceBefore("[");
            List<TomlParser.ValueContext> values = c.value();
            List<TomlRightPadded<Toml>> elements = new ArrayList<>();
            for (int i = 0; i < values.size(); i++) {
                Toml element = visit(values.get(i));
                if (i == values.size() - 1) {
                    if (positionOfNext(",", ']') >= 0) {
                        elements.add(TomlRightPadded.build(element).withAfter(sourceBefore(",")));
                        elements.add(TomlRightPadded.build((Toml) new Toml.Empty(randomId(), Space.EMPTY, Markers.EMPTY)).withAfter(sourceBefore("]")));
                    } else {
                        elements.add(TomlRightPadded.build(element).withAfter(sourceBefore("]")));
                    }
                } else {
                    elements.add(TomlRightPadded.build(element).withAfter(sourceBefore(",")));
                }
            }

            return new Toml.Array(
                    randomId(),
                    prefix,
                    Markers.EMPTY,
                    elements
            );
        });
    }

    @Override
    public Toml visitInlineTable(TomlParser.InlineTableContext ctx) {
        return convert(ctx, (c, prefix) -> {
            sourceBefore("{");
            List<TomlParser.KeyValueContext> values = c.keyValue();
            List<TomlRightPadded<Toml>> elements = new ArrayList<>();
            for (int i = 0; i < values.size(); i++) {
                Toml element = visit(values.get(i));
                if (i == values.size() - 1) {
                    if (positionOfNext(",", '}') >= 0) {
                        elements.add(TomlRightPadded.build(element).withAfter(sourceBefore(",")));
                        elements.add(TomlRightPadded.build((Toml) new Toml.Empty(randomId(), Space.EMPTY, Markers.EMPTY)).withAfter(sourceBefore("}")));
                    } else {
                        elements.add(TomlRightPadded.build(element).withAfter(sourceBefore("}")));
                    }
                } else {
                    elements.add(TomlRightPadded.build(element).withAfter(sourceBefore(",")));
                }
            }

            return new Toml.Table(
                    randomId(),
                    prefix,
                    Markers.build(singletonList(new InlineTable(randomId()))),
                    null,
                    elements
            );
        });
    }

    @Override
    public Toml visitStandardTable(TomlParser.StandardTableContext ctx) {
        return convert(ctx, (c, prefix) -> {
            sourceBefore("[");
            Toml.Identifier tableName = visitKey(c.key());
            TomlRightPadded<Toml.Identifier> nameRightPadded = TomlRightPadded.build(tableName).withAfter(sourceBefore("]"));

            List<TomlParser.KeyValueContext> values = c.keyValue();
            List<TomlRightPadded<Toml>> elements = new ArrayList<>();
            for (int i = 0; i < values.size(); i++) {
                elements.add(TomlRightPadded.build(visit(values.get(i))));
            }

            return new Toml.Table(
                    randomId(),
                    prefix,
                    Markers.EMPTY,
                    nameRightPadded,
                    elements
            );
        });
    }

    @Override
    public Toml visitArrayTable(TomlParser.ArrayTableContext ctx) {
        return convert(ctx, (c, prefix) -> {
            sourceBefore("[[");
            Toml.Identifier tableName = visitKey(c.key());
            TomlRightPadded<Toml.Identifier> nameRightPadded = TomlRightPadded.build(tableName).withAfter(sourceBefore("]]"));

            List<TomlParser.KeyValueContext> values = c.keyValue();
            List<TomlRightPadded<Toml>> elements = new ArrayList<>();
            for (int i = 0; i < values.size(); i++) {
                elements.add(TomlRightPadded.build(visit(values.get(i))));
            }

            return new Toml.Table(
                    randomId(),
                    prefix,
                    Markers.build(singletonList(new ArrayTable(randomId()))),
                    nameRightPadded,
                    elements
            );
        });
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
        return Space.format(source, cursor, advanceCursor(start));
    }

    public int advanceCursor(int newCodePointIndex) {
        for (; codePointCursor < newCodePointIndex; codePointCursor++) {
            cursor = source.offsetByCodePoints(cursor, 1);
        }
        return cursor;
    }

    private <C extends ParserRuleContext, T> @Nullable T convert(C ctx, BiFunction<C, Space, T> conversion) {
        if (ctx == null) {
            return null;
        }

        T t = conversion.apply(ctx, prefix(ctx));
        if (ctx.getStop() != null) {
            advanceCursor(ctx.getStop().getStopIndex() + 1);
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

        Space space = Space.format(source, cursor, delimIndex);
        advanceCursor(codePointCursor + Character.codePointCount(source, cursor, delimIndex) + untilDelim.length());
        return space;
    }

    private int positionOfNext(String untilDelim, @Nullable Character stop) {
        boolean inSingleLineComment = false;

        int delimIndex = cursor;
        for (; delimIndex < source.length() - untilDelim.length() + 1; delimIndex++) {
            if (inSingleLineComment) {
                if (source.charAt(delimIndex) == '\n') {
                    inSingleLineComment = false;
                }
            } else {
                if (source.charAt(delimIndex) == '#') {
                    inSingleLineComment = true;
                    delimIndex++;
                }

                if (!inSingleLineComment) {
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
}
