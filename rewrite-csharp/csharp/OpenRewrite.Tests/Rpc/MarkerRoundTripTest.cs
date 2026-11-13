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
using System.Collections;
using System.Reflection;
using System.Text.Json;
using OpenRewrite.Core;
using OpenRewrite.Core.Rpc;
using OpenRewrite.CSharp;
using OpenRewrite.CSharp.Rpc;
using OpenRewrite.CSharp.Template;
using OpenRewrite.Java;
using OpenRewrite.Test;
using ExecutionContext = OpenRewrite.Core.ExecutionContext;

namespace OpenRewrite.Tests.Rpc;

/// <summary>
/// A marker with no counterpart or no codec on the Java side does not fail the transfer: it
/// arrives there as nothing, or as something else, and what it said is lost. Every marker type
/// this side can attach to a tree is sent here, so a new one fails until Java can carry it.
/// </summary>
public class MarkerRoundTripTest : RpcRewriteTest
{
    public MarkerRoundTripTest(RpcFixture fixture) : base(fixture) { }

    /// <summary>
    /// Markers that never leave this process.
    /// </summary>
    private static readonly Type[] NeverSent =
    [
        typeof(UnknownMarker), // stands in for a Java marker this side has no type for
        typeof(SyntheticBlockContainer) // lives only while a template is being parsed
    ];

    [Fact]
    public void EveryMarkerMakesTheTripToJavaAndBack()
    {
        var markers = typeof(Marker).Assembly.GetTypes()
            .Where(t => typeof(Marker).IsAssignableFrom(t) && t is { IsAbstract: false, IsInterface: false })
            // a tree that is also a marker is sent as the tree it is
            .Where(t => !NeverSent.Contains(t) && !typeof(J).IsAssignableFrom(t))
            .OrderBy(t => t.FullName)
            .Select(Sample)
            .ToList();
        Assert.NotEmpty(markers);

        var returned = OnJava(With(markers.ToArray()), resend: false);

        var seenByJava = Report(returned, markers).Split(',');
        var failures = new List<string>();
        for (var i = 0; i < markers.Count; i++)
        {
            var sent = markers[i];
            var name = sent.GetType().Name;
            var javaType = RpcSendQueue.ToJavaTypeName(sent.GetType());
            if (i >= seenByJava.Length || seenByJava[i] != javaType)
            {
                failures.Add($"{name} reached Java as {(i < seenByJava.Length ? seenByJava[i] : "nothing")} rather than {javaType}");
            }

            var back = returned.Markers.MarkerList.FirstOrDefault(m => m.Id == sent.Id);
            if (back?.GetType() != sent.GetType())
            {
                failures.Add($"{name} came back as {back?.GetType().Name ?? "nothing"}");
            }
            else
            {
                var differences = new List<string>();
                Compare(sent, back, name, differences);
                if (differences.Count > 0)
                {
                    failures.Add($"{name} came back with a different {string.Join(", ", differences.Distinct())}");
                }
            }
        }
        Assert.True(failures.Count == 0, string.Join("\n", failures));
    }

    /// <summary>
    /// A marker of a type Java does not have is carried there as a generic marker. Java cannot
    /// say what it stands for when it sends it back, so the one still held here is kept.
    /// </summary>
    [Theory]
    [InlineData(false)]
    [InlineData(true)]
    public void AMarkerJavaHasNoTypeForIsCarriedAsAGenericOne(bool resend)
    {
        var sent = new LocalMarker(Guid.NewGuid(), "known only here");

        var returned = OnJava(With(sent), resend);

        Assert.Equal(RpcSendQueue.GenericMarker, Report(returned, [sent]));
        var back = Assert.IsType<LocalMarker>(returned.Markers.MarkerList.Single(m => m.Id == sent.Id));
        Assert.Equal("known only here", back.Note);
    }

    /// <summary>
    /// A generic marker that is not held here keeps the fields it arrived with, and Java gets
    /// them back.
    /// </summary>
    [Fact]
    public void AGenericMarkerThatIsNotHeldHereKeepsItsFields()
    {
        var sent = new UnknownMarker(Guid.NewGuid(), RpcSendQueue.GenericMarker);
        sent.Data["note"] = JsonSerializer.SerializeToElement("known only there", RpcJson.Options);

        var returned = OnJava(With(sent), resend: true);

        Assert.Equal(RpcSendQueue.GenericMarker, Report(returned, [sent]));
        var back = Assert.IsType<UnknownMarker>(returned.Markers.MarkerList.Single(m => m.Id == sent.Id));
        Assert.NotSame(sent, back);
        Assert.Equal(RpcSendQueue.GenericMarker, back.JavaType);
        Assert.Equal("known only there", Assert.Single(back.Data, entry => entry.Key == "note").Value.GetString());
    }

    /// <summary>
    /// A marker of a Java type this side has no class for is sent back as that type, with the
    /// fields it came with, rather than as a generic marker that would replace it there.
    /// </summary>
    [Fact]
    public void AJavaMarkerWithNoTypeHereIsSentBackAsItCame()
    {
        var id = Guid.NewGuid();
        var message = new RpcObjectData
        {
            State = RpcObjectData.ObjectState.ADD,
            ValueType = "org.openrewrite.marker.GitProvenance",
            Value = JsonSerializer.SerializeToElement(
                new Dictionary<string, object> { ["@ref"] = 1, ["id"] = id, ["branch"] = "main" }, RpcJson.Options)
        };
        var received = Assert.IsType<UnknownMarker>(
            new RpcReceiveQueue([message], new Dictionary<int, object>(), sourceFileType: null).Receive<Marker>(null));

        var sent = new List<RpcObjectData>();
        var queue = new RpcSendQueue(8, sent.AddRange, new RpcRefs(), null, false);
        queue.Send<Marker>(received, null, null);
        queue.Flush();

        var added = Assert.Single(sent);
        Assert.Equal("org.openrewrite.marker.GitProvenance", added.ValueType);
        var value = JsonSerializer.SerializeToElement(added.Value, RpcJson.Options);
        Assert.Equal(id, value.GetProperty("id").GetGuid());
        Assert.Equal("main", value.GetProperty("branch").GetString());
        Assert.False(value.TryGetProperty("@ref", out _));
    }

