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
using System.Reflection;
using Microsoft.CodeAnalysis;
using Microsoft.CodeAnalysis.CSharp;
using OpenRewrite.Core;
using OpenRewrite.CSharp;
using OpenRewrite.Java;
using OpenRewrite.Test;
using Xunit.Sdk;
using ExecutionContext = OpenRewrite.Core.ExecutionContext;

namespace OpenRewrite.Tests.Core;

/// <summary>
/// A migration has to leave code that compiles, often against the newer framework it migrates to, and the types the
/// recipe sees are only right for input that compiles.
/// </summary>
public class CompilesTests : RewriteTest
{
    private static string Calls(string name) =>
        $$"""
          class C
          {
              void Foo() { }
              void Bar() { }
              void M()
              {
                  {{name}}();
              }
          }
          """;

    private const string Remoting =
        """
        class C
        {
            bool Foo(object o) => System.Runtime.Remoting.RemotingServices.IsTransparentProxy(o);
            bool Bar(object o) => false;
            bool M(object o) => Foo(o);
        }
        """;

    [Fact]
    public void OutputThatCompilesPasses()
    {
        RewriteRun(
            spec => spec.SetRecipe(new RenameCalls("Foo", "Bar")).ExpectOutputCompiles(),
            CSharp(Calls("Foo"), Calls("Bar"))
        );
    }

    [Fact]
    public void OutputThatDoesNotCompileFails()
    {
        var failure = Assert.Throws<FailException>(() => RewriteRun(
            spec => spec.SetRecipe(new RenameCalls("Foo", "Missing")).ExpectOutputCompiles(),
            CSharp(Calls("Foo"), Calls("Missing"))
        ));

        Assert.Equal(
            "The recipe's output doesn't compile against net9.0:\n" +
            "  source.cs(7,9): error CS0103: The name 'Missing' does not exist in the current context\n" +
            "source.cs:\n" + Calls("Missing"),
            failure.Message);
    }

    [Fact]
    public void OutputCompilesAgainstTheFrameworkTheRecipeMigratesTo()
    {
        var inspected = false;
        RewriteRun(
            spec => spec
                .SetRecipe(new RenameCalls("Foo", "Bar"))
                .SetReferenceAssemblies(Assemblies.Net48)
                .ExpectInputCompiles()
                .ExpectOutputCompiles(Assemblies.Net90, compilation =>
                {
                    Assert.NotNull(compilation.GetTypeByMetadataName("System.HashCode"));
                    inspected = true;
                }),
            CSharp(Calls("Foo"), Calls("Bar"))
        );
        Assert.True(inspected);
    }

    [Fact]
    public void OutputThatOnlyCompilesAgainstTheInputFrameworkFails()
    {
        var failure = Assert.Throws<FailException>(() => RewriteRun(
            spec => spec
                .SetRecipe(new RenameCalls("Foo", "Bar"))
                .SetReferenceAssemblies(Assemblies.Net48)
                .ExpectInputCompiles()
                .ExpectOutputCompiles()
                .ExpectOutputCompiles(Assemblies.Net90),
            CSharp(Remoting, Remoting.Replace("=> Foo(o)", "=> Bar(o)"))
        ));

        Assert.Equal(
            "The recipe's output doesn't compile against net9.0:\n" +
            "  source.cs(3,27): error CS0234: The type or namespace name 'RemotingServices' does not exist in the " +
            "namespace 'System.Runtime.Remoting' (are you missing an assembly reference?)\n" +
            "source.cs:\n" + Remoting.Replace("=> Foo(o)", "=> Bar(o)"),
            failure.Message);
    }

    [Fact]
    public void ExpectOutputCompilesWithoutARecipeCompilesTheInput()
    {
        Assert.Throws<FailException>(() => RewriteRun(
            spec => spec.SetReferenceAssemblies(Assemblies.Net48).ExpectOutputCompiles(Assemblies.Net90),
            CSharp(Remoting)
        ));
    }

    [Fact]
    public void InputThatDoesNotCompileFails()
    {
        var failure = Assert.Throws<FailException>(() => RewriteRun(
            spec => spec.SetRecipe(new RenameCalls("Foo", "Bar")).ExpectInputCompiles(),
            CSharp(Calls("Baz"))
        ));

        Assert.Equal(
            "The sources of the test don't compile against net9.0, so types the recipe looks at are missing or " +
            "wrong:\n" +
            "  source.cs(7,9): error CS0103: The name 'Baz' does not exist in the current context\n" +
            "source.cs:\n" + Calls("Baz"),
            failure.Message);
    }

    [Fact]
    public void CompileChecksNeedReferenceAssemblies()
    {
        Assert.Throws<ArgumentException>(() => RewriteRun(
            spec => spec.SetReferenceAssemblies(null).ExpectInputCompiles(),
            CSharp(Calls("Foo"))
        ));
        Assert.Throws<ArgumentException>(() => RewriteRun(
            spec => spec.SetReferenceAssemblies(null).ExpectOutputCompiles(),
            CSharp(Calls("Foo"))
        ));
    }

    [Fact]
    public void WarningsFailTheOutputCompilationThatTreatsThemAsErrors()
    {
        var source = Calls("Foo").Replace("void Bar() { }", "void Bar() { int unused; }");
        var failure = Assert.Throws<FailException>(() => RewriteRun(
            spec => spec
                .SetRecipe(new RenameCalls("Foo", "Bar"))
                .ExpectOutputCompiles()
                .ExpectOutputCompiles(options: new CSharpCompilationOptions(OutputKind.DynamicallyLinkedLibrary,
                    generalDiagnosticOption: ReportDiagnostic.Error)),
            CSharp(source, source.Replace("Foo();", "Bar();"))
        ));

        Assert.Contains(
            "source.cs(4,22): error CS0168: The variable 'unused' is declared but never used", failure.Message);
    }

