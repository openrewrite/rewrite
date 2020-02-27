/*
 * Copyright 2020 the original authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.tree;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.koloboke.collect.map.hash.HashObjObjMaps;
import org.openrewrite.internal.lang.Nullable;
import lombok.Getter;
import org.openrewrite.internal.lang.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.PrimitiveIterator;

/**
 * The stylistic surroundings of a tree element
 */
@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@ref")
@Getter
public class Formatting implements Serializable {
    // suffixes are uncommon, so we'll treat them as a secondary index
    private static final Map<String, Map<String, Formatting>> flyweights = HashObjObjMaps.newMutableMap();

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
                    .computeIfAbsent(prefix, p -> HashObjObjMaps.newMutableMap())
                    .computeIfAbsent(suffix, s -> new Formatting(prefix, s));
        }
    }

    public Formatting withPrefix(String prefix) {
        return format(prefix, suffix);
    }

    public Formatting withSuffix(String suffix) {
        return format(prefix, suffix);
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
        return trees == null || trees.isEmpty() ? "" : trees.iterator().next().getFormatting().getPrefix();
    }

    public static String lastSuffix(@Nullable List<? extends Tree> trees) {
        return trees == null || trees.isEmpty() ? "" : trees.get(trees.size() - 1).getFormatting().getSuffix();
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