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
/// Changes the targetFramework attribute on &lt;httpRuntime&gt; and &lt;compilation&gt; elements
/// in web.config and app.config files. These attributes determine ASP.NET runtime
/// quirks-mode behavior independently of the binary TFM.
/// </summary>
public class ChangeWebConfigTargetFramework : Recipe
{
    public override string DisplayName => "Change web.config target framework";

    public override string Description =>
        "Changes the `targetFramework` attribute on `<httpRuntime>` and `<compilation>` elements " +
        "in web.config and app.config. These attributes control ASP.NET quirks-mode behavior and " +
        "are distinct from the binary TFM in the .csproj file.";

    [Option(DisplayName = "Old version",
        Description = "The .NET Framework version to replace (e.g., 4.7.2). The leading 'v' is optional.",
        Example = "4.7.2")]
    public string OldVersion { get; set; } = "";

    [Option(DisplayName = "New version",
        Description = "The .NET Framework version to use instead (e.g., 4.8). The leading 'v' is optional.",
        Example = "4.8")]
    public string NewVersion { get; set; } = "";

    public override ITreeVisitor<ExecutionContext> GetVisitor() =>
        new ChangeWebConfigTargetFrameworkVisitor(OldVersion, NewVersion);
}
