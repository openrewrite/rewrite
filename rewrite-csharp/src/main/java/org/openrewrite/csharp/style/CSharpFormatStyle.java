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
 */
@Getter
@EqualsAndHashCode(callSuper = true)
public class CSharpFormatStyle extends NamedStyles implements RpcCodec<CSharpFormatStyle> {
    private static final String NAME = "org.openrewrite.csharp.CSharpFormatStyle";
    private static final String DISPLAY_NAME = "C# Format Style";
    private static final String DESCRIPTION = "C# formatting style from .editorconfig or Roslyn defaults.";

    // General
    private final boolean useTabs;
    private final int indentSize;
    private final int tabSize;
    private final String newLine;

    // Indentation
    private final boolean indentBlock;
    private final boolean indentBraces;
    private final boolean indentSwitchCaseSection;
    private final boolean indentSwitchCaseSectionWhenBlock;
    private final boolean indentSwitchSection;
    private final int labelPositioning;

    // Brace placement
    private final boolean newLinesForBracesInTypes;
    private final boolean newLinesForBracesInMethods;
    private final boolean newLinesForBracesInProperties;
    private final boolean newLinesForBracesInAccessors;
    private final boolean newLinesForBracesInAnonymousMethods;
    private final boolean newLinesForBracesInAnonymousTypes;
    private final boolean newLinesForBracesInControlBlocks;
    private final boolean newLinesForBracesInLambdaExpressionBody;
    private final boolean newLinesForBracesInObjectCollectionArrayInitializers;
    private final boolean newLinesForBracesInLocalFunctions;

    // New line before keywords / members
    private final boolean newLineBeforeElse;
    private final boolean newLineBeforeCatch;
    private final boolean newLineBeforeFinally;
    private final boolean newLineForClausesInQuery;
    private final boolean newLineForMembersInAnonymousTypes;
    private final boolean newLineForMembersInObjectInit;

    // Spacing
    private final boolean spaceAfterCast;
    private final boolean spaceAfterColonInBaseTypeDeclaration;
    private final boolean spaceAfterComma;
    private final boolean spaceAfterControlFlowStatementKeyword;
    private final boolean spaceAfterDot;
    private final boolean spaceAfterMethodCallName;
    private final boolean spaceAfterSemicolonsInForStatement;
    private final boolean spaceBeforeColonInBaseTypeDeclaration;
    private final boolean spaceBeforeComma;
    private final boolean spaceBeforeDot;
    private final boolean spaceBeforeOpenSquareBracket;
    private final boolean spaceBeforeSemicolonsInForStatement;
    private final boolean spaceBetweenEmptyMethodCallParentheses;
    private final boolean spaceBetweenEmptyMethodDeclarationParentheses;
    private final boolean spaceBetweenEmptySquareBrackets;
    private final boolean spacesIgnoreAroundVariableDeclaration;
    private final boolean spaceWithinCastParentheses;
    private final boolean spaceWithinExpressionParentheses;
    private final boolean spaceWithinMethodCallParentheses;
    private final boolean spaceWithinMethodDeclarationParenthesis;
    private final boolean spaceWithinOtherParentheses;
    private final boolean spaceWithinSquareBrackets;
    private final boolean spacingAfterMethodDeclarationName;
    private final int spacingAroundBinaryOperator;

    // Wrapping
    private final boolean wrappingPreserveSingleLine;
    private final boolean wrappingKeepStatementsOnSingleLine;

