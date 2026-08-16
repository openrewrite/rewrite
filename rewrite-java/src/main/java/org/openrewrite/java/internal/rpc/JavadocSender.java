/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.java.internal.rpc;

import org.openrewrite.Tree;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.JavadocVisitor;
import org.openrewrite.java.tree.Javadoc;
import org.openrewrite.rpc.RpcSendQueue;

import java.util.List;
import java.util.function.Function;

/**
 * Decomposes a {@link Javadoc} tree over RPC, the same way {@link JavaSender} decomposes the rest
 * of the LST, so a doc comment survives the round trip as a tree instead of being flattened back
 * to text. Modelled on {@code CsDocCommentSender}.
 * <p>
 * The field lists below follow declaration order in {@link Javadoc}, <b>not</b> the traversal in
 * {@link JavadocVisitor}: that visitor skips every scalar (it never visits {@code Text#text} or
 * {@code LineBreak#margin}) and omits {@code markers} on roughly a third of the node types, so a
 * sender derived from it would silently drop fields. {@link JavadocReceiver} mirrors this order.
 */
class JavadocSender extends JavadocVisitor<RpcSendQueue> {

    JavadocSender(JavaVisitor<RpcSendQueue> javaVisitor) {
        super(javaVisitor);
    }

    @Override
    public Javadoc preVisit(Javadoc javadoc, RpcSendQueue q) {
        q.getAndSend(javadoc, Tree::getId);
        q.getAndSend(javadoc, Tree::getMarkers);
        return javadoc;
    }

    @Override
    public Javadoc visitAttribute(Javadoc.Attribute attribute, RpcSendQueue q) {
        q.getAndSend(attribute, Javadoc.Attribute::getName);
        sendList(attribute, Javadoc.Attribute::getSpaceBeforeEqual, q);
        sendList(attribute, Javadoc.Attribute::getValue, q);
        return attribute;
    }

    @Override
    public Javadoc visitAuthor(Javadoc.Author author, RpcSendQueue q) {
        sendList(author, Javadoc.Author::getName, q);
        return author;
    }

    @Override
    public Javadoc visitDeprecated(Javadoc.Deprecated deprecated, RpcSendQueue q) {
        sendList(deprecated, Javadoc.Deprecated::getDescription, q);
        return deprecated;
    }

    @Override
    public Javadoc visitDocComment(Javadoc.DocComment docComment, RpcSendQueue q) {
        sendList(docComment, Javadoc.DocComment::getBody, q);
        q.getAndSend(docComment, Javadoc.DocComment::getSuffix);
        return docComment;
    }

    @Override
    public Javadoc visitDocRoot(Javadoc.DocRoot docRoot, RpcSendQueue q) {
        sendList(docRoot, Javadoc.DocRoot::getEndBrace, q);
        return docRoot;
    }

    @Override
    public Javadoc visitDocType(Javadoc.DocType docType, RpcSendQueue q) {
        sendList(docType, Javadoc.DocType::getText, q);
        return docType;
    }

    @Override
    public Javadoc visitEndElement(Javadoc.EndElement endElement, RpcSendQueue q) {
        q.getAndSend(endElement, Javadoc.EndElement::getName);
        sendList(endElement, Javadoc.EndElement::getSpaceBeforeEndBracket, q);
        return endElement;
    }

    @Override
    public Javadoc visitErroneous(Javadoc.Erroneous erroneous, RpcSendQueue q) {
        sendList(erroneous, Javadoc.Erroneous::getText, q);
        return erroneous;
    }

    @Override
    public Javadoc visitHidden(Javadoc.Hidden hidden, RpcSendQueue q) {
        sendList(hidden, Javadoc.Hidden::getBody, q);
        return hidden;
    }

    @Override
    public Javadoc visitIndex(Javadoc.Index index, RpcSendQueue q) {
        sendList(index, Javadoc.Index::getSearchTerm, q);
        sendList(index, Javadoc.Index::getDescription, q);
        sendList(index, Javadoc.Index::getEndBrace, q);
        return index;
    }

