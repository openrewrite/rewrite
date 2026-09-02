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
using System.Text.Json;
using OpenRewrite.Core;
using OpenRewrite.CSharp.Rpc;

namespace OpenRewrite.Tests.Rpc;

/// <summary>
/// Verifies that a NuGet install reports where its publish output landed, and that a fresh
/// server can load the bundle again from that directory alone — no feed, no NuGet cache —
/// activating only the bundle's own assemblies and resolving its dependencies through the
/// publish output's <c>Recipes.deps.json</c>.
/// </summary>
public class InstallRecipesFromPublishDirTest : IDisposable
{
    private readonly string _root;
    private readonly string _installDir;

    public InstallRecipesFromPublishDirTest()
    {
        _root = Path.Combine(Path.GetTempPath(), "rewrite-publish-dir-test",
            Guid.NewGuid().ToString("N")[..8]);
        _installDir = Path.Combine(_root, "install");
        var feed = Path.Combine(_root, "feed");
        Directory.CreateDirectory(feed);

        // Everything under _root (the packed projects and the server's install dir) restores from
        // the folder feed only, into a packages folder of its own rather than the user's cache.
        File.WriteAllText(Path.Combine(_root, "nuget.config"), """
            <?xml version="1.0" encoding="utf-8"?>
            <configuration>
              <config>
                <add key="globalPackagesFolder" value="packages" />
              </config>
              <packageSources>
                <clear />
                <add key="feed" value="feed" />
              </packageSources>
            </configuration>
            """);

        var hostDll = typeof(IRecipeActivator).Assembly.Location;

        // Dep: a transitive dependency of the bundle with an activator of its own.
        var depDir = Path.Combine(_root, "Dep");
        Directory.CreateDirectory(depDir);
        File.WriteAllText(Path.Combine(depDir, "Dep.csproj"), $"""
            <Project Sdk="Microsoft.NET.Sdk">
              <PropertyGroup>
                <TargetFramework>net10.0</TargetFramework>
                <Version>1.0.0</Version>
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

            namespace Dep;

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

        // Primary: the bundle itself, composing Dep's recipe.
        var primaryDir = Path.Combine(_root, "Primary");
        Directory.CreateDirectory(primaryDir);
        File.WriteAllText(Path.Combine(primaryDir, "Primary.csproj"), $"""
            <Project Sdk="Microsoft.NET.Sdk">
              <PropertyGroup>
                <TargetFramework>net10.0</TargetFramework>
                <Version>1.0.0</Version>
              </PropertyGroup>
              <ItemGroup>
                <Reference Include="OpenRewrite">
                  <HintPath>{hostDll}</HintPath>
                  <Private>false</Private>
                </Reference>
                <PackageReference Include="Dep" Version="1.0.0" />
              </ItemGroup>
            </Project>
            """);
        File.WriteAllText(Path.Combine(primaryDir, "Primary.cs"), """
            using System.Collections.Generic;
            using Dep;
            using OpenRewrite.Core;
            using OpenRewrite.CSharp;
            using OpenRewrite.Java;

            namespace Primary;

            public class PrimaryActivator : IRecipeActivator
            {
                public void Activate(RecipeMarketplace marketplace) =>
                    marketplace.Install(new PrimaryRecipe(), new CategoryDescriptor("Primary"));
            }

            public class PrimaryRecipe : Recipe
            {
                public override string DisplayName => "Primary composite recipe";
                public override string Description => "Composes a recipe from a referenced package.";
                public override List<Recipe> GetRecipeList() => [new DepRecipe()];
            }
            """);

        RunDotnet($"pack \"{Path.Combine(depDir, "Dep.csproj")}\" -c Release -o \"{feed}\"");
        RunDotnet($"pack \"{Path.Combine(primaryDir, "Primary.csproj")}\" -c Release -o \"{feed}\"");
    }

    public void Dispose()
    {
        try { Directory.Delete(_root, true); }
        catch { /* best-effort cleanup */ }
    }

    [Fact]
    public void NuGetInstallReportsPublishDirThatAFreshServerReloadsWithoutAFeed()
    {
        var installed = new RewriteRpcServer(new RecipeMarketplace(), _installDir)
            .InstallRecipes(new InstallRecipesRequest
            {
                Recipes = JsonSerializer.SerializeToElement(new { packageName = "Primary", version = "1.0.0" })
            })
            .GetAwaiter().GetResult();

        Assert.Equal(1, installed.RecipesInstalled);
        Assert.Equal("1.0.0", installed.Version);
        Assert.NotNull(installed.PublishDir);
        Assert.StartsWith(_installDir, installed.PublishDir);
        Assert.True(File.Exists(Path.Combine(installed.PublishDir, "Dep.dll")),
            "the transitive dependency is published beside the bundle's own assembly");

        // A fresh server with no install dir and no feed: the publish dir is all it gets.
        var marketplace = new RecipeMarketplace();
        var server = new RewriteRpcServer(marketplace);
        var reloaded = server.InstallRecipes(new InstallRecipesRequest { Recipes = installed.PublishDir })
            .GetAwaiter().GetResult();

        Assert.Equal(1, reloaded.RecipesInstalled);
        Assert.Equal("1.0.0", reloaded.Version);
        Assert.Equal(installed.PublishDir, reloaded.PublishDir);
        var names = marketplace.AllRecipes().Select(r => r.Name).ToHashSet();
        Assert.Contains("Primary.PrimaryRecipe", names);
        Assert.DoesNotContain("Dep.DepRecipe", names);

        // The composite's child comes from Dep.dll, resolved through Recipes.deps.json.
        var prepared = server.PrepareRecipe(new PrepareRecipeRequest { Id = "Primary.PrimaryRecipe" })
            .GetAwaiter().GetResult();
        Assert.Single(prepared.RecipeList);
        Assert.Equal("Dependency recipe", prepared.RecipeList[0].Descriptor.DisplayName);

        var row = server.GetMarketplace().GetAwaiter().GetResult()
            .Single(r => r.Descriptor.Name == "Primary.PrimaryRecipe");
        Assert.Equal(installed.PublishDir, row.PackageName);

        // The package form is what the Java side sends for a path that exists; a directory works there too.
        var byPackageForm = new RewriteRpcServer(new RecipeMarketplace())
            .InstallRecipes(new InstallRecipesRequest
            {
                Recipes = JsonSerializer.SerializeToElement(new { packageName = installed.PublishDir, version = (string?)null })
            })
            .GetAwaiter().GetResult();
        Assert.Equal(1, byPackageForm.RecipesInstalled);
        Assert.Equal("1.0.0", byPackageForm.Version);
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
