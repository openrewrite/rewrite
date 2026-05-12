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
using OpenRewrite.CSharp.Rpc;

namespace OpenRewrite.Tests.Rpc;

/// <summary>
/// Unit tests for RewriteRpcServer.ParseInstalledFromUrls, which extracts NuGet
/// source feed URLs from "Installed ... from ... with content hash ..." lines
/// emitted by `dotnet restore --verbosity normal`.
/// </summary>
public class InstallRecipesParseTests
{
    [Fact]
    public void ExtractsNugetOrgUrl()
    {
        var output = """
            Restoring packages for /tmp/Recipes.csproj...
              Installed Newtonsoft.Json 13.0.3 from https://api.nuget.org/v3/index.json with content hash ABC=.
              Installed Microsoft.Extensions.Logging 8.0.0 from https://api.nuget.org/v3/index.json with content hash DEF=.
            """;

        var urls = RewriteRpcServer.ParseInstalledFromUrls(output).ToList();

        Assert.Equal(2, urls.Count);
        Assert.All(urls, u => Assert.Equal("https://api.nuget.org/v3/index.json", u));
    }

    [Fact]
    public void ExtractsInternalArtifactoryFeed()
    {
        var output = "Installed Internal.Pkg 1.0.0 from https://internal.example.com/artifactory/api/nuget/v3/nuget-virtual/index.json with content hash X=.";

        var urls = RewriteRpcServer.ParseInstalledFromUrls(output).ToList();

        Assert.Single(urls);
        Assert.Equal(
            "https://internal.example.com/artifactory/api/nuget/v3/nuget-virtual/index.json",
            urls[0]);
    }

    [Fact]
    public void ReturnsEmptyForOutputWithoutInstalledLines()
    {
        Assert.Empty(RewriteRpcServer.ParseInstalledFromUrls(""));
        Assert.Empty(RewriteRpcServer.ParseInstalledFromUrls("Restoring packages for /tmp/Recipes.csproj..."));
    }
}
