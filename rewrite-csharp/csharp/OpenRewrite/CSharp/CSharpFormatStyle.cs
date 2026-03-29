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
/// <para>
/// Boolean flags are packed into a single <c>long</c> for compact RPC serialization.
/// Bit positions are assigned in declaration order — new flags must be appended at the end.
/// </para>
/// </summary>
public sealed class CSharpFormatStyle : Marker, IRpcCodec<CSharpFormatStyle>, IEquatable<CSharpFormatStyle>
{
    /// <summary>Roslyn default flags: 27 of 47 bits set (Allman style, standard spacing).</summary>
    internal const long DefaultFlags = 0x0000600033BFFFFAL;

    // ── Bit positions for boolean flags (append-only — do not reorder) ──
    private const int BitUseTabs = 0;
    private const int BitIndentBlock = 1;
    private const int BitIndentBraces = 2;
    private const int BitIndentSwitchCaseSection = 3;
    private const int BitIndentSwitchCaseSectionWhenBlock = 4;
    private const int BitIndentSwitchSection = 5;
    private const int BitNewLinesForBracesInTypes = 6;
    private const int BitNewLinesForBracesInMethods = 7;
    private const int BitNewLinesForBracesInProperties = 8;
    private const int BitNewLinesForBracesInAccessors = 9;
    private const int BitNewLinesForBracesInAnonymousMethods = 10;
    private const int BitNewLinesForBracesInAnonymousTypes = 11;
    private const int BitNewLinesForBracesInControlBlocks = 12;
    private const int BitNewLinesForBracesInLambdaExpressionBody = 13;
    private const int BitNewLinesForBracesInObjectCollectionArrayInitializers = 14;
    private const int BitNewLinesForBracesInLocalFunctions = 15;
    private const int BitNewLineBeforeElse = 16;
    private const int BitNewLineBeforeCatch = 17;
    private const int BitNewLineBeforeFinally = 18;
    private const int BitNewLineForClausesInQuery = 19;
    private const int BitNewLineForMembersInAnonymousTypes = 20;
    private const int BitNewLineForMembersInObjectInit = 21;
    private const int BitSpaceAfterCast = 22;
    private const int BitSpaceAfterColonInBaseTypeDeclaration = 23;
    private const int BitSpaceAfterComma = 24;
    private const int BitSpaceAfterControlFlowStatementKeyword = 25;
    private const int BitSpaceAfterDot = 26;
    private const int BitSpaceAfterMethodCallName = 27;
    private const int BitSpaceAfterSemicolonsInForStatement = 28;
    private const int BitSpaceBeforeColonInBaseTypeDeclaration = 29;
    private const int BitSpaceBeforeComma = 30;
    private const int BitSpaceBeforeDot = 31;
    private const int BitSpaceBeforeOpenSquareBracket = 32;
    private const int BitSpaceBeforeSemicolonsInForStatement = 33;
    private const int BitSpaceBetweenEmptyMethodCallParentheses = 34;
    private const int BitSpaceBetweenEmptyMethodDeclarationParentheses = 35;
    private const int BitSpaceBetweenEmptySquareBrackets = 36;
    private const int BitSpacesIgnoreAroundVariableDeclaration = 37;
    private const int BitSpaceWithinCastParentheses = 38;
    private const int BitSpaceWithinExpressionParentheses = 39;
    private const int BitSpaceWithinMethodCallParentheses = 40;
    private const int BitSpaceWithinMethodDeclarationParenthesis = 41;
    private const int BitSpaceWithinOtherParentheses = 42;
    private const int BitSpaceWithinSquareBrackets = 43;
    private const int BitSpacingAfterMethodDeclarationName = 44;
    private const int BitWrappingPreserveSingleLine = 45;
    private const int BitWrappingKeepStatementsOnSingleLine = 46;
    // Next flag = 47. Maximum = 63.

    private readonly long _flags;

    public Guid Id { get; }
    public int IndentSize { get; }
    public int TabSize { get; }
    public string NewLine { get; }
    /// <summary>Label positioning: 0=LeftMost (flush_left), 1=OneLess (one_less_than_current), 2=NoIndent (no_change).</summary>
    public int LabelPositioning { get; }
    /// <summary>Binary operator spacing: 0=Single (before_and_after), 1=Ignore, 2=Remove (none).</summary>
    public int SpacingAroundBinaryOperator { get; }

    // ── Boolean flag accessors ──
    private bool Flag(int bit) => (_flags & (1L << bit)) != 0;

