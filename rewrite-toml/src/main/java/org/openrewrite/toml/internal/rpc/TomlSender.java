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
package org.openrewrite.toml.internal.rpc;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Tree;
import org.openrewrite.toml.TomlVisitor;
import org.openrewrite.toml.tree.Comment;
import org.openrewrite.toml.tree.Space;
import org.openrewrite.toml.tree.Toml;
import org.openrewrite.toml.tree.TomlRightPadded;
import org.openrewrite.rpc.Reference;
import org.openrewrite.rpc.RpcSendQueue;

import static org.openrewrite.rpc.Reference.asRef;

public class TomlSender extends TomlVisitor<RpcSendQueue> {

    @Override
    public Toml preVisit(Toml j, RpcSendQueue q) {
        q.getAndSend(j, Tree::getId);
        q.getAndSend(j, j2 -> asRef(j2.getPrefix()), space ->
                visitSpace(Reference.getValueNonNull(space), q));
        q.getAndSend(j, Tree::getMarkers);
        return j;
    }

    @Override
    public Toml visitDocument(Toml.Document document, RpcSendQueue q) {
        q.getAndSend(document, d -> d.getSourcePath().toString());
        q.getAndSend(document, d -> d.getCharset().name());
        q.getAndSend(document, Toml.Document::isCharsetBomMarked);
        q.getAndSend(document, Toml.Document::getChecksum);
        q.getAndSend(document, Toml.Document::getFileAttributes);
        q.getAndSendList(document, Toml.Document::getValues,
                v -> v.getId(),
                v -> visit(v, q));
        q.getAndSend(document, d -> asRef(d.getEof()), space ->
                visitSpace(Reference.getValueNonNull(space), q));
        return document;
    }

    @Override
    public Toml visitArray(Toml.Array array, RpcSendQueue q) {
        q.getAndSendList(array, a -> a.getPadding().getValues(),
                j -> j.getElement().getId(),
                j -> visitRightPadded(j, q));
        return array;
    }

    @Override
    public Toml visitTable(Toml.Table table, RpcSendQueue q) {
        q.getAndSend(table, t -> t.getPadding().getName(), rp -> visitRightPadded(rp, q));
        q.getAndSendList(table, t -> t.getPadding().getValues(),
                j -> j.getElement().getId(),
                j -> visitRightPadded(j, q));
        return table;
    }

    @Override
    public Toml visitKeyValue(Toml.KeyValue keyValue, RpcSendQueue q) {
        q.getAndSend(keyValue, kv -> kv.getPadding().getKey(), rp -> visitRightPadded(rp, q));
        q.getAndSend(keyValue, Toml.KeyValue::getValue, j -> visit(j, q));
        return keyValue;
    }

    @Override
    public Toml visitLiteral(Toml.Literal literal, RpcSendQueue q) {
        q.getAndSend(literal, Toml.Literal::getType);
        q.getAndSend(literal, Toml.Literal::getSource);
        q.getAndSend(literal, Toml.Literal::getValue);
        return literal;
    }

    @Override
    public Toml visitIdentifier(Toml.Identifier identifier, RpcSendQueue q) {
        q.getAndSend(identifier, Toml.Identifier::getSource);
        q.getAndSend(identifier, Toml.Identifier::getName);
        return identifier;
    }

    @Override
    public Toml visitEmpty(Toml.Empty empty, RpcSendQueue q) {
        return empty;
    }

    @Override
    public Space visitSpace(Space space, RpcSendQueue q) {
        q.getAndSendList(space, Space::getComments, c -> c.getText() + c.getSuffix(), c -> {
            q.getAndSend(c, Comment::getText);
            q.getAndSend(c, Comment::getSuffix);
            q.getAndSend(c, Comment::getMarkers);
        });
        q.getAndSend(space, Space::getWhitespace);
        return space;
    }

    @Override
    public @Nullable <T> TomlRightPadded<T> visitRightPadded(@Nullable TomlRightPadded<T> right, RpcSendQueue q) {
        assert right != null;
        q.getAndSend(right, TomlRightPadded::getElement, j -> visit((Toml) j, q));
        q.getAndSend(right, j -> asRef(j.getAfter()),
                space -> visitSpace(Reference.getValueNonNull(space), q));
        q.getAndSend(right, TomlRightPadded::getMarkers);
        return right;
    }
}
