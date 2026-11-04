/*
 * Copyright 2025 the original author or authors.
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
/// Prints XML LST back to source code.
/// Mirrors Java's org.openrewrite.xml.internal.XmlPrinter.
/// </summary>
public class XmlPrinter<P> : XmlVisitor<PrintOutputCapture<P>>
{
    private static string XmlMarkerWrapper(string text) =>
        "<!--~~" + text + (text.Length == 0 ? "" : "~~") + ">-->";

    public override Xml VisitDocument(Document document, PrintOutputCapture<P> p)
    {
        if (document.CharsetBomMarked) p.Append('\uFEFF');
        BeforeSyntax(document, p);
        if (document.Prolog != null) Visit(document.Prolog, p);
        Visit(document.Root, p);
        AfterSyntax(document, p);
        p.Append(document.Eof);
        return document;
    }

    public override Xml VisitProlog(Prolog prolog, PrintOutputCapture<P> p)
    {
        BeforeSyntax(prolog, p);
        if (prolog.XmlDecl != null) Visit(prolog.XmlDecl, p);
        foreach (var misc in prolog.MiscList) Visit(misc, p);
        foreach (var jsp in prolog.JspDirectives) Visit(jsp, p);
        AfterSyntax(prolog, p);
        return prolog;
    }

    public override Xml VisitXmlDecl(XmlDecl xmlDecl, PrintOutputCapture<P> p)
    {
        BeforeSyntax(xmlDecl, p);
        p.Append("<?").Append(xmlDecl.Name);
        foreach (var attr in xmlDecl.Attributes) Visit(attr, p);
        p.Append(xmlDecl.BeforeTagDelimiterPrefix).Append("?>");
        AfterSyntax(xmlDecl, p);
        return xmlDecl;
    }

    public override Xml VisitTag(Tag tag, PrintOutputCapture<P> p)
    {
        BeforeSyntax(tag, p);
        p.Append('<').Append(tag.Name);
        foreach (var attr in tag.Attributes) Visit(attr, p);
        p.Append(tag.BeforeTagDelimiterPrefix);
        if (tag.ClosingTag == null)
        {
            p.Append("/>");
        }
        else
        {
            p.Append('>');
            if (tag.ContentList != null)
            {
                foreach (var content in tag.ContentList) Visit(content, p);
            }
            Visit(tag.ClosingTag, p);
        }
        AfterSyntax(tag, p);
        return tag;
    }

    public override Xml VisitTagClosing(Tag.Closing closing, PrintOutputCapture<P> p)
    {
        BeforeSyntax(closing, p);
        p.Append("</").Append(closing.Name).Append(closing.BeforeTagDelimiterPrefix).Append(">");
        AfterSyntax(closing, p);
        return closing;
    }

    public override Xml VisitAttribute(Attribute attribute, PrintOutputCapture<P> p)
    {
        BeforeSyntax(attribute, p);
        p.Append(attribute.Key.Prefix).Append(attribute.Key.Name);
        p.Append(attribute.BeforeEquals).Append('=');
        Visit(attribute.Val, p);
        AfterSyntax(attribute, p);
        return attribute;
    }

    public override Xml VisitAttributeValue(Attribute.Value value, PrintOutputCapture<P> p)
    {
        BeforeSyntax(value, p);
        var delim = value.QuoteStyle == Attribute.Value.Quote.Double ? '"' : '\'';
        p.Append(delim).Append(value.Val).Append(delim);
        AfterSyntax(value, p);
        return value;
    }

    public override Xml VisitCharData(CharData charData, PrintOutputCapture<P> p)
    {
        BeforeSyntax(charData, p);
        if (charData.Cdata)
        {
            p.Append("<![CDATA[").Append(charData.Text).Append("]]>");
        }
        else
        {
            p.Append(charData.Text);
        }
        p.Append(charData.AfterText);
        AfterSyntax(charData, p);
        return charData;
    }

    public override Xml VisitComment(Comment comment, PrintOutputCapture<P> p)
    {
        BeforeSyntax(comment, p);
        p.Append("<!--").Append(comment.Text).Append("-->");
        AfterSyntax(comment, p);
        return comment;
    }

    public override Xml VisitProcessingInstruction(ProcessingInstruction pi, PrintOutputCapture<P> p)
    {
        BeforeSyntax(pi, p);
        p.Append("<?").Append(pi.Name);
        Visit(pi.ProcessingInstructions, p);
        p.Append(pi.BeforeTagDelimiterPrefix).Append("?>");
        AfterSyntax(pi, p);
        return pi;
    }

    public override Xml VisitDocTypeDecl(DocTypeDecl docTypeDecl, PrintOutputCapture<P> p)
    {
        BeforeSyntax(docTypeDecl, p);
        p.Append("<!").Append(docTypeDecl.DocumentDeclaration);
        Visit(docTypeDecl.Name, p);
        if (docTypeDecl.ExternalId != null) Visit(docTypeDecl.ExternalId, p);
        foreach (var ident in docTypeDecl.InternalSubset) Visit(ident, p);
        if (docTypeDecl.ExternalSubsetsNode != null) Visit(docTypeDecl.ExternalSubsetsNode, p);
        p.Append(docTypeDecl.BeforeTagDelimiterPrefix).Append('>');
        AfterSyntax(docTypeDecl, p);
        return docTypeDecl;
    }

    public override Xml VisitDocTypeDeclExternalSubsets(DocTypeDecl.ExternalSubsets externalSubsets, PrintOutputCapture<P> p)
    {
        BeforeSyntax(externalSubsets, p);
        p.Append('[');
        foreach (var elem in externalSubsets.Elements) Visit(elem, p);
        p.Append(']');
        AfterSyntax(externalSubsets, p);
        return externalSubsets;
    }

    public override Xml VisitElement(Element element, PrintOutputCapture<P> p)
    {
        BeforeSyntax(element, p);
        foreach (var ident in element.Subset) Visit(ident, p);
        p.Append(element.BeforeTagDelimiterPrefix);
        AfterSyntax(element, p);
        return element;
    }

    public override Xml VisitIdent(Ident ident, PrintOutputCapture<P> p)
    {
        BeforeSyntax(ident, p);
        p.Append(ident.Name);
        AfterSyntax(ident, p);
        return ident;
    }

    public override Xml VisitJspDirective(JspDirective jspDirective, PrintOutputCapture<P> p)
    {
        BeforeSyntax(jspDirective, p);
        p.Append("<%@").Append(jspDirective.BeforeTypePrefix).Append(jspDirective.Type);
        foreach (var attr in jspDirective.Attributes) Visit(attr, p);
        p.Append(jspDirective.BeforeDirectiveEndPrefix).Append("%>");
        AfterSyntax(jspDirective, p);
        return jspDirective;
    }

    public override Xml VisitJspScriptlet(JspScriptlet jspScriptlet, PrintOutputCapture<P> p)
    {
        BeforeSyntax(jspScriptlet, p);
        p.Append("<%").Append(jspScriptlet.JspContent).Append("%>");
        AfterSyntax(jspScriptlet, p);
        return jspScriptlet;
    }

    public override Xml VisitJspExpression(JspExpression jspExpression, PrintOutputCapture<P> p)
    {
        BeforeSyntax(jspExpression, p);
        p.Append("<%=").Append(jspExpression.JspContent).Append("%>");
        AfterSyntax(jspExpression, p);
        return jspExpression;
    }

    public override Xml VisitJspDeclaration(JspDeclaration jspDeclaration, PrintOutputCapture<P> p)
    {
        BeforeSyntax(jspDeclaration, p);
        p.Append("<%!").Append(jspDeclaration.JspContent).Append("%>");
        AfterSyntax(jspDeclaration, p);
        return jspDeclaration;
    }

    public override Xml VisitJspComment(JspComment jspComment, PrintOutputCapture<P> p)
    {
        BeforeSyntax(jspComment, p);
        p.Append("<%--").Append(jspComment.JspContent).Append("--%>");
        AfterSyntax(jspComment, p);
        return jspComment;
    }

    private void BeforeSyntax(Xml x, PrintOutputCapture<P> p)
    {
        foreach (var marker in x.Markers.MarkerList)
        {
            p.Append(p.MarkerPrinter.BeforePrefix(marker, new Cursor(Cursor, marker), XmlMarkerWrapper));
        }
        p.Append(x.Prefix);
        foreach (var marker in x.Markers.MarkerList)
        {
            p.Append(p.MarkerPrinter.BeforeSyntax(marker, new Cursor(Cursor, marker), XmlMarkerWrapper));
        }
    }

    private void AfterSyntax(Xml x, PrintOutputCapture<P> p)
    {
        foreach (var marker in x.Markers.MarkerList)
        {
            p.Append(p.MarkerPrinter.AfterSyntax(marker, new Cursor(Cursor, marker), XmlMarkerWrapper));
        }
    }
}