    // General
    public bool UseTabs => Flag(BitUseTabs);
    // Indentation
    public bool IndentBlock => Flag(BitIndentBlock);
    public bool IndentBraces => Flag(BitIndentBraces);
    public bool IndentSwitchCaseSection => Flag(BitIndentSwitchCaseSection);
    public bool IndentSwitchCaseSectionWhenBlock => Flag(BitIndentSwitchCaseSectionWhenBlock);
    public bool IndentSwitchSection => Flag(BitIndentSwitchSection);
    // Brace placement
    public bool NewLinesForBracesInTypes => Flag(BitNewLinesForBracesInTypes);
    public bool NewLinesForBracesInMethods => Flag(BitNewLinesForBracesInMethods);
    public bool NewLinesForBracesInProperties => Flag(BitNewLinesForBracesInProperties);
    public bool NewLinesForBracesInAccessors => Flag(BitNewLinesForBracesInAccessors);
    public bool NewLinesForBracesInAnonymousMethods => Flag(BitNewLinesForBracesInAnonymousMethods);
    public bool NewLinesForBracesInAnonymousTypes => Flag(BitNewLinesForBracesInAnonymousTypes);
    public bool NewLinesForBracesInControlBlocks => Flag(BitNewLinesForBracesInControlBlocks);
    public bool NewLinesForBracesInLambdaExpressionBody => Flag(BitNewLinesForBracesInLambdaExpressionBody);
    public bool NewLinesForBracesInObjectCollectionArrayInitializers => Flag(BitNewLinesForBracesInObjectCollectionArrayInitializers);
    public bool NewLinesForBracesInLocalFunctions => Flag(BitNewLinesForBracesInLocalFunctions);
    // New line before keywords / members
    public bool NewLineBeforeElse => Flag(BitNewLineBeforeElse);
    public bool NewLineBeforeCatch => Flag(BitNewLineBeforeCatch);
    public bool NewLineBeforeFinally => Flag(BitNewLineBeforeFinally);
    public bool NewLineForClausesInQuery => Flag(BitNewLineForClausesInQuery);
    public bool NewLineForMembersInAnonymousTypes => Flag(BitNewLineForMembersInAnonymousTypes);
    public bool NewLineForMembersInObjectInit => Flag(BitNewLineForMembersInObjectInit);
    // Spacing
    public bool SpaceAfterCast => Flag(BitSpaceAfterCast);
    public bool SpaceAfterColonInBaseTypeDeclaration => Flag(BitSpaceAfterColonInBaseTypeDeclaration);
    public bool SpaceAfterComma => Flag(BitSpaceAfterComma);
    public bool SpaceAfterControlFlowStatementKeyword => Flag(BitSpaceAfterControlFlowStatementKeyword);
    public bool SpaceAfterDot => Flag(BitSpaceAfterDot);
    public bool SpaceAfterMethodCallName => Flag(BitSpaceAfterMethodCallName);
    public bool SpaceAfterSemicolonsInForStatement => Flag(BitSpaceAfterSemicolonsInForStatement);
    public bool SpaceBeforeColonInBaseTypeDeclaration => Flag(BitSpaceBeforeColonInBaseTypeDeclaration);
    public bool SpaceBeforeComma => Flag(BitSpaceBeforeComma);
    public bool SpaceBeforeDot => Flag(BitSpaceBeforeDot);
    public bool SpaceBeforeOpenSquareBracket => Flag(BitSpaceBeforeOpenSquareBracket);
    public bool SpaceBeforeSemicolonsInForStatement => Flag(BitSpaceBeforeSemicolonsInForStatement);
    public bool SpaceBetweenEmptyMethodCallParentheses => Flag(BitSpaceBetweenEmptyMethodCallParentheses);
    public bool SpaceBetweenEmptyMethodDeclarationParentheses => Flag(BitSpaceBetweenEmptyMethodDeclarationParentheses);
    public bool SpaceBetweenEmptySquareBrackets => Flag(BitSpaceBetweenEmptySquareBrackets);
    public bool SpacesIgnoreAroundVariableDeclaration => Flag(BitSpacesIgnoreAroundVariableDeclaration);
    public bool SpaceWithinCastParentheses => Flag(BitSpaceWithinCastParentheses);
    public bool SpaceWithinExpressionParentheses => Flag(BitSpaceWithinExpressionParentheses);
    public bool SpaceWithinMethodCallParentheses => Flag(BitSpaceWithinMethodCallParentheses);
    public bool SpaceWithinMethodDeclarationParenthesis => Flag(BitSpaceWithinMethodDeclarationParenthesis);
    public bool SpaceWithinOtherParentheses => Flag(BitSpaceWithinOtherParentheses);
    public bool SpaceWithinSquareBrackets => Flag(BitSpaceWithinSquareBrackets);
    public bool SpacingAfterMethodDeclarationName => Flag(BitSpacingAfterMethodDeclarationName);
    // Wrapping — stored for .editorconfig fidelity but intentionally overridden to false by
    // RoslynFormatter.BuildOptions because synthesized template nodes may lack structural newlines.
    public bool WrappingPreserveSingleLine => Flag(BitWrappingPreserveSingleLine);
    public bool WrappingKeepStatementsOnSingleLine => Flag(BitWrappingKeepStatementsOnSingleLine);

