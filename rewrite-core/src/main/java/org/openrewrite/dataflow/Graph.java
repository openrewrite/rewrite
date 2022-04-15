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
import org.openrewrite.Incubating;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Data
@AllArgsConstructor
@Getter(AccessLevel.PROTECTED)
@Incubating(since = "7.22.0")
public class Graph<T> {
    final T t;

    List<Graph<T>> children;

    public Stream<T> postorder() {
        Stream.Builder<T> postorder = Stream.builder();

        Set<Graph<T>> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        Stack<Graph<T>> s = new Stack<>();
        s.add(this);

        nextStack:
        while (!s.isEmpty()) {
            Graph<T> current = s.peek();
            for (int i = current.children.size() - 1; i >= 0; i--) {
                Graph<T> child = current.children.get(i);
                if (seen.add(child)) {
                    s.add(child);
                    continue nextStack;
                }
            }
            postorder.add(current.t);
            s.pop();
        }

        return postorder.build();
    }

    public Stream<T> reversePostorder() {
        return postorder().collect(
                Collectors.collectingAndThen(
                        Collectors.toList(),
                        (List<T> l) -> {
                            Collections.reverse(l);
                            return l;
                        }
                )
        ).stream();
    }
}
