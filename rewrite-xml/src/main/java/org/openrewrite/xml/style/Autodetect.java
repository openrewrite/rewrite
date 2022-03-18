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

    public static Autodetect detect(List<Xml.Document> xmls) {
        IndentStatistics indentStatistics = new IndentStatistics();
        GeneralFormatStatistics generalFormatStatistics = new GeneralFormatStatistics();

        for (Xml.Document xml : xmls) {
            new FindIndentXmlVisitor().visit(xml, indentStatistics);
            new FindLineFormatJavaVisitor().visit(xml, generalFormatStatistics);
        }

        return new Autodetect(Tree.randomId(), Arrays.asList(
                indentStatistics.getTabsAndIndentsStyle(),
                generalFormatStatistics.getFormatStyle()));
    }

    private static class IndentStatistics {
        private final Map<Integer, Long> indentFrequencies = new HashMap<>();
        private int linesWithSpaceIndents = 0;
        private int linesWithTabIndents = 0;

        public boolean isIndentedWithSpaces() {
            return linesWithSpaceIndents >= linesWithTabIndents;
        }

        public TabsAndIndentsStyle getTabsAndIndentsStyle() {
            boolean useTabs = !isIndentedWithSpaces();

            Map.Entry<Integer, Long> i1 = null;
            Map.Entry<Integer, Long> i2 = null;

            for (Map.Entry<Integer, Long> sample : indentFrequencies.entrySet()) {
                if (sample.getKey() == 0) {
                    continue;
                }
                if (i1 == null || i1.getValue() < sample.getValue()) {
                    i1 = sample;
                } else if (i2 == null || i2.getValue() < sample.getValue()) {
                    i2 = sample;
                }
            }

            int indent1 = i1 == null ? 4 : i1.getKey();
            int indent2 = i2 == null ? indent1 : i2.getKey();

            int indent = Math.min(indent1, indent2);

            return new TabsAndIndentsStyle(
                    useTabs,
                    useTabs ? indent : 1,
                    useTabs ? 1 : indent
            );
        }
    }

    private static class FindLineFormatJavaVisitor extends XmlVisitor<GeneralFormatStatistics> {
        @Override
        public @Nullable Xml visit(@Nullable Tree tree, GeneralFormatStatistics stats) {
            if(tree instanceof Xml) {
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
        public Xml preVisit(Xml tree, IndentStatistics stats) {
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

                stats.indentFrequencies.merge(indent, 1L, Long::sum);

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
                    stats.linesWithSpaceIndents++;
                } else {
                    stats.linesWithTabIndents++;
                }
            }

            return tree;
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
