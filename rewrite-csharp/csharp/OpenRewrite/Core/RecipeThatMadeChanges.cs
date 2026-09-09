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
/// One frame of a <see cref="RecipesThatMadeChanges"/> stack: how the recipe is named, how it was
/// configured, and what it is worth. Field order mirrors Java's RecipeThatMadeChanges codec.
/// </summary>
public sealed class RecipeThatMadeChanges(
    string name,
    string? displayName,
    string? instanceName,
    object? options,
    long? estimatedEffortPerOccurrenceMillis)
    : IRpcCodec<RecipeThatMadeChanges>
{
    public string Name { get; } = name;
    public string? DisplayName { get; } = displayName;
    public string? InstanceName { get; } = instanceName;
    /// <summary>
    /// Configured option values. C# never interprets them, so they stay in whatever shape the wire
    /// delivered rather than being projected into a dictionary.
    /// </summary>
    public object? Options { get; } = options;
    public long? EstimatedEffortPerOccurrenceMillis { get; } = estimatedEffortPerOccurrenceMillis;

    public void RpcSend(RecipeThatMadeChanges after, RpcSendQueue q)
    {
        q.GetAndSend(after, r => r.Name);
        q.GetAndSend(after, r => r.DisplayName);
        q.GetAndSend(after, r => r.InstanceName);
        q.GetAndSend(after, r => r.Options);
        q.GetAndSend(after, r => r.EstimatedEffortPerOccurrenceMillis);
    }

    public RecipeThatMadeChanges RpcReceive(RecipeThatMadeChanges before, RpcReceiveQueue q)
    {
        var name = q.Receive(before.Name);
        var displayName = q.Receive(before.DisplayName);
        var instanceName = q.Receive(before.InstanceName);
        var options = q.Receive(before.Options);
        var effort = q.Receive(before.EstimatedEffortPerOccurrenceMillis);
        return new RecipeThatMadeChanges(name!, displayName, instanceName, options, effort);
    }
}
