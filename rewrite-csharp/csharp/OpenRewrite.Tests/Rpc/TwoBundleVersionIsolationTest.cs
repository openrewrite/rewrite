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
/// Verifies that two installed artifacts each resolve their composite child recipe against
/// their OWN bundled dependency version. When artifact A bundles DepPlugin v1 and artifact B
/// bundles DepPlugin v2 — both loaded in the same process — A's composite must see "child-v1"
/// and B's must see "child-v2" even though DepPlugin.ChildRecipe is the same FQN in both.
/// This proves the per-bundle AssemblyLoadContext isolation that whole-tree preparation relies on.
/// </summary>
public class TwoBundleVersionIsolationTest : IDisposable
{
    private readonly string _root;
    private readonly string _publishDirA;
    private readonly string _publishDirB;

    public TwoBundleVersionIsolationTest()
    {
        _root = Path.Combine(Path.GetTempPath(), "rewrite-two-bundle-test",
            Guid.NewGuid().ToString("N")[..8]);
        Directory.CreateDirectory(_root);

        // The host DLL (OpenRewrite.dll) is referenced by plugin projects with Private=false so
        // it is not copied into each publish dir — the PluginLoadContext picks it up from the
        // Default ALC, preserving type identity for Recipe, IRecipeActivator, etc.
        var hostDll = typeof(IRecipeActivator).Assembly.Location;

        // --- Dep v1: DepPlugin assembly whose ChildRecipe.DisplayName is "child-v1" ----------
        var depV1Dir = Path.Combine(_root, "DepV1");
        Directory.CreateDirectory(depV1Dir);
        File.WriteAllText(Path.Combine(depV1Dir, "DepV1.csproj"), $"""
            <Project Sdk="Microsoft.NET.Sdk">
              <PropertyGroup>
                <TargetFramework>net10.0</TargetFramework>
                <AssemblyName>DepPlugin</AssemblyName>
              </PropertyGroup>
              <ItemGroup>
                <Reference Include="OpenRewrite">
                  <HintPath>{hostDll}</HintPath>
                  <Private>false</Private>
                </Reference>
              </ItemGroup>
            </Project>
            """);
        File.WriteAllText(Path.Combine(depV1Dir, "Dep.cs"), """
            using OpenRewrite.Core;
            using OpenRewrite.CSharp;
            using OpenRewrite.Java;

            namespace DepPlugin;

            public class ChildRecipe : Recipe
            {
                public override string DisplayName => "child-v1";
                public override string Description => "Child recipe from dep v1.";
                public override JavaVisitor<ExecutionContext> GetVisitor() =>
                    new CSharpVisitor<ExecutionContext>();
            }
            """);

        // --- Dep v2: identical FQN (DepPlugin.ChildRecipe), different DisplayName -------------
        var depV2Dir = Path.Combine(_root, "DepV2");
        Directory.CreateDirectory(depV2Dir);
        File.WriteAllText(Path.Combine(depV2Dir, "DepV2.csproj"), $"""
            <Project Sdk="Microsoft.NET.Sdk">
              <PropertyGroup>
                <TargetFramework>net10.0</TargetFramework>
                <AssemblyName>DepPlugin</AssemblyName>
              </PropertyGroup>
              <ItemGroup>
                <Reference Include="OpenRewrite">
                  <HintPath>{hostDll}</HintPath>
                  <Private>false</Private>
                </Reference>
              </ItemGroup>
            </Project>
            """);
        File.WriteAllText(Path.Combine(depV2Dir, "Dep.cs"), """
            using OpenRewrite.Core;
            using OpenRewrite.CSharp;
            using OpenRewrite.Java;

            namespace DepPlugin;

            public class ChildRecipe : Recipe
            {
                public override string DisplayName => "child-v2";
                public override string Description => "Child recipe from dep v2.";
                public override JavaVisitor<ExecutionContext> GetVisitor() =>
                    new CSharpVisitor<ExecutionContext>();
            }
            """);

        // --- Primary A: distinct FQN PrimaryA.PrimaryRecipeA, composes DepPlugin v1 ----------
        var primaryADir = Path.Combine(_root, "PrimaryA");
        Directory.CreateDirectory(primaryADir);
        File.WriteAllText(Path.Combine(primaryADir, "PrimaryA.csproj"), $"""
            <Project Sdk="Microsoft.NET.Sdk">
              <PropertyGroup>
                <TargetFramework>net10.0</TargetFramework>
              </PropertyGroup>
              <ItemGroup>
                <Reference Include="OpenRewrite">
                  <HintPath>{hostDll}</HintPath>
                  <Private>false</Private>
                </Reference>
                <ProjectReference Include="../DepV1/DepV1.csproj" />
              </ItemGroup>
            </Project>
            """);
        File.WriteAllText(Path.Combine(primaryADir, "PrimaryA.cs"), """
            using System.Collections.Generic;
            using DepPlugin;
            using OpenRewrite.Core;
            using OpenRewrite.CSharp;
            using OpenRewrite.Java;

            namespace PrimaryA;

            public class PrimaryActivatorA : IRecipeActivator
            {
                public void Activate(RecipeMarketplace marketplace) =>
                    marketplace.Install(new PrimaryRecipeA(), new CategoryDescriptor("PrimaryA"));
            }

            public class PrimaryRecipeA : Recipe
            {
                public override string DisplayName => "Primary A";
                public override string Description => "Composite that bundles dep v1.";
                public override List<Recipe> GetRecipeList() => [new ChildRecipe()];
            }
            """);

        // --- Primary B: distinct FQN PrimaryB.PrimaryRecipeB, composes DepPlugin v2 ----------
        var primaryBDir = Path.Combine(_root, "PrimaryB");
        Directory.CreateDirectory(primaryBDir);
        File.WriteAllText(Path.Combine(primaryBDir, "PrimaryB.csproj"), $"""
            <Project Sdk="Microsoft.NET.Sdk">
              <PropertyGroup>
                <TargetFramework>net10.0</TargetFramework>
              </PropertyGroup>
              <ItemGroup>
                <Reference Include="OpenRewrite">
                  <HintPath>{hostDll}</HintPath>
                  <Private>false</Private>
                </Reference>
                <ProjectReference Include="../DepV2/DepV2.csproj" />
              </ItemGroup>
            </Project>
            """);
        File.WriteAllText(Path.Combine(primaryBDir, "PrimaryB.cs"), """
            using System.Collections.Generic;
            using DepPlugin;
            using OpenRewrite.Core;
            using OpenRewrite.CSharp;
            using OpenRewrite.Java;

            namespace PrimaryB;

            public class PrimaryActivatorB : IRecipeActivator
            {
                public void Activate(RecipeMarketplace marketplace) =>
                    marketplace.Install(new PrimaryRecipeB(), new CategoryDescriptor("PrimaryB"));
            }

            public class PrimaryRecipeB : Recipe
            {
                public override string DisplayName => "Primary B";
                public override string Description => "Composite that bundles dep v2.";
                public override List<Recipe> GetRecipeList() => [new ChildRecipe()];
            }
            """);

        // Publish each primary to its own flat directory. Each publish dir contains the primary
        // DLL + the dep DLL (DepPlugin.dll) from the corresponding dep version. The two
        // DepPlugin.dll files are identical in name/FQN but differ in content — that's the point.
        _publishDirA = Path.Combine(_root, "publishA");
        RunDotnet($"publish \"{Path.Combine(primaryADir, "PrimaryA.csproj")}\" " +
                  $"-c Release -o \"{_publishDirA}\"");

        _publishDirB = Path.Combine(_root, "publishB");
        RunDotnet($"publish \"{Path.Combine(primaryBDir, "PrimaryB.csproj")}\" " +
                  $"-c Release -o \"{_publishDirB}\"");
    }

