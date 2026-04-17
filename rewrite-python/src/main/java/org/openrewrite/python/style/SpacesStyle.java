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
package org.openrewrite.python.style;

import lombok.Value;
import lombok.With;
import org.openrewrite.style.Style;
import org.openrewrite.style.StyleHelper;

@Value
@With
public class SpacesStyle implements PythonStyle {

    BeforeParentheses beforeParentheses;
    AroundOperators aroundOperators;
    Within within;
    Other other;

    @Value
    @With
    public static class BeforeParentheses {
        Boolean methodDeclaration;
        Boolean methodCall;
        Boolean leftBracket;
    }

    @Value
    @With
    public static class AroundOperators {
        Boolean assignment;
        Boolean equality;
        Boolean relational;
        Boolean bitwise;
        Boolean additive;
        Boolean multiplicative;
        Boolean shift;
        Boolean power;
        Boolean eqInNamedParameter;
        Boolean eqInKeywordArgument;
    }

    @Value
    @With
    public static class Within {
        Boolean brackets;
        Boolean methodDeclarationParentheses;
        Boolean emptyMethodDeclarationParentheses;
        Boolean methodCallParentheses;
        Boolean emptyMethodCallParentheses;
        Boolean braces;
    }

    @Value
    @With
    public static class Other {
        Boolean beforeComma;
        Boolean afterComma;
        Boolean beforeForSemicolon;
        Boolean beforeColon;
        Boolean afterColon;
        Boolean beforeBackslash;
        Boolean beforeHash;
        Boolean afterHash;
    }

    @Override
    public Style applyDefaults() {
        return StyleHelper.merge(IntelliJ.spaces(), this);
    }
}
