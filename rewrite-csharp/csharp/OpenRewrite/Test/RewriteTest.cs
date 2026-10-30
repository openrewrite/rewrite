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
using System.Collections.Immutable;
using JetBrains.Annotations;
using Microsoft.CodeAnalysis;
using Microsoft.CodeAnalysis.CSharp;
using Microsoft.CodeAnalysis.Testing;
using OpenRewrite.Core;
using OpenRewrite.CSharp;
using OpenRewrite.CSharp.Format;
using OpenRewrite.CSharp.Rpc;
using OpenRewrite.Java;
using OpenRewrite.Xml;
using OpenRewrite.Xml.Rpc;
using Rewrite.Core;
using ExecutionContext = OpenRewrite.Core.ExecutionContext;

namespace OpenRewrite.Test;

/// <summary>
/// Base class for rewrite tests with round-trip validation and recipe execution.
/// </summary>
public abstract class RewriteTest
{
    static RewriteTest()
    {
        WhitespaceReconciler.ThrowOnMismatchDefault = true;
    }

    private static readonly ConcurrentDictionary<ReferenceAssemblies, ImmutableArray<MetadataReference>>
        ResolvedAssembliesCache = new();

    private static ImmutableArray<MetadataReference> ResolveAssemblies(ReferenceAssemblies assemblies)
    {
        return ResolvedAssembliesCache.GetOrAdd(assemblies, a =>
            a.ResolveAsync(LanguageNames.CSharp, CancellationToken.None)
                .GetAwaiter()
                .GetResult());
    }

    protected void RewriteRun(params SourceSpec[] specs)
    {
        RewriteRun(_ => { }, specs);
    }

