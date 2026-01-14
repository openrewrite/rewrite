/*
 * Copyright 2024 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.python.tree;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

public class PyRightPadded {
    @Getter
    @RequiredArgsConstructor
    public enum Location {
        ASSERT_ELEMENT(PySpace.Location.ASSERT_ELEMENT_SUFFIX),
        CHAINED_ASSIGNMENT_VARIABLES(PySpace.Location.CHAINED_ASSIGNMENT_VARIABLE_SUFFIX),
        COLLECTION_LITERAL_ELEMENT(PySpace.Location.COLLECTION_LITERAL_ELEMENT_SUFFIX),
        COMPILATION_UNIT_STATEMENTS(PySpace.Location.COMPILATION_UNIT_STATEMENT_SUFFIX),
        COMPREHENSION_EXPRESSION_CLAUSE_ASYNC(PySpace.Location.COMPREHENSION_EXPRESSION_CLAUSE_ASYNC_SUFFIX),
        DEL_TARGETS(PySpace.Location.DEL_TARGET_SUFFIX),
        DICT_ENTRY_KEY(PySpace.Location.DICT_ENTRY_KEY_SUFFIX),
        DICT_LITERAL_ELEMENT(PySpace.Location.DICT_LITERAL_ELEMENT_SUFFIX),
        FORMATTED_STRING_VALUE_DEBUG(PySpace.Location.FORMATTED_STRING_VALUE_DEBUG_SUFFIX),
        FORMATTED_STRING_VALUE_EXPRESSION(PySpace.Location.FORMATTED_STRING_VALUE_EXPRESSION_SUFFIX),
        KEY_VALUE_KEY(PySpace.Location.KEY_VALUE_SUFFIX),
        MATCH_CASE_PATTERN_CHILD(PySpace.Location.MATCH_PATTERN_ELEMENT_SUFFIX),
        MULTI_IMPORT_FROM(PySpace.Location.MULTI_IMPORT_FROM_SUFFIX),
        MULTI_IMPORT_NAME(PySpace.Location.MULTI_IMPORT_NAME_SUFFIX),
        SLICE_START(PySpace.Location.SLICE_START_SUFFIX),
        SLICE_STEP(PySpace.Location.SLICE_STEP_SUFFIX),
        SLICE_STOP(PySpace.Location.SLICE_STOP_SUFFIX),
        TOP_LEVEL_STATEMENT_SUFFIX(PySpace.Location.TOP_LEVEL_STATEMENT),
        UNION_TYPE_TYPES(PySpace.Location.UNION_ELEMENT_SUFFIX),
        VARIABLE_SCOPE_NAMES(PySpace.Location.VARIABLE_SCOPE_NAME_SUFFIX),
        ;

        private final PySpace.Location afterLocation;
    }
}
