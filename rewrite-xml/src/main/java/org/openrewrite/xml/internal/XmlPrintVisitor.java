package org.openrewrite.xml.internal;

import org.openrewrite.Tree;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.xml.XmlSourceVisitor;
import org.openrewrite.xml.tree.Xml;

public class XmlPrintVisitor extends XmlSourceVisitor<String> {
    @Override
    public String defaultTo(Tree t) {
        return "";
    }

    @Override
    public String reduce(String r1, String r2) {
        return r1 + r2;
    }

    @Override
    public String visitDocument(Xml.Document document) {
        return fmt(document, visit(document.getProlog()) + visit(document.getRoot()));
    }

    @Override
    public String visitProlog(Xml.Prolog prolog) {
        return fmt(prolog, visit(prolog.getXmlDecl()) + visit(prolog.getMisc()));
    }

    @Override
    public String visitTag(Xml.Tag tag) {
        return fmt(tag, "<" + tag.getName() + visit(tag.getAttributes()) +
                tag.getBeforeTagDelimiterPrefix() +
                (tag.getClosing() == null ?
                        "/>" :
                        ">" + visit(tag.getContent()) + fmt(tag.getClosing(), "</" + tag.getClosing().getName() + tag.getClosing().getBeforeTagDelimiterPrefix() + ">")
                )
        );
    }

    @Override
    public String visitAttribute(Xml.Attribute attribute) {
        String valueDelim = Xml.Attribute.Value.Quote.Double.equals(attribute.getValue().getQuote()) ?
                "\"" : "'";

        return fmt(attribute, fmt(attribute.getKey(), attribute.getKeyAsString()) + "=" +
                fmt(attribute.getValue(), valueDelim + attribute.getValueAsString() + valueDelim));
    }

    @Override
    public String visitComment(Xml.Comment comment) {
        return fmt(comment, "<!--" + comment.getText() + "-->");
    }

    @Override
    public String visitProcessingInstruction(Xml.ProcessingInstruction pi) {
        return fmt(pi, "<?" + pi.getName() + visit(pi.getAttributes()) +
                pi.getBeforeTagDelimiterPrefix() + "?>");
    }

    @Override
    public String visitCharData(Xml.CharData charData) {
        return fmt(charData, charData.isCdata() ?
                "<![CDATA[" + charData.getText() + "]]>" :
                charData.getText()
        );
    }

    @Override
    public String visitDocTypeDecl(Xml.DocTypeDecl docTypeDecl) {
        return fmt(docTypeDecl, "<!DOCTYPE" + visit(docTypeDecl.getName()) + visit(docTypeDecl.getExternalId()) +
                visit(docTypeDecl.getInternalSubset()) +
                (docTypeDecl.getExternalSubsets() == null ?
                        "" :
                        fmt(docTypeDecl.getExternalSubsets(), "[" + visit(docTypeDecl.getExternalSubsets().getElements()) + "]")) +
                ">");
    }

    @Override
    public String visitElement(Xml.DocTypeDecl.Element element) {
        return fmt(element, "<!ELEMENT" + visit(element.getSubset()) + ">");
    }

    @Override
    public String visitIdent(Xml.Ident ident) {
        return fmt(ident, ident.getName());
    }

    private String fmt(@Nullable Tree tree, @Nullable String code) {
        return tree == null || code == null ? "" : tree.getFormatting().getPrefix() + code + tree.getFormatting().getSuffix();
    }
}
