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

import org.openrewrite.Tree;
import org.openrewrite.TreePrinter;
import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.xml.XmlVisitor;
import org.openrewrite.xml.tree.Xml;

import java.util.List;

public class XmlPrinter<P> implements XmlVisitor<String, P> {

    private final TreePrinter<P> treePrinter;

    public XmlPrinter(TreePrinter<P> treePrinter) {
        this.treePrinter = treePrinter;
    }

    @NonNull
    @Override
    public String defaultValue(@Nullable Tree tree, P p) {
        return "";
    }

    @NonNull
    @Override
    public String visit(@Nullable Tree tree, P p) {
        if (tree == null) {
            return defaultValue(null, p);
        }

        Xml t = treePrinter.doFirst((Xml) tree, p);
        if (t == null) {
            return defaultValue(null, p);
        }

        //noinspection ConstantConditions
        return treePrinter.doLast(tree, t.accept(this, p), p);
    }

    public String visit(@Nullable List<? extends Xml> nodes, P p) {
        if (nodes == null) {
            return "";
        }

        StringBuilder acc = new StringBuilder();
        for (Xml node : nodes) {
            acc.append(visit(node, p));
        }
        return acc.toString();
    }

    @Override
    public String visitDocument(Xml.Document document, P p) {
        return fmt(document, visit(document.getProlog(), p) + visit(document.getRoot(), p) + document.getEof());
    }

    @Override
    public String visitProlog(Xml.Prolog prolog, P p) {
        return fmt(prolog, visit(prolog.getXmlDecls(), p) + visit(prolog.getMisc(), p));
    }

    @Override
    public String visitTag(Xml.Tag tag, P p) {
        return fmt(tag, "<" + tag.getName() + visit(tag.getAttributes(), p) +
                tag.getBeforeTagDelimiterPrefix() +
                (tag.getClosing() == null ?
                        "/>" :
                        ">" + visit(tag.getContent(), p) + fmt(tag.getClosing(), "</" + tag.getClosing().getName() + tag.getClosing().getBeforeTagDelimiterPrefix() + ">")
                )
        );
    }

    @Override
    public String visitAttribute(Xml.Attribute attribute, P p) {
        String valueDelim = Xml.Attribute.Value.Quote.Double.equals(attribute.getValue().getQuote()) ?
                "\"" : "'";

        return fmt(attribute, fmt(attribute.getKey(), attribute.getKeyAsString()) + "=" +
                fmt(attribute.getValue(), valueDelim + attribute.getValueAsString() + valueDelim));
    }

    @Override
    public String visitComment(Xml.Comment comment, P p) {
        return fmt(comment, "<!--" + comment.getText() + "-->");
    }

    @Override
    public String visitProcessingInstruction(Xml.ProcessingInstruction pi, P p) {
        return fmt(pi, "<?" + pi.getName() + visit(pi.getAttributes(), p) +
                pi.getBeforeTagDelimiterPrefix() + "?>");
    }

    @Override
    public String visitCharData(Xml.CharData charData, P p) {
        return fmt(charData, charData.isCdata() ?
                "<![CDATA[" + charData.getText() + "]]>" :
                charData.getText()
        ) + charData.getAfterText();
    }

    @Override
    public String visitDocTypeDecl(Xml.DocTypeDecl docTypeDecl, P p) {
        return fmt(docTypeDecl, "<!DOCTYPE" + visit(docTypeDecl.getName(), p) + visit(docTypeDecl.getExternalId(), p) +
                visit(docTypeDecl.getInternalSubset(), p) +
                (docTypeDecl.getExternalSubsets() == null ?
                        "" :
                        fmt(docTypeDecl.getExternalSubsets(), "[" + visit(docTypeDecl.getExternalSubsets().getElements(), p) + "]")) +
                ">");
    }

    @Override
    public String visitElement(Xml.DocTypeDecl.Element element, P p) {
        return fmt(element, "<!ELEMENT" + visit(element.getSubset(), p) + ">");
    }

    @Override
    public String visitIdent(Xml.Ident ident, P p) {
        return fmt(ident, ident.getName());
    }

    private String fmt(@Nullable Xml tree, @Nullable String code) {
        return tree == null || code == null ? "" : tree.getPrefix() + code;
    }
}
