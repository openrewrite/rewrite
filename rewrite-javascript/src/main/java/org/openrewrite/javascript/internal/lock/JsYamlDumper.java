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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Port of @zkochan/js-yaml 0.0.11 lib/dumper.js {@code dump} under the options of pnpm 11.2.2's
 * {@code lockfileYamlDump}.
 */
public final class JsYamlDumper {

    private static final int INDENT = 2;
    private static final int LINE_WIDTH = -1;

    private static final int STYLE_PLAIN = 1;
    private static final int STYLE_SINGLE = 2;
    private static final int STYLE_LITERAL = 3;
    private static final int STYLE_FOLDED = 4;
    private static final int STYLE_DOUBLE = 5;

    private static final Map<Integer, String> ESCAPE_SEQUENCES = new HashMap<>();
    private static final Set<String> SINGLE_LINE_KEYS = new HashSet<>();

    static {
        ESCAPE_SEQUENCES.put(0x00, "\\0");
        ESCAPE_SEQUENCES.put(0x07, "\\a");
        ESCAPE_SEQUENCES.put(0x08, "\\b");
        ESCAPE_SEQUENCES.put(0x09, "\\t");
        ESCAPE_SEQUENCES.put(0x0A, "\\n");
        ESCAPE_SEQUENCES.put(0x0B, "\\v");
        ESCAPE_SEQUENCES.put(0x0C, "\\f");
        ESCAPE_SEQUENCES.put(0x0D, "\\r");
        ESCAPE_SEQUENCES.put(0x1B, "\\e");
        ESCAPE_SEQUENCES.put(0x22, "\\\"");
        ESCAPE_SEQUENCES.put(0x5C, "\\\\");
        ESCAPE_SEQUENCES.put(0x85, "\\N");
        ESCAPE_SEQUENCES.put(0xA0, "\\_");
        ESCAPE_SEQUENCES.put(0x2028, "\\L");
        ESCAPE_SEQUENCES.put(0x2029, "\\P");
        SINGLE_LINE_KEYS.add("cpu");
        SINGLE_LINE_KEYS.add("engines");
        SINGLE_LINE_KEYS.add("os");
        SINGLE_LINE_KEYS.add("libc");
        // a plain object: Object.prototype's members are truthy keys of it too
        SINGLE_LINE_KEYS.addAll(Arrays.asList("constructor", "__defineGetter__", "__defineSetter__",
                "hasOwnProperty", "__lookupGetter__", "__lookupSetter__", "isPrototypeOf", "propertyIsEnumerable",
                "toString", "valueOf", "__proto__", "toLocaleString"));
    }

    private @Nullable String tag;
    private @Nullable Object dump;

    private JsYamlDumper() {
    }

    /**
     * {@code dump}: the YAML text of a value, or the empty string for one js-yaml skips ({@code undefined}).
     */
    public static String dump(@Nullable Object input) {
        JsYamlDumper state = new JsYamlDumper();
        if (state.writeNode(0, input, true, true, false, false, null, false)) {
            return state.dump + "\n";
        }
        return "";
    }

    private static boolean isWhiteSpace(int c) {
        return c == ' ' || c == '\t';
    }

    private static boolean isPrintable(int c) {
        return (0x00020 <= c && c <= 0x00007E) ||
               ((0x000A1 <= c && c <= 0x00D7FF) && c != 0x2028 && c != 0x2029) ||
               ((0x0E000 <= c && c <= 0x00FFFD) && c != 0xFEFF) ||
               (0x10000 <= c && c <= 0x10FFFF);
    }

    private static boolean isNsCharOrWhitespace(int c) {
        return isPrintable(c) && c != 0xFEFF && c != '\r' && c != '\n';
    }

