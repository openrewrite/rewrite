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
using Microsoft.CodeAnalysis;
using Microsoft.CodeAnalysis.CSharp;
using OpenRewrite.CSharp;
using OpenRewrite.Java;

namespace OpenRewrite.Tests.CSharp;

public class ConditionalBranchAttributionTests
{
    [Fact]
    public void AttributesCodePresentOnlyInANonPrimaryBranch()
    {
        const string source = """
            #if !FEATURE
            using System.Text;
            class Example { object Create() => Encoding.GetEncoding(1252); }
            #endif
            """;
        var calls = ParseCalls(source);
        var call = Assert.Single(calls);
        Assert.Equal("System.Text.Encoding", TypeUtils.GetFullyQualifiedName(call.MethodType?.DeclaringType));
    }

    [Fact]
    public void ResolvesEachBranchWithItsOwnAliases()
    {
        const string source = """
            #if FEATURE
            using Selected = System.Text.Encoding;
            #else
            using Selected = CustomEncoding;
            #endif
            class CustomEncoding { public static object GetEncoding(int value) => null; }
            class Example { object Create() => Selected.GetEncoding(1252); }
            """;
        var calls = ParseCalls(source);
        Assert.Equal(2, calls.Count);
        var declaringTypes = calls.Select(call => TypeUtils.GetFullyQualifiedName(call.MethodType?.DeclaringType)).ToHashSet();
        Assert.True(declaringTypes.SetEquals(["System.Text.Encoding", "CustomEncoding"]));
    }

    [Fact]
    public void KeepsDependenciesAndParseOptionsForAllBranches()
    {
        const string source = """
            #if FEATURE
            class Example { object Create() => Dependency.Create(); }
            #else
            class Example { object Create() => Dependency.Create(); object Other() => Dependency.Create(); }
            #endif
            """;
        var calls = ParseCalls(source, "class Dependency { public static object Create() => null; }");
        Assert.Equal(3, calls.Count);
        Assert.All(calls, call => Assert.Equal("Dependency", TypeUtils.GetFullyQualifiedName(call.MethodType?.DeclaringType)));
    }

    [Fact]
    public void LeavesBranchesUnattributedWhenNoCompilationIsSupplied()
    {
        const string source = """
            #if FEATURE
            class Example { object Create() => Unknown.Create(); }
            #else
            class Example { object Other() => Unknown.Create(); }
            #endif
            """;
        var unit = new CSharpParser().Parse(source);
        var calls = new List<MethodInvocation>();
        new Calls().Visit(unit, calls);
        Assert.Equal(2, calls.Count);
        Assert.All(calls, call => Assert.Null(call.MethodType));
    }

    private static List<MethodInvocation> ParseCalls(string source, string? dependency = null)
    {
        var options = new CSharpParseOptions(LanguageVersion.CSharp13, preprocessorSymbols: ["FEATURE"]);
        var tree = CSharpSyntaxTree.ParseText(source, options, path: "Example.cs");
        var trees = new List<SyntaxTree> { tree };
        if (dependency != null)
            trees.Add(CSharpSyntaxTree.ParseText(dependency, options, path: "Dependency.cs"));
        var references = ((string)AppContext.GetData("TRUSTED_PLATFORM_ASSEMBLIES")!).Split(Path.PathSeparator)
            .Select(path => MetadataReference.CreateFromFile(path));
        var compilation = CSharpCompilation.Create("BranchTest", trees, references);
        var unit = new CSharpParser().Parse(source, semanticModel: compilation.GetSemanticModel(tree));
        var calls = new List<MethodInvocation>();
        new Calls().Visit(unit, calls);
        Assert.Equal(source, new CSharpPrinter<int>().Print(unit));
        return calls;
    }

    private class Calls : CSharpVisitor<List<MethodInvocation>>
    {
        public override J VisitMethodInvocation(MethodInvocation invocation, List<MethodInvocation> calls)
        {
            calls.Add(invocation);
            return base.VisitMethodInvocation(invocation, calls);
        }
    }
}
