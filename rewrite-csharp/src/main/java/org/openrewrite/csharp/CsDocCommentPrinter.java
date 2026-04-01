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
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.csharp.tree.CsDocComment;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.Markers;

import java.util.List;
import java.util.function.UnaryOperator;

public class CsDocCommentPrinter<P> extends CsDocCommentVisitor<PrintOutputCapture<P>> {
    public CsDocCommentPrinter() {
        super(new CsDocCommentCSharpPrinter<>());
    }

    @Override
    public CsDocComment visitDocComment(CsDocComment.DocComment docComment, PrintOutputCapture<P> p) {
        beforeSyntax(docComment, p, MARKER_WRAPPER);
        p.append("///");
        visit(docComment.getBody(), p);
        afterSyntax(docComment, p);
        return docComment;
    }

    @Override
    public CsDocComment visitXmlElement(CsDocComment.XmlElement element, PrintOutputCapture<P> p) {
        beforeSyntax(element, p);
        // Opening tag
        p.append('<').append(element.getName());
        visit(element.getAttributes(), p);
        visit(element.getSpaceBeforeClose(), p);
        p.append('>');
        // Content
        visit(element.getContent(), p);
        // Closing tag
        p.append("</").append(element.getName());
        visit(element.getClosingTagSpaceBeforeClose(), p);
        p.append('>');
        afterSyntax(element, p);
        return element;
    }

    @Override
    public CsDocComment visitXmlEmptyElement(CsDocComment.XmlEmptyElement element, PrintOutputCapture<P> p) {
        beforeSyntax(element, p);
        p.append('<').append(element.getName());
        visit(element.getAttributes(), p);
        visit(element.getSpaceBeforeSlashClose(), p);
        p.append("/>");
        afterSyntax(element, p);
        return element;
    }

    @Override
    public CsDocComment visitXmlText(CsDocComment.XmlText text, PrintOutputCapture<P> p) {
        beforeSyntax(text, p);
        p.append(text.getText());
        afterSyntax(text, p);
        return text;
    }

    @Override
    public CsDocComment visitXmlAttribute(CsDocComment.XmlAttribute attribute, PrintOutputCapture<P> p) {
        beforeSyntax(attribute, p);
        p.append(attribute.getName());
        if (attribute.getSpaceBeforeEquals() != null && !attribute.getSpaceBeforeEquals().isEmpty()) {
            visit(attribute.getSpaceBeforeEquals(), p);
            if (attribute.getValue() != null) {
                p.append('=');
                visit(attribute.getValue(), p);
            }
        }
        afterSyntax(attribute, p);
        return attribute;
    }

    @Override
    public CsDocComment visitXmlCrefAttribute(CsDocComment.XmlCrefAttribute attribute, PrintOutputCapture<P> p) {
        beforeSyntax(attribute, p);
        p.append("cref");
        if (attribute.getSpaceBeforeEquals() != null && !attribute.getSpaceBeforeEquals().isEmpty()) {
            visit(attribute.getSpaceBeforeEquals(), p);
            if (attribute.getValue() != null) {
                p.append('=');
                visit(attribute.getValue(), p);
            }
        }
        afterSyntax(attribute, p);
        return attribute;
    }

    @Override
    public CsDocComment visitXmlNameAttribute(CsDocComment.XmlNameAttribute attribute, PrintOutputCapture<P> p) {
        beforeSyntax(attribute, p);
        p.append("name");
        if (attribute.getSpaceBeforeEquals() != null && !attribute.getSpaceBeforeEquals().isEmpty()) {
            visit(attribute.getSpaceBeforeEquals(), p);
            if (attribute.getValue() != null) {
                p.append('=');
                visit(attribute.getValue(), p);
            }
        }
        afterSyntax(attribute, p);
        return attribute;
    }

    @Override
    public CsDocComment visitLineBreak(CsDocComment.LineBreak lineBreak, PrintOutputCapture<P> p) {
        beforeSyntax(lineBreak, p, MARKER_WRAPPER);
        p.append(lineBreak.getMargin());
        afterSyntax(lineBreak, p);
        return lineBreak;
    }

    public void visit(@Nullable List<? extends CsDocComment> nodes, PrintOutputCapture<P> p) {
        if (nodes != null) {
            for (CsDocComment node : nodes) {
                visit(node, p);
            }
        }
    }

    private static final UnaryOperator<String> MARKER_WRAPPER =
            out -> "~~" + out + (out.isEmpty() ? "" : "~~") + ">";

    private static final UnaryOperator<String> CSHARP_MARKER_WRAPPER =
            out -> "/*~~" + out + (out.isEmpty() ? "" : "~~") + ">*/";

    private void beforeSyntax(CsDocComment j, PrintOutputCapture<P> p) {
        beforeSyntax(j, p, MARKER_WRAPPER);
    }

    private void beforeSyntax(CsDocComment j, PrintOutputCapture<P> p, UnaryOperator<String> commentWrapper) {
        beforeSyntax(j.getMarkers(), p, commentWrapper);
    }

    private void beforeSyntax(Markers markers, PrintOutputCapture<P> p, UnaryOperator<String> commentWrapper) {
        visitMarkers(markers, p);
        for (Marker marker : markers.getMarkers()) {
            p.append(p.getMarkerPrinter().beforeSyntax(marker, new Cursor(getCursor(), marker), commentWrapper));
        }
    }

    private void afterSyntax(CsDocComment j, PrintOutputCapture<P> p) {
        afterSyntax(j.getMarkers(), p);
    }

    private void afterSyntax(Markers markers, PrintOutputCapture<P> p) {
        for (Marker marker : markers.getMarkers()) {
            p.append(p.getMarkerPrinter().afterSyntax(marker, new Cursor(getCursor(), marker), MARKER_WRAPPER));
        }
    }

    /**
     * Inner printer for handling embedded C# (J) references within cref attributes.
     */
    static class CsDocCommentCSharpPrinter<P> extends CSharpVisitor<PrintOutputCapture<P>> {
        @Override
        public J visitIdentifier(J.Identifier ident, PrintOutputCapture<P> p) {
            beforeSyntax(ident, p);
            p.append(ident.getSimpleName());
            afterSyntax(ident, p);
            return ident;
        }

        @Override
        public J visitFieldAccess(J.FieldAccess fieldAccess, PrintOutputCapture<P> p) {
            beforeSyntax(fieldAccess, p);
            visit(fieldAccess.getTarget(), p);
            p.append('.');
            visit(fieldAccess.getName(), p);
            afterSyntax(fieldAccess, p);
            return fieldAccess;
        }

        private void beforeSyntax(J j, PrintOutputCapture<P> p) {
            for (Marker marker : j.getMarkers().getMarkers()) {
                p.append(p.getMarkerPrinter().beforePrefix(marker, new Cursor(getCursor(), marker), CSHARP_MARKER_WRAPPER));
            }
            p.append(j.getPrefix().getWhitespace());
            visitMarkers(j.getMarkers(), p);
            for (Marker marker : j.getMarkers().getMarkers()) {
                p.append(p.getMarkerPrinter().beforeSyntax(marker, new Cursor(getCursor(), marker), CSHARP_MARKER_WRAPPER));
            }
        }

        private void afterSyntax(J j, PrintOutputCapture<P> p) {
            for (Marker marker : j.getMarkers().getMarkers()) {
                p.append(p.getMarkerPrinter().afterSyntax(marker, new Cursor(getCursor(), marker), CSHARP_MARKER_WRAPPER));
            }
        }
    }
}
