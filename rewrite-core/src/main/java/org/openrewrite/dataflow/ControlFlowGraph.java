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

import lombok.*;
import lombok.experimental.NonFinal;
import org.openrewrite.Incubating;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Data
@AllArgsConstructor
@Getter(AccessLevel.PROTECTED)
@Setter(AccessLevel.PRIVATE)
@Incubating(since = "7.22.0")
public class ControlFlowGraph<S> {
    BasicBlock<S> basicBlock;

    @NonFinal
    List<? extends ControlFlowGraph<S>> children;

    @NonFinal
    List<? extends ControlFlowGraph<S>> loops;

    public void unsafeSetChildren(List<? extends ControlFlowGraph<S>> children) {
        this.children = children;
    }

    public void unsafeSetLoops(List<? extends ControlFlowGraph<S>> loops) {
        this.loops = loops;
    }

    public Stream<BasicBlock<S>> postorder() {
        Stream.Builder<BasicBlock<S>> postorder = Stream.builder();

        Set<ControlFlowGraph<S>> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        Stack<ControlFlowGraph<S>> s = new Stack<>();
        s.add(this);

        nextStack:
        while (!s.isEmpty()) {
            ControlFlowGraph<S> current = s.peek();
            for (int i = current.children.size() - 1; i >= 0; i--) {
                ControlFlowGraph<S> child = current.children.get(i);
                if (seen.add(child)) {
                    s.add(child);
                    continue nextStack;
                }
            }
            postorder.add(current.basicBlock);
            s.pop();
        }

        return postorder.build();
    }

    public Stream<BasicBlock<S>> reversePostorder() {
        return postorder().collect(
                Collectors.collectingAndThen(
                        Collectors.toList(),
                        (List<BasicBlock<S>> l) -> {
                            Collections.reverse(l);
                            return l;
                        }
                )
        ).stream();
    }
}
