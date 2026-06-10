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
/// When a local NuGet feed (<c>~/.nuget/local-feed</c>) exists for cross-repo
/// development, the server makes it additive to the recipe project's
/// <c>nuget.config</c>: it creates one (public + local feed) when none exists,
/// and otherwise only appends the local feed — preserving a caller-written
/// config (e.g. an exclusive configured feed) untouched.
/// </summary>
public class RecipesNuGetConfigTests
{
    private const string LocalFeed = "/home/dev/.nuget/local-feed";

    [Fact]
    public void CreatesPublicPlusLocalFeedWhenNoConfigExists()
    {
        string xml = RewriteRpcServer.BuildRecipesNuGetConfig(null, LocalFeed);

        Assert.Contains("api.nuget.org", xml);
        Assert.Contains(LocalFeed, xml);
    }

    [Fact]
    public void AppendsOnlyLocalFeedToExistingConfig()
    {
        // A caller-written exclusive config: nuget.org cleared, one configured feed.
        string existing = """
            <?xml version="1.0" encoding="utf-8"?>
            <configuration>
              <packageSources>
                <clear />
                <add key="internal" value="http://localhost:9091/nuget-all" allowInsecureConnections="true" />
              </packageSources>
            </configuration>
            """;

        string xml = RewriteRpcServer.BuildRecipesNuGetConfig(existing, LocalFeed);

        // Caller's exclusivity and feed are preserved...
        Assert.Contains("<clear", xml);
        Assert.Contains("http://localhost:9091/nuget-all", xml);
        // ...local feed is added...
        Assert.Contains(LocalFeed, xml);
        // ...and nuget.org is NOT reintroduced.
        Assert.DoesNotContain("api.nuget.org", xml);
    }

    [Fact]
    public void IsIdempotentWhenLocalFeedAlreadyPresent()
    {
        string existing = $"""
            <?xml version="1.0" encoding="utf-8"?>
            <configuration>
              <packageSources>
                <add key="local-feed" value="{LocalFeed}" />
              </packageSources>
            </configuration>
            """;

        string xml = RewriteRpcServer.BuildRecipesNuGetConfig(existing, LocalFeed);

        int occurrences = xml.Split(LocalFeed).Length - 1;
        Assert.Equal(1, occurrences);
    }
}
