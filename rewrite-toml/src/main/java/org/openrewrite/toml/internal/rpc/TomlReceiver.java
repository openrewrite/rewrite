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
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.toml.TomlVisitor;
import org.openrewrite.toml.tree.*;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

import static java.util.Objects.requireNonNull;
import static org.openrewrite.rpc.RpcReceiveQueue.toEnum;

public class TomlReceiver extends TomlVisitor<RpcReceiveQueue> {

    @Override
    public Toml preVisit(Toml t, RpcReceiveQueue q) {
        t = t.withId(q.receiveAndGet(t.getId(), UUID::fromString));
        t = t.withPrefix(q.receive(t.getPrefix(), space -> visitSpace(space, q)));
        return t.withMarkers(q.receive(t.getMarkers()));
    }

    @Override
    public Toml visitDocument(Toml.Document document, RpcReceiveQueue q) {
        Toml.Document d = document.withSourcePath(q.<Path, String>receiveAndGet(document.getSourcePath(), Paths::get));
        String charsetName = q.receive(d.getCharsetName());
        if (charsetName != null && !charsetName.equals(d.getCharsetName())) {
            d = d.withCharset(Charset.forName(charsetName));
        }
        return d.withCharsetBomMarked(q.receive(d.isCharsetBomMarked()))
                .withChecksum(q.receive(d.getChecksum()))
                .withFileAttributes(q.receive(d.getFileAttributes()))
                .withValues(q.receiveList(d.getValues(), v -> (TomlValue) visitNonNull(v, q)))
                .withEof(q.receive(d.getEof(), space -> visitSpace(space, q)));
    }

    @Override
    public Toml visitArray(Toml.Array array, RpcReceiveQueue q) {
        return array.getPadding().withValues(
                q.receiveList(array.getPadding().getValues(), rp -> visitRightPadded(rp, q)));
    }

    @Override
    public Toml visitEmpty(Toml.Empty empty, RpcReceiveQueue q) {
        return empty;
    }

    @Override
    public Toml visitIdentifier(Toml.Identifier identifier, RpcReceiveQueue q) {
        return identifier.withSource(q.receive(identifier.getSource()))
                .withName(q.receive(identifier.getName()));
    }

    @Override
    public Toml visitKeyValue(Toml.KeyValue keyValue, RpcReceiveQueue q) {
        return keyValue
                .getPadding().withKey(q.receive(keyValue.getPadding().getKey(),
                        rp -> requireNonNull(visitRightPadded(rp, q))))
                .withValue(q.receive(keyValue.getValue(), v -> visitNonNull(v, q)));
    }

    @Override
    public Toml visitLiteral(Toml.Literal literal, RpcReceiveQueue q) {
        Toml.Literal l = literal.withType(q.receiveAndGet(literal.getType(), toEnum(TomlType.Primitive.class)));
        TomlType.Primitive type = l.getType();
        return l.withSource(q.receive(l.getSource()))
                .withValue(q.receiveAndGet(l.getValue(), raw -> decodeValue(type, raw)));
    }

    @Override
    public Toml visitTable(Toml.Table table, RpcReceiveQueue q) {
        return table.getPadding().withName(q.receive(table.getPadding().getName(), rp -> visitRightPadded(rp, q)))
                .getPadding().withValues(q.receiveList(table.getPadding().getValues(), rp -> visitRightPadded(rp, q)));
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
        return right.withElement(q.receive(right.getElement(), t -> (T) visitNonNull((Toml) t, q)))
                .withAfter(q.receive(right.getAfter(), space -> visitSpace(space, q)))
                .withMarkers(q.receive(right.getMarkers()));
    }

    static Object decodeValue(TomlType.Primitive type, Object raw) {
        switch (type) {
            case Integer:
                return ((Number) raw).longValue();
            case Float:
                return raw instanceof Number ? ((Number) raw).doubleValue() : decodeFloat((String) raw);
            case LocalDate:
                return LocalDate.parse((String) raw);
            case LocalDateTime:
                return LocalDateTime.parse((String) raw);
            case LocalTime:
                return LocalTime.parse((String) raw);
            case OffsetDateTime:
                return OffsetDateTime.parse((String) raw);
            default:
                return raw;
        }
    }

    private static double decodeFloat(String raw) {
        switch (raw) {
            case "inf":
            case "+inf":
                return Double.POSITIVE_INFINITY;
            case "-inf":
                return Double.NEGATIVE_INFINITY;
            case "nan":
            case "+nan":
            case "-nan":
                return Double.NaN;
            default:
                return Double.parseDouble(raw);
        }
    }
}
