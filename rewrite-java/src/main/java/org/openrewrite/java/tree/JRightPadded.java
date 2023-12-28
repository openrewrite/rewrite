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
package org.openrewrite.java.tree;

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.With;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Markers;

import java.util.*;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

/**
 * A Java element that could have trailing space.
 *
 * @param <T>
 */
@Value
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@With
public class JRightPadded<T> {
    T element;
    Space after;
    Markers markers;

    public JRightPadded<T> map(UnaryOperator<T> map) {
        return withElement(map.apply(element));
    }

    public enum Location {
        ANNOTATION_ARGUMENT(Space.Location.ANNOTATION_ARGUMENT_SUFFIX),
        ARRAY_INDEX(Space.Location.ARRAY_INDEX_SUFFIX),
        BLOCK_STATEMENT(Space.Location.BLOCK_STATEMENT_SUFFIX),
        CASE(Space.Location.CASE_SUFFIX),
        CASE_EXPRESSION(Space.Location.CASE_EXPRESSION),
        CASE_BODY(Space.Location.CASE_BODY),
        CATCH_ALTERNATIVE(Space.Location.CATCH_ALTERNATIVE_SUFFIX),
        DIMENSION(Space.Location.DIMENSION_SUFFIX),
        ENUM_VALUE(Space.Location.ENUM_VALUE_SUFFIX),
        FOR_BODY(Space.Location.FOR_BODY_SUFFIX),
        FOR_CONDITION(Space.Location.FOR_CONDITION_SUFFIX),
        FOR_INIT(Space.Location.FOR_INIT_SUFFIX),
        FOR_UPDATE(Space.Location.FOR_UPDATE_SUFFIX),
        FOREACH_VARIABLE(Space.Location.FOREACH_VARIABLE_SUFFIX),
        FOREACH_ITERABLE(Space.Location.FOREACH_ITERABLE_SUFFIX),
        IF_ELSE(Space.Location.IF_ELSE_SUFFIX),
        IF_THEN(Space.Location.IF_THEN_SUFFIX),
        IMPLEMENTS(Space.Location.IMPLEMENTS_SUFFIX),
        PERMITS(Space.Location.PERMITS_SUFFIX),
        IMPORT(Space.Location.IMPORT_SUFFIX),
        INSTANCEOF(Space.Location.INSTANCEOF_SUFFIX),
        LABEL(Space.Location.LABEL_SUFFIX),
        LAMBDA_PARAM(Space.Location.LAMBDA_PARAMETER),
        LANGUAGE_EXTENSION(Space.Location.LANGUAGE_EXTENSION),
        MEMBER_REFERENCE_CONTAINING(Space.Location.MEMBER_REFERENCE_CONTAINING),
        METHOD_DECLARATION_PARAMETER(Space.Location.METHOD_DECLARATION_PARAMETER_SUFFIX),
        METHOD_INVOCATION_ARGUMENT(Space.Location.METHOD_INVOCATION_ARGUMENT_SUFFIX),
        METHOD_SELECT(Space.Location.METHOD_SELECT_SUFFIX),
        NAMED_VARIABLE(Space.Location.NAMED_VARIABLE_SUFFIX),
        NEW_ARRAY_INITIALIZER(Space.Location.NEW_ARRAY_INITIALIZER_SUFFIX),
        NEW_CLASS_ARGUMENTS(Space.Location.NEW_CLASS_ARGUMENTS_SUFFIX),
        NEW_CLASS_ENCLOSING(Space.Location.NEW_CLASS_ENCLOSING_SUFFIX),
        NULLABLE(Space.Location.NULLABLE_TYPE_SUFFIX),
        PACKAGE(Space.Location.PACKAGE_SUFFIX),
        PARENTHESES(Space.Location.PARENTHESES_SUFFIX),
        RECORD_STATE_VECTOR(Space.Location.RECORD_STATE_VECTOR_SUFFIX),
        STATIC_INIT(Space.Location.STATIC_INIT_SUFFIX),
        THROWS(Space.Location.THROWS_SUFFIX),
        TRY_RESOURCE(Space.Location.TRY_RESOURCE_SUFFIX),
        TYPE_PARAMETER(Space.Location.TYPE_PARAMETER_SUFFIX),
        TYPE_BOUND(Space.Location.TYPE_BOUND_SUFFIX),
        WHILE_BODY(Space.Location.WHILE_BODY_SUFFIX);

        private final Space.Location afterLocation;

        Location(Space.Location afterLocation) {
            this.afterLocation = afterLocation;
        }

        public Space.Location getAfterLocation() {
            return afterLocation;
        }
    }

    public static <T> List<T> getElements(@Nullable List<JRightPadded<T>> ls) {
        if (ls == null) {
            return Collections.emptyList();
        }
        List<T> list = new ArrayList<>(ls.size());
        for (JRightPadded<T> l : ls) {
            if (l == null) {
                continue;
            }
            T elem = l.getElement();
            list.add(elem);
        }
        return list;
    }

    @Nullable
    public static <T> JRightPadded<T> withElement(@Nullable JRightPadded<T> before, @Nullable T element) {
        if (before == null) {
            if (element == null) {
                return null;
            }
            return new JRightPadded<>(element, Space.EMPTY, Markers.EMPTY);
        }
        if (element == null) {
            return null;
        }
        return before.withElement(element);
    }

    public static <J2 extends J> List<JRightPadded<J2>> withElements(List<JRightPadded<J2>> before, List<J2> elements) {
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
        } else if (elements.isEmpty()) {
            return Collections.emptyList();
        }

        List<JRightPadded<J2>> after = new ArrayList<>(elements.size());
        Map<UUID, JRightPadded<J2>> beforeById = before.stream().collect(Collectors
                .toMap(j -> j.getElement().getId(), Function.identity()));

        for (J2 t : elements) {
            JRightPadded<J2> found;
            if ((found = beforeById.get(t.getId())) != null) {
                after.add(found.withElement(t));
            } else {
                after.add(new JRightPadded<>(t, Space.EMPTY, Markers.EMPTY));
            }
        }

        return after;
    }

    public static <T> JRightPadded<T> build(T element) {
        return new JRightPadded<>(element, Space.EMPTY, Markers.EMPTY);
    }

    @Override
    public String toString() {
        return "JRightPadded(element=" + element + ", after=" + after + ')';
    }
}
