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
package org.openrewrite.golang.internal.modgraph;

import java.util.ArrayList;
import java.util.List;

/**
 * Extracts the import paths from a Go source file — the Java equivalent of the
 * resolver's previous use of {@code go/parser} in imports-only mode. Imports in
 * Go appear at the top of a file, after the {@code package} clause and before
 * any other top-level declaration, so the scanner reads the package clause, then
 * {@code import} declarations (single or {@code ( ... )} block, with optional
 * {@code _}/{@code .}/alias), and stops at the first non-import declaration.
 * Comments and string/raw-string literals are skipped so tokens inside them are
 * never mistaken for imports.
 */
public final class GoImports {

    private final String s;
    private int i;

    private GoImports(String src) {
        this.s = src;
    }

    public static List<String> parse(String src) {
        return new GoImports(src).scan();
    }

    private List<String> scan() {
        List<String> out = new ArrayList<>();
        // Skip the package clause: 'package <name>'.
        skipSpaceAndComments();
        if (matchWord("package")) {
            skipSpaceAndComments();
            readIdent();
        }
        while (true) {
            skipSpaceAndComments();
            if (atEnd() || !matchWord("import")) {
                break; // first non-import top-level declaration ends the import section
            }
            skipSpaceAndComments();
            if (!atEnd() && peek() == '(') {
                i++; // consume '('
                while (true) {
                    skipSpaceAndComments();
                    if (atEnd() || peek() == ')') {
                        if (!atEnd()) {
                            i++;
                        }
                        break;
                    }
                    readImportSpec(out);
                }
            } else {
                readImportSpec(out);
            }
        }
        return out;
    }

    // An import spec is an optional name (_, ., or identifier) followed by the
    // quoted import path.
    private void readImportSpec(List<String> out) {
        if (atEnd()) {
            return;
        }
        char c = peek();
        if (c == '"') {
            out.add(readString());
            return;
        }
        if (c == '.' || c == '_' || isIdentStart(c)) {
            if (c == '.') {
                i++;
            } else {
                readIdent();
            }
            skipSpaceAndComments();
            if (!atEnd() && peek() == '"') {
                out.add(readString());
            }
            return;
        }
        i++; // unexpected character; skip defensively
    }

    private void skipSpaceAndComments() {
        while (!atEnd()) {
            char c = peek();
            if (Character.isWhitespace(c)) {
                i++;
            } else if (c == '/' && i + 1 < s.length() && s.charAt(i + 1) == '/') {
                while (!atEnd() && peek() != '\n') {
                    i++;
                }
            } else if (c == '/' && i + 1 < s.length() && s.charAt(i + 1) == '*') {
                i += 2;
                while (i + 1 < s.length() && !(s.charAt(i) == '*' && s.charAt(i + 1) == '/')) {
                    i++;
                }
                i = Math.min(i + 2, s.length());
            } else {
                return;
            }
        }
    }

    // Reads a double-quoted Go string literal (peek == '"'), returning its
    // unescaped content. Import paths only ever use the basic escapes.
    private String readString() {
        i++; // consume opening quote
        StringBuilder sb = new StringBuilder();
        while (!atEnd()) {
            char c = s.charAt(i++);
            if (c == '\\' && !atEnd()) {
                char n = s.charAt(i++);
                switch (n) {
                    case 'n': sb.append('\n'); break;
                    case 't': sb.append('\t'); break;
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    default: sb.append(n); break;
                }
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private void readIdent() {
        while (!atEnd() && isIdentPart(peek())) {
            i++;
        }
    }

    // Consumes the word if it matches exactly (and is not part of a longer
    // identifier), returning whether it matched.
    private boolean matchWord(String w) {
        if (s.startsWith(w, i)) {
            int end = i + w.length();
            if (end >= s.length() || !isIdentPart(s.charAt(end))) {
                i = end;
                return true;
            }
        }
        return false;
    }

    private char peek() {
        return s.charAt(i);
    }

    private boolean atEnd() {
        return i >= s.length();
    }

    private static boolean isIdentStart(char c) {
        return Character.isLetter(c) || c == '_';
    }

    private static boolean isIdentPart(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }
}
