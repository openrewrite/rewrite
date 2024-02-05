/*
 * Copyright 2023 the original author or authors.
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

import org.junit.jupiter.api.Test;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Markers;
import org.openrewrite.quark.Quark;

import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;


class TreeVisitorTest {

    @Test
    void scheduleAfterOnVisitWithCursor() {
        Quark quark = new Quark(Tree.randomId(), Paths.get("quark"), Markers.EMPTY, null, null);
        AtomicInteger visited = new AtomicInteger(0);
        TreeVisitor<Tree, Integer> scheduled = new TreeVisitor<>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, Integer i) {
                assertThat(tree).isSameAs(quark);
                assertThat(getCursor()).isSameAs(getCursor().getRoot());
                visited.incrementAndGet();
                return tree;
            }
        };
        TreeVisitor<Tree, Integer> visitor = new TreeVisitor<>() {
            @Override
            public Tree preVisit(Tree tree, Integer i) {
                stopAfterPreVisit();
                doAfterVisit(scheduled);
                return tree;
            }
        };

        visitor.visit(quark, 0);
        assertThat(visited).hasValue(1);

        visitor.visit(quark, 0);
        assertThat(visited).hasValue(2);
    }
}
