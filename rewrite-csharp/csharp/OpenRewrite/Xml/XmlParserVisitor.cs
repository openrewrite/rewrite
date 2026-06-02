/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
using Antlr4.Runtime;
using Antlr4.Runtime.Tree;
using OpenRewrite.Core;
using OpenRewrite.Xml.Grammar;
using FileAttributes = OpenRewrite.Core.FileAttributes;

namespace OpenRewrite.Xml;

/// <summary>
/// Converts ANTLR parse tree into OpenRewrite XML LST.
/// Direct port of Java's org.openrewrite.xml.internal.XmlParserVisitor.
/// </summary>
internal class XmlParserVisitor : XMLParserBaseVisitor<Xml>
{
    private readonly string _path;
    private readonly FileAttributes? _fileAttributes;
    private readonly string _source;
    private readonly string _charsetName;
    private readonly bool _charsetBomMarked;

    /// <summary>
    /// Track position within the file by char index.
    /// In C#, ANTLR token StartIndex/StopIndex are UTF-16 char indices,
    /// matching C# string indexing directly — no code point conversion needed.
    /// </summary>
    private int _cursor;

    public XmlParserVisitor(string path, FileAttributes? fileAttributes, string source, string charsetName,
        bool charsetBomMarked)
    {
        _path = path;
        _fileAttributes = fileAttributes;
        _source = source;
        _charsetName = charsetName;
        _charsetBomMarked = charsetBomMarked;
    }

    public override Xml VisitDocument(XMLParser.DocumentContext ctx)
    {
        return Convert(ctx, (c, prefix) => new Document(
            Guid.NewGuid(),
            _path,
            prefix,
            Markers.Empty,
            _charsetName,
            _charsetBomMarked,
            null,
            _fileAttributes,
            (Prolog?)VisitProlog(ctx.prolog()),
            (Tag)VisitElement(ctx.element()),
            _source.Substring(_cursor))
        )!;
    }

    public override Xml VisitProlog(XMLParser.PrologContext ctx)
    {
        return Convert(ctx, (c, prefix) => new Prolog(
            Guid.NewGuid(),
            prefix,
            Markers.Empty,
            (XmlDecl?)VisitXmldecl(ctx.xmldecl()),
            c.misc().Select(m => (Misc)Visit(m)).ToList(),
            c.jspdirective().Select(j => (JspDirective)Visit(j)).ToList())
        )!;
    }

    public override Xml VisitMisc(XMLParser.MiscContext ctx)
    {
        if (ctx.COMMENT() != null)
        {
            return Convert(ctx.COMMENT(), (comment, prefix) => new Comment(
                Guid.NewGuid(),
                prefix,
                Markers.Empty,
                comment.GetText().Substring("<!--".Length, comment.GetText().Length - "<!--".Length - "-->".Length)));
        }
        else if (ctx.jspdeclaration() != null)
        {
            return VisitJspdeclaration(ctx.jspdeclaration());
        }
        else if (ctx.jspcomment() != null)
        {
            return VisitJspcomment(ctx.jspcomment());
        }
        else
        {
            return base.VisitMisc(ctx);
        }
    }

    public override Xml VisitContent(XMLParser.ContentContext ctx)
    {
        if (ctx.CDATA() != null)
        {
            return Convert(ctx.CDATA(), (cdata, prefix) =>
                CreateCharData(cdata.GetText(), true, prefix));
        }
        else if (ctx.chardata() != null)
        {
            var charData = Convert(ctx.chardata(), (chardata, prefix) =>
                CreateCharData(chardata.GetText(), false, prefix));
            // Avoid off-by-one on cursor positioning error for closing tags
            AdvanceCursor(_cursor + 1);
            return charData;
        }
        else if (ctx.reference() != null)
        {
            if (ctx.reference().EntityRef() != null)
            {
                var prefix = Prefix(ctx);
                AdvanceCursor(ctx.reference().EntityRef().Symbol.StopIndex + 1);
                return new CharData(Guid.NewGuid(),
                    prefix,
                    Markers.Empty,
                    false,
                    ctx.reference().EntityRef().GetText(),
                    "");
            }
            else if (ctx.reference().CharRef() != null)
            {
                var prefix = Prefix(ctx);
                AdvanceCursor(ctx.reference().CharRef().Symbol.StopIndex + 1);
                return new CharData(Guid.NewGuid(),
                    prefix,
                    Markers.Empty,
                    false,
                    ctx.reference().CharRef().GetText(),
                    "");
            }
        }
        else if (ctx.COMMENT() != null)
        {
            return Convert(ctx.COMMENT(), (comment, prefix) => new Comment(
                Guid.NewGuid(),
                prefix,
                Markers.Empty,
                comment.GetText().Substring("<!--".Length, comment.GetText().Length - "<!--".Length - "-->".Length)));
        }

        return base.VisitContent(ctx);
    }

