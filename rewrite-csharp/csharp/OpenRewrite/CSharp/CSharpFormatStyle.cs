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
using OpenRewrite.Core;
using OpenRewrite.Core.Rpc;

namespace OpenRewrite.CSharp;

/// <summary>
/// Marker attached to C# CompilationUnits carrying detected or configured formatting style.
/// Populated from .editorconfig files when available, otherwise from Roslyn defaults.
/// Covers all options exposed by Roslyn's <c>CSharpFormattingOptions</c>.
/// Used by <see cref="Format.RoslynFormatter"/> to configure the Roslyn formatting workspace.
/// </summary>
public sealed class CSharpFormatStyle : Marker, IRpcCodec<CSharpFormatStyle>, IEquatable<CSharpFormatStyle>
{
    public Guid Id { get; }

    // General
    public bool UseTabs { get; }
    public int IndentSize { get; }
    public int TabSize { get; }
    public string NewLine { get; }

    // Indentation
    public bool IndentBlock { get; }
    public bool IndentBraces { get; }
    public bool IndentSwitchCaseSection { get; }
    public bool IndentSwitchCaseSectionWhenBlock { get; }
    public bool IndentSwitchSection { get; }
    /// <summary>Label positioning: 0=LeftMost (flush_left), 1=OneLess (one_less_than_current), 2=NoIndent (no_change).</summary>
    public int LabelPositioning { get; }

    // Brace placement
    public bool NewLinesForBracesInTypes { get; }
    public bool NewLinesForBracesInMethods { get; }
    public bool NewLinesForBracesInProperties { get; }
    public bool NewLinesForBracesInAccessors { get; }
    public bool NewLinesForBracesInAnonymousMethods { get; }
    public bool NewLinesForBracesInAnonymousTypes { get; }
    public bool NewLinesForBracesInControlBlocks { get; }
    public bool NewLinesForBracesInLambdaExpressionBody { get; }
    public bool NewLinesForBracesInObjectCollectionArrayInitializers { get; }
    public bool NewLinesForBracesInLocalFunctions { get; }

    // New line before keywords
    public bool NewLineBeforeElse { get; }
    public bool NewLineBeforeCatch { get; }
    public bool NewLineBeforeFinally { get; }
    public bool NewLineForClausesInQuery { get; }
    public bool NewLineForMembersInAnonymousTypes { get; }
    public bool NewLineForMembersInObjectInit { get; }

    // Spacing
    public bool SpaceAfterCast { get; }
    public bool SpaceAfterColonInBaseTypeDeclaration { get; }
    public bool SpaceAfterComma { get; }
    public bool SpaceAfterControlFlowStatementKeyword { get; }
    public bool SpaceAfterDot { get; }
    public bool SpaceAfterMethodCallName { get; }
    public bool SpaceAfterSemicolonsInForStatement { get; }
    public bool SpaceBeforeColonInBaseTypeDeclaration { get; }
    public bool SpaceBeforeComma { get; }
    public bool SpaceBeforeDot { get; }
    public bool SpaceBeforeOpenSquareBracket { get; }
    public bool SpaceBeforeSemicolonsInForStatement { get; }
    public bool SpaceBetweenEmptyMethodCallParentheses { get; }
    public bool SpaceBetweenEmptyMethodDeclarationParentheses { get; }
    public bool SpaceBetweenEmptySquareBrackets { get; }
    public bool SpacesIgnoreAroundVariableDeclaration { get; }
    public bool SpaceWithinCastParentheses { get; }
    public bool SpaceWithinExpressionParentheses { get; }
    public bool SpaceWithinMethodCallParentheses { get; }
    public bool SpaceWithinMethodDeclarationParenthesis { get; }
    public bool SpaceWithinOtherParentheses { get; }
    public bool SpaceWithinSquareBrackets { get; }
    public bool SpacingAfterMethodDeclarationName { get; }
    /// <summary>Binary operator spacing: 0=Single (before_and_after), 1=Ignore, 2=Remove (none).</summary>
    public int SpacingAroundBinaryOperator { get; }

