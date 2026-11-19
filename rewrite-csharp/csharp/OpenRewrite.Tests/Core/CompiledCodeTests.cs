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
using Xunit.Sdk;
using ExecutionContext = OpenRewrite.Core.ExecutionContext;

namespace OpenRewrite.Tests.Core;

/// <summary>
/// A test of a recipe that must not change what code does runs the code before and after the recipe and compares.
/// </summary>
public class CompiledCodeTests : RewriteTest
{
    private const string Values =
        """
        public class Values
        {
            int _offset = 10;
            static int Foo(int x) => x + 1;
            static int Bar(int x) => x + 1;
            public static int Twice(int x) => Foo(x) * 2;
            int Shifted(int x) => Foo(x) + _offset;
            static int Fail() => throw new System.InvalidOperationException("from the code");
        }
        """;

    [Fact]
    public void TheInputAndTheOutputDoTheSame()
    {
        var results = new List<object?>();
        void Run(Microsoft.CodeAnalysis.CSharp.CSharpCompilation compilation)
        {
            results.Add(CompiledCode.Invoke(compilation, "Values", "Twice", 4));
            results.Add(CompiledCode.Invoke(compilation, "Values", "Shifted", 4));
        }

        RewriteRun(
            spec => spec
                .SetRecipe(new RenameCalls("Foo", "Bar"))
                .ExpectInputCompiles(Run)
                .ExpectOutputCompiles(inspect: Run),
            CSharp(Values, Values.Replace("=> Foo(x)", "=> Bar(x)"))
        );

        Assert.Equal([10, 15, 10, 15], results);
    }

    [Fact]
    public void WhatTheCodeThrowsTheCallThrows()
    {
        RewriteRun(
            spec => spec.ExpectInputCompiles(compilation =>
                Assert.Equal("from the code", Assert.Throws<InvalidOperationException>(() =>
                    CompiledCode.Invoke(compilation, "Values", "Fail")).Message)),
            CSharp(Values)
        );
    }

    [Fact]
    public void AProjectRunsWithTheProjectsItReferences()
    {
        RewriteRun(
            spec => spec.ExpectInputCompiles(compilation =>
            {
                if (compilation.AssemblyName == "App")
                {
                    Assert.Equal(42, CompiledCode.Invoke(compilation, "App", "M"));
                }
            }),
            CSharp("public static class App { public static int M() => Lib.Answer(); }").InProject("App", "Lib"),
            CSharp("public static class Lib { public static int Answer() => 42; }").InProject("Lib")
        );
    }

    [Fact]
    public void AnUnknownMethodNamesTheTypeAndArity()
    {
        RewriteRun(
            spec => spec.ExpectInputCompiles(compilation =>
                Assert.Equal("Values declares no methods Missing of 0 parameters.",
                    Assert.Throws<ArgumentException>(() => CompiledCode.Invoke(compilation, "Values", "Missing"))
                        .Message)),
            CSharp(Values)
        );
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
                var call = (MethodInvocation)base.VisitMethodInvocation(method, ctx);
                return call.Name.SimpleName == from ? call.WithName(call.Name.WithSimpleName(to)) : call;
            }
        }
    }
}
