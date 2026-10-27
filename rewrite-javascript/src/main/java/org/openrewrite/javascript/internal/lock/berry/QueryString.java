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

import org.openrewrite.javascript.internal.lock.JsError;
import org.openrewrite.javascript.internal.lock.UriComponent;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Node.js lib/querystring.js {@code parse} and {@code stringify} with their default separators.
 */
final class QueryString {

    // Node's ERR_INVALID_URI, printed with its code
    static final String URI_ERROR = "URIError [ERR_INVALID_URI]";

    private static final String UNRESERVED = "!'()*-.0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ_abcdefghijklmnopqrstuvwxyz~";
    private static final String HEX = "0123456789ABCDEF";

    private QueryString() {
    }

    /**
     * {@code parse(qs)}: a key or value is decoded only when it holds a {@code %XX} escape (a {@code +} is a space
     * either way), and at most 1000 pairs are read.
     */
    static Map<String, Object> parse(String qs) {
        Map<String, Object> obj = new LinkedHashMap<>();
        if (qs.isEmpty()) {
            return obj;
        }
        int pairs = 1000;
        int lastPos = 0;
        boolean sawSep = false;
        boolean sawEq = false;
        StringBuilder key = new StringBuilder();
        StringBuilder value = new StringBuilder();
        boolean keyEncoded = false;
        boolean valEncoded = false;
        int encodeCheck = 0;
        for (int i = 0; i < qs.length(); ++i) {
            char code = qs.charAt(i);
            if (code == '&') {
                sawSep = true;
                int end = i;
                if (!sawEq) {
                    if (lastPos < end) {
                        key.append(qs, lastPos, end);
                    } else if (key.length() == 0) {
                        if (--pairs == 0) {
                            return obj;
                        }
                        lastPos = i + 1;
                        sawSep = sawEq = false;
                        continue;
                    }
                } else if (lastPos < end) {
                    value.append(qs, lastPos, end);
                }
                addKeyVal(obj, key.toString(), value.toString(), keyEncoded, valEncoded);
                if (--pairs == 0) {
                    return obj;
                }
                keyEncoded = valEncoded = false;
                key.setLength(0);
                value.setLength(0);
                encodeCheck = 0;
                lastPos = i + 1;
                sawSep = sawEq = false;
                continue;
            }
            sawSep = false;
            if (!sawEq) {
                if (code == '=') {
                    sawEq = true;
                    if (lastPos < i) {
                        key.append(qs, lastPos, i);
                    }
                    encodeCheck = 0;
                    lastPos = i + 1;
                    continue;
                }
                if (!keyEncoded) {
                    if (code == '%') {
                        encodeCheck = 1;
                        continue;
                    } else if (encodeCheck > 0) {
                        if (isHex(code)) {
                            if (++encodeCheck == 3) {
                                keyEncoded = true;
                            }
                            continue;
                        } else {
                            encodeCheck = 0;
                        }
                    }
                }
                if (code == '+') {
                    if (lastPos < i) {
                        key.append(qs, lastPos, i);
                    }
                    key.append(' ');
                    lastPos = i + 1;
                    continue;
                }
            }
            if (code == '+') {
                if (lastPos < i) {
                    value.append(qs, lastPos, i);
                }
                value.append(' ');
                lastPos = i + 1;
            } else if (!valEncoded) {
                if (code == '%') {
                    encodeCheck = 1;
                } else if (encodeCheck > 0) {
                    if (isHex(code)) {
                        if (++encodeCheck == 3) {
                            valEncoded = true;
                        }
                    } else {
                        encodeCheck = 0;
                    }
                }
            }
        }
        if (lastPos < qs.length()) {
            if (!sawEq) {
                key.append(qs, lastPos, qs.length());
            } else if (!sawSep) {
                value.append(qs, lastPos, qs.length());
            }
        } else if (!sawEq && key.length() == 0) {
            return obj;
        }
        addKeyVal(obj, key.toString(), value.toString(), keyEncoded, valEncoded);
        return obj;
    }

