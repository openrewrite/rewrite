/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.javascript.internal.lock;

import org.jspecify.annotations.Nullable;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;
import org.openrewrite.json.JsonParser;
import org.openrewrite.json.tree.Json;
import org.openrewrite.json.tree.JsonRightPadded;
import org.openrewrite.json.tree.JsonValue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.singletonList;

/**
 * Keyed access and byte-exact edit helpers over the rewrite-json LST, which exposes only a raw member list.
 * The npm and bun patchers both edit their locks losslessly (Jackson would reformat), so both lean on these.
 */
final class LockJson {

    private LockJson() {
    }

    /** The member named {@code key}, or {@code null}. */
    static Json.@Nullable Member member(Json.@Nullable JsonObject obj, String key) {
        if (obj == null) {
            return null;
        }
        for (Json member : obj.getMembers()) {
            if (member instanceof Json.Member && key.equals(memberKey((Json.Member) member))) {
                return (Json.Member) member;
            }
        }
        return null;
    }

    /** The value of member {@code key} when it is an object, else {@code null}. */
    static Json.@Nullable JsonObject objectMember(Json.@Nullable JsonObject obj, String key) {
        Json.Member m = member(obj, key);
        return m != null && m.getValue() instanceof Json.JsonObject ? (Json.JsonObject) m.getValue() : null;
    }

    /** The value of member {@code key} when it is an array, else {@code null}. */
    static Json.@Nullable Array arrayMember(Json.@Nullable JsonObject obj, String key) {
        Json.Member m = member(obj, key);
        return m != null && m.getValue() instanceof Json.Array ? (Json.Array) m.getValue() : null;
    }

    /** Replace the value of member {@code key} in place, preserving its position and padding. */
    static Json.JsonObject replaceValue(Json.JsonObject obj, String key, JsonValue newValue) {
        List<JsonRightPadded<Json>> members = new ArrayList<>(obj.getPadding().getMembers());
        for (int i = 0; i < members.size(); i++) {
            Json el = members.get(i).getElement();
            if (el instanceof Json.Member && key.equals(memberKey((Json.Member) el))) {
                members.set(i, members.get(i).withElement(((Json.Member) el).withValue(newValue)));
                return obj.getPadding().withMembers(members);
            }
        }
        return obj;
    }

    /** The string key of a member, or {@code null} for a non-literal key. */
    static @Nullable String memberKey(Json.Member member) {
        return literal(member.getKey());
    }

    /** The string form of a literal node (a key or a value), or {@code null}. */
    static @Nullable String literal(@Nullable Json node) {
        if (node instanceof Json.Literal) {
            Object v = ((Json.Literal) node).getValue();
            return v == null ? null : v.toString();
        }
        return null;
    }

    /** The newline+indent prefix of {@code obj}'s first member, or {@code null} when it has none. */
    static @Nullable String memberWhitespace(Json.JsonObject obj) {
        for (Json member : obj.getMembers()) {
            if (member instanceof Json.Member) {
                return member.getPrefix().getWhitespace();
            }
        }
        return null;
    }

    /** Parse lock content into a lossless document; {@code path} attributes the source ({@code null} = none). */
    static Json.Document parse(String content, @Nullable Path path) {
        Parser.Input input = path == null ?
                Parser.Input.fromString(content) :
                Parser.Input.fromString(path, content);
        SourceFile sf = JsonParser.builder().build()
                .parseInputs(singletonList(input), null, new InMemoryExecutionContext())
                .findFirst().orElse(null);
        if (!(sf instanceof Json.Document)) {
            throw new EngineFailure(Reason.MALFORMED_LOCK, null, "lock content is not valid JSON");
        }
        return (Json.Document) sf;
    }
}
