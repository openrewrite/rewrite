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

import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.EcmaScript;
import org.openrewrite.javascript.internal.lock.JsError;
import org.openrewrite.javascript.internal.lock.JsYaml;
import org.openrewrite.javascript.internal.lock.V8RegExp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Port of @yarnpkg/parsers sources/syml.ts and sources/grammars/syml.pegjs (Yarn 4.10.3).
 */
final class Syml {

    private static final JsYaml.Options OPTIONS = new JsYaml.Options(JsYaml.Release.V3_14_1, true, LinkedHashMap::new);

    private static final Pattern SIMPLE_STRING =
            V8RegExp.of("^(?![-?:,\\][{}#&*!|>'\"%@` \\t\\r\\n]).([ \\t]*(?![,\\][{}:# \\t\\r\\n]).)*$");
    private static final List<String> SPECIAL_OBJECT_KEYS = Arrays.asList("__metadata", "version", "resolution",
            "dependencies", "peerDependencies", "dependenciesMeta", "peerDependenciesMeta", "binaries");
    private static final Pattern LEGACY = Pattern.compile(Objects.requireNonNull(
            V8RegExp.toJava("^(#.*(\\r?\\n))*?#\\s+yarn\\s+lockfile\\s+v1\\r?\\n")), Pattern.CASE_INSENSITIVE);

    /**
     * A mapping written in its own key order ({@code stringifySyml.PreserveOrdering}).
     */
    static final class PreserveOrdering {
        final Map<String, ?> data;

        PreserveOrdering(Map<String, ?> data) {
            this.data = data;
        }
    }

    /**
     * The value {@code undefined}: a field with it is dropped, as is a mapping holding only such fields.
     */
    static final Object UNDEFINED = Js.UNDEFINED;

    private Syml() {
    }

    static String stringifySyml(Object value) {
        String stringified = stringifyValue(value, 0, false);
        return !"\n".equals(stringified) ? stringified : "";
    }

    private static String stringifyString(String value) {
        if (SIMPLE_STRING.matcher(value).find()) {
            return value;
        }
        return EcmaScript.quote(value);
    }

