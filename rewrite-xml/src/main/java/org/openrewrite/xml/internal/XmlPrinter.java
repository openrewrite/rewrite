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
import org.openrewrite.Tree;
import org.openrewrite.TreePrinter;
import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.xml.XmlVisitor;
import org.openrewrite.xml.tree.Xml;

import java.util.List;

public class XmlPrinter<P> extends XmlVisitor<P> {

    private static final String PRINTER_ACC_KEY = "printed";

    private final TreePrinter<P> treePrinter;

    public XmlPrinter(TreePrinter<P> treePrinter) {
        this.treePrinter = treePrinter;
        setCursoringOn();
    }

    @NonNull
    protected StringBuilder getPrinter() {
        StringBuilder acc = getCursor().getRoot().getMessage(PRINTER_ACC_KEY);
        if (acc == null) {
            acc = new StringBuilder();
            getCursor().getRoot().putMessage(PRINTER_ACC_KEY, acc);
        }
        return acc;
    }

    public String print(Xml xml, P p) {
        setCursor(new Cursor(null, "EPSILON"));
        visit(xml, p);
        return getPrinter().toString();
    }

    @Override
    @Nullable
    public Xml visit(@Nullable Tree tree, P p) {

        if (tree == null) {
            return defaultValue(null, p);
        }

        StringBuilder printerAcc = getPrinter();
        treePrinter.doBefore(tree, printerAcc, p);
        tree = super.visit(tree, p);
        if (tree != null) {
            treePrinter.doAfter(tree, printerAcc, p);
        }
        return (Xml) tree;
    }

    public void visit(@Nullable List<? extends Xml> nodes, P p) {
        if (nodes != null) {
            for (Xml node : nodes) {
                visit(node, p);
            }
        }
    }

    @Override
    public Xml visitDocument(Xml.Document document, P p) {
        StringBuilder acc = getPrinter();
        acc.append(document.getPrefix());
        document = (Xml.Document) super.visitDocument(document, p);
        acc.append(document.getEof());
        return document;
    }

    @Override
    public Xml visitProlog(Xml.Prolog prolog, P p) {
        getPrinter().append(prolog.getPrefix());
        return super.visitProlog(prolog, p);
    }

    @Override
    public Xml visitTag(Xml.Tag tag, P p) {
        StringBuilder acc = getPrinter();
        acc.append(tag.getPrefix())
                .append('<')
                .append(tag.getName());
        visit(tag.getAttributes(), p);
        acc.append(tag.getBeforeTagDelimiterPrefix());
        if (tag.getClosing() == null) {
            acc.append("/>");
        } else {
            acc.append('>');
            visit(tag.getContent(), p);
            acc.append(tag.getClosing().getPrefix())
                    .append("</")
                    .append(tag.getClosing().getName())
                    .append(tag.getClosing().getBeforeTagDelimiterPrefix())
                    .append(">");

        }
        return tag;
    }

    @Override
    public Xml visitAttribute(Xml.Attribute attribute, P p) {
        StringBuilder acc = getPrinter();
        char valueDelim;
        if (Xml.Attribute.Value.Quote.Double.equals(attribute.getValue().getQuote())) {
            valueDelim = '"';
        } else {
            valueDelim = '\'';
        }
        acc.append(attribute.getPrefix())
                .append(attribute.getKey().getPrefix())
                .append(attribute.getKeyAsString())
                .append('=')
                .append(attribute.getValue().getPrefix())
                .append(valueDelim)
                .append(attribute.getValueAsString())
                .append(valueDelim);


        return attribute;
    }

    @Override
    public Xml visitComment(Xml.Comment comment, P p) {
        StringBuilder acc = getPrinter();
        acc.append(comment.getPrefix())
                .append("<!--")
                .append(comment.getText())
                .append("-->");
        return comment;
    }

    @Override
    public Xml visitProcessingInstruction(Xml.ProcessingInstruction pi, P p) {
        StringBuilder acc = getPrinter();
        acc.append(pi.getPrefix())
                .append("<?")
                .append(pi.getName());
        visit(pi.getAttributes(), p);
        acc.append(pi.getBeforeTagDelimiterPrefix())
                .append("?>");
        return pi;
    }

    @Override
    public Xml visitCharData(Xml.CharData charData, P p) {
        StringBuilder acc = getPrinter();
        acc.append(charData.getPrefix());
        if (charData.isCdata()) {
            acc.append("<![CDATA[")
                    .append(charData.getText())
                    .append("]]>");
        } else {
            acc.append(charData.getText());
        }
        acc.append(charData.getAfterText());
        return charData;
    }

    @Override
    public Xml visitDocTypeDecl(Xml.DocTypeDecl docTypeDecl, P p) {
        StringBuilder acc = getPrinter();
        acc.append(docTypeDecl.getPrefix())
                .append("<!DOCTYPE");
        visit(docTypeDecl.getName(), p);
        visit(docTypeDecl.getExternalId(), p);
        visit(docTypeDecl.getInternalSubset(), p);
        if (docTypeDecl.getExternalSubsets() != null) {
            acc.append(docTypeDecl.getExternalSubsets().getPrefix())
                    .append('[');
            visit(docTypeDecl.getExternalSubsets().getElements(), p);
            acc.append(']');
        }
        acc.append('>');
        return docTypeDecl;
    }

    @Override
    public Xml visitElement(Xml.Element element, P p) {
        StringBuilder acc = getPrinter();
        acc.append(element.getPrefix())
                .append("<!ELEMENT");
        visit(element.getSubset(), p);
        acc.append('>');
        return element;
    }

    @Override
    public Xml visitIdent(Xml.Ident ident, P p) {
        getPrinter().append(ident.getPrefix())
                .append(ident.getName());
        return ident;
    }
}
