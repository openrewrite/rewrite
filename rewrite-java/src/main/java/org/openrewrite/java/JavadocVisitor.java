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

import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Javadoc;

public class JavadocVisitor<P> extends TreeVisitor<Javadoc, P> {
    protected JavaVisitor<P> javaVisitor;

    public JavadocVisitor() {
        this(new JavaVisitor<>());
    }

    public JavadocVisitor(JavaVisitor<P> javaVisitor) {
        this.javaVisitor = javaVisitor;
    }

    public Javadoc visitAttribute(Javadoc.Attribute attribute, P p) {
        Javadoc.Attribute a = attribute;
        a = a.withBeforeEqual(ListUtils.map(a.getBeforeEqual(), v -> visit(v, p)));
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
        return docRoot;
    }

    public Javadoc visitDocType(Javadoc.DocType docType, P p) {
        return docType;
    }

    public Javadoc visitEndElement(Javadoc.EndElement endElement, P p) {
        return endElement;
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
        i = i.withSearchTerm(visit(i.getSearchTerm(), p));
        i = i.withDescription(ListUtils.map(i.getDescription(), desc -> visit(desc, p)));
        return i;
    }

    public Javadoc visitInheritDoc(Javadoc.InheritDoc inheritDoc, P p) {
        return inheritDoc;
    }

    public Javadoc visitInlinedValue(Javadoc.InlinedValue inlinedValue, P p) {
        Javadoc.InlinedValue i = inlinedValue;
        i = i.withTree(javaVisitor.visit(i.getTree(), p));
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
        l = l.withTree(javaVisitor.visit(l.getTree(), p));
        l = l.withLabel(ListUtils.map(l.getLabel(), la -> visit(la, p)));
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
        pa = pa.withName(javaVisitor.visit(pa.getName(), p));
        pa = pa.withDescription(ListUtils.map(pa.getDescription(), desc -> visit(desc, p)));
        return pa;
    }

    public Javadoc visitProvides(Javadoc.Provides provides, P p) {
        Javadoc.Provides pr = provides;
        pr = pr.withServiceType(javaVisitor.visit(pr.getServiceType(), p));
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
        s = s.withTree(javaVisitor.visit(s.getTree(), p));
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
        s = s.withName((J.Identifier) javaVisitor.visit(s.getName(), p));
        s = s.withType(javaVisitor.visit(s.getType(), p));
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
        return s;
    }

    public Javadoc visitSummary(Javadoc.Summary summary, P p) {
        Javadoc.Summary s = summary;
        s = s.withSummary(ListUtils.map(s.getSummary(), sum -> visit(sum, p)));
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
        e = e.withDescription(ListUtils.map(e.getDescription(), desc -> visit(desc, p)));
        return e;
    }

    public Javadoc visitUnknownBlock(Javadoc.UnknownBlock unknownBlock, P p) {
        Javadoc.UnknownBlock u = unknownBlock;
        u = u.withContent(ListUtils.map(u.getContent(), c -> visit(c, p)));
        return u;
    }

    public Javadoc visitUnknownInline(Javadoc.UnknownInline unknownInline, P p) {
        return unknownInline;
    }

    public Javadoc visitUses(Javadoc.Uses uses, P p) {
        Javadoc.Uses u = uses;
        u = u.withServiceType(javaVisitor.visit(u.getServiceType(), p));
        u = u.withDescription(ListUtils.map(u.getDescription(), d -> visit(d, p)));
        return u;
    }

    public Javadoc visitVersion(Javadoc.Version version, P p) {
        Javadoc.Version v = version;
        v = v.withMarkers(visitMarkers(v.getMarkers(), p));
        v = v.withBody(ListUtils.map(v.getBody(), b -> visit(b, p)));
        return v;
    }
}
