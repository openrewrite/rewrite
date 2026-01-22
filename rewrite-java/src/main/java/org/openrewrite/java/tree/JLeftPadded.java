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
/*
 * CopyLeft 2020 the original author or authors.
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
package org.openrewrite.java.tree;

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.With;
import org.jspecify.annotations.Nullable;
import org.openrewrite.marker.Markers;

import java.util.function.UnaryOperator;

/**
 * A Java element that could have space preceding some delimiter.
 * For example an array dimension could have space before the opening
 * bracket, and the containing {@link #element} could have a prefix that occurs
 * after the bracket.
 *
 * @param <T>
 */
@Value
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@With
public class JLeftPadded<T> {

    Space before;
    T element;
    Markers markers;

    public JLeftPadded<T> map(UnaryOperator<T> map) {
        return withElement(map.apply(element));
    }

    public enum Location {
        ASSERT_DETAIL(Space.Location.ASSERT_DETAIL_PREFIX),
        ASSIGNMENT(Space.Location.ASSIGNMENT),
        ASSIGNMENT_OPERATION_OPERATOR(Space.Location.ASSIGNMENT_OPERATION_OPERATOR),
        BINARY_OPERATOR(Space.Location.BINARY_OPERATOR),
        CLASS_KIND(Space.Location.CLASS_KIND),
        EXTENDS(Space.Location.EXTENDS),
        FIELD_ACCESS_NAME(Space.Location.FIELD_ACCESS_NAME),
        IMPORT_ALIAS_PREFIX(Space.Location.IMPORT_ALIAS_PREFIX),
        LANGUAGE_EXTENSION(Space.Location.LANGUAGE_EXTENSION),
        MEMBER_REFERENCE_NAME(Space.Location.MEMBER_REFERENCE_NAME),
        METHOD_DECLARATION_DEFAULT_VALUE(Space.Location.METHOD_DECLARATION_DEFAULT_VALUE),
        STATIC_IMPORT(Space.Location.STATIC_IMPORT),
        TERNARY_TRUE(Space.Location.TERNARY_TRUE),
        TERNARY_FALSE(Space.Location.TERNARY_FALSE),
        TRY_FINALLY(Space.Location.TRY_FINALLY),
        UNARY_OPERATOR(Space.Location.UNARY_OPERATOR),
        VARIABLE_INITIALIZER(Space.Location.VARIABLE_INITIALIZER),
        WHILE_CONDITION(Space.Location.WHILE_CONDITION),
        WILDCARD_BOUND(Space.Location.WILDCARD_BOUND);

        private final Space.Location beforeLocation;

        Location(Space.Location beforeLocation) {
            this.beforeLocation = beforeLocation;
        }

        public Space.Location getBeforeLocation() {
            return beforeLocation;
        }
    }

    public static <T> @Nullable JLeftPadded<T> withElement(@Nullable JLeftPadded<T> before, @Nullable T element) {
        if (element == null) {
            return null;
        }
        if (before == null) {
            return new JLeftPadded<>(Space.EMPTY, element, Markers.EMPTY);
        }
        return before.withElement(element);
    }

    @Override
    public String toString() {
        return "JLeftPadded(before=" + before + ", element=" + element + ')';
    }

    public static <T> JLeftPadded<T> build(T element) {
        return new JLeftPadded<>(Space.EMPTY, element, Markers.EMPTY);
    }
}
