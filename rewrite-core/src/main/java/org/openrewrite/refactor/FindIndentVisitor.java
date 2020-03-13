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
package org.openrewrite.refactor;

import lombok.RequiredArgsConstructor;
import org.openrewrite.SourceVisitor;
import org.openrewrite.Tree;
import org.openrewrite.internal.StringUtils;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.counting;

/**
 * Discover the most common indentation level of a tree, and whether this indentation is built with spaces or tabs.
 */
@RequiredArgsConstructor
public class FindIndentVisitor extends SourceVisitor<Void> {
    private final SortedMap<Integer, Long> indentFrequencies = new TreeMap<>();
    private final int enclosingIndent;

    private int linesWithSpaceIndents = 0;
    private int linesWithTabIndents = 0;

    @Override
    public Void defaultTo(Tree t) {
        return null;
    }

    private static final Pattern SINGLE_LINE_COMMENT = Pattern.compile("//[^\\n]+");
    private static final Pattern MULTI_LINE_COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);

    @Override
    public Void visitTree(Tree tree) {
        String prefix = tree.getFormatting().getPrefix();
        if (prefix.chars().takeWhile(c -> c == '\n' || c == '\r').count() > 0) {
            int indent = 0;
            char[] chars = MULTI_LINE_COMMENT.matcher(SINGLE_LINE_COMMENT.matcher(prefix)
                    .replaceAll("")).replaceAll("").toCharArray();
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

            Map<Boolean, Long> indentTypeCounts = prefix.chars().dropWhile(c -> c == '\n' || c == '\r')
                    .takeWhile(Character::isWhitespace)
                    .mapToObj(c -> c == ' ')
                    .collect(Collectors.groupingBy(identity(), counting()));

            if (indentTypeCounts.getOrDefault(true, 0L) >= indentTypeCounts.getOrDefault(false, 0L)) {
                linesWithSpaceIndents++;
            } else {
                linesWithTabIndents++;
            }
        }

        return super.visitTree(tree);
    }

    public boolean isIndentedWithSpaces() {
        return linesWithSpaceIndents >= linesWithTabIndents;
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
}