    protected void RewriteRun(Action<RecipeSpec> configure, params SourceSpec[] specs)
    {
        var recipeSpec = new RecipeSpec();
        configure(recipeSpec);

        var parser = new CSharpParser();
        var printer = new CSharpPrinter<object>();

        // Resolve metadata references if ReferenceAssemblies is configured
        ImmutableArray<MetadataReference>? metadataReferences = recipeSpec.ReferenceAssemblies != null
            ? ResolveAssemblies(recipeSpec.ReferenceAssemblies)
            : null;

        // 1. Parse all sources and validate round-trip
        var validations = recipeSpec.Validations;
        var parsed = new List<(SourceSpec Spec, SourceFile Source)>();

        // Collect all csproj-related specs and parse them together in a shared temp directory
        // so that MSBuild imports (Directory.Build.props/.targets) resolve correctly during restore.
        var csprojSpecs = specs.Where(s => s.SourcePath != null && IsCsprojPath(s.SourcePath)).ToList();
        Dictionary<string, SourceFile>? csprojParsed = null;
        if (csprojSpecs.Count > 0)
        {
            csprojParsed = ParseCsprojFilesTogether(csprojSpecs);
        }

        var sharedTrees = new Dictionary<SourceSpec, SyntaxTree>();
        CSharpCompilation? sharedCompilation = null;
        if (metadataReferences != null)
        {
            foreach (var spec in specs)
            {
                var isCsproj = spec.SourcePath != null && IsCsprojPath(spec.SourcePath);
                var isCs = spec.SourcePath == null ||
                           spec.SourcePath.EndsWith(".cs", StringComparison.OrdinalIgnoreCase);
                if (isCs && !isCsproj)
                {
                    sharedTrees[spec] = CSharpSyntaxTree.ParseText(
                        spec.Before, path: spec.SourcePath ?? "source.cs");
                }
            }

            if (sharedTrees.Count > 0)
            {
                sharedCompilation = Compile(metadataReferences.Value, sharedTrees.Values);
            }
        }

        foreach (var spec in specs)
        {
            SourceFile source;
            var isCsFile = spec.SourcePath != null &&
                           spec.SourcePath.EndsWith(".cs", StringComparison.OrdinalIgnoreCase);
            if (spec.SourcePath != null && IsCsprojPath(spec.SourcePath) && csprojParsed != null)
            {
                source = csprojParsed[spec.SourcePath];
            }
            else if (spec.SourcePath != null && !isCsFile)
            {
                // Remote-parsed source (e.g., XML via Java RPC). C# files always use local
                // parsing — even with a custom source path — because the Java peer doesn't
                // ship a C# parser.
                var rpc = RewriteRpcServer.Current
                          ?? throw new InvalidOperationException(
                              $"Parsing {spec.SourcePath} requires an RPC connection. " +
                              "Use RpcFixture to start a Java RPC server.");
                source = rpc.ParseOnRemote(spec.SourcePath, spec.Before,
                    spec.SourceFileType);
            }
            else
            {
                // Local C# parsing
                var localSourcePath = spec.SourcePath ?? "source.cs";
                SemanticModel? semanticModel = null;
                if (sharedCompilation != null && sharedTrees.TryGetValue(spec, out var syntaxTree))
                {
                    semanticModel = sharedCompilation.GetSemanticModel(syntaxTree);
                }

                source = parser.Parse(spec.Before, sourcePath: localSourcePath, semanticModel: semanticModel);

                // Verify no non-whitespace content leaked into Space fields
                if (validations.WhitespaceInSpaces)
                {
                    var whitespaceViolations = new List<WhitespaceViolation>();
                    new WhitespaceValidator().Visit(source, whitespaceViolations);
                    Assert.True(whitespaceViolations.Count == 0,
                        $"Found non-whitespace content in Space fields:\n" +
                        string.Join("\n", whitespaceViolations));
                }

                // Verify round-trip: printed should match input
                var printed = printer.Print(source);
                if (validations.PrintEqualsInput)
                {
                    AssertContentEquals(spec.Before, printed, source.SourcePath,
                        "The printed source didn't match the original source code. " +
                        "This means there is a bug in the parser implementation itself.");
                }

                // Verify idempotence: reparse and reprint should match
                if (validations.PrintIdempotence)
                {
                    var reparsed = parser.Parse(printed);
                    var reprinted = printer.Print(reparsed);
                    AssertContentEquals(printed, reprinted, source.SourcePath,
                        "The source is not print idempotent. Printing, re-parsing, and re-printing produced different output.");
                }
            }

            parsed.Add((spec, source));
        }

        // 2. If recipe configured, run it and verify results
        if (recipeSpec.Recipe != null)
        {
            var sources = parsed.Select(p => p.Source).ToList();
            // Marker placements the inputs already carry were not added by the recipe
            var inputMarkers = validations.ReportingMarkersOnly ? AddedMarkersCheck.Placements(sources) : null;
            var expectedCycles = recipeSpec.ExpectedCyclesThatMakeChanges ?? 1;
            var cycles = recipeSpec.Cycles ?? Math.Max(2, expectedCycles + 1);
            if (cycles < Math.Max(1, expectedCycles))
            {
                throw new ArgumentException(
                    $"Cycles={cycles} has to be at least 1 and at least ExpectedCyclesThatMakeChanges={expectedCycles}");
            }

            var prevThrow = WhitespaceReconciler.ThrowOnMismatchDefault;
            WhitespaceReconciler.ThrowOnMismatchDefault = validations.FormattingReconciliation;
            var runCtx = new ExecutionContext();
            try
            {
                var run = RunCycles(recipeSpec.Recipe, sources, runCtx, cycles, expectedCycles);
                var results = run.Changes;

                if (inputMarkers != null)
                {
                    AddedMarkersCheck.Verify(inputMarkers, results);
                }

                foreach (var afterRecipe in recipeSpec.AfterRecipe)
                {
                    afterRecipe(runCtx);
                }

                foreach (var (spec, source) in parsed)
                {
                    var result = results.FirstOrDefault(r =>
                        r.Before != null && r.Before.Id == source.Id);

                    if (spec.After != null)
                    {
                        // Expected a change
                        Assert.True(result != null && result.After != null,
                            $"Recipe was expected to make changes but did not modify the source file.");
                        var afterPrinted = PrintTree(result.After);
                        AssertContentEquals(spec.After, afterPrinted, result.After.SourcePath,
                            "Unexpected result from recipe");
                    }
                    else
                    {
                        // Expected no change
                        Assert.True(result == null || result.After == null || result.Before == result.After,
                            "Recipe made unexpected changes to a source file that was not expected to change.");
                    }
                }

                if (run.Unsettled is var (cycle, changes))
                {
                    ExpectNoFurtherChanges(changes,
                        $"Expected recipe to complete in {PluralCycles(expectedCycles)}, but cycle {cycle} changed");
                }
                if (cycles > expectedCycles && results.Count > 0)
                {
                    ExpectStableOnFreshParse(recipeSpec.Recipe, run.Finals, metadataReferences, runCtx);
                }
            }
            finally
            {
                WhitespaceReconciler.ThrowOnMismatchDefault = prevThrow;
            }
        }
    }

