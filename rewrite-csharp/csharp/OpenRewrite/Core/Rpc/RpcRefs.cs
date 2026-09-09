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
using System.Collections;
using System.Collections.Concurrent;

namespace OpenRewrite.Core.Rpc;

/// <summary>
/// Identity map from an object to the ref id the remote knows it by, shared across the send
/// queues of concurrent requests. Ids come from a counter rather than the entry count, because
/// <see cref="ConcurrentDictionary{TKey,TValue}.Count"/> takes every bucket lock, and because a
/// count read separately from the insert hands the same id to two threads.
/// </summary>
public sealed class RpcRefs : IDictionary<object, int>
{
    private readonly ConcurrentDictionary<object, int> _ids = new(ReferenceEqualityComparer.Instance);

    private int _lastId;

    /// <summary>The highest id handed out, which <see cref="RollbackTo"/> takes as a mark.</summary>
    public int HighWater => Volatile.Read(ref _lastId);

    public int NextId() => Interlocked.Increment(ref _lastId);

    /// <summary>Drops every ref issued after <paramref name="highWater"/> and reissues from there.</summary>
    public void RollbackTo(int highWater)
    {
        foreach (var kv in _ids)
        {
            if (kv.Value > highWater)
            {
                _ids.TryRemove(kv.Key, out _);
            }
        }
        Interlocked.Exchange(ref _lastId, highWater);
    }

    public bool TryGetValue(object key, out int value) => _ids.TryGetValue(key, out value);

    public int this[object key]
    {
        get => _ids[key];
        set => _ids[key] = value;
    }

    public int Count => _ids.Count;

    public void Clear()
    {
        _ids.Clear();
        Interlocked.Exchange(ref _lastId, 0);
    }

    public void Add(object key, int value) => _ids[key] = value;

    public bool ContainsKey(object key) => _ids.ContainsKey(key);

    public bool Remove(object key) => _ids.TryRemove(key, out _);

    public ICollection<object> Keys => _ids.Keys;

    public ICollection<int> Values => _ids.Values;

    public bool IsReadOnly => false;

    public void Add(KeyValuePair<object, int> item) => _ids[item.Key] = item.Value;

    public bool Contains(KeyValuePair<object, int> item) =>
        _ids.TryGetValue(item.Key, out var v) && v == item.Value;

    public void CopyTo(KeyValuePair<object, int>[] array, int arrayIndex) =>
        ((ICollection<KeyValuePair<object, int>>)_ids).CopyTo(array, arrayIndex);

    public bool Remove(KeyValuePair<object, int> item) =>
        ((ICollection<KeyValuePair<object, int>>)_ids).Remove(item);

    public IEnumerator<KeyValuePair<object, int>> GetEnumerator() => _ids.GetEnumerator();

    IEnumerator IEnumerable.GetEnumerator() => GetEnumerator();
}
