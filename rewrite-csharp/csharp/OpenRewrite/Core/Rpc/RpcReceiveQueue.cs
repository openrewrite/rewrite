using System.Runtime.CompilerServices;
using System.Text.Json;
using Newtonsoft.Json.Linq;
using OpenRewrite.CSharp;
using OpenRewrite.Java;
using static OpenRewrite.Core.Rpc.RpcObjectData.ObjectState;

namespace OpenRewrite.Core.Rpc;

/// <summary>
/// Deserializes RPC object data received from a remote process.
/// Mirrors Java's org.openrewrite.rpc.RpcReceiveQueue.
/// </summary>
public class RpcReceiveQueue
{
    private readonly Queue<RpcObjectData> _batch = new();
    private readonly Dictionary<int, object> _refs;
    private readonly Func<List<RpcObjectData>>? _pull;
    private readonly string? _sourceFileType;
    private readonly IRpcCodec? _treeCodec;

    public RpcReceiveQueue(List<RpcObjectData> data, Dictionary<int, object> refs,
                           string? sourceFileType, IRpcCodec? treeCodec = null)
    {
        foreach (var item in data) _batch.Enqueue(item);
        _refs = refs;
        _sourceFileType = sourceFileType;
        _treeCodec = treeCodec;
    }

    public RpcReceiveQueue(Dictionary<int, object> refs, Func<List<RpcObjectData>> pull,
                           string? sourceFileType, IRpcCodec? treeCodec = null)
    {
        _refs = refs;
        _pull = pull;
        _sourceFileType = sourceFileType;
        _treeCodec = treeCodec;
    }

    public RpcObjectData Take()
    {
        if (_batch.Count == 0 && _pull != null)
        {
            foreach (var item in _pull()) _batch.Enqueue(item);
        }
        return _batch.Dequeue();
    }

    /// <summary>
    /// Receive a simple value from the remote.
    /// </summary>
    public T? Receive<T>(T? before) => Receive(before, (Func<T, T>?)null);

    /// <summary>
    /// Receive a value and apply a mapping function to convert it.
    /// Only applied when the value from the queue is not null and differs from before.
    /// </summary>
    public T? ReceiveAndGet<T, U>(T? before, Func<U, T?> mapping)
    {
        var message = Take();
        switch (message.State)
        {
            case NO_CHANGE:
                return before;
            case DELETE:
                return default;
            case ADD:
            case CHANGE:
                return mapping(ExtractValue<U>(message.Value));
            default:
                throw new InvalidOperationException($"Unexpected state in ReceiveAndGet: {message.State}");
        }
    }

    /// <summary>
    /// Receive a value from the remote with an optional onChange callback
    /// for receiving constituent parts of complex values.
    /// </summary>
    public T? Receive<T>(T? before, Func<T, T>? onChange)
    {
        var message = Take();
        int? @ref = null;
        switch (message.State)
        {
            case NO_CHANGE:
                return before;
            case DELETE:
                return default;
            case ADD:
                @ref = message.Ref;
                if (@ref != null && message.ValueType == null && message.Value == null)
                {
                    // Pure reference to an existing object
                    if (_refs.TryGetValue(@ref.Value, out var existing))
                        return (T)existing;
                    throw new InvalidOperationException(
                        $"Received reference to unknown object: {@ref}");
                }
                // New object or forward declaration with ref
                if (message.ValueType != null && message.Value != null)
                {
                    // Non-codec type with inline value (e.g. RecipesThatMadeChanges)
                    before = DeserializeInline<T>(message.ValueType, message.Value);
                }
                else if (message.ValueType != null)
                {
                    // Codec type: create shell, data follows in CHANGE messages
                    before = NewObj<T>(message.ValueType);
                }
                else
                {
                    before = ExtractValue<T>(message.Value);
                }
                if (@ref != null && before != null)
                    _refs[@ref.Value] = before;
                goto case CHANGE; // Intentional fall-through
            case CHANGE:
                T? after;
                if (onChange != null)
                {
                    after = onChange(before!);
                }
                else if (_treeCodec != null && before != null && IsTreeType(before))
                {
                    // Tree types must go through the visitor (TreeCodec) which handles
                    // PreVisit (Id, Prefix, Markers) before the type-specific fields.
                    after = (T)_treeCodec.RpcReceive(before, this);
                }
                else if (before is IRpcCodec selfCodec)
                {
                    after = (T)selfCodec.RpcReceive(before, this);
                }
                else if (message.ValueType == null)
                {
                    after = ExtractValue<T>(message.Value);
                }
                else
                {
                    after = before;
                }
                if (@ref != null && after != null)
                    _refs[@ref.Value] = after;
                return after;
            default:
                throw new InvalidOperationException($"Unknown state: {message.State}");
        }
    }

