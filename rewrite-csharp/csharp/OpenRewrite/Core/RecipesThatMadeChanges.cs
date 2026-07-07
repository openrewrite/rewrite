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
using System.Runtime.CompilerServices;
using OpenRewrite.Core.Rpc;

namespace OpenRewrite.Core;

/// <summary>
/// Marker that records which recipes made changes to a source file.
/// C# stores recipe payloads opaquely; the RPC codec interns repeated payloads.
/// </summary>
public sealed class RecipesThatMadeChanges(Guid id, IList<IList<object?>> recipes)
    : Marker, IRpcCodec<RecipesThatMadeChanges>, IEquatable<RecipesThatMadeChanges>
{
    public Guid Id { get; } = id;
    public IList<IList<object?>> Recipes { get; } = recipes;

    public void RpcSend(RecipesThatMadeChanges after, RpcSendQueue q)
    {
        q.GetAndSend(after, m => m.Id);
        q.GetAndSendListAsRef(after, m => WireForm.From(m).RecipeTable,
            recipe => RuntimeHelpers.GetHashCode(recipe), null);
        q.GetAndSend(after, m => WireForm.From(m).Stacks);
    }

    public RecipesThatMadeChanges RpcReceive(RecipesThatMadeChanges before, RpcReceiveQueue q)
    {
        var id = q.ReceiveAndGet<Guid, string>(before.Id, Guid.Parse);
        var beforeWire = WireForm.From(before);
        var recipeTable = q.ReceiveList(beforeWire.RecipeTable, null) ?? [];
        var stacksValue = q.Receive<object>(beforeWire.Stacks);
        var stacks = ToStacks(stacksValue ?? new List<List<int>>());

        var recipes = new List<IList<object?>>(stacks.Count);
        foreach (var stack in stacks)
        {
            var recipeStack = new List<object?>(stack.Count);
            foreach (var recipeIndex in stack)
            {
                recipeStack.Add(recipeIndex < 0 ? null : recipeTable[recipeIndex]);
            }
            recipes.Add(recipeStack);
        }
        return new RecipesThatMadeChanges(id, recipes);
    }

    public bool Equals(RecipesThatMadeChanges? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as RecipesThatMadeChanges);
    public override int GetHashCode() => Id.GetHashCode();

    private sealed class WireForm
    {
        public List<object?> RecipeTable { get; } = [];
        public List<List<int>> Stacks { get; } = [];
        private readonly Dictionary<object, int> _recipeIds = new(ReferenceEqualityComparer.Instance);

        public static WireForm From(RecipesThatMadeChanges? marker)
        {
            var wire = new WireForm();
            if (marker?.Recipes == null)
            {
                return wire;
            }

            foreach (var stack in marker.Recipes)
            {
                var stackIds = new List<int>(stack.Count);
                foreach (var recipe in stack)
                {
                    if (recipe == null)
                    {
                        stackIds.Add(-1);
                        continue;
                    }

                    if (!wire._recipeIds.TryGetValue(recipe, out var recipeId))
                    {
                        recipeId = wire.RecipeTable.Count;
                        wire._recipeIds[recipe] = recipeId;
                        wire.RecipeTable.Add(recipe);
                    }
                    stackIds.Add(recipeId);
                }
                wire.Stacks.Add(stackIds);
            }
            return wire;
        }
    }

    private static List<List<int>> ToStacks(object value)
    {
        if (value is List<List<int>> typed)
        {
            return typed;
        }

        if (value is IEnumerable<object?> outer)
        {
            return outer.Select(stack => ((IEnumerable<object?>)stack!).Select(Convert.ToInt32).ToList()).ToList();
        }

        return [];
    }
}