    /// <summary>
    /// Primary constructor. Boolean flags are packed into a long via <see cref="PackFlags"/>.
    /// </summary>
    public CSharpFormatStyle(
        Guid id, long flags, int indentSize, int tabSize, string newLine,
        int labelPositioning, int spacingAroundBinaryOperator)
    {
        Id = id; _flags = flags;
        IndentSize = indentSize; TabSize = tabSize; NewLine = newLine;
        LabelPositioning = labelPositioning; SpacingAroundBinaryOperator = spacingAroundBinaryOperator;
    }

    /// <summary>
    /// Convenience constructor accepting individual boolean parameters.
    /// </summary>
    public CSharpFormatStyle(
        Guid id, bool useTabs, int indentSize, int tabSize, string newLine,
        bool indentBlock, bool indentBraces, bool indentSwitchCaseSection,
        bool indentSwitchCaseSectionWhenBlock, bool indentSwitchSection, int labelPositioning,
        bool newLinesForBracesInTypes, bool newLinesForBracesInMethods,
        bool newLinesForBracesInProperties, bool newLinesForBracesInAccessors,
        bool newLinesForBracesInAnonymousMethods, bool newLinesForBracesInAnonymousTypes,
        bool newLinesForBracesInControlBlocks, bool newLinesForBracesInLambdaExpressionBody,
        bool newLinesForBracesInObjectCollectionArrayInitializers, bool newLinesForBracesInLocalFunctions,
        bool newLineBeforeElse, bool newLineBeforeCatch, bool newLineBeforeFinally,
        bool newLineForClausesInQuery, bool newLineForMembersInAnonymousTypes,
        bool newLineForMembersInObjectInit,
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
        bool wrappingPreserveSingleLine, bool wrappingKeepStatementsOnSingleLine)
        : this(id, PackFlags(
            useTabs, indentBlock, indentBraces, indentSwitchCaseSection,
            indentSwitchCaseSectionWhenBlock, indentSwitchSection,
            newLinesForBracesInTypes, newLinesForBracesInMethods,
            newLinesForBracesInProperties, newLinesForBracesInAccessors,
            newLinesForBracesInAnonymousMethods, newLinesForBracesInAnonymousTypes,
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
            spaceWithinSquareBrackets, spacingAfterMethodDeclarationName,
            wrappingPreserveSingleLine, wrappingKeepStatementsOnSingleLine),
            indentSize, tabSize, newLine, labelPositioning, spacingAroundBinaryOperator)
    { }

    private static long PackFlags(params bool[] bits)
    {
        long flags = 0;
        for (var i = 0; i < bits.Length; i++)
            if (bits[i]) flags |= 1L << i;
        return flags;
    }

    /// <summary>Default style matching Roslyn/Visual Studio defaults (Allman style).</summary>
    public static CSharpFormatStyle Default { get; } = new(
        Guid.NewGuid(), DefaultFlags, indentSize: 4, tabSize: 4, newLine: "\n",
        labelPositioning: 1, spacingAroundBinaryOperator: 0);

    public CSharpFormatStyle WithId(Guid id) =>
        id == Id ? this : new(id, _flags, IndentSize, TabSize, NewLine, LabelPositioning, SpacingAroundBinaryOperator);

    // ── RPC serialization: 6 fields instead of 52 ──
    public void RpcSend(CSharpFormatStyle after, RpcSendQueue q)
    {
        q.GetAndSend(after, m => m.Id);
        q.GetAndSend(after, m => m._flags);
        q.GetAndSend(after, m => m.IndentSize);
        q.GetAndSend(after, m => m.TabSize);
        q.GetAndSend(after, m => m.NewLine);
        q.GetAndSend(after, m => m.LabelPositioning);
        q.GetAndSend(after, m => m.SpacingAroundBinaryOperator);
    }

    public CSharpFormatStyle RpcReceive(CSharpFormatStyle before, RpcReceiveQueue q)
    {
        return new CSharpFormatStyle(
            q.ReceiveAndGet<Guid, string>(before.Id, Guid.Parse),
            q.Receive<long>(before._flags),
            q.Receive<int>(before.IndentSize),
            q.Receive<int>(before.TabSize),
            q.ReceiveAndGet<string, string>(before.NewLine, x => x)!,
            q.Receive<int>(before.LabelPositioning),
            q.Receive<int>(before.SpacingAroundBinaryOperator)
        );
    }

    public bool Equals(CSharpFormatStyle? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as CSharpFormatStyle);
    public override int GetHashCode() => Id.GetHashCode();
}
