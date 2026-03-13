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
public sealed class SearchResult(Guid id, string? description, string? recipeName = null) : Marker, IRpcCodec<SearchResult>, IEquatable<SearchResult>
{
    public Guid Id { get; } = id;
    public string? Description { get; } = description;
    public string? RecipeName { get; } = recipeName;

    /// <summary>
    /// The name of the recipe currently being executed. Set by the Visit handler
    /// so that SearchResult.Found() can capture it at creation time.
    /// </summary>
    [ThreadStatic]
    private static string? _currentRecipeName;

    public static string? CurrentRecipeName
    {
        get => _currentRecipeName;
        set => _currentRecipeName = value;
    }

    /// <summary>
    /// Adds a SearchResult marker to the given tree node.
    /// Uses reflection to call WithMarkers on the concrete type.
    /// </summary>
    public static T Found<T>(T tree, string? description = null) where T : J
    {
        var newMarkers = tree.Markers.Add(new SearchResult(Guid.NewGuid(), description, _currentRecipeName));
        var withMarkers = tree.GetType().GetMethod("WithMarkers", [typeof(Markers)]);
        return withMarkers != null ? (T)withMarkers.Invoke(tree, [newMarkers])! : tree;
    }

    public void RpcSend(SearchResult after, RpcSendQueue q)
    {
        q.GetAndSend(after, sr => sr.Id);
        q.GetAndSend(after, sr => sr.Description);
        q.GetAndSend(after, sr => sr.RecipeName);
    }

    public SearchResult RpcReceive(SearchResult before, RpcReceiveQueue q)
    {
        var id = q.ReceiveAndGet<Guid, string>(before.Id, Guid.Parse);
        var description = q.Receive(before.Description);
        // recipeName is only sent from remote to Java, not from Java to remote
        return new SearchResult(id, description);
    }

    public bool Equals(SearchResult? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as SearchResult);
    public override int GetHashCode() => Id.GetHashCode();
}
