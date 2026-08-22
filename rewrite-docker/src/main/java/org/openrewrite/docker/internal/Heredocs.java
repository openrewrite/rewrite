/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.docker.internal;

/**
 * Where the body of a heredoc ends.
 * <p>
 * A heredoc opened with {@code <<-} may be closed by a delimiter line indented with tabs, so that the
 * body of one written inside an indented block reads as part of it; one opened with plain {@code <<}
 * is closed only by a line that is the delimiter and nothing else, and an indented copy of it is
 * content. The lexer decides where to leave heredoc mode by this rule and
 * {@code org.openrewrite.docker.Assertions} reads that decision back off the parsed tree, so both ask
 * it here rather than each spelling out its own idea of what closes a body.
 */
public final class Heredocs {

    private Heredocs() {
    }

    /**
     * Whether a line of a heredoc body closes it.
     *
     * @param marker the marker as written after {@code <<}, so {@code "EOF"} or {@code "-EOF"}
     * @param line   a whole line of the body, without its newline
     */
    public static boolean closes(String marker, String line) {
        String delimiter = delimiter(marker);
        if (marker.startsWith("-")) {
            int indent = 0;
            while (indent < line.length() && line.charAt(indent) == '\t') {
                indent++;
            }
            return line.substring(indent).equals(delimiter);
        }
        return line.equals(delimiter);
    }

    /**
     * The delimiter a marker names, which is the marker without its {@code -} and with the quotes taken
     * off. Docker puts the marker through the same quote removal a shell word gets, so {@code <<'EOF'},
     * {@code <<"EOF"} and {@code <<EOF} all name {@code EOF}; what the quotes say is that the body is
     * not to have its variables expanded, which is no part of the name the terminator line carries.
     *
     * @param marker the marker as written after {@code <<}, so {@code "EOF"}, {@code "-EOF"} or {@code "'EOF'"}
     */
    public static String delimiter(String marker) {
        String name = marker.startsWith("-") ? marker.substring(1) : marker;
        StringBuilder unquoted = new StringBuilder(name.length());
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c != '\'' && c != '"') {
                unquoted.append(c);
            }
        }
        return unquoted.toString();
    }
}
