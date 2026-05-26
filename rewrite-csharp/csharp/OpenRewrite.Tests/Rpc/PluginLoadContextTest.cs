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
using System.Reflection;
using System.Runtime.Loader;
using OpenRewrite.Core;
using OpenRewrite.CSharp.Rpc;

namespace OpenRewrite.Tests.Rpc;

/// <summary>
/// Demonstrates that loading plugin assemblies into AssemblyLoadContext.Default
/// causes type identity failures, and that PluginLoadContext fixes this.
/// </summary>
public class PluginLoadContextTest : IDisposable
{
    private readonly string _pluginDir;
    private readonly string _publishDir;

    public PluginLoadContextTest()
    {
        // Build a minimal plugin assembly that implements IRecipeActivator.
        // The plugin references OpenRewrite.CSharp from the NuGet local feed,
        // which is a *different build* than the one running in this test process.
        // This version mismatch is exactly what happens in production.
        _pluginDir = Path.Combine(Path.GetTempPath(), "rewrite-plugin-test", Guid.NewGuid().ToString("N")[..8]);
        Directory.CreateDirectory(_pluginDir);

        var hostDll = typeof(IRecipeActivator).Assembly.Location;
        var hostDir = Path.GetDirectoryName(hostDll)!;

        // Create a plugin project that references the host assembly directly.
        // In production this would come from NuGet; using a direct reference
        // ensures the plugin compiles against the same API surface but produces
        // a separate assembly with its own copy of the dependency graph.
        File.WriteAllText(Path.Combine(_pluginDir, "TestPlugin.csproj"), $"""
            <Project Sdk="Microsoft.NET.Sdk">
              <PropertyGroup>
                <TargetFramework>net10.0</TargetFramework>
                <EnableDefaultCompileItems>true</EnableDefaultCompileItems>
              </PropertyGroup>
              <ItemGroup>
                <Reference Include="OpenRewrite">
                  <HintPath>{hostDll}</HintPath>
                  <Private>false</Private>
                </Reference>
              </ItemGroup>
            </Project>
            """);

        File.WriteAllText(Path.Combine(_pluginDir, "TestActivator.cs"), """
            using OpenRewrite.Core;
            using OpenRewrite.CSharp;
            using OpenRewrite.Java;

            namespace TestPlugin;

            public class TestActivator : IRecipeActivator
            {
                public void Activate(RecipeMarketplace marketplace)
                {
                    marketplace.Install(new TestRecipe(),
                        new CategoryDescriptor("Test"));
                }
            }

            public class TestRecipe : Recipe
            {
                public override string DisplayName => "Test recipe from plugin";
                public override string Description => "A test recipe loaded from an external plugin assembly.";
                public override JavaVisitor<ExecutionContext> GetVisitor() =>
                    new CSharpVisitor<ExecutionContext>();
            }
            """);

        _publishDir = Path.Combine(_pluginDir, "publish");
        RunDotnet($"publish \"{Path.Combine(_pluginDir, "TestPlugin.csproj")}\" -c Release -o \"{_publishDir}\"");
    }

    public void Dispose()
    {
        try { Directory.Delete(_pluginDir, true); }
        catch { /* best-effort cleanup */ }
    }

    /// <summary>
    /// When a plugin is loaded into a separate AssemblyLoadContext that does NOT
    /// delegate shared assemblies back to Default, the plugin's IRecipeActivator
    /// is a *different type* than the host's IRecipeActivator. The `is` check
    /// and IsAssignableFrom both return false, so no recipes are discovered.
    /// </summary>
    [Fact]
    public void IsolatedContextWithoutDelegation_BreaksTypeIdentity()
    {
        var pluginDll = Path.Combine(_publishDir, "TestPlugin.dll");
        Assert.True(File.Exists(pluginDll), $"Plugin DLL not found at {pluginDll}");

        // Load into a plain isolated ALC with NO delegation to Default.
        // This simulates what happens when assemblies are loaded without
        // proper host-sharing: the plugin gets its own copy of OpenRewrite.dll.
        var isolated = new AssemblyLoadContext("no-delegation", isCollectible: true);
        try
        {
            // Pre-load the host's OpenRewrite.dll into the isolated context so
            // the plugin can resolve its dependency — but as a SEPARATE copy.
            var hostOpenRewrite = typeof(IRecipeActivator).Assembly.Location;
            isolated.LoadFromAssemblyPath(hostOpenRewrite);

            var assembly = isolated.LoadFromAssemblyPath(pluginDll);
            var activatorType = assembly.GetExportedTypes()
                .FirstOrDefault(t => t.Name == "TestActivator");

            Assert.NotNull(activatorType);

            // The type implements "IRecipeActivator" by name, but it's the
            // isolated context's copy — NOT the host's IRecipeActivator.
            var hostActivatorType = typeof(IRecipeActivator);
            Assert.False(hostActivatorType.IsAssignableFrom(activatorType),
                "Without delegation, the plugin's IRecipeActivator should NOT be " +
                "assignable to the host's IRecipeActivator (different type identity)");

            // Instantiation works, but casting to the host's interface fails.
            var instance = Activator.CreateInstance(activatorType)!;
            Assert.False(instance is IRecipeActivator,
                "Without delegation, 'instance is IRecipeActivator' should be false");
        }
        finally
        {
            isolated.Unload();
        }
    }

    /// <summary>
    /// PluginLoadContext delegates shared/host assemblies back to Default,
    /// so the plugin's IRecipeActivator IS the same type as the host's.
    /// Recipes are discovered and activated successfully.
    /// </summary>
    [Fact]
    public void PluginLoadContext_PreservesTypeIdentity()
    {
        var pluginDll = Path.Combine(_publishDir, "TestPlugin.dll");
        Assert.True(File.Exists(pluginDll), $"Plugin DLL not found at {pluginDll}");

        var context = new PluginLoadContext(pluginDll);
        var assembly = context.LoadFromAssemblyPath(pluginDll);

        var activatorType = assembly.GetExportedTypes()
            .FirstOrDefault(t => t.Name == "TestActivator");

        Assert.NotNull(activatorType);

        // With PluginLoadContext, the host's IRecipeActivator type identity is preserved.
        var hostActivatorType = typeof(IRecipeActivator);
        Assert.True(hostActivatorType.IsAssignableFrom(activatorType),
            "PluginLoadContext should preserve type identity: " +
            "plugin's IRecipeActivator must be assignable to host's IRecipeActivator");

        // Casting works — this is the critical fix.
        var instance = Activator.CreateInstance(activatorType)!;
        Assert.True(instance is IRecipeActivator,
            "With PluginLoadContext, 'instance is IRecipeActivator' should be true");

        // End-to-end: activate and verify recipes are registered.
        var activator = (IRecipeActivator)instance;
        var marketplace = new RecipeMarketplace();
        activator.Activate(marketplace);

        var recipes = marketplace.AllRecipes();
        Assert.Single(recipes);
        Assert.Equal("TestPlugin.TestRecipe", recipes[0].Name);
        Assert.Equal("Test recipe from plugin", recipes[0].DisplayName);
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