    private static boolean isHex(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    private static void addKeyVal(Map<String, Object> obj, String key, String value, boolean keyEncoded,
                                  boolean valEncoded) {
        if (!key.isEmpty() && keyEncoded) {
            key = unescape(key);
        }
        if (!value.isEmpty() && valEncoded) {
            value = unescape(value);
        }
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

    static String stringify(Map<String, ?> obj) {
        StringBuilder out = new StringBuilder();
        for (String field : Js.keys(obj)) {
            String key = escape(field) + "=";
            Object value = obj.get(field);
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

    // encodeStringified
    private static String primitive(Object value) {
        if (value instanceof String) {
            return escape((String) value);
        }
        if (value instanceof Number && !Double.isNaN(((Number) value).doubleValue()) &&
            !Double.isInfinite(((Number) value).doubleValue())) {
            String number = Js.numberToString((Number) value);
            return Math.abs(((Number) value).doubleValue()) < 1e21 ? number : escape(number);
        }
        if (value instanceof Boolean) {
            return value.toString();
        }
        return "";
    }

    /**
     * {@code qsEscape}, internal/querystring.js {@code encodeStr}: encodeURIComponent's unreserved set, but a surrogate
     * takes the next code unit as its pair, whatever it is, and only one that ends the string is a URIError.
     */
    static String escape(String str) {
        StringBuilder out = new StringBuilder();
        byte[] utf8 = new byte[4];
        for (int i = 0; i < str.length(); i++) {
            int c = str.charAt(i);
            int bytes;
            if (c < 0x80) {
                if (UNRESERVED.indexOf(c) >= 0) {
                    out.append((char) c);
                    continue;
                }
                utf8[0] = (byte) c;
                bytes = 1;
            } else if (c < 0x800) {
                utf8[0] = (byte) (0xC0 | (c >> 6));
                utf8[1] = (byte) (0x80 | (c & 0x3F));
                bytes = 2;
            } else if (c < 0xD800 || c >= 0xE000) {
                utf8[0] = (byte) (0xE0 | (c >> 12));
                utf8[1] = (byte) (0x80 | ((c >> 6) & 0x3F));
                utf8[2] = (byte) (0x80 | (c & 0x3F));
                bytes = 3;
            } else {
                if (++i >= str.length()) {
                    throw new JsError(URI_ERROR, "URI malformed");
                }
                c = 0x10000 + (((c & 0x3FF) << 10) | (str.charAt(i) & 0x3FF));
                utf8[0] = (byte) (0xF0 | (c >> 18));
                utf8[1] = (byte) (0x80 | ((c >> 12) & 0x3F));
                utf8[2] = (byte) (0x80 | ((c >> 6) & 0x3F));
                utf8[3] = (byte) (0x80 | (c & 0x3F));
                bytes = 4;
            }
            for (int b = 0; b < bytes; b++) {
                out.append('%').append(HEX.charAt((utf8[b] >> 4) & 0xF)).append(HEX.charAt(utf8[b] & 0xF));
            }
        }
        return out.toString();
    }

    // qsUnescape: decodeURIComponent, else the lenient unescapeBuffer
    private static String unescape(String s) {
        try {
            return UriComponent.decodeURIComponent(s);
        } catch (IllegalArgumentException e) {
            return unescapeBuffer(s);
        }
    }

    // unescapeBuffer(s).toString(): one byte per UTF-16 code unit (its low byte), %XX decoded, then read as UTF-8
    private static String unescapeBuffer(String s) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int maxLength = s.length() - 2;
        int index = 0;
        while (index < s.length()) {
            int currentChar = s.charAt(index);
            if (currentChar == '%' && index < maxLength) {
                currentChar = s.charAt(++index);
                int hexHigh = Character.digit(currentChar, 16);
                if (hexHigh < 0 || currentChar > 0x7F) {
                    out.write('%');
                    continue;
                }
                int nextChar = s.charAt(++index);
                int hexLow = Character.digit(nextChar, 16);
                if (hexLow < 0 || nextChar > 0x7F) {
                    out.write('%');
                    index--;
                } else {
                    currentChar = hexHigh * 16 + hexLow;
                }
            }
            out.write(currentChar & 0xFF);
            index++;
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
}
