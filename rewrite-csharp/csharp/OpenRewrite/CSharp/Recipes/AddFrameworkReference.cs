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
/// Adds a <c>&lt;FrameworkReference&gt;</c> to a .csproj file's project root if one
/// with a matching Include doesn't already exist. The reference is placed in a
/// dedicated <c>&lt;ItemGroup&gt;</c> appended to the project. No-op when the SDK
/// is <c>Microsoft.NET.Sdk.Web</c>, which already imports
/// <c>Microsoft.AspNetCore.App</c> implicitly.
/// </summary>
public class AddFrameworkReference : ScanningRecipe<DotNetBuildContext>
{
    public override string DisplayName => "Add framework reference";

    public override string Description =>
        "Adds a `<FrameworkReference>` to a .csproj if it isn't already present.";

    [Option(DisplayName = "Framework name",
        Description = "The shared framework name to reference.",
        Example = "Microsoft.AspNetCore.App")]
    public string FrameworkName { get; set; } = "";

    [Option(DisplayName = "Trigger package glob",
        Description = "Optional glob: only add the framework reference when a `<PackageReference>` " +
                      "matching this glob is present in the project. Leave blank to always add.",
        Example = "Microsoft.AspNetCore.*",
        Required = false)]
    public string? TriggerPackageGlob { get; set; }

    public override DotNetBuildContext GetInitialValue(ExecutionContext ctx) => DotNetBuildContext.GetOrCreate(ctx);

    public override ITreeVisitor<ExecutionContext> GetScanner(DotNetBuildContext acc) => new BuildContextScanner();

    public override ITreeVisitor<ExecutionContext> GetVisitor(DotNetBuildContext acc)
    {
        return Preconditions.Check(
            new IsProjectFile(),
            new AddFrameworkReferenceVisitor(FrameworkName, TriggerPackageGlob));
    }
}