    /// <summary>
    /// Receive a list from the remote. Uses position indices for element matching.
    /// </summary>
    public IList<T>? ReceiveList<T>(IList<T>? before, Func<T, T>? onChange)
    {
        var msg = Take();
        switch (msg.State)
        {
            case NO_CHANGE:
                return before;
            case DELETE:
                return null;
            case ADD:
                before = new List<T>();
                goto case CHANGE;
            case CHANGE:
                var posMsg = Take();
                if (posMsg.State != CHANGE)
                    throw new InvalidOperationException(
                        $"Expected CHANGE with positions in receiveList, got {posMsg.State}");
                var positions = ExtractPositions(posMsg.Value!);
                var after = new List<T>(positions.Count);
                foreach (var beforeIdx in positions)
                {
                    var beforeItem = beforeIdx >= 0 && before != null ? before[beforeIdx] : default;
                    after.Add(Receive(beforeItem, onChange)!);
                }
                return after;
            default:
                throw new InvalidOperationException($"Unsupported state for lists: {msg.State}");
        }
    }

    /// <summary>
    /// Creates an enum mapping function for use with ReceiveAndGet.
    /// </summary>
    public static Func<object, T> ToEnum<T>() where T : struct, Enum
    {
        return value =>
        {
            var str = value is JsonElement je ? je.GetString()! : (string)value;
            return Enum.Parse<T>(str);
        };
    }

    // ---- Private helpers ----

    private static T ExtractValue<T>(object? value)
    {
        if (value == null) return default!;
        if (value is T t) return t;

        if (value is JsonElement je)
        {
            return ExtractFromJsonElement<T>(je);
        }

        // Try numeric conversions for mismatched numeric types
        if (typeof(T) == typeof(int) && value is IConvertible ic)
            return (T)(object)ic.ToInt32(null);
        if (typeof(T) == typeof(long) && value is IConvertible lc)
            return (T)(object)lc.ToInt64(null);
        if (typeof(T) == typeof(bool) && value is IConvertible bc)
            return (T)(object)bc.ToBoolean(null);

        return (T)value;
    }

    private static T ExtractFromJsonElement<T>(JsonElement je)
    {
        var targetType = typeof(T);

        // Handle nullable types
        var underlyingType = Nullable.GetUnderlyingType(targetType) ?? targetType;

        if (underlyingType == typeof(string))
            return (T)(object)je.GetString()!;
        if (underlyingType == typeof(Guid))
            return (T)(object)Guid.Parse(je.GetString()!);
        if (underlyingType == typeof(bool))
            return (T)(object)je.GetBoolean();
        if (underlyingType == typeof(int))
            return (T)(object)je.GetInt32();
        if (underlyingType == typeof(long))
            return (T)(object)je.GetInt64();
        if (underlyingType == typeof(double))
            return (T)(object)je.GetDouble();

        if (je.ValueKind == JsonValueKind.String)
            return (T)(object)je.GetString()!;
        if (je.ValueKind == JsonValueKind.Null)
            return default!;

        // For object values, return as-is and let caller handle
        return (T)(object)je;
    }

    private static List<int> ExtractPositions(object value)
    {
        if (value is JsonElement je && je.ValueKind == JsonValueKind.Array)
        {
            var result = new List<int>();
            foreach (var elem in je.EnumerateArray())
                result.Add(elem.GetInt32());
            return result;
        }

        if (value is IList<int> intList) return new List<int>(intList);

        if (value is System.Collections.IEnumerable enumerable)
        {
            var result = new List<int>();
            foreach (var item in enumerable)
                result.Add(Convert.ToInt32(item));
            return result;
        }

        throw new InvalidOperationException(
            $"Cannot extract positions from {value?.GetType().FullName}");
    }