    private sealed class LocalMarker(Guid id, string note) : Marker
    {
        public Guid Id { get; } = id;
        public string Note { get; } = note;
    }

    private static CompilationUnit With(params Marker[] markers)
    {
        var cu = new CSharpParser().Parse("class C { }", sourcePath: "C.cs");
        return cu.WithMarkers(new Markers(Guid.NewGuid(), markers));
    }

    internal static J OnJava(CompilationUnit cu, bool resend, bool poison = false)
    {
        var server = RewriteRpcServer.Current!;
        server.StoreLocalObject(cu.Id.ToString(), cu);
        var ctxId = Guid.NewGuid().ToString();
        server.StoreLocalObject(ctxId, new ExecutionContext());
        return (J)server.VisitOnRemote("org.openrewrite.csharp.rpc.MarkerProbe",
            cu.Id.ToString(), "org.openrewrite.csharp.tree.Cs$CompilationUnit", ctxId,
            new Dictionary<string, object?> { ["resend"] = resend, ["poison"] = poison });
    }

    /// <summary>
    /// The types the markers arrived as on Java, which the probe reports in the one marker that was not sent.
    /// </summary>
    private static string Report(J returned, IList<Marker> sent) =>
        returned.Markers.MarkerList.OfType<SearchResult>().Single(m => sent.All(s => s.Id != m.Id)).Description!;

    private static Marker Sample(Type type)
    {
        try
        {
            return (Marker)SampleValue(type, [])!;
        }
        catch (Exception e)
        {
            throw new InvalidOperationException($"Could not make a {type.Name} to send: {e.Message}", e);
        }
    }

    private static object? SampleValue(Type type, HashSet<Type> making)
    {
        if (Nullable.GetUnderlyingType(type) is { } underlying) return SampleValue(underlying, making);
        if (type == typeof(Guid)) return Guid.NewGuid();
        if (type == typeof(string)) return "sample";
        if (type == typeof(bool)) return true;
        if (type == typeof(int)) return 7;
        if (type == typeof(long)) return 7L;
        if (type == typeof(Space)) return Space.SingleSpace;
        if (type == typeof(Marker)) return new SearchResult(Guid.NewGuid(), "sample");
        if (type.IsEnum) return Enum.GetValues(type).Cast<object>().Last();
        if (type.IsGenericType && type.GetGenericArguments() is [var element] &&
            type.IsAssignableFrom(typeof(List<>).MakeGenericType(element)))
        {
            var list = (IList)Activator.CreateInstance(typeof(List<>).MakeGenericType(element))!;
            list.Add(SampleValue(element, making));
            return list;
        }
        if (type.IsGenericType && type.GetGenericArguments() is [var key, var value] &&
            type.IsAssignableFrom(typeof(Dictionary<,>).MakeGenericType(key, value)))
        {
            var dictionary = (IDictionary)Activator.CreateInstance(typeof(Dictionary<,>).MakeGenericType(key, value))!;
            dictionary.Add(SampleValue(key, making)!, SampleValue(value, making));
            return dictionary;
        }
        if (type.IsClass && type.GetConstructors().Length > 0)
        {
            // a type that holds its own kind gets no deeper than one of them
            if (!making.Add(type)) return null;
            var constructor = type.GetConstructors().OrderByDescending(c => c.GetParameters().Length).First();
            var sample = constructor.Invoke(constructor.GetParameters().Select(p => SampleValue(p.ParameterType, making)).ToArray());
            making.Remove(type);
            return sample;
        }
        throw new InvalidOperationException($"No sample value for {type}; teach this test one");
    }

    private static void Compare(object? sent, object? back, string path, List<string> differences)
    {
        if (sent is null || back is null)
        {
            if (!ReferenceEquals(sent, back)) differences.Add(path);
        }
        else if (sent is string or ValueType)
        {
            if (!sent.Equals(back)) differences.Add(path);
        }
        else if (path.Count(c => c == '.') > 12)
        {
            differences.Add(path + " (too deep to compare)");
        }
        else if (sent is IDictionary entries)
        {
            Compare(entries.Keys, ((IDictionary)back).Keys, path, differences);
            Compare(entries.Values, ((IDictionary)back).Values, path, differences);
        }
        else if (sent is IEnumerable items)
        {
            var sentItems = items.Cast<object?>().ToList();
            var backItems = ((IEnumerable)back).Cast<object?>().ToList();
            if (sentItems.Count != backItems.Count)
            {
                differences.Add(path);
                return;
            }
            for (var i = 0; i < sentItems.Count; i++)
            {
                Compare(sentItems[i], backItems[i], path, differences);
            }
        }
        else
        {
            foreach (var property in sent.GetType().GetProperties(BindingFlags.Public | BindingFlags.Instance)
                         .Where(p => p.GetIndexParameters().Length == 0))
            {
                Compare(property.GetValue(sent), property.GetValue(back), path + "." + property.Name, differences);
            }
        }
    }
}
