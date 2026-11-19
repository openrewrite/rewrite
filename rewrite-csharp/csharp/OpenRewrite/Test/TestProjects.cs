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
using System.Collections.Immutable;
using Microsoft.CodeAnalysis;
using Microsoft.CodeAnalysis.CSharp;
using OpenRewrite.Core;
using OpenRewrite.CSharp;

namespace OpenRewrite.Test;

/// <summary>
/// The projects the sources of a test belong to, by <see cref="SourceSpec.InProject"/> and
/// <see cref="SourceSpec.TargetFrameworks"/>, each compiled on its own against the projects it references. The sources
/// in no project compile together.
/// </summary>
internal sealed class TestProjects
{
    private sealed class Project(DotNetProject marker)
    {
        public DotNetProject Marker { get; set; } = marker;
        public bool FrameworksStated { get; set; }
        public HashSet<string> References { get; } = [];
    }

    private readonly Dictionary<string, Project> _byName = new();
    private readonly Dictionary<ImmutableArray<string>, Project> _unnamed = new(new SequenceComparer());
    private readonly Dictionary<SourceSpec, Project> _bySpec = new(ReferenceEqualityComparer.Instance);

    public TestProjects(IEnumerable<SourceSpec> specs)
    {
        var all = specs.ToList();
        foreach (var spec in all.Where(s => s.ProjectName != null))
        {
            _bySpec[spec] = Named(spec);
        }
        foreach (var spec in all.Where(s => s.ProjectName == null))
        {
            if (spec.ProjectTargetFrameworks is { } frameworks)
            {
                _bySpec[spec] = Unnamed(frameworks);
            }
        }
        foreach (var project in _byName.Values)
        {
            foreach (var reference in project.References.Where(r => !_byName.ContainsKey(r)))
            {
                throw new ArgumentException(
                    $"{project.Marker.ProjectName} references {reference}, which no source of the test is in.");
            }
        }
    }

    private Project Named(SourceSpec spec)
    {
        var name = spec.ProjectName!;
        if (!_byName.TryGetValue(name, out var project))
        {
            project = new Project(Marker(name, []));
            _byName[name] = project;
        }
        project.References.UnionWith(spec.ProjectReferences);
        if (spec.ProjectTargetFrameworks is { } frameworks)
        {
            if (project.FrameworksStated && !project.Marker.TargetFrameworks.SequenceEqual(frameworks))
            {
                throw new ArgumentException(
                    $"The sources in {name} target {string.Join(";", project.Marker.TargetFrameworks)} and " +
                    $"{string.Join(";", frameworks)}, and a project targets the same frameworks for all its sources.");
            }
            project.Marker = project.Marker.WithTargetFrameworks([..frameworks]);
            project.FrameworksStated = true;
        }
        return project;
    }

    private Project Unnamed(ImmutableArray<string> frameworks)
    {
        if (!_unnamed.TryGetValue(frameworks, out var project))
        {
            var name = Enumerable.Range(1, int.MaxValue)
                .Select(n => n == 1 ? "Test" : $"Test{n}")
                .First(n => !_byName.ContainsKey(n) && _unnamed.Values.All(p => p.Marker.ProjectName != n));
            project = new Project(Marker(name, frameworks));
            _unnamed[frameworks] = project;
        }
        return project;
    }

    private static DotNetProject Marker(string name, IEnumerable<string> frameworks) =>
        new(Tree.RandomId(), name, [..frameworks], "Microsoft.NET.Sdk");

    /// <summary>The project marker of the spec's source, or null for a source in no project.</summary>
    public DotNetProject? Of(SourceSpec spec) => _bySpec.TryGetValue(spec, out var project) ? project.Marker : null;

    /// <summary>The project a source compiles in, by its marker, or null for the sources in no project.</summary>
    public DotNetProject? Of(SourceFile source) =>
        source.Markers.FindFirst<DotNetProject>() is { } marker &&
        Projects.FirstOrDefault(p => p.Marker.Id == marker.Id) is { } project
            ? project.Marker
            : null;

    private IEnumerable<Project> Projects => _byName.Values.Concat(_unnamed.Values);

    /// <summary>
    /// Compiles the syntax trees of each project after the projects it references, and those of no project together.
    /// </summary>
    public Dictionary<SyntaxTree, CSharpCompilation> Compile(
        IEnumerable<(DotNetProject? Project, SyntaxTree Tree)> trees, CompileSetup setup)
    {
        var byProject = trees.GroupBy(t => t.Project?.Id ?? Guid.Empty)
            .ToDictionary(g => g.Key, g => g.Select(t => t.Tree).ToList());
        var compilations = new Dictionary<Guid, CSharpCompilation>();
        var compiling = new HashSet<Guid>();

        CSharpCompilation CompileProject(Guid id)
        {
            if (compilations.TryGetValue(id, out var done))
            {
                return done;
            }
            var project = Projects.FirstOrDefault(p => p.Marker.Id == id);
            if (!compiling.Add(id))
            {
                throw new ArgumentException(
                    $"{project?.Marker.ProjectName} references itself through its project references.");
            }
            var references = (project?.References ?? []).Select(name => _byName[name])
                .Select(referenced => CompileProject(referenced.Marker.Id).ToMetadataReference());
            var compilation = CSharpCompilation.Create(
                    project == null ? "TestCompilation" : Path.GetFileNameWithoutExtension(project.Marker.ProjectName))
                .WithOptions(setup.Options)
                .AddReferences(setup.References)
                .AddReferences(references)
                .AddSyntaxTrees(byProject.GetValueOrDefault(id) ?? []);
            compilations[id] = compilation;
            return compilation;
        }

        var result = new Dictionary<SyntaxTree, CSharpCompilation>();
        foreach (var (id, projectTrees) in byProject)
        {
            var compilation = CompileProject(id);
            foreach (var tree in projectTrees)
            {
                result[tree] = compilation;
            }
        }
        return result;
    }

    private sealed class SequenceComparer : IEqualityComparer<ImmutableArray<string>>
    {
        public bool Equals(ImmutableArray<string> x, ImmutableArray<string> y) => x.SequenceEqual(y);
        public int GetHashCode(ImmutableArray<string> obj) => string.Join(";", obj).GetHashCode();
    }
}

/// <summary>What the sources of a test compile against, and with which options.</summary>
internal sealed record CompileSetup(
    ImmutableArray<MetadataReference> References,
    CSharpCompilationOptions Options,
    CSharpParseOptions ParseOptions)
{
    public SyntaxTree Parse(string text, string path) => CSharpSyntaxTree.ParseText(text, ParseOptions, path);
}
