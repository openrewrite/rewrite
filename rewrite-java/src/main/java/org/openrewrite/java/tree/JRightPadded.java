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

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.With;
import lombok.experimental.FieldDefaults;
import org.openrewrite.marker.Markable;
import org.openrewrite.marker.Markers;

import java.util.function.Function;

/**
 * A Java element that could have trailing space.
 *
 * @param <T>
 */
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Data
public class JRightPadded<T> implements Markable {
    @With
    T elem;

    @With
    Space after;

    @With
    Markers markers;

    public JRightPadded<T> map(Function<T, T> map) {
        return withElem(map.apply(elem));
    }

    public enum Location {
        ANNOTATION_ARGUMENT(Space.Location.ANNOTATION_ARGUMENT_SUFFIX),
        ARRAY_INDEX(Space.Location.ARRAY_INDEX_SUFFIX),
        BLOCK_STATEMENT(Space.Location.BLOCK_STATEMENT_SUFFIX),
        CASE(Space.Location.CASE_SUFFIX),
        CATCH_ALTERNATIVE(Space.Location.CATCH_ALTERNATIVE_SUFFIX),
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
        IMPORT(Space.Location.IMPORT_SUFFIX),
        INSTANCEOF(Space.Location.INSTANCEOF_SUFFIX),
        METHOD_DECL_ARGUMENT(Space.Location.METHOD_DECL_ARGUMENT_SUFFIX),
        METHOD_INVOCATION_ARGUMENT(Space.Location.METHOD_INVOCATION_ARGUMENT_SUFFIX),
        METHOD_SELECT(Space.Location.METHOD_SELECT_SUFFIX),
        NAMED_VARIABLE(Space.Location.NAMED_VARIABLE_SUFFIX),
        NEW_ARRAY_INITIALIZER(Space.Location.NEW_ARRAY_INITIALIZER_SUFFIX),
        NEW_CLASS_ARGS(Space.Location.NEW_CLASS_ARGS_SUFFIX),
        PACKAGE(Space.Location.PACKAGE_SUFFIX),
        PARENTHESES(Space.Location.PARENTHESES_SUFFIX),
        STATIC_INIT(Space.Location.STATIC_INIT_SUFFIX),
        THROWS(Space.Location.THROWS_SUFFIX),
        TRY_RESOURCES(Space.Location.TRY_RESOURCES_SUFFIX),
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
}