    private CharData CreateCharData(string text, bool cdata, string prefix)
    {
        var prefixDone = false;
        var newPrefix = new System.Text.StringBuilder(prefix);
        var value = new System.Text.StringBuilder();
        var suffix = new System.Text.StringBuilder();

        for (var i = 0; i < text.Length; i++)
        {
            if (!prefixDone)
            {
                if (char.IsWhiteSpace(text[i]))
                {
                    newPrefix.Append(text[i]);
                }
                else
                {
                    prefixDone = true;
                    value.Append(text[i]);
                }
            }
            else
            {
                if (char.IsWhiteSpace(text[i]))
                {
                    suffix.Append(text[i]);
                }
                else
                {
                    suffix.Clear();
                }
                value.Append(text[i]);
            }
        }

        var valueStr = value.ToString();
        valueStr = valueStr.Substring(0, valueStr.Length - suffix.Length);

        return new CharData(Guid.NewGuid(),
            newPrefix.ToString(),
            Markers.Empty,
            cdata,
            cdata
                ? valueStr.Substring("<![CDATA[".Length, text.Length - "<![CDATA[".Length - "]]>".Length)
                : valueStr,
            suffix.ToString());
    }

    public override Xml VisitXmldecl(XMLParser.XmldeclContext ctx)
    {
        return Convert(ctx, (c, prefix) =>
        {
            AdvanceCursor(ctx.SPECIAL_OPEN_XML().Symbol.StopIndex + 1);
            var name = Convert(ctx.SPECIAL_OPEN_XML(), (n, p) => n.GetText()).Substring(2);
            var attributes = ctx.attribute().Select(a => (Attribute)VisitAttribute(a)).ToList();
            return new XmlDecl(
                Guid.NewGuid(),
                prefix,
                Markers.Empty,
                name,
                attributes,
                Prefix(ctx.Stop)
            );
        })!;
    }

    public override Xml VisitProcessinginstruction(XMLParser.ProcessinginstructionContext ctx)
    {
        return Convert(ctx, (c, prefix) =>
        {
            var name = Convert(ctx.SPECIAL_OPEN(), (n, p) => n.GetText()).Substring(2);

            var piTexts = c.PI_TEXT()
                .Select(piText => Convert(piText, (cdata, p) => CreateCharData(cdata.GetText(), false, p)))
                .ToList();
            var piText = piTexts[0];
            if (piTexts.Count > 1)
            {
                var sb = new System.Text.StringBuilder();
                foreach (var it in piTexts) sb.Append(it.Text);
                piText = piText.WithText(sb.ToString());
            }

            return new ProcessingInstruction(
                Guid.NewGuid(),
                prefix,
                Markers.Empty,
                name,
                piText,
                Prefix(ctx.Stop)
            );
        })!;
    }

    public override Xml VisitJspdirective(XMLParser.JspdirectiveContext ctx)
    {
        return Convert(ctx, (c, prefix) =>
        {
            AdvanceCursor(ctx.DIRECTIVE_OPEN().Symbol.StopIndex + 1);
            var beforeType = Prefix(ctx.Name());
            var type = Convert(ctx.Name(), (n, p) => n.GetText());
            var attributes = ctx.attribute().Select(a => (Attribute)VisitAttribute(a)).ToList();

            return new JspDirective(
                Guid.NewGuid(),
                prefix,
                Markers.Empty,
                beforeType,
                type,
                attributes,
                Prefix(ctx.DIRECTIVE_CLOSE())
            );
        })!;
    }

