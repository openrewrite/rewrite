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
package org.openrewrite.java.dataflow;

import org.openrewrite.Incubating;
import org.openrewrite.dataflow.BasicBlock;
import org.openrewrite.dataflow.ControlFlowGraph;
import org.openrewrite.dataflow.Graph;
import org.openrewrite.java.tree.Statement;

import java.util.List;

@Incubating(since = "7.22.0")
public class JavaControlFlowGraph extends ControlFlowGraph<Statement> {
    protected JavaControlFlowGraph(BasicBlock<Statement> bb, List<Graph<BasicBlock<Statement>>> children) {
        super(bb, children);
    }

    /**
     * @param statement A local control flow statement is built from either a block
     *                  or a single statement of a conditional or loop "block".
     * @return A local control flow graph.
     */
    public JavaControlFlowGraph buildLocal(Statement statement) {
        throw new UnsupportedOperationException("local control flow");
    }
}
