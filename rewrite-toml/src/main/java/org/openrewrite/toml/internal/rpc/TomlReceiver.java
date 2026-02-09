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
package org.openrewrite.toml.internal.rpc;

import org.jspecify.annotations.Nullable;
import org.openrewrite.toml.TomlVisitor;
import org.openrewrite.toml.tree.*;
import org.openrewrite.rpc.RpcReceiveQueue;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

public class TomlReceiver extends TomlVisitor<RpcReceiveQueue> {

    @Override
    public Toml preVisit(Toml j, RpcReceiveQueue q) {
        j = j.withId(q.receiveAndGet(j.getId(), UUID::fromString));
        j = j.withPrefix(q.receive(j.getPrefix(), space -> visitSpace(space, q)));
        return j.withMarkers(q.receive(j.getMarkers()));
    }

    @Override
    public Toml visitDocument(Toml.Document document, RpcReceiveQueue q) {
        return document.withSourcePath(q.<Path, String>receiveAndGet(document.getSourcePath(), Paths::get))
                .withCharset(q.<Charset, String>receiveAndGet(document.getCharset(), Charset::forName))
                .withCharsetBomMarked(q.receive(document.isCharsetBomMarked()))
                .withChecksum(q.receive(document.getChecksum()))
                .withFileAttributes(q.receive(document.getFileAttributes()))
                .withValues(q.receiveList(document.getValues(), v -> (TomlValue) visitNonNull(v, q)))
                .withEof(q.receive(document.getEof(), space -> visitSpace(space, q)));
    }

    @Override
    public Toml visitArray(Toml.Array array, RpcReceiveQueue q) {
        return array.getPadding().withValues(
                q.receiveList(array.getPadding().getValues(), j -> visitRightPadded(j, q)));
    }

    @Override
    public Toml visitTable(Toml.Table table, RpcReceiveQueue q) {
        table = table.getPadding().withName(q.receive(table.getPadding().getName(),
                rp -> requireNonNull(visitRightPadded(rp, q))));
        return table.getPadding().withValues(
                q.receiveList(table.getPadding().getValues(), j -> visitRightPadded(j, q)));
    }

    @Override
    public Toml visitKeyValue(Toml.KeyValue keyValue, RpcReceiveQueue q) {
        return keyValue
                .getPadding().withKey(q.receive(keyValue.getPadding().getKey(),
                        j -> requireNonNull(visitRightPadded(j, q))))
                .withValue(q.receive(keyValue.getValue(), j -> visitNonNull(j, q)));
    }

    @Override
    public Toml visitLiteral(Toml.Literal literal, RpcReceiveQueue q) {
        return literal.withType(q.receiveAndGet(literal.getType(), RpcReceiveQueue.toEnum(TomlType.Primitive.class)))
                .withSource(q.receive(literal.getSource()))
                .withValue(q.receive(literal.getValue()));
    }

    @Override
    public Toml visitIdentifier(Toml.Identifier identifier, RpcReceiveQueue q) {
        return identifier.withSource(q.receive(identifier.getSource()))
                .withName(q.receive(identifier.getName()));
    }

    @Override
    public Toml visitEmpty(Toml.Empty empty, RpcReceiveQueue q) {
        return empty;
    }

    @Override
    public Space visitSpace(Space space, RpcReceiveQueue q) {
        return space
                .withComments(q.receiveList(space.getComments(), c -> c
                        .withText(q.receive(c.getText()))
                        .withSuffix(q.receive(c.getSuffix()))
                        .withMarkers(q.receive(c.getMarkers()))))
                .withWhitespace(q.receive(space.getWhitespace()));
    }

    @Override
    public <T> TomlRightPadded<T> visitRightPadded(@Nullable TomlRightPadded<T> right, RpcReceiveQueue q) {
        assert right != null : "TreeDataReceiveQueue should have instantiated an empty padding";

        //noinspection unchecked
        return right.withElement(q.receive(right.getElement(), j -> (T) visitNonNull((Toml) j, q)))
                .withAfter(q.receive(right.getAfter(), space -> visitSpace(space, q)))
                .withMarkers(q.receive(right.getMarkers()));
    }
}
