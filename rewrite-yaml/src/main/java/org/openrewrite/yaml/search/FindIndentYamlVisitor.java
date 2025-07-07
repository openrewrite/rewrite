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

import org.jspecify.annotations.Nullable;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.yaml.YamlVisitor;
import org.openrewrite.yaml.tree.Yaml;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Discover the most common indentation level of a tree.
 */
public class FindIndentYamlVisitor<P> extends YamlVisitor<P> {
    private final SortedMap<Integer, Long> indentFrequencies = new TreeMap<>();

    @Override
    public @Nullable Yaml preVisit(Yaml tree, P p) {
        String prefix = tree.getPrefix();

        if (StringUtils.hasLineBreak(prefix)) {
            int indent = 0;
            for (char c : prefix.toCharArray()) {
                if (c == '\n' || c == '\r') {
                    indent = 0;
                } else if (Character.isWhitespace(c)) {
                    indent++;
                }
            }

            indentFrequencies.merge(indent, 1L, Long::sum);
        }

        return super.preVisit(tree, p);
    }

    public int getMostCommonIndent() {
        indentFrequencies.remove(0);

        if (indentFrequencies.getOrDefault(0, 0L) > 1) {
            return 0;
        }

        return calculateMostCommonIndent();
    }

    public long nonZeroIndents() {
        return indentFrequencies.tailMap(1).values().stream().mapToLong(f -> f).sum();
    }

    private int calculateMostCommonIndent() {
        // the frequency of each indent level is an integral divisor of longer indent levels
        SortedMap<Integer, Integer> indentFrequencyAsDivisors = new TreeMap<>();
        for (Map.Entry<Integer, Long> indentFrequency : indentFrequencies.entrySet()) {
            int indent = indentFrequency.getKey();
            int freq;
            switch (indent) {
                case 0:
                    freq = indentFrequency.getValue().intValue();
                    break;
                case 1:
                    // gcd(1, N) == 1, so we can avoid the test for this case
                    freq = (int) indentFrequencies.tailMap(indent).values().stream().mapToLong(l -> l).sum();
                    break;
                default:
                    freq = (int) indentFrequencies.tailMap(indent).entrySet().stream()
                            .filter(inF -> gcd(inF.getKey(), indent) != 0)
                            .mapToLong(Map.Entry::getValue)
                            .sum();
            }

            indentFrequencyAsDivisors.put(indent, freq);
        }

        return indentFrequencyAsDivisors.entrySet().stream()
                .max((e1, e2) -> {
                    int valCompare = e1.getValue().compareTo(e2.getValue());
                    return valCompare != 0 ?
                            valCompare :
                            // take the smallest indent otherwise, unless it would be zero
                            e1.getKey() == 0 ? -1 : e2.getKey().compareTo(e1.getKey());
                })
                .map(Map.Entry::getKey)
                .orElse(0);
    }

    private static int gcd(int n1, int n2) {
        return n2 == 0 ? n1 : gcd(n2, n1 % n2);
    }
}
