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
using OpenRewrite.Core;

namespace OpenRewrite.Xml;

/// <summary>
/// Hand-rolled recursive-descent XML parser that produces a lossless AST.
/// Preserves all whitespace, comments, and formatting to enable round-trip fidelity.
/// </summary>
public class XmlParser
{
    public Document Parse(string source, string sourcePath = "file.xml")
    {
        var ctx = new ParseContext(source);
        return ctx.ParseDocument(sourcePath);
    }

    private class ParseContext
    {
        private readonly string _source;
        private int _cursor;

        public ParseContext(string source)
        {
            _source = source;
            _cursor = 0;
        }

        private bool Eof => _cursor >= _source.Length;
        private char Current => _source[_cursor];

        private string Rest => _cursor < _source.Length ? _source[_cursor..] : "";

        private bool StartsWith(string s) =>
            _cursor + s.Length <= _source.Length && _source.AsSpan(_cursor, s.Length).SequenceEqual(s);

        private void Advance(int count = 1) => _cursor += count;

        private string ConsumeUntil(string delimiter)
        {
            var idx = _source.IndexOf(delimiter, _cursor, StringComparison.Ordinal);
            if (idx < 0) idx = _source.Length;
            var result = _source[_cursor..idx];
            _cursor = idx;
            return result;
        }

        private string ConsumeWhitespace()
        {
            var start = _cursor;
            while (!Eof && char.IsWhiteSpace(Current))
                Advance();
            return _source[start.._cursor];
        }

        public Document ParseDocument(string sourcePath)
        {
            var prefix = ConsumeWhitespace();
            var prolog = ParseProlog();
            var root = ParseTag();
            var eof = Eof ? "" : _source[_cursor..];
            _cursor = _source.Length;

            return new Document(
                Guid.NewGuid(), sourcePath, prefix, Markers.Empty,
                null, false, null, null,
                prolog, root!, eof);
        }

        private Prolog? ParseProlog()
        {
            XmlDecl? xmlDecl = null;
            var miscList = new List<Misc>();
            var jspDirectives = new List<JspDirective>();

            var prefix = "";

            // Check for XML declaration
            if (StartsWith("<?xml") && (_cursor + 5 >= _source.Length || !char.IsLetterOrDigit(_source[_cursor + 5]) && _source[_cursor + 5] != '-'))
            {
                xmlDecl = ParseXmlDecl(prefix);
                prefix = "";
            }

            // Parse misc (comments, PIs, DOCTYPE, JSP elements) before root element
            while (!Eof)
            {
                var ws = ConsumeWhitespace();
                if (Eof) break;

                if (StartsWith("<!--"))
                {
                    miscList.Add(ParseComment(ws));
                }
                else if (StartsWith("<!"))
                {
                    miscList.Add(ParseDocTypeDecl(ws));
                }
                else if (StartsWith("<%@"))
                {
                    jspDirectives.Add(ParseJspDirective(ws));
                }
                else if (StartsWith("<%--"))
                {
                    miscList.Add(ParseJspComment(ws));
                }
                else if (StartsWith("<%!"))
                {
                    miscList.Add(ParseJspDeclaration(ws));
                }
                else if (StartsWith("<%="))
                {
                    // JSP expressions in prolog treated as misc
                    break;
                }
                else if (StartsWith("<%"))
                {
                    // JSP scriptlets in prolog - stop prolog parsing
                    break;
                }
                else if (StartsWith("<?"))
                {
                    miscList.Add(ParseProcessingInstruction(ws));
                }
                else
                {
                    // Must be the root element - push whitespace back
                    _cursor -= ws.Length;
                    break;
                }
            }

            if (xmlDecl == null && miscList.Count == 0 && jspDirectives.Count == 0)
                return null;

            return new Prolog(Guid.NewGuid(), prefix, Markers.Empty, xmlDecl, miscList, jspDirectives);
        }

        private XmlDecl ParseXmlDecl(string prefix)
        {
            // Consume "<?xml" or "<?XML" etc.
            Advance(2); // "<?"
            var nameStart = _cursor;
            while (!Eof && !char.IsWhiteSpace(Current) && !StartsWith("?>"))
                Advance();
            var name = _source[nameStart.._cursor];

            var attributes = ParseAttributes();
            var beforeDelimiter = ConsumeWhitespace();
            if (StartsWith("?>"))
                Advance(2);

            return new XmlDecl(Guid.NewGuid(), prefix, Markers.Empty, name, attributes, beforeDelimiter);
        }

