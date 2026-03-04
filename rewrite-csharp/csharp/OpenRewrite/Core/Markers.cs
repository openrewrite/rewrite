using OpenRewrite.Core.Rpc;

namespace OpenRewrite.Core;

/// <summary>
/// A marker that can be attached to any LST element to carry additional metadata.
/// </summary>
public interface Marker
{
    Guid Id { get; }
}

/// <summary>
/// Fallback marker for Java marker types that have no C# equivalent.
/// Similar to Java's RpcMarker, this captures the Id so the marker can
/// be round-tripped without losing identity, while the actual data is ignored.
/// </summary>
public sealed class UnknownMarker(Guid id) : Marker
{
    public Guid Id { get; } = id;
}

/// <summary>
/// A collection of markers attached to an LST element.
/// Markers allow attaching metadata without modifying the tree structure.
/// </summary>
public sealed class Markers(Guid id, IList<Marker> markerList) : IRpcCodec<Markers>, IEquatable<Markers>
{
    public Guid Id { get; } = id;
    public IList<Marker> MarkerList { get; } = markerList;

    public void RpcSend(Markers after, RpcSendQueue q)
    {
        q.GetAndSend(after, m => m.Id);
        q.GetAndSendListAsRef(after, m => m.MarkerList,
            m => (object)m.Id, null);
    }

    public Markers RpcReceive(Markers before, RpcReceiveQueue q)
    {
        var id = q.ReceiveAndGet<Guid, string>(before.Id, Guid.Parse);
        var markerList = q.ReceiveList(before.MarkerList, marker =>
        {
            if (marker is IRpcCodec codec)
                return (Marker)codec.RpcReceive(marker, q);
            return marker;
        });
        return new Markers(id, markerList!);
    }

    public static readonly Markers Empty = new(Guid.Empty, []);

    public static Markers Build(IEnumerable<Marker> markers)
    {
        var list = markers.ToList();
        return list.Count == 0 ? Empty : new Markers(Guid.NewGuid(), list);
    }

    public T? FindFirst<T>() where T : Marker =>
        MarkerList.OfType<T>().FirstOrDefault();

    public Markers Add(Marker marker) =>
        new(Id, [..MarkerList, marker]);

    public Markers Remove<T>() where T : Marker =>
        new(Id, MarkerList.Where(m => m is not T).ToList());

    public Markers WithId(Guid id) =>
        id == Id ? this : new(id, MarkerList);

    public Markers WithMarkerList(IList<Marker> markerList) =>
        ReferenceEquals(markerList, MarkerList) ? this : new(Id, markerList);

    public bool Equals(Markers? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as Markers);
    public override int GetHashCode() => Id.GetHashCode();
}
