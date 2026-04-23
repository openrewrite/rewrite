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
/// Metadata about a .NET project (.csproj) extracted from MSBuild evaluation.
/// Attached as a marker to the Xml.Document representing the .csproj file in the LST.
/// </summary>
public sealed class MSBuildProject : Marker, IRpcCodec<MSBuildProject>, IEquatable<MSBuildProject>
{
    public Guid Id { get; }
    public string? Sdk { get; }
    public IDictionary<string, PropertyValue> Properties { get; }
    public IList<PackageSource> PackageSources { get; }
    public IList<TargetFramework> TargetFrameworks { get; }

    public MSBuildProject(
        Guid id,
        string? sdk = null,
        IDictionary<string, PropertyValue>? properties = null,
        IList<PackageSource>? packageSources = null,
        IList<TargetFramework>? targetFrameworks = null)
    {
        Id = id;
        Sdk = sdk;
        Properties = properties ?? new Dictionary<string, PropertyValue>();
        PackageSources = packageSources ?? [];
        TargetFrameworks = targetFrameworks ?? [];
    }

    public MSBuildProject WithId(Guid id) =>
        id == Id ? this : new(id, Sdk, Properties, PackageSources, TargetFrameworks);

    public MSBuildProject WithSdk(string? sdk) =>
        sdk == Sdk ? this : new(Id, sdk, Properties, PackageSources, TargetFrameworks);

    public MSBuildProject WithProperties(IDictionary<string, PropertyValue> properties) =>
        ReferenceEquals(properties, Properties) ? this : new(Id, Sdk, properties, PackageSources, TargetFrameworks);

    public MSBuildProject WithPackageSources(IList<PackageSource> packageSources) =>
        ReferenceEquals(packageSources, PackageSources) ? this : new(Id, Sdk, Properties, packageSources, TargetFrameworks);

    public MSBuildProject WithTargetFrameworks(IList<TargetFramework> targetFrameworks) =>
        ReferenceEquals(targetFrameworks, TargetFrameworks) ? this : new(Id, Sdk, Properties, PackageSources, targetFrameworks);

    public void RpcSend(MSBuildProject after, RpcSendQueue q)
    {
        q.GetAndSend(after, m => m.Id);
        q.GetAndSend(after, m => m.Sdk);
        // Send map as parallel lists: keys then values
        q.GetAndSendList(after, m => (IList<string>)new List<string>(m.Properties.Keys),
            k => k,
            k => q.GetAndSend(k, x => x));
        q.GetAndSendList(after, m => (IList<PropertyValue>)new List<PropertyValue>(m.Properties.Values),
            v => v.Value,
            v => v.RpcSend(v, q));
        q.GetAndSendList(after, m => m.PackageSources,
            ps => ps.Key,
            ps => ps.RpcSend(ps, q));
        q.GetAndSendList(after, m => m.TargetFrameworks,
            tf => tf.TargetFrameworkMoniker,
            tf => tf.RpcSend(tf, q));
    }

    public MSBuildProject RpcReceive(MSBuildProject before, RpcReceiveQueue q)
    {
        var id = q.ReceiveAndGet<Guid, string>(before.Id, Guid.Parse);
        var sdk = q.Receive(before.Sdk);
        // Receive parallel lists and zip into dictionary
        var beforeProps = before.Properties ?? new Dictionary<string, PropertyValue>();
        var keys = q.ReceiveList((IList<string>)new List<string>(beforeProps.Keys),
            k => q.ReceiveAndGet<string, string>(k, x => x)!);
        var values = q.ReceiveList((IList<PropertyValue>)new List<PropertyValue>(beforeProps.Values),
            v => v.RpcReceive(v, q));
        var props = new Dictionary<string, PropertyValue>();
        if (keys != null && values != null)
        {
            for (int i = 0; i < keys.Count; i++)
                props[keys[i]] = values[i];
        }
        return before
            .WithId(id)
            .WithSdk(sdk)
            .WithProperties(props)
            .WithPackageSources(q.ReceiveList(before.PackageSources,
                ps => ps.RpcReceive(ps, q))!)
            .WithTargetFrameworks(q.ReceiveList(before.TargetFrameworks,
                tf => tf.RpcReceive(tf, q))!);
    }

    public bool Equals(MSBuildProject? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as MSBuildProject);
    public override int GetHashCode() => Id.GetHashCode();
}

