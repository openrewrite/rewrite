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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.openrewrite.json.JsonVisitor;
import org.openrewrite.json.tree.Json;
import org.openrewrite.json.tree.JsonRightPadded;
import org.openrewrite.json.tree.JsonValue;
import org.openrewrite.rpc.RpcReceiveQueue;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

public class JsonReceiver extends JsonVisitor<RpcReceiveQueue> {

    @Override
    public Json preVisit(@NonNull Json j, RpcReceiveQueue q) {
        j = j.withId(UUID.fromString(q.receiveAndGet(j.getId(), UUID::toString)));
        j = j.withPrefix(q.receive(j.getPrefix()));
        j = j.withMarkers(q.receive(j.getMarkers()));
        return j;
    }

    public Json visitDocument(Json.Document document, RpcReceiveQueue q) {
        String sourcePath = q.receiveAndGet(document.getSourcePath(), Path::toString);
        return document.withSourcePath(Paths.get(sourcePath))
                .withCharset(Charset.forName(q.receiveAndGet(document.getCharset(), Charset::name)))
                .withCharsetBomMarked(q.receive(document.isCharsetBomMarked()))
                .withChecksum(q.receive(document.getChecksum()))
                .withFileAttributes(q.receive(document.getFileAttributes()))
                .withValue(q.receive(document.getValue(), j -> (JsonValue) visitNonNull(j, q)))
                .withEof(q.receive(document.getEof()));
    }

    public Json visitArray(Json.Array array, RpcReceiveQueue q) {
        return array.getPadding().withValues(
                q.receiveList(array.getPadding().getValues(), j -> visitRightPadded(j, q)));
    }

    public Json visitEmpty(Json.Empty empty, RpcReceiveQueue q) {
        return empty;
    }

    public Json visitIdentifier(Json.Identifier identifier, RpcReceiveQueue q) {
        return identifier.withName(q.receive(identifier.getName()));
    }

    public Json visitLiteral(Json.Literal literal, RpcReceiveQueue q) {
        return literal.withSource(q.receive(literal.getSource()))
                .withValue(q.receive(literal.getValue()));
    }

    public Json visitMember(Json.Member member, RpcReceiveQueue q) {
        return member
                .getPadding().withKey(q.receive(member.getPadding().getKey(),
                        j -> requireNonNull(visitRightPadded(j, q))))
                .withValue(q.receive(member.getValue(), j -> (JsonValue) visitNonNull(j, q)));
    }

    public Json visitObject(Json.JsonObject object, RpcReceiveQueue q) {
        return object.getPadding().withMembers(
                q.receiveList(object.getPadding().getMembers(), j -> visitRightPadded(j, q)));
    }

    @Override
    public @Nullable <T extends Json> JsonRightPadded<T> visitRightPadded(@Nullable JsonRightPadded<T> right, RpcReceiveQueue q) {
        assert right != null : "TreeDataReceiveQueue should have instantiated an empty padding";

        //noinspection unchecked
        return right.withElement(q.receive(right.getElement(), j -> (T) visitNonNull(j, q)))
                .withAfter(q.receive(right.getAfter()))
                .withMarkers(q.receive(right.getMarkers()));
    }
}
