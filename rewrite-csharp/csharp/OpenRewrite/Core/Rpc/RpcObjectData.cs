using System.Text.Json.Serialization;

namespace Rewrite.Core.Rpc;

public sealed class RpcObjectData
{
    public const int AddedListItem = -1;

    [JsonConverter(typeof(JsonStringEnumConverter))]
    public required ObjectState State { get; init; }

    public string? ValueType { get; init; }

    public object? Value { get; init; }

    public int? Ref { get; init; }

    public object? Trace { get; init; }

    public T? GetValue<T>()
    {
        if (Value is null)
            return default;

        if (Value is T t)
            return t;

        throw new InvalidOperationException(
            $"Cannot convert value of type {Value.GetType()} to {typeof(T).Name}");
    }

    public enum ObjectState
    {
        NO_CHANGE,
        ADD,
        DELETE,
        CHANGE,
        END_OF_OBJECT
    }
}
