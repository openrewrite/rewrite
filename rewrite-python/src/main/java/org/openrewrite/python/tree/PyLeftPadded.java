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

public class PyLeftPadded {
    @Getter
    @RequiredArgsConstructor
    public enum Location {
        BINARY_OPERATOR(PySpace.Location.BINARY_OPERATOR),
        COMPREHENSION_EXPRESSION_CLAUSE_ITERATED_LIST(PySpace.Location.COMPREHENSION_EXPRESSION_CLAUSE_ITERATED_LIST),
        ERROR_FROM_FROM(PySpace.Location.ERROR_FROM_SOURCE),
        MATCH_CASE_GUARD(PySpace.Location.MATCH_CASE_GUARD),
        NAMED_ARGUMENT_VALUE(PySpace.Location.NAMED_ARGUMENT),
        TRAILING_ELSE_WRAPPER_ELSE_BLOCK(PySpace.Location.TRAILING_ELSE_WRAPPER_ELSE_BLOCK),
        TYPE_ALIAS_VALUE(PySpace.Location.TYPE_ALIAS_VALUE),
        ;

        private final PySpace.Location beforeLocation;
    }
}
