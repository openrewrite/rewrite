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
package org.openrewrite.java.controlflow;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@AllArgsConstructor(staticName = "forGraph", access = AccessLevel.PACKAGE)
public final class ControlFlowSummary {
    private final ControlFlowNode.Start start;
    private final ControlFlowNode.End end;

    public Set<ControlFlowNode.BasicBlock> getBasicBlocks() {
        return getBasicBlocks(start).collect(Collectors.toSet());
    }

    public Set<ControlFlowNode.ConditionNode> getConditionNodes() {
        return getConditionNodes(start).collect(Collectors.toSet());
    }

    private Stream<ControlFlowNode.BasicBlock> getBasicBlocks(ControlFlowNode controlFlowNode) {
        return controlFlowNode.getSuccessors().stream().flatMap(cfn -> {
            if (cfn instanceof ControlFlowNode.BasicBlock) {
                return Stream.concat(
                        Stream.of((ControlFlowNode.BasicBlock) cfn),
                        getBasicBlocks(cfn)
                );
            } else {
                return getBasicBlocks(cfn);
            }
        });
    }

    private Stream<ControlFlowNode.ConditionNode> getConditionNodes(ControlFlowNode controlFlowNode) {
        return controlFlowNode.getSuccessors().stream().flatMap(cfn -> {
            if (cfn instanceof ControlFlowNode.ConditionNode) {
                return Stream.concat(
                        Stream.of((ControlFlowNode.ConditionNode) cfn),
                        getConditionNodes(cfn)
                );
            } else {
                return getConditionNodes(cfn);
            }
        });
    }

    int getBasicBlockCount() {
        return getBasicBlocks().size();
    }

    int getConditionNodeCount() {
        return getConditionNodes().size();
    }

    int getExitCount() {
        return end.getPredecessors().size();
    }
}
