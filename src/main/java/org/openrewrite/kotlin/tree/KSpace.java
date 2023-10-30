/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.kotlin.tree;

public class KSpace {
    public enum Location {
        ANNOTATION_CALL_SITE_PREFIX,
        BINARY_PREFIX,
        BINARY_OPERATOR,
        BINARY_SUFFIX,
        CHECK_NOT_NULL_PREFIX,
        CONSTRUCTOR_COLON,
        CONSTRUCTOR_INVOCATION_PREFIX,
        DELEGATED_SUPER_TYPE_BY,
        DESTRUCTURING_DECLARATION_PREFIX,
        DESTRUCT_ELEMENTS,
        DESTRUCT_SUFFIX,
        FILE_ANNOTATION_SUFFIX,
        WHEN_BRANCH_PREFIX,
        FUNCTION_TYPE_PARAMETER_PREFIX,
        FUNCTION_TYPE_PARAMETER_SUFFIX,
        FUNCTION_TYPE_PARAMETERS,
        FUNCTION_TYPE_ARROW_PREFIX,
        FUNCTION_TYPE_PREFIX,
        FUNCTION_TYPE_RECEIVER,
        IS_NULLABLE_PREFIX,
        IS_NULL_SAFE_PREFIX,
        KRETURN_PREFIX,
        KSTRING_PREFIX,
        KSTRING_SUFFIX,
        KSTRING_VALUE_PREFIX,
        KSTRING_VALUE_AFTER,
        KTHIS_PREFIX,
        LIST_LITERAL_PREFIX,
        LIST_LITERAL_ELEMENTS,
        LIST_LITERAL_ELEMENT_SUFFIX,
        NAMED_VARIABLE_INITIALIZER_PREFIX,
        OBJECT_PREFIX,
        PROPERTY_PREFIX,
        SPREAD_ARGUMENT_PREFIX,
        TOP_LEVEL_STATEMENT,
        TYPE_CONSTRAINTS_PREFIX,
        TYPE_REFERENCE_PREFIX,
        UNARY_PREFIX,
        WHEN_PREFIX,
        WHEN_BRANCH_EXPRESSION,
    }
}
