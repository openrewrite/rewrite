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
package org.openrewrite.yaml.search;

import org.openrewrite.internal.StringUtils;
import org.openrewrite.yaml.YamlVisitor;
import org.openrewrite.yaml.tree.Yaml;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.counting;

/**
 * Discover the most common indentation level of a tree.
 */
public class FindIndentYamlVisitor<P> extends YamlVisitor<P> {
    private final SortedMap<Integer, Long> indentFrequencies = new TreeMap<>();
    private final int enclosingIndent;

    public FindIndentYamlVisitor(int enclosingIndent) {
        this.enclosingIndent = enclosingIndent;
    }

    @Override
    public Yaml preVisit(Yaml tree, P p) {
        String prefix = tree.getPrefix();

        AtomicBoolean takeWhile = new AtomicBoolean(true);
        if (prefix.chars()
                .filter(c -> {
                    takeWhile.set(takeWhile.get() && (c == '\n' || c == '\r'));
                    return takeWhile.get();
                })
                .count() > 0) {
            int indent = 0;
            char[] chars = prefix.toCharArray();
            for (char c : chars) {
                if (c == '\n' || c == '\r') {
                    indent = 0;
                    continue;
                }
                if (Character.isWhitespace(c)) {
                    indent++;
                }
            }

            indentFrequencies.merge(indent - enclosingIndent, 1L, Long::sum);

            AtomicBoolean dropWhile = new AtomicBoolean(false);
            takeWhile.set(true);
            Map<Boolean, Long> indentTypeCounts = prefix.chars()
                    .filter(c -> {
                        dropWhile.set(dropWhile.get() || !(c == '\n' || c == '\r'));
                        return dropWhile.get();
                    })
                    .filter(c -> {
                        takeWhile.set(takeWhile.get() && Character.isWhitespace(c));
                        return takeWhile.get();
                    })
                    .mapToObj(c -> c == ' ')
                    .collect(Collectors.groupingBy(identity(), counting()));
            indentTypeCounts.getOrDefault(true, 0L);
            indentTypeCounts.getOrDefault(false, 0L);
        }

        return super.preVisit(tree, p);
    }

    public int getMostCommonIndent() {
        indentFrequencies.remove(0);
        return StringUtils.mostCommonIndent(indentFrequencies);
    }

    public long nonZeroIndents() {
        return indentFrequencies.tailMap(1).values().stream().mapToLong(f -> f).sum();
    }
}
