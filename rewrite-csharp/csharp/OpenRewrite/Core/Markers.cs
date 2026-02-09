using Rewrite.Core.Rpc;

namespace Rewrite.Core;

/// <summary>
/// A marker that can be attached to any LST element to carry additional metadata.
/// </summary>
public interface Marker
{
    Guid Id { get; }
}

/// <summary>
/// A collection of markers attached to an LST element.
/// Markers allow attaching metadata without modifying the tree structure.
/// </summary>
public sealed record Markers(Guid Id, IList<Marker> MarkerList) : IRpcCodec<Markers>
{
    public void RpcSend(Markers after, RpcSendQueue q)
    {
        q.GetAndSend(after, m => m.Id);
        q.GetAndSendList(after, m => m.MarkerList,
            m => (object)m.Id, null);
    }

    public Markers RpcReceive(Markers before, RpcReceiveQueue q)
    {
        // Receive-side not yet implemented
        throw new NotImplementedException("Markers.RpcReceive");
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
        this with { MarkerList = [..MarkerList, marker] };

    public Markers Remove<T>() where T : Marker =>
        this with { MarkerList = MarkerList.Where(m => m is not T).ToList() };
}
