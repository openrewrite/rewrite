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
package org.openrewrite.javascript.internal.lock.pnpm;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @pnpm/workspace.project-manifest-reader {@code readJsonFile} (pnpm 11.2.2): a package.json without its byte order
 * mark, parsed as {@code JSON.parse} would, into the values {@link Js} works over.
 */
final class Manifests {

    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .build();
    private static final ObjectReader MEMBER = MAPPER.readerFor(Object.class)
            .without(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

    private Manifests() {
    }

    /**
     * @throws PnpmError {@code ERR_PNPM_JSON_PARSE} when the text is not JSON, or is JSON but not an object
     */
    @SuppressWarnings("unchecked")
    static Map<String, @Nullable Object> parse(String text, String path) {
        Object value;
        try {
            value = order(MAPPER.readValue(LockfileReader.stripBom(text), Object.class));
        } catch (JsonProcessingException e) {
            throw new PnpmError("JSON_PARSE", e.getOriginalMessage() + " in " + path);
        }
        if (!(value instanceof Map)) {
            throw new CannotKnow("what pnpm makes of " + path + " (JSON, but not an object)");
        }
        return (Map<String, @Nullable Object>) value;
    }

    /**
     * {@code JSON.parse} of any JSON text.
     */
    static @Nullable Object parseValue(String text, String path) {
        try {
            return order(MAPPER.readValue(text, Object.class));
        } catch (JsonProcessingException e) {
            throw new PnpmError("JSON_PARSE", e.getOriginalMessage() + " in " + path);
        }
    }

    /**
     * {@code JSON.parse} of a registry document as far as the caller reads it: the top-level members {@code whole}
     * names, and of those {@code keysOnly} names, an object's keys alone (each {@code true}); null for JSON
     * {@code null}, and no member for any other value that is not an object. The rest is checked, not kept.
     *
     * @throws PnpmError {@code ERR_PNPM_JSON_PARSE} when the text is not JSON
     */
    static @Nullable Map<String, @Nullable Object> parseMembers(String text, String path, Set<String> whole,
                                                                Set<String> keysOnly) {
        try (JsonParser parser = MAPPER.getFactory().createParser(text)) {
            Map<String, @Nullable Object> members = new LinkedHashMap<>();
            JsonToken token = parser.nextToken();
            if (token == JsonToken.VALUE_NULL) {
                members = null;
            } else if (token != JsonToken.START_OBJECT) {
                parser.skipChildren();
            } else {
                while (parser.nextToken() == JsonToken.FIELD_NAME) {
                    String name = parser.currentName();
                    JsonToken value = parser.nextToken();
                    if (keysOnly.contains(name) && value == JsonToken.START_OBJECT) {
                        Map<String, @Nullable Object> keys = new LinkedHashMap<>();
                        while (parser.nextToken() == JsonToken.FIELD_NAME) {
                            keys.put(parser.currentName(), Boolean.TRUE);
                            parser.nextToken();
                            parser.skipChildren();
                        }
                        members.put(name, Js.ordered(keys));
                    } else if (whole.contains(name) || keysOnly.contains(name)) {
                        members.put(name, order(MEMBER.readValue(parser)));
                    } else {
                        parser.skipChildren();
                    }
                }
            }
            if (parser.nextToken() != null) {
                throw new PnpmError("JSON_PARSE", "Unexpected non-whitespace character after JSON in " + path);
            }
            return members;
        } catch (IOException e) {
            throw new PnpmError("JSON_PARSE", e.getMessage() + " in " + path);
        }
    }

    @SuppressWarnings("unchecked")
    private static @Nullable Object order(@Nullable Object value) {
        if (value instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) value;
            Map<String, @Nullable Object> ordered = new LinkedHashMap<>();
            for (String key : Js.keys(map)) {
                ordered.put(key, order(map.get(key)));
            }
            return ordered;
        }
        if (value instanceof List) {
            List<@Nullable Object> list = new ArrayList<>();
            for (Object item : (List<Object>) value) {
                list.add(order(item));
            }
            return list;
        }
        if (value instanceof Number && !(value instanceof Double)) {
            return ((Number) value).doubleValue();
        }
        return value;
    }
}