        private ProcessingInstruction ParseProcessingInstruction(string prefix)
        {
            Advance(2); // "<?"
            var nameStart = _cursor;
            while (!Eof && !char.IsWhiteSpace(Current) && !StartsWith("?>"))
                Advance();
            var name = _source[nameStart.._cursor];

            // Parse the PI text content (everything until ?>)
            var textPrefix = "";
            var textStart = _cursor;
            if (!Eof && !StartsWith("?>"))
            {
                // Separate prefix whitespace from actual content
                var wsStart = _cursor;
                while (!Eof && char.IsWhiteSpace(Current) && !StartsWith("?>"))
                    Advance();
                textPrefix = _source[wsStart.._cursor];
            }

            var contentStart = _cursor;
            var contentEnd = _source.IndexOf("?>", _cursor, StringComparison.Ordinal);
            if (contentEnd < 0) contentEnd = _source.Length;

            var fullText = _source[contentStart..contentEnd];

            // Split into value and trailing whitespace
            var trimmedEnd = fullText.Length;
            while (trimmedEnd > 0 && char.IsWhiteSpace(fullText[trimmedEnd - 1]))
                trimmedEnd--;
            var textValue = fullText[..trimmedEnd];
            var afterText = fullText[trimmedEnd..];

            _cursor = contentEnd;
            var beforeDelimiter = "";
            if (StartsWith("?>"))
            {
                beforeDelimiter = afterText;
                afterText = "";
                Advance(2);
            }

            var piText = new CharData(Guid.NewGuid(), textPrefix, Markers.Empty, false, textValue, afterText);

            return new ProcessingInstruction(Guid.NewGuid(), prefix, Markers.Empty, name, piText, beforeDelimiter);
        }

        private Comment ParseComment(string prefix)
        {
            Advance(4); // "<!--"
            var text = ConsumeUntil("-->");
            if (StartsWith("-->"))
                Advance(3);

            return new Comment(Guid.NewGuid(), prefix, Markers.Empty, text);
        }

        private Tag? ParseTag()
        {
            var prefix = ConsumeWhitespace();
            if (Eof || !StartsWith("<") || StartsWith("</"))
            {
                _cursor -= prefix.Length;
                return null;
            }

            return ParseTagInner(prefix);
        }

        private Tag ParseTagInner(string prefix)
        {
            Advance(1); // "<"
            var name = ParseName();
            var attributes = ParseAttributes();
            var beforeTagDelimiter = ConsumeWhitespace();

            IList<Content>? content = null;
            Tag.Closing? closingTag = null;

            if (StartsWith("/>"))
            {
                Advance(2);
            }
            else if (StartsWith(">"))
            {
                Advance(1); // ">"
                content = ParseContent(name);
                closingTag = ParseClosingTag();
            }

            return new Tag(Guid.NewGuid(), prefix, Markers.Empty, name, attributes, content, closingTag, beforeTagDelimiter);
        }

        private Tag.Closing ParseClosingTag()
        {
            var prefix = "";
            // We should be at "</"
            if (StartsWith("</"))
            {
                // Capture any whitespace before "</" as prefix
                Advance(2);
            }

            var name = ParseName();
            var beforeDelimiter = ConsumeWhitespace();
            if (!Eof && Current == '>')
                Advance(1);

            return new Tag.Closing(Guid.NewGuid(), prefix, Markers.Empty, name, beforeDelimiter);
        }

        private IList<Content> ParseContent(string parentTagName)
        {
            var content = new List<Content>();

            while (!Eof)
            {
                if (StartsWith("</"))
                    break;

                if (StartsWith("<![CDATA["))
                {
                    content.Add(ParseCdata());
                }
                else if (StartsWith("<!--"))
                {
                    var ws = "";
                    content.Add(ParseComment(ws));
                }
                else if (StartsWith("<%@"))
                {
                    content.Add(ParseJspDirective(""));
                }
                else if (StartsWith("<%--"))
                {
                    content.Add(ParseJspCommentContent(""));
                }
                else if (StartsWith("<%!"))
                {
                    content.Add(ParseJspDeclarationContent(""));
                }
                else if (StartsWith("<%="))
                {
                    content.Add(ParseJspExpressionContent(""));
                }
                else if (StartsWith("<%"))
                {
                    content.Add(ParseJspScriptletContent(""));
                }
                else if (StartsWith("<?"))
                {
                    content.Add(ParseProcessingInstruction(""));
                }
                else if (StartsWith("<"))
                {
                    content.Add(ParseTagInner(""));
                }
                else
                {
                    content.Add(ParseCharData());
                }
            }

            return content;
        }

