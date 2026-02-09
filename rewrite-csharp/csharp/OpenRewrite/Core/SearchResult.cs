using Rewrite.Core.Rpc;

namespace Rewrite.Core;

/// <summary>
/// Marks an LST node as a search result with an optional description.
/// </summary>
public sealed record SearchResult(Guid Id, string? Description) : Marker, IRpcCodec<SearchResult>
{
    public void RpcSend(SearchResult after, RpcSendQueue q)
    {
        q.GetAndSend(after, sr => sr.Id);
        q.GetAndSend(after, sr => sr.Description);
    }

    public SearchResult RpcReceive(SearchResult before, RpcReceiveQueue q)
    {
        throw new NotImplementedException("SearchResult.RpcReceive");
    }
}