    public override Xml VisitJspscriptlet(XMLParser.JspscriptletContext ctx)
    {
        return Convert(ctx, (c, prefix) =>
        {
            var scriptletText = ctx.JSP_SCRIPTLET().GetText();
            // Extract content between <% and %>, preserving all whitespace
            var content = scriptletText.Substring(2, scriptletText.Length - 4);

            return new JspScriptlet(
                Guid.NewGuid(),
                prefix,
                Markers.Empty,
                content
            );
        })!;
    }

    public override Xml VisitJspexpression(XMLParser.JspexpressionContext ctx)
    {
        return Convert(ctx, (c, prefix) =>
        {
            var expressionText = ctx.JSP_EXPRESSION().GetText();
            // Extract content between <%= and %>, preserving all whitespace
            var content = expressionText.Substring(3, expressionText.Length - 5);

            return new JspExpression(
                Guid.NewGuid(),
                prefix,
                Markers.Empty,
                content
            );
        })!;
    }

    public override Xml VisitJspdeclaration(XMLParser.JspdeclarationContext ctx)
    {
        return Convert(ctx, (c, prefix) =>
        {
            var declarationText = ctx.JSP_DECLARATION().GetText();
            // Extract content between <%! and %>, preserving all whitespace
            var content = declarationText.Substring(3, declarationText.Length - 5);

            return new JspDeclaration(
                Guid.NewGuid(),
                prefix,
                Markers.Empty,
                content
            );
        })!;
    }

    public override Xml VisitJspcomment(XMLParser.JspcommentContext ctx)
    {
        return Convert(ctx, (c, prefix) =>
        {
            var commentText = ctx.JSP_COMMENT().GetText();
            // Extract content between <%-- and --%>, preserving all whitespace
            var content = commentText.Substring(4, commentText.Length - 8);

            return new JspComment(
                Guid.NewGuid(),
                prefix,
                Markers.Empty,
                content
            );
        })!;
    }

    public override Xml VisitElement(XMLParser.ElementContext ctx)
    {
        return Convert(ctx, (c, prefix) =>
        {
            var name = Convert(ctx.Name(0), (n, p) => n.GetText());

            var attributes = ctx.attribute().Select(a => (Attribute)VisitAttribute(a)).ToList();

            List<Content>? content = null;
            string beforeTagDelimiterPrefix;
            Tag.Closing? closeTag = null;

            if (ctx.SLASH_CLOSE() != null)
            {
                beforeTagDelimiterPrefix = Prefix(ctx.SLASH_CLOSE());
                AdvanceCursor(ctx.SLASH_CLOSE().Symbol.StopIndex + 1);
            }
            else
            {
                beforeTagDelimiterPrefix = Prefix(ctx.CLOSE(0));
                AdvanceCursor(ctx.CLOSE(0).Symbol.StopIndex + 1);

                content = ctx.content()
                    .Select(cont => (Content)Visit(cont))
                    .ToList();

                var closeTagPrefix = Prefix(ctx.OPEN(1));
                AdvanceCursor(_cursor + 2);

                closeTag = new Tag.Closing(
                    Guid.NewGuid(),
                    closeTagPrefix,
                    Markers.Empty,
                    Convert(ctx.Name(1), (n, p) => n.GetText()),
                    Prefix(ctx.CLOSE(1))
                );
                AdvanceCursor(_cursor + 1);
            }

            return new Tag(Guid.NewGuid(), prefix, Markers.Empty, name, attributes,
                content, closeTag, beforeTagDelimiterPrefix);
        })!;
    }

    public override Xml VisitAttribute(XMLParser.AttributeContext ctx)
    {
        return Convert(ctx, (c, prefix) =>
        {
            var key = Convert(c.Name(), (t, p) => new Ident(Guid.NewGuid(), p, Markers.Empty, t.GetText()));

            var beforeEquals = Convert(c.EQUALS(), (e, p) => p);

            var val = Convert(c.STRING(), (v, p) => new Attribute.Value(
                Guid.NewGuid(),
                p,
                Markers.Empty,
                v.GetText().StartsWith("'") ? Attribute.Value.Quote.Single : Attribute.Value.Quote.Double,
                v.GetText().Substring(1, c.STRING().GetText().Length - 2)
            ));

            return new Attribute(Guid.NewGuid(), prefix, Markers.Empty, key, beforeEquals, val);
        })!;
    }

