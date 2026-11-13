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
using OpenRewrite.CSharp;
using OpenRewrite.Java;
using OpenRewrite.Test;
using OpenRewrite.Xml;
using Xunit.Sdk;
using ExecutionContext = OpenRewrite.Core.ExecutionContext;

namespace OpenRewrite.Tests.Core;

/// <summary>
/// The host sends a C# recipe only the C# and XML files it parsed, so the harness runs a recipe without the first file
/// of each kind it changed too, and fails it if it throws there.
/// </summary>
public class UnparsedSourcesTests : RewriteTest
{
    private static SourceSpec[] Project() =>
    [
        CsProj(
            """
            <Project>
              <PropertyGroup>
                <RootNamespace>App</RootNamespace>
              </PropertyGroup>
            </Project>
            """,
            """
            <!--~~(root namespace App)~~>--><Project>
              <PropertyGroup>
                <RootNamespace>App</RootNamespace>
              </PropertyGroup>
            </Project>
            """,
            sourcePath: "Directory.Build.props"),
        CSharp(
            """
            class C
            {
            }
            """,
            """
            /*~~(in App)~~>*/class C
            {
            }
            """)
    ];

    [Fact]
    public void ARecipeThatReadsWhatItsScannerNeverRecordedFails()
    {
        var failure = Assert.Throws<XunitException>(() => RewriteRun(
            spec => spec.SetRecipe(new FindRootNamespaces { ChecksWhatItsScannerFound = false }),
            Project()
        ));

        Assert.StartsWith(
            "Check that the scanner found the files the recipe needs before using what it keeps of them, so that it " +
            "changes nothing when one is missing.\n" +
            "OpenRewrite.Tests.Core.FindRootNamespaces threw when Directory.Build.props was missing, which is " +
            "how a C# recipe sees a file its parser failed on:\n" +
            "    System.Collections.Generic.KeyNotFoundException: The given key '' was not present in the dictionary.",
            failure.Message);
        Assert.Contains(
            "\n        at OpenRewrite.Tests.Core.FindRootNamespaces.ClassMarker.VisitClassDeclaration(" +
            "UnparsedSourcesTests.cs:",
            failure.Message);
        Assert.Contains(
            "The host sends a C# recipe only the C# and XML files it parsed, so to the recipe such a file is simply " +
            "not there.\n",
            failure.Message);
        Assert.EndsWith("  - don't catch the exception to make this pass", failure.Message);
        Assert.IsType<KeyNotFoundException>(failure.InnerException);
    }

    [Fact]
    public void ARecipeThatChecksWhatItsScannerFoundPasses()
    {
        RewriteRun(
            spec => spec.SetRecipe(new FindRootNamespaces { ChecksWhatItsScannerFound = true }),
            Project()
        );
    }

    /// <summary>A probe of a recipe with its options runs once in the process, whether it passes or fails.</summary>
    [Fact]
    public void AProbeRunsOncePerProcess()
    {
        Assert.Throws<XunitException>(() => RewriteRun(
            spec => spec.SetRecipe(new FindRootNamespacesOfOneTest()),
            Project()
        ));

        RewriteRun(
            spec => spec.SetRecipe(new FindRootNamespacesOfOneTest()),
            Project()
        );
    }

    /// <summary>A recipe declared inside a test class is scaffolding for the test, so it isn't probed.</summary>
    [Fact]
    public void AHelperRecipeOfATestClassIsNotProbed()
    {
        RewriteRun(
            spec => spec.SetRecipe(new HelperFindRootNamespaces()),
            Project()
        );
    }

    /// <summary>A recipe that runs a helper recipe of a test class is scaffolding too.</summary>
    [Fact]
    public void ARecipeThatRunsAHelperRecipeOfATestClassIsNotProbed()
    {
        RewriteRun(
            spec => spec.SetRecipe(new RunsAHelperRecipe()),
            Project()
        );
    }

    /// <summary>A test's helper that reads what its scanner never recorded.</summary>
    internal class HelperFindRootNamespaces : FindRootNamespaces;
}

/// <summary>
/// Marks the project file that declares a root namespace, and each class in its directory with that namespace,
/// trusting its scanner to have found the project file unless it checks.
/// </summary>
internal class FindRootNamespaces : ScanningRecipe<Dictionary<string, string>>
{
    [Option(DisplayName = "Checks what its scanner found",
        Description = "Whether a class whose project file the scanner didn't find is left alone.")]
    public bool ChecksWhatItsScannerFound { get; init; }

    public override string DisplayName => "Find root namespaces";
    public override string Description => "Marks each class with the root namespace its project declares.";

    public override Dictionary<string, string> GetInitialValue(ExecutionContext ctx) => [];

    public override ITreeVisitor<ExecutionContext> GetScanner(Dictionary<string, string> rootNamespaces) =>
        new ProjectScanner(rootNamespaces);

    public override ITreeVisitor<ExecutionContext> GetVisitor(Dictionary<string, string> rootNamespaces) =>
        new ProjectMarker(rootNamespaces).Combine(new ClassMarker(rootNamespaces, ChecksWhatItsScannerFound));

    private static string DirectoryOf(SourceFile sourceFile) => Path.GetDirectoryName(sourceFile.SourcePath) ?? "";

    private class ProjectScanner(Dictionary<string, string> rootNamespaces) : XmlVisitor<ExecutionContext>
    {
        public override global::OpenRewrite.Xml.Xml VisitDocument(Document document, ExecutionContext ctx)
        {
            if (document.Root.GetChild("PropertyGroup")?.GetChild("RootNamespace")?.GetValue() is { } rootNamespace)
            {
                rootNamespaces[DirectoryOf(document)] = rootNamespace;
            }
            return document;
        }
    }

    private class ProjectMarker(Dictionary<string, string> rootNamespaces) : XmlVisitor<ExecutionContext>
    {
        public override global::OpenRewrite.Xml.Xml VisitDocument(Document document, ExecutionContext ctx) =>
            rootNamespaces.TryGetValue(DirectoryOf(document), out var rootNamespace)
                ? SearchResult.Found(document, $"root namespace {rootNamespace}")
                : document;
    }

    private class ClassMarker(Dictionary<string, string> rootNamespaces, bool checks)
        : CSharpVisitor<ExecutionContext>
    {
        public override J VisitClassDeclaration(ClassDeclaration classDecl, ExecutionContext ctx)
        {
            var directory = DirectoryOf(Cursor.FirstEnclosingOrThrow<CompilationUnit>());
            if (checks && !rootNamespaces.ContainsKey(directory))
            {
                return classDecl;
            }
            return SearchResult.Found(classDecl, $"in {rootNamespaces[directory]}");
        }
    }
}

/// <summary>A recipe that no other test runs, so that its first probe is this test's.</summary>
internal class FindRootNamespacesOfOneTest : FindRootNamespaces;

/// <summary>Runs the helper recipe that UnparsedSourcesTests declares.</summary>
internal class RunsAHelperRecipe : global::OpenRewrite.Core.Recipe
{
    public override string DisplayName => "Run a helper recipe";
    public override string Description => "Runs the recipe a test class declares to find root namespaces.";

    public override List<global::OpenRewrite.Core.Recipe> GetRecipeList() =>
        [new UnparsedSourcesTests.HelperFindRootNamespaces()];
}
