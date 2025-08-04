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
import org.openrewrite.json.tree.Space;
import org.openrewrite.rpc.RpcReceiveQueue;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

public class JsonReceiver extends JsonVisitor<RpcReceiveQueue> {

    @Override
    public Json preVisit(@NonNull Json j, RpcReceiveQueue q) {
        j = j.withId(q.receiveAndGet(j.getId(), UUID::fromString));
        j = j.withPrefix(q.receive(j.getPrefix(), space -> visitSpace(space, q)));
        return j.withMarkers(q.receiveMarkers(j.getMarkers()));
    }

    @Override
    public Json visitDocument(Json.Document document, RpcReceiveQueue q) {
        return document.withSourcePath(q.<Path, String>receiveAndGet(document.getSourcePath(), Paths::get))
                .withCharset(q.<Charset, String>receiveAndGet(document.getCharset(), Charset::forName))
                .withCharsetBomMarked(q.receive(document.isCharsetBomMarked()))
                .withChecksum(q.receive(document.getChecksum()))
                .withFileAttributes(q.receive(document.getFileAttributes()))
                .withValue(q.receive(document.getValue(), j -> (JsonValue) visitNonNull(j, q)))
                .withEof(q.receive(document.getEof(), space -> visitSpace(space, q)));
    }

    @Override
    public Json visitArray(Json.Array array, RpcReceiveQueue q) {
        return array.getPadding().withValues(
                q.receiveList(array.getPadding().getValues(), j -> visitRightPadded(j, q)));
    }

    @Override
    public Json visitEmpty(Json.Empty empty, RpcReceiveQueue q) {
        return empty;
    }

    @Override
    public Json visitIdentifier(Json.Identifier identifier, RpcReceiveQueue q) {
        return identifier.withName(q.receive(identifier.getName()));
    }

    @Override
    public Json visitLiteral(Json.Literal literal, RpcReceiveQueue q) {
        return literal.withSource(q.receive(literal.getSource()))
                .withValue(q.receive(literal.getValue()));
    }

    @Override
    public Json visitMember(Json.Member member, RpcReceiveQueue q) {
        return member
                .getPadding().withKey(q.receive(member.getPadding().getKey(),
                        j -> requireNonNull(visitRightPadded(j, q))))
                .withValue(q.receive(member.getValue(), j -> (JsonValue) visitNonNull(j, q)));
    }

    @Override
    public Json visitObject(Json.JsonObject object, RpcReceiveQueue q) {
        return object.getPadding().withMembers(
                q.receiveList(object.getPadding().getMembers(), j -> visitRightPadded(j, q)));
    }

    @Override
    public Space visitSpace(Space space, RpcReceiveQueue q) {
        return space
                .withComments(q.receiveList(space.getComments(), c -> c
                        .withMultiline(q.receive(c.isMultiline()))
                        .withText(q.receive(c.getText()))
                        .withSuffix(q.receive(c.getSuffix()))
                        .withMarkers(q.receiveMarkers(c.getMarkers()))))
                .withWhitespace(q.receive(space.getWhitespace()));
    }

    @Override
    public <T extends Json> JsonRightPadded<T> visitRightPadded(@Nullable JsonRightPadded<T> right, RpcReceiveQueue q) {
        assert right != null : "TreeDataReceiveQueue should have instantiated an empty padding";

        //noinspection unchecked
        return right.withElement(q.receive(right.getElement(), j -> (T) visitNonNull(j, q)))
                .withAfter(q.receive(right.getAfter(), space -> visitSpace(space, q)))
                .withMarkers(q.receiveMarkers(right.getMarkers()));
    }
}
