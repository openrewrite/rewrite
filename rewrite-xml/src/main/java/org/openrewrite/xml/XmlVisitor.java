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
package org.openrewrite.xml;

import org.openrewrite.ExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.xml.tree.Xml;

public class XmlVisitor<P> extends TreeVisitor<Xml, P> {

    @Override
    public boolean isAcceptable(SourceFile sourceFile, ExecutionContext ctx) {
        return sourceFile instanceof Xml.Document;
    }

    @Override
    public String getLanguage() {
        return "xml";
    }

    public Xml visitDocument(Xml.Document document, P p) {
        Xml.Document d = document;
        d = d.withProlog(visitAndCast(d.getProlog(), p));
        d = d.withRoot(visitAndCast(d.getRoot(), p));
        return d.withMarkers(visitMarkers(d.getMarkers(), p));
    }

    public Xml visitXmlDecl(Xml.XmlDecl xmlDecl, P p) {
        xmlDecl = xmlDecl.withAttributes(ListUtils.map(xmlDecl.getAttributes(), a -> visitAndCast(a, p)));
        return xmlDecl.withMarkers(visitMarkers(xmlDecl.getMarkers(), p));
    }

    public Xml visitProcessingInstruction(Xml.ProcessingInstruction pi, P p) {
        pi = pi.withProcessingInstructions(visitAndCast(pi.getProcessingInstructions(), p));
        return pi.withMarkers(visitMarkers(pi.getMarkers(), p));
    }

    public Xml visitTag(Xml.Tag tag, P p) {
        Xml.Tag t = tag;
        t = t.withAttributes(ListUtils.map(t.getAttributes(), a -> visitAndCast(a, p)));
        if(t.getContent() != null) {
            t = t.withContent(ListUtils.map(t.getContent(), c -> visitAndCast(c, p)));
        }
        t = t.withClosing(visitAndCast(t.getClosing(), p));
        return t.withMarkers(visitMarkers(t.getMarkers(), p));
    }

    public Xml visitAttribute(Xml.Attribute attribute, P p) {
        return attribute.withMarkers(visitMarkers(attribute.getMarkers(), p));
    }

    public Xml visitCharData(Xml.CharData charData, P p) {
        return charData.withMarkers(visitMarkers(charData.getMarkers(), p));
    }

    public Xml visitComment(Xml.Comment comment, P p) {
        return comment.withMarkers(visitMarkers(comment.getMarkers(), p));
    }

    public Xml visitDocTypeDecl(Xml.DocTypeDecl docTypeDecl, P p) {
        Xml.DocTypeDecl d = docTypeDecl;
        d = d.withInternalSubset(ListUtils.map(d.getInternalSubset(), i -> visitAndCast(i, p)));
        d = d.withExternalSubsets(visitAndCast(d.getExternalSubsets(), p));
        return d.withMarkers(visitMarkers(d.getMarkers(), p));
    }

    public Xml visitProlog(Xml.Prolog prolog, P p) {
        Xml.Prolog pl = prolog;
        pl = pl.withXmlDecl(visitAndCast(prolog.getXmlDecl(), p));
        pl = pl.withMisc(ListUtils.map(pl.getMisc(), m -> visitAndCast(m, p)));
        return pl.withMarkers(visitMarkers(pl.getMarkers(), p));
    }

    public Xml visitIdent(Xml.Ident ident, P p) {
        return ident.withMarkers(visitMarkers(ident.getMarkers(), p));
    }

    public Xml visitElement(Xml.Element element, P p) {
        return element.withSubset(ListUtils.map(element.getSubset(), i -> visitAndCast(i, p)));
    }
}
