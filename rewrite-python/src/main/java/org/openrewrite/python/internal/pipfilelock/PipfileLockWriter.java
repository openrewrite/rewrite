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
package org.openrewrite.python.internal.pipfilelock;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads and writes `Pipfile.lock` content byte-compatibly with pipenv's
 * {@code _LockFileEncoder}: {@code json.JSONEncoder(indent=4, separators=(",", ": "),
 * sort_keys=True)} with {@code ensure_ascii=True}, plus exactly one trailing newline
 * ({@code write_lockfile}, {@code pipenv/project.py}).
 */
public final class PipfileLockWriter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private PipfileLockWriter() {
    }

    /**
     * @param root    the lock structure; values may be {@code Map}, {@code List},
     *                {@code String}, {@code Boolean}, {@code Long}/{@code BigInteger}, or null
     * @param newline {@code "\n"} or {@code "\r\n"}; pipenv preserves the pre-existing
     *                lock's newline style
     * @return lock content byte-identical to what pipenv would write
     */
    public static String write(Map<String, Object> root, String newline) {
        if (!"\n".equals(newline) && !"\r\n".equals(newline)) {
            throw new IllegalArgumentException("newline must be \"\\n\" or \"\\r\\n\"");
        }
        StringBuilder out = new StringBuilder();
        writeValue(root, 0, out);
        out.append('\n');
        String content = out.toString();
        return "\n".equals(newline) ? content : content.replace("\n", newline);
    }

    /**
     * Parses lock JSON into plain Java structures ({@code LinkedHashMap}/{@code ArrayList},
     * integers as {@code Long}, or {@code BigInteger} when too large) such that
     * {@code write(read(lock), ...)} reproduces a pipenv-written lock byte-identically.
     */
    public static Map<String, Object> read(String lockJson) {
        JsonNode node;
        try {
            node = MAPPER.readTree(lockJson);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Malformed Pipfile.lock JSON", e);
        }
        if (node == null || !node.isObject()) {
            throw new IllegalArgumentException("Expected a top-level JSON object in Pipfile.lock");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> root = (Map<String, Object>) convert(node);
        return root;
    }

    private static @Nullable Object convert(JsonNode node) {
        if (node.isObject()) {
            Map<String, Object> map = new LinkedHashMap<>();
            for (Iterator<Map.Entry<String, JsonNode>> it = node.fields(); it.hasNext(); ) {
                Map.Entry<String, JsonNode> entry = it.next();
                map.put(entry.getKey(), convert(entry.getValue()));
            }
            return map;
        }
        if (node.isArray()) {
            List<Object> list = new ArrayList<>(node.size());
            for (JsonNode element : node) {
                list.add(convert(element));
            }
            return list;
        }
        if (node.isTextual()) {
            return node.textValue();
        }
        if (node.isBoolean()) {
            return node.booleanValue();
        }
        if (node.isIntegralNumber()) {
            return node.canConvertToLong() ? (Object) node.longValue() : node.bigIntegerValue();
        }
        if (node.isNull()) {
            return null;
        }
        throw new IllegalArgumentException("Unsupported JSON value in Pipfile.lock: " + node.getNodeType());
    }

    private static void writeValue(@Nullable Object value, int depth, StringBuilder out) {
        if (value instanceof Map) {
            writeMap((Map<?, ?>) value, depth, out);
        } else if (value instanceof List) {
            writeList((List<?>) value, depth, out);
        } else {
            CanonicalJsonEmitter.writeScalar(value, out);
        }
    }

    private static void writeMap(Map<?, ?> map, int depth, StringBuilder out) {
        if (map.isEmpty()) {
            out.append("{}");
            return;
        }
        out.append('{');
        boolean first = true;
        for (String key : CanonicalJsonEmitter.sortedKeys(map)) {
            if (!first) {
                out.append(',');
            }
            first = false;
            newlineIndent(depth + 1, out);
            CanonicalJsonEmitter.writeString(key, out);
            out.append(": ");
            writeValue(map.get(key), depth + 1, out);
        }
        newlineIndent(depth, out);
        out.append('}');
    }

    private static void writeList(List<?> list, int depth, StringBuilder out) {
        if (list.isEmpty()) {
            out.append("[]");
            return;
        }
        out.append('[');
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                out.append(',');
            }
            newlineIndent(depth + 1, out);
            writeValue(list.get(i), depth + 1, out);
        }
        newlineIndent(depth, out);
        out.append(']');
    }

    private static void newlineIndent(int depth, StringBuilder out) {
        out.append('\n');
        for (int i = 0; i < depth; i++) {
            out.append("    ");
        }
    }
}
