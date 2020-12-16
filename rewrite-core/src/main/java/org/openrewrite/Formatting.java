/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite;

import com.fasterxml.jackson.annotation.*;
import lombok.Getter;
import org.openrewrite.internal.lang.Nullable;

import java.io.Serializable;
import java.util.*;

/**
 * The stylistic surroundings of a tree element
 */
@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@ref")
@Getter
public class Formatting implements Serializable {
    // suffixes are uncommon, so we'll treat them as a secondary index
    private static final Map<String, Map<String, Formatting>> flyweights = new HashMap<>();

    public static Formatting EMPTY = new Formatting("", "") {
        @Override
        public String toString() {
            return "Formatting{EMPTY}";
        }
    };

    private final String prefix;
    private final String suffix;

    private Formatting(String prefix, String suffix) {
        this.prefix = prefix;
        this.suffix = suffix;
    }

    public static Formatting format(String prefix) {
        return format(prefix, "");
    }

    @JsonCreator
    public static Formatting format(@JsonProperty("prefix") String prefix, @JsonProperty("suffix") String suffix) {
        synchronized (flyweights) {
            if (prefix.isEmpty() && suffix.isEmpty()) {
                return EMPTY;
            }

            return flyweights
                    .computeIfAbsent(prefix, p -> new HashMap<>())
                    .computeIfAbsent(suffix, s -> new Formatting(prefix, s));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Formatting that = (Formatting) o;
        return Objects.equals(prefix, that.prefix) &&
                Objects.equals(suffix, that.suffix);
    }

    @Override
    public int hashCode() {
        return Objects.hash(prefix, suffix);
    }

    public Formatting withPrefix(String prefix) {
        return format(prefix, suffix);
    }

    public Formatting withSuffix(String suffix) {
        return format(prefix, suffix);
    }

    public Formatting withMinimumBlankLines(int min) {
        int blankLinesLessThanMin = (int) ((min + 1) - prefix.chars().filter(c -> c == '\n').count());
        StringBuilder newBlankLines = new StringBuilder();
        for (int i = 0; i < blankLinesLessThanMin; i++) {
            newBlankLines.append('\n');
        }
        return withPrefix(newBlankLines + prefix);
    }

    public Formatting withMaximumBlankLines(int max) {
        int blankLines = (int) (prefix.chars().filter(c -> c == '\n').count() - 1);

        if(blankLines <= max) {
            return this;
        }

        StringBuilder newPrefix = new StringBuilder();

        char[] chars = prefix.toCharArray();
        int newLinesSeen = 0;

        for (char c : chars) {
            if(c == '\n') {
                newLinesSeen++;
            }

            if (newLinesSeen == 0 || newLinesSeen > max) {
                newPrefix.append(c);
            }
        }

        return withPrefix(newPrefix.toString());
    }

    @JsonIgnore
    public int getIndent() {
        return getIndent(prefix);
    }

    public static int getIndent(String formatting) {
        int indent = 0;
        for (char c : formatting.toCharArray()) {
            if (c == '\n' || c == '\r' || !Character.isWhitespace(c)) {
                indent = 0;
                continue;
            }
            indent++;
        }
        return indent;
    }

    public static String firstPrefix(@Nullable List<? extends Tree> trees) {
        return trees == null || trees.isEmpty() ? "" : trees.iterator().next().getPrefix();
    }

    public static String lastSuffix(@Nullable List<? extends Tree> trees) {
        return trees == null || trees.isEmpty() ? "" : trees.get(trees.size() - 1).getSuffix();
    }

    public static <T extends Tree> List<T> formatFirstPrefix(@Nullable List<T> trees, String prefix) {
        if (trees == null) {
            return null;
        }
        if (!trees.isEmpty()) {
            List<T> formattedTrees = new ArrayList<>(trees);
            formattedTrees.set(0, formattedTrees.get(0).withPrefix(prefix));
            return formattedTrees;
        }
        return trees;
    }

    public static <T extends Tree> List<T> formatLastSuffix(@Nullable List<T> trees, String suffix) {
        if (trees == null) {
            return null;
        }
        if (!trees.isEmpty()) {
            List<T> formattedTrees = new ArrayList<>(trees);
            formattedTrees.set(formattedTrees.size() - 1, formattedTrees.get(formattedTrees.size() - 1).withSuffix(suffix));
            return formattedTrees;
        }
        return trees;
    }

    public static <T extends Tree> T stripSuffix(@Nullable T t) {
        return t == null ? null : t.withSuffix("");
    }

    public static <T extends Tree> T stripPrefix(@Nullable T t) {
        return t == null ? null : t.withPrefix("");
    }
}
