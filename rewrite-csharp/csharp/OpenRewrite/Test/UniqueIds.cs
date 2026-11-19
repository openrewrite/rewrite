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
using System.Text;
using OpenRewrite.Core;

namespace OpenRewrite.Test;

/// <summary>
/// Fails a recipe run that leaves two nodes with the same id in a source file, as a recipe does that puts one node in
/// two places. Recipes and tools find nodes by id, such as to report or patch one, and find the wrong one.
/// </summary>
internal static class UniqueIds
{
    private const int MaxPerFile = 5;

    public static void AssertUnique(Recipe recipe, IEnumerable<Result> results)
    {
        var found = new StringBuilder();
        foreach (var (before, after) in results)
        {
            if (after == null)
            {
                continue;
            }
            var alreadyShared = before == null ? [] : Duplicates(before).Select(d => d.Id).ToHashSet();
            var duplicates = Duplicates(after).Where(d => !alreadyShared.Contains(d.Id)).Take(MaxPerFile).ToList();
            if (duplicates.Count > 0)
            {
                found.Append("  ").Append(after.SourcePath).Append(":\n");
                duplicates.ForEach(d => found.Append("    ").Append(d.Description).Append('\n'));
            }
        }

        if (found.Length > 0)
        {
            Assert.Fail(
                "Give every node of a copy a new id, such as with a visitor whose PreVisit returns " +
                "tree.WithId(Tree.RandomId()).\n" +
                $"The output of {recipe.Name} holds nodes that share an id:\n" +
                found +
                "Recipes and tools find a node by its id, such as to report or patch it, and find the wrong one. " +
                "A recipe that can't help it opts out with " +
                "spec.SetValidations(new Validations { UniqueIds = false }).");
        }
    }

    private static IEnumerable<(Guid Id, string Description)> Duplicates(SourceFile source)
    {
        var seen = new Dictionary<Guid, Tree>();
        var pending = new Stack<object>([source]);
        while (pending.Count > 0)
        {
            var node = pending.Pop();
            if (node is Tree tree && !seen.TryAdd(tree.Id, tree))
            {
                yield return (tree.Id, ReferenceEquals(seen[tree.Id], tree)
                    ? $"{tree.GetType().Name} {tree.Id} appears twice"
                    : $"{tree.GetType().Name} and {seen[tree.Id].GetType().Name} share the id {tree.Id}");
                continue;
            }
            foreach (var child in AddedMarkersCheck.ChildrenOf(node))
            {
                pending.Push(child);
            }
        }
    }
}
