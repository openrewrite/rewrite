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
/// Diffing a Reference-wrapped cyclic type graph must terminate. A changed ref
/// slot is re-added (see RpcSendQueue.Send), and the walk of the new value's
/// graph terminates because each node is registered in the refs table before
/// its fields are walked, so a cycle resolves to a ref-only ADD.
/// </summary>
public class RpcSendReceiveCycleTest
{
    /// <summary>
    /// Creates two separate instances of the same cyclic type (simulating
    /// deserialized types with different reference identity), then diffs one
    /// against the other. Without the ref-table registration this would
    /// stack-overflow.
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

        // Diff node2 against node1 as the before state
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