    public CSharpFormatStyle(
            UUID id, boolean useTabs, int indentSize, int tabSize, String newLine,
            boolean indentBlock, boolean indentBraces, boolean indentSwitchCaseSection,
            boolean indentSwitchCaseSectionWhenBlock, boolean indentSwitchSection, int labelPositioning,
            boolean newLinesForBracesInTypes, boolean newLinesForBracesInMethods,
            boolean newLinesForBracesInProperties, boolean newLinesForBracesInAccessors,
            boolean newLinesForBracesInAnonymousMethods, boolean newLinesForBracesInAnonymousTypes,
            boolean newLinesForBracesInControlBlocks, boolean newLinesForBracesInLambdaExpressionBody,
            boolean newLinesForBracesInObjectCollectionArrayInitializers, boolean newLinesForBracesInLocalFunctions,
            boolean newLineBeforeElse, boolean newLineBeforeCatch, boolean newLineBeforeFinally,
            boolean newLineForClausesInQuery, boolean newLineForMembersInAnonymousTypes,
            boolean newLineForMembersInObjectInit,
            boolean spaceAfterCast, boolean spaceAfterColonInBaseTypeDeclaration, boolean spaceAfterComma,
            boolean spaceAfterControlFlowStatementKeyword, boolean spaceAfterDot, boolean spaceAfterMethodCallName,
            boolean spaceAfterSemicolonsInForStatement, boolean spaceBeforeColonInBaseTypeDeclaration,
            boolean spaceBeforeComma, boolean spaceBeforeDot, boolean spaceBeforeOpenSquareBracket,
            boolean spaceBeforeSemicolonsInForStatement, boolean spaceBetweenEmptyMethodCallParentheses,
            boolean spaceBetweenEmptyMethodDeclarationParentheses, boolean spaceBetweenEmptySquareBrackets,
            boolean spacesIgnoreAroundVariableDeclaration, boolean spaceWithinCastParentheses,
            boolean spaceWithinExpressionParentheses, boolean spaceWithinMethodCallParentheses,
            boolean spaceWithinMethodDeclarationParenthesis, boolean spaceWithinOtherParentheses,
            boolean spaceWithinSquareBrackets, boolean spacingAfterMethodDeclarationName,
            int spacingAroundBinaryOperator,
            boolean wrappingPreserveSingleLine, boolean wrappingKeepStatementsOnSingleLine) {
        super(id, NAME, DISPLAY_NAME, DESCRIPTION, emptySet(), emptyList());
        this.useTabs = useTabs; this.indentSize = indentSize; this.tabSize = tabSize; this.newLine = newLine;
        this.indentBlock = indentBlock; this.indentBraces = indentBraces;
        this.indentSwitchCaseSection = indentSwitchCaseSection;
        this.indentSwitchCaseSectionWhenBlock = indentSwitchCaseSectionWhenBlock;
        this.indentSwitchSection = indentSwitchSection; this.labelPositioning = labelPositioning;
        this.newLinesForBracesInTypes = newLinesForBracesInTypes;
        this.newLinesForBracesInMethods = newLinesForBracesInMethods;
        this.newLinesForBracesInProperties = newLinesForBracesInProperties;
        this.newLinesForBracesInAccessors = newLinesForBracesInAccessors;
        this.newLinesForBracesInAnonymousMethods = newLinesForBracesInAnonymousMethods;
        this.newLinesForBracesInAnonymousTypes = newLinesForBracesInAnonymousTypes;
        this.newLinesForBracesInControlBlocks = newLinesForBracesInControlBlocks;
        this.newLinesForBracesInLambdaExpressionBody = newLinesForBracesInLambdaExpressionBody;
        this.newLinesForBracesInObjectCollectionArrayInitializers = newLinesForBracesInObjectCollectionArrayInitializers;
        this.newLinesForBracesInLocalFunctions = newLinesForBracesInLocalFunctions;
        this.newLineBeforeElse = newLineBeforeElse; this.newLineBeforeCatch = newLineBeforeCatch;
        this.newLineBeforeFinally = newLineBeforeFinally;
        this.newLineForClausesInQuery = newLineForClausesInQuery;
        this.newLineForMembersInAnonymousTypes = newLineForMembersInAnonymousTypes;
        this.newLineForMembersInObjectInit = newLineForMembersInObjectInit;
        this.spaceAfterCast = spaceAfterCast;
        this.spaceAfterColonInBaseTypeDeclaration = spaceAfterColonInBaseTypeDeclaration;
        this.spaceAfterComma = spaceAfterComma;
        this.spaceAfterControlFlowStatementKeyword = spaceAfterControlFlowStatementKeyword;
        this.spaceAfterDot = spaceAfterDot; this.spaceAfterMethodCallName = spaceAfterMethodCallName;
        this.spaceAfterSemicolonsInForStatement = spaceAfterSemicolonsInForStatement;
        this.spaceBeforeColonInBaseTypeDeclaration = spaceBeforeColonInBaseTypeDeclaration;
        this.spaceBeforeComma = spaceBeforeComma; this.spaceBeforeDot = spaceBeforeDot;
        this.spaceBeforeOpenSquareBracket = spaceBeforeOpenSquareBracket;
        this.spaceBeforeSemicolonsInForStatement = spaceBeforeSemicolonsInForStatement;
        this.spaceBetweenEmptyMethodCallParentheses = spaceBetweenEmptyMethodCallParentheses;
        this.spaceBetweenEmptyMethodDeclarationParentheses = spaceBetweenEmptyMethodDeclarationParentheses;
        this.spaceBetweenEmptySquareBrackets = spaceBetweenEmptySquareBrackets;
        this.spacesIgnoreAroundVariableDeclaration = spacesIgnoreAroundVariableDeclaration;
        this.spaceWithinCastParentheses = spaceWithinCastParentheses;
        this.spaceWithinExpressionParentheses = spaceWithinExpressionParentheses;
        this.spaceWithinMethodCallParentheses = spaceWithinMethodCallParentheses;
        this.spaceWithinMethodDeclarationParenthesis = spaceWithinMethodDeclarationParenthesis;
        this.spaceWithinOtherParentheses = spaceWithinOtherParentheses;
        this.spaceWithinSquareBrackets = spaceWithinSquareBrackets;
        this.spacingAfterMethodDeclarationName = spacingAfterMethodDeclarationName;
        this.spacingAroundBinaryOperator = spacingAroundBinaryOperator;
        this.wrappingPreserveSingleLine = wrappingPreserveSingleLine;
        this.wrappingKeepStatementsOnSingleLine = wrappingKeepStatementsOnSingleLine;
    }

