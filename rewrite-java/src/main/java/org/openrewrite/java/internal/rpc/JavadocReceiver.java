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

import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.JavadocVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Javadoc;
import org.openrewrite.rpc.RpcReceiveQueue;

import java.util.List;
import java.util.UUID;

/**
 * Rebuilds a {@link Javadoc} tree from the stream {@link JavadocSender} produces. Field order here
 * is the mirror of that sender and must stay in lockstep with it.
 */
class JavadocReceiver extends JavadocVisitor<RpcReceiveQueue> {

    JavadocReceiver(JavaVisitor<RpcReceiveQueue> javaVisitor) {
        super(javaVisitor);
    }

    @Override
    public Javadoc preVisit(Javadoc javadoc, RpcReceiveQueue q) {
        return javadoc
                .withId(q.receiveAndGet(javadoc.getId(), UUID::fromString))
                .withMarkers(q.receive(javadoc.getMarkers()));
    }

    @Override
    public Javadoc visitAttribute(Javadoc.Attribute attribute, RpcReceiveQueue q) {
        return attribute
                .withName(q.receive(attribute.getName()))
                .withSpaceBeforeEqual(receiveList(attribute.getSpaceBeforeEqual(), q))
                .withValue(receiveList(attribute.getValue(), q));
    }

    @Override
    public Javadoc visitAuthor(Javadoc.Author author, RpcReceiveQueue q) {
        return author.withName(receiveList(author.getName(), q));
    }

    @Override
    public Javadoc visitDeprecated(Javadoc.Deprecated deprecated, RpcReceiveQueue q) {
        return deprecated.withDescription(receiveList(deprecated.getDescription(), q));
    }

    @Override
    public Javadoc visitDocComment(Javadoc.DocComment docComment, RpcReceiveQueue q) {
        return docComment
                .withBody(receiveList(docComment.getBody(), q))
                .withSuffix(q.receive(docComment.getSuffix()));
    }

    @Override
    public Javadoc visitDocRoot(Javadoc.DocRoot docRoot, RpcReceiveQueue q) {
        return docRoot.withEndBrace(receiveList(docRoot.getEndBrace(), q));
    }

    @Override
    public Javadoc visitDocType(Javadoc.DocType docType, RpcReceiveQueue q) {
        return docType.withText(receiveList(docType.getText(), q));
    }

    @Override
    public Javadoc visitEndElement(Javadoc.EndElement endElement, RpcReceiveQueue q) {
        return endElement
                .withName(q.receive(endElement.getName()))
                .withSpaceBeforeEndBracket(receiveList(endElement.getSpaceBeforeEndBracket(), q));
    }

    @Override
    public Javadoc visitErroneous(Javadoc.Erroneous erroneous, RpcReceiveQueue q) {
        return erroneous.withText(receiveList(erroneous.getText(), q));
    }

    @Override
    public Javadoc visitHidden(Javadoc.Hidden hidden, RpcReceiveQueue q) {
        return hidden.withBody(receiveList(hidden.getBody(), q));
    }

    @Override
    public Javadoc visitIndex(Javadoc.Index index, RpcReceiveQueue q) {
        return index
                .withSearchTerm(receiveList(index.getSearchTerm(), q))
                .withDescription(receiveList(index.getDescription(), q))
                .withEndBrace(receiveList(index.getEndBrace(), q));
    }

    @Override
    public Javadoc visitInheritDoc(Javadoc.InheritDoc inheritDoc, RpcReceiveQueue q) {
        return inheritDoc.withEndBrace(receiveList(inheritDoc.getEndBrace(), q));
    }

    @Override
    public Javadoc visitInlinedValue(Javadoc.InlinedValue inlinedValue, RpcReceiveQueue q) {
        return inlinedValue
                .withSpaceBeforeTree(receiveList(inlinedValue.getSpaceBeforeTree(), q))
                .withTree(q.receive(inlinedValue.getTree(), t -> javaVisitorVisit(t, q)))
                .withEndBrace(receiveList(inlinedValue.getEndBrace(), q));
    }

    @Override
    public Javadoc visitLineBreak(Javadoc.LineBreak lineBreak, RpcReceiveQueue q) {
        return lineBreak.withMargin(q.receive(lineBreak.getMargin()));
    }

    @Override
    public Javadoc visitLink(Javadoc.Link link, RpcReceiveQueue q) {
        return link
                .withPlain(q.receive(link.isPlain()))
                .withSpaceBeforeTree(receiveList(link.getSpaceBeforeTree(), q))
                .withTree(q.receive(link.getTree(), t -> javaVisitorVisit(t, q)))
                .withTreeReference(q.receive(link.getTreeReference(), r -> (Javadoc.Reference) visit(r, q)))
                .withLabel(receiveList(link.getLabel(), q))
                .withEndBrace(receiveList(link.getEndBrace(), q));
    }

    @Override
    public Javadoc visitLiteral(Javadoc.Literal literal, RpcReceiveQueue q) {
        return literal
                .withCode(q.receive(literal.isCode()))
                .withDescription(receiveList(literal.getDescription(), q))
                .withEndBrace(receiveList(literal.getEndBrace(), q));
    }

    @Override
    public Javadoc visitParameter(Javadoc.Parameter parameter, RpcReceiveQueue q) {
        return parameter
                .withSpaceBeforeName(receiveList(parameter.getSpaceBeforeName(), q))
                .withName(q.receive(parameter.getName(), n -> javaVisitorVisit(n, q)))
                .withNameReference(q.receive(parameter.getNameReference(), r -> (Javadoc.Reference) visit(r, q)))
                .withDescription(receiveList(parameter.getDescription(), q));
    }

