using System.Reflection;
using Rewrite.Core.Rpc;
using Rewrite.Java;

namespace Rewrite.Core;

/// <summary>
/// Marks an LST node as a search result with an optional description.
/// </summary>
public sealed class SearchResult(Guid id, string? description) : Marker, IRpcCodec<SearchResult>, IEquatable<SearchResult>
{
    public Guid Id { get; } = id;
    public string? Description { get; } = description;

    /// <summary>
    /// Adds a SearchResult marker to the given tree node.
    /// Uses reflection to call WithMarkers on the concrete type.
    /// </summary>
    public static T Found<T>(T tree, string? description = null) where T : J
    {
        var newMarkers = tree.Markers.Add(new SearchResult(Guid.NewGuid(), description));
        var withMarkers = tree.GetType().GetMethod("WithMarkers", [typeof(Markers)]);
        return withMarkers != null ? (T)withMarkers.Invoke(tree, [newMarkers])! : tree;
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
