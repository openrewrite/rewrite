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
package org.openrewrite.visitor.refactor;

import org.openrewrite.internal.StringUtils;
import org.openrewrite.tree.Cursor;
import org.openrewrite.tree.Formatting;
import org.openrewrite.tree.J;
import org.openrewrite.tree.Tree;
import org.openrewrite.visitor.CursorAstVisitor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.tree.Cursor;
import org.openrewrite.tree.Formatting;
import org.openrewrite.tree.J;
import org.openrewrite.tree.Tree;
import org.openrewrite.visitor.CursorAstVisitor;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.joining;
import static java.util.stream.IntStream.range;

@RequiredArgsConstructor
public class Formatter {
    private final J.CompilationUnit cu;
    private Result wholeSourceIndent;

    @RequiredArgsConstructor
    @Getter
    public static class Result {
        private final int enclosingIndent;
        private final int indentToUse;
        private final boolean indentedWithSpaces;

        public String getPrefix() {
            return getPrefix(0);
        }

        public String getPrefix(int offset) {
            return range(0, indentToUse + (indentToUse * offset) + enclosingIndent)
                    .mapToObj(i -> indentedWithSpaces ? " " : "\t")
                    .collect(joining("", "\n", ""));
        }
    }

    private Result wholeSourceIndent() {
        if (wholeSourceIndent == null) {
            var wholeSourceIndentVisitor = new FindIndentVisitor(0);
            wholeSourceIndentVisitor.visit(cu);
            wholeSourceIndent = new Result(0, wholeSourceIndentVisitor.getMostCommonIndent() > 0 ?
                    wholeSourceIndentVisitor.getMostCommonIndent() : 4 /* default to 4 spaces */,
                    wholeSourceIndentVisitor.isIndentedWithSpaces());
        }
        return wholeSourceIndent;
    }

    public Result findIndent(int enclosingIndent, Tree... trees) {
        var findIndentVisitor = new FindIndentVisitor(enclosingIndent);
        for (Tree tree : trees) {
            findIndentVisitor.visit(tree);
        }

        var indentToUse = findIndentVisitor.getMostCommonIndent() > 0 ?
                findIndentVisitor.getMostCommonIndent() :
                wholeSourceIndent().getIndentToUse();
        var indentedWithSpaces = findIndentVisitor.getTotalLines() > 0 ? findIndentVisitor.isIndentedWithSpaces() :
                wholeSourceIndent().isIndentedWithSpaces();

        return new Result(enclosingIndent, indentToUse, indentedWithSpaces);
    }

    public Formatting format(Tree relativeToEnclosing) {
        Tree[] siblings = new Tree[0];
        if(relativeToEnclosing instanceof J.Block) {
            siblings = ((J.Block<?>) relativeToEnclosing).getStatements().toArray(Tree[]::new);
        }
        else if(relativeToEnclosing instanceof J.Case) {
            siblings = ((J.Case) relativeToEnclosing).getStatements().toArray(Tree[]::new);
        }

        Result indentation = findIndent(enclosingIndent(relativeToEnclosing), siblings);
        return Formatting.format(indentation.getPrefix());
    }

    public Formatting format(Cursor cursor) {
        return format(cursor.enclosingBlock());
    }

    /**
     * @param moving The tree that is moving
     * @param into   The block the tree is moving into
     * @return A shift right format visitor that can be appended to a refactor visitor pipeline
     */
    public ShiftFormatRightVisitor shiftRight(Tree moving, Tree into, Tree enclosesBoth) {
        // NOTE: This isn't absolutely perfect... suppose the block moving was indented with tabs and the surrounding source was spaces.
        // Should be close enough in the vast majority of cases.
        int shift = enclosingIndent(into) - findIndent(enclosingIndent(enclosesBoth), moving).getEnclosingIndent();
        return new ShiftFormatRightVisitor(moving.getId(), shift, wholeSourceIndent().isIndentedWithSpaces());
    }

    public static int enclosingIndent(Tree enclosesBoth) {
        return enclosesBoth instanceof J.Block ? ((J.Block<?>) enclosesBoth).getIndent() :
                (int) enclosesBoth.getFormatting().getPrefix().chars().dropWhile(c -> c == '\n' || c == '\r')
                        .takeWhile(Character::isWhitespace).count();
    }

    public boolean isIndentedWithSpaces() {
        return wholeSourceIndent().isIndentedWithSpaces();
    }

    /**
     * Discover the most common indentation level of a tree, and whether this indentation is built with spaces or tabs.
     */
    @RequiredArgsConstructor
    private static class FindIndentVisitor extends CursorAstVisitor<Integer> {
        private final SortedMap<Integer, Long> indentFrequencies = new TreeMap<>();
        private final int enclosingIndent;

        private int linesWithSpaceIndents = 0;
        private int linesWithTabIndents = 0;

        @Override
        public Integer defaultTo(Tree t) {
            return null;
        }

        private static final Pattern SINGLE_LINE_COMMENT = Pattern.compile("//[^\\n]+");
        private static final Pattern MULTI_LINE_COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);

        @Override
        public Integer visitTree(Tree tree) {
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
    }
}
