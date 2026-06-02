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
/// A marker that captures information about a parse failure.
/// Mirrors org.openrewrite.ParseExceptionResult.
/// </summary>
public sealed class ParseExceptionResult(
    Guid id,
    string parserType,
    string exceptionType,
    string message,
    string? treeType
) : Marker, IRpcCodec<ParseExceptionResult>
{
    public Guid Id { get; } = id;
    public string ParserType { get; } = parserType;
    public string ExceptionType { get; } = exceptionType;
    public string Message { get; } = message;
    public string? TreeType { get; } = treeType;

    public static ParseExceptionResult Build(string parserType, Exception ex)
    {
        return new ParseExceptionResult(
            Guid.NewGuid(),
            parserType,
            ex.GetType().Name,
            ex.ToString(),
            null
        );
    }

    public void RpcSend(ParseExceptionResult after, RpcSendQueue q)
    {
        q.GetAndSend(after, r => r.Id);
        q.GetAndSend(after, r => r.ParserType);
        q.GetAndSend(after, r => r.ExceptionType);
        q.GetAndSend(after, r => r.Message);
        q.GetAndSend(after, r => r.TreeType);
    }

    public ParseExceptionResult RpcReceive(ParseExceptionResult before, RpcReceiveQueue q)
    {
        var id = q.ReceiveAndGet<Guid, string>(before.Id, Guid.Parse);
        var parserType = q.ReceiveAndGet<string, string>(before.ParserType, s => s);
        var exceptionType = q.ReceiveAndGet<string, string>(before.ExceptionType, s => s);
        var message = q.ReceiveAndGet<string, string>(before.Message, s => s);
        var treeType = q.ReceiveAndGet<string?, string?>(before.TreeType, s => s);
        return new ParseExceptionResult(id, parserType!, exceptionType!, message!, treeType);
    }
}
