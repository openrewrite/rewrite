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
package org.openrewrite.xml.internal;

import org.openrewrite.Cursor;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.marker.Marker;
import org.openrewrite.xml.XmlVisitor;
import org.openrewrite.xml.tree.Xml;

import java.util.function.UnaryOperator;

public class XmlPrinter<P> extends XmlVisitor<PrintOutputCapture<P>> {

    @Override
    public Xml visitDocument(Xml.Document document, PrintOutputCapture<P> p) {
        beforeSyntax(document, p);
        document = (Xml.Document) super.visitDocument(document, p);
        afterSyntax(document, p);
        p.append(document.getEof());
        return document;
    }

    @Override
    public Xml visitProlog(Xml.Prolog prolog, PrintOutputCapture<P> p) {
        beforeSyntax(prolog, p);
        super.visitProlog(prolog, p);
        afterSyntax(prolog, p);
        return prolog;
    }

    @Override
    public Xml visitXmlDecl(Xml.XmlDecl xmlDecl, PrintOutputCapture<P> p) {
        beforeSyntax(xmlDecl, p);
        p.append("<?")
                .append(xmlDecl.getName());
        visit(xmlDecl.getAttributes(), p);
        p.append(xmlDecl.getBeforeTagDelimiterPrefix())
                .append("?>");
        afterSyntax(xmlDecl, p);
        return xmlDecl;
    }

    @Override
    public Xml visitTag(Xml.Tag tag, PrintOutputCapture<P> p) {
        beforeSyntax(tag, p);
        p.append('<');
        boolean isJspDirective = getCursor().getParentOrThrow().getValue() instanceof Xml.Prolog;
        if (isJspDirective) {
            p.append("%@");
        }
        p.append(tag.getName());
        visit(tag.getAttributes(), p);
        p.append(tag.getBeforeTagDelimiterPrefix());
        if (tag.getClosing() == null) {
            p.append("/>");
        } else {
            p.append('>');
            visit(tag.getContent(), p);
            p.append(tag.getClosing().getPrefix())
                    .append("</")
                    .append(tag.getClosing().getName())
                    .append(tag.getClosing().getBeforeTagDelimiterPrefix())
                    .append(">");

        }
        afterSyntax(tag, p);
        return tag;
    }

    @Override
    public Xml visitAttribute(Xml.Attribute attribute, PrintOutputCapture<P> p) {
        char valueDelim;
        if (Xml.Attribute.Value.Quote.Double.equals(attribute.getValue().getQuote())) {
            valueDelim = '"';
        } else {
            valueDelim = '\'';
        }
        beforeSyntax(attribute, p);
        p.append(attribute.getKey().getPrefix())
                .append(attribute.getKeyAsString())
                .append(attribute.getBeforeEquals())
                .append('=')
                .append(attribute.getValue().getPrefix())
                .append(valueDelim)
                .append(attribute.getValueAsString())
                .append(valueDelim);


        afterSyntax(attribute, p);
        return attribute;
    }

    @Override
    public Xml visitComment(Xml.Comment comment, PrintOutputCapture<P> p) {
        beforeSyntax(comment, p);
        p.append("<!--")
                .append(comment.getText())
                .append("-->");
        afterSyntax(comment, p);
        return comment;
    }

    @Override
    public Xml visitProcessingInstruction(Xml.ProcessingInstruction processingInstruction, PrintOutputCapture<P> p) {
        beforeSyntax(processingInstruction, p);
        p.append("<?")
                .append(processingInstruction.getName());
        visit(processingInstruction.getProcessingInstructions(), p);
        p.append(processingInstruction.getBeforeTagDelimiterPrefix())
                .append("?>");
        afterSyntax(processingInstruction, p);
        return processingInstruction;
    }

    @Override
    public Xml visitCharData(Xml.CharData charData, PrintOutputCapture<P> p) {
        beforeSyntax(charData, p);
        if (charData.isCdata()) {
            p.append("<![CDATA[")
                    .append(charData.getText())
                    .append("]]>");
        } else {
            p.append(charData.getText());
        }
        p.append(charData.getAfterText());
        afterSyntax(charData, p);
        return charData;
    }

    @Override
    public Xml visitDocTypeDecl(Xml.DocTypeDecl docTypeDecl, PrintOutputCapture<P> p) {
        beforeSyntax(docTypeDecl, p);
        p.append("<!DOCTYPE");
        visit(docTypeDecl.getName(), p);
        visit(docTypeDecl.getExternalId(), p);
        visit(docTypeDecl.getInternalSubset(), p);
        if (docTypeDecl.getExternalSubsets() != null) {
            p.append(docTypeDecl.getExternalSubsets().getPrefix())
                    .append('[');
            visit(docTypeDecl.getExternalSubsets().getElements(), p);
            p.append(']');
        }
        p.append(docTypeDecl.getBeforeTagDelimiterPrefix());
        p.append('>');
        afterSyntax(docTypeDecl, p);
        return docTypeDecl;
    }

    @Override
    public Xml visitElement(Xml.Element element, PrintOutputCapture<P> p) {
        beforeSyntax(element, p);
        visit(element.getSubset(), p);
        p.append(element.getBeforeTagDelimiterPrefix());
        afterSyntax(element, p);
        return element;
    }

    @Override
    public Xml visitIdent(Xml.Ident ident, PrintOutputCapture<P> p) {
        beforeSyntax(ident, p);
        p.append(ident.getName());
        afterSyntax(ident, p);
        return ident;
    }

    @Override
    public Xml visitJspDirective(Xml.JspDirective jsp, PrintOutputCapture<P> p) {
        beforeSyntax(jsp, p);
        p.append("<%@");
        p.append(jsp.getBeforeTypePrefix());
        p.append(jsp.getType());
        visit(jsp.getAttributes(), p);
        p.append(jsp.getBeforeDirectiveEndPrefix());
        p.append("%>");
        afterSyntax(jsp, p);
        return jsp;
    }

    private static final UnaryOperator<String> XML_MARKER_WRAPPER =
            out -> "<!--~~" + out + (out.isEmpty() ? "" : "~~") + ">-->";

    private void beforeSyntax(Xml x, PrintOutputCapture<P> p) {
        for (Marker marker : x.getMarkers().getMarkers()) {
            p.append(p.getMarkerPrinter().beforePrefix(marker, new Cursor(getCursor(), marker), XML_MARKER_WRAPPER));
        }
        p.append(x.getPrefix());
        visitMarkers(x.getMarkers(), p);
        for (Marker marker : x.getMarkers().getMarkers()) {
            p.append(p.getMarkerPrinter().beforeSyntax(marker, new Cursor(getCursor(), marker), XML_MARKER_WRAPPER));
        }
    }

    private void afterSyntax(Xml x, PrintOutputCapture<P> p) {
        for (Marker marker : x.getMarkers().getMarkers()) {
            p.append(p.getMarkerPrinter().afterSyntax(marker, new Cursor(getCursor(), marker), XML_MARKER_WRAPPER));
        }
    }
}
