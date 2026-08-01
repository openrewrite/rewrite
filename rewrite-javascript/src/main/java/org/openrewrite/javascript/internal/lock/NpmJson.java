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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;

import java.util.ArrayList;
import java.util.List;

/**
 * The byte-exact npm {@code package-lock.json} serialization primitives, shared by the surgical
 * {@link NpmLockPatcher} and the {@link org.openrewrite.javascript.internal.lock.resolve.NpmLockWriter}. npm
 * serializes through {@code json-stringify-nice}: object keys are ordered by {@link NpmKeyOrder} within a
 * value-kind partition (primitive-valued keys before object-valued ones), values pretty-printed at a fixed
 * two-space step. Both the patcher (inserting one member into a captured LST) and the writer (emitting a whole
 * file) must reproduce that exactly, so the rules live here once.
 */
public final class NpmJson {

    private static final ObjectMapper JSON = new ObjectMapper();

    private NpmJson() {
    }

    /** Order two object keys as npm's lock serializer would within one value-kind partition. */
    public static int compareKeys(String a, String b) {
        return NpmKeyOrder.compareKeys(a, b);
    }

    /**
     * Pretty-print a JSON value exactly as npm's {@code json-stringify-nice} does at {@code indent}, stepping by
     * {@code unit}. Object members are ordered with primitive-valued keys before object-valued ones, each group
     * sorted by {@link #compareKeys}; a nested object/array is rendered recursively.
     */
    public static String render(JsonNode node, String indent, String unit) {
        if (node.isObject()) {
            if (node.size() == 0) {
                return "{}";
            }
            String inner = indent + unit;
            List<String> keys = new ArrayList<>();
            node.fieldNames().forEachRemaining(keys::add);
            keys.sort((a, b) -> {
                boolean ao = node.get(a).isObject();
                boolean bo = node.get(b).isObject();
                return ao != bo ? (ao ? 1 : -1) : compareKeys(a, b);
            });
            List<String> members = new ArrayList<>();
            for (String k : keys) {
                members.add("\n" + inner + jsonEncode(k) + ": " + render(node.get(k), inner, unit));
            }
            return "{" + String.join(",", members) + "\n" + indent + "}";
        }
        if (node.isArray()) {
            if (node.size() == 0) {
                return "[]";
            }
            String inner = indent + unit;
            List<String> elements = new ArrayList<>();
            for (JsonNode el : node) {
                elements.add("\n" + inner + render(el, inner, unit));
            }
            return "[" + String.join(",", elements) + "\n" + indent + "]";
        }
        return scalarSource(node);
    }

    /** Jackson-escaped quoted JSON string literal (registry values may carry {@code "}/{@code \}/newlines). */
    public static String jsonEncode(String value) {
        try {
            return JSON.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new EngineFailure(Reason.MALFORMED_LOCK, null, "could not JSON-encode value: " + value);
        }
    }

    private static String scalarSource(JsonNode node) {
        try {
            return JSON.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            throw new EngineFailure(Reason.MALFORMED_LOCK, null, "could not JSON-encode value: " + node);
        }
    }
}
