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
using static OpenRewrite.CSharp.Recipes.Categories;
using OpenRewrite.Xml;
using ExecutionContext = OpenRewrite.Core.ExecutionContext;

namespace OpenRewrite.CSharp.Recipes;

/// <summary>
/// Re-runs <c>dotnet restore</c> against each .csproj whose
/// <see cref="MSBuildProject"/> marker has been marked stale by an earlier
/// mutating recipe in this run, and refreshes the marker from the resulting
/// <c>project.assets.json</c>. Files that no recipe touched are skipped — the
/// staleness flag is set by mutating visitors via
/// <see cref="MSBuildProjectHelper.MarkAttestationStale"/>. Intended as the
/// terminator step in a composite recipe that suppresses intermediate marker
/// reattestation via <c>RegenerateMarker = false</c> on csproj-mutating
/// sub-recipes.
/// </summary>
[Category, Csproj]
public class EnsureCsprojAttestation : ScanningRecipe<DotNetBuildContext>
{
    public override string DisplayName => "Ensure csproj attestation";

    public override string Description =>
        "Re-runs `dotnet restore` against each .csproj whose `MSBuildProject` marker " +
        "is stale (set by any csproj-mutating recipe in the run) and refreshes the marker " +
        "from the resulting `project.assets.json`. Use this at the end of a composite " +
        "recipe whose csproj-mutating sub-recipes have `RegenerateMarker = false`, so " +
        "reattestation happens once on the final consistent state instead of after every edit. " +
        "Unmodified .csproj files incur no `dotnet restore` cost.";

    public override DotNetBuildContext GetInitialValue(ExecutionContext ctx) => DotNetBuildContext.GetOrCreate(ctx);

    public override ITreeVisitor<ExecutionContext> GetScanner(DotNetBuildContext acc) => new BuildContextScanner();

    public override ITreeVisitor<ExecutionContext> GetVisitor(DotNetBuildContext acc)
    {
        return Preconditions.Check(
            new IsProjectFile(),
            MSBuildProjectHelper.RegenerateMarkerVisitor());
    }
}
