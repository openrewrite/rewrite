using Rewrite.Core.Rpc;
using static OpenRewrite.Core.Rpc.RpcObjectData;
using static OpenRewrite.Core.Rpc.RpcObjectData.ObjectState;

namespace OpenRewrite.Core.Rpc;

public class RpcSendQueue
{
    /// <summary>
    /// Overrides for C# types whose Java names don't follow the convention.
    /// Key: C# type, Value: Java fully-qualified class name.
    /// </summary>
    private static readonly Dictionary<Type, string> JavaTypeNameOverrides = new();

    public static void RegisterJavaTypeName(Type csharpType, string javaTypeName)
    {
        JavaTypeNameOverrides[csharpType] = javaTypeName;
    }

    private readonly int _batchSize;
    private readonly List<RpcObjectData> _batch;
    private readonly Action<List<RpcObjectData>> _drain;
    private readonly Dictionary<object, int> _refs;
    private readonly string? _sourceFileType;
    private readonly bool _trace;
    private readonly IRpcCodec? _treeCodec;

    private object? _before;

    public RpcSendQueue(int batchSize, Action<List<RpcObjectData>> drain,
                        Dictionary<object, int> refs, string? sourceFileType, bool trace,
                        IRpcCodec? treeCodec = null)
    {
        _batchSize = batchSize;
        _batch = new List<RpcObjectData>(batchSize);
        _drain = drain;
        _refs = refs;
        _sourceFileType = sourceFileType;
        _trace = trace;
        _treeCodec = treeCodec;
    }

    /// <summary>
    /// Finds the codec for a value: self-codecs (like Markers) first,
    /// then the injected tree codec for tree nodes.
    /// </summary>
    private IRpcCodec? GetCodecFor(object val)
    {
        if (val is IRpcCodec selfCodec) return selfCodec;
        if (val is Marker) return null; // Markers without IRpcCodec are serialized as plain values
        if (_treeCodec != null && GetValueType(val) != null) return _treeCodec;
        return null;
    }

    public void Put(RpcObjectData rpcObjectData)
    {
        _batch.Add(rpcObjectData);
        if (_batch.Count == _batchSize)
        {
            Flush();
        }
    }

    public void Flush()
    {
        if (_batch.Count == 0) return;
        _drain(new List<RpcObjectData>(_batch));
        _batch.Clear();
    }

    public void GetAndSend<T, U>(T parent, Func<T, U?> value)
    {
        GetAndSend(parent, value, (Action<U>?)null);
    }

    public void GetAndSend<T, U>(T parent, Func<T, U?> value, Action<U>? onChange)
    {
        var after = value(parent);
        var before = _before == null ? default : value((T)_before);
        Send(after, before, onChange == null || after == null ? null : () => onChange(after));
    }

    public void GetAndSendListAsRef<T, U>(T? parent,
                                           Func<T, IList<U>?> values,
                                           Func<U, object> id,
                                           Action<U>? onChange) where T : class
    {
        GetAndSendList(parent, values, id, onChange, true);
    }

    public void GetAndSendList<T, U>(T? parent,
                                      Func<T, IList<U>?> values,
                                      Func<U, object> id,
                                      Action<U>? onChange) where T : class
    {
        GetAndSendList(parent, values, id, onChange, false);
    }

    private void GetAndSendList<T, U>(T? parent,
                                       Func<T, IList<U>?> values,
                                       Func<U, object> id,
                                       Action<U>? onChange,
                                       bool asRef) where T : class
    {
        var after = parent == null ? null : values(parent);
        var before = _before == null ? null : values((T)_before);
        SendList(after, before, id, onChange, asRef);
    }

    public void Send<T>(T? after, T? before, Action? onChange)
    {
        var afterVal = Reference.GetValue<object>(after);
        var beforeVal = Reference.GetValue<object>(before);

        if (ReferenceEquals(beforeVal, afterVal))
        {
            Put(new RpcObjectData { State = NO_CHANGE });
        }
        else if (beforeVal == null || (afterVal != null && afterVal.GetType() != beforeVal.GetType()))
        {
            Add(after!, onChange);
        }
        else if (afterVal == null)
        {
            Put(new RpcObjectData { State = DELETE });
        }
        else
        {
            var afterCodec = GetCodecFor(afterVal);
            Put(new RpcObjectData
            {
                State = CHANGE,
                ValueType = GetValueType(afterVal),
                Value = onChange == null && afterCodec == null ? afterVal : null
            });
            DoChange(afterVal, beforeVal, onChange, afterCodec);
        }
    }

