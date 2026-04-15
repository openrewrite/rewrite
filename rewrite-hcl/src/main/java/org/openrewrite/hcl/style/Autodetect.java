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
package org.openrewrite.hcl.style;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.jspecify.annotations.Nullable;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.hcl.HclVisitor;
import org.openrewrite.hcl.tree.BodyContent;
import org.openrewrite.hcl.tree.Hcl;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.style.GeneralFormatStyle;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.style.Style;

import java.util.*;

import static java.util.Collections.emptySet;

public class Autodetect extends NamedStyles {
    @JsonCreator
    public Autodetect(UUID id, Collection<Style> styles) {
        super(id,
                "org.openrewrite.hcl.Autodetect",
                "Auto-detected",
                "Automatically detect styles from a repository's existing code.",
                emptySet(), styles);
    }

    public static Detector detector() {
        return new Detector();
    }

    public static class Detector {

        private final IndentStatistics indentStatistics = new IndentStatistics();
        private final GeneralFormatStatistics generalFormatStatistics = new GeneralFormatStatistics();
        private final FindIndentHclVisitor findIndentVisitor = new FindIndentHclVisitor();
        private final FindLineFormatHclVisitor findLineFormatVisitor = new FindLineFormatHclVisitor();

        public Detector sample(SourceFile hcl) {
            if (hcl instanceof Hcl.ConfigFile) {
                findIndentVisitor.visit(hcl, indentStatistics);
                findLineFormatVisitor.visit(hcl, generalFormatStatistics);
            }
            return this;
        }

        public Autodetect build() {
            return new Autodetect(Tree.randomId(), Arrays.asList(
                    indentStatistics.getTabsAndIndentsStyle(),
                    generalFormatStatistics.getFormatStyle()
            ));
        }
    }

    private static class IndentStatistics {
        private final Map<Integer, Long> spaceIndentFrequencies = new HashMap<>();
        private final Map<Integer, Long> tabIndentFrequencies = new HashMap<>();
        private int linesWithSpaceIndents = 0;
        private int linesWithTabIndents = 0;

        public boolean isIndentedWithSpaces() {
            return linesWithSpaceIndents >= linesWithTabIndents;
        }

        public TabsAndIndentsStyle getTabsAndIndentsStyle() {
            boolean useTabs = !isIndentedWithSpaces();

            int indent = TabsAndIndentsStyle.DEFAULT.getIndentSize();
            long indentCount = 0;

            Map<Integer, Long> indentFrequencies = useTabs ? tabIndentFrequencies : spaceIndentFrequencies;
            for (Map.Entry<Integer, Long> current : indentFrequencies.entrySet()) {
                if (current.getKey() == 0) {
                    continue;
                }
                long currentCount = 0;
                for (Map.Entry<Integer, Long> candidate : indentFrequencies.entrySet()) {
                    if (candidate.getKey() == 0) {
                        continue;
                    }
                    if (candidate.getKey() % current.getKey() == 0) {
                        currentCount += candidate.getValue();
                    }
                }
                if (currentCount > indentCount) {
                    indent = current.getKey();
                    indentCount = currentCount;
                } else if (currentCount == indentCount) {
                    indent = Math.min(indent, current.getKey());
                }
            }

            return new TabsAndIndentsStyle(
                    useTabs,
                    useTabs ? indent : 1,
                    useTabs ? 1 : indent
            );
        }
    }

    private static class GeneralFormatStatistics {
        private int linesWithCRLFNewLines = 0;
        private int linesWithLFNewLines = 0;

        public boolean isIndentedWithLFNewLines() {
            return linesWithLFNewLines >= linesWithCRLFNewLines;
        }

        public GeneralFormatStyle getFormatStyle() {
            return new GeneralFormatStyle(!isIndentedWithLFNewLines());
        }
    }

    private static class FindLineFormatHclVisitor extends HclVisitor<GeneralFormatStatistics> {
        @Override
        public @Nullable Hcl visit(@Nullable Tree tree, GeneralFormatStatistics stats) {
            if (tree instanceof Hcl) {
                String prefix = ((Hcl) tree).getPrefix().getWhitespace();
                char[] chars = prefix.toCharArray();
                for (int i = 0; i < chars.length; i++) {
                    char c = chars[i];
                    if (c == '\n') {
                        if (i == 0 || chars[i - 1] != '\r') {
                            stats.linesWithLFNewLines++;
                        } else {
                            stats.linesWithCRLFNewLines++;
                        }
                    }
                }
            }
            return super.visit(tree, stats);
        }
    }

    private static class FindIndentHclVisitor extends HclVisitor<IndentStatistics> {
        @Override
        public Hcl visitBlock(Hcl.Block block, IndentStatistics stats) {
            for (BodyContent content : block.getBody()) {
                measureFrequencies(content.getPrefix().getWhitespace(), stats);
            }
            return super.visitBlock(block, stats);
        }

        @Override
        public Hcl visitAttribute(Hcl.Attribute attribute, IndentStatistics stats) {
            measureFrequencies(attribute.getPrefix().getWhitespace(), stats);
            return super.visitAttribute(attribute, stats);
        }

        private void measureFrequencies(String prefix, IndentStatistics stats) {
            if (!StringUtils.hasLineBreak(prefix)) {
                return;
            }

            int tabIndent = 0;
            int spaceIndent = 0;
            boolean mixed = false;
            char[] chars = prefix.toCharArray();
            for (char c : chars) {
                if (c == '\n' || c == '\r') {
                    tabIndent = 0;
                    spaceIndent = 0;
                    mixed = false;
                    continue;
                }
                if (mixed) {
                    continue;
                }
                if (c == ' ') {
                    if (tabIndent > 0) {
                        mixed = true;
                        tabIndent = 0;
                        spaceIndent = 0;
                        continue;
                    }
                    spaceIndent++;
                } else if (Character.isWhitespace(c)) {
                    if (spaceIndent > 0) {
                        mixed = true;
                        tabIndent = 0;
                        spaceIndent = 0;
                        continue;
                    }
                    tabIndent++;
                }
            }

            stats.spaceIndentFrequencies.merge(spaceIndent, 1L, Long::sum);
            stats.tabIndentFrequencies.merge(tabIndent, 1L, Long::sum);

            if (spaceIndent > 0 || tabIndent > 0) {
                if (spaceIndent >= tabIndent) {
                    stats.linesWithSpaceIndents++;
                } else {
                    stats.linesWithTabIndents++;
                }
            }
        }
    }
}
