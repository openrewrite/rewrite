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
package org.openrewrite.json.internal;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.openrewrite.FileAttributes;
import org.openrewrite.internal.EncodingDetectingInputStream;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.json.internal.grammar.JSON5BaseVisitor;
import org.openrewrite.json.internal.grammar.JSON5Parser;
import org.openrewrite.json.tree.*;
import org.openrewrite.marker.Markers;

import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

import static org.openrewrite.Tree.randomId;

@SuppressWarnings("ConstantConditions")
public class JsonParserVisitor extends JSON5BaseVisitor<Json> {
    private final Path path;
    private final String source;
    private final Charset charset;
    private final boolean charsetBomMarked;

    @Nullable
    private final FileAttributes fileAttributes;

    private int cursor = 0;
    private int codePointCursor = 0;

    public JsonParserVisitor(Path path, @Nullable FileAttributes fileAttributes, EncodingDetectingInputStream source) {
        this.path = path;
        this.fileAttributes = fileAttributes;
        this.source = source.readFully();
        this.charset = source.getCharset();
        this.charsetBomMarked = source.isCharsetBomMarked();
    }

    @Override
    public Json visitArr(JSON5Parser.ArrContext ctx) {
        return convert(ctx, (arr, prefix) -> {
            sourceBefore("[");
            List<JsonRightPadded<JsonValue>> converted = new ArrayList<>(ctx.value().size());
            for (int i = 0; i < ctx.value().size(); i++) {
                JSON5Parser.ValueContext value = ctx.value().get(i);
                if (i == ctx.value().size() - 1) {
                    JsonRightPadded<JsonValue> unpadded = JsonRightPadded.build((JsonValue) visit(value));
                    if (positionOfNext(",", ']') >= 0) {
                        converted.add(unpadded.withAfter(sourceBefore(",")));
                        converted.add(JsonRightPadded.build((JsonValue) new Json.Empty(randomId(), Space.EMPTY, Markers.EMPTY))
                                .withAfter(sourceBefore("]")));
                    } else {
                        converted.add(unpadded.withAfter(sourceBefore("]")));
                    }
                } else {
                    converted.add(JsonRightPadded.build((JsonValue) visit(value))
                            .withAfter(sourceBefore(",")));
                }
            }

            if (ctx.value().isEmpty()) {
                converted.add(JsonRightPadded.build((JsonValue) new Json.Empty(randomId(), Space.EMPTY, Markers.EMPTY))
                        .withAfter(sourceBefore("]")));
            }

            return new Json.Array(randomId(), prefix, Markers.EMPTY, converted);
        });
    }

    @Override
    public Json.Document visitJson5(JSON5Parser.Json5Context ctx) {
        return !ctx.children.isEmpty() && "<EOF>".equals(ctx.children.get(0).getText()) ? new Json.Document(
                randomId(),
                path,
                Space.EMPTY,
                Markers.EMPTY,
                charset.name(),
                charsetBomMarked,
                null,
                fileAttributes,
                new Json.Literal(randomId(), Space.EMPTY, Markers.EMPTY, source, ""),
                Space.EMPTY
        ) : convert(ctx, (c, prefix) -> new Json.Document(
                randomId(),
                path,
                prefix,
                Markers.EMPTY,
                charset.name(),
                charsetBomMarked,
                null,
                fileAttributes,
                visitValue(c.value()),
                Space.format(source.substring(cursor))
        ));
    }

    @Override
    public JsonKey visitKey(JSON5Parser.KeyContext ctx) {
        if (ctx.IDENTIFIER() != null) {
            return convert(ctx.IDENTIFIER(), (ident, fmt) ->
                    new Json.Identifier(randomId(), fmt, Markers.EMPTY, ctx.IDENTIFIER().getText()));
        }

        return convert(ctx.STRING(), (str, prefix) -> {
            String source = str.getText();
            return new Json.Literal(randomId(), prefix, Markers.EMPTY, source,
                    source.substring(1, source.length() - 1));
        });
    }

    @Override
    public Json.Member visitMember(JSON5Parser.MemberContext ctx) {
        return convert(ctx, (member, prefix) -> new Json.Member(
                randomId(),
                prefix,
                Markers.EMPTY,
                JsonRightPadded.build(visitKey(ctx.key())).withAfter(sourceBefore(":")),
                visitValue(ctx.value())
        ));
    }

