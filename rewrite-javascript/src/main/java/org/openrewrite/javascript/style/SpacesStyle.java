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
package org.openrewrite.javascript.style;

import lombok.Value;
import lombok.With;
import org.openrewrite.javascript.JavaScriptStyle;

@Value
@With
public class SpacesStyle implements JavaScriptStyle {

    BeforeParentheses beforeParentheses;
    AroundOperators aroundOperators;
    BeforeLeftBrace beforeLeftBrace;
    BeforeKeywords beforeKeywords;
    Within within;
    TernaryOperator ternaryOperator;
    Other other;

    @Value
    @With
    public static class BeforeParentheses {
        Boolean functionDeclarationParentheses;
        Boolean functionCallParentheses;
        Boolean ifParentheses;
        Boolean forParentheses;
        Boolean whileParentheses;
        Boolean switchParentheses;
        Boolean catchParentheses;
        Boolean inFunctionCallExpression;
        Boolean inAsyncArrowFunction;
    }

    @Value
    @With
    public static class AroundOperators {
        Boolean assignment;
        Boolean logical;
        Boolean equality;
        Boolean relational;
        Boolean bitwise;
        Boolean additive;
        Boolean multiplicative;
        Boolean shift;
        Boolean unary;
        Boolean arrowFunction;
        Boolean beforeUnaryNotAndNotNull;
        Boolean afterUnaryNotAndNotNull;
    }

    @Value
    @With
    public static class BeforeLeftBrace {
        Boolean functionLeftBrace;
        Boolean ifLeftBrace;
        Boolean elseLeftBrace;
        Boolean forLeftBrace;
        Boolean whileLeftBrace;
        Boolean doLeftBrace;
        Boolean switchLeftBrace;
        Boolean tryLeftBrace;
        Boolean catchLeftBrace;
        Boolean finallyLeftBrace;
        Boolean classInterfaceModuleLeftBrace;
    }

    @Value
    @With
    public static class BeforeKeywords {
        Boolean elseKeyword;
        Boolean whileKeyword;
        Boolean catchKeyword;
        Boolean finallyKeyword;
    }

    @Value
    @With
    public static class Within {
        Boolean brackets;
        Boolean groupingParentheses;
        Boolean functionDeclarationParentheses;
        Boolean functionCallParentheses;
        Boolean ifParentheses;
        Boolean forParentheses;
        Boolean whileParentheses;
        Boolean switchParentheses;
        Boolean catchParentheses;
        Boolean objectLiteralBraces;
        Boolean es6ImportExportBraces;
        Boolean arrayBrackets;
        Boolean interpolationExpressions;
        Boolean objectLiteralTypeBraces;
        Boolean unionAndIntersectionTypes;
        Boolean typeAssertions;
    }

    @Value
    @With
    public static class TernaryOperator {
        Boolean beforeQuestionMark;
        Boolean afterQuestionMark;
        Boolean beforeColon;
        Boolean afterColon;
    }

    @Value
    @With
    public static class Other {
        Boolean beforeComma;
        Boolean afterComma;
        Boolean beforeForSemicolon;
        Boolean afterForSemicolon;
        Boolean beforePropertyNameValueSeparator;
        Boolean afterPropertyNameValueSeparator;
        Boolean afterVarArgInRestOrSpread;
        Boolean beforeAsteriskInGenerator;
        Boolean afterAsteriskInGenerator;
        Boolean beforeTypeReferenceColon;
        Boolean afterTypeReferenceColon;
    }
}