    [Fact]
    public void OutputCompilesAsAConsoleApp()
    {
        const string program =
            """
            class Program
            {
                static void Foo() { }
                static void Bar() { }
                static void Main() => Foo();
            }
            """;
        RewriteRun(
            spec => spec
                .SetRecipe(new RenameCalls("Foo", "Bar"))
                .ExpectOutputCompiles(options: new CSharpCompilationOptions(OutputKind.ConsoleApplication)),
            CSharp(program, program.Replace("=> Foo();", "=> Bar();"))
        );
    }

    [Fact]
    public void OutputCompilesWithTheLanguageVersionTheTestGives()
    {
        var source = Calls("Foo")
            .Replace("void Bar() { }", "void Bar() { }\n    bool Check(object o) => o is not null;");
        var failure = Assert.Throws<FailException>(() => RewriteRun(
            spec => spec
                .SetRecipe(new RenameCalls("Foo", "Bar"))
                .ExpectOutputCompiles(
                    parseOptions: CSharpParseOptions.Default.WithLanguageVersion(LanguageVersion.CSharp7_3)),
            CSharp(source, source.Replace("Foo();", "Bar();"))
        ));

        Assert.Contains("error CS8370: Feature 'not pattern' is not available in C# 7.3", failure.Message);
    }

    [Fact]
    public void TheInputAndOutputCompilationsShowWhatTheRecipeChangesInWhatTheCodeDoes()
    {
        const string values =
            """
            public static class Values
            {
                static int Foo() => 1;
                static int Bar() => 2;
                public static int M() => Foo();
            }
            """;
        var results = new List<object?>();
        RewriteRun(
            spec => spec
                .SetRecipe(new RenameCalls("Foo", "Bar"))
                .ExpectInputCompiles(compilation => results.Add(Run(compilation, "Values", "M")))
                .ExpectOutputCompiles(inspect: compilation => results.Add(Run(compilation, "Values", "M"))),
            CSharp(values, values.Replace("=> Foo();", "=> Bar();"))
        );

        Assert.Equal([1, 2], results);
    }

    [Fact]
    public void CompilationOptionsApplyToTheInput()
    {
        const string pointers = "unsafe class C { int* p; }";
        Assert.Throws<FailException>(() => RewriteRun(
            spec => spec.ExpectInputCompiles(),
            CSharp(pointers)
        ));
        RewriteRun(
            spec => spec
                .SetCompilationOptions(
                    new CSharpCompilationOptions(OutputKind.DynamicallyLinkedLibrary, allowUnsafe: true))
                .ExpectInputCompiles(),
            CSharp(pointers)
        );
    }

    [Fact]
    public void ParseOptionsApplyToTheInput()
    {
        var failure = Assert.Throws<FailException>(() => RewriteRun(
            spec => spec
                .SetParseOptions(CSharpParseOptions.Default.WithLanguageVersion(LanguageVersion.CSharp7_3))
                .ExpectInputCompiles(),
            CSharp("class C { bool M(object o) => o is not null; }")
        ));

        Assert.Contains("error CS8370: Feature 'not pattern' is not available in C# 7.3", failure.Message);
    }

    [Fact]
    public void TheSourcesCompileAgainstTheReferencesTheTestAdds()
    {
        var library = CSharpCompilation.Create("Library",
            [CSharpSyntaxTree.ParseText("public static class Library { public static int Foo() => 1; }")],
            ReferenceAssemblies(Assemblies.Net90),
            new CSharpCompilationOptions(OutputKind.DynamicallyLinkedLibrary));
        const string source = "class C { int M() => Library.Foo(); }";
        RewriteRun(
            spec => spec
                .AddReferences(library.ToMetadataReference())
                .ExpectInputCompiles()
                .ExpectOutputCompiles(Assemblies.Net100),
            CSharp(source).AfterRecipe(cu =>
            {
                var foo = FindFirst<MethodInvocation>(cu)!;
                Assert.Equal("Library", TypeUtils.GetFullyQualifiedName(foo.MethodType!.DeclaringType));
            })
        );
    }

    private static IEnumerable<MetadataReference> ReferenceAssemblies(
        Microsoft.CodeAnalysis.Testing.ReferenceAssemblies assemblies) =>
        assemblies.ResolveAsync(LanguageNames.CSharp, CancellationToken.None).GetAwaiter().GetResult();

    private static object? Run(CSharpCompilation compilation, string type, string method)
    {
        using var image = new MemoryStream();
        Assert.True(compilation.Emit(image).Success);
        return Assembly.Load(image.ToArray()).GetType(type)!.GetMethod(method)!.Invoke(null, null);
    }

    private class RenameCalls(string from, string to) : global::OpenRewrite.Core.Recipe
    {
        public override string DisplayName => "Rename calls";
        public override string Description => "Renames the calls to one method.";

        public override ITreeVisitor<ExecutionContext> GetVisitor() => new Visitor(from, to);

        private class Visitor(string from, string to) : CSharpVisitor<ExecutionContext>
        {
            public override J VisitMethodInvocation(MethodInvocation method, ExecutionContext ctx)
            {
                var visited = base.VisitMethodInvocation(method, ctx);
                return visited is MethodInvocation call && call.Name.SimpleName == from
                    ? call.WithName(call.Name.WithSimpleName(to))
                    : visited;
            }
        }
    }
}
