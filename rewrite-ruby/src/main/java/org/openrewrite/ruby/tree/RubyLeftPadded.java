/*
 * Copyright 2022 the original author or authors.
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

public class RubyLeftPadded {

    @RequiredArgsConstructor(access = lombok.AccessLevel.PRIVATE)
    @Getter
    public enum Location {
        BOOLEAN_CHECK_IN_PREFIX(RubySpace.Location.BOOLEAN_CHECK_IN_PREFIX),
        CLASS_METHOD_NAME_PREFIX(RubySpace.Location.CLASS_METHOD_NAME_PREFIX),
        KEY_VALUE_SEPARATOR_PREFIX(RubySpace.Location.KEY_VALUE_SEPARATOR_PREFIX),
        OPEN_EIGENCLASS_IDENTIFIER(RubySpace.Location.OPEN_EIGENCLASS_IDENTIFIER_PREFIX),
        PATTERN_BINDING_NAME(RubySpace.Location.PATTERN_BINDING_NAME_PREFIX),
        PATTERN_GUARD_CONDITION(RubySpace.Location.PATTERN_GUARD_CONDITION_PREFIX),
        SUB_ARRAY_LENGTH_PREFIX(RubySpace.Location.SUB_ARRAY_LENGTH_PREFIX);

        private final RubySpace.Location beforeLocation;
    }
}
