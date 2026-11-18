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
package org.openrewrite.ruby.internal;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * How deeply a Ruby source nests brackets, string interpolation and keyword blocks, read with
 * just enough lexing to step over comments, strings, symbols, heredocs and percent literals. It
 * is an estimate for refusing pathological input before Prism sees it, not a parser.
 */
public final class NestingDepth {
    private final String s;
    private final int n;
    private int i;
    private int depth;
    private int max;

    /**
     * Whether the previous token ends an expression, which makes a following {@code if},
     * {@code unless}, {@code while} or {@code until} a modifier rather than a block.
     */
    private boolean exprEnd;

    /**
     * Set by {@code while}, {@code until} and {@code for}, whose optional {@code do} opens nothing.
     */
    private boolean loopDo;

    private final List<String[]> heredocs = new ArrayList<>();

    /**
     * One entry per open brace: {@link #BRACE}, or the literal an interpolation resumes.
     */
    private final Deque<Literal> braces = new ArrayDeque<>();

    private record Literal(char open, char close, boolean interpolates, int nested) {
    }

    private static final Literal BRACE = new Literal('{', '}', false, 0);

    private NestingDepth(String source) {
        this.s = source;
        this.n = source.length();
    }

    public static int of(String source) {
        NestingDepth d = new NestingDepth(source);
        d.code();
        return d.max;
    }

    private void open() {
        depth++;
        max = Math.max(max, depth);
    }

    private void close() {
        depth = Math.max(0, depth - 1);
    }

    private void code() {
        boolean lineStart = true;
        while (i < n) {
            char c = s.charAt(i);
            if (lineStart && c == '=' && s.startsWith("=begin", i)) {
                skipEmbeddedDocument();
                continue;
            }
            if (lineStart && c == '_' && s.startsWith("__END__", i) && (i + 7 == n || s.charAt(i + 7) == '\n' ||
                                                                         s.charAt(i + 7) == '\r')) {
                return;
            }
            lineStart = false;
            if (c == '\n') {
                i++;
                skipHeredocBodies();
                exprEnd = false;
                loopDo = false;
                lineStart = true;
            } else if (c == ' ' || c == '\t' || c == '\r' || c == '\f') {
                i++;
            } else if (c == '\\' && i + 1 < n && s.charAt(i + 1) == '\n') {
                i += 2;
            } else if (c == '#') {
                while (i < n && s.charAt(i) != '\n') {
                    i++;
                }
            } else if (c == ';') {
                i++;
                exprEnd = false;
                loopDo = false;
            } else if (c == '(' || c == '[') {
                i++;
                open();
                exprEnd = false;
            } else if (c == ')' || c == ']') {
                i++;
                close();
                exprEnd = true;
            } else if (c == '{') {
                i++;
                braces.push(BRACE);
                open();
                exprEnd = false;
            } else if (c == '}') {
                i++;
                close();
                Literal resume = braces.isEmpty() ? BRACE : braces.pop();
                if (resume == BRACE) {
                    exprEnd = true;
                } else {
                    string(resume.open(), resume.close(), resume.interpolates(), resume.nested());
                }
            } else if (c == '"' || c == '`') {
                i++;
                string(c, c, true);
            } else if (c == '\'') {
                i++;
                string(c, c, false);
            } else if (c == '/' && !exprEnd) {
                i++;
                string('/', '/', true);
            } else if (c == '%' && percentLiteral()) {
                // consumed
            } else if (c == '<' && s.startsWith("<<", i) && heredocStart()) {
                // consumed
            } else if (c == '?' && !exprEnd && i + 1 < n && !Character.isWhitespace(s.charAt(i + 1))) {
                i += s.charAt(i + 1) == '\\' ? 3 : 2;
                exprEnd = true;
            } else if (c == ':' && i + 1 < n && s.charAt(i + 1) != ':' && symbol()) {
                exprEnd = true;
            } else if (c == '@' || c == '$' || Character.isLetter(c) || c == '_' || c >= 0x80) {
                word();
            } else if (Character.isDigit(c)) {
                while (i < n && (Character.isLetterOrDigit(s.charAt(i)) || s.charAt(i) == '_' || s.charAt(i) == '.' &&
                                 i + 1 < n && Character.isDigit(s.charAt(i + 1)))) {
                    i++;
                }
                exprEnd = true;
            } else {
                // An operator: what follows starts an expression.
                if (c == ':' && i + 1 < n && s.charAt(i + 1) == ':') {
                    i += 2;
                    continue;
                }
                i++;
                exprEnd = false;
            }
        }
    }

