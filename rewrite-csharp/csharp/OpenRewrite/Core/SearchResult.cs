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
using OpenRewrite.Java;

namespace OpenRewrite.Core;

/// <summary>
/// Marks an LST node as a search result with an optional description.
/// </summary>
public sealed class SearchResult(Guid id, string? description) : Marker, IRpcCodec<SearchResult>, IEquatable<SearchResult>
{
    public Guid Id { get; } = id;
    public string? Description { get; } = description;

    /// <summary>
    /// Adds a SearchResult marker to the given tree node, resolving <c>WithMarkers(Markers)</c> on the
    /// concrete type. Throws when the node exposes none: a search recipe's whole output is the marker
    /// it attaches, so quietly returning the node unchanged reports "no findings" for code that does
    /// have them — a failure mode with no symptom at all. Every concrete <c>Tree</c> type in this
    /// assembly exposes the wither (enforced by <c>JWitherContractTests</c>), so this cannot fire for
    /// in-tree nodes.
    /// </summary>
    public static T Found<T>(T tree, string? description = null) where T : Tree
    {
        var newMarkers = tree.Markers.Add(new SearchResult(Guid.NewGuid(), description));
        if (tree is J j)
            return (T)J.SetMarkers(j, newMarkers);

        var withMarkers = tree.GetType().GetMethod("WithMarkers", [typeof(Markers)]);
        if (withMarkers == null)
            throw new InvalidOperationException(
                $"{tree.GetType().FullName} does not expose WithMarkers(Markers), so markers cannot be attached to it.");

        return (T)withMarkers.Invoke(tree, [newMarkers])!;
    }

    public void RpcSend(SearchResult after, RpcSendQueue q)
    {
        q.GetAndSend(after, sr => sr.Id);
        q.GetAndSend(after, sr => sr.Description);
    }

    public SearchResult RpcReceive(SearchResult before, RpcReceiveQueue q)
    {
        var id = q.ReceiveAndGet<Guid, string>(before.Id, Guid.Parse);
        var description = q.Receive(before.Description);
        return new SearchResult(id, description);
    }

    public bool Equals(SearchResult? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as SearchResult);
    public override int GetHashCode() => Id.GetHashCode();
}
