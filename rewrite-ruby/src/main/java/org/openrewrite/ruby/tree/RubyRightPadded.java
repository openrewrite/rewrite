/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.ruby.tree;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

public class RubyRightPadded {

    @RequiredArgsConstructor(access = lombok.AccessLevel.PRIVATE)
    @Getter
    public enum Location {
        ARRAY_ELEMENTS_SUFFIX(RubySpace.Location.LIST_LITERAL_SUFFIX),
        BLOCK_PARAMETERS_SUFFIX(RubySpace.Location.BLOCK_PARAMETERS_SUFFIX),
        BLOCK_STATEMENT(RubySpace.Location.BLOCK_STATEMENT_SUFFIX),
        CLASS_METHOD_DECLARATION_PARAMETERS_SUFFIX(RubySpace.Location.CLASS_METHOD_DECLARATION_PARAMETER_SUFFIX),
        COMPILATION_UNIT_STATEMENT_SUFFIX(RubySpace.Location.COMPILATION_UNIT_STATEMENT_SUFFIX),
        DELIMITED_ARRAY_ELEMENT_SUFFIX(RubySpace.Location.DELIMITED_ARRAY_ELEMENT_SUFFIX),
        HASH_ELEMENTS_SUFFIX(RubySpace.Location.HASH_ELEMENTS_SUFFIX),
        MULTIPLE_ASSIGNMENT_INITIALIZERS_SUFFIX(RubySpace.Location.MULTIPLE_ASSIGNMENT_INITIALIZER_SUFFIX),
        MULTIPLE_ASSIGNMENT_SUFFIX(RubySpace.Location.MULTIPLE_ASSIGNMENT_SUFFIX),
        NUMERIC_VALUE_SUFFIX(RubySpace.Location.NUMERIC_VALUE_SUFFIX),
        COMPLEX_STRING_STRINGS_SUFFIX(RubySpace.Location.COMPLEX_STRING_STRINGS_SUFFIX),
        RESCUE_TYPE_SUFFIX(RubySpace.Location.RESCUE_TYPE_SUFFIX),
        STRUCT_EXTENDS_ARGUMENTS_SUFFIX(RubySpace.Location.STRUCT_EXTENDS_ARGUMENTS_SUFFIX),
        STRUCT_PATTERN_ELEMENT_SUFFIX(RubySpace.Location.STRUCT_PATTERN_ELEMENT_SUFFIX), YIELD_DATA_SUFFIX(RubySpace.Location.YIELD_DATA_SUFFIX);

        private final RubySpace.Location afterLocation;
    }
}
