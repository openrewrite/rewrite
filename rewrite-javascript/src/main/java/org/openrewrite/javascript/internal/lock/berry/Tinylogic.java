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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Port of tinylogic 2.0.0 (index.js, grammar.pegjs and the pegjs 0.10 parser it generates), which structUtils'
 * {@code isPackageCompatible} evaluates conditions with.
 */
final class Tinylogic {

    private static final String TOKEN_CLASS = "[^ \\t\\n\\r()!|&\\^]";

    private final String input;
    private final Pattern queryPattern;
    private final Predicate<String> checkFn;

    private int pos;
    private int maxFailPos;
    private final List<String> maxFailExpected = new ArrayList<>();

    private Tinylogic(String input, Pattern queryPattern, Predicate<String> checkFn) {
        this.input = input;
        this.queryPattern = queryPattern;
        this.checkFn = checkFn;
    }

    /**
     * {@code makeParser(queryPattern)(str, checkFn)}; a string the grammar rejects is pegjs's SyntaxError.
     */
    static boolean parse(String str, Pattern queryPattern, Predicate<String> checkFn) {
        Tinylogic parser = new Tinylogic(str, queryPattern, checkFn);
        Boolean result = parser.expression();
        if (result != null && parser.pos == str.length()) {
            return result;
        }
        if (result != null) {
            parser.fail("end of input");
        }
        throw new JsError(JsError.SYNTAX_ERROR, "Expected " + parser.describeExpected() + " but " +
                                                (parser.maxFailPos < str.length() ?
                                                        "\"" + literalEscape(str.charAt(parser.maxFailPos)) + "\"" :
                                                        "end of input") + " found.");
    }

    // Expression = head:Term tail:(_ ("|" / "&" / "^") _ Term)*
    private Boolean expression() {
        Boolean result = term();
        if (result == null) {
            return null;
        }
        while (true) {
            int start = pos;
            whitespace();
            char op;
            if (literal('|')) {
                op = '|';
            } else if (literal('&')) {
                op = '&';
            } else if (literal('^')) {
                op = '^';
            } else {
                pos = start;
                break;
            }
            whitespace();
            Boolean next = term();
            if (next == null) {
                pos = start;
                break;
            }
            result = op == '|' ? result | next : op == '&' ? result & next : result ^ next;
        }
        return result;
    }

    // Term = "!" term:Term / "(" _ expr:Expression _ ")" / Token
    private Boolean term() {
        int start = pos;
        if (literal('!')) {
            Boolean term = term();
            if (term != null) {
                return !term;
            }
            pos = start;
        }
        if (literal('(')) {
            whitespace();
            Boolean expr = expression();
            if (expr != null) {
                whitespace();
                if (literal(')')) {
                    return expr;
                }
            }
            pos = start;
        }
        return token();
    }

    // Token = _ token:$[^ \t\n\r()!|&^]+ &{ queryPattern.test(token) } { checkFn(token) }
    private Boolean token() {
        int start = pos;
        whitespace();
        int tokenStart = pos;
        while (pos < input.length() && " \t\n\r()!|&^".indexOf(input.charAt(pos)) < 0) {
            pos++;
        }
        fail(TOKEN_CLASS);
        String token = input.substring(tokenStart, pos);
        if (token.isEmpty() || !queryPattern.matcher(token).find()) {
            pos = start;
            return null;
        }
        return checkFn.test(token);
    }

    // _ "whitespace" = [ \t\n\r]*, named, so its own failures are silent
    private void whitespace() {
        while (pos < input.length() && " \t\n\r".indexOf(input.charAt(pos)) >= 0) {
            pos++;
        }
    }

    private boolean literal(char c) {
        if (pos < input.length() && input.charAt(pos) == c) {
            pos++;
            return true;
        }
        fail("\"" + literalEscape(c) + "\"");
        return false;
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

    private String describeExpected() {
        List<String> descriptions = new ArrayList<>(maxFailExpected);
        Collections.sort(descriptions);
        List<String> unique = new ArrayList<>();
        for (String description : descriptions) {
            if (unique.isEmpty() || !unique.get(unique.size() - 1).equals(description)) {
                unique.add(description);
            }
        }
        if (unique.size() == 1) {
            return unique.get(0);
        }
        if (unique.size() == 2) {
            return unique.get(0) + " or " + unique.get(1);
        }
        return String.join(", ", unique.subList(0, unique.size() - 1)) + ", or " + unique.get(unique.size() - 1);
    }

    private static String literalEscape(char c) {
        switch (c) {
            case '\\':
                return "\\\\";
            case '"':
                return "\\\"";
            case '\0':
                return "\\0";
            case '\t':
                return "\\t";
            case '\n':
                return "\\n";
            case '\r':
                return "\\r";
            default:
                if (c <= 0x0F) {
                    return "\\x0" + Integer.toHexString(c).toUpperCase();
                }
                if (c <= 0x1F || c >= 0x7F && c <= 0x9F) {
                    return "\\x" + Integer.toHexString(c).toUpperCase();
                }
                return String.valueOf(c);
        }
    }
}
