/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.scala.internal;

import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TextComment;
import org.openrewrite.marker.Markers;

import java.util.ArrayList;
import java.util.List;

/**
 * Scala-aware replacement for {@link Space#format(String, int, int)}.
 * <p>
 * Unlike Java, Scala block comments nest: {@code /* outer /* inner *​/ still outer *​/} is a single
 * comment, and the first {@code *​/} only closes the inner block. The shared {@link Space#format}
 * tracks block comments with a boolean and therefore closes the comment at the first {@code *​/},
 * which corrupts both the comment text and the source that follows it on round-trip. This formatter
 * tracks the nesting depth so nested block comments are preserved.
 */
public final class ScalaSpace {

    private ScalaSpace() {
    }

    public static Space format(String formatting) {
        return format(formatting, 0, formatting.length());
    }

    public static Space format(String formatting, int beginIndex, int toIndex) {
        if (beginIndex == toIndex) {
            return Space.EMPTY;
        } else if (toIndex == beginIndex + 1 && ' ' == formatting.charAt(beginIndex)) {
            return Space.SINGLE_SPACE;
        }

        StringBuilder prefix = new StringBuilder();
        StringBuilder comment = new StringBuilder();
        List<Comment> comments = new ArrayList<>(1);

        boolean inSingleLineComment = false;
        int blockDepth = 0;

        char last = 0;

        for (int i = beginIndex; i < toIndex; i++) {
            char c = formatting.charAt(i);
            switch (c) {
                case '/':
                    if (inSingleLineComment) {
                        comment.append(c);
                    } else if (last == '/' && blockDepth == 0) {
                        inSingleLineComment = true;
                        comment.setLength(0);
                        prefix.setLength(prefix.length() - 1);
                    } else if (last == '*' && blockDepth > 0 && comment.length() > 0) {
                        if (blockDepth == 1) {
                            blockDepth = 0;
                            comment.setLength(comment.length() - 1); // trim the last '*'
                            comments.add(new TextComment(true, comment.toString(), prefix.substring(0, prefix.length() - 1), Markers.EMPTY));
                            prefix.setLength(0);
                            comment.setLength(0);
                            last = 0;
                            continue;
                        } else {
                            blockDepth--;
                            comment.append(c); // keep the nested '*/' as comment text
                        }
                    } else if (blockDepth > 0) {
                        comment.append(c);
                    } else {
                        prefix.append(c);
                    }
                    break;
                case '\r':
                case '\n':
                    if (inSingleLineComment) {
                        inSingleLineComment = false;
                        comments.add(new TextComment(false, comment.toString(), prefix.toString(), Markers.EMPTY));
                        prefix.setLength(0);
                        comment.setLength(0);
                        prefix.append(c);
                    } else if (blockDepth == 0) {
                        prefix.append(c);
                    } else {
                        comment.append(c);
                    }
                    break;
                case '*':
                    if (inSingleLineComment) {
                        comment.append(c);
                    } else if (last == '/' && blockDepth == 0) {
                        blockDepth = 1;
                        comment.setLength(0);
                    } else if (last == '/' && blockDepth > 0) {
                        blockDepth++;
                        comment.append(c); // the '/' is already in the comment buffer
                    } else {
                        comment.append(c);
                    }
                    break;
                default:
                    if (inSingleLineComment || blockDepth > 0) {
                        comment.append(c);
                    } else {
                        prefix.append(c);
                    }
            }
            last = c;
        }
        // If a file ends with a single-line comment there may be no terminating newline
        if ((comment.length() > 0 && blockDepth == 0) || inSingleLineComment) {
            comments.add(new TextComment(false, comment.toString(), prefix.toString(), Markers.EMPTY));
            prefix.setLength(0);
        }

        // Shift the whitespace on each comment forward to be a suffix of the comment before it, and the
        // whitespace on the first comment to be the whitespace of the tree element. The remaining prefix is the suffix
        // of the last comment.
        String whitespace = prefix.toString();
        if (!comments.isEmpty()) {
            for (int i = comments.size() - 1; i >= 0; i--) {
                Comment c = comments.get(i);
                String next = c.getSuffix();
                comments.set(i, c.withSuffix(whitespace));
                whitespace = next;
            }
        }

        return Space.build(whitespace, comments);
    }
}
