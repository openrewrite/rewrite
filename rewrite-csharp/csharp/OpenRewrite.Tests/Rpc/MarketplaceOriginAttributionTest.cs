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
/// Verifies the RPC server attributes each marketplace row to the package that contributed it,
/// so the host can correctly bind recipes to their bundle instead of over-tagging.
/// </summary>
public class MarketplaceOriginAttributionTest : IDisposable
{
    private readonly string _root;
    private readonly string _alphaDll;
    private readonly string _betaDll;

    public MarketplaceOriginAttributionTest()
    {
        _root = Path.Combine(Path.GetTempPath(), "rewrite-origin-attr-test",
            Guid.NewGuid().ToString("N")[..8]);
        Directory.CreateDirectory(_root);
        var hostDll = typeof(IRecipeActivator).Assembly.Location;
        _alphaDll = BuildPackage("Alpha", hostDll);
        _betaDll = BuildPackage("Beta", hostDll);
    }

    private string BuildPackage(string id, string hostDll)
    {
        var dir = Path.Combine(_root, id);
        Directory.CreateDirectory(dir);
        File.WriteAllText(Path.Combine(dir, $"{id}.csproj"), $"""
            <Project Sdk="Microsoft.NET.Sdk">
              <PropertyGroup><TargetFramework>net10.0</TargetFramework></PropertyGroup>
              <ItemGroup>
                <Reference Include="OpenRewrite"><HintPath>{hostDll}</HintPath><Private>false</Private></Reference>
              </ItemGroup>
            </Project>
            """);
        File.WriteAllText(Path.Combine(dir, "Recipe.cs"), $$"""
            using OpenRewrite.Core;
            using OpenRewrite.CSharp;
            using OpenRewrite.Java;
            namespace {{id}}Plugin;
            public class {{id}}Activator : IRecipeActivator
            {
                public void Activate(RecipeMarketplace marketplace) =>
                    marketplace.Install(new {{id}}Recipe(), new CategoryDescriptor("{{id}}"));
            }
            public class {{id}}Recipe : Recipe
            {
                public override string DisplayName => "{{id}} recipe";
                public override string Description => "Recipe from {{id}}.";
                public override JavaVisitor<ExecutionContext> GetVisitor() => new CSharpVisitor<ExecutionContext>();
            }
            """);
        var publish = Path.Combine(dir, "publish");
        RunDotnet($"publish \"{Path.Combine(dir, $"{id}.csproj")}\" -c Release -o \"{publish}\"");
        return Path.Combine(publish, $"{id}.dll");
    }

    public void Dispose()
    {
        try { Directory.Delete(_root, true); } catch { /* best-effort */ }
    }

    [Fact]
    public void GetMarketplace_AttributesEachRecipeToItsOwnPackage()
    {
        var marketplace = new RecipeMarketplace();
        var server = new RewriteRpcServer(marketplace);

        // Install via the object form so the server sees a packageName for each.
        // Serialize to JsonElement so the server's JsonValueKind.Object branch is reached
        // (in-process anonymous objects don't deserialize to JsonElement automatically).
        server.InstallRecipes(new InstallRecipesRequest
            {
                Recipes = JsonSerializer.SerializeToElement(new { packageName = _alphaDll, version = (string?)null })
            })
            .GetAwaiter().GetResult();
        server.InstallRecipes(new InstallRecipesRequest
            {
                Recipes = JsonSerializer.SerializeToElement(new { packageName = _betaDll, version = (string?)null })
            })
            .GetAwaiter().GetResult();

        var rows = server.GetMarketplace().GetAwaiter().GetResult();

        var alpha = rows.Single(r => r.Descriptor.Name == "AlphaPlugin.AlphaRecipe");
        var beta = rows.Single(r => r.Descriptor.Name == "BetaPlugin.BetaRecipe");

        Assert.Equal(_alphaDll, alpha.PackageName);
        Assert.Equal(_betaDll, beta.PackageName);
    }

    private static void RunDotnet(string arguments)
    {
        var psi = new ProcessStartInfo("dotnet", arguments)
        {
            RedirectStandardOutput = true, RedirectStandardError = true,
            UseShellExecute = false, CreateNoWindow = true
        };
        using var process = Process.Start(psi)
                            ?? throw new InvalidOperationException("Failed to start dotnet process");
        var stdout = process.StandardOutput.ReadToEnd();
        var stderr = process.StandardError.ReadToEnd();
        process.WaitForExit();
        if (process.ExitCode != 0)
            throw new InvalidOperationException($"dotnet {arguments} failed ({process.ExitCode}):\n{stderr}\n{stdout}");
    }
}
