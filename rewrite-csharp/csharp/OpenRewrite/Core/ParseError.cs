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
using OpenRewrite.Core.Rpc;

namespace OpenRewrite.Core;

/// <summary>
/// Represents a source file that failed to parse or failed print-idempotency validation.
/// Mirrors org.openrewrite.tree.ParseError.
/// </summary>
public sealed class ParseError(
    Guid id,
    Markers markers,
    string sourcePath,
    string charsetName,
    bool charsetBomMarked,
    Checksum? checksum,
    FileAttributes? fileAttributes,
    string text
) : SourceFile, IRpcCodec<ParseError>
{
    public Guid Id { get; } = id;
    public Markers Markers { get; } = markers;
    public string SourcePath { get; } = sourcePath;
    public string CharsetName { get; } = charsetName;
    public bool CharsetBomMarked { get; } = charsetBomMarked;
    public Checksum? Checksum { get; } = checksum;
    public FileAttributes? FileAttributes { get; } = fileAttributes;
    public string Text { get; } = text;

    public Tree WithId(Guid id) =>
        id == Id ? this : new ParseError(id, Markers, SourcePath, CharsetName, CharsetBomMarked, Checksum, FileAttributes, Text);

    public SourceFile WithSourcePath(string sourcePath) =>
        sourcePath == SourcePath ? this : new ParseError(Id, Markers, sourcePath, CharsetName, CharsetBomMarked, Checksum, FileAttributes, Text);

    /// <summary>
    /// Creates a ParseError from a source file path, source text, and exception.
    /// </summary>
    public static ParseError Build(string sourcePath, string source, Exception ex)
    {
        var marker = ParseExceptionResult.Build("CSharpParser", ex);
        var markers = new Markers(Guid.NewGuid(), new List<Marker> { marker });
        return new ParseError(
            Guid.NewGuid(),
            markers,
            sourcePath,
            "UTF-8",
            false,
            null,
            null,
            source
        );
    }

    public void RpcSend(ParseError after, RpcSendQueue q)
    {
        q.GetAndSend(after, e => e.Id);
        q.GetAndSend(after, e => (object)e.Markers);
        q.GetAndSend(after, e => e.SourcePath);
        q.GetAndSend(after, e => e.CharsetName);
        q.GetAndSend(after, e => (object)e.CharsetBomMarked);
        q.GetAndSend(after, e => (object?)e.Checksum);
        q.GetAndSend(after, e => (object?)e.FileAttributes);
        q.GetAndSend(after, e => e.Text);
    }

    public ParseError RpcReceive(ParseError before, RpcReceiveQueue q)
    {
        var id = q.ReceiveAndGet<Guid, string>(before.Id, Guid.Parse);
        var markers = q.Receive(before.Markers);
        var sourcePath = q.Receive(before.SourcePath);
        var charsetName = q.Receive(before.CharsetName);
        var charsetBomMarked = q.Receive(before.CharsetBomMarked);
        var checksum = q.Receive(before.Checksum);
        var fileAttributes = q.Receive(before.FileAttributes);
        var text = q.Receive(before.Text);
        return new ParseError(id, markers!, sourcePath!, charsetName!, charsetBomMarked, checksum, fileAttributes, text!);
    }
}