    private static T NewObj<T>(string javaTypeName)
    {
        var type = FromJavaTypeName(javaTypeName);
        if (type == null)
        {
            // Generic container types: use T directly since the caller knows the exact closed type
            if (javaTypeName is "org.openrewrite.java.tree.JRightPadded"
                             or "org.openrewrite.java.tree.JLeftPadded"
                             or "org.openrewrite.java.tree.JContainer")
            {
                type = typeof(T);
            }
            else if (typeof(Marker).IsAssignableFrom(typeof(T)))
            {
                // Unknown marker type from Java — use UnknownMarker as fallback
                return (T)(object)new UnknownMarker(Guid.NewGuid());
            }
            else
            {
                throw new InvalidOperationException(
                    $"Cannot map Java type name to C# type: {javaTypeName}");
            }
        }
        if (type.IsInterface || type.IsAbstract)
        {
            if (typeof(Marker).IsAssignableFrom(type))
                return (T)(object)new UnknownMarker(Guid.NewGuid());
            throw new InvalidOperationException(
                $"Cannot instantiate interface/abstract type: {type.FullName} (from {javaTypeName})");
        }
        return (T)RuntimeHelpers.GetUninitializedObject(type);
    }

    /// <summary>
    /// Deserializes a non-codec object that was sent inline in the ADD message.
    /// The value is typically a JObject from Newtonsoft.Json deserialization.
    /// </summary>
    private static T DeserializeInline<T>(string javaTypeName, object value)
    {
        var type = FromJavaTypeName(javaTypeName) ?? typeof(T);

        // If the resolved type is an interface or abstract class, use UnknownMarker
        // as fallback for marker types that have no C# equivalent (e.g., GitProvenance,
        // LstProvenance, BuildTool, etc. added by the mod CLI).
        if ((type.IsInterface || type.IsAbstract) && typeof(Marker).IsAssignableFrom(type))
        {
            if (value is JObject jobj)
            {
                var idToken = jobj["id"];
                var id = idToken != null ? Guid.Parse(idToken.ToString()) : Guid.NewGuid();
                return (T)(object)new UnknownMarker(id);
            }
            return (T)(object)new UnknownMarker(Guid.NewGuid());
        }

        if (value is JObject jobjNormal)
            return (T)jobjNormal.ToObject(type)!;

        if (value is T t)
            return t;

        return ExtractValue<T>(value);
    }

    private static bool IsTreeType(object obj)
    {
        var type = obj.GetType();
        var ns = type.Namespace;
        return ns != null && (ns.StartsWith("OpenRewrite.Java") || ns.StartsWith("OpenRewrite.CSharp"));
    }

    /// <summary>
    /// Maps a Java type name to its C# Type. Reverse of RpcSendQueue.ToJavaTypeName.
    /// </summary>
    private static Type? FromJavaTypeName(string javaTypeName)
    {
        // Known direct mappings
        return javaTypeName switch
        {
            "org.openrewrite.java.tree.Space" => typeof(Space),
            "org.openrewrite.java.tree.TextComment" => typeof(TextComment),
            "org.openrewrite.marker.Markers" => typeof(Markers),
            "org.openrewrite.marker.SearchResult" => typeof(SearchResult),
            "org.openrewrite.marker.RecipesThatMadeChanges" => typeof(RecipesThatMadeChanges),

            // Special C# type name overrides (reverse of RpcSendQueue.RegisterJavaTypeName)
            "org.openrewrite.csharp.tree.Cs$BlockScopeNamespaceDeclaration" =>
                typeof(NamespaceDeclaration),
            "org.openrewrite.csharp.tree.Cs$Lambda" =>
                typeof(CsLambda),
            // Cs-prefixed types that correspond to unprefixed Java names
            "org.openrewrite.csharp.tree.Cs$MethodDeclaration" =>
                typeof(CsMethodDeclaration),
            "org.openrewrite.csharp.tree.Cs$ConstrainedTypeParameter" =>
                typeof(ConstrainedTypeParameter),
            "org.openrewrite.csharp.tree.Cs$ExpressionStatement" =>
                typeof(ExpressionStatement),
            "org.openrewrite.java.tree.J$VariableDeclarations$NamedVariable" =>
                typeof(NamedVariable),

            // Marker type overrides
            "org.openrewrite.java.marker.Semicolon" =>
                typeof(Semicolon),
            "org.openrewrite.csharp.marker.PrimaryConstructor" =>
                typeof(PrimaryConstructor),
            "org.openrewrite.csharp.marker.Implicit" =>
                typeof(Implicit),
            "org.openrewrite.csharp.marker.Struct" =>
                typeof(Struct),
            "org.openrewrite.csharp.marker.RecordClass" =>
                typeof(RecordClass),
            "org.openrewrite.csharp.marker.ExpressionBodied" =>
                typeof(ExpressionBodied),
            "org.openrewrite.java.marker.OmitParentheses" =>
                typeof(OmitParentheses),
            "org.openrewrite.csharp.marker.PatternCombinator" =>
                typeof(PatternCombinator),
            "org.openrewrite.csharp.marker.WhereClauseOrder" =>
                typeof(WhereClauseOrder),
            "org.openrewrite.csharp.marker.MultiDimensionContinuation" =>
                typeof(MultiDimensionContinuation),
            "org.openrewrite.java.marker.TrailingComma" =>
                typeof(TrailingComma),
            "org.openrewrite.java.marker.OmitBraces" =>
                typeof(OmitBraces),
            "org.openrewrite.java.marker.NullSafe" =>
                typeof(NullSafe),
            "org.openrewrite.csharp.marker.PointerMemberAccess" =>
                typeof(PointerMemberAccess),

            _ => FromJavaTypeNameByConvention(javaTypeName)
        };
    }

