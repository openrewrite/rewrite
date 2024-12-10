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
package org.openrewrite.toml.tree;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.With;
import lombok.experimental.FieldDefaults;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Markers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

/**
 * A Toml element that could have trailing space.
 *
 * @param <T>
 */
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Data
public class TomlRightPadded<T> {
    @With
    T element;

    @With
    Space after;

    @With
    Markers markers;

    public TomlRightPadded<T> map(UnaryOperator<T> map) {
        return withElement(map.apply(element));
    }

    public static <T> List<T> getElements(List<TomlRightPadded<T>> ls) {
        List<T> list = new ArrayList<>();
        for (TomlRightPadded<T> l : ls) {
            T elem = l.getElement();
            list.add(elem);
        }
        return list;
    }

    public static <P extends Toml> List<TomlRightPadded<P>> withElements(List<TomlRightPadded<P>> before, List<P> elements) {
        // a cheaper check for the most common case when there are no changes
        if (elements.size() == before.size()) {
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

        List<TomlRightPadded<P>> after = new ArrayList<>(elements.size());
        Map<UUID, TomlRightPadded<P>> beforeById = before.stream().collect(Collectors
                .toMap(j -> j.getElement().getId(), Function.identity()));

        for (P t : elements) {
            if (beforeById.get(t.getId()) != null) {
                TomlRightPadded<P> found = beforeById.get(t.getId());
                after.add(found.withElement(t));
            } else {
                after.add(new TomlRightPadded<>(t, Space.EMPTY, Markers.EMPTY));
            }
        }

        return after;
    }

    public static <T> TomlRightPadded<T> build(T element) {
        return new TomlRightPadded<>(element, Space.EMPTY, Markers.EMPTY);
    }

    @Nullable
    public static <T> TomlRightPadded<T> withElement(@Nullable TomlRightPadded<T> before, @Nullable T elements) {
        if (before == null) {
            if (elements == null) {
                return null;
            }
            return new TomlRightPadded<>(elements, Space.EMPTY, Markers.EMPTY);
        }
        if (elements == null) {
            return null;
        }
        return before.withElement(elements);
    }

    @Override
    public String toString() {
        return "TomlRightPadded(element=" + element.getClass().getSimpleName() + ", after=" + after + ')';
    }
}
