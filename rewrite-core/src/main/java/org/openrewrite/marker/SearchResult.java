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

import java.util.Optional;

/**
 * Mark any AST element as the result of a search. Used by search visitors to mark AST elements that
 * match the search criteria. By painting AST elements in a tree, search results can be
 * contextualized in the tree that they are found in.
 */
@Incubating(since = "7.0.0")
public interface SearchResult extends Marker {
    TreePrinter<Void> PRINTER = printer("~~>", "~~(%s)~~>");

    /**
     * @param markerText The text to be output when encountering a SearchResult with no description
     * @param markerTextWithDescription The text to be output when encountering a SearchResult with a description.
     *                                  Use "%s" inside of this string where the description text should be interpolated in.
     * @return a TreePrinter that uses the specified text for any SearchResult markers encountered on a Tree.
     */
    static <T> TreePrinter<T> printer(String markerText, String markerTextWithDescription) {
        return new TreePrinter<T>() {
            private SearchResult marker;
            private Integer mark;

            @Override
            public void doBefore(Tree tree, StringBuilder printerAcc, T unused) {
                Optional<SearchResult> marker = tree.getMarkers().findFirst(SearchResult.class);
                if (marker.isPresent()) {
                    this.marker = marker.get();
                    this.mark = printerAcc.length();
                }
            }

            @Override
            public void doAfter(Tree tree, StringBuilder printerAcc, T unused) {
                if (mark != null) {
                    for (int i = mark; i < printerAcc.length(); i++) {
                        if (!Character.isWhitespace(printerAcc.charAt(i))) {
                            printerAcc.insert(i, marker.getDescription() == null ?
                                    markerText :
                                    String.format(markerTextWithDescription, marker.getDescription()));
                            break;
                        }
                    }
                    mark = null;
                }
            }
        };
    }

    @Nullable
    String getDescription();
}