    public void Dispose()
    {
        try { Directory.Delete(_root, true); }
        catch { /* best-effort cleanup */ }
    }

    [Fact]
    public void TwoBundles_ResolveTheirOwnDependencyVersion()
    {
        var primaryADll = Path.Combine(_publishDirA, "PrimaryA.dll");
        var primaryBDll = Path.Combine(_publishDirB, "PrimaryB.dll");

        Assert.True(File.Exists(primaryADll), $"PrimaryA DLL not found at {primaryADll}");
        Assert.True(File.Exists(primaryBDll), $"PrimaryB DLL not found at {primaryBDll}");
        Assert.True(File.Exists(Path.Combine(_publishDirA, "DepPlugin.dll")),
            "DepPlugin.dll (v1) must be published alongside PrimaryA");
        Assert.True(File.Exists(Path.Combine(_publishDirB, "DepPlugin.dll")),
            "DepPlugin.dll (v2) must be published alongside PrimaryB");

        var marketplace = new RecipeMarketplace();
        var server = new RewriteRpcServer(marketplace);

        // Install both primaries into the same server / marketplace.
        // Each is loaded into its own PluginLoadContext (ALC), so each primary's
        // ALC holds its own copy of DepPlugin.dll.
        server.InstallRecipes(new InstallRecipesRequest { Recipes = primaryADll })
            .GetAwaiter().GetResult();
        server.InstallRecipes(new InstallRecipesRequest { Recipes = primaryBDll })
            .GetAwaiter().GetResult();

        // Whole-tree preparation: PrepareInstance calls recipe.GetRecipeList() on the live
        // instance stored in the marketplace. Because PrimaryRecipeA was created inside ALC-A,
        // its GetRecipeList() returns a DepPlugin.ChildRecipe instance from ALC-A's DepPlugin
        // (v1), not from ALC-B's DepPlugin (v2) — and vice versa for PrimaryRecipeB.
        var a = server.PrepareRecipe(new PrepareRecipeRequest { Id = "PrimaryA.PrimaryRecipeA" })
            .GetAwaiter().GetResult();
        var b = server.PrepareRecipe(new PrepareRecipeRequest { Id = "PrimaryB.PrimaryRecipeB" })
            .GetAwaiter().GetResult();

        // Each parent's composite child must reflect ITS OWN bundled DepPlugin version.
        // Same FQN (DepPlugin.ChildRecipe), different DisplayName proves per-ALC isolation:
        // no shared/global instance per recipe FQN bleeds across bundle boundaries.
        Assert.Single(a.RecipeList);
        Assert.Single(b.RecipeList);
        Assert.Equal("child-v1", a.RecipeList[0].Descriptor.DisplayName);
        Assert.Equal("child-v2", b.RecipeList[0].Descriptor.DisplayName);
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
