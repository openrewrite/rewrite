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
package org.openrewrite.python.style;

import org.openrewrite.style.NamedStyles;

import java.util.Arrays;

import static java.util.Collections.emptySet;
import static org.openrewrite.Tree.randomId;


public class IntelliJ extends NamedStyles {
    private static final IntelliJ INSTANCE = new IntelliJ();

    public IntelliJ() {
        super(randomId(),
                "org.openrewrite.python.style.IntelliJ",
                "IntelliJ IDEA",
                "IntelliJ IDEA default Python style.",
                emptySet(),
                Arrays.asList(
                        spaces(),
                        wrappingAndBraces(),
                        tabsAndIndents(),
                        blankLines(),
                        other()
                )
        );
    }

    public static IntelliJ defaults() {
        return INSTANCE;
    }

    public static SpacesStyle spaces() {
        return new SpacesStyle(
                new SpacesStyle.BeforeParentheses(
                        false, // methodCall
                        false, // methodDeclaration
                        false  // leftBracket
                ),
                new SpacesStyle.AroundOperators(
                        true,  // assignment
                        true,  // equality
                        true,  // relational
                        true,  // bitwise
                        true,  // additive
                        true,  // multiplicative
                        true,  // shift
                        true,  // power
                        false, // eqInNamedParameter
                        false  // eqInKeywordArgument
                ),
                new SpacesStyle.Within(
                        false, // brackets
                        false, // methodDeclarationParentheses
                        false, // emptyMethodDeclarationParentheses
                        false, // methodCallParentheses
                        false, // emptyMethodCallParentheses
                        false  // braces
                ),
                new SpacesStyle.Other(
                        false, // beforeComma
                        true,  // afterComma
                        false, // beforeForSemicolon
                        false, // beforeColon
                        true,  // afterColon
                        true,  // beforeBackslash
                        true,  // beforeHash
                        true   // afterHash
                )
        );
    }

    public static WrappingAndBracesStyle wrappingAndBraces() {
        return new WrappingAndBracesStyle();
    }

    public static TabsAndIndentsStyle tabsAndIndents() {
        return new TabsAndIndentsStyle(
                false, // useTabCharacter
                4,     // tabSize
                4,     // indentSize
                8,     // continuationIndent
                false, // keepIndentsOnEmptyLines
                new TabsAndIndentsStyle.MethodDeclarationParameters(true)
        );
    }

    public static BlankLinesStyle blankLines() {
        return new BlankLinesStyle(
                new BlankLinesStyle.KeepMaximum(
                        1, // inDeclarations
                        1  // inCode
                ),
                new BlankLinesStyle.Minimum(
                        1, // afterTopLevelImports
                        1, // aroundClass
                        1, // aroundMethod
                        2, // aroundTopLevelClassesFunctions
                        0, // afterLocalImports
                        0  // beforeFirstMethod
                )
        );
    }

    public static OtherStyle other() {
        return new OtherStyle(
                new OtherStyle.UseContinuationIndent(
                        false, // methodCallArguments
                        true,  // methodDeclarationParameters
                        false  // collectionsAndComprehensions
                )
        );
    }
}
