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
/// Removes an MSBuild property element (e.g. <c>RuntimeFrameworkVersion</c>) from a
/// <c>PropertyGroup</c> in .csproj files. Useful for stripping legacy properties that
/// are no longer applicable after upgrading the target framework.
/// </summary>
public class RemoveMSBuildProperty : ScanningRecipe<DotNetBuildContext>
{
    public override string DisplayName => "Remove MSBuild property";

    public override string Description =>
        "Removes an MSBuild property element (e.g. `<RuntimeFrameworkVersion>`) from " +
        "`<PropertyGroup>` in .csproj files.";

    [Option(DisplayName = "Property name",
        Description = "The MSBuild property element name to remove (case-sensitive).",
        Example = "RuntimeFrameworkVersion")]
    public string PropertyName { get; set; } = "";

    public override DotNetBuildContext GetInitialValue(ExecutionContext ctx) => DotNetBuildContext.GetOrCreate(ctx);

    public override ITreeVisitor<ExecutionContext> GetScanner(DotNetBuildContext acc) => new BuildContextScanner();

    public override ITreeVisitor<ExecutionContext> GetVisitor(DotNetBuildContext acc)
    {
        return Preconditions.Check(
            new IsProjectFile(),
            new RemoveMSBuildPropertyVisitor(PropertyName));
    }
}
