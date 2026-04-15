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
package org.openrewrite.yaml.style;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.Tree;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.style.GeneralFormatStyle;
import org.openrewrite.yaml.YamlIsoVisitor;
import org.openrewrite.yaml.search.FindIndentYamlVisitor;
import org.openrewrite.yaml.tree.Yaml;

public class Autodetect {
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
