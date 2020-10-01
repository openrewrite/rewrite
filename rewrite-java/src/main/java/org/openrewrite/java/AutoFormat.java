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
package org.openrewrite.java;

import org.openrewrite.AbstractSourceVisitor;
import org.openrewrite.Incubating;
import org.openrewrite.Tree;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.style.TabAndIndentStyle;
import org.openrewrite.java.tree.J;
import org.openrewrite.refactor.Formatter;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.counting;
import static java.util.stream.IntStream.range;

/**
 * A general purpose means of formatting arbitrarily complex blocks of code relative based on their
 * surrounding context.
 * <p>
 * TODO when complete, this should replace {@link ShiftFormatRightVisitor}.
 */
@Incubating(since = "2.1.0")
public class AutoFormat extends JavaIsoRefactorVisitor {
    private final J[] scope;

    private Formatter.Result wholeSourceIndent;

    public AutoFormat(J... scope) {
        this.scope = scope;
        setCursoringOn();
    }

    @Override
    public boolean isIdempotent() {
        return false;
    }

    @Override
    public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu) {
        FindIndentExceptScope findIndent = new FindIndentExceptScope(0);
        findIndent.visit(cu);

        TabAndIndentStyle tabAndIndentStyle = cu.getStyle(TabAndIndentStyle.class)
                .orElse(new TabAndIndentStyle());

        int mostCommonIndent = findIndent.getMostCommonIndent();

        wholeSourceIndent = new Formatter.Result(
                0,
                mostCommonIndent == 0 ?
                        tabAndIndentStyle.getIndentSize() :
                        mostCommonIndent,
                mostCommonIndent == 0 ?
                        !tabAndIndentStyle.isUseTabCharacter() :
                        findIndent.isIndentedWithSpaces()
        );

        return super.visitCompilationUnit(cu);
    }

    @Override
    public J reduce(J r1, J r2) {
        J j = super.reduce(r1, r2);
        if (r2 != null && r2.getPrefix().startsWith("|")) {
            j = j.withPrefix(r2.getPrefix().substring(1));
        }
        return j;
    }

    @Override
    public J visitTree(Tree tree) {
        J j = super.visitTree(tree);

        String prefix = tree.getPrefix();
        if (prefix.contains("\n") && stream(scope).anyMatch(s -> getCursor().isScopeInPath(s))) {
            int indentMultiple = (int) getCursor().getPathAsStream().filter(J.Block.class::isInstance).count();
            if(tree instanceof J.Block.End) {
                indentMultiple--;
            }

            String shiftedPrefix = "|" + prefix.substring(0, prefix.lastIndexOf('\n') + 1) + range(0, indentMultiple * wholeSourceIndent.getIndentToUse())
                    .mapToObj(n -> wholeSourceIndent.isIndentedWithSpaces() ? " " : "\t")
                    .collect(Collectors.joining(""));

            if (!shiftedPrefix.equals(prefix)) {
                j = j.withPrefix(shiftedPrefix);
            }
        }

        return j;
    }

    private class FindIndentExceptScope extends AbstractSourceVisitor<Void> {
        private final SortedMap<Integer, Long> indentFrequencies = new TreeMap<>();
        private final int enclosingIndent;

        private int linesWithSpaceIndents = 0;
        private int linesWithTabIndents = 0;

        public FindIndentExceptScope(int enclosingIndent) {
            this.enclosingIndent = enclosingIndent;
            setCursoringOn();
        }

        @Override
        public Void defaultTo(Tree t) {
            return null;
        }

        private final Pattern SINGLE_LINE_COMMENT = Pattern.compile("//[^\\n]+");
        private final Pattern MULTI_LINE_COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);

        @Override
        public Void visitTree(Tree tree) {
            if (stream(scope).anyMatch(s -> getCursor().isScopeInPath(s))) {
                return null;
            }

            String prefix = tree.getPrefix();

            AtomicBoolean takeWhile = new AtomicBoolean(true);
            if (prefix.chars()
                    .filter(c -> {
                        takeWhile.set(takeWhile.get() && (c == '\n' || c == '\r'));
                        return takeWhile.get();
                    })
                    .count() > 0) {
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
    }
}
