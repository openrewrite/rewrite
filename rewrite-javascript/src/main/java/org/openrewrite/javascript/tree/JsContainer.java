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
package org.openrewrite.javascript.tree;

import lombok.Getter;

public class JsContainer {
    @Getter
    public enum Location {
        ARRAY_LITERAL_EXPRESSION(JsSpace.Location.ARRAY_LITERAL_ELEMENTS, JsRightPadded.Location.ARRAY_LITERAL_ELEMENT_SUFFIX),
        BINDING_ELEMENT(JsSpace.Location.BINDING_ELEMENTS, JsRightPadded.Location.BINDING_ELEMENT),
        EXPORT_ELEMENT(JsSpace.Location.EXPORT_ELEMENTS, JsRightPadded.Location.EXPORT_ELEMENT_SUFFIX),
        FUNCTION_DECLARATION_PARAMETERS(JsSpace.Location.FUNCTION_DECLARATION_PARAMETERS, JsRightPadded.Location.FUNCTION_DECLARATION_PARAMETERS_SUFFIX),
        FUNCTION_TYPE_PARAMETERS(JsSpace.Location.FUNCTION_TYPE_PARAMETERS, JsRightPadded.Location.FUNCTION_TYPE_PARAMETERS_SUFFIX),
        IMPORT_ELEMENT(JsSpace.Location.IMPORT_ELEMENTS, JsRightPadded.Location.IMPORT_ELEMENT_SUFFIX),
        TUPLE_ELEMENT(JsSpace.Location.TUPLE_ELEMENT, JsRightPadded.Location.TUPLE_ELEMENT_SUFFIX),
        JSMETHOD_DECLARATION_PARAMETERS(JsSpace.Location.JSMETHOD_DECLARATION_PARAMETERS, JsRightPadded.Location.JSMETHOD_DECLARATION_PARAMETER),
        TYPE_LITERAL_MEMBERS(JsSpace.Location.TYPE_LITERAL_MEMBERS_PREFIX, JsRightPadded.Location.TYPE_LITERAL_MEMBERS),
        INDEXED_SIGNATURE_DECLARATION_PARAMETERS(JsSpace.Location.INDEXED_SIGNATURE_DECLARATION_PARAMETERS_PREFIX, JsRightPadded.Location.INDEXED_SIGNATURE_DECLARATION_PARAMETERS),
        ARRAY_BINDING_PATTERN_ELEMENTS(JsSpace.Location.ARRAY_BINDING_PATTERN_ELEMENTS_PREFIX, JsRightPadded.Location.ARRAY_BINDING_PATTERN_ELEMENTS),
        EXPR_WITH_TYPE_ARG_PARAMETERS(JsSpace.Location.EXPR_WITH_TYPE_ARG_PARAMETERS, JsRightPadded.Location.EXPR_WITH_TYPE_ARG_PARAMETERS_SUFFIX),
        TEMPLATE_EXPRESSION_TYPE_ARG_PARAMETERS(JsSpace.Location.TEMPLATE_EXPRESSION_TYPE_ARG_PARAMETERS, JsRightPadded.Location.TEMPLATE_EXPRESSION_TYPE_ARG_PARAMETERS_SUFFIX),
        CONDITIONAL_TYPE_CONDITION(JsSpace.Location.CONDITIONAL_TYPE_CONDITION, JsRightPadded.Location.CONDITIONAL_TYPE_CONDITION),
        IMPORT_TYPE_TYPE_ARGUMENTS(JsSpace.Location.IMPORT_TYPE_TYPE_ARGUMENTS, JsRightPadded.Location.IMPORT_TYPE_TYPE_ARGUMENTS),
        NAMED_EXPORTS_ELEMENTS(JsSpace.Location.NAMED_EXPORTS_ELEMENTS_PREFIX, JsRightPadded.Location.NAMED_EXPORTS_ELEMENTS),
        MAPPED_TYPE_VALUE_TYPE(JsSpace.Location.MAPPED_TYPE_VALUE_TYPE, JsRightPadded.Location.MAPPED_TYPE_VALUE_TYPE),
        TYPE_QUERY_TYPE_ARGUMENTS(JsSpace.Location.TYPE_QUERY_TYPE_ARGUMENTS, JsRightPadded.Location.TYPE_QUERY_TYPE_ARGUMENTS),
        NAMED_IMPORTS_ELEMENTS(JsSpace.Location.NAMED_IMPORTS_ELEMENTS_PREFIX, JsRightPadded.Location.NAMED_IMPORTS_ELEMENTS),
        JS_IMPORT_ATTRIBUTES_ELEMENTS(JsSpace.Location.JS_IMPORT_ATTRIBUTES_ELEMENTS_PREFIX, JsRightPadded.Location.JS_IMPORT_ATTRIBUTES_ELEMENTS),
        IMPORT_TYPE_ARGUMENTS_AND_ATTRIBUTES(JsSpace.Location.IMPORT_TYPE_ARGUMENTS_AND_ATTRIBUTES, JsRightPadded.Location.IMPORT_TYPE_ARGUMENTS_AND_ATTRIBUTES),
        JS_IMPORT_TYPE_ATTRIBUTES_ELEMENTS(JsSpace.Location.JS_IMPORT_TYPE_ATTRIBUTES_ELEMENTS, JsRightPadded.Location.JS_IMPORT_TYPE_ATTRIBUTES_ELEMENTS)
        ;

        private final JsSpace.Location beforeLocation;
        private final JsRightPadded.Location elementLocation;

        Location(JsSpace.Location beforeLocation, JsRightPadded.Location elementLocation) {
            this.beforeLocation = beforeLocation;
            this.elementLocation = elementLocation;
        }

    }
}
