/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.csharp.rpc;

import org.openrewrite.csharp.CSharpVisitor;
import org.openrewrite.csharp.CsDocCommentVisitor;
import org.openrewrite.csharp.tree.CsDocComment;
import org.openrewrite.java.tree.J;
import org.openrewrite.rpc.RpcReceiveQueue;

import java.util.UUID;

/**
 * Reconstructs a {@link CsDocComment} tree decomposed by {@link CsDocCommentSender}. The
 * field order here must mirror the sender exactly so the queue stays in sync.
 */
class CsDocCommentReceiver extends CsDocCommentVisitor<RpcReceiveQueue> {

    CsDocCommentReceiver(CSharpVisitor<RpcReceiveQueue> csharpVisitor) {
        super(csharpVisitor);
    }

    @Override
    public CsDocComment visitDocComment(CsDocComment.DocComment docComment, RpcReceiveQueue q) {
        return docComment
                .withId(q.receiveAndGet(docComment.getId(), UUID::fromString))
                .withMarkers(q.receive(docComment.getMarkers()))
                .withBody(q.receiveList(docComment.getBody(), b -> visit(b, q)))
                .withSuffix(q.receive(docComment.getSuffix()));
    }

    @Override
    public CsDocComment visitXmlElement(CsDocComment.XmlElement element, RpcReceiveQueue q) {
        return element
                .withId(q.receiveAndGet(element.getId(), UUID::fromString))
                .withMarkers(q.receive(element.getMarkers()))
                .withName(q.receive(element.getName()))
                .withAttributes(q.receiveList(element.getAttributes(), a -> visit(a, q)))
                .withSpaceBeforeClose(q.receiveList(element.getSpaceBeforeClose(), s -> visit(s, q)))
                .withContent(q.receiveList(element.getContent(), c -> visit(c, q)))
                .withClosingTagSpaceBeforeClose(q.receiveList(element.getClosingTagSpaceBeforeClose(), s -> visit(s, q)));
    }

    @Override
    public CsDocComment visitXmlEmptyElement(CsDocComment.XmlEmptyElement element, RpcReceiveQueue q) {
        return element
                .withId(q.receiveAndGet(element.getId(), UUID::fromString))
                .withMarkers(q.receive(element.getMarkers()))
                .withName(q.receive(element.getName()))
                .withAttributes(q.receiveList(element.getAttributes(), a -> visit(a, q)))
                .withSpaceBeforeSlashClose(q.receiveList(element.getSpaceBeforeSlashClose(), s -> visit(s, q)));
    }

    @Override
    public CsDocComment visitXmlText(CsDocComment.XmlText text, RpcReceiveQueue q) {
        return text
                .withId(q.receiveAndGet(text.getId(), UUID::fromString))
                .withMarkers(q.receive(text.getMarkers()))
                .withText(q.receive(text.getText()));
    }

    @Override
    public CsDocComment visitXmlAttribute(CsDocComment.XmlAttribute attribute, RpcReceiveQueue q) {
        return attribute
                .withId(q.receiveAndGet(attribute.getId(), UUID::fromString))
                .withMarkers(q.receive(attribute.getMarkers()))
                .withName(q.receive(attribute.getName()))
                .withSpaceBeforeEquals(q.receiveList(attribute.getSpaceBeforeEquals(), s -> visit(s, q)))
                .withValue(q.receiveList(attribute.getValue(), v -> visit(v, q)));
    }

    @Override
    public CsDocComment visitXmlCrefAttribute(CsDocComment.XmlCrefAttribute attribute, RpcReceiveQueue q) {
        return attribute
                .withId(q.receiveAndGet(attribute.getId(), UUID::fromString))
                .withMarkers(q.receive(attribute.getMarkers()))
                .withSpaceBeforeEquals(q.receiveList(attribute.getSpaceBeforeEquals(), s -> visit(s, q)))
                .withValue(q.receiveList(attribute.getValue(), v -> visit(v, q)))
                .withReference((J) q.receive(attribute.getReference(), ref -> csharpVisitorVisit(ref, q)));
    }

    @Override
    public CsDocComment visitXmlNameAttribute(CsDocComment.XmlNameAttribute attribute, RpcReceiveQueue q) {
        return attribute
                .withId(q.receiveAndGet(attribute.getId(), UUID::fromString))
                .withMarkers(q.receive(attribute.getMarkers()))
                .withSpaceBeforeEquals(q.receiveList(attribute.getSpaceBeforeEquals(), s -> visit(s, q)))
                .withValue(q.receiveList(attribute.getValue(), v -> visit(v, q)))
                .withParamName((J) q.receive(attribute.getParamName(), name -> csharpVisitorVisit(name, q)));
    }

    @Override
    public CsDocComment visitLineBreak(CsDocComment.LineBreak lineBreak, RpcReceiveQueue q) {
        return lineBreak
                .withId(q.receiveAndGet(lineBreak.getId(), UUID::fromString))
                .withMargin(q.receive(lineBreak.getMargin()))
                .withMarkers(q.receive(lineBreak.getMarkers()));
    }
}
