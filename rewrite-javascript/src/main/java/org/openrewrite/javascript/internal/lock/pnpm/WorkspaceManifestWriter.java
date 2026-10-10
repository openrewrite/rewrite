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

import java.util.List;
import java.util.regex.Pattern;

/**
 * Port of @pnpm/workspace.workspace-manifest-writer src/index.ts (pnpm 11.2.2, the release's writer otherwise): where
 * it puts a new key and its blank line, and how its yaml writes a new scalar ({@code singleQuote: true}).
 */
public final class WorkspaceManifestWriter {

    private static final Pattern PLAIN_UNSAFE = Pattern.compile(
            "^[\\n\\t ,\\[\\]{}#&*!|>'\"%@`]|^[?-]$|^[?-][ \\t]|[\\n:][ \\t]|[ \\t]\\n|[\\n\\t ]#|[\\n\\t :]$");
    private static final Pattern CORE_SCHEMA = Pattern.compile(
            "^(?:~|[Nn]ull|NULL)?$|^(?:[Tt]rue|TRUE|[Ff]alse|FALSE)$|^0o[0-7]+$|^[-+]?[0-9]+$|^0x[0-9a-fA-F]+$|" +
            "^(?:[-+]?\\.(?:inf|Inf|INF)|\\.nan|\\.NaN|\\.NAN)$|" +
            "^[-+]?(?:\\.[0-9]+|[0-9]+(?:\\.[0-9]*)?)[eE][-+]?[0-9]+$|^[-+]?(?:\\.[0-9]+|[0-9]+\\.[0-9]*)$");

    private final int release;

    /**
     * The writer of pnpm {@code release} (a major): 11 keeps an unsorted level's order, 10 sorts every level.
     */
    public WorkspaceManifestWriter(int release) {
        this.release = release;
    }

    /**
     * {@code reorderRecursive}: the index among a level's {@code keys} a new {@code key} takes, sorted in where the
     * keys are sorted (packages first or not) and last otherwise; pnpm 10 sorts every level, packages first.
     */
    public int insertionIndex(List<String> keys, String key) {
        if (release >= 11 && !sorted(keys)) {
            return keys.size();
        }
        boolean packagesFirst = release < 11 || keys.isEmpty() || "packages".equals(keys.get(0));
        int at = 0;
        for (String existing : keys) {
            if (compare(existing, key, packagesFirst) < 0) {
                at++;
            }
        }
        return at;
    }

    /**
     * {@code propagateBlankLinesToNewPairs}: whether a new top-level pair inserted at {@code at} gets a blank line
     * before it, given whether each existing pair has one.
     */
    public boolean blankBefore(List<Boolean> blankBefore, int at) {
        if (release < 11 || at == 0) {
            return false;
        }
        return blankLineStyle(blankBefore) ||
               (at < blankBefore.size() ? blankBefore.get(at) : blankBefore.get(at - 1));
    }

    /**
     * Whether the document separates its top-level pairs by blank lines, read from every pair after the first, so
     * that a pair a new one displaces from the front gets one too.
     */
    public boolean blankLineStyle(List<Boolean> blankBefore) {
        if (release < 11 || blankBefore.size() < 2) {
            return false;
        }
        for (int i = 1; i < blankBefore.size(); i++) {
            if (!blankBefore.get(i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * How the yaml library writes {@code value} as a new scalar: plain where it reads back as that string, else
     * single quoted, double quoted when it holds only a single quote; refused where it would need an escape.
     */
    public static String scalar(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c < 0x20 || c >= 0x7f && c <= 0x9f || c == 0x2028 || c == 0x2029) {
                throw new IllegalArgumentException("pnpm-workspace.yaml would need an escape for " + value);
            }
        }
        if (PLAIN_UNSAFE.matcher(value).find() || CORE_SCHEMA.matcher(value).find()) {
            return value.contains("'") && !value.contains("\"") ? doubleQuoted(value) :
                    "'" + value.replace("'", "''") + "'";
        }
        return value;
    }

    public static String doubleQuoted(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    // detectKeyLayout: alphabetical after an optional leading `packages`, by lexCompare
    private static boolean sorted(List<String> keys) {
        int start = !keys.isEmpty() && "packages".equals(keys.get(0)) ? 1 : 0;
        for (int i = start + 1; i < keys.size(); i++) {
            if (KeySorting.lexCompare(keys.get(i - 1), keys.get(i)) > 0) {
                return false;
            }
        }
        return true;
    }

    private static int compare(String left, String right, boolean packagesFirst) {
        if (packagesFirst && "packages".equals(left) != "packages".equals(right)) {
            return "packages".equals(left) ? -1 : 1;
        }
        return KeySorting.lexCompare(left, right);
    }
}