    @Override
    public CSharpFormatStyle withId(UUID id) {
        return id == getId() ? this : new CSharpFormatStyle(id,
                useTabs, indentSize, tabSize, newLine,
                indentBlock, indentBraces, indentSwitchCaseSection, indentSwitchCaseSectionWhenBlock,
                indentSwitchSection, labelPositioning,
                newLinesForBracesInTypes, newLinesForBracesInMethods, newLinesForBracesInProperties,
                newLinesForBracesInAccessors, newLinesForBracesInAnonymousMethods, newLinesForBracesInAnonymousTypes,
                newLinesForBracesInControlBlocks, newLinesForBracesInLambdaExpressionBody,
                newLinesForBracesInObjectCollectionArrayInitializers, newLinesForBracesInLocalFunctions,
                newLineBeforeElse, newLineBeforeCatch, newLineBeforeFinally,
                newLineForClausesInQuery, newLineForMembersInAnonymousTypes, newLineForMembersInObjectInit,
                spaceAfterCast, spaceAfterColonInBaseTypeDeclaration, spaceAfterComma,
                spaceAfterControlFlowStatementKeyword, spaceAfterDot, spaceAfterMethodCallName,
                spaceAfterSemicolonsInForStatement, spaceBeforeColonInBaseTypeDeclaration,
                spaceBeforeComma, spaceBeforeDot, spaceBeforeOpenSquareBracket,
                spaceBeforeSemicolonsInForStatement, spaceBetweenEmptyMethodCallParentheses,
                spaceBetweenEmptyMethodDeclarationParentheses, spaceBetweenEmptySquareBrackets,
                spacesIgnoreAroundVariableDeclaration, spaceWithinCastParentheses,
                spaceWithinExpressionParentheses, spaceWithinMethodCallParentheses,
                spaceWithinMethodDeclarationParenthesis, spaceWithinOtherParentheses,
                spaceWithinSquareBrackets, spacingAfterMethodDeclarationName, spacingAroundBinaryOperator,
                wrappingPreserveSingleLine, wrappingKeepStatementsOnSingleLine);
    }

