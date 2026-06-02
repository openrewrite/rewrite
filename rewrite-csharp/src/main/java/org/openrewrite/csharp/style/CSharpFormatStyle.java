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
package org.openrewrite.csharp.style;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.openrewrite.rpc.RpcCodec;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.style.Style;

import java.util.Collection;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;

/**
 * Marker attached to C# CompilationUnits carrying detected or configured formatting style.
 * Populated from .editorconfig files when available, otherwise from Roslyn defaults.
 * Covers all options exposed by Roslyn's {@code CSharpFormattingOptions}.
 * <p>
 * Boolean flags are packed into a single {@code long} for compact RPC serialization.
 * Bit positions are assigned in declaration order — new flags must be appended at the end.
 */
@Getter
@EqualsAndHashCode(callSuper = true)
public class CSharpFormatStyle extends NamedStyles implements RpcCodec<CSharpFormatStyle> {
    private static final String NAME = "org.openrewrite.csharp.CSharpFormatStyle";
    private static final String DISPLAY_NAME = "C# Format Style";
    private static final String DESCRIPTION = "C# formatting style from .editorconfig or Roslyn defaults.";

    /**
     * Roslyn default flags: 27 of 47 bits set (Allman style, standard spacing).
     */
    public static final long DEFAULT_FLAGS = 0x0000600033BFFFFAL;

    // Bit positions — must match C# side exactly (append-only, do not reorder)
    private static final int BIT_USE_TABS = 0;
    private static final int BIT_INDENT_BLOCK = 1;
    private static final int BIT_INDENT_BRACES = 2;
    private static final int BIT_INDENT_SWITCH_CASE_SECTION = 3;
    private static final int BIT_INDENT_SWITCH_CASE_SECTION_WHEN_BLOCK = 4;
    private static final int BIT_INDENT_SWITCH_SECTION = 5;
    private static final int BIT_NEW_LINES_FOR_BRACES_IN_TYPES = 6;
    private static final int BIT_NEW_LINES_FOR_BRACES_IN_METHODS = 7;
    private static final int BIT_NEW_LINES_FOR_BRACES_IN_PROPERTIES = 8;
    private static final int BIT_NEW_LINES_FOR_BRACES_IN_ACCESSORS = 9;
    private static final int BIT_NEW_LINES_FOR_BRACES_IN_ANONYMOUS_METHODS = 10;
    private static final int BIT_NEW_LINES_FOR_BRACES_IN_ANONYMOUS_TYPES = 11;
    private static final int BIT_NEW_LINES_FOR_BRACES_IN_CONTROL_BLOCKS = 12;
    private static final int BIT_NEW_LINES_FOR_BRACES_IN_LAMBDA_EXPRESSION_BODY = 13;
    private static final int BIT_NEW_LINES_FOR_BRACES_IN_OBJECT_COLLECTION_ARRAY_INITIALIZERS = 14;
    private static final int BIT_NEW_LINES_FOR_BRACES_IN_LOCAL_FUNCTIONS = 15;
    private static final int BIT_NEW_LINE_BEFORE_ELSE = 16;
    private static final int BIT_NEW_LINE_BEFORE_CATCH = 17;
    private static final int BIT_NEW_LINE_BEFORE_FINALLY = 18;
    private static final int BIT_NEW_LINE_FOR_CLAUSES_IN_QUERY = 19;
    private static final int BIT_NEW_LINE_FOR_MEMBERS_IN_ANONYMOUS_TYPES = 20;
    private static final int BIT_NEW_LINE_FOR_MEMBERS_IN_OBJECT_INIT = 21;
    private static final int BIT_SPACE_AFTER_CAST = 22;
    private static final int BIT_SPACE_AFTER_COLON_IN_BASE_TYPE_DECLARATION = 23;
    private static final int BIT_SPACE_AFTER_COMMA = 24;
    private static final int BIT_SPACE_AFTER_CONTROL_FLOW_STATEMENT_KEYWORD = 25;
    private static final int BIT_SPACE_AFTER_DOT = 26;
    private static final int BIT_SPACE_AFTER_METHOD_CALL_NAME = 27;
    private static final int BIT_SPACE_AFTER_SEMICOLONS_IN_FOR_STATEMENT = 28;
    private static final int BIT_SPACE_BEFORE_COLON_IN_BASE_TYPE_DECLARATION = 29;
    private static final int BIT_SPACE_BEFORE_COMMA = 30;
    private static final int BIT_SPACE_BEFORE_DOT = 31;
    private static final int BIT_SPACE_BEFORE_OPEN_SQUARE_BRACKET = 32;
    private static final int BIT_SPACE_BEFORE_SEMICOLONS_IN_FOR_STATEMENT = 33;
    private static final int BIT_SPACE_BETWEEN_EMPTY_METHOD_CALL_PARENTHESES = 34;
    private static final int BIT_SPACE_BETWEEN_EMPTY_METHOD_DECLARATION_PARENTHESES = 35;
    private static final int BIT_SPACE_BETWEEN_EMPTY_SQUARE_BRACKETS = 36;
    private static final int BIT_SPACES_IGNORE_AROUND_VARIABLE_DECLARATION = 37;
    private static final int BIT_SPACE_WITHIN_CAST_PARENTHESES = 38;
    private static final int BIT_SPACE_WITHIN_EXPRESSION_PARENTHESES = 39;
    private static final int BIT_SPACE_WITHIN_METHOD_CALL_PARENTHESES = 40;
    private static final int BIT_SPACE_WITHIN_METHOD_DECLARATION_PARENTHESIS = 41;
    private static final int BIT_SPACE_WITHIN_OTHER_PARENTHESES = 42;
    private static final int BIT_SPACE_WITHIN_SQUARE_BRACKETS = 43;
    private static final int BIT_SPACING_AFTER_METHOD_DECLARATION_NAME = 44;
    private static final int BIT_WRAPPING_PRESERVE_SINGLE_LINE = 45;
    private static final int BIT_WRAPPING_KEEP_STATEMENTS_ON_SINGLE_LINE = 46;

