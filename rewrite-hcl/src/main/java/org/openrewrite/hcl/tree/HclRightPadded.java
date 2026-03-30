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
package org.openrewrite.hcl.tree;

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.With;
import org.jspecify.annotations.Nullable;
import org.openrewrite.marker.Markers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import static java.util.stream.Collectors.toMap;

/**
 * An HCL element that could have trailing space.
 *
 * @param <T> The type of instance that is being padded.
 */
@Value
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@With
public class HclRightPadded<T> {
    T element;
    Space after;
    Markers markers;

    public HclRightPadded<T> map(UnaryOperator<T> map) {
        return withElement(map.apply(element));
    }

    public enum Location {
        FOR_VARIABLE_ARGUMENT(Space.Location.FOR_VARIABLE_SUFFIX),
        FUNCTION_CALL_ARGUMENT(Space.Location.FUNCTION_CALL_ARGUMENT_SUFFIX),
        INDEX_POSITION(Space.Location.INDEX_POSITION_SUFFIX),
        LEGACY_INDEX_ATTRIBUTE_ACCESS_BASE(Space.Location.LEGACY_INDEX_ATTRIBUTE_ACCESS_BASE),
        OBJECT_VALUE_ARGUMENT(Space.Location.OBJECT_VALUE_ATTRIBUTE_SUFFIX),
        PARENTHESES(Space.Location.PARENTHESES_SUFFIX),
        SPLAT_OPERATOR(Space.Location.SPLAT_OPERATOR_SUFFIX),
        TEMPLATE_INTERPOLATION(Space.Location.TEMPLATE_INTERPOLATION_SUFFIX),
        TUPLE_VALUE(Space.Location.TUPLE_VALUE_SUFFIX);

        private final Space.Location afterLocation;

        Location(Space.Location afterLocation) {
            this.afterLocation = afterLocation;
        }

        public Space.Location getAfterLocation() {
            return afterLocation;
        }
    }

    public static <T> List<T> getElements(List<HclRightPadded<T>> ls) {
        List<T> list = new ArrayList<>();
        for (HclRightPadded<T> l : ls) {
            T elem = l.getElement();
            list.add(elem);
        }
        return list;
    }

    public static <T> @Nullable HclRightPadded<T> withElement(@Nullable HclRightPadded<T> before, @Nullable T elements) {
        if (before == null) {
            if (elements == null) {
                return null;
            }
            return new HclRightPadded<>(elements, Space.EMPTY, Markers.EMPTY);
        }
        if (elements == null) {
            return null;
        }
        return before.withElement(elements);
    }

    public static <H extends Hcl> List<HclRightPadded<H>> withElements(List<HclRightPadded<H>> before, List<H> elements) {
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

        List<HclRightPadded<H>> after = new ArrayList<>(elements.size());
        Map<UUID, HclRightPadded<H>> beforeById = before.stream().collect(toMap(j -> j.getElement().getId(), Function.identity()));

        for (H t : elements) {
            if (beforeById.get(t.getId()) != null) {
                HclRightPadded<H> found = beforeById.get(t.getId());
                after.add(found.withElement(t));
            } else {
                after.add(new HclRightPadded<>(t, Space.EMPTY, Markers.EMPTY));
            }
        }

        return after;
    }

    public static <T> HclRightPadded<T> build(T element) {
        return new HclRightPadded<>(element, Space.EMPTY, Markers.EMPTY);
    }

    @Override
    public String toString() {
        return "HclRightPadded`(element=" + element.getClass().getSimpleName() + ", after=" + after + ')';
    }
}
