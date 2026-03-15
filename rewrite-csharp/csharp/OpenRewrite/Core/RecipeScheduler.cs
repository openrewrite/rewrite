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
using OpenRewrite.Java;

namespace OpenRewrite.Core;

/// <summary>
/// Runs a recipe (and its sub-recipes) against a set of source files,
/// collecting before/after results. Handles composite recipes by recursing
/// through <see cref="Recipe.GetRecipeList"/>.
/// </summary>
public static class RecipeScheduler
{
    public static List<Result> Run(Recipe recipe, List<SourceFile> sources, ExecutionContext ctx)
    {
        var allResults = new Dictionary<Guid, Result>();
        var currentSources = new List<SourceFile>(sources);

        RunRecipe(recipe, currentSources, allResults, ctx);

        return allResults.Values.ToList();
    }

    private static void RunRecipe(
        Recipe recipe,
        List<SourceFile> currentSources,
        Dictionary<Guid, Result> allResults,
        ExecutionContext ctx)
    {
        var recipeList = recipe.GetRecipeList();
        if (recipeList.Count > 0)
        {
            foreach (var subRecipe in recipeList)
            {
                RunRecipe(subRecipe, currentSources, allResults, ctx);
            }
        }
        else
        {
            var results = EditSources(recipe, currentSources, ctx);
            ApplyResults(results, currentSources, allResults);
        }
    }

    private static List<Result> EditSources(
        Recipe recipe,
        List<SourceFile> sources,
        ExecutionContext ctx)
    {
        if (recipe is IScanningRecipe scanning)
        {
            var acc = scanning.InitialValue(ctx);

            // Phase 1: Scan
            var scanner = scanning.Scanner(acc);
            foreach (var source in sources)
            {
                scanner.Visit((Tree)source, ctx);
            }

            // Phase 2: Generate
            var generated = scanning.Generate(acc, ctx).ToList();

            // Phase 3: Edit
            var results = VisitAll(scanning.Editor(acc), sources, ctx);

            foreach (var gen in generated)
            {
                results.Add(new Result(null, gen));
            }

            return results;
        }

        return VisitAll(recipe.GetVisitor(), sources, ctx);
    }

    private static List<Result> VisitAll(
        JavaVisitor<ExecutionContext> visitor,
        List<SourceFile> sources,
        ExecutionContext ctx)
    {
        var results = new List<Result>();
        foreach (var source in sources)
        {
            var after = visitor.Visit((Tree)source, ctx);
            if (after == null)
            {
                results.Add(new Result(source, null));
            }
            else if (after is SourceFile sf && !ReferenceEquals(source, after))
            {
                results.Add(new Result(source, sf));
            }
        }

        return results;
    }

    private static void ApplyResults(
        List<Result> results,
        List<SourceFile> currentSources,
        Dictionary<Guid, Result> allResults)
    {
        foreach (var result in results)
        {
            if (result.Before != null)
            {
                allResults[result.Before.Id] = result;
                if (result.After != null)
                {
                    for (var i = 0; i < currentSources.Count; i++)
                    {
                        if (currentSources[i].Id == result.Before.Id)
                        {
                            currentSources[i] = result.After;
                            break;
                        }
                    }
                }
                else
                {
                    currentSources.RemoveAll(s => s.Id == result.Before.Id);
                }
            }
            else if (result.After != null)
            {
                allResults[result.After.Id] = result;
                currentSources.Add(result.After);
            }
        }
    }
}