    private void SendList<T>(IList<T>? after, IList<T>? before,
                              Func<T, object> id, Action<T>? onChange, bool asRef)
    {
        Send(after, before, () =>
        {
            if (after == null)
                throw new InvalidOperationException("A DELETE event should have been sent.");

            var beforeIdx = PutListPositions(after, before, id);

            foreach (var anAfter in after)
            {
                var itemId = id(anAfter);
                var beforePos = beforeIdx.GetValueOrDefault(itemId, -1);
                Action? onChangeRun = onChange == null ? null : () => onChange(anAfter);

                if (!beforeIdx.ContainsKey(itemId))
                {
                    Add(asRef ? Reference.AsRef(anAfter) : anAfter!, onChangeRun);
                }
                else
                {
                    var aBefore = before == null ? default : before[beforePos];
                    if (ReferenceEquals(aBefore, anAfter))
                    {
                        Put(new RpcObjectData { State = NO_CHANGE });
                    }
                    else if (aBefore == null || anAfter!.GetType() != aBefore.GetType())
                    {
                        Add(asRef ? Reference.AsRef(anAfter) : anAfter!, onChangeRun);
                    }
                    else
                    {
                        Put(new RpcObjectData
                        {
                            State = CHANGE,
                            ValueType = GetValueType(anAfter)
                        });
                        DoChange(anAfter, aBefore, onChangeRun,
                                 GetCodecFor(anAfter!));
                    }
                }
            }
        });
    }

    private Dictionary<object, int> PutListPositions<T>(IList<T> after, IList<T>? before, Func<T, object> id)
    {
        var beforeIdx = new Dictionary<object, int>(ReferenceEqualityComparer.Instance);
        if (before != null)
        {
            for (int i = 0; i < before.Count; i++)
            {
                beforeIdx[id(before[i])] = i;
            }
        }

        var positions = new List<int>();
        foreach (var t in after)
        {
            if (beforeIdx.TryGetValue(id(t), out var beforePos))
            {
                positions.Add(beforePos);
            }
            else
            {
                positions.Add(AddedListItem);
            }
        }

        Put(new RpcObjectData { State = CHANGE, Value = positions });
        return beforeIdx;
    }

    private void Add(object after, Action? onChange)
    {
        var afterVal = Reference.GetValueNonNull<object>(after);
        int? refValue = null;

        if (after is Reference)
        {
            if (_refs.TryGetValue(afterVal, out var existingRef))
            {
                Put(new RpcObjectData { State = ADD, Ref = existingRef });
                return;
            }
            refValue = _refs.Count + 1;
            _refs[afterVal] = refValue.Value;
        }

        var afterCodec = GetCodecFor(afterVal);
        Put(new RpcObjectData
        {
            State = ADD,
            ValueType = GetValueType(afterVal),
            Value = onChange == null && afterCodec == null ? afterVal : null,
            Ref = refValue
        });
        DoChange(afterVal, null, onChange, afterCodec);
    }

    private void DoChange(object? after, object? before, Action? onChange, IRpcCodec? afterCodec)
    {
        var lastBefore = _before;
        _before = before;
        try
        {
            if (onChange != null)
            {
                if (after != null)
                {
                    onChange();
                }
            }
            else if (afterCodec != null && after != null)
            {
                afterCodec.RpcSend(after, this);
            }
        }
        finally
        {
            _before = lastBefore;
        }
    }

    private static string? GetValueType(object? after)
    {
        if (after == null) return null;

        var type = after.GetType();
        if (type.IsPrimitive || type.IsArray ||
            (type.Namespace != null && type.Namespace.StartsWith("System")) ||
            type == typeof(Guid) ||
            typeof(System.Collections.IEnumerable).IsAssignableFrom(type))
        {
            return null;
        }

        if (type.IsEnum)
        {
            return null;
        }

        return ToJavaTypeName(type);
    }

    /// <summary>
    /// Maps a C# type to its equivalent Java type name for RPC protocol compatibility.
    /// Uses namespace-based conventions to derive the Java class name.
    /// </summary>
    private static string? ToJavaTypeName(Type type)
    {
        // Check explicit overrides first
        if (JavaTypeNameOverrides.TryGetValue(type, out var overrideName))
            return overrideName;

        var name = type.Name;

        // Strip generic arity suffix (e.g., JRightPadded`1 → JRightPadded)
        var backtick = name.IndexOf('`');
        if (backtick >= 0) name = name[..backtick];

        // Handle nested types: Outer+Inner → Outer$Inner
        if (type.IsNested && type.DeclaringType != null)
        {
            var outerName = ToJavaTypeName(type.DeclaringType);
            return outerName != null ? outerName + "$" + name : null;
        }

        return type.Namespace switch
        {
            "OpenRewrite.Java" => name switch
            {
                "JRightPadded" or "JLeftPadded" or "JContainer" or "JavaType" =>
                    $"org.openrewrite.java.tree.{name}",
                _ => $"org.openrewrite.java.tree.J${name}",
            },
            "OpenRewrite.CSharp" => $"org.openrewrite.csharp.tree.Cs${name}",
            "OpenRewrite.Core" => name switch
            {
                "Markers" => "org.openrewrite.marker.Markers",
                "SearchResult" => "org.openrewrite.marker.SearchResult",
                "Markup" => "org.openrewrite.marker.Markup",
                "Space" => "org.openrewrite.java.tree.Space",
                "TextComment" => "org.openrewrite.java.tree.TextComment",
                _ => null,
            },
            _ => null
        };
    }
}