    /// <param name="Changes">The changes of the expected cycles, from the parsed sources to the output of the last of them.</param>
    /// <param name="Finals">Every source file as the last of the expected cycles left it.</param>
    /// <param name="Unsettled">The cycle that changed the sources after the expected ones, with its changes.</param>
    private sealed record CycleRun(
        List<Result> Changes,
        List<SourceFile> Finals,
        (int Cycle, List<Result> Changes)? Unsettled);

    /// <summary>Follows one source file from where it started to where it is now, either end null for a file a cycle generated or deleted.</summary>
    private sealed class Lineage(SourceFile? origin, SourceFile? latest)
    {
        public SourceFile? Origin { get; } = origin;
        public SourceFile? Latest { get; set; } = latest;
    }

    /// <summary>Runs the recipe cycle after cycle, each on what the cycle before produced.</summary>
    private static CycleRun RunCycles(Recipe recipe, List<SourceFile> sources, ExecutionContext ctx,
        int cycles, int expectedCycles)
    {
        var lineages = sources.Select(s => new Lineage(s, s)).ToList();
        CycleRun Settle(int cycle, List<Result>? changes) => new(
            lineages.Where(l => !ReferenceEquals(l.Origin, l.Latest)).Select(l => new Result(l.Origin, l.Latest)).ToList(),
            lineages.Where(l => l.Latest != null).Select(l => l.Latest!).ToList(),
            changes == null ? null : (cycle, changes));

        var settled = expectedCycles == 0 ? Settle(0, null) : null;
        var cyclesThatMadeChanges = 0;
        for (var cycle = 1; cycle <= cycles; cycle++)
        {
            var live = lineages.Where(l => l.Latest != null).Select(l => l.Latest!).ToList();
            var changes = cycle == 1
                ? RecipeScheduler.Run(recipe, live, ctx)
                : RunWithoutWritingDataTables(recipe, live, ctx);
            if (changes.Count == 0)
            {
                break;
            }
            if (settled != null)
            {
                return settled with { Unsettled = (cycle, changes) };
            }

            cyclesThatMadeChanges++;
            foreach (var (before, after) in changes)
            {
                var lineage = before == null ? null : lineages.Find(l => ReferenceEquals(l.Latest, before));
                if (lineage != null)
                {
                    lineage.Latest = after;
                }
                else
                {
                    lineages.Add(new Lineage(null, after));
                }
            }
            if (cycle >= expectedCycles)
            {
                settled = Settle(cycle, null);
            }
        }

        if (expectedCycles > 1 && cyclesThatMadeChanges < expectedCycles)
        {
            Assert.Fail($"Expected recipe to complete in {PluralCycles(expectedCycles)}, but it settled after " +
                        $"{PluralCycles(cyclesThatMadeChanges)}. Lower ExpectedCyclesThatMakeChanges.");
        }
        return settled ?? Settle(0, null);
    }

    /// <summary>Data tables are written by the first cycle only, as in Java.</summary>
    private static List<Result> RunWithoutWritingDataTables(Recipe recipe, List<SourceFile> sources, ExecutionContext ctx)
    {
        var dataTables = ctx.ComputeMessageIfAbsent<IDataTableStore>(DataTable<object>.DataTableStoreKey,
            _ => new InMemoryDataTableStore());
        ctx.PutMessage(DataTable<object>.DataTableStoreKey, new InMemoryDataTableStore());
        try
        {
            return RecipeScheduler.Run(recipe, sources, ctx);
        }
        finally
        {
            ctx.PutMessage(DataTable<object>.DataTableStoreKey, dataTables);
        }
    }