    private void word() {
        int start = i;
        if (s.charAt(i) == '$') {
            i++;
            if (i < n && !Character.isLetter(s.charAt(i)) && s.charAt(i) != '_') {
                i++;
                exprEnd = true;
                return;
            }
        }
        while (i < n && s.charAt(i) == '@') {
            i++;
        }
        while (i < n && (Character.isLetterOrDigit(s.charAt(i)) || s.charAt(i) == '_' || s.charAt(i) >= 0x80)) {
            i++;
        }
        if (i < n && (s.charAt(i) == '?' || s.charAt(i) == '!') && (i + 1 == n || s.charAt(i + 1) != '=')) {
            i++;
        }
        String word = s.substring(start, i);
        boolean member = start > 0 && (s.charAt(start - 1) == '.' || start > 1 && s.charAt(start - 1) == ':' &&
                                                                     s.charAt(start - 2) == ':');
        boolean label = i < n && s.charAt(i) == ':' && (i + 1 == n || s.charAt(i + 1) != ':');
        if (member || label) {
            if (label) {
                i++;
            }
            exprEnd = !label;
            return;
        }
        switch (word) {
            case "if", "unless" -> {
                if (!exprEnd) {
                    open();
                }
                exprEnd = false;
            }
            case "while", "until" -> {
                if (!exprEnd) {
                    open();
                    loopDo = true;
                }
                exprEnd = false;
            }
            case "for" -> {
                open();
                loopDo = true;
                exprEnd = false;
            }
            case "case", "begin", "module", "class" -> {
                open();
                exprEnd = false;
            }
            case "def" -> {
                if (!endlessDef()) {
                    open();
                }
                exprEnd = false;
            }
            case "do" -> {
                if (loopDo) {
                    loopDo = false;
                } else {
                    open();
                }
                exprEnd = false;
            }
            case "end" -> {
                close();
                exprEnd = true;
            }
            case "and", "or", "not", "then", "else", "elsif", "when", "in", "rescue", "ensure" -> exprEnd = false;
            default -> exprEnd = true;
        }
    }

    /**
     * Steps over a method's name and parameters, and reports whether {@code =} follows them.
     */
    private boolean endlessDef() {
        int j = i;
        while (j < n && (s.charAt(j) == ' ' || s.charAt(j) == '\t')) {
            j++;
        }
        int nameStart = j;
        while (j < n && " \t\n;(".indexOf(s.charAt(j)) < 0 &&
               !(j > nameStart && s.charAt(j) == '=' && j + 1 < n && (s.charAt(j + 1) == ' ' || s.charAt(j + 1) == '\t'))) {
            j++;
        }
        if (j < n && s.charAt(j) == '(') {
            int parens = 0;
            while (j < n) {
                char c = s.charAt(j++);
                if (c == '(') {
                    parens++;
                } else if (c == ')' && --parens == 0) {
                    break;
                }
            }
        }
        i = Math.max(i, j);
        while (j < n && (s.charAt(j) == ' ' || s.charAt(j) == '\t')) {
            j++;
        }
        return j + 1 < n && s.charAt(j) == '=' && "=~>".indexOf(s.charAt(j + 1)) < 0;
    }

    private boolean symbol() {
        char next = s.charAt(i + 1);
        if (exprEnd && (next == ' ' || next == '\t')) {
            return false;
        }
        if (next == '"' || next == '\'') {
            i += 2;
            string(next, next, next == '"');
            return true;
        }
        if (Character.isLetter(next) || next == '_' || next == '@' || next == '$' || next >= 0x80) {
            i++;
            while (i < n && (Character.isLetterOrDigit(s.charAt(i)) || "_@$".indexOf(s.charAt(i)) >= 0 || s.charAt(i) >= 0x80)) {
                i++;
            }
            if (i < n && "?!=".indexOf(s.charAt(i)) >= 0 && (i + 1 == n || "=~>".indexOf(s.charAt(i + 1)) < 0)) {
                i++;
            }
            return true;
        }
        if (s.startsWith("[]", i + 1)) {
            i += s.startsWith("[]=", i + 1) ? 4 : 3;
            return true;
        }
        if ("+-*/%<>=!~^&|".indexOf(next) >= 0 && !exprEnd) {
            i++;
            while (i < n && "+-*/%<>=!~^&|@".indexOf(s.charAt(i)) >= 0) {
                i++;
            }
            return true;
        }
        return false;
    }

