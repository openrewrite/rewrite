package org.openrewrite.toml.internal;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.jspecify.annotations.Nullable;
import org.openrewrite.FileAttributes;
import org.openrewrite.internal.EncodingDetectingInputStream;
import org.openrewrite.marker.Markers;
import org.openrewrite.toml.internal.grammar.TomlParser;
import org.openrewrite.toml.internal.grammar.TomlParserBaseVisitor;
import org.openrewrite.toml.tree.Comment;
import org.openrewrite.toml.tree.Space;
import org.openrewrite.toml.tree.Toml;
import org.openrewrite.toml.tree.TomlValue;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static org.openrewrite.Tree.randomId;

public class TomlParserVisitor extends TomlParserBaseVisitor<Toml> {

    @Nullable
    private final FileAttributes fileAttributes;

    private final Path path;
    private final String source;
    private final Charset charset;
    private final boolean charsetBomMarked;
    private int cursor = 0;

    public TomlParserVisitor(Path path, @Nullable FileAttributes fileAttributes, EncodingDetectingInputStream source) {
        this.path = path;
        this.fileAttributes = fileAttributes;
        this.source = source.readFully();
        this.charset = source.getCharset();
        this.charsetBomMarked = source.isCharsetBomMarked();
    }

    @Override
    public Toml.Document visitDocument(TomlParser.DocumentContext ctx) {
        return convert(ctx, (c, prefix) -> new Toml.Document(
                randomId(),
                path,
                prefix,
                Markers.EMPTY,
                charset.name(),
                charsetBomMarked,
                null,
                fileAttributes,
                c.expression().stream().map(this::visitExpression).collect(Collectors.toList()),
                Space.format(source.substring(cursor))
        ));
    }

    @Override
    public Toml.Expression visitExpression(TomlParser.ExpressionContext ctx) {
        return convert(ctx, (c, prefix) -> {
            TomlValue value = null;
            if (c.key_value() != null) {
                value = visitKeyValue(c.key_value());
            } else if (c.table() != null) {
                value = visitTable(c.table());
            }
            return new Toml.Expression(
                    randomId(),
                    prefix,
                    Markers.EMPTY,
                    value,
                    new Comment(c.comment().getText(), null, Markers.EMPTY)
            );
        });
    }

    @Override
    public Toml.KeyValue visitKeyValue(TomlParser.Key_valueContext ctx) {
        return convert(ctx, (c, prefix) -> new Toml.KeyValue(
                randomId(),
                prefix,
                Markers.EMPTY,
                visitKey(c.key()),
                visitValue(c.value())
        ));
    }

    @Override
    public Toml.Key visitKey(TomlParser.KeyContext ctx) {
        return convert(ctx, (c, prefix) -> new Toml.Key(
                randomId(),
                prefix,
                Markers.EMPTY,
                c.getText()
        ));
    }

    @Override
    public Toml.Literal visitValue(TomlParser.ValueContext ctx) {
        return convert(ctx, (c, prefix) -> {
            Object value = null;
            if (c.string() != null) {
                value = c.string();
            } else if (c.integer() != null) {
                value = c.integer();
            } else if (c.floating_point() != null) {
                value = c.floating_point();
            } else if (c.bool_() != null) {
                value = c.bool_();
            } else if (c.date_time() != null) {
                value = c.date_time();
            } else if (c.array_() != null) {
                value = c.array_();
            } else if (c.inline_table() != null) {
                value = c.inline_table();
            }
            return new Toml.Literal(
                    randomId(),
                    prefix,
                    Markers.EMPTY,
                    c.getText(),
                    value
            );
        });
    }

    @Override
    public Toml visitArray_(TomlParser.Array_Context ctx) {
        return super.visitArray_(ctx);
    }

    @Override
    public Toml visitArray_values(TomlParser.Array_valuesContext ctx) {
        return super.visitArray_values(ctx);
    }

    @Override
    public Toml visitComment_or_nl(TomlParser.Comment_or_nlContext ctx) {
        return super.visitComment_or_nl(ctx);
    }

    @Override
    public Toml visitNl_or_comment(TomlParser.Nl_or_commentContext ctx) {
        return super.visitNl_or_comment(ctx);
    }

    @Override
    public Toml.Table visitTable(TomlParser.TableContext ctx) {
        return (Toml.Table) super.visitTable(ctx);
    }

    @Override
    public Toml visitStandard_table(TomlParser.Standard_tableContext ctx) {
        return super.visitStandard_table(ctx);
    }

    @Override
    public Toml visitInline_table(TomlParser.Inline_tableContext ctx) {
        return super.visitInline_table(ctx);
    }

    @Override
    public Toml visitInline_table_keyvals(TomlParser.Inline_table_keyvalsContext ctx) {
        return super.visitInline_table_keyvals(ctx);
    }

    @Override
    public Toml visitInline_table_keyvals_non_empty(TomlParser.Inline_table_keyvals_non_emptyContext ctx) {
        return super.visitInline_table_keyvals_non_empty(ctx);
    }

    @Override
    public Toml visitArray_table(TomlParser.Array_tableContext ctx) {
        return super.visitArray_table(ctx);
    }

    private Space prefix(ParserRuleContext ctx) {
        return prefix(ctx.getStart());
    }

    private Space prefix(@Nullable TerminalNode terminalNode) {
        return terminalNode == null ? Space.EMPTY : prefix(terminalNode.getSymbol());
    }

    private Space prefix(Token token) {
        int start = token.getStartIndex();
        if (start < cursor) {
            return Space.EMPTY;
        }
        String prefix = source.substring(cursor, start);
        cursor = start;
        return Space.format(prefix);
    }

    private <C extends ParserRuleContext, T> @Nullable T convert(C ctx, BiFunction<C, Space, T> conversion) {
        if (ctx == null) {
            return null;
        }

        T t = conversion.apply(ctx, prefix(ctx));
        if (ctx.getStop() != null) {
            cursor = ctx.getStop().getStopIndex() + (Character.isWhitespace(source.charAt(ctx.getStop().getStopIndex())) ? 0 : 1);
        }

        return t;
    }

    private <T> T convert(TerminalNode node, BiFunction<TerminalNode, Space, T> conversion) {
        T t = conversion.apply(node, prefix(node));
        cursor = node.getSymbol().getStopIndex() + 1;
        return t;
    }

    private void skip(TerminalNode node) {
        cursor = node.getSymbol().getStopIndex() + 1;
    }
}
