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
namespace OpenRewrite.Core;

/// <summary>
/// Listing-weight view the marketplace holds and serves for the InstallRecipes and GetMarketplace
/// RPC commands, so listing never materializes the full recursive descriptor. The full tree is built
/// lazily per recipe by PrepareRecipe. <see cref="RecipeCount"/> is 1 + every transitive RecipeList
/// entry, computed once at install time (the host uses it as a sort key).
/// </summary>
public sealed record RecipeListing(
    string Name,
    string DisplayName,
    string Description,
    TimeSpan? EstimatedEffortPerOccurrence,
    IReadOnlyList<OptionDescriptor> Options,
    IReadOnlyList<DataTableDescriptor> DataTables,
    int RecipeCount)
{
    /// <summary>
    /// Derives the listing-weight view from a full descriptor, collapsing its recursive RecipeList
    /// to a count.
    /// </summary>
    public static RecipeListing From(RecipeDescriptor descriptor) => new(
        descriptor.Name,
        descriptor.DisplayName,
        descriptor.Description,
        descriptor.EstimatedEffortPerOccurrence,
        descriptor.Options,
        descriptor.DataTables,
        CountRecipes(descriptor));

    private static int CountRecipes(RecipeDescriptor descriptor)
    {
        var count = 1;
        foreach (var sub in descriptor.RecipeList)
        {
            count += CountRecipes(sub);
        }
        return count;
    }
}
