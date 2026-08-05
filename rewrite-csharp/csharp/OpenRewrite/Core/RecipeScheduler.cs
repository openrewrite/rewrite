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
        var currentSources = new List<SourceFile>(sources);

        RunRecipe(recipe, currentSources, ctx);

        return BuildResults(sources, currentSources);
    }

    private static void RunRecipe(
        Recipe recipe,
        List<SourceFile> currentSources,
        ExecutionContext ctx)
    {
        // Pre-order traversal, as in Java's RecipeRunCycle/RecipeStack: the recipe's own
        // visitor (or scan/generate/edit lifecycle) runs first, then its recipe list.
        ApplyResults(EditSources(recipe, currentSources, ctx), currentSources);

        foreach (var subRecipe in recipe.GetRecipeList())
        {
            RunRecipe(subRecipe, currentSources, ctx);
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
                scanner.Visit(source, ctx);
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

        var visitor = recipe.GetVisitor();
        if (visitor is NoopVisitor<ExecutionContext>)
        {
            // The default visitor is a no-op, so recipes that don't override it
            // (typically composites) need no traversal of the source set.
            return [];
        }

        return VisitAll(visitor, sources, ctx);
    }

    private static List<Result> VisitAll(
        ITreeVisitor<ExecutionContext> visitor,
        List<SourceFile> sources,
        ExecutionContext ctx)
    {
        var results = new List<Result>();
        foreach (var source in sources)
        {
            var after = visitor.Visit(source, ctx);
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

    private static void ApplyResults(List<Result> results, List<SourceFile> currentSources)
    {
        foreach (var result in results)
        {
            var before = result.Before;
            if (before != null)
            {
                if (result.After != null)
                {
                    var i = currentSources.FindIndex(s => s.Id == before.Id);
                    if (i >= 0)
                    {
                        currentSources[i] = result.After;
                    }
                }
                else
                {
                    currentSources.RemoveAll(s => s.Id == before.Id);
                }
            }
            else if (result.After != null)
            {
                currentSources.Add(result.After);
            }
        }
    }

    /// <summary>
    /// Diffs the source set as parsed against its state after the run, pairing files by id:
    /// same id with a different tree → changed, id only present before → deleted, id only
    /// present after → generated. Because only the initial and final states are compared,
    /// <see cref="Result.Before"/> is always the originally parsed source no matter how many
    /// recipes edited the file, and a generated file that a later recipe deleted yields no
    /// result at all. Relies on visitors preserving <see cref="SourceFile.Id"/> across edits.
    /// </summary>
    private static List<Result> BuildResults(List<SourceFile> before, List<SourceFile> after)
    {
        var results = new List<Result>();
        var afterById = after.ToDictionary(s => s.Id);
        foreach (var beforeSource in before)
        {
            if (afterById.Remove(beforeSource.Id, out var afterSource))
            {
                if (!ReferenceEquals(beforeSource, afterSource))
                {
                    results.Add(new Result(beforeSource, afterSource));
                }
            }
            else
            {
                results.Add(new Result(beforeSource, null));
            }
        }

        foreach (var afterSource in after)
        {
            if (afterById.ContainsKey(afterSource.Id))
            {
                results.Add(new Result(null, afterSource));
            }
        }

        return results;
    }
}
