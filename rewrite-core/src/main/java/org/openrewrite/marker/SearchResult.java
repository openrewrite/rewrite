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
package org.openrewrite.marker;

import org.openrewrite.Incubating;
import org.openrewrite.Tree;
import org.openrewrite.TreePrinter;
import org.openrewrite.internal.lang.Nullable;

/**
 * Mark any AST element with "paint". Used by search visitors to mark AST elements that
 * match the search criteria. By painting AST elements in a tree, search results can be
 * contextualized in the tree that they are found in.
 */
@Incubating(since = "7.0.0")
public interface SearchResult extends Marker {

    @Nullable
    String getDescription();

    /**
     * Most SearchResult implementations have a default printed representation, which is returned by their print() method.
     * For example:
     * Normally RecipeSearchResult.print() will return the empty string.
     * Normally JavaSearchResult.print() will return something like an arrow surrounded by a comment.
     * TreePrinters returned by this method overwrite the default printed form of SearchResults with the text you specify.
     *
     * @param markerText The text to be emitted when printing a SearchResult with no description
     * @param markerTextWithDescription The text to be emitted when printing a SearchResult that has a description.
     *                                  Use "%s" to specify where the description should be printed.
     * @return A TreePrinter which will overwrite the natural result of Marker.print() with markerText/markerTextDescription
     */
    static TreePrinter<Void> printer(String markerText, String markerTextWithDescription) {
        return new TreePrinter<Void>() {
            private SearchResult marker;
            private Integer mark;

            @Override
            public void doBefore(Tree tree, StringBuilder printerAcc, Void unused) {
                if (tree instanceof SearchResult) {
                    this.marker = (SearchResult)tree;
                    this.mark = printerAcc.length();
                }
            }

            @Override
            public void doAfter(Tree tree, StringBuilder printerAcc, Void unused) {
                if (mark != null) {
                    printerAcc.delete(mark, printerAcc.length());
                    printerAcc.append(marker.getDescription() == null ?
                            markerText :
                            String.format(markerTextWithDescription, marker.getDescription()));
                    mark = null;
                }
            }
        };
    }
}
