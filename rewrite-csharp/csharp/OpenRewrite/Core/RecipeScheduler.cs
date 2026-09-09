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
/// Runs a recipe (and its sub-recipes) against a set of source files, collecting
/// before/after results. When any recipe in the tree is an <see cref="IScanningRecipe"/>
/// the run is phased: every scanner visits every file, then generated files are collected,
/// and only then do editors run — so scanners observe the source set in its unedited state
/// and generated files are edited like any other source. Structured after the TypeScript
/// scheduler (rewrite-javascript/rewrite/src/run.ts) to keep the implementations aligned.
/// </summary>
public static class RecipeScheduler
{
    public static List<Result> Run(Recipe recipe, List<SourceFile> sources, ExecutionContext ctx)
    {
        var state = new RunState(ctx);
        var results = new List<Result>();

        if (HasScanningRecipe(recipe, state))
        {
            // Phase 1: Scan — every scanner visits every file. Scanning must not modify
            // the tree, so the visit result is discarded and the original file is passed
            // on to the next recipe in the list.
            foreach (var source in sources)
            {
                RecurseRecipeList(recipe, source, state, (r, s) =>
                {
                    if (r is IScanningRecipe scanning)
                    {
                        scanning.Scanner(state.Accumulator(scanning)).Visit(s, ctx);
                    }
                    return s;
                });
            }

            // Phase 2: Collect generated files
            var generated = RecurseRecipeList(recipe, new List<SourceFile>(), state, (r, g) =>
            {
                if (r is IScanningRecipe scanning)
                {
                    g.AddRange(scanning.Generate(state.Accumulator(scanning), ctx));
                }
                return g;
            })!;

            // Phase 3: Edit existing files
            EditAll(recipe, sources, state, results);

            // Phase 4: Edit generated files, including by the generating recipe's own editor
            foreach (var source in generated)
            {
                var after = EditFile(recipe, source, state);
                if (after != null)
                {
                    results.Add(new Result(null, after));
                }
            }
        }
        else
        {
            EditAll(recipe, sources, state, results);
        }

        return results;
    }

    private static void EditAll(Recipe recipe, List<SourceFile> sources, RunState state, List<Result> results)
    {
        foreach (var source in sources)
        {
            var after = EditFile(recipe, source, state);
            if (!ReferenceEquals(source, after))
            {
                results.Add(new Result(source, after));
            }
        }
    }

    private static SourceFile? EditFile(Recipe recipe, SourceFile source, RunState state)
    {
        return RecurseRecipeList(recipe, source, state, (r, s) =>
        {
            var visitor = r is IScanningRecipe scanning
                ? scanning.Editor(state.Accumulator(scanning))
                : r.GetVisitor();
            if (visitor is NoopVisitor<ExecutionContext>)
            {
                // The default visitor is a no-op, so recipes that don't override it
                // (typically composites) need no visit.
                return s;
            }

            var after = visitor.Visit(s, state.Ctx);
            return after == null ? null : after as SourceFile ?? s;
        });
    }

    /// <summary>
    /// Pre-order fold over the recipe tree: the recipe itself is visited first, then its
    /// recipe list. A null callback result short-circuits the remaining traversal — in the
    /// edit phase that is how a deleted file stops being visited.
    /// </summary>
    private static T? RecurseRecipeList<T>(Recipe recipe, T initial, RunState state, Func<Recipe, T, T?> fn)
        where T : class
    {
        var t = fn(recipe, initial);
        foreach (var subRecipe in state.SubRecipes(recipe))
        {
            if (t == null)
            {
                return null;
            }
            t = RecurseRecipeList(subRecipe, t, state, fn);
        }
        return t;
    }

    private static bool HasScanningRecipe(Recipe recipe, RunState state)
    {
        if (recipe is IScanningRecipe)
        {
            return true;
        }
        foreach (var subRecipe in state.SubRecipes(recipe))
        {
            if (HasScanningRecipe(subRecipe, state))
            {
                return true;
            }
        }
        return false;
    }

    /// <summary>
    /// Per-run state. Recipes commonly instantiate their sub-recipes inside
    /// <see cref="Recipe.GetRecipeList"/>, so calling it more than once yields different
    /// instances, and a scanning sub-recipe would then be handed a different accumulator in
    /// every phase. Resolving each recipe list once per run keeps sub-recipe identity — and
    /// therefore accumulator identity — stable.
    /// </summary>
    private sealed class RunState(ExecutionContext ctx)
    {
        public ExecutionContext Ctx { get; } = ctx;

        private readonly Dictionary<Recipe, List<Recipe>> _recipeLists = new(ReferenceEqualityComparer.Instance);
        private readonly Dictionary<IScanningRecipe, object> _accumulators = new(ReferenceEqualityComparer.Instance);

        public List<Recipe> SubRecipes(Recipe recipe)
        {
            if (!_recipeLists.TryGetValue(recipe, out var subRecipes))
            {
                subRecipes = recipe.GetRecipeList();
                _recipeLists[recipe] = subRecipes;
            }
            return subRecipes;
        }

        public object Accumulator(IScanningRecipe recipe)
        {
            if (!_accumulators.TryGetValue(recipe, out var acc))
            {
                acc = recipe.InitialValue(Ctx);
                _accumulators[recipe] = acc;
            }
            return acc;
        }
    }
}
