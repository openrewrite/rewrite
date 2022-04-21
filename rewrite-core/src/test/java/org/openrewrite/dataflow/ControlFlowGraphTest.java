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

public class ControlFlowGraphTest {
    ControlFlowGraph<Integer> bottom = new ControlFlowGraph<>(new BasicBlock<>(singletonList(4)), emptyList(), emptyList());
    ControlFlowGraph<Integer> loop;

    {
        ControlFlowGraph<Integer> lastStatementInLoop = new ControlFlowGraph<>(new BasicBlock<>(singletonList(5)), emptyList(), emptyList());
        loop = new ControlFlowGraph<>(new BasicBlock<>(singletonList(2)),
                Arrays.asList(
                        lastStatementInLoop,
                        bottom
                ),
                emptyList()
        );
        lastStatementInLoop.unsafeSetLoops(singletonList(loop));
    }

    ControlFlowGraph<Integer> g = new ControlFlowGraph<>(new BasicBlock<>(singletonList(1)), Arrays.asList(
            loop,
            new ControlFlowGraph<>(new BasicBlock<>(singletonList(3)), singletonList(bottom), emptyList())
    ), emptyList());

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
        assertThat(g.postorder().map(bb -> bb.getStatements().get(0)))
                .containsExactly(4, 3, 5, 2, 1);
    }

    // NOTE Preorder != reverse postorder
    // NOTE Possible to visit either right-to-left or left-to-right the subtrees of a given vertex. Visiting
    //      right-to-left in reverse postorder visits the control flow graph in the order conditions are evaluated in
    //      at runtime.
    @Test
    void reversePostorder() {
        assertThat(g.reversePostorder().map(bb -> bb.getStatements().get(0)))
                .containsExactly(1, 2, 5, 3, 4);
    }
}