    private static boolean isPlainSafe(int c, int prev, boolean inblock) {
        boolean cIsNsCharOrWhitespace = isNsCharOrWhitespace(c);
        boolean cIsNsChar = cIsNsCharOrWhitespace && !isWhiteSpace(c);
        return ((inblock ? cIsNsCharOrWhitespace :
                cIsNsCharOrWhitespace && c != ',' && c != '[' && c != ']' && c != '{' && c != '}') &&
                c != '#' && !(prev == ':' && !cIsNsChar)) ||
               (isNsCharOrWhitespace(prev) && !isWhiteSpace(prev) && c == '#') ||
               (prev == ':' && cIsNsChar);
    }

    private static boolean isPlainSafeFirst(int c) {
        return isPrintable(c) && c != 0xFEFF && !isWhiteSpace(c) &&
               c != '-' && c != '?' && c != ':' && c != ',' && c != '[' && c != ']' && c != '{' && c != '}' &&
               c != '#' && c != '&' && c != '*' && c != '!' && c != '|' && c != '=' && c != '>' && c != '\'' &&
               c != '"' && c != '%' && c != '@' && c != '`';
    }

    private static boolean isPlainSafeLast(int c) {
        return !isWhiteSpace(c) && c != ':';
    }

    // String.prototype.codePointAt, NaN past the end standing as -1
    private static int codePointAt(String string, int pos) {
        if (pos < 0 || pos >= string.length()) {
            return -1;
        }
        char first = string.charAt(pos);
        if (Character.isHighSurrogate(first) && pos + 1 < string.length()) {
            char second = string.charAt(pos + 1);
            if (Character.isLowSurrogate(second)) {
                return Character.toCodePoint(first, second);
            }
        }
        return first;
    }

    private static boolean testImplicitResolving(String string) {
        for (String type : JsYaml.IMPLICIT) {
            if (JsYaml.resolve(type, string)) {
                return true;
            }
        }
        return false;
    }

    private static int chooseScalarStyle(String string, boolean singleLineOnly, boolean inblock) {
        int i;
        int c = 0;
        int prevChar = -1;
        boolean hasLineBreak = false;
        boolean plain = isPlainSafeFirst(codePointAt(string, 0)) &&
                        isPlainSafeLast(codePointAt(string, string.length() - 1));
        if (singleLineOnly) {
            for (i = 0; i < string.length(); i += c >= 0x10000 ? 2 : 1) {
                c = codePointAt(string, i);
                if (!isPrintable(c)) {
                    return STYLE_DOUBLE;
                }
                plain = plain && isPlainSafe(c, prevChar, inblock);
                prevChar = c;
            }
        } else {
            for (i = 0; i < string.length(); i += c >= 0x10000 ? 2 : 1) {
                c = codePointAt(string, i);
                if (c == '\n') {
                    hasLineBreak = true;
                } else if (!isPrintable(c)) {
                    return STYLE_DOUBLE;
                }
                plain = plain && isPlainSafe(c, prevChar, inblock);
                prevChar = c;
            }
        }
        if (!hasLineBreak) {
            if (plain && !testImplicitResolving(string)) {
                return STYLE_PLAIN;
            }
            return STYLE_SINGLE;
        }
        return STYLE_LITERAL;
    }

    private void writeScalar(String string, int level, boolean iskey, boolean inblock, boolean singleLO) {
        if (string.isEmpty()) {
            dump = "''";
            return;
        }
        int indent = INDENT * Math.max(1, level);
        boolean singleLineOnly = iskey || singleLO;
        switch (chooseScalarStyle(string, singleLineOnly, inblock)) {
            case STYLE_PLAIN:
                dump = string;
                return;
            case STYLE_SINGLE:
                dump = "'" + string.replace("'", "''") + "'";
                return;
            case STYLE_LITERAL:
                dump = "|" + blockHeader(string) + dropEndingNewline(indentString(string, indent));
                return;
            case STYLE_DOUBLE:
                dump = "\"" + escapeString(string) + "\"";
                return;
            default:
                throw new IllegalStateException("impossible error: invalid scalar style");
        }
    }

