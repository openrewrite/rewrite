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
                if (metadataReferences != null)
                {
                    var syntaxTree = CSharpSyntaxTree.ParseText(spec.Before, path: localSourcePath);
                    var compilation = CSharpCompilation.Create("TestCompilation")
                        .WithOptions(new CSharpCompilationOptions(OutputKind.DynamicallyLinkedLibrary))
                        .AddReferences(metadataReferences)
                        .AddSyntaxTrees(syntaxTree);
                    semanticModel = compilation.GetSemanticModel(syntaxTree);
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
            var prevThrow = WhitespaceReconciler.ThrowOnMismatchDefault;
            WhitespaceReconciler.ThrowOnMismatchDefault = validations.FormattingReconciliation;
            List<Result> results;
            try
            {
                results = RecipeScheduler.Run(recipeSpec.Recipe, sources, new ExecutionContext());
            }
            finally
            {
                WhitespaceReconciler.ThrowOnMismatchDefault = prevThrow;
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
        }
    }

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

    private static string PrintTree(SourceFile tree)
    {
        if (tree is Xml.Document)
        {
            var capture = new PrintOutputCapture<object>(null!);
            new XmlPrinter<object>().Visit(tree, capture);
            return capture.ToString();
        }
        return new CSharpPrinter<object>().Print(tree);
    }

    private static readonly CsprojParser CsprojParserInstance = new();

    private static bool IsCsprojPath(string path) => CsprojParserInstance.Accept(path);

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

    public RecipeSpec SetRecipe(Recipe recipe)
    {
        Recipe = recipe;
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