    // Wrapping — stored for .editorconfig fidelity but intentionally overridden to false by
    // RoslynFormatter.BuildOptions because synthesized template nodes may lack structural newlines
    // and Roslyn must insert them.
    public bool WrappingPreserveSingleLine { get; }
    public bool WrappingKeepStatementsOnSingleLine { get; }

    public CSharpFormatStyle(
        Guid id, bool useTabs, int indentSize, int tabSize, string newLine,
        // Indentation
        bool indentBlock, bool indentBraces, bool indentSwitchCaseSection,
        bool indentSwitchCaseSectionWhenBlock, bool indentSwitchSection, int labelPositioning,
        // Brace placement
        bool newLinesForBracesInTypes, bool newLinesForBracesInMethods,
        bool newLinesForBracesInProperties, bool newLinesForBracesInAccessors,
        bool newLinesForBracesInAnonymousMethods, bool newLinesForBracesInAnonymousTypes,
        bool newLinesForBracesInControlBlocks, bool newLinesForBracesInLambdaExpressionBody,
        bool newLinesForBracesInObjectCollectionArrayInitializers, bool newLinesForBracesInLocalFunctions,
        // New line before keywords
        bool newLineBeforeElse, bool newLineBeforeCatch, bool newLineBeforeFinally,
        bool newLineForClausesInQuery, bool newLineForMembersInAnonymousTypes,
        bool newLineForMembersInObjectInit,
        // Spacing
        bool spaceAfterCast, bool spaceAfterColonInBaseTypeDeclaration, bool spaceAfterComma,
        bool spaceAfterControlFlowStatementKeyword, bool spaceAfterDot, bool spaceAfterMethodCallName,
        bool spaceAfterSemicolonsInForStatement, bool spaceBeforeColonInBaseTypeDeclaration,
        bool spaceBeforeComma, bool spaceBeforeDot, bool spaceBeforeOpenSquareBracket,
        bool spaceBeforeSemicolonsInForStatement, bool spaceBetweenEmptyMethodCallParentheses,
        bool spaceBetweenEmptyMethodDeclarationParentheses, bool spaceBetweenEmptySquareBrackets,
        bool spacesIgnoreAroundVariableDeclaration, bool spaceWithinCastParentheses,
        bool spaceWithinExpressionParentheses, bool spaceWithinMethodCallParentheses,
        bool spaceWithinMethodDeclarationParenthesis, bool spaceWithinOtherParentheses,
        bool spaceWithinSquareBrackets, bool spacingAfterMethodDeclarationName,
        int spacingAroundBinaryOperator,
        // Wrapping
        bool wrappingPreserveSingleLine, bool wrappingKeepStatementsOnSingleLine)
    {
        Id = id; UseTabs = useTabs; IndentSize = indentSize; TabSize = tabSize; NewLine = newLine;
        IndentBlock = indentBlock; IndentBraces = indentBraces;
        IndentSwitchCaseSection = indentSwitchCaseSection;
        IndentSwitchCaseSectionWhenBlock = indentSwitchCaseSectionWhenBlock;
        IndentSwitchSection = indentSwitchSection; LabelPositioning = labelPositioning;
        NewLinesForBracesInTypes = newLinesForBracesInTypes;
        NewLinesForBracesInMethods = newLinesForBracesInMethods;
        NewLinesForBracesInProperties = newLinesForBracesInProperties;
        NewLinesForBracesInAccessors = newLinesForBracesInAccessors;
        NewLinesForBracesInAnonymousMethods = newLinesForBracesInAnonymousMethods;
        NewLinesForBracesInAnonymousTypes = newLinesForBracesInAnonymousTypes;
        NewLinesForBracesInControlBlocks = newLinesForBracesInControlBlocks;
        NewLinesForBracesInLambdaExpressionBody = newLinesForBracesInLambdaExpressionBody;
        NewLinesForBracesInObjectCollectionArrayInitializers = newLinesForBracesInObjectCollectionArrayInitializers;
        NewLinesForBracesInLocalFunctions = newLinesForBracesInLocalFunctions;
        NewLineBeforeElse = newLineBeforeElse; NewLineBeforeCatch = newLineBeforeCatch;
        NewLineBeforeFinally = newLineBeforeFinally;
        NewLineForClausesInQuery = newLineForClausesInQuery;
        NewLineForMembersInAnonymousTypes = newLineForMembersInAnonymousTypes;
        NewLineForMembersInObjectInit = newLineForMembersInObjectInit;
        SpaceAfterCast = spaceAfterCast;
        SpaceAfterColonInBaseTypeDeclaration = spaceAfterColonInBaseTypeDeclaration;
        SpaceAfterComma = spaceAfterComma;
        SpaceAfterControlFlowStatementKeyword = spaceAfterControlFlowStatementKeyword;
        SpaceAfterDot = spaceAfterDot; SpaceAfterMethodCallName = spaceAfterMethodCallName;
        SpaceAfterSemicolonsInForStatement = spaceAfterSemicolonsInForStatement;
        SpaceBeforeColonInBaseTypeDeclaration = spaceBeforeColonInBaseTypeDeclaration;
        SpaceBeforeComma = spaceBeforeComma; SpaceBeforeDot = spaceBeforeDot;
        SpaceBeforeOpenSquareBracket = spaceBeforeOpenSquareBracket;
        SpaceBeforeSemicolonsInForStatement = spaceBeforeSemicolonsInForStatement;
        SpaceBetweenEmptyMethodCallParentheses = spaceBetweenEmptyMethodCallParentheses;
        SpaceBetweenEmptyMethodDeclarationParentheses = spaceBetweenEmptyMethodDeclarationParentheses;
        SpaceBetweenEmptySquareBrackets = spaceBetweenEmptySquareBrackets;
        SpacesIgnoreAroundVariableDeclaration = spacesIgnoreAroundVariableDeclaration;
        SpaceWithinCastParentheses = spaceWithinCastParentheses;
        SpaceWithinExpressionParentheses = spaceWithinExpressionParentheses;
        SpaceWithinMethodCallParentheses = spaceWithinMethodCallParentheses;
        SpaceWithinMethodDeclarationParenthesis = spaceWithinMethodDeclarationParenthesis;
        SpaceWithinOtherParentheses = spaceWithinOtherParentheses;
        SpaceWithinSquareBrackets = spaceWithinSquareBrackets;
        SpacingAfterMethodDeclarationName = spacingAfterMethodDeclarationName;
        SpacingAroundBinaryOperator = spacingAroundBinaryOperator;
        WrappingPreserveSingleLine = wrappingPreserveSingleLine;
        WrappingKeepStatementsOnSingleLine = wrappingKeepStatementsOnSingleLine;
    }

