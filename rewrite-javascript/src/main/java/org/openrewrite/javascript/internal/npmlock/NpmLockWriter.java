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
package org.openrewrite.javascript.internal.npmlock;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Emits {@code package-lock.json} byte-identically to npm. npm writes the lock via
 * {@code json-stringify-nice(data, swKeyOrder, indent) + '\n'} with {@code \n}
 * replaced by the original file's line ending — a deterministic full-document
 * re-serialization that sorts every object's keys: non-object values before object
 * values, the {@code swKeyOrder} priority list first, then {@code localeCompare('en')}.
 * <p>
 * The en-collation comparator is reproduced as a two-pass comparison (per-character
 * primary weights with case folded, then lowercase-before-uppercase), verified against
 * {@code Intl.Collator}-sorted vectors in {@code src/test/resources/npmlock/}.
 */
public final class NpmLockWriter {

    private static final List<String> SW_KEY_ORDER = java.util.Arrays.asList(
            "name", "version", "lockfileVersion", "resolved", "integrity", "requires", "packages", "dependencies");

    /**
     * Primary collation weights: characters in ascending {@code localeCompare('en')}
     * order with upper/lowercase folded together. Generated with Node's Intl collator.
     */
    private static final String PRIMARY_ORDER = " _-,;:!?.'\"()[]{}@*/\\&#%`^+<=>|~$0123456789abcdefghijklmnopqrstuvwxyz";
    private static final int[] PRIMARY_WEIGHT = new int[128];

    static {
        java.util.Arrays.fill(PRIMARY_WEIGHT, -1);
        for (int i = 0; i < PRIMARY_ORDER.length(); i++) {
            char c = PRIMARY_ORDER.charAt(i);
            PRIMARY_WEIGHT[c] = i;
            if (c >= 'a' && c <= 'z') {
                PRIMARY_WEIGHT[Character.toUpperCase(c)] = i;
            }
        }
    }

    private NpmLockWriter() {
    }

    public static String write(JsonNode root, String indent, String eol) {
        StringBuilder sb = new StringBuilder();
        writeValue(root, sb, indent, "");
        sb.append('\n');
        String out = sb.toString();
        return "\n".equals(eol) ? out : out.replace("\n", eol);
    }

    private static void writeValue(JsonNode node, StringBuilder sb, String indent, String gap) {
        if (node.isObject()) {
            writeObject(node, sb, indent, gap);
        } else if (node.isArray()) {
            writeArray(node, sb, indent, gap);
        } else if (node.isTextual()) {
            writeString(node.textValue(), sb);
        } else if (node.isNull()) {
            sb.append("null");
        } else if (node.isBoolean()) {
            sb.append(node.booleanValue());
        } else if (node.isIntegralNumber()) {
            sb.append(node.bigIntegerValue());
        } else {
            sb.append(node.decimalValue().stripTrailingZeros().toPlainString());
        }
    }

    private static void writeObject(JsonNode node, StringBuilder sb, String indent, String gap) {
        if (node.isEmpty()) {
            sb.append("{}");
            return;
        }
        List<Map.Entry<String, JsonNode>> entries = new ArrayList<>();
        node.fields().forEachRemaining(entries::add);
        entries.sort((a, b) -> {
            boolean aObj = isObj(a.getValue());
            boolean bObj = isObj(b.getValue());
            if (aObj != bObj) {
                return aObj ? 1 : -1;
            }
            return compareKeys(a.getKey(), b.getKey());
        });
        String childGap = gap + indent;
        sb.append("{\n");
        for (int i = 0; i < entries.size(); i++) {
            sb.append(childGap);
            writeString(entries.get(i).getKey(), sb);
            sb.append(": ");
            writeValue(entries.get(i).getValue(), sb, indent, childGap);
            if (i < entries.size() - 1) {
                sb.append(',');
            }
            sb.append('\n');
        }
        sb.append(gap).append('}');
    }

    private static void writeArray(JsonNode node, StringBuilder sb, String indent, String gap) {
        if (node.isEmpty()) {
            sb.append("[]");
            return;
        }
        String childGap = gap + indent;
        sb.append("[\n");
        for (int i = 0; i < node.size(); i++) {
            sb.append(childGap);
            writeValue(node.get(i), sb, indent, childGap);
            if (i < node.size() - 1) {
                sb.append(',');
            }
            sb.append('\n');
        }
        sb.append(gap).append(']');
    }

    private static boolean isObj(JsonNode node) {
        return node.isObject();
    }

    static int compareKeys(String a, String b) {
        int prefA = SW_KEY_ORDER.indexOf(a);
        int prefB = SW_KEY_ORDER.indexOf(b);
        if (prefA >= 0 || prefB >= 0) {
            if (prefA < 0) {
                return 1;
            }
            if (prefB < 0) {
                return -1;
            }
            return Integer.compare(prefA, prefB);
        }
        return localeCompareEn(a, b);
    }

    /**
     * {@code String.prototype.localeCompare(other, 'en')}: primary weights across the
     * whole string first (shorter prefix wins), then case as a tiebreak per position
     * (lowercase before uppercase).
     */
    static int localeCompareEn(String a, String b) {
        int len = Math.min(a.length(), b.length());
        for (int i = 0; i < len; i++) {
            int wa = primaryWeight(a.charAt(i));
            int wb = primaryWeight(b.charAt(i));
            if (wa != wb) {
                return Integer.compare(wa, wb);
            }
        }
        if (a.length() != b.length()) {
            return Integer.compare(a.length(), b.length());
        }
        for (int i = 0; i < len; i++) {
            char ca = a.charAt(i);
            char cb = b.charAt(i);
            if (ca != cb) {
                boolean upperA = ca >= 'A' && ca <= 'Z';
                boolean upperB = cb >= 'A' && cb <= 'Z';
                if (upperA != upperB) {
                    return upperA ? 1 : -1;
                }
                return Character.compare(ca, cb);
            }
        }
        return 0;
    }

    private static int primaryWeight(char c) {
        if (c < 128 && PRIMARY_WEIGHT[c] >= 0) {
            return PRIMARY_WEIGHT[c];
        }
        return 1000 + c;
    }

    /** JSON.stringify string escaping: short escapes, {@code \\u00xx} control chars, UTF-8 passthrough. */
    private static void writeString(String s, StringBuilder sb) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                default:
                    if (c < 0x20 ||
                            (Character.isHighSurrogate(c) && (i + 1 >= s.length() || !Character.isLowSurrogate(s.charAt(i + 1)))) ||
                            (Character.isLowSurrogate(c) && (i == 0 || !Character.isHighSurrogate(s.charAt(i - 1))))) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
    }

    /** Indent unit of the original file (whitespace run opening the second line), npm's default is two spaces. */
    static String detectIndent(String content) {
        int nl = content.indexOf('\n');
        if (nl >= 0) {
            int i = nl + 1;
            int start = i;
            while (i < content.length() && (content.charAt(i) == ' ' || content.charAt(i) == '\t')) {
                i++;
            }
            if (i > start && i < content.length()) {
                return content.substring(start, i);
            }
        }
        return "  ";
    }

    static String detectEol(String content) {
        return content.contains("\r\n") ? "\r\n" : "\n";
    }
}
