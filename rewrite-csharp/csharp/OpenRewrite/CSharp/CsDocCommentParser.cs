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

using System.Text;
using Microsoft.CodeAnalysis;
using Microsoft.CodeAnalysis.CSharp;
using Microsoft.CodeAnalysis.CSharp.Syntax;
using OpenRewrite.Core;

namespace OpenRewrite.CSharp;

/// <summary>
/// Builds the structured <see cref="CsDocComment"/> tree for C# XML documentation comments
/// (<c>///</c>) directly from Roslyn's own XML documentation-comment parse
/// (<see cref="DocumentationCommentTriviaSyntax"/>). This replaces the previous approach of
/// shipping raw doc-comment text to the Java side for parsing there.
/// </summary>
public static class CsDocCommentParser
{
    /// <summary>
    /// Group consecutive single-line <c>///</c> comments in a space into structured
    /// <see cref="CsDocComment.DocComment"/> nodes. Non-doc comments are left untouched.
    /// A blank line (more than one line break) ends a doc-comment block, matching Roslyn.
    /// </summary>
    public static Space StructureDocComments(Space space)
    {
        var comments = space.Comments;
        if (comments.Count == 0)
        {
            return space;
        }

        var hasDoc = false;
        foreach (var c in comments)
        {
            if (IsDocLine(c))
            {
                hasDoc = true;
                break;
            }
        }
        if (!hasDoc)
        {
            return space;
        }

        var result = new List<Comment>(comments.Count);
        var i = 0;
        while (i < comments.Count)
        {
            if (!IsDocLine(comments[i]))
            {
                result.Add(comments[i]);
                i++;
                continue;
            }

            // Accumulate consecutive /// lines joined by single line breaks back into the
            // original doc-comment source text (each line's Text is the content after "//").
            var sb = new StringBuilder();
            var j = i;
            while (true)
            {
                var tc = (TextComment)comments[j];
                sb.Append("//").Append(tc.Text);
                var hasNext = j + 1 < comments.Count && IsDocLine(comments[j + 1]) &&
                              IsSingleLineBreak(tc.Suffix);
                if (!hasNext)
                {
                    break;
                }
                sb.Append(tc.Suffix);
                j++;
            }

            var docSuffix = ((TextComment)comments[j]).Suffix;
            result.Add(ParseDocComment(sb.ToString(), docSuffix));
            i = j + 1;
        }

        return space.WithComments(result);
    }

    private static bool IsDocLine(Comment c) =>
        c is TextComment { Multiline: false } tc && tc.Text.StartsWith("/");

    private static bool IsSingleLineBreak(string suffix)
    {
        var count = 0;
        foreach (var ch in suffix)
        {
            if (ch == '\n')
            {
                count++;
            }
        }
        return count == 1;
    }

    /// <summary>
    /// Parse a single doc-comment block (full <c>/// ...</c> source text, no trailing newline)
    /// into a structured <see cref="CsDocComment.DocComment"/>. Falls back to a single flat
    /// <see cref="CsDocComment.XmlText"/> body if the structured mapping is not lossless.
    /// </summary>
    public static CsDocComment.DocComment ParseDocComment(string fullText, string suffix)
    {
        DocumentationCommentTriviaSyntax? doc = null;
        foreach (var trivia in SyntaxFactory.ParseLeadingTrivia(fullText))
        {
            if (trivia.GetStructure() is DocumentationCommentTriviaSyntax d)
            {
                doc = d;
                break;
            }
        }

        if (doc != null)
        {
            var body = new List<CsDocComment>();
            var first = true;
            MapContent(doc.Content, body, ref first);
            var docComment = new CsDocComment.DocComment(Guid.NewGuid(), Markers.Empty, body, suffix);
            if (Print(docComment) == fullText)
            {
                return docComment;
            }
        }

        // Fallback: keep the whole body as literal text after the leading "///".
        var flat = new List<CsDocComment>
        {
            new CsDocComment.XmlText(Guid.NewGuid(), Markers.Empty, fullText.Substring(3))
        };
        return new CsDocComment.DocComment(Guid.NewGuid(), Markers.Empty, flat, suffix);
    }

    private static string Print(CsDocComment.DocComment docComment)
    {
        var capture = new PrintOutputCapture<int>(0);
        new CsDocCommentPrinter<int>().Visit(docComment, capture);
        return capture.ToString();
    }

    // ---- Roslyn XML doc-comment tree → CsDocComment mapping ----

    private static void MapContent(SyntaxList<XmlNodeSyntax> content, List<CsDocComment> output, ref bool first)
    {
        foreach (var node in content)
        {
            switch (node)
            {
                case XmlTextSyntax text:
                    MapText(text, output, ref first);
                    break;
                case XmlElementSyntax element:
                    EmitLeading(element.GetFirstToken(), output, ref first);
                    output.Add(MapElement(element));
                    break;
                case XmlEmptyElementSyntax empty:
                    EmitLeading(empty.GetFirstToken(), output, ref first);
                    output.Add(MapEmptyElement(empty));
                    break;
                default:
                    // XmlCDataSection / XmlComment / XmlProcessingInstruction: keep verbatim.
                    first = false;
                    output.Add(Text(node.ToFullString()));
                    break;
            }
        }
    }

