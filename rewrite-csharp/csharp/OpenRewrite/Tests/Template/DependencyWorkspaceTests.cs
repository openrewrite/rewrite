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
using OpenRewrite.CSharp;
using OpenRewrite.CSharp.Template;
using OpenRewrite.Java;
using OpenRewrite.Test;

namespace OpenRewrite.Tests.Template;

public class DependencyWorkspaceTests : RewriteTest
{
    private static readonly Dictionary<string, string> NewtonsoftDep = new()
    {
        ["Newtonsoft.Json"] = "13.0.3"
    };

    [Fact]
    public void TemplateWithDependencyHasTypeAttribution()
    {
        var tmpl = CSharpTemplate.Create(
            "Newtonsoft.Json.JsonConvert.SerializeObject(\"hello\")",
            usings: ["Newtonsoft.Json"],
            dependencies: NewtonsoftDep);

        var tree = tmpl.GetTree();

        var mi = Assert.IsType<MethodInvocation>(tree);
        Assert.NotNull(mi.MethodType);
        Assert.Equal("SerializeObject", mi.MethodType.Name);
        var declaringType = Assert.IsType<JavaType.Class>(mi.MethodType.DeclaringType);
        Assert.Equal("Newtonsoft.Json.JsonConvert", declaringType.FullyQualifiedName);
    }

    [Fact]
    public void TemplateWithoutDependencyLacksTypeAttribution()
    {
        // Same code but no dependencies — parser can't resolve the type
        var tmpl = CSharpTemplate.Create(
            "Newtonsoft.Json.JsonConvert.SerializeObject(\"hello\")",
            usings: ["Newtonsoft.Json"]);

        var tree = tmpl.GetTree();

        var mi = Assert.IsType<MethodInvocation>(tree);
        // Without the NuGet dependency, no type attribution is possible
        Assert.Null(mi.MethodType);
    }

    [Fact]
    public void PatternWithDependencyParsesExternalType()
    {
        // The placeholder argument prevents full overload resolution,
        // but the parse should still succeed and recognize the method invocation
        var expr = Capture.Of<Expression>("expr");
        var pat = CSharpPattern.Create(
            $"Newtonsoft.Json.JsonConvert.SerializeObject({expr})",
            usings: ["Newtonsoft.Json"],
            dependencies: NewtonsoftDep);

        var patternTree = pat.GetTree();
        Assert.IsType<MethodInvocation>(patternTree);
    }

    [Fact]
    public void PatternWithTypedCaptureHasTypeAttribution()
    {
        // A typed capture gives the placeholder proper type attribution,
        // enabling Roslyn to resolve the method overload
        var expr = Capture.Expression("expr", type: "object");
        var pat = CSharpPattern.Create(
            $"Newtonsoft.Json.JsonConvert.SerializeObject({expr})",
            usings: ["Newtonsoft.Json"],
            dependencies: NewtonsoftDep);

        var patternTree = pat.GetTree();

        var mi = Assert.IsType<MethodInvocation>(patternTree);
        Assert.NotNull(mi.MethodType);
        Assert.Equal("SerializeObject", mi.MethodType.Name);
    }

    [Fact]
    public void TemplateWithoutDependenciesStillWorks()
    {
        var tmpl = CSharpTemplate.Create("1 + 2");
        var tree = tmpl.GetTree();
        Assert.IsType<Binary>(tree);
    }
}
