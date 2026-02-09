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
package org.openrewrite.csharp.tree;

public class CsLeftPadded {
    public enum Location {
        ASSIGNMENT_OPERATION_OPERATOR(CsSpace.Location.ASSIGNMENT_OPERATION_OPERATOR),
        BINARY_OPERATOR(CsSpace.Location.BINARY_OPERATOR),
        EXTERN_ALIAS_IDENTIFIER(CsSpace.Location.EXTERN_ALIAS),
        PROPERTY_DECLARATION_EXPRESSION_BODY(CsSpace.Location.PROPERTY_DECLARATION_EXPRESSION_BODY),
        PROPERTY_DECLARATION_INITIALIZER(CsSpace.Location.PROPERTY_DECLARATION_INITIALIZER),
        USING_DIRECTIVE_STATIC(CsSpace.Location.USING_DIRECTIVE_STATIC),
        USING_DIRECTIVE_UNSAFE(CsSpace.Location.USING_DIRECTIVE_UNSAFE),
        UNARY_OPERATOR(CsSpace.Location.UNARY_OPERATOR),
        SWITCH_EXPRESSION_ARM_EXPRESSION(CsSpace.Location.SWITCH_EXPRESSION_ARM_EXPRESSION),
        SWITCH_EXPRESSION_ARM_WHEN_EXPRESSION(CsSpace.Location.SWITCH_EXPRESSION_ARM_WHEN_EXPRESSION),
        CASE_PATTERN_SWITCH_LABEL_WHEN_CLAUSE(CsSpace.Location.CASE_PATTERN_SWITCH_LABEL_WHEN_CLAUSE),
        IS_PATTERN_PATTERN(CsSpace.Location.IS_PATTERN_PATTERN),
        BINARY_PATTERN_OPERATOR(CsSpace.Location.BINARY_PATTERN_OPERATOR),
        RELATIONAL_PATTERN_OPERATOR(CsSpace.Location.RELATIONAL_PATTERN_OPERATOR),
        SUBPATTERN_PATTERN(CsSpace.Location.SUBPATTERN_PATTERN),
        INDEXER_DECLARATION_EXPRESSION_BODY(CsSpace.Location.INDEXER_DECLARATION_EXPRESSION_BODY),
        JOIN_CLAUSE_INTO(CsSpace.Location.JOIN_CLAUSE_INTO),
        CONVERSION_OPERATOR_DECLARATION_KIND(CsSpace.Location.CONVERSION_OPERATOR_DECLARATION_KIND),
        CONVERSION_OPERATOR_DECLARATION_EXPRESSION_BODY(CsSpace.Location.CONVERSION_OPERATOR_DECLARATION_EXPRESSION_BODY),
        USING_STATEMENT_EXPRESSION(CsSpace.Location.USING_STATEMENT_EXPRESSION),
        CLASS_DECLARATION_EXTENDINGS(CsSpace.Location.CLASS_DECLARATION_EXTENDINGS),
        ENUM_MEMBER_DECLARATION_INITIALIZER(CsSpace.Location.ENUM_MEMBER_DECLARATION_INITIALIZER),
        ENUM_DECLARATION_NAME(CsSpace.Location.ENUM_DECLARATION_NAME),
        ENUM_DECLARATION_BASE_TYPE(CsSpace.Location.ENUM_DECLARATION_BASE_TYPE),
        TYPE_PARAMETER_VARIANCE(CsSpace.Location.TYPE_PARAMETER_VARIANCE),
        CONVERSION_OPERATOR_DECLARATION_RETURN_TYPE(CsSpace.Location.CONVERSION_OPERATOR_DECLARATION_RETURN_TYPE),
        DELEGATE_DECLARATION_RETURN_TYPE(CsSpace.Location.DELEGATE_DECLARATION_RETURN_TYPE),
        TRY_CATCH_FILTER_EXPRESSION(CsSpace.Location.TRY_CATCH_FILTER_EXPRESSION),
        TRY_FINALLIE(CsSpace.Location.TRY_FINALLIE),
        ACCESSOR_DECLARATION_KIND(CsSpace.Location.ACCESSOR_DECLARATION_KIND),
        ACCESSOR_DECLARATION_EXPRESSION_BODY(CsSpace.Location.ACCESSOR_DECLARATION_EXPRESSION_BODY),
        POINTER_FIELD_ACCESS_NAME(CsSpace.Location.POINTER_FIELD_ACCESS_NAME),
        EVENT_DECLARATION_TYPE_EXPRESSION(CsSpace.Location.EVENT_DECLARATION_TYPE_EXPRESSION), OPERATOR_DECLARATION_OPERATOR_TOKEN(CsSpace.Location.OPERATOR_DECLARATION_OPERATOR_TOKEN);

        private CsSpace.Location beforeLocation;

        private Location(CsSpace.Location beforeLocation) {
            this.beforeLocation = beforeLocation;
        }

        public CsSpace.Location getBeforeLocation() {
            return beforeLocation;
        }
    }
}
