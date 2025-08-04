/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.javascript.tree;

import lombok.Getter;

public class JsLeftPadded {
    @Getter
    public enum Location {
        BINARY_OPERATOR(JsSpace.Location.BINARY_PREFIX),
        BINDING_ELEMENT_INITIALIZER(JsSpace.Location.BINDING_INITIALIZER_PREFIX),
        CONDITIONAL_TYPE_CONDITION(JsSpace.Location.CONDITIONAL_TYPE_CONDITION),
        IMPORT_INITIALIZER(JsSpace.Location.IMPORT_INITIALIZER_PREFIX),
        TYPE_DECLARATION_NAME(JsSpace.Location.TYPE_DECLARATION_NAME_PREFIX),
        TYPE_DECLARATION_INITIALIZER(JsSpace.Location.TYPE_DECLARATION_INITIALIZER_PREFIX),
        TYPE_OPERATOR_EXPRESSION(JsSpace.Location.TYPE_OPERATOR_EXPRESSION_PREFIX),
        SCOPED_VARIABLE_DECLARATIONS_SCOPE(JsSpace.Location.SCOPED_VARIABLE_DECLARATIONS_SCOPE_PREFIX),
        NAMESPACE_DECLARATION_KEYWORD_TYPE(JsSpace.Location.NAMESPACE_DECLARATION_KEYWORD_PREFIX),
        IMPORT_CLAUSE_TYPE_ONLY(JsSpace.Location.IMPORT_CLAUSE_TYPE_ONLY_PREFIX),
        IMPORT_SPECIFIER_IMPORT_TYPE(JsSpace.Location.IMPORT_SPECIFIER_IMPORT_TYPE_PREFIX),
        INDEXED_SIGNATURE_DECLARATION_TYPE_EXPRESSION(JsSpace.Location.INDEXED_SIGNATURE_DECLARATION_TYPE_EXPRESSION_PREFIX),
        INFER_TYPE_PARAMETER(JsSpace.Location.INFER_TYPE_PARAMETER_PREFIX),
        TYPE_PREDICATE_ASSERTS(JsSpace.Location.TYPE_PREDICATE_ASSERTS_PREFIX),
        TYPE_PREDICATE_EXPRESSION(JsSpace.Location.TYPE_PREDICATE_EXPRESSION_PREFIX),
        SATISFIES_EXPRESSION_TYPE(JsSpace.Location.SATISFIES_EXPRESSION_TYPE_PREFIX),
        IMPORT_TYPE_QUALIFIER(JsSpace.Location.IMPORT_TYPE_QUALIFIER_PREFIX),
        EXPORT_DECLARATION_TYPE_ONLY(JsSpace.Location.EXPORT_DECLARATION_TYPE_ONLY_PREFIX),
        EXPORT_DECLARATION_MODULE_SPECIFIER(JsSpace.Location.EXPORT_DECLARATION_MODULE_SPECIFIER_PREFIX),
        EXPORT_SPECIFIER_TYPE_ONLY(JsSpace.Location.EXPORT_SPECIFIER_TYPE_ONLY_PREFIX),
        EXPORT_ASSIGNMENT_EXPRESSION(JsSpace.Location.EXPORT_ASSIGNMENT_EXPRESSION_PREFIX),
        ASSIGNMENT_OPERATION_OPERATOR(JsSpace.Location.ASSIGNMENT_OPERATION_OPERATOR_PREFIX),
        MAPPED_TYPE_PREFIX_TOKEN(JsSpace.Location.MAPPED_TYPE_PREFIX_TOKEN_PREFIX),
        MAPPED_TYPE_SUFFIX_TOKEN(JsSpace.Location.MAPPED_TYPE_SUFFIX_TOKEN_PREFIX),
        MAPPED_TYPE_MAPPED_TYPE_PARAMETER_ITERATE(JsSpace.Location.MAPPED_TYPE_MAPPED_TYPE_PARAMETER_ITERATE_PREFIX),
        MAPPED_TYPE_READONLY(JsSpace.Location.MAPPED_TYPE_READONLY_PREFIX),
        MAPPED_TYPE_QUESTION_TOKEN(JsSpace.Location.MAPPED_TYPE_QUESTION_TOKEN_PREFIX),
        LAMBDA_ARROW(JsSpace.Location.LAMBDA_ARROW_PREFIX),
        FUNCTION_TYPE_CONSTRUCTOR(JsSpace.Location.FUNCTION_TYPE_CONSTRUCTOR_PREFIX),
        FUNCTION_TYPE_RETURN_TYPE(JsSpace.Location.FUNCTION_TYPE_RETURN_TYPE_PREFIX),
        IMPORT_MODULE_SPECIFIER(JsSpace.Location.IMPORT_MODULE_SPECIFIER_PREFIX),
        IMPORT_ATTRIBUTE_VALUE(JsSpace.Location.IMPORT_ATTRIBUTE_VALUE_PREFIX),
        ;

        private final JsSpace.Location beforeLocation;

        Location(JsSpace.Location beforeLocation) {
            this.beforeLocation = beforeLocation;
        }
    }
}
