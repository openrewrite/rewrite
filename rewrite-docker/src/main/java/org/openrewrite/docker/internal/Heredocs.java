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
 * Where the body of a heredoc ends. A {@code <<-} heredoc may be closed by a delimiter line indented
 * with tabs; a plain {@code <<} one only by a line that is the delimiter and nothing else. Both the
 * lexer and {@code org.openrewrite.docker.Assertions} ask here rather than each spelling out its own
 * idea of what closes a body.
 */
public final class Heredocs {

    private Heredocs() {
    }

    /// @param marker the marker as written after `<<`, so `EOF` or `-EOF`
    /// @param line   a whole line of the body, without its newline
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

    /// The marker without its `-` and with the quotes taken off, so `<<'EOF'`, `<<"EOF"` and `<<EOF`
    /// all name `EOF`. Quote removal is what a shell does, so only the quote that opened a run of them
    /// closes it and one of the other kind inside that run is part of the name: `<<"it's"` names `it's`.
    public static String delimiter(String marker) {
        String name = marker.startsWith("-") ? marker.substring(1) : marker;
        StringBuilder unquoted = new StringBuilder(name.length());
        char openQuote = 0;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == openQuote) {
                openQuote = 0;
            } else if (openQuote == 0 && (c == '\'' || c == '"')) {
                openQuote = c;
            } else {
                unquoted.append(c);
            }
        }
        return unquoted.toString();
    }
}