    @Override
    public Javadoc visitProvides(Javadoc.Provides provides, RpcReceiveQueue q) {
        return provides
                .withSpaceBeforeServiceType(receiveList(provides.getSpaceBeforeServiceType(), q))
                .withServiceType(q.receive(provides.getServiceType(), t -> javaVisitorVisit(t, q)))
                .withDescription(receiveList(provides.getDescription(), q));
    }

    @Override
    public Javadoc visitReference(Javadoc.Reference reference, RpcReceiveQueue q) {
        return reference
                .withTree(q.receive(reference.getTree(), t -> javaVisitorVisit(t, q)))
                .withLineBreaks(receiveList(reference.getLineBreaks(), q));
    }

    @Override
    public Javadoc visitReturn(Javadoc.Return aReturn, RpcReceiveQueue q) {
        return aReturn.withDescription(receiveList(aReturn.getDescription(), q));
    }

    @Override
    public Javadoc visitSee(Javadoc.See see, RpcReceiveQueue q) {
        return see
                .withSpaceBeforeTree(receiveList(see.getSpaceBeforeTree(), q))
                .withTree(q.receive(see.getTree(), t -> javaVisitorVisit(t, q)))
                .withTreeReference(q.receive(see.getTreeReference(), r -> (Javadoc.Reference) visit(r, q)))
                .withReference(receiveList(see.getReference(), q));
    }

    @Override
    public Javadoc visitSerial(Javadoc.Serial serial, RpcReceiveQueue q) {
        return serial.withDescription(receiveList(serial.getDescription(), q));
    }

    @Override
    public Javadoc visitSerialData(Javadoc.SerialData serialData, RpcReceiveQueue q) {
        return serialData.withDescription(receiveList(serialData.getDescription(), q));
    }

    @Override
    public Javadoc visitSerialField(Javadoc.SerialField serialField, RpcReceiveQueue q) {
        return serialField
                .withName(q.receive(serialField.getName(), n -> (J.Identifier) javaVisitorVisit(n, q)))
                .withType(q.receive(serialField.getType(), t -> javaVisitorVisit(t, q)))
                .withDescription(receiveList(serialField.getDescription(), q));
    }

    @Override
    public Javadoc visitSince(Javadoc.Since since, RpcReceiveQueue q) {
        return since.withDescription(receiveList(since.getDescription(), q));
    }

    @Override
    public Javadoc visitSnippet(Javadoc.Snippet snippet, RpcReceiveQueue q) {
        return snippet
                .withAttributes(receiveList(snippet.getAttributes(), q))
                .withContent(receiveList(snippet.getContent(), q))
                .withEndBrace(receiveList(snippet.getEndBrace(), q));
    }

    @Override
    public Javadoc visitStartElement(Javadoc.StartElement startElement, RpcReceiveQueue q) {
        return startElement
                .withName(q.receive(startElement.getName()))
                .withAttributes(receiveList(startElement.getAttributes(), q))
                .withSelfClosing(q.receive(startElement.isSelfClosing()))
                .withSpaceBeforeEndBracket(receiveList(startElement.getSpaceBeforeEndBracket(), q));
    }

    @Override
    public Javadoc visitSummary(Javadoc.Summary summary, RpcReceiveQueue q) {
        return summary
                .withSummary(receiveList(summary.getSummary(), q))
                .withBeforeBrace(receiveList(summary.getBeforeBrace(), q));
    }

    @Override
    public Javadoc visitText(Javadoc.Text text, RpcReceiveQueue q) {
        return text.withText(q.receive(text.getText()));
    }

    @Override
    public Javadoc visitThrows(Javadoc.Throws aThrows, RpcReceiveQueue q) {
        return aThrows
                .withThrowsKeyword(q.receive(aThrows.isThrowsKeyword()))
                .withSpaceBeforeExceptionName(receiveList(aThrows.getSpaceBeforeExceptionName(), q))
                .withExceptionName(q.receive(aThrows.getExceptionName(), n -> javaVisitorVisit(n, q)))
                .withDescription(receiveList(aThrows.getDescription(), q));
    }

    @Override
    public Javadoc visitUnknownBlock(Javadoc.UnknownBlock unknownBlock, RpcReceiveQueue q) {
        return unknownBlock
                .withName(q.receive(unknownBlock.getName()))
                .withContent(receiveList(unknownBlock.getContent(), q));
    }

    @Override
    public Javadoc visitUnknownInline(Javadoc.UnknownInline unknownInline, RpcReceiveQueue q) {
        return unknownInline
                .withName(q.receive(unknownInline.getName()))
                .withContent(receiveList(unknownInline.getContent(), q))
                .withEndBrace(receiveList(unknownInline.getEndBrace(), q));
    }

    @Override
    public Javadoc visitUses(Javadoc.Uses uses, RpcReceiveQueue q) {
        return uses
                .withBeforeServiceType(receiveList(uses.getBeforeServiceType(), q))
                .withServiceType(q.receive(uses.getServiceType(), t -> javaVisitorVisit(t, q)))
                .withDescription(receiveList(uses.getDescription(), q));
    }

    @Override
    public Javadoc visitVersion(Javadoc.Version version, RpcReceiveQueue q) {
        return version.withBody(receiveList(version.getBody(), q));
    }

    private List<Javadoc> receiveList(List<Javadoc> before, RpcReceiveQueue q) {
        return q.receiveList(before, d -> visit(d, q));
    }
}
