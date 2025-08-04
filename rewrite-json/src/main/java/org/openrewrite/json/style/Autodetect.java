/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.json.style;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.jspecify.annotations.Nullable;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.json.JsonVisitor;
import org.openrewrite.json.tree.Json;
import org.openrewrite.json.tree.JsonValue;
import org.openrewrite.style.GeneralFormatStyle;
import org.openrewrite.style.LineWrapSetting;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.style.Style;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Collections.emptySet;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;

public class Autodetect extends NamedStyles {
    @JsonCreator
    public Autodetect(UUID id, Collection<Style> styles) {
        super(id,
                "org.openrewrite.json.Autodetect",
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
        private final WrappingStatistics wrappingStatistics = new WrappingStatistics();
        private final FindIndentJsonVisitor findIndentVisitor = new FindIndentJsonVisitor();
        private final FindLineFormatJsonVisitor findLineFormatVisitor = new FindLineFormatJsonVisitor();
        private final ClassifyWrappingJsonVisitor classifyWrappingVisitor = new ClassifyWrappingJsonVisitor();

        public Detector sample(SourceFile json) {
            if(json instanceof Json.Document) {
                findIndentVisitor.visit(json, indentStatistics);
                findLineFormatVisitor.visit(json, generalFormatStatistics);
                classifyWrappingVisitor.visit(json, wrappingStatistics);
            }
            return this;
        }

        public Autodetect build() {
            return new Autodetect(Tree.randomId(), Arrays.asList(
                    indentStatistics.getTabsAndIndentsStyle(),
                    generalFormatStatistics.getFormatStyle(),
                    wrappingStatistics.getWrappingAndBracesStyle()
                    ));
        }
    }

    private static class IndentStatistics {
        IndentFrequencies indentFrequencies = new IndentFrequencies();

        public TabsAndIndentsStyle getTabsAndIndentsStyle() {
            return indentFrequencies.getTabsAndIndentsStyle();
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

    private static class WrappingStatistics {
        private Map<Boolean, AtomicInteger> arraysWrapped = new HashMap<>();
        private Map<Boolean, AtomicInteger> objectsWrapped = new HashMap<>();

        private WrappingStatistics() {
            arraysWrapped.put(Boolean.FALSE, new AtomicInteger());
            arraysWrapped.put(Boolean.TRUE, new AtomicInteger());
            objectsWrapped.put(Boolean.FALSE, new AtomicInteger());
            objectsWrapped.put(Boolean.TRUE, new AtomicInteger());
        }

        private WrappingAndBracesStyle getWrappingAndBracesStyle() {
            return new WrappingAndBracesStyle(
                    objectsWrapped.get(Boolean.TRUE).get() >= objectsWrapped.get(Boolean.FALSE).get() ? LineWrapSetting.WrapAlways : LineWrapSetting.DoNotWrap,
                    arraysWrapped.get(Boolean.TRUE).get() >= arraysWrapped.get(Boolean.FALSE).get() ? LineWrapSetting.WrapAlways : LineWrapSetting.DoNotWrap
            );
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
                    useTabs ? 1 : indent
            );
        }
    }

    private static class FindLineFormatJsonVisitor extends JsonVisitor<GeneralFormatStatistics> {
        @Override
        public @Nullable Json visit(@Nullable Tree tree, GeneralFormatStatistics stats) {
            if (tree instanceof Json) {
                String prefix = ((Json) tree).getPrefix().getWhitespace();
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

    private static class FindIndentJsonVisitor extends JsonVisitor<IndentStatistics> {
        @Override
        public Json visitMember(Json.Member member, IndentStatistics stats) {
            measureFrequencies(member.getPrefix().getWhitespace(), stats.indentFrequencies);
            return super.visitMember(member, stats);
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
                        .collect(groupingBy(identity(), counting()));

                if (indentTypeCounts.getOrDefault(true, 0L) >= indentTypeCounts.getOrDefault(false, 0L)) {
                    frequencies.linesWithSpaceIndents++;
                } else {
                    frequencies.linesWithTabIndents++;
                }
            }
        }
    }
    private static class ClassifyWrappingJsonVisitor extends JsonVisitor<WrappingStatistics> {


        @Override
        public Json visitArray(Json.Array array, WrappingStatistics wrappingStatistics) {
            for (JsonValue value: array.getValues()) {
                boolean isWrapped = value.getPrefix().getWhitespace().contains("\n");
                wrappingStatistics.arraysWrapped.get(isWrapped).incrementAndGet();
            }
            return super.visitArray(array, wrappingStatistics);
        }

        @Override
        public Json visitObject(Json.JsonObject obj, WrappingStatistics wrappingStatistics) {
            for (Json member: obj.getMembers()) {
                boolean isWrapped = member.getPrefix().getWhitespace().contains("\n");
                wrappingStatistics.objectsWrapped.get(isWrapped).incrementAndGet();
            }
            return super.visitObject(obj, wrappingStatistics);
        }
    }
}
