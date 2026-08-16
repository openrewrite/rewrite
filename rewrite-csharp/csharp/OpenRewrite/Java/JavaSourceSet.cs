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
using OpenRewrite.Core;
using OpenRewrite.Core.Rpc;
using OpenRewrite.Java.Rpc;

namespace OpenRewrite.Java;

/// <summary>
/// Mirrors org.openrewrite.java.marker.JavaSourceSet. It rides on every source file in a Java
/// source set, resource files included, so it reaches this peer through Xml as well as C#.
/// <para>
/// Field order is the protocol; see JavaSourceSet#rpcSend on the Java side. Without this codec the
/// marker resolves to <see cref="UnknownMarker"/>, which consumes one message where Java sends
/// many, and the queue then desynchronizes with no diagnostic.
/// </para>
/// </summary>
public sealed class JavaSourceSet(
    Guid id,
    string name,
    IList<JavaType.FullyQualified> classpath,
    IDictionary<string, IList<JavaType.FullyQualified>> gavToTypes
) : Marker, IRpcCodec<JavaSourceSet>
{
    public Guid Id { get; } = id;
    public string Name { get; } = name;
    public IList<JavaType.FullyQualified> Classpath { get; } = classpath;
    public IDictionary<string, IList<JavaType.FullyQualified>> GavToTypes { get; } = gavToTypes;

    public void RpcSend(JavaSourceSet after, RpcSendQueue q)
    {
        var typeSender = new JavaSender();
        q.GetAndSend(after, s => s.Id);
        q.GetAndSend(after, s => s.Name);
        q.GetAndSendListAsRef(after, s => s.Classpath, TypeKey, t => typeSender.VisitType(t, q));

        var gavs = after.GavToTypes.Keys.ToList();
        q.GetAndSendList(after, _ => gavs, gav => gav, null);
        foreach (var gav in gavs)
        {
            q.GetAndSendListAsRef(after, s => s.GavToTypes[gav], TypeKey,
                t => typeSender.VisitType(t, q));
        }
    }

    // Classpath entries are Class instances in practice; the key only feeds the sender's own diff.
    private static object TypeKey(JavaType.FullyQualified type) =>
        type is JavaType.Class cls ? cls.FullyQualifiedName : type;

    public JavaSourceSet RpcReceive(JavaSourceSet before, RpcReceiveQueue q)
    {
        var typeReceiver = new JavaReceiver();
        var id = q.ReceiveAndGet<Guid, string>(before.Id, Guid.Parse);
        var name = q.Receive(before.Name)!;
        var classpath = q.ReceiveList(before.Classpath,
            t => (JavaType.FullyQualified)typeReceiver.VisitType(t, q)!)!;

        // The uninitialized instance the queue hands back on an ADD has null collections, so the
        // before state is never dereferenced without a guard.
        var beforeGavs = before.GavToTypes;
        var gavs = q.ReceiveList(beforeGavs?.Keys.ToList(), null);
        var gavToTypes = new Dictionary<string, IList<JavaType.FullyQualified>>();
        foreach (var gav in gavs ?? [])
        {
            IList<JavaType.FullyQualified>? beforeBucket = null;
            beforeGavs?.TryGetValue(gav, out beforeBucket);
            gavToTypes[gav] = q.ReceiveList(beforeBucket,
                t => (JavaType.FullyQualified)typeReceiver.VisitType(t, q)!)!;
        }
        return new JavaSourceSet(id, name, classpath, gavToTypes);
    }
}
