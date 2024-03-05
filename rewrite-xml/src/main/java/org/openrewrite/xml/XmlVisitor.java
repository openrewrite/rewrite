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

import org.openrewrite.Cursor;
import org.openrewrite.SourceFile;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.xml.format.AutoFormatVisitor;
import org.openrewrite.xml.tree.Xml;

public class XmlVisitor<P> extends TreeVisitor<Xml, P> {

    @Override
    public boolean isAcceptable(SourceFile sourceFile, P p) {
        return sourceFile instanceof Xml.Document;
    }

    @Override
    public String getLanguage() {
        return "xml";
    }

    public <X extends Xml> X maybeAutoFormat(X before, X after, P p) {
        return maybeAutoFormat(before, after, p, getCursor());
    }

    public <X extends Xml> X maybeAutoFormat(X before, X after, P p, Cursor cursor) {
        return maybeAutoFormat(before, after, null, p, cursor);
    }

    public <X extends Xml> X maybeAutoFormat(X before, X after, @Nullable Xml stopAfter, P p, Cursor cursor) {
        if (before != after) {
            //noinspection unchecked
            return (X) new AutoFormatVisitor<>(stopAfter).visitNonNull(after, p, cursor);
        }
        return after;
    }

    public <X extends Xml> X autoFormat(X j, P p) {
        return autoFormat(j, p, getCursor().getParentOrThrow());
    }

    public <X extends Xml> X autoFormat(X j, P p, Cursor cursor) {
        return autoFormat(j, null, p, cursor);
    }

    public <X extends Xml> X autoFormat(X j, @Nullable Xml stopAfter, P p, Cursor cursor) {
        //noinspection unchecked
        return (X) new AutoFormatVisitor<>(stopAfter).visitNonNull(j, p, cursor);
    }

    public Xml visitDocument(Xml.Document document, P p) {
        Xml.Document d = document;
        d = d.withMarkers(visitMarkers(d.getMarkers(), p));
        d = d.withProlog(visitAndCast(d.getProlog(), p));
        d = d.withRoot(visitAndCast(d.getRoot(), p));
        return d;
    }

    public Xml visitXmlDecl(Xml.XmlDecl xmlDecl, P p) {
        Xml.XmlDecl x = xmlDecl.withMarkers(visitMarkers(xmlDecl.getMarkers(), p));
        return x.withAttributes(ListUtils.map(x.getAttributes(), a -> visitAndCast(a, p)));
    }

    public Xml visitProcessingInstruction(Xml.ProcessingInstruction processingInstruction, P p) {
        Xml.ProcessingInstruction pi = processingInstruction.withMarkers(visitMarkers(processingInstruction.getMarkers(), p));
        pi = pi.withProcessingInstructions(visitAndCast(pi.getProcessingInstructions(), p));
        return pi;
    }

    public Xml visitTag(Xml.Tag tag, P p) {
        Xml.Tag t = tag;
        t = t.withMarkers(visitMarkers(t.getMarkers(), p));
        t = t.withAttributes(ListUtils.map(t.getAttributes(), a -> visitAndCast(a, p)));
        if (t.getContent() != null) {
            t = t.withContent(ListUtils.map(t.getContent(), c -> visitAndCast(c, p)));
        }
        t = t.withClosing(visitAndCast(t.getClosing(), p));
        return t;
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
        d = d.withMarkers(visitMarkers(d.getMarkers(), p));
        d = d.withInternalSubset(ListUtils.map(d.getInternalSubset(), i -> visitAndCast(i, p)));
        d = d.withExternalSubsets(visitAndCast(d.getExternalSubsets(), p));
        return d;
    }

    public Xml visitProlog(Xml.Prolog prolog, P p) {
        Xml.Prolog pl = prolog;
        pl = pl.withMarkers(visitMarkers(pl.getMarkers(), p));
        pl = pl.withXmlDecl(visitAndCast(prolog.getXmlDecl(), p));
        pl = pl.withMisc(ListUtils.map(pl.getMisc(), m -> visitAndCast(m, p)));
        pl = pl.withJspDirectives(ListUtils.map(pl.getJspDirectives(), m -> visitAndCast(m, p)));
        return pl;
    }

    public Xml visitIdent(Xml.Ident ident, P p) {
        return ident.withMarkers(visitMarkers(ident.getMarkers(), p));
    }

    public Xml visitElement(Xml.Element element, P p) {
        Xml.Element e = element;
        e = e.withMarkers(visitMarkers(e.getMarkers(), p));
        e = e.withSubset(ListUtils.map(e.getSubset(), i -> visitAndCast(i, p)));
        return e;
    }

    public Xml visitJspDirective(Xml.JspDirective jspDirective, P p) {
        Xml.JspDirective j = jspDirective;
        j = j.withMarkers(visitMarkers(j.getMarkers(), p));
        j = j.withAttributes(ListUtils.map(j.getAttributes(), a -> visitAndCast(a, p)));
        return j;
    }
}
