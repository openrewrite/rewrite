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
using OpenRewrite.Xml;
using ExecutionContext = OpenRewrite.Core.ExecutionContext;

namespace OpenRewrite.CSharp.Recipes;

/// <summary>
/// Changes the .NET Framework version in legacy (non-SDK) .csproj files and matching app.config entries.
/// Updates &lt;TargetFrameworkVersion&gt; in legacy csproj and the sku version inside
/// &lt;supportedRuntime&gt; in app.config. For SDK-style .csproj &lt;TargetFramework&gt; / &lt;TargetFrameworks&gt;
/// elements, use ChangeDotNetTargetFramework instead.
/// </summary>
public class ChangeDotNetFrameworkVersion : Recipe
{
    public override string DisplayName => "Change .NET Framework version";

    public override string Description =>
        "Changes the `<TargetFrameworkVersion>` value in legacy .csproj files and the version " +
        "inside `<supportedRuntime sku>` in app.config files. For SDK-style csproj files, use " +
        "`ChangeDotNetTargetFramework` to update `<TargetFramework>` / `<TargetFrameworks>` instead.";

    [Option(DisplayName = "Old version",
        Description = "The .NET Framework version to replace (e.g., v4.7.2). The leading 'v' is optional.",
        Example = "v4.7.2")]
    public string OldVersion { get; set; } = "";

    [Option(DisplayName = "New version",
        Description = "The .NET Framework version to use instead (e.g., v4.8). The leading 'v' is optional.",
        Example = "v4.8")]
    public string NewVersion { get; set; } = "";

    [Option(DisplayName = "Regenerate MSBuild marker",
        Description = "Whether to re-run `dotnet restore` after the edit to refresh the project's " +
                      "MSBuildProject marker. Defaults to `true`. Composite recipes that chain " +
                      "multiple csproj-mutating steps may set this to `false` on intermediate steps " +
                      "and finalize once with `EnsureCsprojAttestation`.",
        Required = false)]
    public bool RegenerateMarker { get; set; } = true;

    public override ITreeVisitor<ExecutionContext> GetVisitor() =>
        new ChangeDotNetFrameworkVersionVisitor(OldVersion, NewVersion, RegenerateMarker);
}
