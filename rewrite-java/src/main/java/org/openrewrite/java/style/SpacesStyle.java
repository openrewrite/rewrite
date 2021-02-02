/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.java.style;

import lombok.AccessLevel;
import lombok.Data;
import lombok.With;
import lombok.experimental.FieldDefaults;
import org.openrewrite.java.JavaStyle;

@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Data
@With
public class SpacesStyle implements JavaStyle {
    BeforeParentheses beforeParentheses;
    AroundOperators aroundOperators;
    BeforeLeftBrace beforeLeftBrace;
    BeforeKeywords beforeKeywords;
    Within within;
    TernaryOperator ternaryOperator;
    TypeArguments typeArguments;
    Other other;
    TypeParameters typeParameters;

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Data
    @With
    public static class BeforeParentheses {
        boolean methodDeclaration;
        boolean methodCall;
        boolean ifParentheses;
        boolean forParentheses;
        boolean whileParentheses;
        boolean switchParentheses;
        boolean tryParentheses;
        boolean catchParentheses;
        boolean synchronizedParentheses;
        boolean annotationParameters;
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Data
    @With
    public static class AroundOperators {
        boolean assignment;
        boolean logical;
        boolean equality;
        boolean relational;
        boolean bitwise;
        boolean additive;
        boolean multiplicative;
        boolean shift;
        boolean unary;
        boolean lambdaArrow;
        boolean methodReferenceDoubleColon;
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Data
    @With
    public static class BeforeLeftBrace {
        boolean classLeftBrace;
        boolean methodLeftBrace;
        boolean ifLeftBrace;
        boolean elseLeftBrace;
        boolean forLeftBrace;
        boolean whileLeftBrace;
        boolean doLeftBrace;
        boolean switchLeftBrace;
        boolean tryLeftBrace;
        boolean catchLeftBrace;
        boolean finallyLeftBrace;
        boolean synchronizedLeftBrace;
        boolean arrayInitializerLeftBrace;
        boolean annotationArrayInitializerLeftBrace;
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Data
    @With
    public static class BeforeKeywords {
        boolean elseKeyword;
        boolean whileKeyword;
        boolean catchKeyword;
        boolean finallyKeyword;
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Data
    @With
    public static class Within {
        /**
         * Based on testing in IntelliJ, within codeBraces only affects whether or not a space is inserted
         * into empty interface and class braces, not functions, logical blocks, or lambda braces.
         */
        boolean codeBraces;

        boolean brackets;
        boolean arrayInitializerBraces;
        boolean emptyArrayInitializerBraces;
        boolean groupingParentheses;
        boolean methodDeclarationParentheses;
        boolean emptyMethodDeclarationParentheses;
        boolean methodCallParentheses;
        boolean emptyMethodCallParentheses;
        boolean ifParentheses;
        boolean forParentheses;
        boolean whileParentheses;
        boolean switchParentheses;
        boolean tryParentheses;
        boolean catchParentheses;
        boolean synchronizedParentheses;
        boolean typeCastParentheses;
        boolean annotationParentheses;
        boolean angleBrackets;
        boolean recordHeader;
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Data
    @With
    public static class TernaryOperator {
        boolean beforeQuestionMark;
        boolean afterQuestionMark;
        boolean beforeColon;
        boolean afterColon;
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Data
    @With
    public static class TypeArguments {
        boolean afterComma;
        boolean beforeOpeningAngleBracket;
        boolean afterClosingAngleBracket;
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Data
    @With
    public static class Other {
        boolean beforeComma;
        boolean afterComma;
        boolean beforeForSemicolon;
        boolean afterForSemicolon;
        boolean afterTypeCast;
        boolean beforeColonInForEach;
        boolean insideOneLineEnumBraces;
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Data
    @With
    public static class TypeParameters {
        boolean beforeOpeningAngleBracket;
        boolean aroundTypeBounds;
    }
}