    /// <summary>
    /// Default style matching Roslyn/Visual Studio defaults (Allman style).
    /// </summary>
    public static CSharpFormatStyle Default { get; } = new(
        Guid.NewGuid(),
        useTabs: false, indentSize: 4, tabSize: 4, newLine: "\n",
        // Indentation
        indentBlock: true, indentBraces: false, indentSwitchCaseSection: true,
        indentSwitchCaseSectionWhenBlock: true, indentSwitchSection: true,
        labelPositioning: 1, // OneLess
        // Brace placement
        newLinesForBracesInTypes: true, newLinesForBracesInMethods: true,
        newLinesForBracesInProperties: true, newLinesForBracesInAccessors: true,
        newLinesForBracesInAnonymousMethods: true, newLinesForBracesInAnonymousTypes: true,
        newLinesForBracesInControlBlocks: true, newLinesForBracesInLambdaExpressionBody: true,
        newLinesForBracesInObjectCollectionArrayInitializers: true, newLinesForBracesInLocalFunctions: true,
        // New line before keywords
        newLineBeforeElse: true, newLineBeforeCatch: true, newLineBeforeFinally: true,
        newLineForClausesInQuery: true, newLineForMembersInAnonymousTypes: true,
        newLineForMembersInObjectInit: true,
        // Spacing
        spaceAfterCast: false, spaceAfterColonInBaseTypeDeclaration: true, spaceAfterComma: true,
        spaceAfterControlFlowStatementKeyword: true, spaceAfterDot: false, spaceAfterMethodCallName: false,
        spaceAfterSemicolonsInForStatement: true, spaceBeforeColonInBaseTypeDeclaration: true,
        spaceBeforeComma: false, spaceBeforeDot: false, spaceBeforeOpenSquareBracket: false,
        spaceBeforeSemicolonsInForStatement: false, spaceBetweenEmptyMethodCallParentheses: false,
        spaceBetweenEmptyMethodDeclarationParentheses: false, spaceBetweenEmptySquareBrackets: false,
        spacesIgnoreAroundVariableDeclaration: false, spaceWithinCastParentheses: false,
        spaceWithinExpressionParentheses: false, spaceWithinMethodCallParentheses: false,
        spaceWithinMethodDeclarationParenthesis: false, spaceWithinOtherParentheses: false,
        spaceWithinSquareBrackets: false, spacingAfterMethodDeclarationName: false,
        spacingAroundBinaryOperator: 0, // Single (before_and_after)
        // Wrapping
        wrappingPreserveSingleLine: true, wrappingKeepStatementsOnSingleLine: true
    );

