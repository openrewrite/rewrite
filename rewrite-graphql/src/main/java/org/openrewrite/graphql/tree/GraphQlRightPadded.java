/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.graphql.tree;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.With;
import lombok.experimental.FieldDefaults;
import org.openrewrite.marker.Markers;

import java.util.*;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

/**
 * A GraphQL element that could have trailing space.
 *
 * @param <T>
 */
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Data
public class GraphQlRightPadded<T extends GraphQl> {
    @With
    T element;

    @With
    Space after;

    @With
    Markers markers;

    public GraphQlRightPadded<T> map(UnaryOperator<T> map) {
        return withElement(map.apply(element));
    }

    public static <T extends GraphQl> List<T> getElements(List<GraphQlRightPadded<T>> ls) {
        List<T> list = new ArrayList<>();
        for (GraphQlRightPadded<T> l : ls) {
            T elem = l.getElement();
            list.add(elem);
        }
        return list;
    }

    public static <J2 extends GraphQl> List<GraphQlRightPadded<J2>> withElements(List<GraphQlRightPadded<J2>> before, List<J2> elements) {
        // a cheaper check for the most common case when there are no changes
        boolean hasSizeChanged = elements.size() != before.size();
        if (!hasSizeChanged) {
            boolean hasChanges = false;
            for (int i = 0; i < before.size(); i++) {
                if (before.get(i).getElement() != elements.get(i)) {
                    hasChanges = true;
                    break;
                }
            }
            if (!hasChanges) {
                return before;
            }
        }

        List<GraphQlRightPadded<J2>> after = new ArrayList<>(elements.size());
        Map<UUID, GraphQlRightPadded<J2>> beforeById = before.stream().collect(Collectors
                .toMap(j -> j.getElement().getId(), Function.identity()));

        for (J2 t : elements) {
            GraphQlRightPadded<J2> found = beforeById.get(t.getId());
            if (found != null) {
                after.add(found.withElement(t));
            } else {
                after.add(new GraphQlRightPadded<>(t, Space.EMPTY, Markers.EMPTY));
            }
        }

        return after;
    }

    public static <T extends GraphQl> GraphQlRightPadded<T> build(T element) {
        return new GraphQlRightPadded<>(element, Space.EMPTY, Markers.EMPTY);
    }

    @Override
    public String toString() {
        return "GraphQlRightPadded(element=" + element.getClass().getSimpleName() + ", after=" + after + ')';
    }
}