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
package org.openrewrite.javascript.internal.lock.berry;

import org.openrewrite.javascript.internal.lock.npm.UriComponent;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Node.js lib/querystring.js {@code parse} and {@code stringify} with their default separators: a parsed
 * value is a string, or a list of strings for a repeated key.
 */
final class QueryString {

    private QueryString() {
    }

    static Map<String, Object> parse(String qs) {
        Map<String, Object> obj = new LinkedHashMap<>();
        if (qs.isEmpty()) {
            return obj;
        }
        for (String piece : qs.split("&", -1)) {
            if (piece.isEmpty()) {
                continue;
            }
            int eq = piece.indexOf('=');
            String key = unescape(eq == -1 ? piece : piece.substring(0, eq));
            String value = eq == -1 ? "" : unescape(piece.substring(eq + 1));
            Object existing = obj.get(key);
            if (existing == null) {
                obj.put(key, value);
            } else if (existing instanceof List) {
                @SuppressWarnings("unchecked") List<String> list = (List<String>) existing;
                list.add(value);
            } else {
                List<String> list = new ArrayList<>();
                list.add((String) existing);
                list.add(value);
                obj.put(key, list);
            }
        }
        return obj;
    }

    static String stringify(Map<String, ?> obj) {
        StringBuilder out = new StringBuilder();
        for (Map.Entry<String, ?> field : obj.entrySet()) {
            String key = UriComponent.encodeURIComponent(field.getKey()) + "=";
            Object value = field.getValue();
            if (value instanceof List) {
                for (Object item : (List<?>) value) {
                    if (out.length() > 0) {
                        out.append('&');
                    }
                    out.append(key).append(primitive(item));
                }
                continue;
            }
            if (out.length() > 0) {
                out.append('&');
            }
            out.append(key).append(primitive(value));
        }
        return out.toString();
    }

    private static String primitive(Object value) {
        if (value instanceof String) {
            return UriComponent.encodeURIComponent((String) value);
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        return "";
    }

    // querystring.unescape: decodeURIComponent after `+` becomes a space, else the lenient unescapeBuffer
    private static String unescape(String s) {
        String plus = s.replace('+', ' ');
        try {
            return UriComponent.decodeURIComponent(plus);
        } catch (IllegalArgumentException e) {
            return unescapeBuffer(plus);
        }
    }

    private static String unescapeBuffer(String s) {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int i = 0; i < bytes.length; i++) {
            if (bytes[i] == '%' && i + 2 < bytes.length && Character.digit(bytes[i + 1], 16) >= 0 &&
                Character.digit(bytes[i + 2], 16) >= 0) {
                out.write(Character.digit(bytes[i + 1], 16) * 16 + Character.digit(bytes[i + 2], 16));
                i += 2;
            } else {
                out.write(bytes[i]);
            }
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
}
