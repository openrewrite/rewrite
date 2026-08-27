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
using OpenRewrite.Java;

namespace OpenRewrite.CSharp;

/// <summary>
/// Serializes a structured C# XML documentation comment back to <c>///</c> source text.
/// Mirrors the Java <c>CsDocCommentPrinter</c> exactly so the same tree prints identically on
/// both sides of the RPC boundary.
/// </summary>
public class CsDocCommentPrinter<P> : CsDocCommentVisitor<PrintOutputCapture<P>>
{
    private static readonly Func<string, string> MarkerWrapper =
        @out => "~~" + @out + (@out.Length == 0 ? "" : "~~") + ">";

    public CsDocCommentPrinter() : base(new CSharpPrinter<P>())
    {
    }

    public override CsDocComment VisitDocComment(CsDocComment.DocComment docComment, PrintOutputCapture<P> p)
    {
        BeforeSyntax(docComment.Markers, p);
        p.Append("///");
        VisitAll(docComment.Body, p);
        AfterSyntax(docComment.Markers, p);
        return docComment;
    }

    public override CsDocComment VisitXmlElement(CsDocComment.XmlElement element, PrintOutputCapture<P> p)
    {
        BeforeSyntax(element.Markers, p);
        p.Append('<').Append(element.Name);
        VisitAll(element.Attributes, p);
        VisitAll(element.SpaceBeforeClose, p);
        p.Append('>');
        VisitAll(element.Content, p);
        p.Append("</").Append(element.Name);
        VisitAll(element.ClosingTagSpaceBeforeClose, p);
        p.Append('>');
        AfterSyntax(element.Markers, p);
        return element;
    }

    public override CsDocComment VisitXmlEmptyElement(CsDocComment.XmlEmptyElement element, PrintOutputCapture<P> p)
    {
        BeforeSyntax(element.Markers, p);
        p.Append('<').Append(element.Name);
        VisitAll(element.Attributes, p);
        VisitAll(element.SpaceBeforeSlashClose, p);
        p.Append("/>");
        AfterSyntax(element.Markers, p);
        return element;
    }

    public override CsDocComment VisitXmlText(CsDocComment.XmlText text, PrintOutputCapture<P> p)
    {
        BeforeSyntax(text.Markers, p);
        p.Append(text.Text);
        AfterSyntax(text.Markers, p);
        return text;
    }

    public override CsDocComment VisitXmlAttribute(CsDocComment.XmlAttribute attribute, PrintOutputCapture<P> p)
    {
        BeforeSyntax(attribute.Markers, p);
        p.Append(attribute.Name);
        PrintAttributeValue(attribute.SpaceBeforeEquals, attribute.Value, p);
        AfterSyntax(attribute.Markers, p);
        return attribute;
    }

    public override CsDocComment VisitXmlCrefAttribute(CsDocComment.XmlCrefAttribute attribute, PrintOutputCapture<P> p)
    {
        BeforeSyntax(attribute.Markers, p);
        p.Append("cref");
        PrintAttributeValue(attribute.SpaceBeforeEquals, attribute.Value, p);
        AfterSyntax(attribute.Markers, p);
        return attribute;
    }

    public override CsDocComment VisitXmlNameAttribute(CsDocComment.XmlNameAttribute attribute, PrintOutputCapture<P> p)
    {
        BeforeSyntax(attribute.Markers, p);
        p.Append("name");
        PrintAttributeValue(attribute.SpaceBeforeEquals, attribute.Value, p);
        AfterSyntax(attribute.Markers, p);
        return attribute;
    }

    public override CsDocComment VisitLineBreak(CsDocComment.LineBreak lineBreak, PrintOutputCapture<P> p)
    {
        BeforeSyntax(lineBreak.Markers, p);
        p.Append(lineBreak.Margin);
        AfterSyntax(lineBreak.Markers, p);
        return lineBreak;
    }

    /// <summary>
    /// Print the <c>=value</c> portion of an XML attribute. Standard XML attributes have no
    /// whitespace around the <c>=</c>, so <paramref name="spaceBeforeEquals"/> is normally
    /// null/empty; the value must still be printed whenever it is present.
    /// </summary>
    private void PrintAttributeValue(IList<CsDocComment>? spaceBeforeEquals, IList<CsDocComment>? value,
        PrintOutputCapture<P> p)
    {
        if (value == null)
        {
            return;
        }
        if (spaceBeforeEquals != null)
        {
            VisitAll(spaceBeforeEquals, p);
        }
        p.Append('=');
        VisitAll(value, p);
    }

    private void VisitAll(IList<CsDocComment>? nodes, PrintOutputCapture<P> p)
    {
        if (nodes == null) return;
        foreach (var node in nodes)
        {
            Visit(node, p);
        }
    }

    private void BeforeSyntax(Markers markers, PrintOutputCapture<P> p)
    {
        foreach (var marker in markers.MarkerList)
        {
            p.Append(p.MarkerPrinter.BeforeSyntax(marker, new Cursor(Cursor, marker), MarkerWrapper));
        }
    }

    private void AfterSyntax(Markers markers, PrintOutputCapture<P> p)
    {
        foreach (var marker in markers.MarkerList)
        {
            p.Append(p.MarkerPrinter.AfterSyntax(marker, new Cursor(Cursor, marker), MarkerWrapper));
        }
    }
}
