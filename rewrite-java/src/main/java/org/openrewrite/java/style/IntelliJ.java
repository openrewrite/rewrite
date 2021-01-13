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

public class IntelliJ {
    public static TabsAndIndentsStyle defaultTabsAndIndents() {
        TabsAndIndentsStyle style = new TabsAndIndentsStyle();

        style.setUseTabCharacter(false);
        style.setTabSize(4);
        style.setIndentSize(4);
        style.setContinuationIndent(8);
        style.setIndentsRelativeToExpressionStart(false);

        return style;
    }

    public static BlankLineStyle defaultBlankLine() {
        BlankLineStyle style = new BlankLineStyle();

        BlankLineStyle.KeepMaximum max = new BlankLineStyle.KeepMaximum();
        max.setInDeclarations(2);
        max.setInCode(2);
        max.setBeforeEndOfBlock(2);
        max.setBetweenHeaderAndPackage(2);

        style.setKeepMaximum(max);

        BlankLineStyle.Minimum min = new BlankLineStyle.Minimum();
        min.setBeforePackage(0);
        min.setAfterPackage(1);
        min.setBeforeImports(3);
        min.setAfterImports(1);
        min.setAroundClass(1);
        min.setAfterClassHeader(0);
        min.setBeforeClassEnd(0);
        min.setAfterAnonymousClassHeader(0);
        min.setAroundFieldInInterface(0);
        min.setAroundField(0);

        style.setMinimum(min);

        return style;
    }

    public static SpacesStyle defaultSpaces() {
        SpacesStyle style = new SpacesStyle();

        SpacesStyle.BeforeParentheses beforeParentheses = new SpacesStyle.BeforeParentheses();
        beforeParentheses.setIfParentheses(true);
        beforeParentheses.setForParentheses(true);
        beforeParentheses.setWhileParentheses(true);
        beforeParentheses.setSwitchParentheses(true);
        beforeParentheses.setTryParentheses(true);
        beforeParentheses.setCatchParentheses(true);
        beforeParentheses.setSynchronizedParentheses(true);
        style.setBeforeParentheses(beforeParentheses);

        SpacesStyle.AroundOperators aroundOperators = new SpacesStyle.AroundOperators();
        aroundOperators.setAssignment(true);
        aroundOperators.setLogical(true);
        aroundOperators.setEquality(true);
        aroundOperators.setRelational(true);
        aroundOperators.setBitwise(true);
        aroundOperators.setAdditive(true);
        aroundOperators.setMultiplicative(true);
        aroundOperators.setShift(true);
        aroundOperators.setLambdaArrow(true);
        style.setAroundOperators(aroundOperators);

        SpacesStyle.BeforeLeftBrace beforeLeftBrace = new SpacesStyle.BeforeLeftBrace();
        beforeLeftBrace.setClassLeftBrace(true);
        beforeLeftBrace.setMethodLeftBrace(true);
        beforeLeftBrace.setIfLeftBrace(true);
        beforeLeftBrace.setElseLeftBrace(true);
        beforeLeftBrace.setForLeftBrace(true);
        beforeLeftBrace.setWhileLeftBrace(true);
        beforeLeftBrace.setDoLeftBrace(true);
        beforeLeftBrace.setSwitchLeftBrace(true);
        beforeLeftBrace.setTryLeftBrace(true);
        beforeLeftBrace.setCatchLeftBrace(true);
        beforeLeftBrace.setFinallyLeftBrace(true);
        beforeLeftBrace.setSynchronizedLeftBrace(true);
        style.setBeforeLeftBrace(beforeLeftBrace);

        SpacesStyle.BeforeKeywords beforeKeywords = new SpacesStyle.BeforeKeywords();
        beforeKeywords.setElseKeyword(true);
        beforeKeywords.setWhileKeyword(true);
        beforeKeywords.setCatchKeyword(true);
        beforeKeywords.setFinallyKeyword(true);
        style.setBeforeKeywords(beforeKeywords);

        style.setWithin(new SpacesStyle.Within());

        SpacesStyle.TernaryOperator ternaryOperator = new SpacesStyle.TernaryOperator();
        ternaryOperator.setBeforeQuestionMark(true);
        ternaryOperator.setAfterQuestionMark(true);
        ternaryOperator.setBeforeColon(true);
        ternaryOperator.setAfterColon(true);
        style.setTernaryOperator(ternaryOperator);

        SpacesStyle.TypeArguments typeArguments = new SpacesStyle.TypeArguments();
        typeArguments.setAfterComma(true);
        style.setTypeArguments(typeArguments);

        SpacesStyle.Other other = new SpacesStyle.Other();
        other.setAfterComma(true);
        other.setAfterForSemicolon(true);
        other.setAfterTypeCast(true);
        other.setBeforeColonInForEach(true);
        style.setOther(other);

        SpacesStyle.TypeParameters typeParameters = new SpacesStyle.TypeParameters();
        typeParameters.setAroundTypeBounds(true);
        style.setTypeParameters(typeParameters);

        return style;
    }
}
