/*
 * Copyright 2021 the original author or authors.
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

import java.util.function.UnaryOperator;

@Value
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@With
public class HclLeftPadded<T> {
    Space before;
    T element;
    Markers markers;

    public HclLeftPadded<T> map(UnaryOperator<T> map) {
        return withElement(map.apply(element));
    }

    public enum Location {
        ATTRIBUTE_ACCESS_NAME(Space.Location.ATTRIBUTE_ACCESS_NAME),
        CONDITIONAL_TRUE(Space.Location.CONDITIONAL_TRUE_PREFIX),
        CONDITIONAL_FALSE(Space.Location.CONDITIONAL_FALSE_PREFIX),
        FOR_CONDITION(Space.Location.FOR_CONDITION),
        FOR_UPDATE(Space.Location.FOR_UPDATE),
        FOR_UPDATE_VALUE(Space.Location.FOR_UPDATE_VALUE);

        private final Space.Location beforeLocation;

        Location(Space.Location beforeLocation) {
            this.beforeLocation = beforeLocation;
        }

        public Space.Location getBeforeLocation() {
            return beforeLocation;
        }
    }

    public static <T> @Nullable HclLeftPadded<T> withElement(@Nullable HclLeftPadded<T> before, @Nullable T elements) {
        if (before == null) {
            if (elements == null) {
                return null;
            }
            return new HclLeftPadded<>(Space.EMPTY, elements, Markers.EMPTY);
        }
        if (elements == null) {
            return null;
        }
        return before.withElement(elements);
    }

    @Override
    public String toString() {
        return "HclLeftPadded(before=" + before + ", element=" + element.getClass().getSimpleName() + ')';
    }

    public static <T> HclLeftPadded<T> build(T element) {
        return new HclLeftPadded<>(Space.EMPTY, element, Markers.EMPTY);
    }
}
