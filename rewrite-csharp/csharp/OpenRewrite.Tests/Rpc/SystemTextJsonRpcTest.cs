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
using System.Text.Json;
using OpenRewrite.Core;
using OpenRewrite.Core.Rpc;

namespace OpenRewrite.Tests.Rpc;

/// <summary>
/// Verifies the JSON-RPC wire format under the streaming System.Text.Json path:
/// the receive side reconstructs inline payloads from <see cref="JsonElement"/>
/// (as produced by SystemTextJsonFormatter) rather than Newtonsoft JObject, and
/// the wire shape stays compatible with the Java peer (camelCase, string enums,
/// omitted nulls).
/// </summary>
public class SystemTextJsonRpcTest
{
    [Fact]
    public void DeserializeInline_UnknownMarker_FromJsonElement_PreservesId()
    {
        var id = Guid.NewGuid();
        // SystemTextJsonFormatter deserializes object-typed payloads to JsonElement.
        object value = JsonSerializer.SerializeToElement(new { id = id.ToString() }, RpcJson.Options);

        // An unmapped marker type name resolves to the Marker interface -> UnknownMarker path.
        var result = RpcReceiveQueue.DeserializeInline<Marker>(
            "org.openrewrite.marker.SomeUnknownMarker", value);

        var unknown = Assert.IsType<UnknownMarker>(result);
        Assert.Equal(id, unknown.Id);
    }

    [Fact]
    public void Serialize_RpcObjectData_IsJavaCompatible()
    {
        var data = new RpcObjectData
        {
            State = RpcObjectData.ObjectState.ADD,
            ValueType = "java.lang.String",
            Value = "hi",
        };

        var json = JsonSerializer.Serialize(data, RpcJson.Options);

        Assert.Contains("\"state\":\"ADD\"", json);   // camelCase key + enum as string
        Assert.Contains("\"valueType\":\"java.lang.String\"", json);
        Assert.Contains("\"value\":\"hi\"", json);
        Assert.DoesNotContain("\"ref\"", json);        // null omitted
        Assert.DoesNotContain("\"trace\"", json);      // null omitted
    }
}
