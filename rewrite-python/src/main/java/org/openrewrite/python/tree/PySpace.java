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

public final class PySpace {

    public enum Location {
        ASSERT_ELEMENT_SUFFIX,
        ASYNC_PREFIX,
        AWAIT_PREFIX,
        BINARY_NEGATION,
        BINARY_OPERATOR,
        BINARY_PREFIX,
        CHAINED_ASSIGNMENT_PREFIX,
        CHAINED_ASSIGNMENT_VARIABLE_SUFFIX,
        COLLECTION_LITERAL_ELEMENT_SUFFIX,
        COLLECTION_LITERAL_PREFIX,
        COMPILATION_UNIT_STATEMENT_SUFFIX,
        COMPREHENSION_EXPRESSION_CLAUSE_ASYNC_SUFFIX,
        COMPREHENSION_EXPRESSION_CLAUSE_ITERATED_LIST,
        COMPREHENSION_EXPRESSION_CLAUSE_PREFIX,
        COMPREHENSION_EXPRESSION_CONDITION_PREFIX,
        COMPREHENSION_EXPRESSION_PREFIX,
        COMPREHENSION_EXPRESSION_SUFFIX,
        DEL_PREFIX,
        DEL_TARGET_SUFFIX,
        DICT_ENTRY_KEY_SUFFIX,
        DICT_LITERAL_ELEMENT_SUFFIX,
        DICT_LITERAL_PREFIX,
        ERROR_FROM_PREFIX,
        ERROR_FROM_SOURCE,
        EXCEPTION_TYPE_PREFIX,
        EXPRESSION_TYPE_TREE_PREFIX,
        FORMATTED_STRING_PREFIX,
        FORMATTED_STRING_VALUE_CONVERSION_SUFFIX,
        FORMATTED_STRING_VALUE_DEBUG_SUFFIX,
        FORMATTED_STRING_VALUE_EXPRESSION_SUFFIX,
        FORMATTED_STRING_VALUE_PREFIX,
        KEY_VALUE_PREFIX,
        KEY_VALUE_SUFFIX,
        LITERAL_TYPE_PREFIX,
        MATCH_CASE_GUARD,
        MATCH_CASE_PATTERN_CHILD_PREFIX,
        MATCH_CASE_PATTERN_PREFIX,
        MATCH_CASE_PREFIX,
        MATCH_PATTERN_ELEMENT_SUFFIX,
        MULTI_IMPORT_FROM_SUFFIX,
        MULTI_IMPORT_NAME_PREFIX,
        MULTI_IMPORT_NAME_SUFFIX,
        MULTI_IMPORT_PREFIX,
        NAMED_ARGUMENT,
        NAMED_ARGUMENT_PREFIX,
        PASS_PREFIX,
        SHEBANG_PREFIX,
        SLICE_PREFIX,
        SLICE_START_SUFFIX,
        SLICE_STEP_SUFFIX,
        SLICE_STOP_SUFFIX,
        SPECIAL_PARAMETER_PREFIX,
        STAR_PREFIX,
        TOP_LEVEL_STATEMENT,
        TRAILING_ELSE_WRAPPER_ELSE_BLOCK,
        TRAILING_ELSE_WRAPPER_PREFIX,
        TYPE_ALIAS_PREFIX,
        TYPE_ALIAS_VALUE,
        TYPE_HINTED_EXPRESSION_PREFIX,
        TYPE_HINT_PREFIX,
        UNION_ELEMENT_SUFFIX,
        UNION_TYPE_PREFIX,
        VARIABLE_SCOPE_NAME_SUFFIX,
        VARIABLE_SCOPE_PREFIX,
        YIELD_FROM_PREFIX,
    }

    private PySpace() {
    }
}
