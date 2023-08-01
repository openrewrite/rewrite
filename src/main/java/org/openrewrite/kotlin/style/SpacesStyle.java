/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.kotlin.style;

import lombok.Value;
import lombok.With;
import org.openrewrite.kotlin.KotlinStyle;
import org.openrewrite.style.Style;
import org.openrewrite.style.StyleHelper;

@Value
@With
public class SpacesStyle implements KotlinStyle {
    BeforeParentheses beforeParentheses;
    AroundOperators aroundOperators;
    Other other;

    @Value
    @With
    public static class BeforeParentheses {
        Boolean ifParentheses;
        Boolean forParentheses;
        Boolean whileParentheses;
        Boolean catchParentheses;
        Boolean whenParentheses;
    }

    @Value
    @With
    public static class AroundOperators {
        Boolean assignment;
        Boolean logical;
        Boolean equality;
        Boolean relational;
        Boolean additive;
        Boolean multiplicative;
        Boolean unary;
        Boolean range;
    }

    @Value
    @With
    public static class Other {
        Boolean beforeComma;
        Boolean afterComma;
        Boolean beforeColonAfterDeclarationName;
        Boolean afterColonBeforeDeclarationType;
        Boolean beforeColonInNewTypeDefinition;
        Boolean afterColonInNewTypeDefinition;
        Boolean inSimpleOneLineMethods;
        Boolean aroundArrowInFunctionTypes;
        Boolean aroundArrowInWhenClause;
        Boolean beforeLambdaArrow;
    }

    @Override
    public Style applyDefaults() {
        return StyleHelper.merge(IntelliJ.spaces(), this);
    }
}
