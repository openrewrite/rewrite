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
        ANNOTATION_ARGUMENT,
        ARRAY_INDEX,
        BLOCK_STATEMENT,
        CASE,
        CATCH_ALTERNATIVE,
        ENUM_VALUE,
        FOR_BODY,
        FOR_CONDITION,
        FOR_INIT,
        FOR_UPDATE,
        FOREACH_VARIABLE,
        FOREACH_ITERABLE,
        IF_ELSE,
        IF_THEN,
        IMPLEMENTS,
        IMPORT,
        INSTANCEOF,
        METHOD_DECL_ARGUMENT,
        METHOD_INVOCATION_ARGUMENT,
        METHOD_SELECT,
        NAMED_VARIABLE,
        NEW_ARRAY_INITIALIZER,
        NEW_CLASS_ARGS,
        PACKAGE,
        PARENTHESES,
        THROWS,
        TRY_RESOURCES,
        TYPE_PARAMETER,
        TYPE_BOUND,
        WHILE_BODY
    }
}