    @Override
    public CSharpFormatStyle withStyles(Collection<Style> styles) {
        return this;
    }

    @Override
    public void rpcSend(CSharpFormatStyle after, RpcSendQueue q) {
        q.getAndSend(after, NamedStyles::getId);
        q.getAndSend(after, m -> m.useTabs); q.getAndSend(after, m -> m.indentSize);
        q.getAndSend(after, m -> m.tabSize); q.getAndSend(after, m -> m.newLine);
        q.getAndSend(after, m -> m.indentBlock); q.getAndSend(after, m -> m.indentBraces);
        q.getAndSend(after, m -> m.indentSwitchCaseSection);
        q.getAndSend(after, m -> m.indentSwitchCaseSectionWhenBlock);
        q.getAndSend(after, m -> m.indentSwitchSection); q.getAndSend(after, m -> m.labelPositioning);
        q.getAndSend(after, m -> m.newLinesForBracesInTypes);
        q.getAndSend(after, m -> m.newLinesForBracesInMethods);
        q.getAndSend(after, m -> m.newLinesForBracesInProperties);
        q.getAndSend(after, m -> m.newLinesForBracesInAccessors);
        q.getAndSend(after, m -> m.newLinesForBracesInAnonymousMethods);
        q.getAndSend(after, m -> m.newLinesForBracesInAnonymousTypes);
        q.getAndSend(after, m -> m.newLinesForBracesInControlBlocks);
        q.getAndSend(after, m -> m.newLinesForBracesInLambdaExpressionBody);
        q.getAndSend(after, m -> m.newLinesForBracesInObjectCollectionArrayInitializers);
        q.getAndSend(after, m -> m.newLinesForBracesInLocalFunctions);
        q.getAndSend(after, m -> m.newLineBeforeElse); q.getAndSend(after, m -> m.newLineBeforeCatch);
        q.getAndSend(after, m -> m.newLineBeforeFinally);
        q.getAndSend(after, m -> m.newLineForClausesInQuery);
        q.getAndSend(after, m -> m.newLineForMembersInAnonymousTypes);
        q.getAndSend(after, m -> m.newLineForMembersInObjectInit);
        q.getAndSend(after, m -> m.spaceAfterCast);
        q.getAndSend(after, m -> m.spaceAfterColonInBaseTypeDeclaration);
        q.getAndSend(after, m -> m.spaceAfterComma);
        q.getAndSend(after, m -> m.spaceAfterControlFlowStatementKeyword);
        q.getAndSend(after, m -> m.spaceAfterDot); q.getAndSend(after, m -> m.spaceAfterMethodCallName);
        q.getAndSend(after, m -> m.spaceAfterSemicolonsInForStatement);
        q.getAndSend(after, m -> m.spaceBeforeColonInBaseTypeDeclaration);
        q.getAndSend(after, m -> m.spaceBeforeComma); q.getAndSend(after, m -> m.spaceBeforeDot);
        q.getAndSend(after, m -> m.spaceBeforeOpenSquareBracket);
        q.getAndSend(after, m -> m.spaceBeforeSemicolonsInForStatement);
        q.getAndSend(after, m -> m.spaceBetweenEmptyMethodCallParentheses);
        q.getAndSend(after, m -> m.spaceBetweenEmptyMethodDeclarationParentheses);
        q.getAndSend(after, m -> m.spaceBetweenEmptySquareBrackets);
        q.getAndSend(after, m -> m.spacesIgnoreAroundVariableDeclaration);
        q.getAndSend(after, m -> m.spaceWithinCastParentheses);
        q.getAndSend(after, m -> m.spaceWithinExpressionParentheses);
        q.getAndSend(after, m -> m.spaceWithinMethodCallParentheses);
        q.getAndSend(after, m -> m.spaceWithinMethodDeclarationParenthesis);
        q.getAndSend(after, m -> m.spaceWithinOtherParentheses);
        q.getAndSend(after, m -> m.spaceWithinSquareBrackets);
        q.getAndSend(after, m -> m.spacingAfterMethodDeclarationName);
        q.getAndSend(after, m -> m.spacingAroundBinaryOperator);
        q.getAndSend(after, m -> m.wrappingPreserveSingleLine);
        q.getAndSend(after, m -> m.wrappingKeepStatementsOnSingleLine);
    }