    private static void ExpectNoFurtherChanges(List<Result> changes, string headline, bool sanitized = false)
    {
        var markerPrinter = sanitized ? MarkerPrinter.Sanitized : MarkerPrinter.Default;
        var found = new List<string>();
        foreach (var (before, after) in changes)
        {
            if (before == null)
            {
                found.Add($"  {after!.SourcePath}, which it generated");
            }
            else if (after == null)
            {
                found.Add($"  {before.SourcePath}, which it deleted");
            }
            else
            {
                var expected = PrintTree(before, markerPrinter);
                var actual = PrintTree(after, markerPrinter);
                if (expected != actual)
                {
                    found.Add($"  {after.SourcePath}\n{DiffUtils.UnifiedDiff(expected, actual, after.SourcePath)}");
                }
                else if (!sanitized)
                {
                    found.Add($"  {after.SourcePath}, whose LST it changed without changing the printed source");
                }
            }
        }

        if (found.Count > 0)
        {
            Assert.Fail(string.Join("\n", [
                $"{headline} the sources again:",
                ..found,
                "A recipe should recognize code it has already changed and leave it alone, so that running it " +
                "again changes nothing. A recipe that is meant to need more cycles declares how many with " +
                "ExpectedCyclesThatMakeChanges."
            ]));
        }
    }

    /// <summary>
    /// Code that the next run reads is parsed from the printed output, not the LST this run produced, and a
    /// recipe that recognizes its own output only by the shape of that LST changes it again.
    /// </summary>
    private static void ExpectStableOnFreshParse(Recipe recipe, List<SourceFile> finals,
        ImmutableArray<MetadataReference>? metadataReferences, ExecutionContext ctx)
    {
        var units = finals.OfType<CompilationUnit>()
            .Select(unit =>
            {
                var text = PrintTree(unit, MarkerPrinter.Sanitized);
                return (Unit: unit, Text: text, Tree: CSharpSyntaxTree.ParseText(text, path: unit.SourcePath));
            })
            .ToList();
        var compilation = metadataReferences != null && units.Count > 0
            ? Compile(metadataReferences.Value, units.Select(u => u.Tree))
            : null;

        var parser = new CSharpParser();
        var reparsedByFinal = new Dictionary<SourceFile, SourceFile>(ReferenceEqualityComparer.Instance);
        foreach (var (unit, text, tree) in units)
        {
            try
            {
                var reparsed = parser.Parse(text, sourcePath: unit.SourcePath,
                    semanticModel: compilation?.GetSemanticModel(tree));
                if (PrintTree(reparsed, MarkerPrinter.Sanitized) == text)
                {
                    // What the source file carries, like its styles, comes from outside its text
                    reparsedByFinal[unit] = reparsed.WithMarkers(unit.Markers);
                }
            }
            catch (Exception)
            {
                // A file that does not parse again keeps the LST the recipe produced
            }
        }
        if (reparsedByFinal.Count == 0)
        {
            return;
        }

        var sources = finals.Select(f => reparsedByFinal.GetValueOrDefault(f, f)).ToList();
        ExpectNoFurtherChanges(RunWithoutWritingDataTables(recipe, sources, ctx),
            "Running the recipe on a fresh parse of its output changed", sanitized: true);
    }

    private static CSharpCompilation Compile(ImmutableArray<MetadataReference> metadataReferences,
        IEnumerable<SyntaxTree> syntaxTrees)
    {
        return CSharpCompilation.Create("TestCompilation")
            .WithOptions(new CSharpCompilationOptions(OutputKind.DynamicallyLinkedLibrary))
            .AddReferences(metadataReferences)
            .AddSyntaxTrees(syntaxTrees);
    }

    private static string PluralCycles(int count) => count == 1 ? "1 cycle" : $"{count} cycles";

    protected static SourceSpec CSharp(string before, string? after = null)
    {
        return new SourceSpec(before, after);
    }

    protected static SourceSpec CsProj([LanguageInjection("xml")]string before, [LanguageInjection("xml")]string? after = null, string sourcePath = "project.csproj")
    {
        return new SourceSpec(before, after, sourcePath, "org.openrewrite.xml.tree.Xml$Document");
    }

