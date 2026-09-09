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
namespace OpenRewrite.Core;

/// <summary>
/// The base interface for all LST (Lossless Semantic Tree) elements.
/// </summary>
public interface Tree
{
    /// <summary>
    /// An id names a node and carries no secret, so it is drawn in userspace from a per-thread
    /// generator: a tree draws one per node, and the OS entropy source behind
    /// <see cref="Guid.NewGuid"/> costs a syscall per draw. Mirrors Java's <c>Tree.randomId()</c>.
    /// </summary>
    static Guid RandomId()
    {
        Span<byte> b = stackalloc byte[16];
        System.Random.Shared.NextBytes(b);
        b[6] = (byte)((b[6] & 0x0F) | 0x40); // version 4
        b[8] = (byte)((b[8] & 0x3F) | 0x80); // variant IETF
        return new Guid(b, bigEndian: true);
    }

    Guid Id { get; }

    Markers Markers { get; }

    Tree WithId(Guid id);
}

/// <summary>
/// Represents a source file in the LST.
/// </summary>
public interface SourceFile : Tree
{
    string SourcePath { get; }

    SourceFile WithSourcePath(string sourcePath);
}
