/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.xml.style;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.style.GeneralFormatStyle;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.style.Style;
import org.openrewrite.xml.XmlVisitor;
import org.openrewrite.xml.tree.Xml;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static java.util.Collections.emptySet;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.counting;

public class Autodetect extends NamedStyles {
    @JsonCreator
    public Autodetect(UUID id, Collection<Style> styles) {
        super(id,
                "org.openrewrite.xml.Autodetect",
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
        private final FindIndentXmlVisitor findIndentXmlVisitor = new FindIndentXmlVisitor();
        private final FindLineFormatJavaVisitor findLineFormatJavaVisitor = new FindLineFormatJavaVisitor();

        public void sample(SourceFile xml) {
            if(xml instanceof Xml.Document) {
                findIndentXmlVisitor.visit(xml, indentStatistics);
                findLineFormatJavaVisitor.visit(xml, generalFormatStatistics);
            }
        }

        public Autodetect build() {
            return new Autodetect(Tree.randomId(), Arrays.asList(
                    indentStatistics.getTabsAndIndentsStyle(),
                    generalFormatStatistics.getFormatStyle()));
        }
    }

    private static class IndentStatistics {
        IndentFrequencies indentFrequencies = new IndentFrequencies();
        IndentFrequencies continuationIndentFrequencies = new IndentFrequencies();

        public TabsAndIndentsStyle getTabsAndIndentsStyle() {
            TabsAndIndentsStyle style = indentFrequencies.getTabsAndIndentsStyle();
            return continuationIndentFrequencies.hasEnoughSamples() ?
                    style.withContinuationIndentSize(continuationIndentFrequencies.getTabsAndIndentsStyle().getIndentSize()) :
                    style;
        }
    }

    private static class IndentFrequencies {
        private final Map<Integer, Long> spaceIndentFrequencies = new HashMap<>();
        private final Map<Integer, Long> tabIndentFrequencies = new HashMap<>();
        private int linesWithSpaceIndents = 0;
        private int linesWithTabIndents = 0;

        public boolean isIndentedWithSpaces() {
            return linesWithSpaceIndents >= linesWithTabIndents;
        }

        public boolean hasEnoughSamples() {
            return linesWithSpaceIndents + linesWithTabIndents > 1;
        }

        private TabsAndIndentsStyle getTabsAndIndentsStyle() {
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
                    useTabs ? 1 : indent,
                    useTabs ? 2 : indent * 2
            );
        }
    }

    private static class FindLineFormatJavaVisitor extends XmlVisitor<GeneralFormatStatistics> {
        @Override
        public @Nullable Xml visit(@Nullable Tree tree, GeneralFormatStatistics stats) {
            if (tree instanceof Xml) {
                String prefix = ((Xml) tree).getPrefix();
                char[] chars = prefix.toCharArray();

                for (int i = 0; i < chars.length; i++) {
                    char c = chars[i];
                    if (c == '\n' || c == '\r') {
                        if (c == '\n') {
                            if (i == 0 || chars[i - 1] != '\r') {
                                stats.linesWithLFNewLines++;
                            } else {
                                stats.linesWithCRLFNewLines++;
                            }
                        }
                    }
                }
            }
            return super.visit(tree, stats);
        }
    }

    private static class FindIndentXmlVisitor extends XmlVisitor<IndentStatistics> {
        @Override
        public Xml visitTag(Xml.Tag tag, IndentStatistics stats) {
            measureFrequencies(tag.getPrefix(), stats.indentFrequencies);
            return super.visitTag(tag, stats);
        }

        @Override
        public Xml visitAttribute(Xml.Attribute attribute, IndentStatistics stats) {
            measureFrequencies(attribute.getPrefix(), stats.continuationIndentFrequencies);
            return super.visitAttribute(attribute, stats);
        }

        private void measureFrequencies(String prefix, IndentFrequencies frequencies) {
            AtomicBoolean takeWhile = new AtomicBoolean(true);
            if (prefix.chars()
                        .filter(c -> {
                            takeWhile.set(takeWhile.get() && (c == '\n' || c == '\r'));
                            return takeWhile.get();
                        })
                        .count() > 0) {
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
                            tabIndent = 0;
                            spaceIndent = 0;
                            mixed = true;
                            continue;
                        }
                        spaceIndent++;
                    } else if (Character.isWhitespace(c)) {
                        if (spaceIndent > 0) {
                            tabIndent = 0;
                            spaceIndent = 0;
                            break;
                        }
                        tabIndent++;
                    }
                }

                frequencies.spaceIndentFrequencies.merge(spaceIndent, 1L, Long::sum);
                frequencies.tabIndentFrequencies.merge(tabIndent, 1L, Long::sum);

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
                    frequencies.linesWithSpaceIndents++;
                } else {
                    frequencies.linesWithTabIndents++;
                }
            }
        }
    }

    private static class GeneralFormatStatistics {
        private int linesWithCRLFNewLines = 0;
        private int linesWithLFNewLines = 0;

        public boolean isIndentedWithLFNewLines() {
            return linesWithLFNewLines >= linesWithCRLFNewLines;
        }

        public GeneralFormatStyle getFormatStyle() {
            boolean useCRLF = !isIndentedWithLFNewLines();

            return new GeneralFormatStyle(useCRLF);
        }
    }
}
