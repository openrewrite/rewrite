/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.java;

import org.openrewrite.Cursor;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Javadoc;

public class JavadocVisitor<P> extends TreeVisitor<Javadoc, P> {
    /**
     * Don't use directly. Use {@link #javaVisitorVisit(Tree, Object)} instead.
     */
    private final JavaVisitor<P> javaVisitor;

    public JavadocVisitor(JavaVisitor<P> javaVisitor) {
        this.javaVisitor = javaVisitor;
    }

    @Nullable
    protected J javaVisitorVisit(@Nullable Tree tree, P p) {
        Cursor previous = javaVisitor.getCursor();
        J j = javaVisitor.visit(tree, p, getCursor());
        javaVisitor.setCursor(previous);
        return j;
    }

    public Javadoc visitAttribute(Javadoc.Attribute attribute, P p) {
        Javadoc.Attribute a = attribute;
        a = a.withSpaceBeforeEqual(ListUtils.map(a.getSpaceBeforeEqual(), v -> visit(v, p)));
        a = a.withValue(ListUtils.map(a.getValue(), v -> visit(v, p)));
        return a;
    }

    public Javadoc visitAuthor(Javadoc.Author author, P p) {
        Javadoc.Author a = author;
        a = a.withMarkers(visitMarkers(a.getMarkers(), p));
        a = a.withName(ListUtils.map(a.getName(), name -> visit(name, p)));
        return a;
    }

    public Javadoc visitDeprecated(Javadoc.Deprecated deprecated, P p) {
        Javadoc.Deprecated d = deprecated;
        d = d.withMarkers(visitMarkers(d.getMarkers(), p));
        d = d.withDescription(ListUtils.map(d.getDescription(), desc -> visit(desc, p)));
        return d;
    }

    public Javadoc visitDocComment(Javadoc.DocComment javadoc, P p) {
        Javadoc.DocComment d = javadoc;
        d = d.withMarkers(visitMarkers(d.getMarkers(), p));
        d = d.withBody(ListUtils.map(d.getBody(), b -> visit(b, p)));
        return d;
    }

    public Javadoc visitDocRoot(Javadoc.DocRoot docRoot, P p) {
        Javadoc.DocRoot d = docRoot;
        d = d.withEndBrace(ListUtils.map(d.getEndBrace(), s -> visit(s, p)));
        return d;
    }

    public Javadoc visitDocType(Javadoc.DocType docType, P p) {
        Javadoc.DocType d = docType;
        d = d.withText(ListUtils.map(d.getText(), t -> visit(t, p)));
        return d;
    }

    public Javadoc visitEndElement(Javadoc.EndElement endElement, P p) {
        Javadoc.EndElement e = endElement;
        e = e.withSpaceBeforeEndBracket(ListUtils.map(e.getSpaceBeforeEndBracket(), s -> visit(s, p)));
        return e;
    }

    public Javadoc visitErroneous(Javadoc.Erroneous erroneous, P p) {
        Javadoc.Erroneous e = erroneous;
        e = e.withMarkers(visitMarkers(e.getMarkers(), p));
        e = e.withText(ListUtils.map(e.getText(), b -> visit(b, p)));
        return e;
    }

    public Javadoc visitHidden(Javadoc.Hidden hidden, P p) {
        Javadoc.Hidden h = hidden;
        h = h.withMarkers(visitMarkers(h.getMarkers(), p));
        h = h.withBody(ListUtils.map(h.getBody(), b -> visit(b, p)));
        return h;
    }

    public Javadoc visitIndex(Javadoc.Index index, P p) {
        Javadoc.Index i = index;
        i = i.withSearchTerm(ListUtils.map(i.getSearchTerm(), s -> visit(s, p)));
        i = i.withDescription(ListUtils.map(i.getDescription(), desc -> visit(desc, p)));
        i = i.withEndBrace(ListUtils.map(i.getEndBrace(), b -> visit(b, p)));
        return i;
    }

    public Javadoc visitInheritDoc(Javadoc.InheritDoc inheritDoc, P p) {
        Javadoc.InheritDoc i = inheritDoc;
        i = i.withEndBrace(ListUtils.map(i.getEndBrace(), b -> visit(b, p)));
        return i;
    }

