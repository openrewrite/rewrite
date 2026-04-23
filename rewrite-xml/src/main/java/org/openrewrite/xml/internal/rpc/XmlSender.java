/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.xml.internal.rpc;

import org.openrewrite.Tree;
import org.openrewrite.rpc.RpcSendQueue;
import org.openrewrite.xml.XmlVisitor;
import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;

import java.util.List;

public class XmlSender extends XmlVisitor<RpcSendQueue> {

    @Override
    public Xml preVisit(Xml x, RpcSendQueue q) {
        q.getAndSend(x, Tree::getId);
        q.getAndSend(x, Xml::getPrefix);
        q.getAndSend(x, Tree::getMarkers);
        return x;
    }

    @Override
    public Xml visitDocument(Xml.Document document, RpcSendQueue q) {
        q.getAndSend(document, d -> d.getSourcePath().toString());
        q.getAndSend(document, d -> d.getCharset().name());
        q.getAndSend(document, Xml.Document::isCharsetBomMarked);
        q.getAndSend(document, Xml.Document::getChecksum);
        q.getAndSend(document, Xml.Document::getFileAttributes);
        q.getAndSend(document, Xml.Document::getProlog, p -> visit(p, q));
        q.getAndSend(document, Xml.Document::getRoot, t -> visit(t, q));
        q.getAndSend(document, Xml.Document::getEof);
        return document;
    }

    @Override
    public Xml visitProlog(Xml.Prolog prolog, RpcSendQueue q) {
        q.getAndSend(prolog, Xml.Prolog::getXmlDecl, d -> visit(d, q));
        q.getAndSendList(prolog, Xml.Prolog::getMisc, m -> m.getId(),
                m -> visit(m, q));
        q.getAndSendList(prolog, Xml.Prolog::getJspDirectives, j -> j.getId(),
                j -> visit(j, q));
        return prolog;
    }

    @Override
    public Xml visitXmlDecl(Xml.XmlDecl xmlDecl, RpcSendQueue q) {
        q.getAndSend(xmlDecl, Xml.XmlDecl::getName);
        q.getAndSendList(xmlDecl, Xml.XmlDecl::getAttributes, a -> a.getId(),
                a -> visit(a, q));
        q.getAndSend(xmlDecl, Xml.XmlDecl::getBeforeTagDelimiterPrefix);
        return xmlDecl;
    }