    private final long flags;
    private final int indentSize;
    private final int tabSize;
    private final String newLine;
    private final int labelPositioning;
    private final int spacingAroundBinaryOperator;

    private boolean flag(int bit) {
        return (flags & (1L << bit)) != 0;
    }

    // Boolean flag accessors
    public boolean isUseTabs() { return flag(BIT_USE_TABS); }
    public boolean isIndentBlock() { return flag(BIT_INDENT_BLOCK); }
    public boolean isIndentBraces() { return flag(BIT_INDENT_BRACES); }
    public boolean isIndentSwitchCaseSection() { return flag(BIT_INDENT_SWITCH_CASE_SECTION); }
    public boolean isIndentSwitchCaseSectionWhenBlock() { return flag(BIT_INDENT_SWITCH_CASE_SECTION_WHEN_BLOCK); }
    public boolean isIndentSwitchSection() { return flag(BIT_INDENT_SWITCH_SECTION); }
    public boolean isNewLinesForBracesInTypes() { return flag(BIT_NEW_LINES_FOR_BRACES_IN_TYPES); }
    public boolean isNewLinesForBracesInMethods() { return flag(BIT_NEW_LINES_FOR_BRACES_IN_METHODS); }
    public boolean isNewLinesForBracesInProperties() { return flag(BIT_NEW_LINES_FOR_BRACES_IN_PROPERTIES); }
    public boolean isNewLinesForBracesInAccessors() { return flag(BIT_NEW_LINES_FOR_BRACES_IN_ACCESSORS); }
    public boolean isNewLinesForBracesInAnonymousMethods() { return flag(BIT_NEW_LINES_FOR_BRACES_IN_ANONYMOUS_METHODS); }
    public boolean isNewLinesForBracesInAnonymousTypes() { return flag(BIT_NEW_LINES_FOR_BRACES_IN_ANONYMOUS_TYPES); }
    public boolean isNewLinesForBracesInControlBlocks() { return flag(BIT_NEW_LINES_FOR_BRACES_IN_CONTROL_BLOCKS); }
    public boolean isNewLinesForBracesInLambdaExpressionBody() { return flag(BIT_NEW_LINES_FOR_BRACES_IN_LAMBDA_EXPRESSION_BODY); }
    public boolean isNewLinesForBracesInObjectCollectionArrayInitializers() { return flag(BIT_NEW_LINES_FOR_BRACES_IN_OBJECT_COLLECTION_ARRAY_INITIALIZERS); }
    public boolean isNewLinesForBracesInLocalFunctions() { return flag(BIT_NEW_LINES_FOR_BRACES_IN_LOCAL_FUNCTIONS); }
    public boolean isNewLineBeforeElse() { return flag(BIT_NEW_LINE_BEFORE_ELSE); }
    public boolean isNewLineBeforeCatch() { return flag(BIT_NEW_LINE_BEFORE_CATCH); }
    public boolean isNewLineBeforeFinally() { return flag(BIT_NEW_LINE_BEFORE_FINALLY); }
    public boolean isNewLineForClausesInQuery() { return flag(BIT_NEW_LINE_FOR_CLAUSES_IN_QUERY); }
    public boolean isNewLineForMembersInAnonymousTypes() { return flag(BIT_NEW_LINE_FOR_MEMBERS_IN_ANONYMOUS_TYPES); }
    public boolean isNewLineForMembersInObjectInit() { return flag(BIT_NEW_LINE_FOR_MEMBERS_IN_OBJECT_INIT); }
    public boolean isSpaceAfterCast() { return flag(BIT_SPACE_AFTER_CAST); }
    public boolean isSpaceAfterColonInBaseTypeDeclaration() { return flag(BIT_SPACE_AFTER_COLON_IN_BASE_TYPE_DECLARATION); }
    public boolean isSpaceAfterComma() { return flag(BIT_SPACE_AFTER_COMMA); }
    public boolean isSpaceAfterControlFlowStatementKeyword() { return flag(BIT_SPACE_AFTER_CONTROL_FLOW_STATEMENT_KEYWORD); }
    public boolean isSpaceAfterDot() { return flag(BIT_SPACE_AFTER_DOT); }
    public boolean isSpaceAfterMethodCallName() { return flag(BIT_SPACE_AFTER_METHOD_CALL_NAME); }
    public boolean isSpaceAfterSemicolonsInForStatement() { return flag(BIT_SPACE_AFTER_SEMICOLONS_IN_FOR_STATEMENT); }
    public boolean isSpaceBeforeColonInBaseTypeDeclaration() { return flag(BIT_SPACE_BEFORE_COLON_IN_BASE_TYPE_DECLARATION); }
    public boolean isSpaceBeforeComma() { return flag(BIT_SPACE_BEFORE_COMMA); }
    public boolean isSpaceBeforeDot() { return flag(BIT_SPACE_BEFORE_DOT); }
    public boolean isSpaceBeforeOpenSquareBracket() { return flag(BIT_SPACE_BEFORE_OPEN_SQUARE_BRACKET); }
    public boolean isSpaceBeforeSemicolonsInForStatement() { return flag(BIT_SPACE_BEFORE_SEMICOLONS_IN_FOR_STATEMENT); }
    public boolean isSpaceBetweenEmptyMethodCallParentheses() { return flag(BIT_SPACE_BETWEEN_EMPTY_METHOD_CALL_PARENTHESES); }
    public boolean isSpaceBetweenEmptyMethodDeclarationParentheses() { return flag(BIT_SPACE_BETWEEN_EMPTY_METHOD_DECLARATION_PARENTHESES); }
    public boolean isSpaceBetweenEmptySquareBrackets() { return flag(BIT_SPACE_BETWEEN_EMPTY_SQUARE_BRACKETS); }
    public boolean isSpacesIgnoreAroundVariableDeclaration() { return flag(BIT_SPACES_IGNORE_AROUND_VARIABLE_DECLARATION); }
    public boolean isSpaceWithinCastParentheses() { return flag(BIT_SPACE_WITHIN_CAST_PARENTHESES); }
    public boolean isSpaceWithinExpressionParentheses() { return flag(BIT_SPACE_WITHIN_EXPRESSION_PARENTHESES); }
    public boolean isSpaceWithinMethodCallParentheses() { return flag(BIT_SPACE_WITHIN_METHOD_CALL_PARENTHESES); }
    public boolean isSpaceWithinMethodDeclarationParenthesis() { return flag(BIT_SPACE_WITHIN_METHOD_DECLARATION_PARENTHESIS); }
    public boolean isSpaceWithinOtherParentheses() { return flag(BIT_SPACE_WITHIN_OTHER_PARENTHESES); }
    public boolean isSpaceWithinSquareBrackets() { return flag(BIT_SPACE_WITHIN_SQUARE_BRACKETS); }
    public boolean isSpacingAfterMethodDeclarationName() { return flag(BIT_SPACING_AFTER_METHOD_DECLARATION_NAME); }
    public boolean isWrappingPreserveSingleLine() { return flag(BIT_WRAPPING_PRESERVE_SINGLE_LINE); }
    public boolean isWrappingKeepStatementsOnSingleLine() { return flag(BIT_WRAPPING_KEEP_STATEMENTS_ON_SINGLE_LINE); }

