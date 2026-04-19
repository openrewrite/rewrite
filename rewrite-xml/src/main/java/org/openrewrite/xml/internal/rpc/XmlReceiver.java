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

import org.openrewrite.marker.Markers;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.xml.XmlVisitor;
import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Misc;
import org.openrewrite.xml.tree.Xml;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

public class XmlReceiver extends XmlVisitor<RpcReceiveQueue> {

    @Override
    public Xml preVisit(Xml x, RpcReceiveQueue q) {
        x = x.withId(q.receiveAndGet(x.getId(), UUID::fromString));
        // Use withPrefixUnsafe to avoid equality check that NPEs on Objenesis-created objects
        String prefix = q.receive(x.getPrefix());
        x = x.withPrefixUnsafe(prefix != null ? prefix : "");
        Markers markers = q.receive(x.getMarkers());
        return x.withMarkers(markers != null ? markers : Markers.EMPTY);
    }

    @Override
    public Xml visitDocument(Xml.Document document, RpcReceiveQueue q) {
        Xml.Document d = document;
        d = d.withSourcePath(q.<Path, String>receiveAndGet(d.getSourcePath(), Paths::get));
        d = (Xml.Document) d.withCharset(q.<Charset, String>receiveAndGet(d.getCharset(), Charset::forName));
        d = d.withCharsetBomMarked(q.receive(d.isCharsetBomMarked()));
        d = d.withChecksum(q.receive(d.getChecksum()));
        d = d.withFileAttributes(q.receive(d.getFileAttributes()));
        d = d.withProlog(q.receive(d.getProlog(), p -> (Xml.Prolog) visitNonNull(p, q)));
        d = d.withRoot(q.receive(d.getRoot(), t -> (Xml.Tag) visitNonNull(t, q)));
        // Document.withEof() has a custom equality check that NPEs on Objenesis-created objects
        String eof = q.receive(d.getEof());
        if (d.getEof() == null) {
            d = new Xml.Document(d.getId(), d.getSourcePath(), d.getPrefix(), d.getMarkers(),
                    d.getCharsetName(), d.isCharsetBomMarked(), d.getChecksum(), d.getFileAttributes(),
                    d.getProlog(), d.getRoot(), eof != null ? eof : "");
        } else {
            d = d.withEof(eof != null ? eof : "");
        }
        return d;
    }

    @Override
    public Xml visitProlog(Xml.Prolog prolog, RpcReceiveQueue q) {
        return prolog
                .withXmlDecl(q.receive(prolog.getXmlDecl(), d -> (Xml.XmlDecl) visitNonNull(d, q)))
                .withMisc(q.receiveList(prolog.getMisc(), m -> (Misc) visitNonNull(m, q)))
                .withJspDirectives(q.receiveList(prolog.getJspDirectives(), j -> (Xml.JspDirective) visitNonNull(j, q)));
    }

    @Override
    public Xml visitXmlDecl(Xml.XmlDecl xmlDecl, RpcReceiveQueue q) {
        return xmlDecl
                .withName(q.receive(xmlDecl.getName()))
                .withAttributes(q.receiveList(xmlDecl.getAttributes(), a -> (Xml.Attribute) visitNonNull(a, q)))
                .withBeforeTagDelimiterPrefix(q.receive(xmlDecl.getBeforeTagDelimiterPrefix()));
    }

    @Override
    public Xml visitProcessingInstruction(Xml.ProcessingInstruction pi, RpcReceiveQueue q) {
        return pi
                .withName(q.receive(pi.getName()))
                .withProcessingInstructions(q.receive(pi.getProcessingInstructions(), c -> (Xml.CharData) visitNonNull(c, q)))
                .withBeforeTagDelimiterPrefix(q.receive(pi.getBeforeTagDelimiterPrefix()));
    }

    @SuppressWarnings("unchecked")
    @Override
    public Xml visitTag(Xml.Tag tag, RpcReceiveQueue q) {
        // Tag.withName() has a custom implementation that NPEs on Objenesis-created objects.
        // Build a new Tag if name is being set on an uninitialized object.
        String name = q.receive(tag.getName());
        if (name == null) name = tag.getName() != null ? tag.getName() : "";
        Xml.Tag t = tag.getName() == null ?
                new Xml.Tag(tag.getId(), tag.getPrefix(), tag.getMarkers(), name,
                        tag.getAttributes() != null ? tag.getAttributes() : java.util.Collections.emptyList(),
                        tag.getContent(), tag.getClosing(), tag.getBeforeTagDelimiterPrefix() != null ? tag.getBeforeTagDelimiterPrefix() : "") :
                tag.withName(name);
        return t
                .withAttributes(q.receiveList(t.getAttributes(), a -> (Xml.Attribute) visitNonNull(a, q)))
                .withContent(q.receiveList((List<Content>) t.getContent(), c -> (Content) visitNonNull(c, q)))
                .withClosing(q.receive(t.getClosing(), c -> (Xml.Tag.Closing) visitNonNull(c, q)))
                .withBeforeTagDelimiterPrefix(q.receive(t.getBeforeTagDelimiterPrefix()));
    }

