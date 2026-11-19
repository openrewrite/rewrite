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
using System.Collections.Concurrent;
using System.Reflection;
using OpenRewrite.Core;
using OpenRewrite.CSharp.Rpc;
using OpenRewrite.Java;
using OpenRewrite.Java.Rpc;

namespace OpenRewrite.Test;

/// <summary>
/// Fails a recipe run whose output doesn't survive a trip to the Java peer and back: Java has to print it as this side
/// does, and send back the nodes, ids and types this side sent it. Recipes of other languages, and Java recipes run
/// after the recipe, read what Java received.
/// </summary>
internal static class RpcRoundTrip
{
    private const int MaxReported = 5;

    private static readonly ConcurrentDictionary<Type, PropertyInfo[]> TypeProperties = new();

    public static void AssertSurvives(IEnumerable<Result> results)
    {
        var rpc = RewriteRpcServer.Current ?? throw new InvalidOperationException(
            "Validations.RpcRoundTrip sends the recipe's output to the Java peer, and no RPC connection is open.");
        foreach (var after in results.Select(r => r.After).OfType<SourceFile>())
        {
            var printed = AddedMarkersCheck.Print(after, MarkerPrinter.Default);
            var printedByJava = rpc.PrintOnRemote(after);
            if (printedByJava != printed)
            {
                Assert.Fail($"The Java peer printed the recipe's output differently from this side:\n" +
                            DiffUtils.UnifiedDiff(printed, printedByJava, after.SourcePath) + Hint);
            }

            var returned = rpc.ResendFromRemote(after);
            var mismatches = Mismatches(Nodes(after), Nodes(returned)).Take(MaxReported + 1).ToList();
            if (mismatches.Count > 0)
            {
                Assert.Fail(string.Join("\n", [
                    $"The recipe's output came back from the Java peer different from what this side sent, in " +
                    $"{after.SourcePath}:",
                    ..mismatches.Take(MaxReported),
                    ..mismatches.Count > MaxReported ? ["  ..."] : Array.Empty<string>()
                ]) + Hint);
            }
        }
    }

    private const string Hint =
        "\nA recipe that builds a node or type the RPC codecs don't carry, or carries differently, loses it for the " +
        "recipes run after it in Java or another language. A test whose recipe can't help it opts out with " +
        "spec.SetValidations(new Validations { RpcRoundTrip = false }).";

    private sealed record Node(Tree Tree, List<(string Property, JavaType Type)> Types)
    {
        public string Describe() => $"{Tree.GetType().Name} {Tree.Id}" +
                                    string.Concat(Types.Select(t => $" {t.Property}={Signature(t.Type)}"));
    }

    private static IEnumerable<string> Mismatches(List<Node> sent, List<Node> received)
    {
        for (var i = 0; i < Math.Max(sent.Count, received.Count); i++)
        {
            if (i >= sent.Count || i >= received.Count)
            {
                var extra = (i < sent.Count ? sent[i] : received[i]).Describe();
                yield return $"  it sent {sent.Count} nodes and received {received.Count}, the first extra one " +
                             $"being {extra}";
                yield break;
            }
            var (s, r) = (sent[i].Describe(), received[i].Describe());
            if (s != r)
            {
                yield return $"  sent     {s}\n  received {r}";
            }
        }
    }

    private static List<Node> Nodes(Tree root)
    {
        var nodes = new List<Node>();
        var pending = new Stack<object>([root]);
        while (pending.Count > 0)
        {
            var node = pending.Pop();
            if (node is Tree tree)
            {
                nodes.Add(new Node(tree, TypesOf(tree)));
            }
            var children = AddedMarkersCheck.ChildrenOf(node);
            for (var i = children.Count - 1; i >= 0; i--)
            {
                pending.Push(children[i]);
            }
        }
        return nodes;
    }

    private static List<(string, JavaType)> TypesOf(Tree tree) =>
        TypeProperties.GetOrAdd(tree.GetType(), type => type
                .GetProperties(BindingFlags.Public | BindingFlags.Instance)
                .Where(p => p.GetIndexParameters().Length == 0 && typeof(JavaType).IsAssignableFrom(p.PropertyType))
                .OrderBy(p => p.Name, StringComparer.Ordinal)
                .ToArray())
            .Select(p => (p.Name, p.GetValue(tree) as JavaType))
            .Where(t => t.Item2 != null)
            .Select(t => (t.Name, t.Item2!))
            .ToList();

    private static string Signature(JavaType type) => JavaSender.TypeSignature(type).ToString() ?? "";
}