    private static String blockHeader(String string) {
        String indentIndicator = string.matches("(?s)^\\n* .*") ? String.valueOf(INDENT) : "";
        boolean clip = string.charAt(string.length() - 1) == '\n';
        boolean keep = clip && (string.length() >= 2 && string.charAt(string.length() - 2) == '\n' ||
                                "\n".equals(string));
        String chomp = keep ? "+" : (clip ? "" : "-");
        return indentIndicator + chomp + "\n";
    }

    private static String dropEndingNewline(String string) {
        return !string.isEmpty() && string.charAt(string.length() - 1) == '\n' ?
                string.substring(0, string.length() - 1) : string;
    }

    // indents every line; empty lines (\n only) are not indented
    private static String indentString(String string, int spaces) {
        String ind = EcmaScript.repeat(" ", spaces);
        int position = 0;
        StringBuilder result = new StringBuilder();
        while (position < string.length()) {
            int next = string.indexOf('\n', position);
            String line;
            if (next == -1) {
                line = string.substring(position);
                position = string.length();
            } else {
                line = string.substring(position, next + 1);
                position = next + 1;
            }
            if (!line.isEmpty() && !"\n".equals(line)) {
                result.append(ind);
            }
            result.append(line);
        }
        return result.toString();
    }

    private static String escapeString(String string) {
        StringBuilder result = new StringBuilder();
        int c = 0;
        for (int i = 0; i < string.length(); i += c >= 0x10000 ? 2 : 1) {
            c = codePointAt(string, i);
            String escapeSeq = ESCAPE_SEQUENCES.get(c);
            if (escapeSeq == null && isPrintable(c)) {
                result.append(string.charAt(i));
                if (c >= 0x10000) {
                    result.append(string.charAt(i + 1));
                }
            } else {
                result.append(escapeSeq != null ? escapeSeq : encodeHex(c));
            }
        }
        return result.toString();
    }

    private static String encodeHex(int character) {
        String string = Integer.toHexString(character).toUpperCase();
        String handle;
        int length;
        if (character <= 0xFF) {
            handle = "x";
            length = 2;
        } else if (character <= 0xFFFF) {
            handle = "u";
            length = 4;
        } else {
            handle = "U";
            length = 8;
        }
        return "\\" + handle + EcmaScript.repeat("0", length - string.length()) + string;
    }

    private static String generateNextLine(int level, boolean doubleLine) {
        return "\n" + (doubleLine ? "\n" : "") + EcmaScript.repeat(" ", INDENT * level);
    }

    @SuppressWarnings("unchecked")
    private void writeFlowSequence(int level, List<@Nullable Object> object) {
        StringBuilder result = new StringBuilder();
        String savedTag = tag;
        for (Object value : object) {
            if (writeNode(level, value, false, false, false, false, null, false) ||
                value == EcmaScript.UNDEFINED && writeNode(level, null, false, false, false, false, null, false)) {
                if (result.length() > 0) {
                    result.append(", ");
                }
                result.append(dump);
            }
        }
        tag = savedTag;
        dump = "[" + result + "]";
    }

    private void writeBlockSequence(int level, List<@Nullable Object> object, boolean compact) {
        StringBuilder result = new StringBuilder();
        String savedTag = tag;
        for (Object value : object) {
            if (writeNode(level + 1, value, true, true, false, true, null, false) ||
                value == EcmaScript.UNDEFINED && writeNode(level + 1, null, true, true, false, true, null, false)) {
                if (!compact || result.length() > 0) {
                    result.append(generateNextLine(level, false));
                }
                if (dump instanceof String && !((String) dump).isEmpty() && ((String) dump).charAt(0) == '\n') {
                    result.append('-');
                } else {
                    result.append("- ");
                }
                result.append(dump);
            }
        }
        tag = savedTag;
        dump = result.length() > 0 ? result.toString() : "[]";
    }