    public CSharpFormatStyle(UUID id, long flags, int indentSize, int tabSize, String newLine,
                             int labelPositioning, int spacingAroundBinaryOperator) {
        super(id, NAME, DISPLAY_NAME, DESCRIPTION, emptySet(), emptyList());
        this.flags = flags;
        this.indentSize = indentSize;
        this.tabSize = tabSize;
        this.newLine = newLine;
        this.labelPositioning = labelPositioning;
        this.spacingAroundBinaryOperator = spacingAroundBinaryOperator;
    }

    @Override
    public CSharpFormatStyle withId(UUID id) {
        return id == getId() ? this : new CSharpFormatStyle(id, flags, indentSize, tabSize, newLine,
                labelPositioning, spacingAroundBinaryOperator);
    }

    @Override
    public CSharpFormatStyle withStyles(Collection<Style> styles) {
        return this;
    }

    @Override
    public void rpcSend(CSharpFormatStyle after, RpcSendQueue q) {
        q.getAndSend(after, NamedStyles::getId);
        q.getAndSend(after, m -> m.flags);
        q.getAndSend(after, m -> m.indentSize);
        q.getAndSend(after, m -> m.tabSize);
        q.getAndSend(after, m -> m.newLine);
        q.getAndSend(after, m -> m.labelPositioning);
        q.getAndSend(after, m -> m.spacingAroundBinaryOperator);
    }

    @Override
    public CSharpFormatStyle rpcReceive(CSharpFormatStyle before, RpcReceiveQueue q) {
        return new CSharpFormatStyle(
                q.receiveAndGet(before.getId(), UUID::fromString),
                q.receive(before.flags),
                q.receive(before.indentSize),
                q.receive(before.tabSize),
                q.<String, String>receiveAndGet(before.newLine, x -> x),
                q.receive(before.labelPositioning),
                q.receive(before.spacingAroundBinaryOperator)
        );
    }
}
