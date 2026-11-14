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

import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.JsYaml;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Port of @pnpm/crypto.object-hasher src/index.ts (pnpm 11.2.2) over object-hash 3.0.0.
 */
final class ObjectHasher {

    private ObjectHasher() {
    }

    /**
     * {@code hashObjectNullableWithPrefix}: {@code sha256-<base64>}, or null for no object or an empty one.
     */
    static @Nullable String hashObjectNullableWithPrefix(@Nullable Object object) {
        if (!Js.truthy(object) || LockfileFormatConverters.isEmpty(object)) {
            return null;
        }
        return "sha256-" + Base64.getEncoder().encodeToString(sha256(serialize(object)));
    }

    /**
     * object-hash's {@code typeHasher} output with {@code unorderedArrays}, {@code unorderedObjects} and
     * {@code unorderedSets}.
     */
    static String serialize(@Nullable Object value) {
        StringBuilder out = new StringBuilder();
        new Hasher(out, new ArrayList<>()).dispatch(value);
        return out.toString();
    }

    private static final class Hasher {
        final StringBuilder out;
        List<Object> context;

        Hasher(StringBuilder out, List<Object> context) {
            this.out = out;
            this.context = context;
        }

        @SuppressWarnings("unchecked")
        void dispatch(@Nullable Object value) {
            if (value == null) {
                out.append("Null");
            } else if (value == Js.UNDEFINED) {
                out.append("Undefined");
            } else if (value instanceof String) {
                String s = (String) value;
                out.append("string:").append(s.length()).append(':').append(s);
            } else if (value instanceof Boolean) {
                out.append("bool:").append(value);
            } else if (value instanceof Number) {
                out.append("number:").append(Js.numberToString(((Number) value).doubleValue()));
            } else {
                object(value);
            }
        }

        @SuppressWarnings("unchecked")
        private void object(Object object) {
            int objectNumber = indexOf(context, object);
            if (objectNumber >= 0) {
                dispatch("[CIRCULAR:" + objectNumber + "]");
                return;
            }
            context.add(object);
            if (object instanceof List) {
                array((List<@Nullable Object>) object, true);
            } else if (object instanceof JsYaml.Timestamp) {
                out.append("date:").append(((JsYaml.Timestamp) object).toIsoString());
            } else if (object instanceof byte[]) {
                out.append("uint8array:");
                List<Object> bytes = new ArrayList<>();
                for (byte b : (byte[]) object) {
                    bytes.add((double) (b & 0xFF));
                }
                dispatch(bytes);
            } else {
                Map<String, @Nullable Object> map = (Map<String, @Nullable Object>) object;
                List<String> keys = new ArrayList<>(Js.keys(map));
                Collections.sort(keys);
                out.append("object:").append(keys.size()).append(':');
                for (String key : keys) {
                    dispatch(key);
                    out.append(':');
                    dispatch(map.get(key));
                    out.append(',');
                }
            }
        }

        private void array(List<@Nullable Object> arr, boolean unordered) {
            out.append("array:").append(arr.size()).append(':');
            if (!unordered || arr.size() <= 1) {
                for (Object entry : arr) {
                    dispatch(entry);
                }
                return;
            }
            List<Object> contextAdditions = new ArrayList<>();
            List<@Nullable Object> entries = new ArrayList<>();
            for (Object entry : arr) {
                StringBuilder strm = new StringBuilder();
                List<Object> localContext = new ArrayList<>(context);
                new Hasher(strm, localContext).dispatch(entry);
                contextAdditions.addAll(localContext.subList(context.size(), localContext.size()));
                entries.add(strm.toString());
            }
            context = new ArrayList<>(context);
            context.addAll(contextAdditions);
            entries.sort((a, b) -> ((String) a).compareTo((String) b));
            array(entries, false);
        }

        private static int indexOf(List<Object> context, Object object) {
            for (int i = 0; i < context.size(); i++) {
                if (context.get(i) == object) {
                    return i;
                }
            }
            return -1;
        }
    }

    static byte[] sha256(String input) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