    /// <summary>
    /// Parses C# source with a semantic model, returning the CompilationUnit.
    /// </summary>
    protected static CSharp.CompilationUnit Parse(string source)
    {
        var syntaxTree = CSharpSyntaxTree.ParseText(source, path: "source.cs");
        var compilation = CSharpCompilation.Create("TestCompilation")
            .WithOptions(new CSharpCompilationOptions(OutputKind.DynamicallyLinkedLibrary))
            .AddReferences(ResolveAssemblies(Assemblies.Net90))
            .AddSyntaxTrees(syntaxTree);
        var semanticModel = compilation.GetSemanticModel(syntaxTree);
        return new CSharpParser().Parse(source, semanticModel: semanticModel);
    }

    /// <summary>
    /// Finds the first node of type TNode in the tree using a depth-first walk.
    /// </summary>
    protected static TNode? FindFirst<TNode>(Tree tree) where TNode : J
    {
        var finder = new FirstFinder<TNode>();
        finder.Visit(tree, 0);
        return finder.Found;
    }

    private class FirstFinder<TNode> : CSharpVisitor<int> where TNode : J
    {
        public TNode? Found { get; private set; }

        public override J? Visit(Tree? tree, int p)
        {
            if (Found != null) return tree as J;
            if (tree is TNode match)
            {
                Found = match;
                return tree as J;
            }
            return base.Visit(tree, p);
        }
    }

    private static string PrintTree(SourceFile tree, IMarkerPrinter? markerPrinter = null)
    {
        var capture = new PrintOutputCapture<object>(null!, markerPrinter ?? MarkerPrinter.Default);
        if (tree is Xml.Document)
        {
            new XmlPrinter<object>().Visit(tree, capture);
        }
        else
        {
            new CSharpPrinter<object>().Visit(tree, capture);
        }
        return capture.ToString();
    }

    private static readonly CsprojParser CsprojParserInstance = new();

    private static bool IsCsprojPath(string path)
    {
        // packages.config / nuget.config ride along into the shared temp directory so the
        // in-process restore sees them (legacy attestation, package sources) during ParseAll.
        var fileName = Path.GetFileName(path);
        if (fileName.Equals("packages.config", StringComparison.OrdinalIgnoreCase) ||
            fileName.Equals("nuget.config", StringComparison.OrdinalIgnoreCase))
        {
            return true;
        }
        return CsprojParserInstance.Accept(path);
    }

    private static Dictionary<string, SourceFile> ParseCsprojFilesTogether(List<SourceSpec> specs)
    {
        var files = specs.Select(s => (s.Before, s.SourcePath!)).ToList();
        var docs = CsprojParserInstance.ParseAll(files);

        var result = new Dictionary<string, SourceFile>();
        for (var i = 0; i < specs.Count; i++)
            result[specs[i].SourcePath!] = docs[i];
        return result;
    }

    private static void AssertContentEquals(string expected, string actual, string sourcePath,
        string errorMessagePrefix)
    {
        if (expected == actual) return;
        var diff = DiffUtils.UnifiedDiff(expected, actual, sourcePath);
        Assert.Fail($"{errorMessagePrefix} \"{sourcePath}\":\n{diff}");
    }
}

/// <summary>
/// Specification for a source file in a test.
/// </summary>
/// <param name="Before">Source content before recipe execution.</param>
/// <param name="After">Expected content after recipe execution (null = expect no change).</param>
/// <param name="SourcePath">File path for remote parsing (null = local C# parsing).</param>
/// <param name="SourceFileType">Java type name for RPC GetObject calls.</param>
public record SourceSpec(
    string Before,
    string? After = null,
    string? SourcePath = null,
    string? SourceFileType = null);

/// <summary>
/// Specification for recipe configuration in a test.
/// </summary>
public class RecipeSpec
{
    public Recipe? Recipe { get; private set; }
    public ReferenceAssemblies? ReferenceAssemblies { get; private set; } = Assemblies.Net90;
    public Validations Validations { get; private set; } = Validations.All;

    private readonly List<Action<ExecutionContext>> _afterRecipe = [];

    /// <summary>
    /// Assertions run against the execution context after the recipe completes (e.g. to verify
    /// data table rows written via the recipe's <see cref="OpenRewrite.Core.IDataTableStore"/>).
    /// </summary>
    public IReadOnlyList<Action<ExecutionContext>> AfterRecipe => _afterRecipe;

