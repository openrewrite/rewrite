/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.graphql.tree;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.*;
import org.jspecify.annotations.Nullable;
import org.openrewrite.marker.Markers;

import java.util.*;

@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@ref")
@Getter
public class Space {
    public static final Space EMPTY = new Space("", Collections.emptyList());
    public static final Space SINGLE_SPACE = new Space(" ", Collections.emptyList());

    private final String whitespace;
    private final List<Comment> comments;

    private Space(String whitespace, List<Comment> comments) {
        this.whitespace = whitespace;
        this.comments = comments;
    }

    @JsonCreator
    public static Space build(@Nullable String whitespace, List<Comment> comments) {
        if (comments.isEmpty()) {
            if (whitespace == null || whitespace.isEmpty()) {
                return Space.EMPTY;
            } else if (whitespace.length() == 1 && whitespace.charAt(0) == ' ') {
                return Space.SINGLE_SPACE;
            }
        }
        return new Space(whitespace == null ? "" : whitespace, comments);
    }

    public Space withComments(List<Comment> comments) {
        if (comments.isEmpty() && whitespace.isEmpty()) {
            return Space.EMPTY;
        }
        if (comments.isEmpty() && whitespace.equals(" ")) {
            return Space.SINGLE_SPACE;
        }
        return build(whitespace, comments);
    }

    public Space withWhitespace(String whitespace) {
        if (comments.isEmpty() && (whitespace.isEmpty() || whitespace.equals(" "))) {
            return whitespace.isEmpty() ? EMPTY : SINGLE_SPACE;
        }

        if (!whitespace.equals(this.whitespace)) {
            return build(whitespace, comments);
        }
        return this;
    }

    public boolean isEmpty() {
        return this == EMPTY || (whitespace.isEmpty() && comments.isEmpty());
    }

    public static Space format(String formatting) {
        return format(formatting, 0, formatting.length());
    }

    public static Space format(String formatting, int beginIndex, int toIndex) {
        if (beginIndex == toIndex) {
            return Space.EMPTY;
        } else if (beginIndex == toIndex - 1 && formatting.charAt(beginIndex) == ' ') {
            return Space.SINGLE_SPACE;
        }

        char[] charArray = formatting.toCharArray();
        List<Comment> comments = new ArrayList<>();
        StringBuilder whitespace = new StringBuilder();

        for (int i = beginIndex; i < toIndex; i++) {
            char c = charArray[i];
            if (c == '#') {
                int end = i;
                for (; end < toIndex && charArray[end] != '\n' && charArray[end] != '\r'; end++) {
                }
                
                String commentText = formatting.substring(i, end);
                String suffix = "";
                
                // Capture the newline as the comment suffix
                if (end < toIndex) {
                    if (charArray[end] == '\r' && end + 1 < toIndex && charArray[end + 1] == '\n') {
                        suffix = "\r\n";
                        end += 2;
                    } else if (charArray[end] == '\n' || charArray[end] == '\r') {
                        suffix = String.valueOf(charArray[end]);
                        end++;
                    }
                }
                
                comments.add(new Comment(false, commentText, suffix, Markers.EMPTY));
                
                i = end - 1;
            } else {
                whitespace.append(c);
            }
        }

        return Space.build(whitespace.toString(), comments);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Comment comment : comments) {
            sb.append(comment.printComment());
            sb.append(comment.getSuffix());
        }
        sb.append(whitespace);
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Space space = (Space) o;
        return Objects.equals(whitespace, space.whitespace) && Objects.equals(comments, space.comments);
    }

    @Override
    public int hashCode() {
        return Objects.hash(whitespace, comments);
    }

    public static String indent(String whitespace, int shiftWidth) {
        if (shiftWidth <= 0) {
            return whitespace;
        }

        StringBuilder newWhitespace = new StringBuilder(whitespace);
        for (int i = 0; i < shiftWidth; i++) {
            newWhitespace.append(' ');
        }

        return newWhitespace.toString();
    }
}