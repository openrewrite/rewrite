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

namespace OpenRewrite.Core;

/// <summary>
/// Records which recipe stacks changed a source file. C# holds the stacks without interpreting
/// them, so a marker served to this peer returns to the host intact.
/// </summary>
public sealed class RecipesThatMadeChanges(Guid id, IList<IList<RecipeThatMadeChanges>> recipes)
    : Marker, IRpcCodec<RecipesThatMadeChanges>, IEquatable<RecipesThatMadeChanges>
{
    public Guid Id { get; } = id;
    public IList<IList<RecipeThatMadeChanges>> Recipes { get; } = recipes;

    public void RpcSend(RecipesThatMadeChanges after, RpcSendQueue q)
    {
        q.GetAndSend(after, m => m.Id);
        // The stack key never travels; it only has to identify a stack within this process.
        q.GetAndSendList(after, m => m.Recipes, stack => string.Join('\0', stack.Select(r => r.Name)),
            stack => q.GetAndSendList(stack, s => s, r => (object)r.Name, null));
    }

    public RecipesThatMadeChanges RpcReceive(RecipesThatMadeChanges before, RpcReceiveQueue q)
    {
        var id = q.ReceiveAndGet<Guid, string>(before.Id, Guid.Parse);
        var recipes = q.ReceiveList(before.Recipes, stack => q.ReceiveList(stack, null)!);
        return new RecipesThatMadeChanges(id, recipes ?? []);
    }

    public bool Equals(RecipesThatMadeChanges? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as RecipesThatMadeChanges);
    public override int GetHashCode() => Id.GetHashCode();
}
