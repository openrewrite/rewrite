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
using System.Text.Json.Serialization;

namespace OpenRewrite.Core.Rpc;

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
