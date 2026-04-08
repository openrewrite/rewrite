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
/// Changes the target framework in .csproj files.
/// Handles both single-TFM and multi-TFM elements.
/// </summary>
public class ChangeDotNetTargetFramework : ScanningRecipe<DotNetBuildContext>
{
    public override string DisplayName => "Change .NET target framework";

    public override string Description =>
        "Changes the `<TargetFramework>` or `<TargetFrameworks>` value in .csproj files. " +
        "For multi-TFM projects, replaces the matching framework within the semicolon-delimited list.";

    [Option(DisplayName = "Old target framework",
        Description = "The target framework moniker to replace (e.g., net8.0).",
        Example = "net8.0")]
    public string OldTargetFramework { get; set; } = "";

    [Option(DisplayName = "New target framework",
        Description = "The target framework moniker to use instead (e.g., net9.0).",
        Example = "net9.0")]
    public string NewTargetFramework { get; set; } = "";

    public override DotNetBuildContext GetInitialValue(ExecutionContext ctx) => DotNetBuildContext.GetOrCreate(ctx);

    public override ITreeVisitor<ExecutionContext> GetScanner(DotNetBuildContext acc) => new BuildContextScanner();

    public override ITreeVisitor<ExecutionContext> GetVisitor(DotNetBuildContext acc)
    {
        return Preconditions.Check(
            new IsProjectFile(),
            new ChangeDotNetTargetFrameworkVisitor(OldTargetFramework, NewTargetFramework));
    }
}
