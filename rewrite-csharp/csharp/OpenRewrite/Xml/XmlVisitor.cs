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
/// Visitor for XML LST elements.
/// Dispatches to type-specific visit methods via switch pattern matching.
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

    public virtual Xml VisitDocument(Document document, P p) => document;
    public virtual Xml VisitProlog(Prolog prolog, P p) => prolog;
    public virtual Xml VisitXmlDecl(XmlDecl xmlDecl, P p) => xmlDecl;
    public virtual Xml VisitProcessingInstruction(ProcessingInstruction pi, P p) => pi;
    public virtual Xml VisitTag(Tag tag, P p) => tag;
    public virtual Xml VisitTagClosing(Tag.Closing closing, P p) => closing;
    public virtual Xml VisitAttribute(Attribute attribute, P p) => attribute;
    public virtual Xml VisitAttributeValue(Attribute.Value value, P p) => value;
    public virtual Xml VisitCharData(CharData charData, P p) => charData;
    public virtual Xml VisitComment(Comment comment, P p) => comment;
    public virtual Xml VisitDocTypeDecl(DocTypeDecl docTypeDecl, P p) => docTypeDecl;
    public virtual Xml VisitDocTypeDeclExternalSubsets(DocTypeDecl.ExternalSubsets externalSubsets, P p) => externalSubsets;
    public virtual Xml VisitElement(Element element, P p) => element;
    public virtual Xml VisitIdent(Ident ident, P p) => ident;
    public virtual Xml VisitJspDirective(JspDirective jspDirective, P p) => jspDirective;
    public virtual Xml VisitJspScriptlet(JspScriptlet jspScriptlet, P p) => jspScriptlet;
    public virtual Xml VisitJspExpression(JspExpression jspExpression, P p) => jspExpression;
    public virtual Xml VisitJspDeclaration(JspDeclaration jspDeclaration, P p) => jspDeclaration;
    public virtual Xml VisitJspComment(JspComment jspComment, P p) => jspComment;
}
