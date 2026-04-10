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
/// Searches for .csproj files that reference a specific NuGet package.
/// Supports glob patterns.
/// </summary>
public class FindNuGetPackageReference : Recipe
{
    public override string DisplayName => "Find NuGet package reference";

    public override string Description =>
        "Searches for .csproj files that reference a specific NuGet package. " +
        "Intended for use as a precondition to scope other recipes.";

    [Option(DisplayName = "Package name",
        Description = "The NuGet package name to search for. Supports glob patterns.",
        Example = "Swashbuckle.AspNetCore")]
    public string PackageName { get; set; } = "";

    public override ITreeVisitor<ExecutionContext> GetVisitor()
    {
        return Preconditions.Check(
            new IsProjectFile(),
            new FindNuGetPackageReferenceVisitor(PackageName));
    }
}
