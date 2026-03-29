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
/// Used by <see cref="Format.RoslynFormatter"/> to configure the Roslyn formatting workspace.
/// </summary>
public sealed class CSharpFormatStyle(
    Guid id,
    bool useTabs,
    int indentSize,
    int tabSize,
    string newLine,
    bool newLinesForBracesInTypes,
    bool newLinesForBracesInMethods,
    bool newLinesForBracesInProperties,
    bool newLinesForBracesInAccessors,
    bool newLinesForBracesInAnonymousMethods,
    bool newLinesForBracesInAnonymousTypes,
    bool newLinesForBracesInControlBlocks,
    bool newLinesForBracesInLambdaExpressionBody,
    bool newLinesForBracesInObjectCollectionArrayInitializers,
    bool newLinesForBracesInLocalFunctions,
    bool newLineBeforeElse,
    bool newLineBeforeCatch,
    bool newLineBeforeFinally,
    bool wrappingPreserveSingleLine,
    bool wrappingKeepStatementsOnSingleLine
) : Marker, IRpcCodec<CSharpFormatStyle>, IEquatable<CSharpFormatStyle>
{
    public Guid Id { get; } = id;
    public bool UseTabs { get; } = useTabs;
    public int IndentSize { get; } = indentSize;
    public int TabSize { get; } = tabSize;
    public string NewLine { get; } = newLine;

    // Brace placement
    public bool NewLinesForBracesInTypes { get; } = newLinesForBracesInTypes;
    public bool NewLinesForBracesInMethods { get; } = newLinesForBracesInMethods;
    public bool NewLinesForBracesInProperties { get; } = newLinesForBracesInProperties;
    public bool NewLinesForBracesInAccessors { get; } = newLinesForBracesInAccessors;
    public bool NewLinesForBracesInAnonymousMethods { get; } = newLinesForBracesInAnonymousMethods;
    public bool NewLinesForBracesInAnonymousTypes { get; } = newLinesForBracesInAnonymousTypes;
    public bool NewLinesForBracesInControlBlocks { get; } = newLinesForBracesInControlBlocks;
    public bool NewLinesForBracesInLambdaExpressionBody { get; } = newLinesForBracesInLambdaExpressionBody;
    public bool NewLinesForBracesInObjectCollectionArrayInitializers { get; } = newLinesForBracesInObjectCollectionArrayInitializers;
    public bool NewLinesForBracesInLocalFunctions { get; } = newLinesForBracesInLocalFunctions;

    // New line before keywords
    public bool NewLineBeforeElse { get; } = newLineBeforeElse;
    public bool NewLineBeforeCatch { get; } = newLineBeforeCatch;
    public bool NewLineBeforeFinally { get; } = newLineBeforeFinally;

    // Wrapping — stored for .editorconfig fidelity but intentionally overridden to false by
    // RoslynFormatter.BuildOptions because synthesized template nodes may lack structural newlines
    // and Roslyn must insert them.
    public bool WrappingPreserveSingleLine { get; } = wrappingPreserveSingleLine;
    public bool WrappingKeepStatementsOnSingleLine { get; } = wrappingKeepStatementsOnSingleLine;

    /// <summary>
    /// Default style matching Roslyn/Visual Studio defaults (Allman style).
    /// </summary>
    public static CSharpFormatStyle Default { get; } = new(
        Guid.NewGuid(),
        useTabs: false,
        indentSize: 4,
        tabSize: 4,
        newLine: "\n",
        newLinesForBracesInTypes: true,
        newLinesForBracesInMethods: true,
        newLinesForBracesInProperties: true,
        newLinesForBracesInAccessors: true,
        newLinesForBracesInAnonymousMethods: true,
        newLinesForBracesInAnonymousTypes: true,
        newLinesForBracesInControlBlocks: true,
        newLinesForBracesInLambdaExpressionBody: true,
        newLinesForBracesInObjectCollectionArrayInitializers: true,
        newLinesForBracesInLocalFunctions: true,
        newLineBeforeElse: true,
        newLineBeforeCatch: true,
        newLineBeforeFinally: true,
        wrappingPreserveSingleLine: true,
        wrappingKeepStatementsOnSingleLine: true
    );

    // Withers
    public CSharpFormatStyle WithId(Guid id) =>
        id == Id ? this : new(id, UseTabs, IndentSize, TabSize, NewLine,
            NewLinesForBracesInTypes, NewLinesForBracesInMethods, NewLinesForBracesInProperties,
            NewLinesForBracesInAccessors, NewLinesForBracesInAnonymousMethods, NewLinesForBracesInAnonymousTypes,
            NewLinesForBracesInControlBlocks, NewLinesForBracesInLambdaExpressionBody,
            NewLinesForBracesInObjectCollectionArrayInitializers, NewLinesForBracesInLocalFunctions,
            NewLineBeforeElse, NewLineBeforeCatch, NewLineBeforeFinally,
            WrappingPreserveSingleLine, WrappingKeepStatementsOnSingleLine);

    // RPC serialization
    public void RpcSend(CSharpFormatStyle after, RpcSendQueue q)
    {
        q.GetAndSend(after, m => m.Id);
        q.GetAndSend(after, m => m.UseTabs);
        q.GetAndSend(after, m => m.IndentSize);
        q.GetAndSend(after, m => m.TabSize);
        q.GetAndSend(after, m => m.NewLine);
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
        q.GetAndSend(after, m => m.NewLineBeforeElse);
        q.GetAndSend(after, m => m.NewLineBeforeCatch);
        q.GetAndSend(after, m => m.NewLineBeforeFinally);
        q.GetAndSend(after, m => m.WrappingPreserveSingleLine);
        q.GetAndSend(after, m => m.WrappingKeepStatementsOnSingleLine);
    }

    public CSharpFormatStyle RpcReceive(CSharpFormatStyle before, RpcReceiveQueue q)
    {
        return new CSharpFormatStyle(
            q.ReceiveAndGet<Guid, string>(before.Id, Guid.Parse),
            q.Receive<bool>(before.UseTabs),
            q.Receive<int>(before.IndentSize),
            q.Receive<int>(before.TabSize),
            q.ReceiveAndGet<string, string>(before.NewLine, x => x)!,
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
            q.Receive<bool>(before.NewLineBeforeElse),
            q.Receive<bool>(before.NewLineBeforeCatch),
            q.Receive<bool>(before.NewLineBeforeFinally),
            q.Receive<bool>(before.WrappingPreserveSingleLine),
            q.Receive<bool>(before.WrappingKeepStatementsOnSingleLine)
        );
    }

    public bool Equals(CSharpFormatStyle? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as CSharpFormatStyle);
    public override int GetHashCode() => Id.GetHashCode();
}
