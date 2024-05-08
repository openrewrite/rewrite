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
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.openrewrite.FileAttributes;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Markers;
import org.openrewrite.xml.internal.grammar.XMLParser;
import org.openrewrite.xml.internal.grammar.XMLParserBaseVisitor;
import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Misc;
import org.openrewrite.xml.tree.Xml;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.openrewrite.Tree.randomId;

@SuppressWarnings("ConstantConditions")
public class XmlParserVisitor extends XMLParserBaseVisitor<Xml> {
    private final Path path;

    @Nullable
    private final FileAttributes fileAttributes;
    private final String source;
    private final Charset charset;
    private final boolean charsetBomMarked;

    private int cursor = 0;

    public XmlParserVisitor(Path path, @Nullable FileAttributes fileAttributes, String source, Charset charset, boolean charsetBomMarked) {
        this.path = path;
        this.fileAttributes = fileAttributes;
        this.source = source;
        this.charset = charset;
        this.charsetBomMarked = charsetBomMarked;
    }

    @Override
    public Xml.Document visitDocument(XMLParser.DocumentContext ctx) {
        return convert(ctx, (c, prefix) -> new Xml.Document(
                randomId(),
                path,
                prefix,
                Markers.EMPTY,
                charset.name(),
                charsetBomMarked,
                null,
                fileAttributes,
                visitProlog(ctx.prolog()),
                visitElement(ctx.element()),
                source.substring(cursor))
        );
    }

    @Override
    public Xml.Prolog visitProlog(XMLParser.PrologContext ctx) {
        return convert(ctx, (c, prefix) -> new Xml.Prolog(
                randomId(),
                prefix,
                Markers.EMPTY,
                visitXmldecl(ctx.xmldecl()),
                ctx.misc().stream().map(this::visit).map(Misc.class::cast).collect(toList()),
                ctx.jspdirective().stream().map(this::visit).map(Xml.JspDirective.class::cast).collect(toList()))
        );
    }

    @Override
    public Xml visitMisc(XMLParser.MiscContext ctx) {
        if (ctx.COMMENT() != null) {
            return convert(ctx.COMMENT(), (comment, prefix) -> new Xml.Comment(randomId(),
                    prefix,
                    Markers.EMPTY,
                    comment.getText().substring("<!--".length(), comment.getText().length() - "-->".length())));
        } else {
            return super.visitMisc(ctx);
        }
    }

    @Override
    public Xml visitContent(XMLParser.ContentContext ctx) {
        if (ctx.CDATA() != null) {
            return convert(ctx.CDATA(), (cdata, prefix) ->
                    charData(cdata.getText(), true, prefix));
        } else if (ctx.chardata() != null) {
            Xml.CharData charData = convert(ctx.chardata(), (chardata, prefix) ->
                    charData(chardata.getText(), false, prefix));
            cursor++; // otherwise an off-by-one on cursor positioning for close tags?
            return charData;
        } else if (ctx.reference() != null) {
            if (ctx.reference().EntityRef() != null) {
                String prefix = prefix(ctx);
                cursor = ctx.reference().EntityRef().getSymbol().getStopIndex() + 1;
                return new Xml.CharData(randomId(),
                        prefix,
                        Markers.EMPTY,
                        false,
                        ctx.reference().EntityRef().getText(),
                        "");
            } else if (ctx.reference().CharRef() != null) {
                String prefix = prefix(ctx);
                cursor = ctx.reference().CharRef().getSymbol().getStopIndex() + 1;
                return new Xml.CharData(randomId(),
                        prefix,
                        Markers.EMPTY,
                        false,
                        ctx.reference().CharRef().getText(),
                        "");
            }
        } else if (ctx.COMMENT() != null) {
            return convert(ctx.COMMENT(), (comment, prefix) -> new Xml.Comment(randomId(),
                    prefix,
                    Markers.EMPTY,
                    comment.getText().substring("<!--".length(), comment.getText().length() - "-->".length())));
        }

        return super.visitContent(ctx);
    }

