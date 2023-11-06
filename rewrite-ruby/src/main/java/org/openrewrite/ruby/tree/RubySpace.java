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
package org.openrewrite.ruby.tree;

public class RubySpace {
    public enum Location {
        BINARY_PREFIX,
        BINARY_OPERATOR,
        DELIMITED_STRING_PREFIX,
        DELIMITED_STRING_VALUE_PREFIX,
        DELIMITED_STRING_VALUE_SUFFIX,
        EXPANSION_PREFIX,
        HASH,
        HASH_PREFIX,
        LIST_LITERAL,
        LIST_LITERAL_SUFFIX,
        KEY_VALUE_PREFIX,
        KEY_VALUE_SUFFIX,
        MULTIPLE_ASSIGNMENT,
        MULTIPLE_ASSIGNMENT_INITIALIZERS_SUFFIX,
        MULTIPLE_ASSIGNMENT_INITIALIZERS,
        MULTIPLE_ASSIGNMENT_SUFFIX,
        REDO_PREFIX,
        YIELD,
        YIELD_DATA_SUFFIX,
    }
}
