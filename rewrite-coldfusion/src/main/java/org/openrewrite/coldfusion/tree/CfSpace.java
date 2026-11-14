/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.coldfusion.tree;

public class CfSpace {
    public enum Location {
        ASSIGNMENT_OPERATION_OPERATOR,
        ASSIGNMENT_OPERATION_PREFIX,
        BINARY_OPERATOR,
        BINARY_PREFIX,
        CLOSURE_PREFIX,
        EXPRESSION_STATEMENT_PREFIX,
        INTERPOLATION_PREFIX,
        INVOCATION_ARGUMENTS,
        INVOCATION_ARGUMENT_SUFFIX,
        INVOCATION_PREFIX,
        INTERPOLATION_SUFFIX,
        KEY_VALUE_PREFIX,
        KEY_VALUE_SEPARATOR,
        SCRIPT_TAG_PREFIX,
        STRING_TEMPLATE_PREFIX,
        STRUCT_LITERAL_ENTRIES,
        STRUCT_LITERAL_ENTRY_SUFFIX,
        STRUCT_LITERAL_PREFIX,
        TAG_ATTRIBUTE_PREFIX,
        TAG_ATTRIBUTE_VALUE,
        TAG_BEFORE_END,
        TAG_CALL_PREFIX,
        TAG_CLOSING_BEFORE_END,
        TAG_PREFIX,
        TEXT_PREFIX,
        TYPE_LITERAL_PREFIX
    }
}
