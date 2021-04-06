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
package org.openrewrite;

import org.openrewrite.marker.SearchResult;

import java.util.Optional;

public interface TreePrinter {

    /**
     * Prints out the input it was given with no modifications.
     */
    TreePrinter IDENTITY = new TreePrinter() {
        private SearchResult marker;
        private Integer mark;

        @Override
        public void doBefore(Tree tree, StringBuilder printerAcc, Object unused) {
            Optional<SearchResult> marker = tree.getMarkers().findFirst(SearchResult.class);
            if (marker.isPresent()) {
                this.marker = marker.get();
                this.mark = printerAcc.length();
            }
        }

        @Override
        public void doAfter(Tree tree, StringBuilder printerAcc, Object unused) {
            if (mark != null) {
                for (int i = mark; i < printerAcc.length(); i++) {
                    if (!Character.isWhitespace(printerAcc.charAt(i))) {
                        printerAcc.insert(i, marker.print());
                        break;
                    }
                }
                mark = null;
            }
        }
    };

    static TreePrinter identity() {
        return IDENTITY;
    }

    /**
     * Called before tree is printed, allows printing additional output before tree.
     *
     * @param tree AST element that is about to be printed
     * @param printerAcc Printer accumulator, can be appended to, contains everything written for the overall visit
     *                   operation so far
     * @param p visit context
     */
    default void doBefore(Tree tree, StringBuilder printerAcc, Object p) {}

    /**
     * Called after tree has been printed, allows printing additional output after tree.
     *
     * @param tree AST element that has just been printed
     * @param printerAcc Printer accumulator, can be appended to, contains everything written for the overall visit
     *      *                   operation so far
     * @param p visit context
     */
    default void doAfter(Tree tree, StringBuilder printerAcc, Object p) {}


}