    public override Xml VisitDoctypedecl(XMLParser.DoctypedeclContext ctx)
    {
        return Convert(ctx, (c, prefix) =>
        {
            Skip(c.DOCTYPE());
            var name = Convert(c.Name(), (n, p) => new Ident(Guid.NewGuid(), p, Markers.Empty, n.GetText()));
            Ident? externalId = null;
            List<Ident>? internalSubset = null;
            if (!c.externalid().Start.Equals(c.DTD_CLOSE().Symbol))
            {
                if (c.externalid().Name() != null)
                {
                    externalId = Convert(c.externalid(),
                        (n, p) => new Ident(Guid.NewGuid(), p, Markers.Empty, n.Name().GetText()));
                }
                internalSubset = c.STRING()
                    .Select(s => Convert(s, (attr, p) => new Ident(Guid.NewGuid(), p, Markers.Empty, attr.GetText())))
                    .ToList();
            }

            DocTypeDecl.ExternalSubsets? externalSubsets = null;
            if (c.intsubset() != null)
            {
                var subsetPrefix = Prefix(c.DTD_SUBSET_OPEN());
                AdvanceCursor(c.DTD_SUBSET_OPEN().Symbol.StopIndex + 1);

                var elements = new List<Element>();
                var children = c.intsubset().children;
                for (var i = 0; i < children.Count; i++)
                {
                    var element = (ParserRuleContext)children[i];
                    // Markup declarations are not fully implemented.
                    // n.GetText() includes element subsets.
                    var ident = Convert(element,
                        (n, p) => new Ident(Guid.NewGuid(), p, Markers.Empty, n.GetText()));

                    var beforeElementTag = "";
                    if (i == children.Count - 1)
                    {
                        beforeElementTag = Prefix(c.DTD_SUBSET_CLOSE());
                        AdvanceCursor(c.DTD_SUBSET_CLOSE().Symbol.StopIndex + 1);
                    }

                    elements.Add(
                        new Element(
                            Guid.NewGuid(),
                            Prefix(element),
                            Markers.Empty,
                            new List<Ident> { ident },
                            beforeElementTag));
                }
                externalSubsets = new DocTypeDecl.ExternalSubsets(Guid.NewGuid(), subsetPrefix, Markers.Empty, elements);
            }

            var beforeTagDelimiterPrefix = Prefix(c.DTD_CLOSE());
            return new DocTypeDecl(Guid.NewGuid(),
                prefix,
                Markers.Empty,
                name,
                ctx.DOCTYPE().GetText(),
                externalId,
                internalSubset ?? new List<Ident>(),
                externalSubsets,
                beforeTagDelimiterPrefix);
        })!;
    }

    private string Prefix(ParserRuleContext ctx)
    {
        return Prefix(ctx.Start);
    }

    private string Prefix(ITerminalNode? terminalNode)
    {
        return terminalNode == null ? "" : Prefix(terminalNode.Symbol);
    }

    private string Prefix(IToken token)
    {
        var start = token.StartIndex;
        if (start < _cursor)
        {
            return "";
        }
        var prefix = _source.Substring(_cursor, start - _cursor);
        _cursor = start;
        return prefix;
    }

    /// <summary>
    /// Advance the cursor to a new char index position.
    /// In C#, ANTLR indices are UTF-16 char indices matching string indexing directly.
    /// </summary>
    private int AdvanceCursor(int newIndex)
    {
        if (newIndex > _cursor)
        {
            _cursor = newIndex;
        }
        return _cursor;
    }

    private T? Convert<C, T>(C? ctx, Func<C, string, T> conversion) where C : ParserRuleContext
    {
        if (ctx == null)
        {
            return default;
        }

        var t = conversion(ctx, Prefix(ctx));
        if (ctx.Stop != null)
        {
            AdvanceCursor(ctx.Stop.StopIndex + 1);
        }

        return t;
    }

    private T Convert<T>(ITerminalNode node, Func<ITerminalNode, string, T> conversion)
    {
        var t = conversion(node, Prefix(node));
        AdvanceCursor(node.Symbol.StopIndex + 1);
        return t;
    }

    private void Skip(ITerminalNode node)
    {
        AdvanceCursor(node.Symbol.StopIndex + 1);
    }
}
