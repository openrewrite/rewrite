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

/**
 * Module path/version escaping for the GOPROXY URL scheme and the on-disk module
 * cache, ported from {@code golang.org/x/mod/module}. To keep case-insensitive
 * filesystems and URLs unambiguous, each uppercase ASCII letter is replaced with
 * {@code !} followed by its lowercase form (e.g. {@code github.com/Azure} ->
 * {@code github.com/!azure}). Inputs here come from parsed go.mod / build-list
 * data, so they are already well-formed; only the escaping transform is needed.
 */
public final class ModulePath {

    private ModulePath() {
    }

    /** Escapes a module path for use in {@code <proxy>/<esc-path>/@v/...}. */
    public static String escapePath(String path) {
        return escape(path);
    }

    /** Escapes a module version for use in {@code .../@v/<esc-version>.mod}. */
    public static String escapeVersion(String version) {
        return escape(version);
    }

    private static String escape(String s) {
        boolean haveUpper = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ('A' <= c && c <= 'Z') {
                haveUpper = true;
                break;
            }
        }
        if (!haveUpper) {
            return s;
        }
        StringBuilder buf = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ('A' <= c && c <= 'Z') {
                buf.append('!').append((char) (c + ('a' - 'A')));
            } else {
                buf.append(c);
            }
        }
        return buf.toString();
    }
}
