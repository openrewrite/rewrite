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

public sealed class CSharpLanguageVersion(Guid id, string version, IList<CSharpLanguageVersion.TargetFrameworkVersion>? targetFrameworkVersions = null, bool isTargetFrameworkCoverageComplete = false) : Marker, IRpcCodec<CSharpLanguageVersion>, IEquatable<CSharpLanguageVersion>
{
    public Guid Id { get; } = id;
    public string Version { get; } = version;
    public IList<TargetFrameworkVersion> TargetFrameworkVersions { get; } = targetFrameworkVersions ?? [];
    public bool IsTargetFrameworkCoverageComplete { get; } = isTargetFrameworkCoverageComplete;

    public CSharpLanguageVersion WithId(Guid id) => id == Id ? this : new(id, Version, TargetFrameworkVersions, IsTargetFrameworkCoverageComplete);
    public CSharpLanguageVersion WithVersion(string version) => version == Version ? this : new(Id, version, TargetFrameworkVersions, IsTargetFrameworkCoverageComplete);

    public CSharpLanguageVersion WithTargetFrameworkVersions(IList<TargetFrameworkVersion> versions) =>
        ReferenceEquals(versions, TargetFrameworkVersions) ? this : new(Id, Version, versions, IsTargetFrameworkCoverageComplete);

    public CSharpLanguageVersion WithTargetFrameworkCoverageComplete(bool complete) =>
        complete == IsTargetFrameworkCoverageComplete ? this : new(Id, Version, TargetFrameworkVersions, complete);

    public void RpcSend(CSharpLanguageVersion after, RpcSendQueue q)
    {
        q.GetAndSend(after, m => m.Id);
        q.GetAndSend(after, m => m.Version);
        q.GetAndSendList(after, m => m.TargetFrameworkVersions, v => v.TargetFramework, null);
        q.GetAndSend(after, m => m.IsTargetFrameworkCoverageComplete);
    }

    public CSharpLanguageVersion RpcReceive(CSharpLanguageVersion before, RpcReceiveQueue q) => before
        .WithId(q.ReceiveAndGet<Guid, string>(before.Id, Guid.Parse))
        .WithVersion(q.Receive(before.Version)!)
        .WithTargetFrameworkVersions(q.ReceiveList(before.TargetFrameworkVersions, null)!)
        .WithTargetFrameworkCoverageComplete(q.Receive(before.IsTargetFrameworkCoverageComplete));

    public bool Equals(CSharpLanguageVersion? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as CSharpLanguageVersion);
    public override int GetHashCode() => Id.GetHashCode();

    public sealed class TargetFrameworkVersion(string targetFramework, string version) : IRpcCodec<TargetFrameworkVersion>
    {
        public string TargetFramework { get; } = targetFramework;
        public string Version { get; } = version;

        public void RpcSend(TargetFrameworkVersion after, RpcSendQueue q)
        {
            q.GetAndSend(after, v => v.TargetFramework);
            q.GetAndSend(after, v => v.Version);
        }

        public TargetFrameworkVersion RpcReceive(TargetFrameworkVersion before, RpcReceiveQueue q) =>
            new(q.Receive(before.TargetFramework)!, q.Receive(before.Version)!);
    }
}
