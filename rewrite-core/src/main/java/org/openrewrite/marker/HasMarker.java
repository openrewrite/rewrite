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

import org.openrewrite.Tree;
import org.openrewrite.TreeProcessor;

import java.util.HashSet;
import java.util.Set;

public class HasMarker {
    public static <T extends Tree> Set<T> find(Tree t, Class<? extends Marker> markerType) {
        Set<T> trees = new HashSet<>();
        new ListMarkersProcessor<T>(markerType).visit(t, trees);
        return trees;
    }

    private static class ListMarkersProcessor<T> extends TreeProcessor<Tree, Set<T>> {
        private final Class<? extends Marker> markerType;

        private ListMarkersProcessor(Class<? extends Marker> markerType) {
            this.markerType = markerType;
        }

        @Override
        public Tree visitEach(Tree tree, Set<T> ts) {
            if (tree.getMarkers().findFirst(markerType).isPresent()) {
                //noinspection unchecked
                ts.add((T) tree);
            }
            return super.visitEach(tree, ts);
        }
    }
}
