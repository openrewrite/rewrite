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
package org.openrewrite.coldfusion.style;

import lombok.Value;
import lombok.With;
import org.openrewrite.style.Style;
import org.openrewrite.style.StyleHelper;

/**
 * The spaces within a line of CFML. Line breaks are never added or removed by these. Each setting says whether a
 * single space goes in a place or none does.
 */
@Value
@With
public class SpacesStyle implements Style {
    BeforeParentheses beforeParentheses;
    AroundOperators aroundOperators;
    Within within;
    Other other;
    Tags tags;

    @Value
    @With
    public static class BeforeParentheses {
        /**
         * {@code if (}, {@code for (}, {@code while (}, {@code switch (} and {@code catch (}.
         */
        Boolean keywords;

        /**
         * {@code function name (}.
         */
        Boolean functionDeclaration;

        /**
         * {@code name (} in a call.
         */
        Boolean functionCall;

        /**
         * {@code function (} in a closure.
         */
        Boolean anonymousFunction;
    }

    @Value
    @With
    public static class AroundOperators {
        /**
         * Arithmetic, comparison, logical and concatenation operators, as {@code a + b} and {@code a & b}. An
         * operator spelled as a word, as {@code EQ}, always has a space on each side.
         */
        Boolean binary;

        /**
         * {@code =}, {@code +=}, {@code &=} and the other assignments.
         */
        Boolean assignment;

        /**
         * The {@code ?} and {@code :} of {@code a ? b : c}.
         */
        Boolean ternary;

        /**
         * {@code =>} in an arrow function.
         */
        Boolean arrow;
    }

    @Value
    @With
    public static class Within {
        /**
         * {@code if ( x )} and the other keywords' parentheses.
         */
        Boolean keywordParentheses;

        /**
         * {@code function name( a, b )}.
         */
        Boolean functionDeclarationParentheses;

        /**
         * {@code function name( )}.
         */
        Boolean emptyFunctionDeclarationParentheses;

        /**
         * {@code name( a, b )} in a call.
         */
        Boolean functionCallParentheses;

        /**
         * {@code name( )} in a call.
         */
        Boolean emptyFunctionCallParentheses;

        /**
         * {@code function( a, b )} in a closure.
         */
        Boolean anonymousFunctionParentheses;

        /**
         * {@code ( a + b )}.
         */
        Boolean groupingParentheses;

        /**
         * {@code items[ 1 ]}.
         */
        Boolean brackets;

        /**
         * {@code [ 1, 2 ]}.
         */
        Boolean arrayLiteral;

        /**
         * {@code [ ]}.
         */
        Boolean emptyArrayLiteral;

        /**
         * {@code { a: 1 }}.
         */
        Boolean structLiteral;

        /**
         * {@code { }}.
         */
        Boolean emptyStructLiteral;
    }

    @Value
    @With
    public static class Other {
        Boolean beforeComma;
        Boolean afterComma;

        /**
         * Before the {@code ;} of a {@code for} loop's parts.
         */
        Boolean beforeForSemicolon;

        /**
         * After the {@code ;} of a {@code for} loop's parts.
         */
        Boolean afterForSemicolon;

        /**
         * Before the {@code :} of a struct key, as {@code {a : 1}}.
         */
        Boolean beforeColon;

        /**
         * After the {@code :} of a struct key, as {@code {a: 1}}.
         */
        Boolean afterColon;

        /**
         * Around the {@code =} of a struct key or a named argument, as {@code {a = 1}} and {@code f(a = 1)}.
         */
        Boolean aroundKeyValueEquals;

        /**
         * Around the {@code =} of an attribute in script: a component's, a function's, a property's or a tag's
         * written as a statement, as {@code output = false}. CFFormat's {@code metadata.key_value.padding}.
         */
        Boolean aroundMetadataEquals;

        /**
         * Before a component's <code>{</code>, as {@code component extends="Base" {}}, and a tag's written as a statement,
         * as {@code lock name="x" {}}.
         */
        Boolean beforeComponentBrace;

        /**
         * Before a function's <code>{</code>, as {@code function f() {}}.
         */
        Boolean beforeFunctionBrace;

        /**
         * Before a closure's <code>{</code>, as {@code function(x) {}}.
         */
        Boolean beforeClosureBrace;

        /**
         * Before the <code>{</code> after a keyword's parentheses, as {@code if (x) {}}, {@code catch (any e) {}} and
         * {@code cfloop(array=a, index="i") {}}.
         */
        Boolean beforeStatementBrace;

        /**
         * Before the <code>{</code> right after a keyword, as {@code else {}}, {@code try {}} and {@code do {}}.
         */
        Boolean beforeKeywordBrace;

        /**
         * Between a block's <code>}</code> and the keyword after it, as <code>} else</code>.
         */
        Boolean beforeKeywordAfterBlock;

        /**
         * Whether spaces that line up consecutive lines are kept where there are more than one, as an author who
         * aligns them writes them: before the {@code =} of an assignment, before a struct key's or named argument's
         * separator, and between a parameter's type and its name. Both sides of an aligned operator stay as written.
         * CFFormat's {@code alignment.consecutive.assignments}.
         */
        Boolean keepAlignedAssignments;
    }

    /**
     * Inside tags and the attributes written like them: a component's, a function's, a property's or a script tag's.
     */
    @Value
    @With
    public static class Tags {
        /**
         * Around the {@code =} of an attribute in a tag, as {@code <cfparam name = "x">}.
         */
        Boolean aroundAttributeEquals;

        /**
         * Whether the spaces before an attribute are kept where there are more than one, as an author who lines up
         * the attributes of consecutive properties or tags writes them. CFFormat's
         * {@code alignment.consecutive.properties} and {@code alignment.consecutive.params}.
         */
        Boolean keepAlignedAttributes;

        /**
         * Before the {@code />} that closes a tag, as {@code <cfset x = 1 />}.
         */
        Boolean beforeSelfClosingEnd;

        /**
         * Before the {@code >} that ends a start tag or an end tag, as {@code <cfif x >}.
         */
        Boolean beforeTagEnd;
    }

    @Override
    public Style applyDefaults() {
        return StyleHelper.merge(CFFormat.spaces(), this);
    }
}