    public CSharpFormatStyle WithId(Guid id) => id == Id ? this : new(id,
        UseTabs, IndentSize, TabSize, NewLine,
        IndentBlock, IndentBraces, IndentSwitchCaseSection, IndentSwitchCaseSectionWhenBlock,
        IndentSwitchSection, LabelPositioning,
        NewLinesForBracesInTypes, NewLinesForBracesInMethods, NewLinesForBracesInProperties,
        NewLinesForBracesInAccessors, NewLinesForBracesInAnonymousMethods, NewLinesForBracesInAnonymousTypes,
        NewLinesForBracesInControlBlocks, NewLinesForBracesInLambdaExpressionBody,
        NewLinesForBracesInObjectCollectionArrayInitializers, NewLinesForBracesInLocalFunctions,
        NewLineBeforeElse, NewLineBeforeCatch, NewLineBeforeFinally,
        NewLineForClausesInQuery, NewLineForMembersInAnonymousTypes, NewLineForMembersInObjectInit,
        SpaceAfterCast, SpaceAfterColonInBaseTypeDeclaration, SpaceAfterComma,
        SpaceAfterControlFlowStatementKeyword, SpaceAfterDot, SpaceAfterMethodCallName,
        SpaceAfterSemicolonsInForStatement, SpaceBeforeColonInBaseTypeDeclaration,
        SpaceBeforeComma, SpaceBeforeDot, SpaceBeforeOpenSquareBracket,
        SpaceBeforeSemicolonsInForStatement, SpaceBetweenEmptyMethodCallParentheses,
        SpaceBetweenEmptyMethodDeclarationParentheses, SpaceBetweenEmptySquareBrackets,
        SpacesIgnoreAroundVariableDeclaration, SpaceWithinCastParentheses,
        SpaceWithinExpressionParentheses, SpaceWithinMethodCallParentheses,
        SpaceWithinMethodDeclarationParenthesis, SpaceWithinOtherParentheses,
        SpaceWithinSquareBrackets, SpacingAfterMethodDeclarationName, SpacingAroundBinaryOperator,
        WrappingPreserveSingleLine, WrappingKeepStatementsOnSingleLine);