    private void writeFlowMapping(int level, Map<String, @Nullable Object> object, boolean singleLineOnly) {
        StringBuilder result = new StringBuilder();
        String savedTag = tag;
        for (String objectKey : EcmaScript.keys(object)) {
            StringBuilder pairBuffer = new StringBuilder();
            if (result.length() > 0) {
                pairBuffer.append(", ");
            }
            Object objectValue = object.get(objectKey);
            if (!writeNode(level, objectKey, false, false, singleLineOnly, false, null, false)) {
                continue;
            }
            String key = String.valueOf(dump);
            if (key.length() > 1024) {
                pairBuffer.append("? ");
            }
            pairBuffer.append(key).append(": ");
            if (!writeNode(level, objectValue, false, false, singleLineOnly, false, null, false)) {
                continue;
            }
            pairBuffer.append(dump);
            result.append(pairBuffer);
        }
        tag = savedTag;
        dump = "{" + result + "}";
    }

    private void writeBlockMapping(int level, Map<String, @Nullable Object> object, boolean compact,
                                   boolean doubleLine) {
        StringBuilder result = new StringBuilder();
        String savedTag = tag;
        for (String objectKey : EcmaScript.keys(object)) {
            StringBuilder pairBuffer = new StringBuilder();
            if (!compact || result.length() > 0) {
                pairBuffer.append(generateNextLine(level, doubleLine));
            }
            Object objectValue = object.get(objectKey);
            if (!writeNode(level + 1, objectKey, true, true, true, false, null, false)) {
                continue;
            }
            String key = String.valueOf(dump);
            boolean explicitPair = (tag != null && !"?".equals(tag)) || key.length() > 1024;
            if (explicitPair) {
                if (!key.isEmpty() && key.charAt(0) == '\n') {
                    pairBuffer.append('?');
                } else {
                    pairBuffer.append("? ");
                }
            }
            pairBuffer.append(key);
            if (explicitPair) {
                pairBuffer.append(generateNextLine(level, false));
            }
            if (!writeNode(level + 1, objectValue, true, explicitPair, false, false, objectKey, false)) {
                continue;
            }
            String value = String.valueOf(dump);
            if (!value.isEmpty() && value.charAt(0) == '\n') {
                pairBuffer.append(':');
            } else {
                pairBuffer.append(": ");
            }
            pairBuffer.append(value);
            result.append(pairBuffer);
        }
        tag = savedTag;
        dump = result.length() > 0 ? result.toString() : "{}";
    }

    // the implicit types that represent a value (null, bool, int, float, timestamp), then the explicit (binary)
    private boolean detectType(@Nullable Object object) {
        if (object == null) {
            tag = "?";
            dump = "null";
            return true;
        }
        if (object instanceof Boolean) {
            tag = "?";
            dump = (Boolean) object ? "true" : "false";
            return true;
        }
        if (object instanceof Number) {
            double d = ((Number) object).doubleValue();
            tag = "?";
            boolean negativeZero = d == 0 && 1 / d < 0;
            if (!Double.isNaN(d) && !Double.isInfinite(d) && d % 1 == 0 && !negativeZero) {
                dump = EcmaScript.numberToString(d);
            } else if (Double.isNaN(d)) {
                dump = ".nan";
            } else if (d == Double.POSITIVE_INFINITY) {
                dump = ".inf";
            } else if (d == Double.NEGATIVE_INFINITY) {
                dump = "-.inf";
            } else if (negativeZero) {
                dump = "-0.0";
            } else {
                String res = EcmaScript.numberToString(d);
                dump = res.matches("^[-+]?[0-9]+e.*") ? res.replaceFirst("e", ".e") : res;
            }
            return true;
        }
        if (object instanceof JsYaml.Timestamp) {
            tag = "?";
            dump = ((JsYaml.Timestamp) object).toIsoString();
            return true;
        }
        if (object instanceof byte[]) {
            tag = JsYaml.TAG_BINARY;
            dump = representBinary((byte[]) object);
            return true;
        }
        return false;
    }

