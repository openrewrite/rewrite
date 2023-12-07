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

import com.fasterxml.jackson.annotation.JsonCreator;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.style.Style;

import java.util.Arrays;
import java.util.Collection;

import static java.util.Collections.emptySet;
import static org.openrewrite.Tree.randomId;

public class IntelliJ extends NamedStyles {
    private static final IntelliJ INSTANCE = new IntelliJ();

    // Public so that Environment can find and activate this style
    // From code, should use IntelliJ.default() instead
    public IntelliJ() {
        super(randomId(),
                "org.openrewrite.kotlin.IntelliJ",
                "IntelliJ IDEA",
                "IntelliJ IDEA defaults for styles.",
                emptySet(),
                Arrays.asList(
                        importLayout(),
                        blankLines(),
                        tabsAndIndents(),
                        spaces(),
                        wrappingAndBraces()
                )
        );
    }

    @JsonCreator
    public static IntelliJ defaults() {
        return INSTANCE;
    }

    @Override
    public Collection<Style> getStyles() {
        return super.getStyles();
    }

    public static ImportLayoutStyle importLayout() {
        return ImportLayoutStyle.builder()
//                .packageToFold("java.util.*", true)
                .packageToFold("kotlinx.android.synthetic.*", true)
                .packageToFold("io.ktor.*", true)
                .importAllOthers()
                .importPackage("java.*")
                .importPackage("javax.*")
                .importPackage("kotlin.*")
                .importAllAliases()
                .build();
    }

    public static TabsAndIndentsStyle tabsAndIndents() {
        return new TabsAndIndentsStyle(false, 4, 4, 8, false,
                new TabsAndIndentsStyle.FunctionDeclarationParameters(true));
    }

    public static BlankLinesStyle blankLines() {
        return new BlankLinesStyle(
                new BlankLinesStyle.KeepMaximum(2, 2, 2),
                new BlankLinesStyle.Minimum(0, 0, 1)
        );
    }

    public static SpacesStyle spaces() {
        return new SpacesStyle(
                new SpacesStyle.BeforeParentheses(true, true, true, true, true),
                new SpacesStyle.AroundOperators(true, true, true, true, true, true, false, false),
                new SpacesStyle.Other(false, true, false, true, true, true, true, true, true, true)
        );
    }

    public static WrappingAndBracesStyle wrappingAndBraces() {
        return new WrappingAndBracesStyle(
                new WrappingAndBracesStyle.KeepWhenFormatting(true, true),
                new WrappingAndBracesStyle.ExtendsImplementsPermitsList(false, false),
                new WrappingAndBracesStyle.FunctionDeclarationParameters(true, true, true, false),
                new WrappingAndBracesStyle.FunctionCallArguments(false, true, true, false),
                new WrappingAndBracesStyle.FunctionParentheses(false),
                new WrappingAndBracesStyle.ChainedFunctionCalls(false, false),
                new WrappingAndBracesStyle.IfStatement(false, true, false),
                new WrappingAndBracesStyle.DoWhileStatement(false),
                new WrappingAndBracesStyle.TryStatement(false, false),
                new WrappingAndBracesStyle.BinaryExpression(false),
                new WrappingAndBracesStyle.WhenStatements(false, true),
                new WrappingAndBracesStyle.BracesPlacement(false),
                new WrappingAndBracesStyle.ExpressionBodyFunctions(false),
                new WrappingAndBracesStyle.ElvisExpressions(false)
        );
    }

    public static OtherStyle other() {
        return new OtherStyle(false);
    }
}
