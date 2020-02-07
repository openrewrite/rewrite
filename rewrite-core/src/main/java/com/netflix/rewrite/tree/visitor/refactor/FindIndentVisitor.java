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
package com.netflix.rewrite.tree.visitor.refactor;

import com.netflix.rewrite.internal.StringUtils;
import com.netflix.rewrite.tree.Formatting;
import com.netflix.rewrite.tree.Tree;
import com.netflix.rewrite.tree.visitor.CursorAstVisitor;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.experimental.NonFinal;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.counting;

/**
 * Discover the most common indentation level of a tree, and whether this indentation is built with spaces or tabs.
 */
@RequiredArgsConstructor
public class FindIndentVisitor extends CursorAstVisitor<Integer> {
    private final SortedMap<Integer, Long> indentFrequencies = new TreeMap<>();

    private int enclosingIndent;

    @NonFinal
    private int linesWithSpaceIndents = 0;

    @NonFinal
    private int linesWithTabIndents = 0;

    @Override
    public Integer defaultTo(Tree t) {
        return null;
    }

    @Override
    public Integer visitTree(Tree tree) {
        if(tree.getFormatting() instanceof Formatting.Reified) {
            String prefix = ((Formatting.Reified) tree.getFormatting()).getPrefix();
            if(prefix.chars().takeWhile(c -> c == '\n' || c == '\r').count() > 0) {
                indentFrequencies.merge((int) prefix.chars()
                        .dropWhile(c -> c == '\n' || c == '\r')
                        .takeWhile(Character::isWhitespace)
                        .count() - enclosingIndent,
                        1L, Long::sum);

                Map<Boolean, Long> indentTypeCounts = prefix.chars().dropWhile(c -> c == '\n' || c == '\r')
                        .takeWhile(Character::isWhitespace)
                        .mapToObj(c -> c == ' ')
                        .collect(Collectors.groupingBy(identity(), counting()));

                if(indentTypeCounts.getOrDefault(true, 0L) >= indentTypeCounts.getOrDefault(false, 0L)) {
                    linesWithSpaceIndents++;
                }
                else {
                    linesWithTabIndents++;
                }
            }
        }

        return super.visitTree(tree);
    }

    public boolean isIndentedWithSpaces() {
        return linesWithSpaceIndents >= linesWithTabIndents;
    }

    @Override
    public Integer visitEnd() {
        return getMostCommonIndent();
    }

    public int getMostCommonIndent() {
        indentFrequencies.remove(0);
        return StringUtils.mostCommonIndent(indentFrequencies);
    }

    /**
     * @return The total number of source lines that this indent decision was made on.
     */
    public int getTotalLines() {
        return linesWithSpaceIndents + linesWithTabIndents;
    }

    public void reset() {
        indentFrequencies.clear();
        linesWithTabIndents = 0;
        linesWithSpaceIndents = 0;
    }
}
