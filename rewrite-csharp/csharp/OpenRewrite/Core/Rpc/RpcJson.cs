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
using System.Text.Encodings.Web;
using System.Text.Json;
using System.Text.Json.Serialization;

namespace OpenRewrite.Core.Rpc;

/// <summary>
/// Shared <see cref="JsonSerializerOptions"/> for the JSON-RPC wire format.
/// <para>
/// Used both by the StreamJsonRpc <c>SystemTextJsonFormatter</c> (which streams
/// the RPC envelope straight to/from the pipe via <c>Utf8JsonWriter</c>/<c>Utf8JsonReader</c>,
/// with no intermediate DOM) and by <see cref="RpcReceiveQueue"/> when deserializing
/// inline object payloads. Keeping a single options instance guarantees the
/// serialize and deserialize sides agree on naming, enum handling, and escaping.
/// </para>
/// <para>
/// The shape matches what the Java peer expects: camelCase property names, enum
/// values as strings, and omitted nulls.
/// </para>
/// </summary>
public static class RpcJson
{
    /// <summary>
    /// The shared, pre-configured options. Configured once and never mutated after
    /// first use (System.Text.Json freezes options on first (de)serialization).
    /// </summary>
    public static JsonSerializerOptions Options { get; } = Create();

    private static JsonSerializerOptions Create()
    {
        var options = new JsonSerializerOptions
        {
            PropertyNamingPolicy = JsonNamingPolicy.CamelCase,
            DefaultIgnoreCondition = JsonIgnoreCondition.WhenWritingNull,
            // Match Newtonsoft's relaxed escaping so the Java/Jackson peer parses
            // byte-identical content. STJ otherwise escapes '+', '<', '>', '&', and
            // other characters for HTML safety, which is unnecessary over a private
            // RPC pipe and would alter (and inflate) the payload.
            Encoder = JavaScriptEncoder.UnsafeRelaxedJsonEscaping,
        };
        options.Converters.Add(new JsonStringEnumConverter());
        return options;
    }
}
