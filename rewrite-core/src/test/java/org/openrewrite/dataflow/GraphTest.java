/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.dataflow;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public class GraphTest {
    Graph<Integer> bottom = new Graph<>(4, emptyList());
    Graph<Integer> loop;

    {
        Graph<Integer> lastStatementInLoop = new Graph<>(5, emptyList());
        loop = new Graph<>(2,
                Arrays.asList(
                        lastStatementInLoop,
                        bottom
                )
        );
        lastStatementInLoop.setChildren(singletonList(loop));
    }

    Graph<Integer> g = new Graph<>(1, Arrays.asList(
            loop,
            new Graph<>(3, singletonList(bottom))
    ));

    /* (https://asciiflow.com)
     *     1
     *     │
     *   ┌─┴─┐
     *   ▼   ▼
     * ┌►2   3
     * │ │   │
     * │ ▼   │
     * └─5   │
     *   │   │
     *   └►4◄┘
     */

    @Test
    void postorder() {
        assertThat(g.postorder()).containsExactly(4, 3, 5, 2, 1);
    }

    // NOTE Preorder != reverse postorder
    // NOTE Possible to visit either right-to-left or left-to-right the subtrees of a given vertex. Visiting
    //      right-to-left in reverse postorder visits the control flow graph in the order conditions are evaluated in
    //      at runtime.
    @Test
    void reversePostorder() {
        assertThat(g.reversePostorder()).containsExactly(1, 2, 5, 3, 4);
    }
}
