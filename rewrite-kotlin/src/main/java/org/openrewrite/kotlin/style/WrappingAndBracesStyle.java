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
public class WrappingAndBracesStyle implements KotlinStyle {

    KeepWhenFormatting keepWhenFormatting;
    ExtendsImplementsPermitsList extendsImplementsPermitsList;
    FunctionDeclarationParameters functionDeclarationParameters;
    FunctionCallArguments functionCallArguments;
    FunctionParentheses functionParentheses;
    ChainedFunctionCalls chainedFunctionCalls;
    IfStatement ifStatement;
    DoWhileStatement doWhileStatement;
    TryStatement tryStatement;
    BinaryExpression binaryExpression;
    WhenStatements whenStatements;
    BracesPlacement bracesPlacement;
    ExpressionBodyFunctions expressionBodyFunctions;
    ElvisExpressions elvisExpressions;

    @Value
    @With
    public static class KeepWhenFormatting {
        Boolean lineBreaks;
        Boolean commentAtFirstColumn;
    }

    @Value
    @With
    public static class ExtendsImplementsPermitsList {
        Boolean alignWhenMultiline;
        Boolean useContinuationIndent;
    }

    @Value
    @With
    public static class FunctionDeclarationParameters {
        Boolean alignWhenMultiline;
        Boolean newLineAfterLeftParen;
        Boolean placeRightParenOnNewLine;
        Boolean useContinuationIndent;
    }

    @Value
    @With
    public static class FunctionCallArguments {
        Boolean alignWhenMultiline;
        Boolean newLineAfterLeftParen;
        Boolean placeRightParenOnNewLine;
        Boolean useContinuationIndent;
    }

    @Value
    @With
    public static class FunctionParentheses {
        Boolean alignWhenMultiline;
    }

    @Value
    @With
    public static class ChainedFunctionCalls {
        Boolean wrapFirstCall;
        Boolean useContinuationIndent;
    }

    @Value
    @With
    public static class IfStatement {
        Boolean elseOnNewLine;
        Boolean placeRightParenOnNewLine;
        Boolean useContinuationIndentInConditions;
    }

    @Value
    @With
    public static class DoWhileStatement {
        Boolean whileOnNewLine;
    }

    @Value
    @With
    public static class TryStatement {
        Boolean catchOnNewLine;
        Boolean finallyOnNewLine;
    }

    @Value
    @With
    public static class BinaryExpression {
        Boolean alignWhenMultiline;
    }

    @Value
    @With
    public static class WhenStatements {
        Boolean alignWhenBranchesInColumns;
        Boolean newLineAfterMultilineEntry;
    }

    @Value
    @With
    public static class BracesPlacement {
        Boolean putLeftBraceOnNewLine;
    }

    @Value
    @With
    public static class ExpressionBodyFunctions {
        Boolean useContinuationIndent;
    }

    @Value
    @With
    public static class ElvisExpressions {
        Boolean useContinuationIndent;
    }

    @Override
    public Style applyDefaults() {
        return StyleHelper.merge(IntelliJ.wrappingAndBraces(), this);
    }
}
