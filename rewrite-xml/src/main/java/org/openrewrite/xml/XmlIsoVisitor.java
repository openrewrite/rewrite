/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.xml;

import org.openrewrite.xml.tree.Xml;

public class XmlIsoVisitor<P> extends XmlVisitor<P> {
    @Override
    public Xml.Document visitDocument(Xml.Document document, P p) {
        return (Xml.Document) super.visitDocument(document, p);
    }

    @Override
    public Xml.XmlDecl visitXmlDecl(Xml.XmlDecl xmlDecl, P p) {
        return (Xml.XmlDecl) super.visitXmlDecl(xmlDecl, p);
    }

    @Override
    public Xml.ProcessingInstruction visitProcessingInstruction(Xml.ProcessingInstruction processingInstruction, P p) {
        return (Xml.ProcessingInstruction) super.visitProcessingInstruction(processingInstruction, p);
    }

    @Override
    public Xml.Tag visitTag(Xml.Tag tag, P p) {
        return (Xml.Tag) super.visitTag(tag, p);
    }

    @Override
    public Xml.Attribute visitAttribute(Xml.Attribute attribute, P p) {
        return (Xml.Attribute) super.visitAttribute(attribute, p);
    }

    @Override
    public Xml.CharData visitCharData(Xml.CharData charData, P p) {
        return (Xml.CharData) super.visitCharData(charData, p);
    }

    @Override
    public Xml.Comment visitComment(Xml.Comment comment, P p) {
        return (Xml.Comment) super.visitComment(comment, p);
    }

    @Override
    public Xml.DocTypeDecl visitDocTypeDecl(Xml.DocTypeDecl docTypeDecl, P p) {
        return (Xml.DocTypeDecl) super.visitDocTypeDecl(docTypeDecl, p);
    }

    @Override
    public Xml.Prolog visitProlog(Xml.Prolog prolog, P p) {
        return (Xml.Prolog) super.visitProlog(prolog, p);
    }

    @Override
    public Xml.Ident visitIdent(Xml.Ident ident, P p) {
        return (Xml.Ident) super.visitIdent(ident, p);
    }

    @Override
    public Xml.JspDirective visitJspDirective(Xml.JspDirective jspDirective, P p) {
        return (Xml.JspDirective) super.visitJspDirective(jspDirective, p);
    }

    @Override
    public Xml.Element visitElement(Xml.Element element, P p) {
        return (Xml.Element) super.visitElement(element, p);
    }
}