    private static String representBinary(byte[] object) {
        String map = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";
        StringBuilder result = new StringBuilder();
        int bits = 0;
        int max = object.length;
        for (int idx = 0; idx < max; idx++) {
            if (idx % 3 == 0 && idx != 0) {
                result.append(map.charAt((bits >> 18) & 0x3F));
                result.append(map.charAt((bits >> 12) & 0x3F));
                result.append(map.charAt((bits >> 6) & 0x3F));
                result.append(map.charAt(bits & 0x3F));
            }
            bits = (bits << 8) + (object[idx] & 0xFF);
        }
        int tail = max % 3;
        if (tail == 0) {
            result.append(map.charAt((bits >> 18) & 0x3F));
            result.append(map.charAt((bits >> 12) & 0x3F));
            result.append(map.charAt((bits >> 6) & 0x3F));
            result.append(map.charAt(bits & 0x3F));
        } else if (tail == 2) {
            result.append(map.charAt((bits >> 10) & 0x3F));
            result.append(map.charAt((bits >> 4) & 0x3F));
            result.append(map.charAt((bits << 2) & 0x3F));
            result.append(map.charAt(64));
        } else {
            result.append(map.charAt((bits >> 2) & 0x3F));
            result.append(map.charAt((bits << 4) & 0x3F));
            result.append(map.charAt(64));
            result.append(map.charAt(64));
        }
        return result.toString();
    }

    @SuppressWarnings("unchecked")
    private boolean writeNode(int level, @Nullable Object object, boolean block, boolean compact, boolean iskey,
                              boolean isblockseq, @Nullable String objectKey, boolean singleLineOnly) {
        tag = null;
        dump = object;
        if (object == EcmaScript.UNDEFINED) {
            return false;
        }
        detectType(object);
        boolean inblock = block;
        if (tag != null && !"?".equals(tag)) {
            compact = false;
        }
        if (dump instanceof Map) {
            Map<String, @Nullable Object> map = (Map<String, @Nullable Object>) dump;
            singleLineOnly = objectKey != null && (SINGLE_LINE_KEYS.contains(objectKey) ||
                                                   "resolution".equals(objectKey) && !isVariationsOrBinary(map));
            if (block && !map.isEmpty() && !singleLineOnly) {
                boolean doubleLine = "packages".equals(objectKey) || "importers".equals(objectKey) ||
                                     "snapshots".equals(objectKey) || level == 0;
                writeBlockMapping(level, map, compact, doubleLine);
            } else {
                writeFlowMapping(level, map, singleLineOnly);
            }
        } else if (dump instanceof List) {
            List<@Nullable Object> list = (List<@Nullable Object>) dump;
            singleLineOnly = objectKey != null && SINGLE_LINE_KEYS.contains(objectKey);
            if (block && !list.isEmpty() && !singleLineOnly) {
                writeBlockSequence(level, list, compact);
            } else {
                writeFlowSequence(level, list);
            }
        } else if (dump instanceof String) {
            if (!"?".equals(tag)) {
                writeScalar((String) dump, level, iskey, inblock, singleLineOnly);
            }
        } else {
            throw new IllegalArgumentException("unacceptable kind of an object to dump " + dump.getClass());
        }
        if (tag != null && !"?".equals(tag)) {
            String tagStr = tag.startsWith("tag:yaml.org,2002:") ? "!!" + tag.substring(18) : "!<" + tag + ">";
            dump = tagStr + " " + dump;
        }
        return true;
    }

    private static boolean isVariationsOrBinary(Map<String, @Nullable Object> resolution) {
        Object type = resolution.get("type");
        return "variations".equals(type) || "binary".equals(type);
    }
}