public sealed class TargetFramework : IRpcCodec<TargetFramework>
{
    public string TargetFrameworkMoniker { get; }
    public IList<PackageReference> PackageReferences { get; }
    public IList<ResolvedPackage> ResolvedPackages { get; }
    public IList<ProjectReference> ProjectReferences { get; }

    public TargetFramework(
        string targetFrameworkMoniker,
        IList<PackageReference>? packageReferences = null,
        IList<ResolvedPackage>? resolvedPackages = null,
        IList<ProjectReference>? projectReferences = null)
    {
        TargetFrameworkMoniker = targetFrameworkMoniker;
        PackageReferences = packageReferences ?? [];
        ResolvedPackages = resolvedPackages ?? [];
        ProjectReferences = projectReferences ?? [];
    }

    public TargetFramework WithTargetFrameworkMoniker(string targetFrameworkMoniker) =>
        targetFrameworkMoniker == TargetFrameworkMoniker ? this : new(targetFrameworkMoniker, PackageReferences, ResolvedPackages, ProjectReferences);

    public TargetFramework WithPackageReferences(IList<PackageReference> packageReferences) =>
        ReferenceEquals(packageReferences, PackageReferences) ? this : new(TargetFrameworkMoniker, packageReferences, ResolvedPackages, ProjectReferences);

    public TargetFramework WithResolvedPackages(IList<ResolvedPackage> resolvedPackages) =>
        ReferenceEquals(resolvedPackages, ResolvedPackages) ? this : new(TargetFrameworkMoniker, PackageReferences, resolvedPackages, ProjectReferences);

    public TargetFramework WithProjectReferences(IList<ProjectReference> projectReferences) =>
        ReferenceEquals(projectReferences, ProjectReferences) ? this : new(TargetFrameworkMoniker, PackageReferences, ResolvedPackages, projectReferences);

    public void RpcSend(TargetFramework after, RpcSendQueue q)
    {
        q.GetAndSend(after, tf => tf.TargetFrameworkMoniker);
        q.GetAndSendList(after, tf => tf.PackageReferences,
            pr => pr.Include,
            pr => pr.RpcSend(pr, q));
        q.GetAndSendListAsRef(after, tf => tf.ResolvedPackages,
            rp => (object)(rp.Name + "@" + rp.ResolvedVersion),
            rp => rp.RpcSend(rp, q));
        q.GetAndSendList(after, tf => tf.ProjectReferences,
            pr => pr.Include,
            pr => pr.RpcSend(pr, q));
    }

    public TargetFramework RpcReceive(TargetFramework before, RpcReceiveQueue q)
    {
        return before
            .WithTargetFrameworkMoniker(q.Receive(before.TargetFrameworkMoniker)!)
            .WithPackageReferences(q.ReceiveList(before.PackageReferences,
                pr => pr.RpcReceive(pr, q))!)
            .WithResolvedPackages(q.ReceiveList(before.ResolvedPackages,
                rp => rp.RpcReceive(rp, q))!)
            .WithProjectReferences(q.ReceiveList(before.ProjectReferences,
                pr => pr.RpcReceive(pr, q))!);
    }
}

public sealed class PackageReference : IRpcCodec<PackageReference>
{
    public string Include { get; }
    public string? RequestedVersion { get; }
    public string? ResolvedVersion { get; }

    public PackageReference(string include, string? requestedVersion = null, string? resolvedVersion = null)
    {
        Include = include;
        RequestedVersion = requestedVersion;
        ResolvedVersion = resolvedVersion;
    }

    public PackageReference WithInclude(string include) =>
        include == Include ? this : new(include, RequestedVersion, ResolvedVersion);

    public PackageReference WithRequestedVersion(string? requestedVersion) =>
        requestedVersion == RequestedVersion ? this : new(Include, requestedVersion, ResolvedVersion);

    public PackageReference WithResolvedVersion(string? resolvedVersion) =>
        resolvedVersion == ResolvedVersion ? this : new(Include, RequestedVersion, resolvedVersion);

    public void RpcSend(PackageReference after, RpcSendQueue q)
    {
        q.GetAndSend(after, pr => pr.Include);
        q.GetAndSend(after, pr => pr.RequestedVersion);
        q.GetAndSend(after, pr => pr.ResolvedVersion);
    }

    public PackageReference RpcReceive(PackageReference before, RpcReceiveQueue q)
    {
        return before
            .WithInclude(q.Receive(before.Include)!)
            .WithRequestedVersion(q.Receive(before.RequestedVersion))
            .WithResolvedVersion(q.Receive(before.ResolvedVersion));
    }
}

