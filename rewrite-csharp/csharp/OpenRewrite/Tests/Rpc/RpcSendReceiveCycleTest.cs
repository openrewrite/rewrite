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
using OpenRewrite.Core.Rpc;
using OpenRewrite.Java;
using OpenRewrite.Java.Rpc;
using Rewrite.Core.Rpc;

namespace OpenRewrite.Tests.Rpc;

/// <summary>
/// Proves that the CHANGE path in RpcSendQueue lacks cycle detection for
/// Reference-wrapped types. On the Java side, the thread-local flyweight
/// in Reference.asRef masks this bug. The C# side creates new Reference
/// instances, exposing the infinite recursion.
/// </summary>
public class RpcSendReceiveCycleTest
{
    /// <summary>
    /// Creates two separate instances of the same cyclic type (simulating
    /// deserialized types with different reference identity), then sends
    /// them through the sender in CHANGE mode. Without cycle detection in
    /// the CHANGE path, this stack-overflows.
    /// </summary>
    [Fact]
    public void CyclicTypeInChangePath()
    {
        // Build a cycle: Node -> interfaces -> ISelf<Node> -> typeParams -> Node
        // Instance 1
        var iface1 = JavaType.ShallowClass.Build("com.example.ISelf");
        var param1 = new JavaType.Parameterized();
        var node1 = new JavaType.Class();
        node1.UnsafeSet(1, JavaType.FullyQualified.FullyQualifiedKind.Class, "com.example.Node",
            null, null, null, null,
            new List<JavaType.FullyQualified> { param1 }, null, null);
        param1.UnsafeSet(iface1, new List<JavaType> { node1 });

        // Instance 2 — same structure, different objects
        var iface2 = JavaType.ShallowClass.Build("com.example.ISelf");
        var param2 = new JavaType.Parameterized();
        var node2 = new JavaType.Class();
        node2.UnsafeSet(1, JavaType.FullyQualified.FullyQualifiedKind.Class, "com.example.Node",
            null, null, null, null,
            new List<JavaType.FullyQualified> { param2 }, null, null);
        param2.UnsafeSet(iface2, new List<JavaType> { node2 });

        Assert.NotSame(node1, node2);

        // Send node2 with node1 as before — triggers the CHANGE path
        var allData = new List<RpcObjectData>();
        var sendRefs = new Dictionary<object, int>(ReferenceEqualityComparer.Instance);
        var sendQueue = new RpcSendQueue(1024, batch => allData.AddRange(batch),
            sendRefs, null, false);

        sendQueue.Send(Reference.AsRef(node2), Reference.AsRef(node1),
            () => new JavaSender().VisitType(node2, sendQueue));
        sendQueue.Flush();

        Assert.NotEmpty(allData);
    }
}
