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
using System.Diagnostics;
using OpenRewrite.Core;
using OpenRewrite.CSharp.Rpc;

namespace OpenRewrite.Tests.Rpc;

/// <summary>
/// Verifies strict isolation when a recipe package is installed as a loose DLL: only the
/// primary assembly's own recipes are registered into the marketplace. Referenced dependency
/// assemblies are NOT activated at install time; cross-package composition is resolved by binary
/// integration at recipe-tree preparation time instead.
/// </summary>
public class InstallRecipesDependencyActivationTest : IDisposable
{
    private readonly string _root;
    private readonly string _publishDir;

    public InstallRecipesDependencyActivationTest()
    {
        _root = Path.Combine(Path.GetTempPath(), "rewrite-dep-activation-test",
            Guid.NewGuid().ToString("N")[..8]);
        Directory.CreateDirectory(_root);

        var hostDll = typeof(IRecipeActivator).Assembly.Location;

        // Dependency package: contributes DepRecipe via its own activator.
        var depDir = Path.Combine(_root, "DepPlugin");
        Directory.CreateDirectory(depDir);
        File.WriteAllText(Path.Combine(depDir, "DepPlugin.csproj"), $"""
            <Project Sdk="Microsoft.NET.Sdk">
              <PropertyGroup>
                <TargetFramework>net10.0</TargetFramework>
              </PropertyGroup>
              <ItemGroup>
                <Reference Include="OpenRewrite">
                  <HintPath>{hostDll}</HintPath>
                  <Private>false</Private>
                </Reference>
              </ItemGroup>
            </Project>
            """);
        File.WriteAllText(Path.Combine(depDir, "Dep.cs"), """
            using OpenRewrite.Core;
            using OpenRewrite.CSharp;
            using OpenRewrite.Java;

            namespace DepPlugin;

            public class DepActivator : IRecipeActivator
            {
                public void Activate(RecipeMarketplace marketplace) =>
                    marketplace.Install(new DepRecipe(), new CategoryDescriptor("Dep"));
            }

            public class DepRecipe : Recipe
            {
                public override string DisplayName => "Dependency recipe";
                public override string Description => "A recipe contributed by a referenced package.";
                public override JavaVisitor<ExecutionContext> GetVisitor() =>
                    new CSharpVisitor<ExecutionContext>();
            }
            """);

        // Primary package: composes DepRecipe (so the compiled assembly retains the reference)
        // but only activates its own PrimaryRecipe.
        var primaryDir = Path.Combine(_root, "PrimaryPlugin");
        Directory.CreateDirectory(primaryDir);
        File.WriteAllText(Path.Combine(primaryDir, "PrimaryPlugin.csproj"), $"""
            <Project Sdk="Microsoft.NET.Sdk">
              <PropertyGroup>
                <TargetFramework>net10.0</TargetFramework>
              </PropertyGroup>
              <ItemGroup>
                <Reference Include="OpenRewrite">
                  <HintPath>{hostDll}</HintPath>
                  <Private>false</Private>
                </Reference>
                <ProjectReference Include="../DepPlugin/DepPlugin.csproj" />
              </ItemGroup>
            </Project>
            """);
        File.WriteAllText(Path.Combine(primaryDir, "Primary.cs"), """
            using System.Collections.Generic;
            using DepPlugin;
            using OpenRewrite.Core;
            using OpenRewrite.CSharp;
            using OpenRewrite.Java;

            namespace PrimaryPlugin;

            public class PrimaryActivator : IRecipeActivator
            {
                public void Activate(RecipeMarketplace marketplace) =>
                    marketplace.Install(new PrimaryRecipe(), new CategoryDescriptor("Primary"));
            }

            public class PrimaryRecipe : Recipe
            {
                public override string DisplayName => "Primary composite recipe";
                public override string Description => "Composes a recipe from a referenced package.";

                // Referencing DepRecipe keeps DepPlugin in this assembly's reference graph.
                public override List<Recipe> GetRecipeList() => [new DepRecipe()];
            }
            """);

        _publishDir = Path.Combine(_root, "publish");
        RunDotnet($"publish \"{Path.Combine(primaryDir, "PrimaryPlugin.csproj")}\" " +
                  $"-c Release -o \"{_publishDir}\"");
    }

    public void Dispose()
    {
        try { Directory.Delete(_root, true); }
        catch { /* best-effort cleanup */ }
    }

    [Fact]
    public void InstallRecipes_FromLooseDll_RegistersOnlyPrimaryAssemblyRecipes()
    {
        var primaryDll = Path.Combine(_publishDir, "PrimaryPlugin.dll");
        Assert.True(File.Exists(primaryDll), $"Primary plugin DLL not found at {primaryDll}");
        Assert.True(File.Exists(Path.Combine(_publishDir, "DepPlugin.dll")),
            "DepPlugin.dll must be published as a private dependency of the primary plugin");

        var marketplace = new RecipeMarketplace();
        var server = new RewriteRpcServer(marketplace);

        server.InstallRecipes(new InstallRecipesRequest { Recipes = primaryDll })
            .GetAwaiter().GetResult();

        var names = marketplace.AllRecipes().Select(r => r.Name).ToHashSet();

        // The installed artifact contributes only its own recipe.
        Assert.Contains("PrimaryPlugin.PrimaryRecipe", names);
        // Strict isolation: the referenced package's recipe is NOT registered into the
        // marketplace by installing the dependent. It is resolved by binary integration
        // at composition time instead (see PrepareRecipeTreeTest).
        Assert.DoesNotContain("DepPlugin.DepRecipe", names);
    }

    private static void RunDotnet(string arguments)
    {
        var psi = new ProcessStartInfo("dotnet", arguments)
        {
            RedirectStandardOutput = true,
            RedirectStandardError = true,
            UseShellExecute = false,
            CreateNoWindow = true
        };

        using var process = Process.Start(psi)
                            ?? throw new InvalidOperationException("Failed to start dotnet process");

        var stdout = process.StandardOutput.ReadToEnd();
        var stderr = process.StandardError.ReadToEnd();
        process.WaitForExit();

        if (process.ExitCode != 0)
        {
            throw new InvalidOperationException(
                $"dotnet {arguments} failed (exit code {process.ExitCode}):\n{stderr}\n{stdout}");
        }
    }
}