    @Override
    public Javadoc visitInheritDoc(Javadoc.InheritDoc inheritDoc, RpcSendQueue q) {
        sendList(inheritDoc, Javadoc.InheritDoc::getEndBrace, q);
        return inheritDoc;
    }

    @Override
    public Javadoc visitInlinedValue(Javadoc.InlinedValue inlinedValue, RpcSendQueue q) {
        sendList(inlinedValue, Javadoc.InlinedValue::getSpaceBeforeTree, q);
        q.getAndSend(inlinedValue, Javadoc.InlinedValue::getTree, t -> javaVisitorVisit(t, q));
        sendList(inlinedValue, Javadoc.InlinedValue::getEndBrace, q);
        return inlinedValue;
    }

    @Override
    public Javadoc visitLineBreak(Javadoc.LineBreak lineBreak, RpcSendQueue q) {
        // `margin` is declared ahead of `markers`, but preVisit has already sent id and markers
        // for every node; the receiver reads them in the same hoisted order.
        q.getAndSend(lineBreak, Javadoc.LineBreak::getMargin);
        return lineBreak;
    }

    @Override
    public Javadoc visitLink(Javadoc.Link link, RpcSendQueue q) {
        q.getAndSend(link, Javadoc.Link::isPlain);
        sendList(link, Javadoc.Link::getSpaceBeforeTree, q);
        q.getAndSend(link, Javadoc.Link::getTree, t -> javaVisitorVisit(t, q));
        q.getAndSend(link, Javadoc.Link::getTreeReference, r -> visit(r, q));
        sendList(link, Javadoc.Link::getLabel, q);
        sendList(link, Javadoc.Link::getEndBrace, q);
        return link;
    }

    @Override
    public Javadoc visitLiteral(Javadoc.Literal literal, RpcSendQueue q) {
        q.getAndSend(literal, Javadoc.Literal::isCode);
        sendList(literal, Javadoc.Literal::getDescription, q);
        sendList(literal, Javadoc.Literal::getEndBrace, q);
        return literal;
    }

    @Override
    public Javadoc visitParameter(Javadoc.Parameter parameter, RpcSendQueue q) {
        sendList(parameter, Javadoc.Parameter::getSpaceBeforeName, q);
        q.getAndSend(parameter, Javadoc.Parameter::getName, n -> javaVisitorVisit(n, q));
        q.getAndSend(parameter, Javadoc.Parameter::getNameReference, r -> visit(r, q));
        sendList(parameter, Javadoc.Parameter::getDescription, q);
        return parameter;
    }

    @Override
    public Javadoc visitProvides(Javadoc.Provides provides, RpcSendQueue q) {
        sendList(provides, Javadoc.Provides::getSpaceBeforeServiceType, q);
        q.getAndSend(provides, Javadoc.Provides::getServiceType, t -> javaVisitorVisit(t, q));
        sendList(provides, Javadoc.Provides::getDescription, q);
        return provides;
    }

    @Override
    public Javadoc visitReference(Javadoc.Reference reference, RpcSendQueue q) {
        q.getAndSend(reference, Javadoc.Reference::getTree, t -> javaVisitorVisit(t, q));
        sendList(reference, Javadoc.Reference::getLineBreaks, q);
        return reference;
    }

    @Override
    public Javadoc visitReturn(Javadoc.Return aReturn, RpcSendQueue q) {
        sendList(aReturn, Javadoc.Return::getDescription, q);
        return aReturn;
    }

    @Override
    public Javadoc visitSee(Javadoc.See see, RpcSendQueue q) {
        sendList(see, Javadoc.See::getSpaceBeforeTree, q);
        q.getAndSend(see, Javadoc.See::getTree, t -> javaVisitorVisit(t, q));
        q.getAndSend(see, Javadoc.See::getTreeReference, r -> visit(r, q));
        sendList(see, Javadoc.See::getReference, q);
        return see;
    }

    @Override
    public Javadoc visitSerial(Javadoc.Serial serial, RpcSendQueue q) {
        sendList(serial, Javadoc.Serial::getDescription, q);
        return serial;
    }