    // RPC serialization — fields sent in declaration order
    public void RpcSend(CSharpFormatStyle after, RpcSendQueue q)
    {
        q.GetAndSend(after, m => m.Id);
        q.GetAndSend(after, m => m.UseTabs); q.GetAndSend(after, m => m.IndentSize);
        q.GetAndSend(after, m => m.TabSize); q.GetAndSend(after, m => m.NewLine);
        q.GetAndSend(after, m => m.IndentBlock); q.GetAndSend(after, m => m.IndentBraces);
        q.GetAndSend(after, m => m.IndentSwitchCaseSection);
        q.GetAndSend(after, m => m.IndentSwitchCaseSectionWhenBlock);
        q.GetAndSend(after, m => m.IndentSwitchSection); q.GetAndSend(after, m => m.LabelPositioning);
        q.GetAndSend(after, m => m.NewLinesForBracesInTypes);
        q.GetAndSend(after, m => m.NewLinesForBracesInMethods);
        q.GetAndSend(after, m => m.NewLinesForBracesInProperties);
        q.GetAndSend(after, m => m.NewLinesForBracesInAccessors);
        q.GetAndSend(after, m => m.NewLinesForBracesInAnonymousMethods);
        q.GetAndSend(after, m => m.NewLinesForBracesInAnonymousTypes);
        q.GetAndSend(after, m => m.NewLinesForBracesInControlBlocks);
        q.GetAndSend(after, m => m.NewLinesForBracesInLambdaExpressionBody);
        q.GetAndSend(after, m => m.NewLinesForBracesInObjectCollectionArrayInitializers);
        q.GetAndSend(after, m => m.NewLinesForBracesInLocalFunctions);
        q.GetAndSend(after, m => m.NewLineBeforeElse); q.GetAndSend(after, m => m.NewLineBeforeCatch);
        q.GetAndSend(after, m => m.NewLineBeforeFinally);
        q.GetAndSend(after, m => m.NewLineForClausesInQuery);
        q.GetAndSend(after, m => m.NewLineForMembersInAnonymousTypes);
        q.GetAndSend(after, m => m.NewLineForMembersInObjectInit);
        q.GetAndSend(after, m => m.SpaceAfterCast);
        q.GetAndSend(after, m => m.SpaceAfterColonInBaseTypeDeclaration);
        q.GetAndSend(after, m => m.SpaceAfterComma);
        q.GetAndSend(after, m => m.SpaceAfterControlFlowStatementKeyword);
        q.GetAndSend(after, m => m.SpaceAfterDot); q.GetAndSend(after, m => m.SpaceAfterMethodCallName);
        q.GetAndSend(after, m => m.SpaceAfterSemicolonsInForStatement);
        q.GetAndSend(after, m => m.SpaceBeforeColonInBaseTypeDeclaration);
        q.GetAndSend(after, m => m.SpaceBeforeComma); q.GetAndSend(after, m => m.SpaceBeforeDot);
        q.GetAndSend(after, m => m.SpaceBeforeOpenSquareBracket);
        q.GetAndSend(after, m => m.SpaceBeforeSemicolonsInForStatement);
        q.GetAndSend(after, m => m.SpaceBetweenEmptyMethodCallParentheses);
        q.GetAndSend(after, m => m.SpaceBetweenEmptyMethodDeclarationParentheses);
        q.GetAndSend(after, m => m.SpaceBetweenEmptySquareBrackets);
        q.GetAndSend(after, m => m.SpacesIgnoreAroundVariableDeclaration);
        q.GetAndSend(after, m => m.SpaceWithinCastParentheses);
        q.GetAndSend(after, m => m.SpaceWithinExpressionParentheses);
        q.GetAndSend(after, m => m.SpaceWithinMethodCallParentheses);
        q.GetAndSend(after, m => m.SpaceWithinMethodDeclarationParenthesis);
        q.GetAndSend(after, m => m.SpaceWithinOtherParentheses);
        q.GetAndSend(after, m => m.SpaceWithinSquareBrackets);
        q.GetAndSend(after, m => m.SpacingAfterMethodDeclarationName);
        q.GetAndSend(after, m => m.SpacingAroundBinaryOperator);
        q.GetAndSend(after, m => m.WrappingPreserveSingleLine);
        q.GetAndSend(after, m => m.WrappingKeepStatementsOnSingleLine);
    }