    @Override
    public Xml visitTagClosing(Xml.Tag.Closing closing, RpcReceiveQueue q) {
        return closing
                .withName(q.receive(closing.getName()))
                .withBeforeTagDelimiterPrefix(q.receive(closing.getBeforeTagDelimiterPrefix()));
    }

    @Override
    public Xml visitAttribute(Xml.Attribute attribute, RpcReceiveQueue q) {
        return attribute
                .withKey(q.receive(attribute.getKey(), k -> (Xml.Ident) visitNonNull(k, q)))
                .withBeforeEquals(q.receive(attribute.getBeforeEquals()))
                .withValue(q.receive(attribute.getValue(), v -> (Xml.Attribute.Value) visitNonNull(v, q)));
    }

    @Override
    public Xml visitAttributeValue(Xml.Attribute.Value value, RpcReceiveQueue q) {
        return value
                .withQuote(q.<Xml.Attribute.Value.Quote, String>receiveAndGet(value.getQuote(), Xml.Attribute.Value.Quote::valueOf))
                .withValue(q.receive(value.getValue()));
    }

    @Override
    public Xml visitCharData(Xml.CharData charData, RpcReceiveQueue q) {
        return charData
                .withCdata(q.receive(charData.isCdata()))
                .withText(q.receive(charData.getText()))
                .withAfterText(q.receive(charData.getAfterText()));
    }

    @Override
    public Xml visitComment(Xml.Comment comment, RpcReceiveQueue q) {
        return comment.withText(q.receive(comment.getText()));
    }

    @Override
    public Xml visitDocTypeDecl(Xml.DocTypeDecl docTypeDecl, RpcReceiveQueue q) {
        return docTypeDecl
                .withName(q.receive(docTypeDecl.getName(), n -> (Xml.Ident) visitNonNull(n, q)))
                .withDocumentDeclaration(q.receive(docTypeDecl.getDocumentDeclaration()))
                .withExternalId(q.receive(docTypeDecl.getExternalId(), e -> (Xml.Ident) visitNonNull(e, q)))
                .withInternalSubset(q.receiveList(docTypeDecl.getInternalSubset(), i -> (Xml.Ident) visitNonNull(i, q)))
                .withExternalSubsets(q.receive(docTypeDecl.getExternalSubsets(), e -> (Xml.DocTypeDecl.ExternalSubsets) visitNonNull(e, q)))
                .withBeforeTagDelimiterPrefix(q.receive(docTypeDecl.getBeforeTagDelimiterPrefix()));
    }

    @Override
    public Xml visitDocTypeDeclExternalSubsets(Xml.DocTypeDecl.ExternalSubsets externalSubsets, RpcReceiveQueue q) {
        return externalSubsets
                .withElements(q.receiveList(externalSubsets.getElements(), e -> (Xml.Element) visitNonNull(e, q)));
    }

    @Override
    public Xml visitElement(Xml.Element element, RpcReceiveQueue q) {
        return element
                .withSubset(q.receiveList(element.getSubset(), i -> (Xml.Ident) visitNonNull(i, q)))
                .withBeforeTagDelimiterPrefix(q.receive(element.getBeforeTagDelimiterPrefix()));
    }

    @Override
    public Xml visitIdent(Xml.Ident ident, RpcReceiveQueue q) {
        return ident.withName(q.receive(ident.getName()));
    }

    @Override
    public Xml visitJspDirective(Xml.JspDirective jspDirective, RpcReceiveQueue q) {
        return jspDirective
                .withBeforeTypePrefix(q.receive(jspDirective.getBeforeTypePrefix()))
                .withType(q.receive(jspDirective.getType()))
                .withAttributes(q.receiveList(jspDirective.getAttributes(), a -> (Xml.Attribute) visitNonNull(a, q)))
                .withBeforeDirectiveEndPrefix(q.receive(jspDirective.getBeforeDirectiveEndPrefix()));
    }

    @Override
    public Xml visitJspScriptlet(Xml.JspScriptlet jspScriptlet, RpcReceiveQueue q) {
        return jspScriptlet.withContent(q.receive(jspScriptlet.getContent()));
    }

    @Override
    public Xml visitJspExpression(Xml.JspExpression jspExpression, RpcReceiveQueue q) {
        return jspExpression.withContent(q.receive(jspExpression.getContent()));
    }

    @Override
    public Xml visitJspDeclaration(Xml.JspDeclaration jspDeclaration, RpcReceiveQueue q) {
        return jspDeclaration.withContent(q.receive(jspDeclaration.getContent()));
    }

    @Override
    public Xml visitJspComment(Xml.JspComment jspComment, RpcReceiveQueue q) {
        return jspComment.withContent(q.receive(jspComment.getContent()));
    }
}
