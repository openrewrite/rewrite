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
using OpenRewrite.CSharp.Rpc;

namespace OpenRewrite.Tests.Rpc;

/// <summary>
/// A recipe assembly compiled against a different OpenRewrite.CSharp than the host is refused at
/// install time, naming the APIs it uses that the host lacks, instead of failing every file with
/// a MissingMethodException once the recipe runs.
/// </summary>
public class HostApiSurfaceTest : IDisposable
{
    private readonly string _root;
    private readonly string _otherHostDll;
    private readonly string _pluginDll;

    public HostApiSurfaceTest()
    {
        _root = Path.Combine(Path.GetTempPath(), "rewrite-host-api-test", Guid.NewGuid().ToString("N")[..8]);

        var otherHostDir = Path.Combine(_root, "OtherHost");
        Directory.CreateDirectory(otherHostDir);
        File.WriteAllText(Path.Combine(otherHostDir, "OtherHost.csproj"), """
            <Project Sdk="Microsoft.NET.Sdk">
              <PropertyGroup>
                <TargetFramework>net10.0</TargetFramework>
                <AssemblyName>OpenRewrite</AssemblyName>
              </PropertyGroup>
            </Project>
            """);
        File.WriteAllText(Path.Combine(otherHostDir, "Api.cs"), """
            namespace OpenRewrite.Core
            {
                public class Markers { }

                public class TextComment
                {
                    public TextComment(string text, string suffix, bool multiline, Markers markers, int extra) { }
                }
            }

            namespace OpenRewrite.Other
            {
                public class Base<T>
                {
                    public void Inherited() { }
                    public virtual T Visit(T value) => value;
                }

                public class Derived : Base<string>
                {
                    public override string Visit(string value) => base.Visit(value);
                }
            }
            """);

        var pluginDir = Path.Combine(_root, "Plugin");
        Directory.CreateDirectory(pluginDir);
        File.WriteAllText(Path.Combine(pluginDir, "Plugin.csproj"), """
            <Project Sdk="Microsoft.NET.Sdk">
              <PropertyGroup>
                <TargetFramework>net10.0</TargetFramework>
              </PropertyGroup>
              <ItemGroup>
                <ProjectReference Include="../OtherHost/OtherHost.csproj" />
              </ItemGroup>
            </Project>
            """);
        File.WriteAllText(Path.Combine(pluginDir, "Uses.cs"), """
            using OpenRewrite.Core;
            using OpenRewrite.Other;

            namespace Plugin;

            public class Uses : Derived
            {
                public override string Visit(string value) => base.Visit(value);

                public object Run()
                {
                    Inherited();
                    return new TextComment("a", "b", false, null!, 1);
                }
            }
            """);

        var outputDir = Path.Combine(_root, "out");
        DotnetCli.Run($"build \"{Path.Combine(pluginDir, "Plugin.csproj")}\" -c Release -o \"{outputDir}\"");
        _otherHostDll = Path.Combine(outputDir, "OpenRewrite.dll");
        _pluginDll = Path.Combine(outputDir, "Plugin.dll");
    }

    public void Dispose()
    {
        try { Directory.Delete(_root, true); }
        catch (IOException) { }
    }

    [Fact]
    public void NothingIsMissingFromTheHostItWasCompiledAgainst()
    {
        Assert.Empty(HostApiSurface.Read(_otherHostDll).FindMissing(_pluginDll));
    }

    [Fact]
    public void ReportsTypesAndMembersTheHostLacks()
    {
        var missing = HostApiSurface.Read(typeof(RewriteRpcServer).Assembly.Location).FindMissing(_pluginDll);

        Assert.Equal(
            [
                "OpenRewrite.Core.TextComment..ctor(System.String, System.String, System.Boolean, OpenRewrite.Core.Markers, System.Int32)",
                "OpenRewrite.Other.Base`1",
                "OpenRewrite.Other.Derived"
            ],
            missing);
    }

    [Fact]
    public void AssemblyBuiltAgainstThisHostIsCompatible()
    {
        Assert.Empty(HostApiSurface.Read(typeof(RewriteRpcServer).Assembly.Location)
            .FindMissing(typeof(HostApiSurfaceTest).Assembly.Location));
    }

    [Fact]
    public void InstallRefusesAnIncompatibleRecipeAssembly()
    {
        var server = new RewriteRpcServer(new RecipeMarketplace());

        var error = Assert.Throws<InvalidOperationException>(() =>
            server.InstallRecipes(new InstallRecipesRequest { Recipes = _pluginDll }).GetAwaiter().GetResult());

        Assert.Equal(
            $"Recipe assembly {_pluginDll} cannot run on this Moderne CLI: Plugin was compiled against a different " +
            "version of OpenRewrite.CSharp than the one this CLI bundles, and uses APIs it does not have " +
            "(OpenRewrite.Core.TextComment..ctor(System.String, System.String, System.Boolean, OpenRewrite.Core.Markers, System.Int32); " +
            "OpenRewrite.Other.Base`1; OpenRewrite.Other.Derived). " +
            "Upgrade the Moderne CLI, or install a version of the recipe package that matches it.",
            error.Message);
    }
}