    public CSharpFormatStyle RpcReceive(CSharpFormatStyle before, RpcReceiveQueue q)
    {
        return new CSharpFormatStyle(
            q.ReceiveAndGet<Guid, string>(before.Id, Guid.Parse),
            q.Receive<bool>(before.UseTabs), q.Receive<int>(before.IndentSize),
            q.Receive<int>(before.TabSize), q.ReceiveAndGet<string, string>(before.NewLine, x => x)!,
            q.Receive<bool>(before.IndentBlock), q.Receive<bool>(before.IndentBraces),
            q.Receive<bool>(before.IndentSwitchCaseSection),
            q.Receive<bool>(before.IndentSwitchCaseSectionWhenBlock),
            q.Receive<bool>(before.IndentSwitchSection), q.Receive<int>(before.LabelPositioning),
            q.Receive<bool>(before.NewLinesForBracesInTypes),
            q.Receive<bool>(before.NewLinesForBracesInMethods),
            q.Receive<bool>(before.NewLinesForBracesInProperties),
            q.Receive<bool>(before.NewLinesForBracesInAccessors),
            q.Receive<bool>(before.NewLinesForBracesInAnonymousMethods),
            q.Receive<bool>(before.NewLinesForBracesInAnonymousTypes),
            q.Receive<bool>(before.NewLinesForBracesInControlBlocks),
            q.Receive<bool>(before.NewLinesForBracesInLambdaExpressionBody),
            q.Receive<bool>(before.NewLinesForBracesInObjectCollectionArrayInitializers),
            q.Receive<bool>(before.NewLinesForBracesInLocalFunctions),
            q.Receive<bool>(before.NewLineBeforeElse), q.Receive<bool>(before.NewLineBeforeCatch),
            q.Receive<bool>(before.NewLineBeforeFinally),
            q.Receive<bool>(before.NewLineForClausesInQuery),
            q.Receive<bool>(before.NewLineForMembersInAnonymousTypes),
            q.Receive<bool>(before.NewLineForMembersInObjectInit),
            q.Receive<bool>(before.SpaceAfterCast),
            q.Receive<bool>(before.SpaceAfterColonInBaseTypeDeclaration),
            q.Receive<bool>(before.SpaceAfterComma),
            q.Receive<bool>(before.SpaceAfterControlFlowStatementKeyword),
            q.Receive<bool>(before.SpaceAfterDot), q.Receive<bool>(before.SpaceAfterMethodCallName),
            q.Receive<bool>(before.SpaceAfterSemicolonsInForStatement),
            q.Receive<bool>(before.SpaceBeforeColonInBaseTypeDeclaration),
            q.Receive<bool>(before.SpaceBeforeComma), q.Receive<bool>(before.SpaceBeforeDot),
            q.Receive<bool>(before.SpaceBeforeOpenSquareBracket),
            q.Receive<bool>(before.SpaceBeforeSemicolonsInForStatement),
            q.Receive<bool>(before.SpaceBetweenEmptyMethodCallParentheses),
            q.Receive<bool>(before.SpaceBetweenEmptyMethodDeclarationParentheses),
            q.Receive<bool>(before.SpaceBetweenEmptySquareBrackets),
            q.Receive<bool>(before.SpacesIgnoreAroundVariableDeclaration),
            q.Receive<bool>(before.SpaceWithinCastParentheses),
            q.Receive<bool>(before.SpaceWithinExpressionParentheses),
            q.Receive<bool>(before.SpaceWithinMethodCallParentheses),
            q.Receive<bool>(before.SpaceWithinMethodDeclarationParenthesis),
            q.Receive<bool>(before.SpaceWithinOtherParentheses),
            q.Receive<bool>(before.SpaceWithinSquareBrackets),
            q.Receive<bool>(before.SpacingAfterMethodDeclarationName),
            q.Receive<int>(before.SpacingAroundBinaryOperator),
            q.Receive<bool>(before.WrappingPreserveSingleLine),
            q.Receive<bool>(before.WrappingKeepStatementsOnSingleLine)
        );
    }

    public bool Equals(CSharpFormatStyle? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as CSharpFormatStyle);
    public override int GetHashCode() => Id.GetHashCode();
}
