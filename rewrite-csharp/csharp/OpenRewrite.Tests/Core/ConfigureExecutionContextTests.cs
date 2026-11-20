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
/// A recipe finds on the execution context what earlier parts of a run put there, such as the build files a
/// <see cref="DotNetBuildContext"/> captured, which a test puts there before the recipe runs.
/// </summary>
public class ConfigureExecutionContextTests : RewriteTest
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

    private static void CaptureNet6Project(ExecutionContext ctx) =>
        DotNetBuildContext.GetOrCreate(ctx).CaptureIfBuildFile(new OpenRewrite.Xml.XmlParser().Parse(
            "<Project Sdk=\"Microsoft.NET.Sdk\"><PropertyGroup><TargetFramework>net6.0</TargetFramework>" +
            "</PropertyGroup></Project>", "src/A/A.csproj"));

    [Fact]
    public void TheRecipeFindsTheBuildFilesTheTestCaptured()
    {
        RewriteRun(
            spec => spec
                .SetRecipe(new RenameCallsInProjectsTargeting("net6.0"))
                .ConfigureExecutionContext(CaptureNet6Project),
            CSharp(Calls("Foo"), Calls("Bar"))
        );
    }

    [Fact]
    public void WhatTheTestPutsThereDoesNotFailTheUnchangedContextCheck()
    {
        RewriteRun(
            spec => spec
                .SetRecipe(new RenameCallsInProjectsTargeting("net6.0"))
                .ConfigureExecutionContext(ctx => ctx.PutMessage("seeded", true)),
            CSharp(Calls("Foo"))
        );
    }

    [Fact]
    public void WhatTheRecipePutsThereStillFails()
    {
        var failure = Assert.Throws<FailException>(() => RewriteRun(
            spec => spec
                .SetRecipe(new RenameCallsInProjectsTargeting("net6.0", ctx => ctx.PutMessage("renamed", true)))
                .ConfigureExecutionContext(CaptureNet6Project),
            CSharp(Calls("Foo"), Calls("Bar"))
        ));

        Assert.Contains("put \"renamed\" on the ExecutionContext", failure.Message);
    }

    /// <summary>Renames calls to Foo when the build files of the run name the framework.</summary>
    private class RenameCallsInProjectsTargeting(string framework, Action<ExecutionContext>? onRename = null)
        : global::OpenRewrite.Core.Recipe
    {
        public override string DisplayName => "Rename calls in projects targeting a framework";
        public override string Description => "Renames calls to Foo when a captured build file targets the framework.";

        public override ITreeVisitor<ExecutionContext> GetVisitor() => new Visitor(framework, onRename);

        private class Visitor(string framework, Action<ExecutionContext>? onRename) : CSharpVisitor<ExecutionContext>
        {
            public override J VisitMethodInvocation(MethodInvocation method, ExecutionContext ctx)
            {
                var call = (MethodInvocation)base.VisitMethodInvocation(method, ctx);
                var targets = DotNetBuildContext.Get(ctx)?.Documents.Values
                    .Any(doc => Print(doc).Contains($">{framework}<")) == true;
                if (!targets || call.Name.SimpleName != "Foo")
                {
                    return call;
                }
                onRename?.Invoke(ctx);
                return call.WithName(call.Name.WithSimpleName("Bar"));
            }
            private static string Print(OpenRewrite.Xml.Document doc)
            {
                var capture = new PrintOutputCapture<int>(0);
                new OpenRewrite.Xml.XmlPrinter<int>().Visit(doc, capture);
                return capture.ToString();
            }
        }
    }
}