    private static Type? FromJavaTypeNameByConvention(string javaTypeName)
    {
        // Pattern: org.openrewrite.java.tree.J$ClassName → OpenRewrite.Java.ClassName
        if (javaTypeName.StartsWith("org.openrewrite.java.tree.J$"))
        {
            var name = javaTypeName["org.openrewrite.java.tree.J$".Length..];
            return FindType("OpenRewrite.Java", name);
        }

        // Pattern: org.openrewrite.java.tree.JavaType$X → OpenRewrite.Java.JavaType+X
        if (javaTypeName.StartsWith("org.openrewrite.java.tree.JavaType$"))
        {
            var name = javaTypeName["org.openrewrite.java.tree.JavaType$".Length..];
            return FindType("OpenRewrite.Java", "JavaType+" + name);
        }

        // Pattern: org.openrewrite.java.tree.X → OpenRewrite.Java.X (JRightPadded etc.)
        if (javaTypeName.StartsWith("org.openrewrite.java.tree."))
        {
            var name = javaTypeName["org.openrewrite.java.tree.".Length..];
            return FindType("OpenRewrite.Java", name);
        }

        // Pattern: org.openrewrite.csharp.tree.Linq$ClassName → OpenRewrite.CSharp.ClassName
        if (javaTypeName.StartsWith("org.openrewrite.csharp.tree.Linq$"))
        {
            var name = javaTypeName["org.openrewrite.csharp.tree.Linq$".Length..];
            return FindType("OpenRewrite.CSharp", name);
        }

        // Pattern: org.openrewrite.csharp.tree.Cs$ClassName → OpenRewrite.CSharp.ClassName
        if (javaTypeName.StartsWith("org.openrewrite.csharp.tree.Cs$"))
        {
            var name = javaTypeName["org.openrewrite.csharp.tree.Cs$".Length..];
            return FindType("OpenRewrite.CSharp", name);
        }

        // Marker type conventions — markers live in marker packages, not tree packages
        // Pattern: org.openrewrite.csharp.marker.ClassName → OpenRewrite.CSharp.ClassName
        if (javaTypeName.StartsWith("org.openrewrite.csharp.marker."))
        {
            var name = javaTypeName["org.openrewrite.csharp.marker.".Length..];
            return FindType("OpenRewrite.CSharp", name);
        }

        // Pattern: org.openrewrite.java.marker.ClassName → OpenRewrite.Java.ClassName
        if (javaTypeName.StartsWith("org.openrewrite.java.marker."))
        {
            var name = javaTypeName["org.openrewrite.java.marker.".Length..];
            return FindType("OpenRewrite.Java", name);
        }

        // Pattern: org.openrewrite.marker.Markup$X → OpenRewrite.Core.Markup+X
        if (javaTypeName.StartsWith("org.openrewrite.marker.Markup$"))
        {
            var name = javaTypeName["org.openrewrite.marker.Markup$".Length..];
            return FindType("OpenRewrite.Core", "Markup+" + name);
        }

        // Pattern: org.openrewrite.marker.ClassName → OpenRewrite.Core.ClassName
        if (javaTypeName.StartsWith("org.openrewrite.marker."))
        {
            var name = javaTypeName["org.openrewrite.marker.".Length..];
            return FindType("OpenRewrite.Core", name);
        }

        return null;
    }

    private static Type? FindType(string ns, string name)
    {
        var fullName = $"{ns}.{name}";
        // Search in all loaded assemblies
        foreach (var assembly in AppDomain.CurrentDomain.GetAssemblies())
        {
            var type = assembly.GetType(fullName);
            if (type != null) return type;
        }
        return null;
    }
}