    /// <summary>
    /// The cycles to run, by default one more than <see cref="ExpectedCyclesThatMakeChanges"/> and at least two.
    /// The cycles after the expected ones, and a run on a fresh parse of the output, fail the test if they change
    /// anything, so setting this to 1 turns the stability checks off.
    /// </summary>
    public int? Cycles { get; private set; }

    /// <summary>
    /// How many cycles a recipe needs to settle, by default 1. A recipe that only reaches its result over several
    /// cycles declares how many here, and the test then compares the output of the last of them.
    /// </summary>
    public int? ExpectedCyclesThatMakeChanges { get; private set; }

    public RecipeSpec SetRecipe(Recipe recipe)
    {
        Recipe = recipe;
        return this;
    }

    public RecipeSpec SetCycles(int cycles)
    {
        Cycles = cycles;
        return this;
    }

    public RecipeSpec SetExpectedCyclesThatMakeChanges(int expectedCyclesThatMakeChanges)
    {
        ExpectedCyclesThatMakeChanges = expectedCyclesThatMakeChanges;
        return this;
    }

    /// <summary>
    /// Assert the rows a recipe wrote to a data table. <paramref name="group"/> is the data
    /// table's bucket key — its <c>Group</c> if set, otherwise its <c>InstanceName</c> (which
    /// defaults to the display name). The test-default in-memory store is read back, so no RPC
    /// data-table configuration is required.
    /// </summary>
    public RecipeSpec ExpectDataTable<TRow>(string name, string group, Action<IReadOnlyList<TRow>> assertion)
        where TRow : notnull
    {
        _afterRecipe.Add(ctx =>
        {
            var store = ctx.GetMessage<OpenRewrite.Core.IDataTableStore>(
                OpenRewrite.Core.DataTable<TRow>.DataTableStoreKey);
            var rows = (store?.GetRows(name, group) ?? []).Cast<TRow>().ToList();
            assertion(rows);
        });
        return this;
    }

    public RecipeSpec SetReferenceAssemblies(ReferenceAssemblies? referenceAssemblies)
    {
        ReferenceAssemblies = referenceAssemblies;
        return this;
    }

    public RecipeSpec SetValidations(Validations validations)
    {
        Validations = validations;
        return this;
    }
}

/// <summary>
/// Pre-configured reference assemblies for common .NET SDK targets.
/// </summary>
public static class Assemblies
{
    public static ReferenceAssemblies Net90 => Microsoft.CodeAnalysis.Testing.ReferenceAssemblies.Net.Net90;
    public static ReferenceAssemblies Net100 => Microsoft.CodeAnalysis.Testing.ReferenceAssemblies.Net.Net100;

    /// <summary>
    /// .NET Framework 4.8 reference assemblies (via the
    /// <c>Microsoft.NETFramework.ReferenceAssemblies.net48</c> package). Use for tests that
    /// need to attest legacy framework types such as <c>System.Web.UI.Page</c>,
    /// <c>System.ServiceModel.*</c>, or <c>System.Data.Entity.*</c>.
    /// </summary>
    public static ReferenceAssemblies Net48 => Microsoft.CodeAnalysis.Testing.ReferenceAssemblies.NetFramework.Net48.Default;

    public static ReferenceAssemblies AspNet90 =>
        Net90.AddPackage("Microsoft.AspNetCore.App.Ref");

    public static ReferenceAssemblies AspNet100 =>
        Net100.AddPackage("Microsoft.AspNetCore.App.Ref");

    public static ReferenceAssemblies AddPackage(this ReferenceAssemblies referenceAssemblies, string package)
    {
        return referenceAssemblies.AddPackage(package,
            referenceAssemblies.ReferenceAssemblyPackage?.Version
            ?? throw new InvalidOperationException("ReferenceAssemblyPackage.Version is null"));
    }

    public static ReferenceAssemblies AddPackage(this ReferenceAssemblies referenceAssemblies, string package,
        string version)
    {
        return referenceAssemblies.AddPackages([new PackageIdentity(package, version)]);
    }
}