    @Override
    public Xml visitProcessingInstruction(Xml.ProcessingInstruction pi, RpcSendQueue q) {
        q.getAndSend(pi, Xml.ProcessingInstruction::getName);
        q.getAndSend(pi, Xml.ProcessingInstruction::getProcessingInstructions, c -> visit(c, q));
        q.getAndSend(pi, Xml.ProcessingInstruction::getBeforeTagDelimiterPrefix);
        return pi;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Xml visitTag(Xml.Tag tag, RpcSendQueue q) {
        q.getAndSend(tag, Xml.Tag::getName);
        q.getAndSendList(tag, Xml.Tag::getAttributes, a -> a.getId(),
                a -> visit(a, q));
        q.getAndSendList(tag, t -> (List<Content>) t.getContent(), c -> c.getId(),
                c -> visit(c, q));
        q.getAndSend(tag, Xml.Tag::getClosing, c -> visit(c, q));
        q.getAndSend(tag, Xml.Tag::getBeforeTagDelimiterPrefix);
        return tag;
    }

    @Override
    public Xml visitTagClosing(Xml.Tag.Closing closing, RpcSendQueue q) {
        q.getAndSend(closing, Xml.Tag.Closing::getName);
        q.getAndSend(closing, Xml.Tag.Closing::getBeforeTagDelimiterPrefix);
        return closing;
    }

    @Override
    public Xml visitAttribute(Xml.Attribute attribute, RpcSendQueue q) {
        q.getAndSend(attribute, Xml.Attribute::getKey, k -> visit(k, q));
        q.getAndSend(attribute, Xml.Attribute::getBeforeEquals);
        q.getAndSend(attribute, Xml.Attribute::getValue, v -> visit(v, q));
        return attribute;
    }

    @Override
    public Xml visitAttributeValue(Xml.Attribute.Value value, RpcSendQueue q) {
        q.getAndSend(value, v -> v.getQuote().name());
        q.getAndSend(value, Xml.Attribute.Value::getValue);
        return value;
    }

    @Override
    public Xml visitCharData(Xml.CharData charData, RpcSendQueue q) {
        q.getAndSend(charData, Xml.CharData::isCdata);
        q.getAndSend(charData, Xml.CharData::getText);
        q.getAndSend(charData, Xml.CharData::getAfterText);
        return charData;
    }

    @Override
    public Xml visitComment(Xml.Comment comment, RpcSendQueue q) {
        q.getAndSend(comment, Xml.Comment::getText);
        return comment;
    }

    @Override
    public Xml visitDocTypeDecl(Xml.DocTypeDecl docTypeDecl, RpcSendQueue q) {
        q.getAndSend(docTypeDecl, Xml.DocTypeDecl::getName, n -> visit(n, q));
        q.getAndSend(docTypeDecl, Xml.DocTypeDecl::getDocumentDeclaration);
        q.getAndSend(docTypeDecl, Xml.DocTypeDecl::getExternalId, e -> visit(e, q));
        q.getAndSendList(docTypeDecl, Xml.DocTypeDecl::getInternalSubset, i -> i.getId(),
                i -> visit(i, q));
        q.getAndSend(docTypeDecl, Xml.DocTypeDecl::getExternalSubsets, e -> visit(e, q));
        q.getAndSend(docTypeDecl, Xml.DocTypeDecl::getBeforeTagDelimiterPrefix);
        return docTypeDecl;
    }

    @Override
    public Xml visitDocTypeDeclExternalSubsets(Xml.DocTypeDecl.ExternalSubsets externalSubsets, RpcSendQueue q) {
        q.getAndSendList(externalSubsets, Xml.DocTypeDecl.ExternalSubsets::getElements, e -> e.getId(),
                e -> visit(e, q));
        return externalSubsets;
    }

    @Override
    public Xml visitElement(Xml.Element element, RpcSendQueue q) {
        q.getAndSendList(element, Xml.Element::getSubset, i -> i.getId(),
                i -> visit(i, q));
        q.getAndSend(element, Xml.Element::getBeforeTagDelimiterPrefix);
        return element;
    }

    @Override
    public Xml visitIdent(Xml.Ident ident, RpcSendQueue q) {
        q.getAndSend(ident, Xml.Ident::getName);
        return ident;
    }

    @Override
    public Xml visitJspDirective(Xml.JspDirective jspDirective, RpcSendQueue q) {
        q.getAndSend(jspDirective, Xml.JspDirective::getBeforeTypePrefix);
        q.getAndSend(jspDirective, Xml.JspDirective::getType);
        q.getAndSendList(jspDirective, Xml.JspDirective::getAttributes, a -> a.getId(),
                a -> visit(a, q));
        q.getAndSend(jspDirective, Xml.JspDirective::getBeforeDirectiveEndPrefix);
        return jspDirective;
    }

    @Override
    public Xml visitJspScriptlet(Xml.JspScriptlet jspScriptlet, RpcSendQueue q) {
        q.getAndSend(jspScriptlet, Xml.JspScriptlet::getContent);
        return jspScriptlet;
    }

    @Override
    public Xml visitJspExpression(Xml.JspExpression jspExpression, RpcSendQueue q) {
        q.getAndSend(jspExpression, Xml.JspExpression::getContent);
        return jspExpression;
    }

    @Override
    public Xml visitJspDeclaration(Xml.JspDeclaration jspDeclaration, RpcSendQueue q) {
        q.getAndSend(jspDeclaration, Xml.JspDeclaration::getContent);
        return jspDeclaration;
    }

    @Override
    public Xml visitJspComment(Xml.JspComment jspComment, RpcSendQueue q) {
        q.getAndSend(jspComment, Xml.JspComment::getContent);
        return jspComment;
    }
}
