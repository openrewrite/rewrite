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

import org.openrewrite.PrintOutputCapture;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.xml.XmlVisitor;
import org.openrewrite.xml.tree.Xml;

public class XmlPrinter<P> extends XmlVisitor<PrintOutputCapture<P>> {

    @Override
    public Xml visitDocument(Xml.Document document, PrintOutputCapture<P> p) {
        p.out.append(document.getPrefix());
        document = (Xml.Document) super.visitDocument(document, p);
        p.out.append(document.getEof());
        return document;
    }

    @Override
    public Xml visitProlog(Xml.Prolog prolog, PrintOutputCapture<P> p) {
        p.out.append(prolog.getPrefix());
        return super.visitProlog(prolog, p);
    }

    @Override
    public Xml visitXmlDecl(Xml.XmlDecl xmlDecl, PrintOutputCapture<P> p) {
        visitMarkers(xmlDecl.getMarkers(), p);
        p.out.append("<?")
                .append(xmlDecl.getName());
        visit(xmlDecl.getAttributes(), p);
        p.out.append(xmlDecl.getBeforeTagDelimiterPrefix())
                .append("?>");
        return xmlDecl;
    }

    @Override
    public Xml visitTag(Xml.Tag tag, PrintOutputCapture<P> p) {
        p.out.append(tag.getPrefix());
        visitMarkers(tag.getMarkers(), p);
        p.out.append('<')
                .append(tag.getName());
        visit(tag.getAttributes(), p);
        p.out.append(tag.getBeforeTagDelimiterPrefix());
        if (tag.getClosing() == null) {
            p.out.append("/>");
        } else {
            p.out.append('>');
            visit(tag.getContent(), p);
            p.out.append(tag.getClosing().getPrefix())
                    .append("</")
                    .append(tag.getClosing().getName())
                    .append(tag.getClosing().getBeforeTagDelimiterPrefix())
                    .append(">");

        }
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
        p.out.append(attribute.getPrefix());
        visitMarkers(attribute.getMarkers(), p);
        p.out.append(attribute.getKey().getPrefix())
                .append(attribute.getKeyAsString())
                .append(attribute.getBeforeEquals())
                .append('=')
                .append(attribute.getValue().getPrefix())
                .append(valueDelim)
                .append(attribute.getValueAsString())
                .append(valueDelim);


        return attribute;
    }

    @Override
    public Xml visitComment(Xml.Comment comment, PrintOutputCapture<P> p) {
        p.out.append(comment.getPrefix());
        visitMarkers(comment.getMarkers(), p);
        p.out.append("<!--")
                .append(comment.getText())
                .append("-->");
        return comment;
    }

    @Override
    public Xml visitProcessingInstruction(Xml.ProcessingInstruction processingInstruction, PrintOutputCapture<P> p) {
        p.out.append(processingInstruction.getPrefix());
        visitMarkers(processingInstruction.getMarkers(), p);
        p.out.append("<?")
                .append(processingInstruction.getName());
        visit(processingInstruction.getProcessingInstructions(), p);
        p.out.append(processingInstruction.getBeforeTagDelimiterPrefix())
                .append("?>");
        return processingInstruction;
    }

    @Override
    public Xml visitCharData(Xml.CharData charData, PrintOutputCapture<P> p) {
        p.out.append(charData.getPrefix());
        visitMarkers(charData.getMarkers(), p);
        if (charData.isCdata()) {
            p.out.append("<![CDATA[")
                    .append(charData.getText())
                    .append("]]>");
        } else {
            p.out.append(charData.getText());
        }
        p.out.append(charData.getAfterText());
        return charData;
    }

    @Override
    public Xml visitDocTypeDecl(Xml.DocTypeDecl docTypeDecl, PrintOutputCapture<P> p) {
        p.out.append(docTypeDecl.getPrefix());
        visitMarkers(docTypeDecl.getMarkers(), p);
        p.out.append("<!DOCTYPE");
        visit(docTypeDecl.getName(), p);
        visit(docTypeDecl.getExternalId(), p);
        visit(docTypeDecl.getInternalSubset(), p);
        if (docTypeDecl.getExternalSubsets() != null) {
            p.out.append(docTypeDecl.getExternalSubsets().getPrefix())
                    .append('[');
            visit(docTypeDecl.getExternalSubsets().getElements(), p);
            p.out.append(']');
        }
        p.out.append(docTypeDecl.getBeforeTagDelimiterPrefix());
        p.out.append('>');
        return docTypeDecl;
    }

    @Override
    public Xml visitElement(Xml.Element element, PrintOutputCapture<P> p) {
        p.out.append(element.getPrefix());
        visitMarkers(element.getMarkers(), p);
        visit(element.getSubset(), p);
        p.out.append(element.getBeforeTagDelimiterPrefix());
        return element;
    }

    @Override
    public Xml visitIdent(Xml.Ident ident, PrintOutputCapture<P> p) {
        p.out.append(ident.getPrefix());
        visitMarkers(ident.getMarkers(), p);
        p.out.append(ident.getName());
        return ident;
    }

    @Override
    public <M extends Marker> M visitMarker(Marker marker, PrintOutputCapture<P> p) {
        if(marker instanceof SearchResult) {
            String description = ((SearchResult) marker).getDescription();
            p.out.append("<!--~~")
                    .append(description == null ? "" : "(" + description + ")~~")
                    .append(">-->");
        }
        //noinspection unchecked
        return (M) marker;
    }
}
