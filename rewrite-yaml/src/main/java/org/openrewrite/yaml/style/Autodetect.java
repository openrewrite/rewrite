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
package org.openrewrite.yaml.style;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.style.GeneralFormatStyle;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.style.Style;
import org.openrewrite.yaml.YamlIsoVisitor;
import org.openrewrite.yaml.search.FindIndentYamlVisitor;
import org.openrewrite.yaml.tree.Yaml;

import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

import static java.util.Collections.emptySet;

public class Autodetect extends NamedStyles {
    @JsonCreator
    public Autodetect(UUID id, Collection<Style> styles) {
        super(id,
                "org.openrewrite.yaml.Autodetect",
                "Auto-detected",
                "Automatically detect styles from a repository's existing code.",
                emptySet(), styles);
    }

    public static Detector detector() {
        return new Detector();
    }

    public static IndentsStyle tabsAndIndents(Yaml yaml, IndentsStyle orElse) {
        FindIndentYamlVisitor<Integer> findIndent = new FindIndentYamlVisitor<>();
        FindSequenceIndentStyleVisitor<Integer> findSeqIndent = new FindSequenceIndentStyleVisitor<>();

        //noinspection ConstantConditions
        findIndent.visit(yaml, 0);
        //noinspection ConstantConditions
        findSeqIndent.visit(yaml, 0);

        if (findIndent.nonZeroIndents() > 0) {
            return new IndentsStyle(findIndent.getMostCommonIndent(), findSeqIndent.isIndentedSequences());
        }
        return orElse;
    }

    public static GeneralFormatStyle generalFormat(Yaml yaml) {
        FindLineFormatYamlVisitor<Integer> findLineFormat = new FindLineFormatYamlVisitor<>();

        //noinspection ConstantConditions
        findLineFormat.visit(yaml, 0);

        return new GeneralFormatStyle(!findLineFormat.isIndentedWithLFNewLines());
    }

    public static class Detector {

        private final FindIndentYamlVisitor<Integer> findIndent = new FindIndentYamlVisitor<>();
        private final FindSequenceIndentStyleVisitor<Integer> findSeqIndent = new FindSequenceIndentStyleVisitor<>();
        private final FindLineFormatYamlVisitor<Integer> findLineFormat = new FindLineFormatYamlVisitor<>();

        public Detector sample(SourceFile yaml) {
            if (yaml instanceof Yaml.Documents) {
                //noinspection ConstantConditions
                findIndent.visit(yaml, 0);
                //noinspection ConstantConditions
                findSeqIndent.visit(yaml, 0);
                //noinspection ConstantConditions
                findLineFormat.visit(yaml, 0);
            }
            return this;
        }

        public Autodetect build() {
            IndentsStyle indentsStyle;
            if (findIndent.nonZeroIndents() > 0) {
                indentsStyle = new IndentsStyle(findIndent.getMostCommonIndent(), findSeqIndent.isIndentedSequences());
            } else {
                indentsStyle = YamlDefaultStyles.indents();
            }

            GeneralFormatStyle generalFormatStyle = new GeneralFormatStyle(!findLineFormat.isIndentedWithLFNewLines());

            return new Autodetect(Tree.randomId(), Arrays.asList(
                    indentsStyle,
                    generalFormatStyle
            ));
        }
    }

    /**
     * Detects whether sequences use indented style (dash indented from parent key)
     * or same-column style (dash at same column as parent key).
     */
    private static class FindSequenceIndentStyleVisitor<P> extends YamlIsoVisitor<P> {
        private int indentedCount;
        private int sameColumnCount;

        /**
         * Returns {@code true} if the document uses indented sequence style (or has no sequences to detect).
         */
        public boolean isIndentedSequences() {
            return sameColumnCount <= indentedCount;
        }

        @Override
        public Yaml.Sequence.Entry visitSequenceEntry(Yaml.Sequence.Entry entry, P p) {
            String prefix = entry.getPrefix();
            if (StringUtils.hasLineBreak(prefix)) {
                int entryIndent = findIndent(prefix);

                // Find the parent mapping entry indent
                Cursor parentMappingEntry = getCursor().dropParentUntil(
                        c -> c instanceof Yaml.Mapping.Entry || c instanceof Yaml.Document);
                if (parentMappingEntry.getValue() instanceof Yaml.Mapping.Entry) {
                    Yaml.Mapping.Entry parentEntry = parentMappingEntry.getValue();
                    int parentIndent = StringUtils.hasLineBreak(parentEntry.getPrefix()) ?
                            findIndent(parentEntry.getPrefix()) : 0;
                    if (entryIndent == parentIndent) {
                        sameColumnCount++;
                    } else if (entryIndent > parentIndent) {
                        indentedCount++;
                    }
                }
            }
            return super.visitSequenceEntry(entry, p);
        }

        private static int findIndent(String prefix) {
            int size = 0;
            for (char c : prefix.toCharArray()) {
                size++;
                if (c == '\n' || c == '\r') {
                    size = 0;
                }
            }
            return size;
        }
    }

    private static class FindLineFormatYamlVisitor<P> extends YamlIsoVisitor<P> {
        private int linesWithCRLFNewLines = 0;
        private int linesWithLFNewLines = 0;

        public boolean isIndentedWithLFNewLines() {
            return linesWithLFNewLines >= linesWithCRLFNewLines;
        }

        @Override
        public @Nullable Yaml visit(@Nullable Tree tree, P p) {
            Yaml y = super.visit(tree, p);
            if (y != null) {
                if (y.getPrefix().startsWith("\r\n")) {
                    linesWithCRLFNewLines++;
                } else if (y.getPrefix().startsWith("\n")) {
                    linesWithLFNewLines++;
                }
            }
            return y;
        }
    }
}