    private Xml.CharData charData(String text, boolean cdata, String prefix) {
        boolean prefixDone = false;
        StringBuilder newPrefix = new StringBuilder(prefix);
        StringBuilder value = new StringBuilder();
        StringBuilder suffix = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {
            if (!prefixDone) {
                if (Character.isWhitespace(text.charAt(i))) {
                    newPrefix.append(text.charAt(i));
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
        valueStr = valueStr.substring(0, valueStr.length() - suffix.length());

        return new Xml.CharData(randomId(),
                newPrefix.toString(),
                Markers.EMPTY,
                cdata,
                cdata ?
                        valueStr.substring("<![CDATA[".length(), text.length() - "]]>".length()) :
                        valueStr,
                suffix.toString());
    }

    @Override
    public Xml.XmlDecl visitXmldecl(XMLParser.XmldeclContext ctx) {
        return convert(ctx, (c, prefix) -> {
                    cursor = ctx.SPECIAL_OPEN_XML().getSymbol().getStopIndex() + 1;
                    String name = convert(ctx.SPECIAL_OPEN_XML(), (n, p) -> n.getText()).substring(2);
                    List<Xml.Attribute> attributes = ctx.attribute().stream()
                            .map(this::visitAttribute)
                            .collect(toList());
                    return new Xml.XmlDecl(
                            randomId(),
                            prefix,
                            Markers.EMPTY,
                            name,
                            attributes,
                            prefix(ctx.getStop())
                    );
                }
        );
    }

    @Override
    public Xml.ProcessingInstruction visitProcessinginstruction(XMLParser.ProcessinginstructionContext ctx) {
        return convert(ctx, (c, prefix) -> {
                    String name = convert(ctx.SPECIAL_OPEN(), (n, p) -> n.getText()).substring(2);

                    List<Xml.CharData> piTexts = c.PI_TEXT().stream()
                            .map(piText -> convert(piText, (cdata, p) -> charData(cdata.getText(), false, p)))
                            .collect(toList());
                    Xml.CharData piText = piTexts.get(0);
                    if (piTexts.size() > 1) {
                        StringBuilder sb = new StringBuilder();
                        piTexts.forEach(it -> sb.append(it.getText()));
                        piText = piText.withText(sb.toString());
                    }

                    return new Xml.ProcessingInstruction(
                            randomId(),
                            prefix,
                            Markers.EMPTY,
                            name,
                            piText,
                            prefix(ctx.getStop())
                    );
                }
        );
    }

    @Override
    public Xml visitJspdirective(XMLParser.JspdirectiveContext ctx) {
        return convert(ctx, (c, prefix) -> {
            cursor = ctx.DIRECTIVE_OPEN().getSymbol().getStopIndex() + 1;
            String beforeType = prefix(ctx.Name());
            String type = convert(ctx.Name(), (n, p) -> n.getText());
            List<Xml.Attribute> attributes = ctx.attribute().stream().map(this::visitAttribute).collect(toList());

            return new Xml.JspDirective(
                    randomId(),
                    prefix,
                    Markers.EMPTY,
                    beforeType,
                    type,
                    attributes,
                    prefix(ctx.DIRECTIVE_CLOSE())
            );
        });
    }

    @Override
    public Xml.Tag visitElement(XMLParser.ElementContext ctx) {
        return convert(ctx, (c, prefix) -> {
                    String name = convert(ctx.Name(0), (n, p) -> n.getText());

                    List<Xml.Attribute> attributes = ctx.attribute().stream().map(this::visitAttribute).collect(toList());

                    List<Content> content = null;
                    String beforeTagDelimiterPrefix;
                    Xml.Tag.Closing closeTag = null;

                    if (ctx.SLASH_CLOSE() != null) {
                        beforeTagDelimiterPrefix = prefix(ctx.SLASH_CLOSE());
                        cursor = ctx.SLASH_CLOSE().getSymbol().getStopIndex() + 1;
                    } else {
                        beforeTagDelimiterPrefix = prefix(ctx.CLOSE(0));
                        cursor = ctx.CLOSE(0).getSymbol().getStopIndex() + 1;

                        content = ctx.content().stream()
                                .map(this::visit)
                                .map(Content.class::cast)
                                .collect(toList());

                        String closeTagPrefix = prefix(ctx.OPEN(1));
                        cursor += 2;

                        closeTag = new Xml.Tag.Closing(
                                randomId(),
                                closeTagPrefix,
                                Markers.EMPTY,
                                convert(ctx.Name(1), (n, p) -> n.getText()),
                                prefix(ctx.CLOSE(1))
                        );
                        cursor++;
                    }

                    return new Xml.Tag(randomId(), prefix, Markers.EMPTY, name, attributes,
                            content, closeTag, beforeTagDelimiterPrefix);
                }
        );
    }

    @Override
    public Xml.Attribute visitAttribute(XMLParser.AttributeContext ctx) {
        return convert(ctx, (c, prefix) -> {
            Xml.Ident key = convert(c.Name(), (t, p) -> new Xml.Ident(randomId(), p, Markers.EMPTY, t.getText()));

            String beforeEquals = convert(c.EQUALS(), (e, p) -> p);

            Xml.Attribute.Value value = convert(c.STRING(), (v, p) -> new Xml.Attribute.Value(
                            randomId(),
                            p,
                            Markers.EMPTY,
                            v.getText().startsWith("'") ? Xml.Attribute.Value.Quote.Single : Xml.Attribute.Value.Quote.Double,
                            v.getText().substring(1, c.STRING().getText().length() - 1)
                    )
            );

            return new Xml.Attribute(randomId(), prefix, Markers.EMPTY, key, beforeEquals, value);
        });
    }

    @Override
    public Xml.DocTypeDecl visitDoctypedecl(XMLParser.DoctypedeclContext ctx) {
        return convert(ctx, (c, prefix) -> {
            skip(c.DOCTYPE());
            Xml.Ident name = convert(c.Name(), (n, p) -> new Xml.Ident(randomId(), p, Markers.EMPTY, n.getText()));
            Xml.Ident externalId = null;
            List<Xml.Ident> internalSubset = null;
            if (!c.externalid().getStart().equals(c.DTD_CLOSE().getSymbol())) {
                if (c.externalid().Name() != null) {
                    externalId = convert(c.externalid(),
                            (n, p) -> new Xml.Ident(randomId(), p, Markers.EMPTY, n.Name().getText()));
                }
                internalSubset = c.STRING().stream()
                        .map(s -> convert(s, (attr, p) -> new Xml.Ident(randomId(), p, Markers.EMPTY, attr.getText())))
                        .collect(toList());
            }

            Xml.DocTypeDecl.ExternalSubsets externalSubsets = null;
            if (c.intsubset() != null) {
                String subsetPrefix = prefix(c.DTD_SUBSET_OPEN());
                cursor = c.DTD_SUBSET_OPEN().getSymbol().getStopIndex() + 1;

                List<Xml.Element> elements = new ArrayList<>();
                List<ParseTree> children = c.intsubset().children;
                for (int i = 0; i < children.size(); i++) {
                    ParserRuleContext element = (ParserRuleContext) children.get(i);
                    // Markup declarations are not fully implemented.
                    // n.getText() includes element subsets.
                    Xml.Ident ident = convert(element, (n, p) -> new Xml.Ident(randomId(), p, Markers.EMPTY, n.getText()));

                    String beforeElementTag = "";
                    if (i == children.size() - 1) {
                        beforeElementTag = prefix(c.DTD_SUBSET_CLOSE());
                        cursor = c.DTD_SUBSET_CLOSE().getSymbol().getStopIndex() + 1;
                    }

                    elements.add(
                            new Xml.Element(
                                    randomId(),
                                    prefix(element),
                                    Markers.EMPTY,
                                    Collections.singletonList(ident),
                                    beforeElementTag));
                }
                externalSubsets = new Xml.DocTypeDecl.ExternalSubsets(randomId(), subsetPrefix, Markers.EMPTY, elements);
            }

            String beforeTagDelimiterPrefix = prefix(c.DTD_CLOSE());
            return new Xml.DocTypeDecl(randomId(),
                    prefix,
                    Markers.EMPTY,
                    name,
                    externalId,
                    internalSubset == null ? emptyList() : internalSubset,
                    externalSubsets,
                    beforeTagDelimiterPrefix);
        });
    }

    private String prefix(ParserRuleContext ctx) {
        return prefix(ctx.getStart());
    }

    private String prefix(@Nullable TerminalNode terminalNode) {
        return terminalNode == null ? "" : prefix(terminalNode.getSymbol());
    }

    private String prefix(Token token) {
        int start = token.getStartIndex();
        if (start < cursor) {
            return "";
        }
        String prefix = source.substring(cursor, start);
        cursor = start;
        return prefix;
    }

    @Nullable
    private <C extends ParserRuleContext, T> T convert(C ctx, BiFunction<C, String, T> conversion) {
        if (ctx == null) {
            return null;
        }

        T t = conversion.apply(ctx, prefix(ctx));
        if (ctx.getStop() != null) {
            cursor = ctx.getStop().getStopIndex() + (Character.isWhitespace(source.charAt(ctx.getStop().getStopIndex())) ? 0 : 1);
        }

        return t;
    }

    private <T> T convert(TerminalNode node, BiFunction<TerminalNode, String, T> conversion) {
        T t = conversion.apply(node, prefix(node));
        cursor = node.getSymbol().getStopIndex() + 1;
        return t;
    }

    private void skip(TerminalNode node) {
        cursor = node.getSymbol().getStopIndex() + 1;
    }
}