    @Override
    public CSharpFormatStyle rpcReceive(CSharpFormatStyle before, RpcReceiveQueue q) {
        return new CSharpFormatStyle(
                q.receiveAndGet(before.getId(), UUID::fromString),
                q.receive(before.useTabs), q.receive(before.indentSize),
                q.receive(before.tabSize), q.<String, String>receiveAndGet(before.newLine, x -> x),
                q.receive(before.indentBlock), q.receive(before.indentBraces),
                q.receive(before.indentSwitchCaseSection), q.receive(before.indentSwitchCaseSectionWhenBlock),
                q.receive(before.indentSwitchSection), q.receive(before.labelPositioning),
                q.receive(before.newLinesForBracesInTypes), q.receive(before.newLinesForBracesInMethods),
                q.receive(before.newLinesForBracesInProperties), q.receive(before.newLinesForBracesInAccessors),
                q.receive(before.newLinesForBracesInAnonymousMethods), q.receive(before.newLinesForBracesInAnonymousTypes),
                q.receive(before.newLinesForBracesInControlBlocks), q.receive(before.newLinesForBracesInLambdaExpressionBody),
                q.receive(before.newLinesForBracesInObjectCollectionArrayInitializers), q.receive(before.newLinesForBracesInLocalFunctions),
                q.receive(before.newLineBeforeElse), q.receive(before.newLineBeforeCatch),
                q.receive(before.newLineBeforeFinally), q.receive(before.newLineForClausesInQuery),
                q.receive(before.newLineForMembersInAnonymousTypes), q.receive(before.newLineForMembersInObjectInit),
                q.receive(before.spaceAfterCast), q.receive(before.spaceAfterColonInBaseTypeDeclaration),
                q.receive(before.spaceAfterComma), q.receive(before.spaceAfterControlFlowStatementKeyword),
                q.receive(before.spaceAfterDot), q.receive(before.spaceAfterMethodCallName),
                q.receive(before.spaceAfterSemicolonsInForStatement), q.receive(before.spaceBeforeColonInBaseTypeDeclaration),
                q.receive(before.spaceBeforeComma), q.receive(before.spaceBeforeDot),
                q.receive(before.spaceBeforeOpenSquareBracket), q.receive(before.spaceBeforeSemicolonsInForStatement),
                q.receive(before.spaceBetweenEmptyMethodCallParentheses), q.receive(before.spaceBetweenEmptyMethodDeclarationParentheses),
                q.receive(before.spaceBetweenEmptySquareBrackets), q.receive(before.spacesIgnoreAroundVariableDeclaration),
                q.receive(before.spaceWithinCastParentheses), q.receive(before.spaceWithinExpressionParentheses),
                q.receive(before.spaceWithinMethodCallParentheses), q.receive(before.spaceWithinMethodDeclarationParenthesis),
                q.receive(before.spaceWithinOtherParentheses), q.receive(before.spaceWithinSquareBrackets),
                q.receive(before.spacingAfterMethodDeclarationName), q.receive(before.spacingAroundBinaryOperator),
                q.receive(before.wrappingPreserveSingleLine), q.receive(before.wrappingKeepStatementsOnSingleLine)
        );
    }
}
