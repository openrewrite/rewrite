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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Value;
import org.jspecify.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.zip.GZIPInputStream;

/**
 * The rows {@code generate/upstream-calls.js} writes: each call an upstream release's own tests make into the library,
 * with its answer, as JavaScript values ({@code undefined} is {@link EcmaScript#UNDEFINED}, numbers are doubles).
 */
public final class UpstreamCalls {

    private static final ObjectMapper JSON = new ObjectMapper();

    /**
     * An object {@code Object.create(null)} made.
     */
    public static final class NullProto extends LinkedHashMap<String, @Nullable Object> {
    }

    @Value
    public static class Call {
        String test;
        String fn;

        /**
         * The receiver of a method, as the spec writes it.
         */
        @Nullable
        Object self;

        List<@Nullable Object> args;

        @Nullable
        Object out;

        /**
         * The error's {@code name}, {@code message} and {@code code}, or null when the call returned.
         */
        @Nullable
        Map<String, @Nullable Object> threw;

        /**
         * The receiver after a method the spec names as one that changes it.
         */
        @Nullable
        Object selfAfter;

        public @Nullable Object arg(int i) {
            return i < args.size() ? args.get(i) : EcmaScript.UNDEFINED;
        }

        /**
         * Whether the call passed an argument at {@code i} that is not undefined.
         */
        public boolean has(int i) {
            return i < args.size() && args.get(i) != EcmaScript.UNDEFINED;
        }

        @Override
        public String toString() {
            return (self != null ? shape(self) + "." : "") + fn + args +
                   (threw != null ? " throws " + threw.get("message") : " = " + out);
        }
    }

    private UpstreamCalls() {
    }

    public static List<Call> read(String resource) {
        String path = "/org/openrewrite/javascript/internal/lock/upstream/" + resource;
        List<Call> calls = new ArrayList<>();
        try (InputStream raw = Objects.requireNonNull(UpstreamCalls.class.getResourceAsStream(path), path);
             InputStream in = resource.endsWith(".gz") ? new GZIPInputStream(raw) : raw;
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) {
                    continue;
                }
                JsonNode row = JSON.readTree(line);
                if (row.has("header")) {
                    continue;
                }
                List<@Nullable Object> args = new ArrayList<>();
                for (JsonNode arg : row.get("args")) {
                    args.add(value(arg));
                }
                @SuppressWarnings("unchecked")
                Map<String, @Nullable Object> threw = row.has("threw") ?
                        (Map<String, @Nullable Object>) value(row.get("threw")) : null;
                calls.add(new Call(row.get("test").asText(), row.get("fn").asText(),
                        row.has("self") ? value(row.get("self")) : null, args,
                        row.has("out") ? value(row.get("out")) : null, threw,
                        row.has("selfAfter") ? value(row.get("selfAfter")) : null));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return calls;
    }

    /**
     * A value written out with its kind of object, so that two values compare equal only when their objects have the
     * same prototype too: {@code null-proto{...}} for {@link NullProto}.
     */
    @SuppressWarnings("unchecked")
    public static String shape(@Nullable Object value) {
        if (value instanceof Map) {
            Map<String, ?> map = (Map<String, ?>) value;
            StringBuilder out = new StringBuilder(value instanceof NullProto ? "null-proto{" : "{");
            boolean first = true;
            for (String key : EcmaScript.keys(map)) {
                out.append(first ? "" : ", ").append(key).append(": ").append(shape(map.get(key)));
                first = false;
            }
            return out.append('}').toString();
        }
        if (value instanceof List) {
            StringBuilder out = new StringBuilder("[");
            boolean first = true;
            for (Object item : (List<?>) value) {
                out.append(first ? "" : ", ").append(shape(item));
                first = false;
            }
            return out.append(']').toString();
        }
        return value instanceof String ? EcmaScript.quote((String) value) : String.valueOf(value);
    }

    /**
     * The rows of a function, split into those the port answers and those it does not, each of these named.
     */
    public static List<Call> select(List<Call> calls, String fn, Predicate<Call> ported) {
        List<Call> out = new ArrayList<>();
        for (Call call : calls) {
            if (call.getFn().equals(fn) && ported.test(call)) {
                out.add(call);
            }
        }
        return out;
    }

    /**
     * How many rows each reason leaves out: the calls no predicate of {@code ported} takes, by the reason
     * {@code reason} gives each.
     */
    public static Map<String, Integer> notPorted(List<Call> calls, Predicate<Call> ported,
                                                 Function<Call, String> reason) {
        Map<String, Integer> out = new LinkedHashMap<>();
        for (Call call : calls) {
            if (!ported.test(call)) {
                out.merge(reason.apply(call), 1, Integer::sum);
            }
        }
        return out;
    }

    public static @Nullable Object value(JsonNode node) {
        if (node.isNull()) {
            return null;
        }
        if (node.isTextual()) {
            return node.asText();
        }
        if (node.isBoolean()) {
            return node.asBoolean();
        }
        if (node.isNumber()) {
            return node.asDouble();
        }
        if (node.isArray()) {
            List<@Nullable Object> list = new ArrayList<>();
            for (JsonNode item : node) {
                list.add(value(item));
            }
            return list;
        }
        JsonNode kind = node.get("$");
        if (kind != null && kind.isTextual()) {
            switch (kind.asText()) {
                case "undefined":
                    return EcmaScript.UNDEFINED;
                case "nullproto": {
                    NullProto out = new NullProto();
                    fields(node.get("value"), out);
                    return out;
                }
                case "number":
                    return "NaN".equals(node.get("value").asText()) ? Double.NaN :
                            "-0".equals(node.get("value").asText()) ? -0.0 :
                                    node.get("value").asText().startsWith("-") ? Double.NEGATIVE_INFINITY :
                                            Double.POSITIVE_INFINITY;
                default:
                    break;
            }
        }
        Map<String, @Nullable Object> out = new LinkedHashMap<>();
        fields(node, out);
        return out;
    }

    private static void fields(JsonNode node, Map<String, @Nullable Object> out) {
        Iterator<Map.Entry<String, JsonNode>> it = node.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> field = it.next();
            out.put(field.getKey(), value(field.getValue()));
        }
    }

    /**
     * A port's value as the rows hold one: numbers as doubles, maps and lists copied, {@code nullProto} maps as
     * {@link NullProto}.
     */
    public static @Nullable Object normalize(@Nullable Object value, Predicate<Object> nullProto) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof Map) {
            Map<String, @Nullable Object> out = nullProto.test(value) ? new NullProto() : new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : ((Map<?, ?>) value).entrySet()) {
                out.put(String.valueOf(e.getKey()), normalize(e.getValue(), nullProto));
            }
            return out;
        }
        if (value instanceof List) {
            List<@Nullable Object> out = new ArrayList<>();
            for (Object item : (List<?>) value) {
                out.add(normalize(item, nullProto));
            }
            return out;
        }
        return value;
    }
}