        private CharData ParseCharData()
        {
            var start = _cursor;
            // Consume text until we hit a tag
            while (!Eof && !StartsWith("<"))
                Advance();

            var rawText = _source[start.._cursor];

            // Split raw text into: prefix whitespace, value, trailing whitespace
            var prefixEnd = 0;
            while (prefixEnd < rawText.Length && char.IsWhiteSpace(rawText[prefixEnd]))
                prefixEnd++;

            string prefix, text, afterText;

            if (prefixEnd == rawText.Length)
            {
                // All whitespace
                prefix = rawText;
                text = "";
                afterText = "";
            }
            else
            {
                prefix = rawText[..prefixEnd];
                var remaining = rawText[prefixEnd..];
                // Find trailing whitespace
                var trimEnd = remaining.Length;
                while (trimEnd > 0 && char.IsWhiteSpace(remaining[trimEnd - 1]))
                    trimEnd--;
                text = remaining[..trimEnd];
                afterText = remaining[trimEnd..];
            }

            return new CharData(Guid.NewGuid(), prefix, Markers.Empty, false, text, afterText);
        }

        private CharData ParseCdata()
        {
            var prefix = "";
            Advance(9); // "<![CDATA["
            var text = ConsumeUntil("]]>");
            if (StartsWith("]]>"))
                Advance(3);

            return new CharData(Guid.NewGuid(), prefix, Markers.Empty, true, text, "");
        }

        private DocTypeDecl ParseDocTypeDecl(string prefix)
        {
            Advance(2); // "<!"
            // Read the DOCTYPE keyword
            var docTypeStart = _cursor;
            while (!Eof && !char.IsWhiteSpace(Current) && Current != '>' && Current != '[')
                Advance();
            var documentDeclaration = _source[docTypeStart.._cursor];

            var namePrefix = ConsumeWhitespace();
            var nameStr = ParseName();
            var name = new Ident(Guid.NewGuid(), namePrefix, Markers.Empty, nameStr);

            // Parse optional external ID and strings
            Ident? externalId = null;
            var internalSubset = new List<Ident>();

            var ws = ConsumeWhitespace();
            if (!Eof && Current != '>' && Current != '[')
            {
                // Could be SYSTEM or PUBLIC
                var keyword = PeekName();
                if (keyword is "SYSTEM" or "PUBLIC")
                {
                    var extIdName = ParseName();
                    externalId = new Ident(Guid.NewGuid(), ws, Markers.Empty, extIdName);
                    ws = ConsumeWhitespace();

                    // Parse string literals (URIs)
                    while (!Eof && (Current == '"' || Current == '\''))
                    {
                        var strPrefix = ws;
                        var quote = Current;
                        Advance(1);
                        var strStart = _cursor;
                        while (!Eof && Current != quote)
                            Advance();
                        var strValue = _source[strStart.._cursor];
                        if (!Eof) Advance(1); // closing quote
                        internalSubset.Add(new Ident(Guid.NewGuid(), strPrefix, Markers.Empty, $"{quote}{strValue}{quote}"));
                        ws = ConsumeWhitespace();
                    }
                }
                else
                {
                    // Not a recognized keyword, push back whitespace
                    _cursor -= ws.Length;
                    ws = "";
                }
            }

            // Parse optional internal subset [...]
            DocTypeDecl.ExternalSubsets? externalSubsets = null;
            if (!Eof && Current == '[')
            {
                var subsetPrefix = ws;
                Advance(1); // "["
                var elements = new List<Element>();

                while (!Eof && Current != ']')
                {
                    var elemWs = ConsumeWhitespace();
                    if (!Eof && Current == ']') break;

                    // Read until end of declaration or next declaration
                    var elemStart = _cursor;
                    if (StartsWith("<!--"))
                    {
                        ConsumeUntil("-->");
                        if (StartsWith("-->")) Advance(3);
                    }
                    else if (StartsWith("<!"))
                    {
                        // Read the full declaration
                        var depth = 0;
                        while (!Eof)
                        {
                            if (Current == '<') depth++;
                            else if (Current == '>')
                            {
                                depth--;
                                Advance();
                                if (depth <= 0) break;
                                continue;
                            }
                            Advance();
                        }
                    }
                    else if (Current == '%')
                    {
                        // Parameter entity reference
                        while (!Eof && Current != ';')
                            Advance();
                        if (!Eof) Advance(1); // ";"
                    }
                    else
                    {
                        Advance();
                    }

                    var elemText = _source[elemStart.._cursor];
                    var identNode = new Ident(Guid.NewGuid(), "", Markers.Empty, elemText);

                    // Check if next is ']' to capture beforeTagDelimiter
                    var afterWs = ConsumeWhitespace();
                    string beforeElementTag;
                    if (!Eof && Current == ']')
                    {
                        beforeElementTag = afterWs;
                    }
                    else
                    {
                        beforeElementTag = "";
                        _cursor -= afterWs.Length;
                    }

                    elements.Add(new Element(Guid.NewGuid(), elemWs, Markers.Empty, [identNode], beforeElementTag));
                }

                if (!Eof && Current == ']')
                    Advance(1);

                externalSubsets = new DocTypeDecl.ExternalSubsets(Guid.NewGuid(), subsetPrefix, Markers.Empty, elements);
                ws = ConsumeWhitespace();
            }

            var beforeTagDelimiter = ws;
            if (!Eof && Current == '>')
                Advance(1);

            return new DocTypeDecl(Guid.NewGuid(), prefix, Markers.Empty, name, documentDeclaration,
                externalId, internalSubset, externalSubsets, beforeTagDelimiter);
        }

