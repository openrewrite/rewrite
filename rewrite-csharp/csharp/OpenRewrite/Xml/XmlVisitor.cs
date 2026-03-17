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
/// Visitor for XML LST elements with recursive child traversal.
/// Dispatches to type-specific visit methods via switch pattern matching.
/// Mirrors Java's org.openrewrite.xml.XmlVisitor.
/// </summary>
public class XmlVisitor<P> : TreeVisitor<Xml, P>
{
    protected override Xml? Accept(Xml tree, P p)
    {
        return tree switch
        {
            Document doc => VisitDocument(doc, p),
            Prolog pro => VisitProlog(pro, p),
            XmlDecl xd => VisitXmlDecl(xd, p),
            ProcessingInstruction pi => VisitProcessingInstruction(pi, p),
            Tag.Closing closing => VisitTagClosing(closing, p),
            Tag tag => VisitTag(tag, p),
            Attribute.Value val => VisitAttributeValue(val, p),
            Attribute attr => VisitAttribute(attr, p),
            CharData cd => VisitCharData(cd, p),
            Comment comment => VisitComment(comment, p),
            DocTypeDecl.ExternalSubsets es => VisitDocTypeDeclExternalSubsets(es, p),
            DocTypeDecl dtd => VisitDocTypeDecl(dtd, p),
            Element elem => VisitElement(elem, p),
            Ident ident => VisitIdent(ident, p),
            JspDirective jd => VisitJspDirective(jd, p),
            JspScriptlet js => VisitJspScriptlet(js, p),
            JspExpression je => VisitJspExpression(je, p),
            JspDeclaration jdec => VisitJspDeclaration(jdec, p),
            JspComment jc => VisitJspComment(jc, p),
            _ => throw new InvalidOperationException($"Unknown XML tree type: {tree.GetType()}")
        };
    }

    private T? VisitAndCast<T>(T? tree, P p) where T : class, Xml
    {
        return (T?)Visit(tree, p);
    }

    private IList<T> MapList<T>(IList<T> list, Func<T, T?> mapper) where T : class
    {
        IList<T>? result = null;
        for (int i = 0; i < list.Count; i++)
        {
            var original = list[i];
            var mapped = mapper(original);
            if (mapped == null)
            {
                result ??= new List<T>(list.Take(i));
            }
            else if (result != null)
            {
                result.Add(mapped);
            }
            else if (!ReferenceEquals(original, mapped))
            {
                result = new List<T>(list.Take(i)) { mapped };
            }
        }
        return result ?? list;
    }

    public virtual Xml VisitDocument(Document document, P p)
    {
        var d = document;
        if (d.Prolog != null)
            d = d.WithProlog(VisitAndCast(d.Prolog, p));
        d = d.WithRoot((Tag)VisitNonNull(d.Root, p));
        return d;
    }

    public virtual Xml VisitProlog(Prolog prolog, P p)
    {
        var pl = prolog;
        if (pl.XmlDecl != null)
            pl = pl.WithXmlDecl(VisitAndCast(pl.XmlDecl, p));
        pl = pl.WithMiscList(MapList(pl.MiscList, m => (Misc?)Visit(m, p)));
        pl = pl.WithJspDirectives(MapList(pl.JspDirectives, j => VisitAndCast(j, p)));
        return pl;
    }

    public virtual Xml VisitXmlDecl(XmlDecl xmlDecl, P p)
    {
        return xmlDecl.WithAttributes(MapList(xmlDecl.Attributes, a => VisitAndCast(a, p)));
    }

    public virtual Xml VisitProcessingInstruction(ProcessingInstruction pi, P p)
    {
        return pi.WithProcessingInstructions((CharData)VisitNonNull(pi.ProcessingInstructions, p));
    }

    public virtual Xml VisitTag(Tag tag, P p)
    {
        var t = tag;
        t = t.WithAttributes(MapList(t.Attributes, a => VisitAndCast(a, p)));
        if (t.ContentList != null)
            t = t.WithContentList(MapList(t.ContentList, c => (Content?)Visit(c, p)));
        if (t.ClosingTag != null)
            t = t.WithClosingTag(VisitAndCast(t.ClosingTag, p));
        return t;
    }

    public virtual Xml VisitTagClosing(Tag.Closing closing, P p) => closing;

    public virtual Xml VisitAttribute(Attribute attribute, P p)
    {
        return attribute.WithVal((Attribute.Value)VisitNonNull(attribute.Val, p));
    }

    public virtual Xml VisitAttributeValue(Attribute.Value value, P p) => value;
    public virtual Xml VisitCharData(CharData charData, P p) => charData;
    public virtual Xml VisitComment(Comment comment, P p) => comment;

    public virtual Xml VisitDocTypeDecl(DocTypeDecl docTypeDecl, P p)
    {
        var d = docTypeDecl;
        d = d.WithInternalSubset(MapList(d.InternalSubset, i => VisitAndCast(i, p)));
        if (d.ExternalSubsetsNode != null)
            d = d.WithExternalSubsetsNode(VisitAndCast(d.ExternalSubsetsNode, p));
        return d;
    }

    public virtual Xml VisitDocTypeDeclExternalSubsets(DocTypeDecl.ExternalSubsets externalSubsets, P p)
    {
        return externalSubsets.WithElements(MapList(externalSubsets.Elements, e => VisitAndCast(e, p)));
    }

    public virtual Xml VisitElement(Element element, P p)
    {
        return element.WithSubset(MapList(element.Subset, i => VisitAndCast(i, p)));
    }

    public virtual Xml VisitIdent(Ident ident, P p) => ident;

    public virtual Xml VisitJspDirective(JspDirective jspDirective, P p)
    {
        return jspDirective.WithAttributes(MapList(jspDirective.Attributes, a => VisitAndCast(a, p)));
    }

    public virtual Xml VisitJspScriptlet(JspScriptlet jspScriptlet, P p) => jspScriptlet;
    public virtual Xml VisitJspExpression(JspExpression jspExpression, P p) => jspExpression;
    public virtual Xml VisitJspDeclaration(JspDeclaration jspDeclaration, P p) => jspDeclaration;
    public virtual Xml VisitJspComment(JspComment jspComment, P p) => jspComment;
}
