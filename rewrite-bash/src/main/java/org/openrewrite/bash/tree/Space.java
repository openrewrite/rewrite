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
package org.openrewrite.bash.tree;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.openrewrite.marker.Markers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import static java.util.Collections.emptyList;
import static java.util.Collections.synchronizedMap;

@EqualsAndHashCode
@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@ref")
public class Space {
    public static final Space EMPTY = new Space("", emptyList());
    public static final Space SINGLE_SPACE = new Space(" ", emptyList());
    public static final Space NEWLINE = new Space("\n", emptyList());

    @Getter
    private final List<Comment> comments;

    @Nullable
    private final String whitespace;

    private static final Map<String, Space> flyweights = synchronizedMap(new WeakHashMap<>());

    private Space(@Nullable String whitespace, List<Comment> comments) {
        this.comments = comments;
        this.whitespace = whitespace == null || whitespace.isEmpty() ? null : whitespace;
    }

    @JsonCreator
    public static Space build(@Nullable String whitespace, List<Comment> comments) {
        if (comments.isEmpty()) {
            if (whitespace == null || whitespace.isEmpty()) {
                return Space.EMPTY;
            }
            if (whitespace.length() <= 100) {
                return flyweights.computeIfAbsent(whitespace, k -> new Space(new String(whitespace), comments));
            }
        }
        return new Space(whitespace, comments);
    }

    public String getWhitespace() {
        return whitespace == null ? "" : whitespace;
    }

    public Space withComments(List<Comment> comments) {
        if (comments == this.comments) {
            return this;
        }
        if (comments.isEmpty() && (whitespace == null || whitespace.isEmpty())) {
            return Space.EMPTY;
        }
        return build(whitespace, comments);
    }

    public Space withWhitespace(String whitespace) {
        if (comments.isEmpty() && whitespace.isEmpty()) {
            return Space.EMPTY;
        }
        if ((whitespace.isEmpty() && this.whitespace == null) || whitespace.equals(this.whitespace)) {
            return this;
        }
        return build(whitespace, comments);
    }

    public boolean isEmpty() {
        return this == EMPTY;
    }

    public static Space format(String formatting) {
        return format(formatting, 0, formatting.length());
    }

    public static Space format(String formatting, int beginIndex, int toIndex) {
        if (beginIndex == toIndex) {
            return Space.EMPTY;
        }
        if (toIndex == beginIndex + 1 && ' ' == formatting.charAt(beginIndex)) {
            return Space.SINGLE_SPACE;
        }
        rangeCheck(formatting.length(), beginIndex, toIndex);

        StringBuilder prefix = new StringBuilder();
        StringBuilder comment = new StringBuilder();
        List<Comment> comments = new ArrayList<>(1);

        boolean inComment = false;

        for (int i = beginIndex; i < toIndex; i++) {
            char c = formatting.charAt(i);
            switch (c) {
                case '#':
                    if (inComment) {
                        comment.append(c);
                    } else {
                        inComment = true;
                        comment.setLength(0);
                        comment.append(c);
                    }
                    break;
                case '\r':
                case '\n':
                    if (inComment) {
                        inComment = false;
                        comments.add(new Comment(comment.toString(), prefix.toString(), Markers.EMPTY));
                        prefix.setLength(0);
                        comment.setLength(0);
                        prefix.append(c);
                    } else {
                        prefix.append(c);
                    }
                    break;
                default:
                    if (inComment) {
                        comment.append(c);
                    } else {
                        prefix.append(c);
                    }
            }
        }
        if (comment.length() > 0 || inComment) {
            comments.add(new Comment(comment.toString(), prefix.toString(), Markers.EMPTY));
            prefix.setLength(0);
        }

        return build(prefix.toString(), comments);
    }

    private static void rangeCheck(int arrayLength, int fromIndex, int toIndex) {
        if (fromIndex > toIndex) {
            throw new IllegalArgumentException("fromIndex(" + fromIndex + ") > toIndex(" + toIndex + ")");
        }
        if (fromIndex < 0) {
            throw new ArrayIndexOutOfBoundsException(fromIndex);
        }
        if (toIndex > arrayLength) {
            throw new ArrayIndexOutOfBoundsException(toIndex);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Comment comment : comments) {
            sb.append(comment.getPrefix());
            sb.append(comment.getText());
        }
        sb.append(whitespace == null ? "" : whitespace);
        return sb.toString();
    }
}