public sealed class ResolvedPackage : IRpcCodec<ResolvedPackage>
{
    public string Name { get; }
    public string ResolvedVersion { get; }
    public IList<ResolvedPackage> Dependencies { get; }
    public int Depth { get; }

    public ResolvedPackage(string name, string resolvedVersion, IList<ResolvedPackage>? dependencies = null, int depth = 0)
    {
        Name = name;
        ResolvedVersion = resolvedVersion;
        Dependencies = dependencies ?? [];
        Depth = depth;
    }

    public ResolvedPackage WithName(string name) =>
        name == Name ? this : new(name, ResolvedVersion, Dependencies, Depth);

    public ResolvedPackage WithResolvedVersion(string resolvedVersion) =>
        resolvedVersion == ResolvedVersion ? this : new(Name, resolvedVersion, Dependencies, Depth);

    public ResolvedPackage WithDependencies(IList<ResolvedPackage> dependencies) =>
        ReferenceEquals(dependencies, Dependencies) ? this : new(Name, ResolvedVersion, dependencies, Depth);

    public ResolvedPackage WithDepth(int depth) =>
        depth == Depth ? this : new(Name, ResolvedVersion, Dependencies, depth);

    public void RpcSend(ResolvedPackage after, RpcSendQueue q)
    {
        q.GetAndSend(after, rp => rp.Name);
        q.GetAndSend(after, rp => rp.ResolvedVersion);
        q.GetAndSendListAsRef(after, rp => rp.Dependencies,
            dep => (object)(dep.Name + "@" + dep.ResolvedVersion),
            dep => dep.RpcSend(dep, q));
        q.GetAndSend(after, rp => rp.Depth);
    }

    public ResolvedPackage RpcReceive(ResolvedPackage before, RpcReceiveQueue q)
    {
        return before
            .WithName(q.Receive(before.Name)!)
            .WithResolvedVersion(q.Receive(before.ResolvedVersion)!)
            .WithDependencies(q.ReceiveList(before.Dependencies,
                dep => dep.RpcReceive(dep, q))!)
            .WithDepth(q.ReceiveAndGet<int, int>(before.Depth, x => x));
    }
}

public sealed class ProjectReference : IRpcCodec<ProjectReference>
{
    public string Include { get; }

    public ProjectReference(string include)
    {
        Include = include;
    }

    public ProjectReference WithInclude(string include) =>
        include == Include ? this : new(include);

    public void RpcSend(ProjectReference after, RpcSendQueue q)
    {
        q.GetAndSend(after, pr => pr.Include);
    }

    public ProjectReference RpcReceive(ProjectReference before, RpcReceiveQueue q)
    {
        return before.WithInclude(q.Receive(before.Include)!);
    }
}

public sealed class PropertyValue : IRpcCodec<PropertyValue>
{
    public string Value { get; }
    public string? DefinedIn { get; }

    public PropertyValue(string value, string? definedIn = null)
    {
        Value = value;
        DefinedIn = definedIn;
    }

    public PropertyValue WithValue(string value) =>
        value == Value ? this : new(value, DefinedIn);

    public PropertyValue WithDefinedIn(string? definedIn) =>
        definedIn == DefinedIn ? this : new(Value, definedIn);

    public void RpcSend(PropertyValue after, RpcSendQueue q)
    {
        q.GetAndSend(after, pv => pv.Value);
        q.GetAndSend(after, pv => pv.DefinedIn);
    }

    public PropertyValue RpcReceive(PropertyValue before, RpcReceiveQueue q)
    {
        return before
            .WithValue(q.Receive(before.Value)!)
            .WithDefinedIn(q.Receive(before.DefinedIn));
    }
}

public sealed class PackageSource : IRpcCodec<PackageSource>
{
    public string Key { get; }
    public string Url { get; }

    public PackageSource(string key, string url)
    {
        Key = key;
        Url = url;
    }

    public PackageSource WithKey(string key) =>
        key == Key ? this : new(key, Url);

    public PackageSource WithUrl(string url) =>
        url == Url ? this : new(Key, url);

    public void RpcSend(PackageSource after, RpcSendQueue q)
    {
        q.GetAndSend(after, ps => ps.Key);
        q.GetAndSend(after, ps => ps.Url);
    }

    public PackageSource RpcReceive(PackageSource before, RpcReceiveQueue q)
    {
        return before
            .WithKey(q.Receive(before.Key)!)
            .WithUrl(q.Receive(before.Url)!);
    }
}
