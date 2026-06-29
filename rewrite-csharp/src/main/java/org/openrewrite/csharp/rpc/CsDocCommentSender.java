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

import org.openrewrite.Tree;
import org.openrewrite.csharp.CSharpVisitor;
import org.openrewrite.csharp.CsDocCommentVisitor;
import org.openrewrite.csharp.tree.CsDocComment;
import org.openrewrite.rpc.RpcSendQueue;

/**
 * Decomposes a {@link CsDocComment} tree over RPC, mirroring the way {@link CSharpSender}
 * decomposes the rest of the C# LST. Each node sends its {@code id}, {@code markers}, and
 * remaining fields so that {@link CsDocCommentReceiver} can faithfully reconstruct the tree
 * on the other side rather than flattening it back to raw text.
 */
class CsDocCommentSender extends CsDocCommentVisitor<RpcSendQueue> {

    CsDocCommentSender(CSharpVisitor<RpcSendQueue> csharpVisitor) {
        super(csharpVisitor);
    }

    @Override
    public CsDocComment visitDocComment(CsDocComment.DocComment docComment, RpcSendQueue q) {
        q.getAndSend(docComment, Tree::getId);
        q.getAndSend(docComment, CsDocComment.DocComment::getMarkers);
        q.getAndSendList(docComment, CsDocComment.DocComment::getBody, Tree::getId, b -> visit(b, q));
        q.getAndSend(docComment, CsDocComment.DocComment::getSuffix);
        return docComment;
    }

    @Override
    public CsDocComment visitXmlElement(CsDocComment.XmlElement element, RpcSendQueue q) {
        q.getAndSend(element, Tree::getId);
        q.getAndSend(element, CsDocComment.XmlElement::getMarkers);
        q.getAndSend(element, CsDocComment.XmlElement::getName);
        q.getAndSendList(element, CsDocComment.XmlElement::getAttributes, Tree::getId, a -> visit(a, q));
        q.getAndSendList(element, CsDocComment.XmlElement::getSpaceBeforeClose, Tree::getId, s -> visit(s, q));
        q.getAndSendList(element, CsDocComment.XmlElement::getContent, Tree::getId, c -> visit(c, q));
        q.getAndSendList(element, CsDocComment.XmlElement::getClosingTagSpaceBeforeClose, Tree::getId, s -> visit(s, q));
        return element;
    }

    @Override
    public CsDocComment visitXmlEmptyElement(CsDocComment.XmlEmptyElement element, RpcSendQueue q) {
        q.getAndSend(element, Tree::getId);
        q.getAndSend(element, CsDocComment.XmlEmptyElement::getMarkers);
        q.getAndSend(element, CsDocComment.XmlEmptyElement::getName);
        q.getAndSendList(element, CsDocComment.XmlEmptyElement::getAttributes, Tree::getId, a -> visit(a, q));
        q.getAndSendList(element, CsDocComment.XmlEmptyElement::getSpaceBeforeSlashClose, Tree::getId, s -> visit(s, q));
        return element;
    }

    @Override
    public CsDocComment visitXmlText(CsDocComment.XmlText text, RpcSendQueue q) {
        q.getAndSend(text, Tree::getId);
        q.getAndSend(text, CsDocComment.XmlText::getMarkers);
        q.getAndSend(text, CsDocComment.XmlText::getText);
        return text;
    }

    @Override
    public CsDocComment visitXmlAttribute(CsDocComment.XmlAttribute attribute, RpcSendQueue q) {
        q.getAndSend(attribute, Tree::getId);
        q.getAndSend(attribute, CsDocComment.XmlAttribute::getMarkers);
        q.getAndSend(attribute, CsDocComment.XmlAttribute::getName);
        q.getAndSendList(attribute, CsDocComment.XmlAttribute::getSpaceBeforeEquals, Tree::getId, s -> visit(s, q));
        q.getAndSendList(attribute, CsDocComment.XmlAttribute::getValue, Tree::getId, v -> visit(v, q));
        return attribute;
    }

    @Override
    public CsDocComment visitXmlCrefAttribute(CsDocComment.XmlCrefAttribute attribute, RpcSendQueue q) {
        q.getAndSend(attribute, Tree::getId);
        q.getAndSend(attribute, CsDocComment.XmlCrefAttribute::getMarkers);
        q.getAndSendList(attribute, CsDocComment.XmlCrefAttribute::getSpaceBeforeEquals, Tree::getId, s -> visit(s, q));
        q.getAndSendList(attribute, CsDocComment.XmlCrefAttribute::getValue, Tree::getId, v -> visit(v, q));
        q.getAndSend(attribute, CsDocComment.XmlCrefAttribute::getReference, ref -> csharpVisitorVisit(ref, q));
        return attribute;
    }

    @Override
    public CsDocComment visitXmlNameAttribute(CsDocComment.XmlNameAttribute attribute, RpcSendQueue q) {
        q.getAndSend(attribute, Tree::getId);
        q.getAndSend(attribute, CsDocComment.XmlNameAttribute::getMarkers);
        q.getAndSendList(attribute, CsDocComment.XmlNameAttribute::getSpaceBeforeEquals, Tree::getId, s -> visit(s, q));
        q.getAndSendList(attribute, CsDocComment.XmlNameAttribute::getValue, Tree::getId, v -> visit(v, q));
        q.getAndSend(attribute, CsDocComment.XmlNameAttribute::getParamName, name -> csharpVisitorVisit(name, q));
        return attribute;
    }

    @Override
    public CsDocComment visitLineBreak(CsDocComment.LineBreak lineBreak, RpcSendQueue q) {
        q.getAndSend(lineBreak, Tree::getId);
        q.getAndSend(lineBreak, CsDocComment.LineBreak::getMargin);
        q.getAndSend(lineBreak, CsDocComment.LineBreak::getMarkers);
        return lineBreak;
    }
}
