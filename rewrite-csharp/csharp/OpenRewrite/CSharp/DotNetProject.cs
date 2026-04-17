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

namespace OpenRewrite.CSharp;

/// <summary>
/// Identifies the .NET project a source file belongs to.
/// Attached to every C# source file parsed from a project,
/// analogous to JavaProject for Java source files.
/// </summary>
public sealed class DotNetProject : Marker, IRpcCodec<DotNetProject>, IEquatable<DotNetProject>
{
    public Guid Id { get; }
    public string ProjectName { get; }
    public IList<string> TargetFrameworks { get; }
    public string? Sdk { get; }

    public DotNetProject(Guid id, string projectName, IList<string>? targetFrameworks = null, string? sdk = null)
    {
        Id = id;
        ProjectName = projectName;
        TargetFrameworks = targetFrameworks ?? [];
        Sdk = sdk;
    }

    public DotNetProject WithId(Guid id) =>
        id == Id ? this : new(id, ProjectName, TargetFrameworks, Sdk);

    public DotNetProject WithProjectName(string projectName) =>
        projectName == ProjectName ? this : new(Id, projectName, TargetFrameworks, Sdk);

    public DotNetProject WithTargetFrameworks(IList<string> targetFrameworks) =>
        ReferenceEquals(targetFrameworks, TargetFrameworks) ? this : new(Id, ProjectName, targetFrameworks, Sdk);

    public DotNetProject WithSdk(string? sdk) =>
        sdk == Sdk ? this : new(Id, ProjectName, TargetFrameworks, sdk);

    public void RpcSend(DotNetProject after, RpcSendQueue q)
    {
        q.GetAndSend(after, m => m.Id);
        q.GetAndSend(after, m => m.ProjectName);
        q.GetAndSendList(after, m => m.TargetFrameworks,
            s => s,
            s => q.GetAndSend(s, x => x));
        q.GetAndSend(after, m => m.Sdk);
    }

    public DotNetProject RpcReceive(DotNetProject before, RpcReceiveQueue q)
    {
        return before
            .WithId(q.ReceiveAndGet<Guid, string>(before.Id, Guid.Parse))
            .WithProjectName(q.Receive(before.ProjectName)!)
            .WithTargetFrameworks(q.ReceiveList(before.TargetFrameworks,
                s => q.ReceiveAndGet<string, string>(s, x => x)!) ?? [])
            .WithSdk(q.Receive(before.Sdk));
    }

    public bool Equals(DotNetProject? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as DotNetProject);
    public override int GetHashCode() => Id.GetHashCode();
}