    @Override
    public Javadoc visitSerialData(Javadoc.SerialData serialData, RpcSendQueue q) {
        sendList(serialData, Javadoc.SerialData::getDescription, q);
        return serialData;
    }

    @Override
    public Javadoc visitSerialField(Javadoc.SerialField serialField, RpcSendQueue q) {
        q.getAndSend(serialField, Javadoc.SerialField::getName, n -> javaVisitorVisit(n, q));
        q.getAndSend(serialField, Javadoc.SerialField::getType, t -> javaVisitorVisit(t, q));
        sendList(serialField, Javadoc.SerialField::getDescription, q);
        return serialField;
    }

    @Override
    public Javadoc visitSince(Javadoc.Since since, RpcSendQueue q) {
        sendList(since, Javadoc.Since::getDescription, q);
        return since;
    }

    @Override
    public Javadoc visitSnippet(Javadoc.Snippet snippet, RpcSendQueue q) {
        sendList(snippet, Javadoc.Snippet::getAttributes, q);
        sendList(snippet, Javadoc.Snippet::getContent, q);
        sendList(snippet, Javadoc.Snippet::getEndBrace, q);
        return snippet;
    }

    @Override
    public Javadoc visitStartElement(Javadoc.StartElement startElement, RpcSendQueue q) {
        q.getAndSend(startElement, Javadoc.StartElement::getName);
        sendList(startElement, Javadoc.StartElement::getAttributes, q);
        q.getAndSend(startElement, Javadoc.StartElement::isSelfClosing);
        sendList(startElement, Javadoc.StartElement::getSpaceBeforeEndBracket, q);
        return startElement;
    }

    @Override
    public Javadoc visitSummary(Javadoc.Summary summary, RpcSendQueue q) {
        sendList(summary, Javadoc.Summary::getSummary, q);
        sendList(summary, Javadoc.Summary::getBeforeBrace, q);
        return summary;
    }

    @Override
    public Javadoc visitText(Javadoc.Text text, RpcSendQueue q) {
        q.getAndSend(text, Javadoc.Text::getText);
        return text;
    }

    @Override
    public Javadoc visitThrows(Javadoc.Throws aThrows, RpcSendQueue q) {
        q.getAndSend(aThrows, Javadoc.Throws::isThrowsKeyword);
        sendList(aThrows, Javadoc.Throws::getSpaceBeforeExceptionName, q);
        q.getAndSend(aThrows, Javadoc.Throws::getExceptionName, n -> javaVisitorVisit(n, q));
        sendList(aThrows, Javadoc.Throws::getDescription, q);
        return aThrows;
    }

    @Override
    public Javadoc visitUnknownBlock(Javadoc.UnknownBlock unknownBlock, RpcSendQueue q) {
        q.getAndSend(unknownBlock, Javadoc.UnknownBlock::getName);
        sendList(unknownBlock, Javadoc.UnknownBlock::getContent, q);
        return unknownBlock;
    }

    @Override
    public Javadoc visitUnknownInline(Javadoc.UnknownInline unknownInline, RpcSendQueue q) {
        q.getAndSend(unknownInline, Javadoc.UnknownInline::getName);
        sendList(unknownInline, Javadoc.UnknownInline::getContent, q);
        sendList(unknownInline, Javadoc.UnknownInline::getEndBrace, q);
        return unknownInline;
    }

    @Override
    public Javadoc visitUses(Javadoc.Uses uses, RpcSendQueue q) {
        sendList(uses, Javadoc.Uses::getBeforeServiceType, q);
        q.getAndSend(uses, Javadoc.Uses::getServiceType, t -> javaVisitorVisit(t, q));
        sendList(uses, Javadoc.Uses::getDescription, q);
        return uses;
    }

    @Override
    public Javadoc visitVersion(Javadoc.Version version, RpcSendQueue q) {
        sendList(version, Javadoc.Version::getBody, q);
        return version;
    }

    private <T extends Javadoc> void sendList(T parent, Function<T, List<Javadoc>> values, RpcSendQueue q) {
        q.getAndSendList(parent, values, Tree::getId, d -> visit(d, q));
    }
}
