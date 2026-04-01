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
package org.openrewrite.json.internal.rpc;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Tree;
import org.openrewrite.json.JsonVisitor;
import org.openrewrite.json.tree.Comment;
import org.openrewrite.json.tree.Json;
import org.openrewrite.json.tree.JsonRightPadded;
import org.openrewrite.json.tree.Space;
import org.openrewrite.rpc.Reference;
import org.openrewrite.rpc.RpcSendQueue;

import static org.openrewrite.rpc.Reference.asRef;

public class JsonSender extends JsonVisitor<RpcSendQueue> {

    @Override
    public Json preVisit(Json j, RpcSendQueue q) {
        q.getAndSend(j, Tree::getId);
        q.getAndSend(j, j2 -> asRef(j2.getPrefix()), space ->
                visitSpace(Reference.getValueNonNull(space), q));
        q.getAndSend(j, Tree::getMarkers);
        return j;
    }

    @Override
    public Json visitDocument(Json.Document document, RpcSendQueue q) {
        q.getAndSend(document, d -> d.getSourcePath().toString());
        q.getAndSend(document, d -> d.getCharset().name());
        q.getAndSend(document, Json.Document::isCharsetBomMarked);
        q.getAndSend(document, Json.Document::getChecksum);
        q.getAndSend(document, Json.Document::getFileAttributes);
        q.getAndSend(document, Json.Document::getValue, j -> visit(j, q));
        q.getAndSend(document, d -> asRef(d.getEof()), space ->
                visitSpace(Reference.getValueNonNull(space), q));
        return document;
    }

    @Override
    public Json visitArray(Json.Array array, RpcSendQueue q) {
        q.getAndSendList(array, a -> a.getPadding().getValues(),
                j -> j.getElement().getId(),
                j -> visitRightPadded(j, q));
        return array;
    }

    @Override
    public Json visitEmpty(Json.Empty empty, RpcSendQueue q) {
        return empty;
    }

    @Override
    public Json visitIdentifier(Json.Identifier identifier, RpcSendQueue q) {
        q.getAndSend(identifier, Json.Identifier::getName);
        return identifier;
    }

    @Override
    public Json visitLiteral(Json.Literal literal, RpcSendQueue q) {
        q.getAndSend(literal, Json.Literal::getSource);
        q.getAndSend(literal, Json.Literal::getValue);
        return literal;
    }

    @Override
    public Json visitMember(Json.Member member, RpcSendQueue q) {
        q.getAndSend(member, m -> m.getPadding().getKey(), j -> visitRightPadded(j, q));
        q.getAndSend(member, Json.Member::getValue, j -> visit(j, q));
        return member;
    }

    @Override
    public Json visitObject(Json.JsonObject obj, RpcSendQueue q) {
        q.getAndSendList(obj, o -> o.getPadding().getMembers(),
                j -> j.getElement().getId(),
                j -> visitRightPadded(j, q));
        return obj;
    }

    @Override
    public Space visitSpace(Space space, RpcSendQueue q) {
        q.getAndSendList(space, Space::getComments, c -> c.getText() + c.getSuffix(), c -> {
            q.getAndSend(c, Comment::isMultiline);
            q.getAndSend(c, Comment::getText);
            q.getAndSend(c, Comment::getSuffix);
            q.getAndSend(c, Comment::getMarkers);
        });
        q.getAndSend(space, Space::getWhitespace);
        return space;
    }

    @Override
    public @Nullable <T extends Json> JsonRightPadded<T> visitRightPadded(@Nullable JsonRightPadded<T> right, RpcSendQueue q) {
        assert right != null;
        q.getAndSend(right, JsonRightPadded::getElement, j -> visit(j, q));
        q.getAndSend(right, j -> asRef(j.getAfter()),
                space -> visitSpace(Reference.getValueNonNull(space), q));
        q.getAndSend(right, JsonRightPadded::getMarkers);
        return right;
    }
}
