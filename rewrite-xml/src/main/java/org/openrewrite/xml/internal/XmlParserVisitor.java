/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.xml.internal;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.openrewrite.Formatting;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.xml.internal.grammar.XMLParser;
import org.openrewrite.xml.internal.grammar.XMLParserBaseVisitor;
import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Misc;
import org.openrewrite.xml.tree.Xml;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.openrewrite.Tree.randomId;

public class XmlParserVisitor extends XMLParserBaseVisitor<Xml> {
    private final URI uri;
    private final String source;

    private int cursor = 0;

    public XmlParserVisitor(URI uri, String source) {
        this.uri = uri;
        this.source = source;
    }

    @Override
    public Xml.Document visitDocument(XMLParser.DocumentContext ctx) {
        Xml.Document d = convert(ctx, (c, format) -> new Xml.Document(
                randomId(),
                uri.toString(),
                emptyList(),
                visitProlog(ctx.prolog()),
                visitElement(ctx.element()),
                format)
        );
        return d == null ? null : d.withSuffix(source.substring(cursor));
    }

    @Override
    public Xml.Prolog visitProlog(XMLParser.PrologContext ctx) {
        return convert(ctx, (c, format) -> new Xml.Prolog(
                randomId(),
                visitXmldecl(ctx.xmldecl()),
                ctx.misc().stream().map(this::visit).map(Misc.class::cast).collect(toList()),
                format)
        );
    }

    @Override
    public Xml visitMisc(XMLParser.MiscContext ctx) {
        if (ctx.COMMENT() != null) {
            return convert(ctx.COMMENT(), (comment, format) -> new Xml.Comment(randomId(),
                    comment.getText().substring("<!--".length(), comment.getText().length() - "-->".length()),
                    format));
        } else {
            return super.visitMisc(ctx);
        }
    }

    @Override
    public Xml visitContent(XMLParser.ContentContext ctx) {
        if (ctx.CDATA() != null) {
            Xml.CharData charData = convert(ctx.CDATA(), (cdata, format) -> {
                Map.Entry<Formatting, String> formatAndText = charDataFormat(cdata.getText());
                return new Xml.CharData(randomId(),
                        true,
                        formatAndText.getValue().substring("<![CDATA[".length(), cdata.getText().length() - "]]>".length()),
                        formatAndText.getKey());
            });
            cursor++; // otherwise an off-by-one on cursor positioning for close tags?
            return charData;
        } else if (ctx.chardata() != null) {
            Xml.CharData charData = convert(ctx.chardata(), (chardata, format) -> {
                Map.Entry<Formatting, String> formatAndText = charDataFormat(chardata.getText());
                return new Xml.CharData(randomId(),
                        false,
                        formatAndText.getValue(),
                        formatAndText.getKey());
            });
            cursor++; // otherwise an off-by-one on cursor positioning for close tags?
            return charData;
        } else if (ctx.reference() != null && ctx.reference().EntityRef() != null) {
            cursor += ctx.reference().EntityRef().getSymbol().getStopIndex() + 1;
            return new Xml.CharData(randomId(),
                    false,
                    ctx.reference().EntityRef().getText(),
                    Formatting.EMPTY);
        }

        return super.visitContent(ctx);
    }

    private Map.Entry<Formatting, String> charDataFormat(String text) {
        boolean prefixDone = false;
        StringBuilder prefix = new StringBuilder();
        StringBuilder value = new StringBuilder();
        StringBuilder suffix = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {
            if (!prefixDone) {
                if (Character.isWhitespace(text.charAt(i))) {
                    prefix.append(text.charAt(i));
                } else {
                    prefixDone = true;
                    value.append(text.charAt(i));
                }
            } else {
                if (Character.isWhitespace(text.charAt(i))) {
                    suffix.append(text.charAt(i));
                } else {
                    suffix.setLength(0);
                }
                value.append(text.charAt(i));
            }
        }

        String valueStr = value.toString();
        Map<Formatting, String> charDataFormat = new HashMap<>(1);
        charDataFormat.put(Formatting.format(prefix.toString(), suffix.toString()),
                valueStr.substring(0, valueStr.length() - suffix.length()));
        return charDataFormat.entrySet().iterator().next();
    }

    @Override
    public Xml.ProcessingInstruction visitXmldecl(XMLParser.XmldeclContext ctx) {
        return convert(ctx, (c, format) -> {
                    cursor = ctx.SPECIAL_OPEN_XML().getSymbol().getStopIndex() + 1;
                    List<Xml.Attribute> attributes = ctx.attribute().stream()
                            .map(this::visitAttribute)
                            .collect(toList());
                    return new Xml.ProcessingInstruction(
                            randomId(),
                            "xml",
                            attributes,
                            format(ctx.getStop()).getPrefix(),
                            format
                    );
                }
        );
    }