    public Javadoc visitInlinedValue(Javadoc.InlinedValue inlinedValue, P p) {
        Javadoc.InlinedValue i = inlinedValue;
        i = i.withSpaceBeforeTree(ListUtils.map(i.getSpaceBeforeTree(), s -> visit(s, p)));
        i = i.withTree(javaVisitorVisit(i.getTree(), p));
        i = i.withEndBrace(ListUtils.map(i.getEndBrace(), b -> visit(b, p)));
        return i;
    }

    public Javadoc visitLineBreak(Javadoc.LineBreak lineBreak, P p) {
        Javadoc.LineBreak l = lineBreak;
        l = l.withMarkers(visitMarkers(l.getMarkers(), p));
        return l;
    }

    public Javadoc visitLink(Javadoc.Link link, P p) {
        Javadoc.Link l = link;
        l = l.withMarkers(visitMarkers(l.getMarkers(), p));
        l = l.withSpaceBeforeTree(ListUtils.map(l.getSpaceBeforeTree(), s -> visit(s, p)));
        l = l.withTreeReference(visitAndCast(l.getTreeReference() , p));
        l = l.withLabel(ListUtils.map(l.getLabel(), la -> visit(la, p)));
        l = l.withEndBrace(ListUtils.map(l.getEndBrace(), s -> visit(s, p)));
        return l;
    }

    public Javadoc visitLiteral(Javadoc.Literal literal, P p) {
        Javadoc.Literal l = literal;
        l = l.withMarkers(visitMarkers(l.getMarkers(), p));
        l = l.withDescription(ListUtils.map(l.getDescription(), desc -> visit(desc, p)));
        return l;
    }

    public Javadoc visitParameter(Javadoc.Parameter parameter, P p) {
        Javadoc.Parameter pa = parameter;
        pa = pa.withMarkers(visitMarkers(pa.getMarkers(), p));
        pa = pa.withSpaceBeforeName(ListUtils.map(pa.getSpaceBeforeName(), b -> visit(b, p)));
        pa = pa.withNameReference(visitAndCast(pa.getNameReference(), p));
        pa = pa.withDescription(ListUtils.map(pa.getDescription(), desc -> visit(desc, p)));
        return pa;
    }

    public Javadoc visitProvides(Javadoc.Provides provides, P p) {
        Javadoc.Provides pr = provides;
        pr = pr.withSpaceBeforeServiceType(ListUtils.map(pr.getSpaceBeforeServiceType(), s -> visit(s, p)));
        pr = pr.withServiceType(javaVisitorVisit(pr.getServiceType(), p));
        pr = pr.withDescription(ListUtils.map(pr.getDescription(), d -> visit(d, p)));
        return pr;
    }

    public Javadoc visitReturn(Javadoc.Return aReturn, P p) {
        Javadoc.Return r = aReturn;
        r = r.withMarkers(visitMarkers(r.getMarkers(), p));
        r = r.withDescription(ListUtils.map(r.getDescription(), desc -> visit(desc, p)));
        return r;
    }

    public Javadoc visitSee(Javadoc.See see, P p) {
        Javadoc.See s = see;
        s = s.withMarkers(visitMarkers(s.getMarkers(), p));
        s = s.withSpaceBeforeTree(ListUtils.map(s.getSpaceBeforeTree(), sb -> visit(sb, p)));
        s = s.withTreeReference(visitAndCast(s.getTreeReference() , p));
        s = s.withReference(ListUtils.map(s.getReference(), desc -> visit(desc, p)));
        return s;
    }

    public Javadoc visitSerial(Javadoc.Serial serial, P p) {
        Javadoc.Serial s = serial;
        s = s.withMarkers(visitMarkers(s.getMarkers(), p));
        s = s.withDescription(ListUtils.map(s.getDescription(), desc -> visit(desc, p)));
        return s;
    }

    public Javadoc visitSerialData(Javadoc.SerialData serialData, P p) {
        Javadoc.SerialData s = serialData;
        s = s.withMarkers(visitMarkers(s.getMarkers(), p));
        s = s.withDescription(ListUtils.map(s.getDescription(), desc -> visit(desc, p)));
        return s;
    }