    private boolean percentLiteral() {
        int j = i + 1;
        if (j >= n) {
            return false;
        }
        char type = s.charAt(j);
        boolean typed = "qQwWiIrsx".indexOf(type) >= 0;
        if (typed) {
            j++;
        }
        if (j >= n) {
            return false;
        }
        char open = s.charAt(j);
        if (Character.isLetterOrDigit(open) || Character.isWhitespace(open)) {
            return false;
        }
        boolean spaced = i > 0 && (s.charAt(i - 1) == ' ' || s.charAt(i - 1) == '\t');
        if (exprEnd && !(typed && spaced) && !(spaced && open != ' ' && open != '=')) {
            return false;
        }
        char close = switch (open) {
            case '(' -> ')';
            case '[' -> ']';
            case '{' -> '}';
            case '<' -> '>';
            default -> open;
        };
        i = j + 1;
        string(open, close, !typed || "QWIrx".indexOf(type) >= 0);
        return true;
    }

    private boolean heredocStart() {
        int j = i + 2;
        boolean indented = j < n && (s.charAt(j) == '~' || s.charAt(j) == '-');
        if (indented) {
            j++;
        }
        if (j >= n) {
            return false;
        }
        char q = s.charAt(j);
        boolean quoted = q == '\'' || q == '"' || q == '`';
        boolean spaced = i > 0 && (s.charAt(i - 1) == ' ' || s.charAt(i - 1) == '\t');
        if (!quoted && !(Character.isLetter(q) || q == '_')) {
            return false;
        }
        if (!indented && !quoted && (exprEnd && !spaced || !Character.isUpperCase(q) && q != '_')) {
            return false;
        }
        if (quoted) {
            j++;
        }
        int start = j;
        while (j < n && (quoted ? s.charAt(j) != q && s.charAt(j) != '\n' : Character.isLetterOrDigit(s.charAt(j)) ||
                                                                             s.charAt(j) == '_')) {
            j++;
        }
        if (j == start) {
            return false;
        }
        heredocs.add(new String[]{s.substring(start, j), indented ? "indented" : ""});
        i = quoted && j < n ? j + 1 : j;
        exprEnd = true;
        return true;
    }

    private void skipHeredocBodies() {
        for (String[] heredoc : heredocs) {
            while (i < n) {
                int eol = s.indexOf('\n', i);
                String line = s.substring(i, eol < 0 ? n : eol);
                i = eol < 0 ? n : eol + 1;
                String candidate = heredoc[1].isEmpty() ? line : line.strip();
                if (candidate.endsWith("\r")) {
                    candidate = candidate.substring(0, candidate.length() - 1);
                }
                if (candidate.equals(heredoc[0])) {
                    break;
                }
            }
        }
        heredocs.clear();
    }

    private void skipEmbeddedDocument() {
        while (i < n) {
            int eol = s.indexOf('\n', i);
            boolean end = s.startsWith("=end", i);
            i = eol < 0 ? n : eol + 1;
            if (end) {
                return;
            }
        }
    }

    /**
     * Steps over a literal from just past its opening delimiter. An interpolation opens a level
     * and hands back to {@link #code()}, whose matching brace resumes the literal.
     */
    private void string(char open, char close, boolean interpolates) {
        string(open, close, interpolates, 0);
    }

    private void string(char open, char close, boolean interpolates, int nested) {
        while (i < n) {
            char c = s.charAt(i++);
            if (c == '\\') {
                i++;
            } else if (c == '\n' && open == '/') {
                // Taken for a regular expression, a division had nothing to close it on its line.
                i--;
                break;
            } else if (interpolates && c == '#' && i < n && s.charAt(i) == '{') {
                i++;
                braces.push(new Literal(open, close, true, nested));
                open();
                exprEnd = false;
                return;
            } else if (c == close && nested == 0) {
                break;
            } else if (open != close && c == open) {
                nested++;
            } else if (open != close && c == close) {
                nested--;
            }
        }
        if (close == '/') {
            while (i < n && Character.isLetter(s.charAt(i))) {
                i++;
            }
        }
        exprEnd = true;
    }
}