        private JspDirective ParseJspDirective(string prefix)
        {
            Advance(3); // "<%@"
            var beforeTypePrefix = ConsumeWhitespace();
            var type = ParseName();
            var attributes = ParseAttributes();
            var beforeEnd = ConsumeWhitespace();
            if (StartsWith("%>"))
                Advance(2);

            return new JspDirective(Guid.NewGuid(), prefix, Markers.Empty, beforeTypePrefix, type, attributes, beforeEnd);
        }

        private JspScriptlet ParseJspScriptletContent(string prefix)
        {
            Advance(2); // "<%"
            var content = ConsumeUntil("%>");
            if (StartsWith("%>"))
                Advance(2);

            return new JspScriptlet(Guid.NewGuid(), prefix, Markers.Empty, content);
        }

        private JspExpression ParseJspExpressionContent(string prefix)
        {
            Advance(3); // "<%="
            var content = ConsumeUntil("%>");
            if (StartsWith("%>"))
                Advance(2);

            return new JspExpression(Guid.NewGuid(), prefix, Markers.Empty, content);
        }

        private JspDeclaration ParseJspDeclarationContent(string prefix)
        {
            Advance(3); // "<%!"
            var content = ConsumeUntil("%>");
            if (StartsWith("%>"))
                Advance(2);

            return new JspDeclaration(Guid.NewGuid(), prefix, Markers.Empty, content);
        }

        private JspComment ParseJspCommentContent(string prefix)
        {
            Advance(4); // "<%--"
            var content = ConsumeUntil("--%>");
            if (StartsWith("--%>"))
                Advance(4);

            return new JspComment(Guid.NewGuid(), prefix, Markers.Empty, content);
        }

        private JspDeclaration ParseJspDeclaration(string prefix)
        {
            return ParseJspDeclarationContent(prefix);
        }

        private JspComment ParseJspComment(string prefix)
        {
            return ParseJspCommentContent(prefix);
        }

        private IList<Attribute> ParseAttributes()
        {
            var attributes = new List<Attribute>();
            while (!Eof)
            {
                var ws = ConsumeWhitespace();
                if (Eof || StartsWith("?>") || StartsWith("/>") || StartsWith("%>") || Current == '>' || StartsWith("</"))
                {
                    _cursor -= ws.Length;
                    break;
                }

                // Check if this is actually a name character (attribute start)
                if (!IsNameChar(Current) && Current != '_' && Current != ':')
                {
                    _cursor -= ws.Length;
                    break;
                }

                var attrPrefix = ws;
                var keyName = ParseName();
                var key = new Ident(Guid.NewGuid(), "", Markers.Empty, keyName);

                var beforeEquals = ConsumeWhitespace();
                if (!Eof && Current == '=')
                    Advance(1);

                var valuePrefix = ConsumeWhitespace();

                Attribute.Value.Quote quoteStyle;
                char quoteChar;
                if (!Eof && (Current == '"' || Current == '\''))
                {
                    quoteChar = Current;
                    quoteStyle = quoteChar == '"' ? Attribute.Value.Quote.Double : Attribute.Value.Quote.Single;
                    Advance(1);
                }
                else
                {
                    // Shouldn't happen in well-formed XML
                    quoteChar = '"';
                    quoteStyle = Attribute.Value.Quote.Double;
                }

                var valStart = _cursor;
                while (!Eof && Current != quoteChar)
                    Advance();
                var val = _source[valStart.._cursor];
                if (!Eof) Advance(1); // closing quote

                var value = new Attribute.Value(Guid.NewGuid(), valuePrefix, Markers.Empty, quoteStyle, val);
                attributes.Add(new Attribute(Guid.NewGuid(), attrPrefix, Markers.Empty, key, beforeEquals, value));
            }

            return attributes;
        }

        private string ParseName()
        {
            var start = _cursor;
            while (!Eof && IsNameChar(Current))
                Advance();
            return _source[start.._cursor];
        }

        private string PeekName()
        {
            var saved = _cursor;
            var name = ParseName();
            _cursor = saved;
            return name;
        }

        private static bool IsNameChar(char c) =>
            char.IsLetterOrDigit(c) || c is '-' or '_' or '.' or ':';
    }
}