    public Javadoc visitSerialField(Javadoc.SerialField serialField, P p) {
        Javadoc.SerialField s = serialField;
        s = s.withMarkers(visitMarkers(s.getMarkers(), p));
        s = s.withName((J.Identifier) javaVisitorVisit(s.getName(), p));
        s = s.withType(javaVisitorVisit(s.getType(), p));
        s = s.withDescription(ListUtils.map(s.getDescription(), desc -> visit(desc, p)));
        return s;
    }

    public Javadoc visitSince(Javadoc.Since since, P p) {
        Javadoc.Since s = since;
        s = s.withMarkers(visitMarkers(s.getMarkers(), p));
        s = s.withDescription(ListUtils.map(s.getDescription(), desc -> visit(desc, p)));
        return s;
    }

    public Javadoc visitStartElement(Javadoc.StartElement startElement, P p) {
        Javadoc.StartElement s = startElement;
        s = s.withAttributes(ListUtils.map(s.getAttributes(), attr -> visit(attr, p)));
        s = s.withSpaceBeforeEndBracket(ListUtils.map(s.getSpaceBeforeEndBracket(), b -> visit(b, p)));
        return s;
    }

    public Javadoc visitSummary(Javadoc.Summary summary, P p) {
        Javadoc.Summary s = summary;
        s = s.withSummary(ListUtils.map(s.getSummary(), sum -> visit(sum, p)));
        s = s.withBeforeBrace(ListUtils.map(s.getBeforeBrace(), b -> visit(b, p)));
        return s;
    }

    public Javadoc visitText(Javadoc.Text text, P p) {
        Javadoc.Text t = text;
        t = t.withMarkers(visitMarkers(t.getMarkers(), p));
        return t;
    }

    public Javadoc visitThrows(Javadoc.Throws aThrows, P p) {
        Javadoc.Throws e = aThrows;
        e = e.withMarkers(visitMarkers(e.getMarkers(), p));
        e = e.withExceptionName(javaVisitorVisit(e.getExceptionName(), p));
        e = e.withSpaceBeforeExceptionName(ListUtils.map(e.getSpaceBeforeExceptionName(), s -> visit(s, p)));
        e = e.withDescription(ListUtils.map(e.getDescription(), desc -> visit(desc, p)));
        return e;
    }

    public Javadoc visitUnknownBlock(Javadoc.UnknownBlock unknownBlock, P p) {
        Javadoc.UnknownBlock u = unknownBlock;
        u = u.withContent(ListUtils.map(u.getContent(), c -> visit(c, p)));
        return u;
    }

    public Javadoc visitUnknownInline(Javadoc.UnknownInline unknownInline, P p) {
        Javadoc.UnknownInline u = unknownInline;
        u = u.withContent(ListUtils.map(u.getContent(), c -> visit(c, p)));
        u = u.withEndBrace(ListUtils.map(u.getEndBrace(), b -> visit(b, p)));
        return u;
    }

    public Javadoc visitUses(Javadoc.Uses uses, P p) {
        Javadoc.Uses u = uses;
        u = u.withBeforeServiceType(ListUtils.map(u.getBeforeServiceType(), b -> visit(b, p)));
        u = u.withServiceType(javaVisitorVisit(u.getServiceType(), p));
        u = u.withDescription(ListUtils.map(u.getDescription(), d -> visit(d, p)));
        return u;
    }

    public Javadoc visitVersion(Javadoc.Version version, P p) {
        Javadoc.Version v = version;
        v = v.withMarkers(visitMarkers(v.getMarkers(), p));
        v = v.withBody(ListUtils.map(v.getBody(), b -> visit(b, p)));
        return v;
    }

    public Javadoc visitReference(Javadoc.Reference reference, P p) {
        Javadoc.Reference r = reference;
        r = r.withTree(javaVisitorVisit(r.getTree(), p));
        r = r.withLineBreaks(ListUtils.map(r.getLineBreaks(), l -> visit(l, p)));
        return r;
    }
}
