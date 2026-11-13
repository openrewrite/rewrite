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
using OpenRewrite.Core.Rpc;
using OpenRewrite.Java;
using OpenRewrite.Java.Rpc;
using Rewrite.Core.Rpc;

namespace OpenRewrite.Tests.Rpc;

public class JavaTypeFlagsRpcTest
{
    private const long ClassFlags = 1 | 1L << 4; // Public, Final
    // Bit 20 is not a Java flag. C# sets it on extension methods.
    private const long MethodFlags = 1 | 1L << 43 | 1L << 20; // Public, Default
    private const long VariableFlags = 1L << 1 | 1L << 3 | 1L << 4; // Private, Static, Final

    [Fact]
    public void FlagsOfClassMethodAndVariableRoundTrip()
    {
        var cls = new JavaType.Class();
        var method = new JavaType.Method(cls, "Bar", MethodFlags,
            JavaType.Primitive.Of(JavaType.PrimitiveKind.Void), null, null, null, null, null, null);
        var member = new JavaType.Variable("Baz", cls, JavaType.Primitive.Of(JavaType.PrimitiveKind.Int), null)
        {
            FlagsBitMap = VariableFlags
        };
        cls.UnsafeSet(ClassFlags, JavaType.FullyQualified.FullyQualifiedKind.Class, "Example.Foo",
            null, null, null, null, null, [member], [method]);

        var data = new List<RpcObjectData>();
        var sq = new RpcSendQueue(1024, batch => data.AddRange(batch), new RpcRefs(), null, false);
        sq.Send(Reference.AsRef(cls), null, () => new JavaSender().VisitType(cls, sq));
        sq.Flush();
        var wire = JsonSerializer.Deserialize<List<RpcObjectData>>(
            JsonSerializer.Serialize(data, RpcJson.Options), RpcJson.Options)!;
        var rq = new RpcReceiveQueue(wire, new Dictionary<int, object>(), null);
        var received = Assert.IsAssignableFrom<JavaType.Class>(
            rq.Receive<JavaType>(null, t => new JavaReceiver().VisitType(t, rq)!));

        Assert.Equal(ClassFlags, received.FlagsBitMap);
        Assert.Equal(MethodFlags, received.Methods![0].FlagsBitMap);
        Assert.Equal(VariableFlags, received.Members![0].FlagsBitMap);
    }
}
