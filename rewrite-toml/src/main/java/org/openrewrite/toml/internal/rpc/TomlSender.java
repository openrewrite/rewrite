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
import org.openrewrite.rpc.RpcSendQueue;
import org.openrewrite.toml.TomlVisitor;
import org.openrewrite.toml.tree.*;

import java.time.temporal.TemporalAccessor;

import static org.openrewrite.rpc.Reference.asRef;

public class TomlSender extends TomlVisitor<RpcSendQueue> {

    @Override
    public Toml preVisit(Toml t, RpcSendQueue q) {
        q.getAndSend(t, Tree::getId);
        q.getAndSend(t, Toml::getPrefix, space -> visitSpace(space, q));
        q.getAndSend(t, t2 -> asRef(t2.getMarkers()));
        return t;
    }

    @Override
    public Toml visitDocument(Toml.Document document, RpcSendQueue q) {
        q.getAndSend(document, d -> d.getSourcePath().toString());
        q.getAndSend(document, Toml.Document::getCharsetName);
        q.getAndSend(document, Toml.Document::isCharsetBomMarked);
        q.getAndSend(document, Toml.Document::getChecksum);
        q.getAndSend(document, Toml.Document::getFileAttributes);
        q.getAndSendList(document, Toml.Document::getValues, Tree::getId, v -> visit(v, q));
        q.getAndSend(document, Toml.Document::getEof, space -> visitSpace(space, q));
        return document;
    }

    @Override
    public Toml visitArray(Toml.Array array, RpcSendQueue q) {
        q.getAndSendList(array, a -> a.getPadding().getValues(),
                rp -> rp.getElement().getId(),
                rp -> visitRightPadded(rp, q));
        return array;
    }

    @Override
    public Toml visitEmpty(Toml.Empty empty, RpcSendQueue q) {
        return empty;
    }

    @Override
    public Toml visitIdentifier(Toml.Identifier identifier, RpcSendQueue q) {
        q.getAndSend(identifier, Toml.Identifier::getSource);
        q.getAndSend(identifier, Toml.Identifier::getName);
        return identifier;
    }

    @Override
    public Toml visitKeyValue(Toml.KeyValue keyValue, RpcSendQueue q) {
        q.getAndSend(keyValue, kv -> kv.getPadding().getKey(), rp -> visitRightPadded(rp, q));
        q.getAndSend(keyValue, Toml.KeyValue::getValue, v -> visit(v, q));
        return keyValue;
    }

    @Override
    public Toml visitLiteral(Toml.Literal literal, RpcSendQueue q) {
        q.getAndSend(literal, l -> l.getType().name());
        q.getAndSend(literal, Toml.Literal::getSource);
        q.getAndSend(literal, l -> encodeValue(l.getValue()));
        return literal;
    }

    @Override
    public Toml visitTable(Toml.Table table, RpcSendQueue q) {
        q.getAndSend(table, t -> t.getPadding().getName(), rp -> visitRightPadded(rp, q));
        q.getAndSendList(table, t -> t.getPadding().getValues(),
                rp -> rp.getElement().getId(),
                rp -> visitRightPadded(rp, q));
        return table;
    }

    @Override
    public Space visitSpace(Space space, RpcSendQueue q) {
        q.getAndSendList(space, Space::getComments, c -> c.getText() + c.getSuffix(), c -> {
            q.getAndSend(c, Comment::getText);
            q.getAndSend(c, Comment::getSuffix);
            q.getAndSend(c, c2 -> asRef(c2.getMarkers()));
        });
        q.getAndSend(space, Space::getWhitespace);
        return space;
    }

    @Override
    public <T> @Nullable TomlRightPadded<T> visitRightPadded(@Nullable TomlRightPadded<T> right, RpcSendQueue q) {
        assert right != null;
        q.getAndSend(right, TomlRightPadded::getElement, t -> visit((Toml) t, q));
        q.getAndSend(right, TomlRightPadded::getAfter, space -> visitSpace(space, q));
        q.getAndSend(right, rp -> asRef(rp.getMarkers()));
        return right;
    }

    /**
     * JSON has no literal for a non-finite float or a date/time, so those travel as their
     * TOML spelling and ISO-8601 form respectively, to be decoded against the literal's type.
     */
    static Object encodeValue(Object value) {
        if (value instanceof Double) {
            double d = (Double) value;
            if (Double.isNaN(d)) {
                return "nan";
            } else if (Double.isInfinite(d)) {
                return d > 0 ? "inf" : "-inf";
            }
        } else if (value instanceof TemporalAccessor) {
            return value.toString();
        }
        return value;
    }
}