    private static void MapText(XmlTextSyntax text, List<CsDocComment> output, ref bool first)
    {
        var tokens = text.TextTokens;
        for (var i = 0; i < tokens.Count; i++)
        {
            var tok = tokens[i];
            if (tok.IsKind(SyntaxKind.XmlTextLiteralNewLineToken))
            {
                var margin = tok.Text;
                // A continuation "///" (with any indentation) is the leading trivia of the
                // token that immediately follows the newline; fold it into the LineBreak margin.
                if (i + 1 < tokens.Count && HasExterior(tokens[i + 1]))
                {
                    margin += LeadingText(tokens[i + 1]);
                    output.Add(LineBreak(margin));
                    output.Add(Text(tokens[i + 1].Text));
                    i++;
                    first = false;
                }
                else
                {
                    output.Add(LineBreak(margin));
                }
            }
            else
            {
                // The very first token carries the opening "///" as leading trivia, which the
                // DocComment itself prints — so drop it by using Text (not full string) there.
                output.Add(Text(first ? tok.Text : tok.ToFullString()));
                first = false;
            }
        }
    }

    private static CsDocComment MapElement(XmlElementSyntax element)
    {
        var start = element.StartTag;
        var content = new List<CsDocComment>();
        var innerFirst = false;
        MapContent(element.Content, content, ref innerFirst);
        return new CsDocComment.XmlElement(
            Guid.NewGuid(), Markers.Empty,
            start.Name.ToString(),
            MapAttributes(start.Attributes),
            LeadingAsList(start.GreaterThanToken),
            content,
            LeadingAsList(element.EndTag.GreaterThanToken));
    }

    private static CsDocComment MapEmptyElement(XmlEmptyElementSyntax element) =>
        new CsDocComment.XmlEmptyElement(
            Guid.NewGuid(), Markers.Empty,
            element.Name.ToString(),
            MapAttributes(element.Attributes),
            LeadingAsList(element.SlashGreaterThanToken));

    private static IList<CsDocComment> MapAttributes(SyntaxList<XmlAttributeSyntax> attributes)
    {
        var result = new List<CsDocComment>();
        foreach (var attribute in attributes)
        {
            // Whitespace before an attribute is a standalone text node in the parent's list.
            result.Add(Text(LeadingText(attribute.GetFirstToken())));
            result.Add(MapAttribute(attribute));
        }
        return result;
    }

    private static CsDocComment MapAttribute(XmlAttributeSyntax attribute)
    {
        switch (attribute)
        {
            case XmlNameAttributeSyntax name:
                return new CsDocComment.XmlNameAttribute(
                    Guid.NewGuid(), Markers.Empty,
                    SpaceBeforeEquals(name.EqualsToken),
                    ValueList(name.StartQuoteToken.ToFullString() + name.Identifier.ToFullString() +
                              name.EndQuoteToken.ToFullString()),
                    null);
            case XmlCrefAttributeSyntax cref:
                return new CsDocComment.XmlCrefAttribute(
                    Guid.NewGuid(), Markers.Empty,
                    SpaceBeforeEquals(cref.EqualsToken),
                    ValueList(cref.StartQuoteToken.ToFullString() + cref.Cref.ToFullString() +
                              cref.EndQuoteToken.ToFullString()),
                    null);
            case XmlTextAttributeSyntax textAttr:
                var valueText = new StringBuilder(textAttr.StartQuoteToken.ToFullString());
                foreach (var t in textAttr.TextTokens)
                {
                    valueText.Append(t.ToFullString());
                }
                valueText.Append(textAttr.EndQuoteToken.ToFullString());
                return new CsDocComment.XmlAttribute(
                    Guid.NewGuid(), Markers.Empty,
                    textAttr.Name.ToString(),
                    SpaceBeforeEquals(textAttr.EqualsToken),
                    ValueList(valueText.ToString()));
            default:
                return Text(attribute.ToFullString());
        }
    }

    private static IList<CsDocComment>? SpaceBeforeEquals(SyntaxToken equalsToken)
    {
        var lead = LeadingText(equalsToken);
        return lead.Length == 0 ? null : new List<CsDocComment> { Text(lead) };
    }

    private static IList<CsDocComment> ValueList(string text) =>
        new List<CsDocComment> { Text(text) };

    /// <summary>Emit the leading trivia of a token as text, dropping the opening "///".</summary>
    private static void EmitLeading(SyntaxToken token, List<CsDocComment> output, ref bool first)
    {
        if (first)
        {
            first = false;
            return;
        }
        var lead = LeadingText(token);
        if (lead.Length > 0)
        {
            output.Add(Text(lead));
        }
    }

    private static IList<CsDocComment> LeadingAsList(SyntaxToken token)
    {
        var lead = LeadingText(token);
        return lead.Length == 0
            ? new List<CsDocComment>()
            : new List<CsDocComment> { Text(lead) };
    }

    private static bool HasExterior(SyntaxToken token)
    {
        foreach (var trivia in token.LeadingTrivia)
        {
            if (trivia.IsKind(SyntaxKind.DocumentationCommentExteriorTrivia))
            {
                return true;
            }
        }
        return false;
    }

    private static string LeadingText(SyntaxToken token)
    {
        if (token.LeadingTrivia.Count == 0)
        {
            return "";
        }
        var sb = new StringBuilder();
        foreach (var trivia in token.LeadingTrivia)
        {
            sb.Append(trivia.ToFullString());
        }
        return sb.ToString();
    }

    private static CsDocComment.XmlText Text(string text) =>
        new(Guid.NewGuid(), Markers.Empty, text);

    private static CsDocComment.LineBreak LineBreak(string margin) =>
        new(Guid.NewGuid(), margin, Markers.Empty);
}
