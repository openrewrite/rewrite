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
        // Blocks because its callers, RewriteRun and Parse, are synchronous test APIs.
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

        // An .editorconfig styles the C# sources, as SolutionParser has it do, and is not a source itself
        var editorConfigSpecs = specs.Where(EditorConfigSources.Is).ToList();
        if (editorConfigSpecs.Any(s => s.After != null))
        {
            throw new ArgumentException("An .editorconfig styles the sources of a test, and no recipe changes it.");
        }
        specs = specs.Where(s => !EditorConfigSources.Is(s)).ToArray();
        using var editorConfigs = editorConfigSpecs.Count > 0 ? new EditorConfigSources(editorConfigSpecs) : null;

        var parser = new CSharpParser();
        var printer = new CSharpPrinter<object>();

        var setup = recipeSpec.ReferenceAssemblies != null
            ? new CompileSetup(ResolveAssemblies(recipeSpec.ReferenceAssemblies).AddRange(recipeSpec.References),
                recipeSpec.CompilationOptions, recipeSpec.ParseOptions)
            : null;
        var projects = new TestProjects(specs);

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
        var compilations = new Dictionary<SyntaxTree, CSharpCompilation>();
        if (setup != null)
        {
            foreach (var spec in specs)
            {
                var isCsproj = spec.SourcePath != null && IsCsprojPath(spec.SourcePath);
                var isCs = spec.SourcePath == null ||
                           spec.SourcePath.EndsWith(".cs", StringComparison.OrdinalIgnoreCase);
                if (isCs && !isCsproj)
                {
                    sharedTrees[spec] = setup.Parse(spec.Before, spec.SourcePath ?? "source.cs");
                }
            }
            compilations = projects.Compile(sharedTrees.Select(t => (projects.Of(t.Key), t.Value)), setup);
        }

        if (recipeSpec.InputCompiles)
        {
            if (recipeSpec.ReferenceAssemblies == null)
            {
                throw new ArgumentException(
                    "ExpectInputCompiles compiles against the ReferenceAssemblies, which are not set.");
            }
            AssertCompiles(compilations.Values.Distinct(), recipeSpec.InputInspections,
                $"The sources of the test don't compile against {recipeSpec.ReferenceAssemblies.TargetFramework}" +
                ", so types the recipe looks at are missing or wrong");
        }

        SourceFile BeforeRecipe(SourceSpec spec, SourceFile source)
        {
            if (projects.Of(spec) is { } project)
            {
                source = SourceSpec.AddMarker(source, project);
            }
            return spec.BeforeRecipeHooks.Aggregate(source, (s, adjust) => adjust(s));
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
                if (sharedTrees.TryGetValue(spec, out var syntaxTree))
                {
                    semanticModel = compilations[syntaxTree].GetSemanticModel(syntaxTree);
                }

                var cu = parser.Parse(spec.Before, sourcePath: localSourcePath, semanticModel: semanticModel);
                source = editorConfigs?.Style(cu, localSourcePath) ?? cu;

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

            parsed.Add((spec, BeforeRecipe(spec, source)));
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
            var checks = new RunChecks(validations);
            var runCtx = checks.Ctx;
            foreach (var seed in recipeSpec.ExecutionContextSeeds)
            {
                checks.Seed(seed);
            }
            var formatting = validations.MinimalFormatting ? new FormattingScope(recipeSpec.Recipe, checks) : null;
            try
            {
                CycleRun run;
                using (formatting?.Install())
                {
                    run = RunCycles(recipeSpec.Recipe, sources, checks, cycles, expectedCycles);
                }
                var results = run.Changes;

                if (inputMarkers != null)
                {
                    AddedMarkersCheck.Verify(inputMarkers, results);
                }
                checks.AssertLightweightAccumulators();

                foreach (var afterRecipe in recipeSpec.AfterRecipe)
                {
                    afterRecipe(runCtx);
                }
                foreach (var assertion in recipeSpec.ResultAssertions)
                {
                    assertion(results);
                }
                foreach (var assertion in recipeSpec.AccumulatorAssertions)
                {
                    assertion(checks.FirstScan ?? new Dictionary<IScanningRecipe, object>());
                }

                foreach (var (spec, source) in parsed)
                {
                    var result = results.FirstOrDefault(r =>
                        r.Before != null && r.Before.Id == source.Id);

                    if (spec.After != null || spec.AfterFromActual != null)
                    {
                        // Expected a change
                        Assert.True(result != null && result.After != null,
                            $"Recipe was expected to make changes but did not modify the source file.");
                        var afterPrinted = PrintTree(result.After);
                        AssertContentEquals(spec.AfterFromActual?.Invoke(afterPrinted) ?? spec.After!, afterPrinted,
                            result.After.SourcePath, "Unexpected result from recipe");
                    }
                    else
                    {
                        // Expected no change
                        if (result is { After: { } changed } && PrintTree(changed) == PrintTree(source))
                        {
                            Assert.Fail(
                                "Return the tree the recipe was given when there is nothing to change.\n" +
                                $"{recipeSpec.Recipe.Name} returned a new tree for {source.SourcePath} that prints " +
                                "the same, so the run reports the source file as changed, with an empty diff.");
                        }
                        Assert.True(result == null || result.After == null || result.Before == result.After,
                            "Recipe made unexpected changes to a source file that was not expected to change.");
                    }
                    AfterRecipe(spec, source, result == null ? source : result.After);
                }
                AssertOutputCompiles(recipeSpec, run.Finals, setup, projects);
                if (validations.RpcRoundTrip && this is RpcRewriteTest)
                {
                    RpcRoundTrip.AssertSurvives(results);
                }

                if (run.Unsettled is var (cycle, changes))
                {
                    ExpectNoFurtherChanges(changes,
                        $"Expected recipe to complete in {PluralCycles(expectedCycles)}, but cycle {cycle} changed");
                }
                var fresh = results.Count > 0 &&
                            (cycles > expectedCycles || validations.ParserShapedTrees || validations.TypesAttributed)
                    ? ParseAgain(run.Finals, setup, projects)
                    : null;
                if (cycles > expectedCycles && fresh != null)
                {
                    ExpectStableOnFreshParse(recipeSpec.Recipe, run.Finals, fresh, checks);
                }
                formatting?.AssertFormattedOnlyWhatChanged(results);
                if (validations.ParserShapedTrees && fresh != null)
                {
                    TreeShapes.AssertParserShaped(recipeSpec.Recipe, results, fresh);
                }
                if (validations.TypesAttributed && fresh != null)
                {
                    TreeShapes.AssertTyped(recipeSpec.Recipe, results, fresh);
                }
                if (validations.UniqueIds)
                {
                    UniqueIds.AssertUnique(recipeSpec.Recipe, results);
                }
                EmptySources.AssertNoneThrow(recipeSpec.Recipe, results, cycles);
                UnparsedSources.AssertNoneThrow(recipeSpec.Recipe, results, sources, cycles);
            }
            finally
            {
                WhitespaceReconciler.ThrowOnMismatchDefault = prevThrow;
            }
        }
        else
        {
            foreach (var assertion in recipeSpec.ResultAssertions)
            {
                assertion([]);
            }
            foreach (var (spec, source) in parsed)
            {
                AfterRecipe(spec, source, source);
            }
            AssertOutputCompiles(recipeSpec, parsed.Select(p => p.Source).ToList(), setup, projects);
        }
    }

    private static void AfterRecipe(SourceSpec spec, SourceFile before, SourceFile? after)
    {
        if (spec.AfterRecipeHooks.IsEmpty)
        {
            return;
        }
        if (after == null)
        {
            Assert.Fail($"The recipe deleted {before.SourcePath}, which has AfterRecipe assertions.");
        }
        foreach (var assertion in spec.AfterRecipeHooks)
        {
            assertion(before, after);
        }
    }

    private static void AssertOutputCompiles(RecipeSpec recipeSpec, List<SourceFile> finals, CompileSetup? setup,
        TestProjects projects)
    {
        var units = finals.OfType<CSharp.CompilationUnit>()
            .Select(unit => (Unit: unit, Text: PrintTree(unit, MarkerPrinter.Sanitized)))
            .ToList();
        foreach (var check in recipeSpec.OutputCompilations)
        {
            var target = check.References ?? recipeSpec.ReferenceAssemblies ?? throw new ArgumentException(
                "ExpectOutputCompiles compiles against the ReferenceAssemblies when it isn't given references, " +
                "and they are not set.");
            var outputSetup = new CompileSetup(
                check.References == null && setup != null
                    ? setup.References
                    : ResolveAssemblies(target).AddRange(recipeSpec.References),
                check.Options ?? recipeSpec.CompilationOptions,
                check.ParseOptions ?? recipeSpec.ParseOptions);
            var compilations = projects.Compile(
                units.Select(u => (projects.Of(u.Unit), outputSetup.Parse(u.Text, u.Unit.SourcePath))), outputSetup);
            AssertCompiles(compilations.Values.Distinct(), check.Inspect == null ? [] : [check.Inspect],
                $"The recipe's output doesn't compile against {target.TargetFramework}");
        }
    }

    private static void AssertCompiles(IEnumerable<CSharpCompilation> compilations,
        IReadOnlyList<Action<CSharpCompilation>> inspections, string headline)
    {
        var all = compilations.ToList();
        var errors = all.SelectMany(c => c.GetDiagnostics())
            .Where(d => d.Severity == DiagnosticSeverity.Error)
            .ToList();
        if (errors.Count > 0)
        {
            var failing = errors.Select(e => e.Location.SourceTree).OfType<SyntaxTree>().Distinct();
            Assert.Fail(string.Join("\n", [
                $"{headline}:",
                ..errors.Select(e => $"  {e}"),
                ..failing.Select(tree => $"{tree.FilePath}:\n{tree.GetText()}")
            ]));
        }
        foreach (var compilation in all)
        {
            foreach (var inspect in inspections)
            {
                inspect(compilation);
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
    private static CycleRun RunCycles(Recipe recipe, List<SourceFile> sources, RunChecks checks,
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
                ? checks.Run(recipe, live)
                : RunWithoutWritingDataTables(recipe, live, checks);
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
    private static List<Result> RunWithoutWritingDataTables(Recipe recipe, List<SourceFile> sources, RunChecks checks)
    {
        var ctx = checks.Ctx;
        var dataTables = ctx.ComputeMessageIfAbsent<IDataTableStore>(DataTable<object>.DataTableStoreKey,
            _ => new InMemoryDataTableStore());
        ctx.PutMessage(DataTable<object>.DataTableStoreKey, new InMemoryDataTableStore());
        try
        {
            return checks.Run(recipe, sources);
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

    /// <param name="SyntaxTree">Roslyn's parse of the printed text.</param>
    /// <param name="Unit">The LST of the printed text, or null when the parser doesn't print it back.</param>
    internal sealed record FreshParse(SyntaxTree SyntaxTree, CompilationUnit? Unit);

    /// <summary>The printed text of each compilation unit the cycles left, parsed as the next run reads it.</summary>
    private static Dictionary<SourceFile, FreshParse> ParseAgain(List<SourceFile> finals, CompileSetup? setup,
        TestProjects projects)
    {
        var units = finals.OfType<CompilationUnit>()
            .Select(unit =>
            {
                var text = PrintTree(unit, MarkerPrinter.Sanitized);
                var tree = setup?.Parse(text, unit.SourcePath) ??
                           CSharpSyntaxTree.ParseText(text, path: unit.SourcePath);
                return (Unit: unit, Text: text, Tree: tree);
            })
            .ToList();
        var compilations = setup != null
            ? projects.Compile(units.Select(u => (projects.Of(u.Unit), u.Tree)), setup)
            : null;

        var parser = new CSharpParser();
        var fresh = new Dictionary<SourceFile, FreshParse>(ReferenceEqualityComparer.Instance);
        foreach (var (unit, text, tree) in units)
        {
            CompilationUnit? reparsed = null;
            try
            {
                reparsed = parser.Parse(text, sourcePath: unit.SourcePath,
                    semanticModel: compilations?[tree].GetSemanticModel(tree));
                if (PrintTree(reparsed, MarkerPrinter.Sanitized) != text)
                {
                    reparsed = null;
                }
            }
            catch (Exception)
            {
                // A file that does not parse again keeps the LST the recipe produced
            }
            fresh[unit] = new FreshParse(tree, reparsed);
        }
        return fresh;
    }

    /// <summary>
    /// Code that the next run reads is parsed from the printed output, not the LST this run produced, and a
    /// recipe that recognizes its own output only by the shape of that LST changes it again.
    /// </summary>
    private static void ExpectStableOnFreshParse(Recipe recipe, List<SourceFile> finals,
        Dictionary<SourceFile, FreshParse> fresh, RunChecks checks)
    {
        if (!fresh.Values.Any(parse => parse.Unit != null))
        {
            return;
        }

        var sources = finals
            .Select(f => fresh.TryGetValue(f, out var parse) && parse.Unit != null
                // What the source file carries, like its styles, comes from outside its text
                ? parse.Unit.WithMarkers(f.Markers)
                : f)
            .ToList();
        ExpectNoFurtherChanges(RunWithoutWritingDataTables(recipe, sources, checks),
            "Running the recipe on a fresh parse of its output changed", sanitized: true);
    }

    private static string PluralCycles(int count) => count == 1 ? "1 cycle" : $"{count} cycles";

    protected static SourceSpec CSharp(string before, string? after = null)
    {
        return new SourceSpec(before, after);
    }

    /// <summary>
    /// A C# source the recipe changes, whose expected text <paramref name="after"/> makes of what the recipe printed,
    /// such as to assert on parts of a large output and return it, or to compute it from the parts that vary.
    /// </summary>
    protected static SourceSpec CSharp(string before, Func<string, string> after)
    {
        return new SourceSpec(before) { AfterFromActual = after };
    }

    /// <summary>
    /// A C# source read from a file, such as a fixture the test project copies to its output directory, by its path
    /// or a path relative to that directory. A <c>.txt</c> suffix that keeps the fixture out of the test project's
    /// build is not part of its source path. <paramref name="sha256"/>, when given, is the hex SHA-256 the file has to
    /// have, as for code taken from a project the test cites.
    /// </summary>
    protected static SourceSpec CSharpFromFile(string path, string? after = null, string? sha256 = null)
    {
        var bytes = File.ReadAllBytes(Path.IsPathRooted(path) ? path : Path.Combine(AppContext.BaseDirectory, path));
        return Fixture(bytes, Path.GetFileName(path), after, sha256);
    }

    /// <summary>
    /// A C# source read from a resource embedded in the test's assembly, by its manifest name or the end of it, such as
    /// <c>Fixture.cs.txt</c>, otherwise as <see cref="CSharpFromFile"/>. MSBuild reads the <c>cs</c> of
    /// <c>Fixture.cs.txt</c> as the Czech culture and puts the resource in a satellite assembly unless the
    /// <c>EmbeddedResource</c> item says <c>WithCulture="false"</c>.
    /// </summary>
    protected SourceSpec CSharpFromResource(string name, string? after = null, string? sha256 = null)
    {
        var assembly = GetType().Assembly;
        var names = assembly.GetManifestResourceNames();
        var matches = names.Where(n => n == name || n.EndsWith("." + name, StringComparison.Ordinal)).ToList();
        if (matches.Count != 1)
        {
            var count = matches.Count == 0 ? "no" : matches.Count.ToString();
            throw new ArgumentException($"{assembly.GetName().Name} embeds {count} resources named {name}, and has: " +
                                        $"{string.Join(", ", names)}.");
        }
        using var stream = assembly.GetManifestResourceStream(matches[0])!;
        using var bytes = new MemoryStream();
        stream.CopyTo(bytes);
        return Fixture(bytes.ToArray(), name, after, sha256);
    }

    private static SourceSpec Fixture(byte[] bytes, string name, string? after, string? sha256)
    {
        if (sha256 != null)
        {
            var actual = Convert.ToHexStringLower(System.Security.Cryptography.SHA256.HashData(bytes));
            if (!actual.Equals(sha256, StringComparison.OrdinalIgnoreCase))
            {
                throw new ArgumentException(
                    $"{name} has the SHA-256 {actual}, not {sha256}, so it isn't the code the test cites.");
            }
        }
        using var reader = new StreamReader(new MemoryStream(bytes), detectEncodingFromByteOrderMarks: true);
        var sourcePath = name.EndsWith(".txt", StringComparison.OrdinalIgnoreCase) ? name[..^4] : name;
        return new SourceSpec(reader.ReadToEnd(), after,
            sourcePath.EndsWith(".cs", StringComparison.OrdinalIgnoreCase) ? sourcePath : null);
    }

    /// <summary>
    /// An .editorconfig of the project the test's sources belong to. The C# sources take the style it sets for their
    /// paths, as <see cref="SolutionParser"/> gives them, and no recipe runs on it.
    /// </summary>
    protected static SourceSpec EditorConfig(string content, string sourcePath = ".editorconfig")
    {
        return new SourceSpec(content, null, sourcePath);
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
    string? SourceFileType = null)
{
    internal Func<string, string>? AfterFromActual { get; init; }
    internal ImmutableList<Func<SourceFile, SourceFile>> BeforeRecipeHooks { get; private init; } = [];
    internal ImmutableList<Action<SourceFile, SourceFile>> AfterRecipeHooks { get; private init; } = [];
    internal ImmutableArray<string>? ProjectTargetFrameworks { get; private init; }
    internal string? ProjectName { get; private init; }
    internal ImmutableArray<string> ProjectReferences { get; private init; } = [];

    /// <summary>
    /// Adjusts the parsed source before the recipe runs on it, after the parse and print checks, as when a test needs
    /// a tree the parser doesn't make. The adjustments run in the order they were added.
    /// </summary>
    public SourceSpec BeforeRecipe(Func<CompilationUnit, CompilationUnit> adjust) =>
        BeforeRecipe<CompilationUnit>(adjust);

    /// <inheritdoc cref="BeforeRecipe(Func{CompilationUnit, CompilationUnit})"/>
    public SourceSpec BeforeRecipe<T>(Func<T, T> adjust) where T : class, SourceFile =>
        this with
        {
            BeforeRecipeHooks = BeforeRecipeHooks.Add(source => adjust(As<T>(source, nameof(BeforeRecipe))))
        };

    /// <summary>Adds markers to the parsed source before the recipe runs on it.</summary>
    public SourceSpec AddMarkers(params Marker[] markers) =>
        BeforeRecipe<SourceFile>(source => markers.Aggregate(source, AddMarker));

    /// <summary>
    /// Has the source's project target the given frameworks, as its <see cref="DotNetProject"/> marker says. A source
    /// in no <see cref="InProject">named project</see> goes in a project named <c>Test</c>, as
    /// <see cref="SolutionParser"/> names the project of <c>Test.csproj</c>, which the sources of the test that target
    /// the same frameworks share.
    /// </summary>
    public SourceSpec TargetFrameworks(params string[] targetFrameworks) =>
        this with { ProjectTargetFrameworks = [..targetFrameworks] };

    /// <summary>
    /// Puts the source in the named SDK-style project, with the <see cref="DotNetProject"/> marker that
    /// <see cref="SolutionParser"/> gives it. The C# sources of each project compile on their own, against the projects
    /// that any of its sources names in <paramref name="projectReferences"/>, and those in no project compile together.
    /// To have sources compile together and still carry different markers, add the markers with
    /// <see cref="AddMarkers"/> instead. A project named by the path of a project file among the test's sources, such
    /// as <c>src/App/App.csproj</c>, is named, targets the frameworks, uses the SDK and references the projects that
    /// project file says, as <see cref="SolutionParser"/> reads them.
    /// </summary>
    public SourceSpec InProject(string projectName, params string[] projectReferences) =>
        this with { ProjectName = projectName, ProjectReferences = [..projectReferences] };

    /// <summary>
    /// Assertions on the source after the recipe ran, which get the source as the last expected cycle left it, or as
    /// it was parsed when the recipe didn't change it.
    /// </summary>
    public SourceSpec AfterRecipe(Action<CompilationUnit> assertion) => AfterRecipe<CompilationUnit>(assertion);

    /// <inheritdoc cref="AfterRecipe(Action{CompilationUnit})"/>
    public SourceSpec AfterRecipe<T>(Action<T> assertion) where T : class, SourceFile =>
        AfterRecipe<T>((_, after) => assertion(after));

    /// <summary>
    /// Assertions on the source as it was before the recipe ran and as the last expected cycle left it, the same tree
    /// twice when the recipe didn't change it, such as that a type the recipe kept is the same object as before.
    /// </summary>
    public SourceSpec AfterRecipe(Action<CompilationUnit, CompilationUnit> assertion) =>
        AfterRecipe<CompilationUnit>(assertion);

    /// <inheritdoc cref="AfterRecipe(Action{CompilationUnit, CompilationUnit})"/>
    public SourceSpec AfterRecipe<T>(Action<T, T> assertion) where T : class, SourceFile =>
        this with
        {
            AfterRecipeHooks = AfterRecipeHooks.Add((before, after) =>
                assertion(As<T>(before, nameof(AfterRecipe)), As<T>(after, nameof(AfterRecipe))))
        };

    internal static SourceFile AddMarker(SourceFile source, Marker marker)
    {
        var markers = source.Markers.Add(marker);
        if (source is J j)
        {
            return (SourceFile)J.SetMarkers(j, markers);
        }
        var withMarkers = source.GetType().GetMethod("WithMarkers", [typeof(Markers)]) ??
                          throw new ArgumentException($"{source.GetType().Name} has no markers to add to.");
        return (SourceFile)withMarkers.Invoke(source, [markers])!;
    }

    private static T As<T>(SourceFile source, string hook) where T : class, SourceFile =>
        source as T ?? throw new ArgumentException(
            $"{hook} takes a {typeof(T).Name}, but {source.SourcePath} is a {source.GetType().Name}.");
}

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

    /// <summary>
    /// Runs the recipes one after the other on each source, as a recipe that lists them does, such as a recipe that
    /// Java runs before and after the recipe under test to check that Java matches the types the recipe left. The
    /// sources the test expects are those the last of them leaves.
    /// </summary>
    public RecipeSpec SetRecipes(params Recipe[] recipes)
    {
        if (recipes.Length == 0)
        {
            throw new ArgumentException("SetRecipes takes the recipes to run, and none were given.");
        }
        Recipe = recipes.Length == 1 ? recipes[0] : new RecipeSteps(recipes);
        return this;
    }

    private readonly List<Action<ExecutionContext>> _executionContextSeeds = [];

    internal IReadOnlyList<Action<ExecutionContext>> ExecutionContextSeeds => _executionContextSeeds;

    /// <summary>
    /// Puts what the recipe expects to find on the execution context before it runs, such as the build files a
    /// <see cref="DotNetBuildContext"/> captured earlier in the run, which the
    /// <see cref="Validations.ExecutionContextUnchanged"/> check then counts as there from the start.
    /// </summary>
    public RecipeSpec ConfigureExecutionContext(Action<ExecutionContext> configure)
    {
        _executionContextSeeds.Add(configure);
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

    private readonly List<Action<IReadOnlyList<Result>>> _resultAssertions = [];

    internal IReadOnlyList<Action<IReadOnlyList<Result>>> ResultAssertions => _resultAssertions;

    /// <summary>
    /// Assertions on the changes of the run: the source files the expected cycles changed, generated or deleted,
    /// each from how it was parsed to how the last of those cycles left it.
    /// </summary>
    public RecipeSpec ExpectResults(Action<IReadOnlyList<Result>> assertion)
    {
        _resultAssertions.Add(assertion);
        return this;
    }

    /// <summary>The options the C# sources compile with, by default those of a library.</summary>
    public CSharpCompilationOptions CompilationOptions { get; private set; } = new(OutputKind.DynamicallyLinkedLibrary);

    /// <summary>
    /// The options the C# sources are parsed with, such as the language version and preprocessor symbols.
    /// </summary>
    public CSharpParseOptions ParseOptions { get; private set; } = CSharpParseOptions.Default;

    private readonly List<MetadataReference> _references = [];

    /// <summary>
    /// The references the C# sources compile against in addition to the <see cref="ReferenceAssemblies"/>.
    /// </summary>
    public IReadOnlyList<MetadataReference> References => _references;

    public RecipeSpec SetCompilationOptions(CSharpCompilationOptions options)
    {
        CompilationOptions = options;
        return this;
    }

    public RecipeSpec SetParseOptions(CSharpParseOptions options)
    {
        ParseOptions = options;
        return this;
    }

    /// <summary>
    /// Has the C# sources, and the output that <see cref="ExpectOutputCompiles"/> compiles, also compile against the
    /// references, such as prebuilt DLLs, NuGet packages or a library compiled in memory, as with
    /// <c>MetadataReference.CreateFromFile(path)</c> or <c>compilation.ToMetadataReference()</c>.
    /// </summary>
    public RecipeSpec AddReferences(params MetadataReference[] references)
    {
        _references.AddRange(references);
        return this;
    }

    private readonly List<Action<IReadOnlyDictionary<IScanningRecipe, object>>> _accumulatorAssertions = [];

    internal IReadOnlyList<Action<IReadOnlyDictionary<IScanningRecipe, object>>> AccumulatorAssertions =>
        _accumulatorAssertions;

    /// <summary>
    /// Assertions on what a scanning recipe of the run found, in each accumulator of a <typeparamref name="T"/> once
    /// the first cycle scanned the sources, such as why the recipe leaves a file alone.
    /// </summary>
    public RecipeSpec ExpectAccumulator<T>(Action<T> assertion)
    {
        _accumulatorAssertions.Add(accumulators =>
        {
            var matching = accumulators.Values.OfType<T>().ToList();
            if (matching.Count == 0)
            {
                var kept = accumulators.Count == 0
                    ? "no scanning recipe ran"
                    : "the scanning recipes keep " +
                      string.Join(", ", accumulators.Values.Select(a => Name(a.GetType())));
                Assert.Fail($"ExpectAccumulator<{Name(typeof(T))}> found no accumulator of that type: {kept}.");
            }
            matching.ForEach(assertion);
        });
        return this;
    }

    private static string Name(Type type) => type.IsGenericType
        ? $"{type.Name[..type.Name.IndexOf('`')]}<{string.Join(", ", type.GetGenericArguments().Select(Name))}>"
        : type.Name;

    private readonly List<OutputCompilation> _outputCompilations = [];

    internal IReadOnlyList<OutputCompilation> OutputCompilations => _outputCompilations;

    /// <summary>
    /// Expect the C# sources as the recipe left them to compile without errors, against
    /// <paramref name="references"/> when the output targets other frameworks than the input, and against
    /// <see cref="ReferenceAssemblies"/> otherwise, and with <paramref name="options"/> and
    /// <paramref name="parseOptions"/> when they differ from <see cref="CompilationOptions"/> and
    /// <see cref="ParseOptions"/>, such as to treat warnings as errors or build a console app.
    /// <paramref name="inspect"/> gets the compilation of each project, such as to emit and run the output. Call it
    /// once for each way the output has to compile.
    /// </summary>
    public RecipeSpec ExpectOutputCompiles(ReferenceAssemblies? references = null,
        Action<CSharpCompilation>? inspect = null, CSharpCompilationOptions? options = null,
        CSharpParseOptions? parseOptions = null)
    {
        _outputCompilations.Add(new OutputCompilation(references, inspect, options, parseOptions));
        return this;
    }

    private readonly List<Action<CSharpCompilation>> _inputInspections = [];

    internal IReadOnlyList<Action<CSharpCompilation>> InputInspections => _inputInspections;

    /// <summary>Whether the test expects the C# sources to compile without errors before the recipe runs.</summary>
    public bool InputCompiles { get; private set; }

    /// <summary>
    /// Expect the C# sources to compile without errors before the recipe runs, so that the types the recipe sees are
    /// the ones the compiler resolved. <paramref name="inspect"/> gets the compilation of each project, such as to run
    /// the input and compare what it does with what the output does.
    /// </summary>
    public RecipeSpec ExpectInputCompiles(Action<CSharpCompilation>? inspect = null)
    {
        InputCompiles = true;
        if (inspect != null)
        {
            _inputInspections.Add(inspect);
        }
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

/// <param name="References">The references to compile against, or null for the ones the input compiled against.</param>
/// <param name="Inspect">What the test checks of the compilation.</param>
/// <param name="Options">The options to compile with, or null for the ones the input compiled with.</param>
/// <param name="ParseOptions">The options to parse with, or null for the ones the input was parsed with.</param>
internal sealed record OutputCompilation(
    ReferenceAssemblies? References,
    Action<CSharpCompilation>? Inspect,
    CSharpCompilationOptions? Options,
    CSharpParseOptions? ParseOptions);

/// <summary>The recipes of <see cref="RecipeSpec.SetRecipes"/>, run one after the other.</summary>
internal sealed class RecipeSteps(IReadOnlyList<Recipe> steps) : Recipe
{
    public override string DisplayName => "Recipe steps";

    public override string Description => string.Join(", then ", steps.Select(step => step.DisplayName)) + ".";

    public override List<Recipe> GetRecipeList() => [..steps];
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