    @Override
    public Json visitObj(JSON5Parser.ObjContext ctx) {
        return convert(ctx, (arr, prefix) -> {
            sourceBefore("{");
            List<JsonRightPadded<Json>> converted = new ArrayList<>(ctx.member().size());
            for (int i = 0; i < ctx.member().size(); i++) {
                JSON5Parser.MemberContext member = ctx.member().get(i);
                if (i == ctx.member().size() - 1) {
                    JsonRightPadded<Json> unpadded = JsonRightPadded.build(visit(member));
                    if (positionOfNext(",", '}') >= 0) {
                        converted.add(unpadded.withAfter(sourceBefore(",")));
                        converted.add(JsonRightPadded.build((Json) new Json.Empty(randomId(), Space.EMPTY, Markers.EMPTY))
                                .withAfter(sourceBefore("}")));
                    } else {
                        converted.add(unpadded.withAfter(sourceBefore("}")));
                    }
                } else {
                    converted.add(JsonRightPadded.build(visit(member))
                            .withAfter(sourceBefore(",")));
                }
            }

            if (ctx.member().isEmpty()) {
                converted.add(JsonRightPadded.build((Json) new Json.Empty(randomId(), Space.EMPTY, Markers.EMPTY))
                        .withAfter(sourceBefore("}")));
            }

            return new Json.JsonObject(randomId(), prefix, Markers.EMPTY, converted);
        });
    }

    @Override
    public Json.Literal visitNumber(JSON5Parser.NumberContext ctx) {
        AtomicReference<Space> prefix = new AtomicReference<>();
        StringBuilder source = new StringBuilder();
        AtomicInteger sign = new AtomicInteger(1);

        if (ctx.SYMBOL() != null) {
            convert(ctx.SYMBOL(), (sym, fmt) -> {
                source.append(ctx.SYMBOL().getText());
                if ("-".equals(ctx.SYMBOL().getText())) {
                    sign.set(-1);
                }
                prefix.set(fmt);
                return null;
            });
        }

        Number value;

        if (ctx.NUMERIC_LITERAL() != null) {
            if (prefix.get() == null) {
                prefix.set(sourceBefore(ctx.NUMERIC_LITERAL().getText()));
            } else {
                skip(ctx.NUMERIC_LITERAL());
            }
            source.append(ctx.NUMERIC_LITERAL().getText());
            if ("Infinity".equals(ctx.NUMERIC_LITERAL().getText())) {
                value = sign.get() == 1 ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
            } else {
                value = Double.NaN;
            }
        } else {
            if (prefix.get() == null) {
                prefix.set(sourceBefore(ctx.NUMBER().getText()));
            } else {
                skip(ctx.NUMBER());
            }
            String text = ctx.NUMBER().getText();
            source.append(text);
            if (text.startsWith("0x")) {
                value = Long.decode(text) * sign.get();
            } else if (text.contains(".") || text.contains("e") || text.contains("E")) {
                value = Double.parseDouble(text) * sign.get();
            } else {
                try {
                    value = Integer.parseInt(text) * sign.get();
                } catch (NumberFormatException e) {
                    try {
                        value = Long.parseLong(text) * sign.get();
                    } catch (NumberFormatException e1) {
                        value = sign.get() == 1 ? new BigInteger(text, 10) : new BigInteger("-" + text, 10);
                    }
                }
            }
        }

        return new Json.Literal(randomId(), prefix.get(), Markers.EMPTY, source.toString(), value);
    }

    @Override
    public JsonValue visitValue(JSON5Parser.ValueContext ctx) {
        if (ctx.STRING() != null) {
            return convert(ctx.STRING(), (str, prefix) -> {
                String source = str.getText();
                return new Json.Literal(randomId(), prefix, Markers.EMPTY, source,
                        source.substring(1, source.length() - 1));
            });
        } else if (ctx.LITERAL() != null) {
            return convert(ctx.LITERAL(), (literal, prefix) -> {
                String source = literal.getText();
                Object value = null;
                if ("true".equals(source)) {
                    value = true;
                } else if ("false".equals(source)) {
                    value = false;
                }
                return new Json.Literal(randomId(), prefix, Markers.EMPTY, source, value);
            });
        }

        // visit numbers
        return (JsonValue) super.visitValue(ctx);
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
        String prefix = source.substring(cursor, advanceCursor(start));
        return Space.format(prefix);
    }

    public int advanceCursor(int newCodePointIndex) {
        for (; codePointCursor < newCodePointIndex; codePointCursor++) {
            cursor = source.offsetByCodePoints(cursor, 1);
        }
        return cursor;
    }

    @Nullable
    private <C extends ParserRuleContext, T> T convert(C ctx, BiFunction<C, Space, T> conversion) {
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

    private void skip(TerminalNode node) {
        advanceCursor(node.getSymbol().getStopIndex() + 1);
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
        advanceCursor(codePointCursor + Character.codePointCount(prefix, 0, prefix.length()) + untilDelim.length());
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
                            inSingleLineComment = true;
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
}