    @Override
    public Xml.ProcessingInstruction visitProcessinginstruction(XMLParser.ProcessinginstructionContext ctx) {
        return convert(ctx, (c, format) -> {
                    String name = convert(ctx.Name(), (n, f) -> n.getText());
                    List<Xml.Attribute> attributes = ctx.attribute().stream()
                            .map(this::visitAttribute)
                            .collect(toList());
                    return new Xml.ProcessingInstruction(
                            randomId(),
                            name,
                            attributes,
                            format(ctx.getStop()).getPrefix(),
                            format
                    );
                }
        );
    }

    @Override
    public Xml.Tag visitElement(XMLParser.ElementContext ctx) {
        return convert(ctx, (c, format) -> {
                    String name = convert(ctx.Name(0), (n, f) -> n.getText());

                    List<Xml.Attribute> attributes = ctx.attribute().stream().map(this::visitAttribute).collect(toList());

                    List<Content> content = null;
                    String beforeTagDelimiterPrefix;
                    Xml.Tag.Closing closeTag = null;

                    if (ctx.SLASH_CLOSE() != null) {
                        beforeTagDelimiterPrefix = format(ctx.SLASH_CLOSE()).getPrefix();
                        cursor = ctx.SLASH_CLOSE().getSymbol().getStopIndex() + 1;
                    } else {
                        beforeTagDelimiterPrefix = format(ctx.CLOSE(0)).getPrefix();
                        cursor = ctx.CLOSE(0).getSymbol().getStopIndex() + 1;

                        content = ctx.content().stream()
                                .map(this::visit)
                                .map(Content.class::cast)
                                .collect(toList());

                        Formatting closeTagFormat = format(ctx.OPEN(1));
                        cursor += 2;

                        closeTag = new Xml.Tag.Closing(
                                randomId(),
                                convert(ctx.Name(1), (n, f) -> n.getText()),
                                format(ctx.CLOSE(1)).getPrefix(),
                                closeTagFormat
                        );
                        cursor++;
                    }

                    return new Xml.Tag(randomId(), name, attributes, content, closeTag, beforeTagDelimiterPrefix, format);
                }
        );
    }

    @Override
    public Xml.Attribute visitAttribute(XMLParser.AttributeContext ctx) {
        return convert(ctx, (c, format) -> {
            Xml.Ident key = convert(c.Name(), (t, f) -> new Xml.Ident(randomId(), t.getText(), f))
                    .withSuffix(convert(c.EQUALS(), (e, f) -> f.getPrefix()));

            Xml.Attribute.Value value = convert(c.STRING(), (v, f) -> new Xml.Attribute.Value(
                            randomId(),
                            v.getText().startsWith("'") ? Xml.Attribute.Value.Quote.Single : Xml.Attribute.Value.Quote.Double,
                            v.getText().substring(1, c.STRING().getText().length() - 1),
                            f
                    )
            );

            return new Xml.Attribute(randomId(), key, value, format);
        });
    }

    @Override
    public Xml.DocTypeDecl visitDoctypedecl(XMLParser.DoctypedeclContext ctx) {
        return convert(ctx, (c, format) -> {
            skip(c.DOCTYPE());
            Xml.Ident name = convert(c.Name(), (n, f) -> new Xml.Ident(randomId(), n.getText(), f));
            Xml.Ident externalId = convert(c.externalid(), (n, f) -> new Xml.Ident(randomId(), n.Name().getText(), f));
            List<Xml.Ident> internalSubset = c.STRING().stream().map(s -> convert(s, (attr, f) -> new Xml.Ident(randomId(), attr.getText(), f))).collect(toList());
            String beforeTagDelimiterPrefix = format(c.CLOSE()).getPrefix();
            return new Xml.DocTypeDecl(randomId(),
                    name,
                    externalId,
                    internalSubset,
                    null, // TODO implement me!
                    beforeTagDelimiterPrefix,
                    format);
        });
    }

    private Formatting format(ParserRuleContext ctx) {
        return format(ctx.getStart());
    }

    private Formatting format(TerminalNode terminalNode) {
        return format(terminalNode.getSymbol());
    }

    private Formatting format(Token token) {
        int start = token.getStartIndex();
        if (start < cursor) {
            return Formatting.EMPTY;
        }
        String prefix = source.substring(cursor, start);
        cursor = start;
        return Formatting.format(prefix);
    }

    @Nullable
    private <C extends ParserRuleContext, T> T convert(C ctx, BiFunction<C, Formatting, T> conversion) {
        if (ctx == null) {
            return null;
        }

        Formatting format = format(ctx);
        T t = conversion.apply(ctx, format);
        if (ctx.getStop() != null) {
            cursor = ctx.getStop().getStopIndex() + (Character.isWhitespace(source.charAt(ctx.getStop().getStopIndex())) ? 0 : 1);
        }

        return t;
    }

    private <T> T convert(TerminalNode node, BiFunction<TerminalNode, Formatting, T> conversion) {
        Formatting format = format(node);
        T t = conversion.apply(node, format);
        cursor = node.getSymbol().getStopIndex() + 1;
        return t;
    }

    private void skip(TerminalNode node) {
        cursor = node.getSymbol().getStopIndex() + 1;
    }
}
