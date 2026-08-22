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

import org.openrewrite.docker.tree.Comment;
import org.openrewrite.docker.tree.Docker;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the parser directives at the head of a Dockerfile back out of the tree. The lexer acts on a
 * directive as it reads, so no LST type carries what one declared; a directive is in the tree only as
 * the comment it is written as.
 */
public class ParserDirectives {
    private static final Pattern DIRECTIVE = Pattern.compile("#[ \t]*([A-Za-z_]+)[ \t]*=[ \t]*(.*)");

    private ParserDirectives() {
    }

    /// The escape character `file` declares, which is a backslash unless an `escape` directive names the
    /// backtick instead. Docker takes only those two and fails the build on anything else, so a value
    /// that is neither leaves the file on the default.
    public static char escapeChar(Docker.File file) {
        char escapeChar = '\\';
        List<Comment> comments = headComments(file);
        for (int i = 0; i < comments.size(); i++) {
            Comment comment = comments.get(i);
            if (newlines(comment.getPrefix()) != (i == 0 ? 0 : 1)) {
                break;
            }
            Matcher directive = DIRECTIVE.matcher(comment.getText());
            if (!directive.matches()) {
                break;
            }
            String value = directive.group(2).trim();
            if ("escape".equalsIgnoreCase(directive.group(1)) && value.length() == 1 &&
                    (value.charAt(0) == '\\' || value.charAt(0) == '`')) {
                escapeChar = value.charAt(0);
            }
        }
        return escapeChar;
    }

    /// A directive stands before anything else in the file, so it is written in the prefix of whatever the
    /// file opens with. [Docker.File#getPrefix()] is not that place; it is empty.
    private static List<Comment> headComments(Docker.File file) {
        if (!file.getGlobalArgs().isEmpty()) {
            return file.getGlobalArgs().get(0).getPrefix().getComments();
        }
        if (!file.getStages().isEmpty()) {
            return file.getStages().get(0).getPrefix().getComments();
        }
        return file.getEof().getComments();
    }

    /// Directives run from the first line of the file with nothing between them, which is what tells one
    /// from a comment that happens to be written the same way. A blank line, or anything else that is not
    /// a directive, ends the head of the file for good.
    private static int newlines(String prefix) {
        int newlines = 0;
        for (int i = 0; i < prefix.length(); i++) {
            if (prefix.charAt(i) == '\n') {
                newlines++;
            }
        }
        return newlines;
    }
}
