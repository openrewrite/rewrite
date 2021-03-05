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
package org.openrewrite.java.internal.template;

import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;

import java.util.*;

import static java.util.stream.Collectors.toList;

public class ExtractTrees {
    public static List<J> extract(J.CompilationUnit cu) {
        ExtractionContext extractionContext = new ExtractionContext();
        new ExtractTemplatedCode().visit(cu, extractionContext);
        return extractionContext.getSnippets();
    }

    /**
     * The template code is marked before/after with comments. The extraction code will grab cursor elements between
     * those two markers. Depending on insertion scope, the first element (the one that has the start marker comment)
     * may not be part of the template. The context is used to demarcate when elements should be collected, collect
     * the elements of the template (and keeping track of the depth those elements appear in the tree), and finally
     * keep track of element IDs that have already been collected (so they are not inadvertently added twice)
     */
    private static class ExtractionContext {
        private boolean collectElements = false;
        private final List<CollectedElement> collectedElements = new ArrayList<>();
        private final Set<UUID> collectedIds = new HashSet<>();

        @SuppressWarnings("unchecked")
        private <J2 extends J> List<J2> getSnippets() {
            if (collectedElements.isEmpty()) {
                return Collections.emptyList();
            } else {
                long startDepth = collectedElements.get(0).depth;
                //This returns all elements that have the same depth as the starting element.
                return collectedElements.stream()
                        .filter(e -> e.depth == startDepth)
                        .map(e -> (J2) e.element)
                        .collect(toList());
            }
        }

        /**
         * The context captures each element and it's depth in the tree.
         */
        private static class CollectedElement {
            final long depth;
            final J element;

            CollectedElement(long depth, J element) {
                this.depth = depth;
                this.element = element;
            }
        }
    }

    private static class ExtractTemplatedCode extends JavaVisitor<ExtractionContext> {

        @Override
        public Space visitSpace(Space space, Space.Location loc, ExtractionContext context) {

            long templateDepth = getCursor().getPathAsStream().count();
            if (findComment(space, JavaTemplatePrinter.SNIPPET_MARKER_END) != null) {
                //Ending marker found, stop collecting elements. NOTE: if the space was part of a prefix of an element
                //that element will not be collected.
                context.collectElements = false;

                // Remove any elements in the same scope as the one having the End Token.
                context.collectedElements.removeIf(ce -> getCursor().isScopeInPath(ce.element));
            }

            Comment startToken = findComment(space, JavaTemplatePrinter.SNIPPET_MARKER_START);
            if (startToken != null) {
                //If the starting marker is found, record the starting depth, collect the current cursor tree element,
                //remove the marker comment, and flag the extractor to start collecting all elements until the end marker
                //is found.
                context.collectElements = true;

                Object value = getCursor().getValue();
                if(!(value instanceof J)) {
                    return space;
                }
                J treeValue = (J) value;
                context.collectedIds.add(treeValue.getId());

                List<Comment> comments = new ArrayList<>(space.getComments());
                comments.remove(startToken);
                context.collectedElements.add(new ExtractionContext.CollectedElement(templateDepth, treeValue.withPrefix(space.withComments(comments))));
            } else if (context.collectElements) {
                //If collecting elements and the current cursor element has not already been collected, add it.
                if (getCursor().getValue() instanceof J) {
                    J treeValue = getCursor().getValue();
                    if (!context.collectedIds.contains(treeValue.getId())) {
                        context.collectedElements.add(new ExtractionContext.CollectedElement(templateDepth, treeValue));
                        context.collectedIds.add(treeValue.getId());
                    }
                }
            }

            return space;
        }

        @Nullable
        private Comment findComment(@Nullable Space space, String comment) {
            if (space == null) {
                return null;
            }
            return space.getComments().stream()
                    .filter(c -> Comment.Style.BLOCK.equals(c.getStyle()))
                    .filter(c -> c.getText().equals(comment))
                    .findAny()
                    .orElse(null);
        }
    }
}
