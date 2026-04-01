/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.csharp;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.csharp.tree.CsDocComment;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.tree.J;

public class CsDocCommentVisitor<P> extends TreeVisitor<CsDocComment, P> {
    /**
     * Don't use directly. Use {@link #csharpVisitorVisit(Tree, Object)} instead.
     */
    private final CSharpVisitor<P> csharpVisitor;

    public CsDocCommentVisitor(CSharpVisitor<P> csharpVisitor) {
        this.csharpVisitor = csharpVisitor;
    }

    protected @Nullable J csharpVisitorVisit(@Nullable Tree tree, P p) {
        Cursor previous = csharpVisitor.getCursor();
        J j = csharpVisitor.visit(tree, p, getCursor());
        csharpVisitor.setCursor(previous);
        return j;
    }

    public CsDocComment visitDocComment(CsDocComment.DocComment docComment, P p) {
        CsDocComment.DocComment d = docComment;
        d = d.withMarkers(visitMarkers(d.getMarkers(), p));
        return d.withBody(ListUtils.map(d.getBody(), b -> visit(b, p)));
    }

    public CsDocComment visitXmlElement(CsDocComment.XmlElement element, P p) {
        CsDocComment.XmlElement e = element;
        e = e.withMarkers(visitMarkers(e.getMarkers(), p));
        e = e.withAttributes(ListUtils.map(e.getAttributes(), attr -> visit(attr, p)));
        e = e.withSpaceBeforeClose(ListUtils.map(e.getSpaceBeforeClose(), s -> visit(s, p)));
        e = e.withContent(ListUtils.map(e.getContent(), c -> visit(c, p)));
        return e.withClosingTagSpaceBeforeClose(ListUtils.map(e.getClosingTagSpaceBeforeClose(), s -> visit(s, p)));
    }

    public CsDocComment visitXmlEmptyElement(CsDocComment.XmlEmptyElement element, P p) {
        CsDocComment.XmlEmptyElement e = element;
        e = e.withMarkers(visitMarkers(e.getMarkers(), p));
        e = e.withAttributes(ListUtils.map(e.getAttributes(), attr -> visit(attr, p)));
        return e.withSpaceBeforeSlashClose(ListUtils.map(e.getSpaceBeforeSlashClose(), s -> visit(s, p)));
    }

    public CsDocComment visitXmlText(CsDocComment.XmlText text, P p) {
        CsDocComment.XmlText t = text;
        return t.withMarkers(visitMarkers(t.getMarkers(), p));
    }

    public CsDocComment visitXmlAttribute(CsDocComment.XmlAttribute attribute, P p) {
        CsDocComment.XmlAttribute a = attribute;
        a = a.withMarkers(visitMarkers(a.getMarkers(), p));
        a = a.withSpaceBeforeEquals(ListUtils.map(a.getSpaceBeforeEquals(), s -> visit(s, p)));
        return a.withValue(ListUtils.map(a.getValue(), v -> visit(v, p)));
    }

    public CsDocComment visitXmlCrefAttribute(CsDocComment.XmlCrefAttribute attribute, P p) {
        CsDocComment.XmlCrefAttribute a = attribute;
        a = a.withMarkers(visitMarkers(a.getMarkers(), p));
        a = a.withSpaceBeforeEquals(ListUtils.map(a.getSpaceBeforeEquals(), s -> visit(s, p)));
        a = a.withValue(ListUtils.map(a.getValue(), v -> visit(v, p)));
        return a.withReference(csharpVisitorVisit(a.getReference(), p));
    }

    public CsDocComment visitXmlNameAttribute(CsDocComment.XmlNameAttribute attribute, P p) {
        CsDocComment.XmlNameAttribute a = attribute;
        a = a.withMarkers(visitMarkers(a.getMarkers(), p));
        a = a.withSpaceBeforeEquals(ListUtils.map(a.getSpaceBeforeEquals(), s -> visit(s, p)));
        a = a.withValue(ListUtils.map(a.getValue(), v -> visit(v, p)));
        return a.withParamName(csharpVisitorVisit(a.getParamName(), p));
    }

    public CsDocComment visitLineBreak(CsDocComment.LineBreak lineBreak, P p) {
        CsDocComment.LineBreak l = lineBreak;
        return l.withMarkers(visitMarkers(l.getMarkers(), p));
    }
}
