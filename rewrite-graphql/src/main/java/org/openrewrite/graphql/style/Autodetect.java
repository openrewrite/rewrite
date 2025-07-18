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
package org.openrewrite.graphql.style;

import org.openrewrite.graphql.GraphQlVisitor;
import org.openrewrite.graphql.tree.GraphQl;
import org.openrewrite.internal.lang.Nullable;

import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class Autodetect {

    static IndentsStyle autodetectIndentStyle(GraphQl.Document document) {
        FindIndentVisitor findIndent = new FindIndentVisitor();
        findIndent.visit(document, 0);

        IndentStatistics indentStatistics = findIndent.getIndentStatistics();
        return indentStatistics.getIndentStyle();
    }

    private static class FindIndentVisitor extends GraphQlVisitor<Integer> {
        private final IndentStatistics indentStatistics = new IndentStatistics();

        public IndentStatistics getIndentStatistics() {
            return indentStatistics;
        }

        @Override
        public GraphQl visitField(GraphQl.Field field, Integer integer) {
            findIndent(field.getPrefix().getWhitespace());
            return super.visitField(field, integer);
        }

        @Override
        public GraphQl visitFieldDefinition(GraphQl.FieldDefinition fieldDefinition, Integer integer) {
            findIndent(fieldDefinition.getPrefix().getWhitespace());
            return super.visitFieldDefinition(fieldDefinition, integer);
        }

        @Override
        public GraphQl visitDirective(GraphQl.Directive directive, Integer integer) {
            findIndent(directive.getPrefix().getWhitespace());
            return super.visitDirective(directive, integer);
        }

        private void findIndent(String whitespace) {
            if (!whitespace.isEmpty()) {
                char[] chars = whitespace.toCharArray();
                int spaceCount = 0;
                int tabCount = 0;
                
                for (int i = chars.length - 1; i >= 0 && (chars[i] == ' ' || chars[i] == '\t'); i--) {
                    if (chars[i] == ' ') {
                        spaceCount++;
                    } else {
                        tabCount++;
                    }
                }
                
                if (tabCount > 0) {
                    indentStatistics.useTab();
                } else if (spaceCount > 0) {
                    indentStatistics.addWidth(spaceCount);
                }
            }
        }
    }

    private static class IndentStatistics {
        private final Map<Integer, Integer> widthCount = new HashMap<>();
        private final AtomicBoolean useTabs = new AtomicBoolean(false);

        void addWidth(int width) {
            widthCount.compute(width, (k, v) -> v == null ? 1 : v + 1);
        }

        void useTab() {
            useTabs.set(true);
        }

        IndentsStyle getIndentStyle() {
            if (useTabs.get()) {
                return new IndentsStyle(1, true);
            }

            if (widthCount.isEmpty()) {
                return GraphQlDefaultStyles.indentsStyle();
            }

            // Find most common indent width
            int mostCommonWidth = 2;
            int maxCount = 0;
            
            for (Map.Entry<Integer, Integer> entry : widthCount.entrySet()) {
                // Look for multiples of common indent sizes (2, 4)
                if (entry.getKey() % 2 == 0 || entry.getKey() % 4 == 0) {
                    if (entry.getValue() > maxCount) {
                        maxCount = entry.getValue();
                        mostCommonWidth = entry.getKey() % 4 == 0 ? 4 : 2;
                    }
                }
            }

            return new IndentsStyle(mostCommonWidth, false);
        }
    }
}