    private static boolean isRemovableField(@Nullable Object value) {
        if (value == UNDEFINED) {
            return true;
        }
        if (value instanceof Map || value instanceof PreserveOrdering) {
            Map<String, ?> map = value instanceof PreserveOrdering ? ((PreserveOrdering) value).data : asMap(value);
            for (Object field : map.values()) {
                if (!isRemovableField(field)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, ?> asMap(Object value) {
        return (Map<String, ?>) value;
    }

    private static String stringifyValue(@Nullable Object value, int indentLevel, boolean newLineIfObject) {
        if (value == null) {
            return "null\n";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value + "\n";
        }
        if (value instanceof String) {
            return stringifyString((String) value) + "\n";
        }
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            if (list.isEmpty()) {
                return "[]\n";
            }
            String indent = Js.repeat("  ", indentLevel);
            StringBuilder serialized = new StringBuilder();
            for (Object sub : list) {
                serialized.append(indent).append("- ").append(stringifyValue(sub, indentLevel + 1, false));
            }
            return "\n" + serialized;
        }
        if (value instanceof Map || value instanceof PreserveOrdering) {
            boolean sort = !(value instanceof PreserveOrdering);
            Map<String, ?> data = sort ? asMap(value) : ((PreserveOrdering) value).data;
            String indent = Js.repeat("  ", indentLevel);
            List<String> keys = new ArrayList<>(data.keySet());
            if (sort) {
                keys.sort((a, b) -> {
                    int aIndex = SPECIAL_OBJECT_KEYS.indexOf(a);
                    int bIndex = SPECIAL_OBJECT_KEYS.indexOf(b);
                    if (aIndex == -1 && bIndex == -1) {
                        return Integer.signum(a.compareTo(b));
                    }
                    if (aIndex != -1 && bIndex == -1) {
                        return -1;
                    }
                    if (aIndex == -1) {
                        return 1;
                    }
                    return aIndex - bIndex;
                });
            }
            StringBuilder fields = new StringBuilder();
            int index = 0;
            for (String key : keys) {
                Object field = data.get(key);
                if (isRemovableField(field)) {
                    continue;
                }
                String stringifiedKey = stringifyString(key);
                String stringifiedValue = stringifyValue(field, indentLevel + 1, true);
                String recordIndentation = index > 0 || newLineIfObject ? indent : "";
                String keyPart = stringifiedKey.length() > 1024 ?
                        "? " + stringifiedKey + "\n" + recordIndentation + ":" :
                        stringifiedKey + ":";
                String valuePart = stringifiedValue.startsWith("\n") ? stringifiedValue : " " + stringifiedValue;
                if (index > 0 && indentLevel == 0) {
                    fields.append('\n');
                }
                fields.append(recordIndentation).append(keyPart).append(valuePart);
                index++;
            }
            String joined = fields.length() == 0 ? "\n" : fields.toString();
            return newLineIfObject ? "\n" + joined : joined;
        }
        throw new JsError(JsError.ERROR, "Unsupported value type (" + value + ")");
    }

    /**
     * {@code parseSyml}: a Yarn 1 lockfile through the legacy grammar, anything else through js-yaml; an empty
     * document is an empty mapping.
     */
    @SuppressWarnings("unchecked")
    static Map<String, @Nullable Object> parseSyml(String source) {
        if (LEGACY.matcher(source).find()) {
            return parseViaPeg(source);
        }
        Object value = JsYaml.safeLoadFailsafe(source, OPTIONS);
        if (value == null) {
            return new LinkedHashMap<>();
        }
        if (value instanceof String) {
            throw new JsError(JsError.ERROR, "Expected an indexed object, got a string instead. " +
                                             "Does your file follow Yaml's rules?");
        }
        if (value instanceof List) {
            throw new JsError(JsError.ERROR, "Expected an indexed object, got an array instead. " +
                                             "Does your file follow Yaml's rules?");
        }
        return (Map<String, @Nullable Object>) value;
    }

    private static Map<String, @Nullable Object> parseViaPeg(String source) {
        if (!source.endsWith("\n")) {
            source += "\n";
        }
        return new PegParser(source).parse();
    }

    /**
     * The grammar's own error, as pegjs reports a parse that stops short.
     */
    static final class SyntaxError extends RuntimeException {
        SyntaxError(String message) {
            super(message);
        }
    }

    private static final Object FAILED = new Object();

    /**
     * The parser pegjs 0.10 generates from syml.pegjs; {@code indentLevel} is the grammar's own state, moved by
     * its {@code Indent} and {@code Dedent} predicates.
     */
    private static final class PegParser {
        private static final int INDENT_STEP = 2;

        final String input;
        int pos;
        int indentLevel;
        int maxFailPos;
        final TreeSet<String> maxFailExpected = new TreeSet<>();

        PegParser(String input) {
            this.input = input;
        }

        Map<String, @Nullable Object> parse() {
            Map<String, @Nullable Object> result = propertyStatements();
            if (pos == input.length()) {
                return result;
            }
            if (pos < input.length()) {
                fail("end of input");
            }
            String found = maxFailPos < input.length() ? EcmaScript.quote(String.valueOf(input.charAt(maxFailPos))) :
                    "end of input";
            throw new SyntaxError("Expected " + describeExpected() + " but " + found + " found.");
        }

        private String describeExpected() {
            List<String> expected = new ArrayList<>(maxFailExpected);
            if (expected.size() == 1) {
                return expected.get(0);
            }
            if (expected.size() == 2) {
                return expected.get(0) + " or " + expected.get(1);
            }
            return String.join(", ", expected.subList(0, expected.size() - 1)) + ", or " +
                   expected.get(expected.size() - 1);
        }

        private void fail(String expected) {
            if (pos < maxFailPos) {
                return;
            }
            if (pos > maxFailPos) {
                maxFailPos = pos;
                maxFailExpected.clear();
            }
            maxFailExpected.add(expected);
        }

        private int ch() {
            return pos < input.length() ? input.charAt(pos) : -1;
        }

        private boolean literal(String s) {
            if (input.startsWith(s, pos)) {
                pos += s.length();
                return true;
            }
            fail(EcmaScript.quote(s));
            return false;
        }

        // ItemStatements = ItemStatement*
        private List<@Nullable Object> itemStatements() {
            List<@Nullable Object> items = new ArrayList<>();
            while (true) {
                Object item = itemStatement();
                if (item == FAILED) {
                    return items;
                }
                items.add(item);
            }
        }

        // ItemStatement = Samedent "-" B value:Expression
        private @Nullable Object itemStatement() {
            int start = pos;
            if (samedent() && literal("-") && b()) {
                Object value = expression();
                if (value != FAILED) {
                    return value;
                }
            }
            pos = start;
            return FAILED;
        }

        // PropertyStatements = PropertyStatement*
        private Map<String, @Nullable Object> propertyStatements() {
            Map<String, @Nullable Object> properties = new LinkedHashMap<>();
            while (true) {
                Map<String, @Nullable Object> statement = propertyStatement();
                if (statement == null) {
                    return properties;
                }
                properties.putAll(statement);
            }
        }

        private @Nullable Map<String, @Nullable Object> propertyStatement() {
            int start = pos;
            // B? ("#" (!EOL .)+)? EOL_ANY+
            b();
            int commentStart = pos;
            if (literal("#")) {
                int chars = 0;
                while (pos < input.length() && !atEol()) {
                    pos++;
                    chars++;
                }
                if (chars == 0) {
                    fail("any character");
                    pos = commentStart;
                }
            }
            if (eolAny()) {
                while (eolAny()) {
                    // EOL_ANY+
                }
                return new LinkedHashMap<>();
            }
            pos = start;

            // Samedent property:Name B? ":" B? value:Expression
            if (samedent()) {
                Object property = name();
                if (property != FAILED) {
                    b();
                    if (literal(":")) {
                        b();
                        Object value = expression();
                        if (value != FAILED) {
                            return single((String) property, value);
                        }
                    }
                }
            }
            pos = start;

            // Samedent property:LegacyName B? ":" B? value:Expression
            if (samedent()) {
                Object property = legacyName();
                if (property != FAILED) {
                    b();
                    if (literal(":")) {
                        b();
                        Object value = expression();
                        if (value != FAILED) {
                            return single((String) property, value);
                        }
                    }
                }
            }
            pos = start;

            // Samedent property:LegacyName B value:LegacyLiteral EOL_ANY+
            if (samedent()) {
                Object property = legacyName();
                if (property != FAILED && b()) {
                    Object value = legacyLiteral();
                    if (value != FAILED && eolAny()) {
                        while (eolAny()) {
                            // EOL_ANY+
                        }
                        return single((String) property, value);
                    }
                }
            }
            pos = start;

            // Samedent property:LegacyName others:(B? "," B? other:LegacyName)+ B? ":" B? value:Expression
            if (samedent()) {
                Object property = legacyName();
                if (property != FAILED) {
                    List<String> names = new ArrayList<>();
                    names.add((String) property);
                    while (true) {
                        int otherStart = pos;
                        b();
                        if (literal(",")) {
                            b();
                            Object other = legacyName();
                            if (other != FAILED) {
                                names.add((String) other);
                                continue;
                            }
                        }
                        pos = otherStart;
                        break;
                    }
                    if (names.size() > 1) {
                        b();
                        if (literal(":")) {
                            b();
                            Object value = expression();
                            if (value != FAILED) {
                                Map<String, @Nullable Object> out = new LinkedHashMap<>();
                                for (String n : names) {
                                    out.put(n, value);
                                }
                                return out;
                            }
                        }
                    }
                }
            }
            pos = start;
            return null;
        }

        private static Map<String, @Nullable Object> single(String key, @Nullable Object value) {
            Map<String, @Nullable Object> out = new LinkedHashMap<>();
            out.put(key, value);
            return out;
        }

        private @Nullable Object expression() {
            int start = pos;
            // &(EOL Extradent "-" B) EOL_ANY Indent statements:ItemStatements Dedent
            boolean lookahead = eol() && extradent() && literal("-") && b();
            pos = start;
            if (lookahead && eolAny()) {
                indentLevel++;
                List<@Nullable Object> statements = itemStatements();
                indentLevel--;
                return statements;
            }
            pos = start;
            // EOL Indent statements:PropertyStatements Dedent
            if (eol()) {
                indentLevel++;
                Map<String, @Nullable Object> statements = propertyStatements();
                indentLevel--;
                return statements;
            }
            pos = start;
            // expression:Literal EOL_ANY+
            Object literal = literalValue();
            if (literal != FAILED && eolAny()) {
                while (eolAny()) {
                    // EOL_ANY+
                }
                return literal;
            }
            pos = start;
            return FAILED;
        }

        private boolean samedent() {
            int spaces = 0;
            while (ch() == ' ') {
                pos++;
                spaces++;
            }
            if (spaces == indentLevel * INDENT_STEP) {
                return true;
            }
            fail("correct indentation");
            return false;
        }

        private boolean extradent() {
            int spaces = 0;
            while (ch() == ' ') {
                pos++;
                spaces++;
            }
            return spaces == (indentLevel + 1) * INDENT_STEP;
        }

        private Object name() {
            Object s = string();
            return s != FAILED ? s : pseudostring();
        }

        private Object legacyName() {
            Object s = string();
            if (s != FAILED) {
                return s;
            }
            int start = pos;
            int count = 0;
            while (pseudostringLegacy() != FAILED) {
                count++;
            }
            if (count == 0) {
                pos = start;
                return FAILED;
            }
            return input.substring(start, pos);
        }

        private @Nullable Object literalValue() {
            if (literal("null")) {
                return null;
            }
            if (literal("true")) {
                return true;
            }
            if (literal("false")) {
                return false;
            }
            Object s = string();
            return s != FAILED ? s : pseudostring();
        }

        private @Nullable Object legacyLiteral() {
            if (literal("null")) {
                return null;
            }
            Object s = string();
            return s != FAILED ? s : pseudostringLegacy();
        }

        // [^\r\n\t ?:,\][{}#&*!|>'"%@`-] (B? [^\r\n\t ,\][{}:#"'])*
        private Object pseudostring() {
            int start = pos;
            int c = ch();
            if (c == -1 || "\r\n\t ?:,][{}#&*!|>'\"%@`-".indexOf(c) >= 0) {
                fail("pseudostring");
                return FAILED;
            }
            pos++;
            while (true) {
                int itemStart = pos;
                b();
                c = ch();
                if (c == -1 || "\r\n\t ,][{}:#\"'".indexOf(c) >= 0) {
                    pos = itemStart;
                    break;
                }
                pos++;
            }
            return trimSpaces(input.substring(start, pos));
        }

        // "--"? [a-zA-Z\/0-9] [^\r\n\t :,]*
        private Object pseudostringLegacy() {
            int start = pos;
            if (input.startsWith("--", pos)) {
                pos += 2;
            }
            int c = ch();
            if (!(('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z') || c == '/' || ('0' <= c && c <= '9'))) {
                fail("[a-zA-Z\\/0-9]");
                pos = start;
                return FAILED;
            }
            pos++;
            while (true) {
                c = ch();
                if (c == -1 || "\r\n\t :,".indexOf(c) >= 0) {
                    break;
                }
                pos++;
            }
            return trimSpaces(input.substring(start, pos));
        }

        private Object string() {
            int start = pos;
            if (input.startsWith("\"\"", pos)) {
                pos += 2;
                return "";
            }
            if (!literal("\"")) {
                fail("string");
                return FAILED;
            }
            StringBuilder chars = new StringBuilder();
            while (true) {
                int c = ch();
                if (c == -1) {
                    break;
                }
                if (c != '"' && c != '\\' && !(c <= 0x1F) && c != 0x7F) {
                    chars.append((char) c);
                    pos++;
                    continue;
                }
                if (c == '\\' && pos + 1 < input.length()) {
                    char e = input.charAt(pos + 1);
                    String simple = e == '"' ? "\"" : e == '\\' ? "\\" : e == '/' ? "/" : e == 'b' ? "\b" :
                            e == 'f' ? "\f" : e == 'n' ? "\n" : e == 'r' ? "\r" : e == 't' ? "\t" : null;
                    if (simple != null) {
                        chars.append(simple);
                        pos += 2;
                        continue;
                    }
                    if (e == 'u' && pos + 6 <= input.length() && isHex(input, pos + 2, 4)) {
                        chars.append((char) Integer.parseInt(input.substring(pos + 2, pos + 6), 16));
                        pos += 6;
                        continue;
                    }
                }
                break;
            }
            if (chars.length() == 0 || !literal("\"")) {
                pos = start;
                fail("string");
                return FAILED;
            }
            return chars.toString();
        }

        private static boolean isHex(String s, int from, int count) {
            for (int i = from; i < from + count; i++) {
                if (Character.digit(s.charAt(i), 16) < 0) {
                    return false;
                }
            }
            return true;
        }

        // B = [ \t]+
        private boolean b() {
            int start = pos;
            while (ch() == ' ' || ch() == '\t') {
                pos++;
            }
            if (pos == start) {
                fail("blank space");
                return false;
            }
            return true;
        }

        private boolean atEol() {
            int c = ch();
            return c == '\n' || c == '\r';
        }

        // EOL = "\r\n" / "\n" / "\r"
        private boolean eol() {
            if (input.startsWith("\r\n", pos)) {
                pos += 2;
                return true;
            }
            if (atEol()) {
                pos++;
                return true;
            }
            fail("\"\\n\"");
            return false;
        }

        // EOL_ANY = EOL (B? EOL)*
        private boolean eolAny() {
            if (!eol()) {
                return false;
            }
            while (true) {
                int start = pos;
                b();
                if (!eol()) {
                    pos = start;
                    return true;
                }
            }
        }

        private static String trimSpaces(String text) {
            int from = 0;
            int to = text.length();
            while (from < to && text.charAt(from) == ' ') {
                from++;
            }
            while (to > from && text.charAt(to - 1) == ' ') {
                to--;
            }
            return text.substring(from, to);
        }
    }
}
