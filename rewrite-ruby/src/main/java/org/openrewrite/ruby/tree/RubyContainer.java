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

public class RubyContainer {

    @RequiredArgsConstructor(access = lombok.AccessLevel.PRIVATE)
    @Getter
    public enum Location {
        ARRAY_ELEMENTS(RubySpace.Location.ARRAY_ELEMENTS, RubyRightPadded.Location.ARRAY_ELEMENTS_SUFFIX),
        BLOCK_PARAMETERS(RubySpace.Location.BLOCK_PARAMETERS, RubyRightPadded.Location.BLOCK_PARAMETERS_SUFFIX),
        CLASS_METHOD_DECLARATION_PARAMETERS(RubySpace.Location.CLASS_METHOD_DECLARATION_PARAMETERS, RubyRightPadded.Location.CLASS_METHOD_DECLARATION_PARAMETERS_SUFFIX),
        COMPLEX_STRING_STRINGS(RubySpace.Location.COMPLEX_STRING_STRINGS, RubyRightPadded.Location.COMPLEX_STRING_STRINGS_SUFFIX),
        DELIMITED_ARRAY_ELEMENTS(RubySpace.Location.DELIMITED_ARRAY_ELEMENTS, RubyRightPadded.Location.DELIMITED_ARRAY_ELEMENT_SUFFIX),
        HASH_ELEMENTS(RubySpace.Location.HASH_ELEMENTS, RubyRightPadded.Location.HASH_ELEMENTS_SUFFIX),
        MULTIPLE_ASSIGNMENT_ASSIGNMENTS(RubySpace.Location.MULTIPLE_ASSIGNMENT_ASSIGNMENTS, RubyRightPadded.Location.MULTIPLE_ASSIGNMENT_SUFFIX),
        MULTIPLE_ASSIGNMENT_INITIALIZERS(RubySpace.Location.MULTIPLE_ASSIGNMENT_INITIALIZERS, RubyRightPadded.Location.MULTIPLE_ASSIGNMENT_INITIALIZERS_SUFFIX),
        STRUCT_PATTERN_ELEMENT(RubySpace.Location.STRUCT_PATTERN_ELEMENT, RubyRightPadded.Location.STRUCT_PATTERN_ELEMENT_SUFFIX),
        UNDEF_NAMES(RubySpace.Location.UNDEF_NAMES, RubyRightPadded.Location.UNDEF_NAMES_SUFFIX),
        YIELD_DATA(RubySpace.Location.YIELD_DATA, RubyRightPadded.Location.YIELD_DATA_SUFFIX);

        private final RubySpace.Location beforeLocation;
        private final RubyRightPadded.Location elementLocation;
    }
}
