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
package org.openrewrite.python.internal.uvlock.marker;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.openrewrite.python.internal.pep440.PythonVersion;
import org.openrewrite.python.internal.uvlock.marker.MarkerExpression.ListKey;
import org.openrewrite.python.internal.uvlock.marker.MarkerExpression.Operator;
import org.openrewrite.python.internal.uvlock.marker.MarkerExpression.Specifier;
import org.openrewrite.python.internal.uvlock.marker.MarkerExpression.StringKey;
import org.openrewrite.python.internal.uvlock.marker.MarkerExpression.VersionKey;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntPredicate;

/**
 * Reads a marker into its interner as it goes, ported from uv-pep508 {@code marker/parse.rs} (uv 0.13.4).
 * An expression uv ignores, such as a comparison of two markers, a string with {@code ~=} or an unparseable
 * version, evaluates to true.
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public final class MarkerParser {

    /** A marker uv rejects outright. */
    public static final class MarkerSyntaxException extends Exception {
        MarkerSyntaxException(String message, String input) {
            super(message + " in `" + input + "`");
        }
    }

    private final MarkerInterner interner;
    private final String input;
    private int pos;

    /** The whole input as one marker. A marker of ignored expressions only is true. */
    int parse() throws MarkerSyntaxException {
        Integer marker = parseOr();
        eatWhitespace();
        if (pos < input.length()) {
            throw error("Unexpected character '" + input.charAt(pos) + "', expected 'and', 'or' or end of input");
        }
        return marker == null ? MarkerInterner.TRUE : marker;
    }

    // ---- cursor

    private void eatWhitespace() {
        while (pos < input.length() && Character.isWhitespace(input.charAt(pos))) {
            pos++;
        }
    }

    private String takeWhile(IntPredicate p) {
        int start = pos;
        while (pos < input.length() && p.test(input.charAt(pos))) {
            pos++;
        }
        return input.substring(start, pos);
    }

    private String peekWhile(IntPredicate p) {
        int end = pos;
        while (end < input.length() && p.test(input.charAt(end))) {
            end++;
        }
        return input.substring(pos, end);
    }

    private void expectChar(char c) throws MarkerSyntaxException {
        if (pos >= input.length()) {
            throw error("Expected '" + c + "', found end of input");
        }
        if (input.charAt(pos) != c) {
            throw error("Expected '" + c + "', found '" + input.charAt(pos) + "'");
        }
        pos++;
    }

    private MarkerSyntaxException error(String message) {
        return new MarkerSyntaxException(message, input);
    }

    // ---- grammar

    /** marker_or = marker_and ('or' marker_and)* */
    private @Nullable Integer parseOr() throws MarkerSyntaxException {
        return parseOp("or", true);
    }

    /** marker_and = marker_expr ('and' marker_expr)* */
    private @Nullable Integer parseAnd() throws MarkerSyntaxException {
        return parseOp("and", false);
    }

    private @Nullable Integer parseOp(String op, boolean or) throws MarkerSyntaxException {
        Integer first = or ? parseAnd() : parseExpr();
        eatWhitespace();
        if (!peekWhile(c -> !Character.isWhitespace(c) && c != '(').equals(op)) {
            return first;
        }
        List<Integer> expressions = new ArrayList<>();
        if (first != null) {
            expressions.add(first);
        }
        while (true) {
            takeWhile(c -> !Character.isWhitespace(c) && c != '(');
            Integer next = or ? parseAnd() : parseExpr();
            if (next != null) {
                expressions.add(next);
            }
            eatWhitespace();
            if (!peekWhile(c -> !Character.isWhitespace(c) && c != '(').equals(op)) {
                break;
            }
        }
        // adjacent pairs combine, so no prefix of a growing diagram is built
        while (expressions.size() > 1) {
            List<Integer> combined = new ArrayList<>();
            for (int i = 0; i < expressions.size(); i += 2) {
                int a = expressions.get(i);
                int result = i + 1 == expressions.size() ? a :
                        or ? interner.or(a, expressions.get(i + 1)) : interner.and(a, expressions.get(i + 1));
                if (or && result == MarkerInterner.TRUE || !or && result == MarkerInterner.FALSE) {
                    return result;
                }
                combined.add(result);
            }
            expressions = combined;
        }
        return expressions.isEmpty() ? null : expressions.get(0);
    }

    /** marker_expr = marker_var marker_op marker_var | '(' marker ')' */
    private @Nullable Integer parseExpr() throws MarkerSyntaxException {
        eatWhitespace();
        if (pos < input.length() && input.charAt(pos) == '(') {
            pos++;
            Integer marker = parseOr();
            eatWhitespace();
            expectChar(')');
            return marker;
        }
        MarkerExpression expression = parseKeyOpValue();
        return expression == null ? null : interner.expression(expression);
    }

    /** A value on either side, which is a quoted string or an environment key. */
    @RequiredArgsConstructor(access = AccessLevel.PACKAGE)
    private static final class Value {
        final @Nullable String quoted;
        final @Nullable VersionKey version;
        final @Nullable StringKey string;
        final @Nullable ListKey list;
        final boolean extra;

        @Override
        public String toString() {
            return quoted != null ? "'" + quoted + "'" : version != null ? version.display : string != null
                    ? string.display :
                    list != null ? list.display : "extra";
        }
    }

    private Value parseValue() throws MarkerSyntaxException {
        if (pos >= input.length()) {
            throw error("Expected marker value, found end of dependency specification");
        }
        char c = input.charAt(pos);
        if (c == '"' || c == '\'') {
            pos++;
            String value = takeWhile(ch -> ch != c);
            expectChar(c);
            return new Value(value, null, null, null, false);
        }
        String key = takeWhile(ch -> !Character.isWhitespace(ch) && ">=<!~)".indexOf(ch) < 0);
        switch (key) {
            case "implementation_name": return new Value(null, null, StringKey.IMPLEMENTATION_NAME, null, false);
            case "implementation_version": return new Value(null, VersionKey.IMPLEMENTATION_VERSION, null, null, false);
            case "os_name": return new Value(null, null, StringKey.OS_NAME, null, false);
            case "os.name": return deprecated(StringKey.OS_NAME_DEPRECATED, "os.name");
            case "platform_machine": return new Value(null, null, StringKey.PLATFORM_MACHINE, null, false);
            case "platform.machine": return deprecated(StringKey.PLATFORM_MACHINE_DEPRECATED, "platform.machine");
            case "platform_python_implementation": return new Value(null, null,
                    StringKey.PLATFORM_PYTHON_IMPLEMENTATION, null, false);
            case "platform.python_implementation":
                return deprecated(StringKey.PLATFORM_PYTHON_IMPLEMENTATION_DEPRECATED,
                        "platform.python_implementation");
            case "python_implementation": return deprecated(StringKey.PYTHON_IMPLEMENTATION_DEPRECATED,
                    "python_implementation");
            case "platform_release": return new Value(null, null, StringKey.PLATFORM_RELEASE, null, false);
            case "platform_system": return new Value(null, null, StringKey.PLATFORM_SYSTEM, null, false);
            case "platform_version": return new Value(null, null, StringKey.PLATFORM_VERSION, null, false);
            case "platform.version": return deprecated(StringKey.PLATFORM_VERSION_DEPRECATED, "platform.version");
            case "python_full_version": return new Value(null, VersionKey.PYTHON_FULL_VERSION, null, null, false);
            case "python_version": return new Value(null, VersionKey.PYTHON_VERSION, null, null, false);
            case "sys_platform": return new Value(null, null, StringKey.SYS_PLATFORM, null, false);
            case "sys.platform": return deprecated(StringKey.SYS_PLATFORM_DEPRECATED, "sys.platform");
            case "extras": return new Value(null, null, null, ListKey.EXTRAS, false);
            case "dependency_groups": return new Value(null, null, null, ListKey.DEPENDENCY_GROUPS, false);
            case "extra": return new Value(null, null, null, null, true);
            default: throw error("Expected a quoted string or a valid marker name, found `" + key + "`");
        }
    }

    // uv accepts the dotted spelling with a deprecation warning
    private Value deprecated(StringKey key, String name) {
        return new Value(null, null, key, null, false);
    }

    private Operator parseOperator() throws MarkerSyntaxException {
        String operator = pos < input.length() && Character.isLetter(input.charAt(pos)) ?
                takeWhile(c -> !Character.isWhitespace(c) && c != '\'' && c != '"') :
                takeWhile(c -> "<=>~!".indexOf(c) >= 0);
        if ("not".equals(operator)) {
            if (pos >= input.length()) {
                throw error("Expected whitespace after 'not', found end of input");
            }
            if (!Character.isWhitespace(input.charAt(pos))) {
                throw error("Expected whitespace after `not`, found `" + input.charAt(pos) + "`");
            }
            pos++;
            eatWhitespace();
            expectChar('i');
            expectChar('n');
            return Operator.NOT_IN;
        }
        Operator op = Operator.parse(operator);
        if (op == null) {
            throw error("Expected a valid marker operator (such as `>=` or `not in`), found `" + operator + "`");
        }
        return op;
    }

    private @Nullable MarkerExpression parseKeyOpValue() throws MarkerSyntaxException {
        eatWhitespace();
        Value l = parseValue();
        eatWhitespace();
        Operator operator = parseOperator();
        eatWhitespace();
        Value r = parseValue();
        if (l.version != null) {
            if (r.quoted == null) {
                return ignore("Expected double quoted PEP 440 version to compare with " + l.version.display
                        + ", found " + r);
            }
            MarkerExpression in = parseVersionIn(l.version, operator, r.quoted);
            if (in != null || operator == Operator.IN || operator == Operator.NOT_IN) {
                return in;
            }
            return parseVersion(l.version, operator, r.quoted);
        }
        if (l.string != null) {
            if (r.quoted == null) {
                return ignore("Comparing two markers with each other doesn't make any sense");
            }
            if (operator == Operator.TILDE_EQUAL) {
                return ignore("Can't compare strings with `~=`");
            }
            return new MarkerExpression.Str(l.string, operator, r.quoted);
        }
        if (l.list != null) {
            throw error("The marker " + l.list.display + " must be on the right hand side of the expression");
        }
        if (l.extra) {
            if (r.quoted == null) {
                return ignore("Comparing extra with something other than a quoted string is wrong");
            }
            return parseExtra(operator, r.quoted);
        }
        String lString = l.quoted;
        assert lString != null;
        if (r.version != null) {
            return parseInvertedVersion(lString, operator, r.version);
        }
        if (r.string != null) {
            if (operator == Operator.TILDE_EQUAL) {
                return ignore("Can't compare strings with `~=`");
            }
            return new MarkerExpression.Str(r.string, operator.invert(), lString);
        }
        if (r.list != null) {
            if (operator != Operator.IN && operator != Operator.NOT_IN) {
                throw error("The operator " + operator.display + " is not supported with the marker " + r.list.display +
                        ", only the `in` and `not in` operators are supported");
            }
            String name = MarkerExpression.normalizeName(lString);
            MarkerExpression.ListPair.Kind kind;
            if (name == null) {
                kind = MarkerExpression.ListPair.Kind.ARBITRARY;
                name = lString;
            } else {
                kind = r.list == ListKey.EXTRAS ? MarkerExpression.ListPair.Kind.EXTRAS
                        : MarkerExpression.ListPair.Kind.DEPENDENCY_GROUP;
            }
            return new MarkerExpression.ListPair(kind, r.list, name, operator == Operator.NOT_IN);
        }
        if (r.extra) {
            return parseExtra(operator, lString);
        }
        return ignore("Comparing two quoted strings with each other doesn't make sense");
    }

    /** uv warns with the reason and treats the expression as true, which an absent expression is. */
    private @Nullable MarkerExpression ignore(String reason) {
        return null;
    }

    private @Nullable MarkerExpression parseVersionIn(VersionKey key, Operator operator, String value) {
        if (operator != Operator.IN && operator != Operator.NOT_IN) {
            return null;
        }
        List<PythonVersion> versions = new ArrayList<>();
        for (String part : value.trim().split("\\s+")) {
            if (part.isEmpty()) {
                continue;
            }
            PythonVersion version = PythonVersion.parse(part);
            if (version == null || !MarkerExpression.fitsReleaseArithmetic(version)) {
                return ignore("Expected PEP 440 versions to compare with " + key.display + ", found " + value);
            }
            versions.add(version);
        }
        return new MarkerExpression.VersionIn(key, versions, operator == Operator.NOT_IN);
    }

    private @Nullable MarkerExpression parseVersion(VersionKey key, Operator operator, String value) {
        Specifier.Op op = operator.toPep440();
        if (op == null) {
            return ignore("Expected PEP 440 version operator to compare " + key.display + " with `" + value
                    + "`, found `" + operator.display + "`");
        }
        Specifier specifier = Specifier.fromPattern(op, value);
        if (specifier == null) {
            return ignore("Expected PEP 440 version to compare with " + key.display + ", found " + value);
        }
        return new MarkerExpression.Version(key, specifier);
    }

    private @Nullable MarkerExpression parseInvertedVersion(String value, Operator operator, VersionKey key) {
        Operator inverted = operator.invert();
        PythonVersion version = PythonVersion.parse(value);
        if (version == null) {
            return ignore("Expected PEP 440 version to compare with " + key.display + ", found " + value);
        }
        Specifier.Op op = inverted.toPep440();
        if (op == null) {
            return ignore("Expected PEP 440 version operator to compare " + key.display + " with `" + value
                    + "`, found `" + inverted.display + "`");
        }
        Specifier specifier = Specifier.fromVersion(op, version);
        if (specifier == null) {
            return ignore("Invalid operator/version combination: " + value);
        }
        return new MarkerExpression.Version(key, specifier);
    }

    private @Nullable MarkerExpression parseExtra(Operator operator, String value) {
        String name = MarkerExpression.normalizeName(value);
        if (name == null) {
        }
        if (operator != Operator.EQUAL && operator != Operator.NOT_EQUAL) {
            return ignore("Comparing `extra` with any operator other than `==` or `!=` is wrong and will be ignored");
        }
        return new MarkerExpression.Extra(name != null ? name : value, name != null, operator == Operator.NOT_EQUAL);
    }
}
