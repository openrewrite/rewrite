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
/// Adds a NuGet PackageReference to .csproj files if not already present.
/// </summary>
public class AddNuGetPackageReference : ScanningRecipe<DotNetBuildContext>
{
    public override string DisplayName => "Add NuGet package reference";

    public override string Description =>
        "Adds a `<PackageReference>` element to .csproj files if not already present.";

    [Option(DisplayName = "Package name",
        Description = "The NuGet package name to add.",
        Example = "Newtonsoft.Json")]
    public string PackageName { get; set; } = "";

    [Option(DisplayName = "Version",
        Description = "The package version to add. If omitted, no Version attribute is set.",
        Example = "13.0.3",
        Required = false)]
    public string? Version { get; set; }

    public override DotNetBuildContext GetInitialValue(ExecutionContext ctx) => DotNetBuildContext.GetOrCreate(ctx);

    public override ITreeVisitor<ExecutionContext> GetScanner(DotNetBuildContext acc) => new BuildContextScanner();

    public override ITreeVisitor<ExecutionContext> GetVisitor(DotNetBuildContext acc)
    {
        return Preconditions.Check(
            new IsProjectFile(),
            new AddNuGetPackageReferenceVisitor(PackageName, Version));
    }
}
