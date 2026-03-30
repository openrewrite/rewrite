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
/// Represents file attributes. Mirrors org.openrewrite.FileAttributes.
/// Time fields are stored as opaque objects to round-trip Java's ZonedDateTime.
/// </summary>
public sealed class FileAttributes(
    object? creationTime,
    object? lastModifiedTime,
    object? lastAccessTime,
    bool isReadable,
    bool isWritable,
    bool isExecutable,
    long size
) : IRpcCodec<FileAttributes>
{
    public object? CreationTime { get; } = creationTime;
    public object? LastModifiedTime { get; } = lastModifiedTime;
    public object? LastAccessTime { get; } = lastAccessTime;
    public bool IsReadable { get; } = isReadable;
    public bool IsWritable { get; } = isWritable;
    public bool IsExecutable { get; } = isExecutable;
    public long Size { get; } = size;

    public void RpcSend(FileAttributes after, RpcSendQueue q)
    {
        q.GetAndSend(after, f => f.CreationTime);
        q.GetAndSend(after, f => f.LastModifiedTime);
        q.GetAndSend(after, f => f.LastAccessTime);
        q.GetAndSend(after, f => (object)f.IsReadable);
        q.GetAndSend(after, f => (object)f.IsWritable);
        q.GetAndSend(after, f => (object)f.IsExecutable);
        q.GetAndSend(after, f => (object)f.Size);
    }

    public FileAttributes RpcReceive(FileAttributes before, RpcReceiveQueue q)
    {
        var creationTime = q.Receive<object?>(before.CreationTime);
        var lastModifiedTime = q.Receive<object?>(before.LastModifiedTime);
        var lastAccessTime = q.Receive<object?>(before.LastAccessTime);
        var isReadable = q.Receive(before.IsReadable);
        var isWritable = q.Receive(before.IsWritable);
        var isExecutable = q.Receive(before.IsExecutable);
        var size = q.Receive(before.Size);
        return new FileAttributes(creationTime, lastModifiedTime, lastAccessTime,
            isReadable, isWritable, isExecutable, size);
    }
}
