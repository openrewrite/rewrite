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
using System.Diagnostics;
using System.Text;
using OpenRewrite.Core;
using OpenRewrite.CSharp.Format;
using ExecutionContext = OpenRewrite.Core.ExecutionContext;

namespace OpenRewrite.Test;

/// <summary>
/// What the checks that run a recipe on sources the test doesn't give it share: how they run it and how they tell
/// what it threw.
/// </summary>
internal static class Probes
{
    /// <summary>
    /// What the recipe throws when run on the sources for the test's cycles, with a fresh execution context, other
    /// than the formatter's failure to reconcile, which production skips.
    /// </summary>
    public static Exception? Run(Recipe recipe, List<SourceFile> sources, int cycles)
    {
        var ctx = new ExecutionContext();
        try
        {
            for (var cycle = 1; cycle <= cycles; cycle++)
            {
                var changes = RecipeScheduler.Run(recipe, sources, ctx);
                if (changes.Count == 0)
                {
                    break;
                }
                sources = Next(sources, changes);
            }
            return null;
        }
        catch (Exception e)
        {
            // the switch that would skip it is process-wide, and tests running alongside need it on
            for (var cause = e; cause != null; cause = cause.InnerException)
            {
                if (cause is WhitespaceReconciler.WhitespaceReconcileMismatchException)
                {
                    return null;
                }
            }
            return e;
        }
    }

    /// <summary>The sources in their order as a cycle left them, followed by the ones it generated.</summary>
    private static List<SourceFile> Next(List<SourceFile> sources, List<Result> changes)
    {
        var next = new List<SourceFile>(sources.Count);
        foreach (var source in sources)
        {
            var change = changes.Find(c => ReferenceEquals(c.Before, source));
            if (change == null)
            {
                next.Add(source);
            }
            else if (change.After != null)
            {
                next.Add(change.After);
            }
        }
        next.AddRange(changes.Where(c => c.Before == null && c.After != null).Select(c => c.After!));
        return next;
    }

    /// <summary>
    /// The exception the recipe threw, with the first line of its message, the first frame outside .NET and, when
    /// that frame isn't the recipe's, the first frame that is.
    /// </summary>
    public static string Describe(Exception thrown, string indent, Recipe recipe)
    {
        var description = new StringBuilder(indent).Append(thrown.GetType().FullName);
        var firstLine = thrown.Message.Trim().Split('\n')[0].Trim();
        if (firstLine.Length > 0)
        {
            description.Append(": ").Append(firstLine.Length > 200 ? firstLine[..200] + "..." : firstLine);
        }

        var recipeTypes = new HashSet<Type>();
        AddTypes(recipe, recipeTypes);
        var outsideDotNet = false;
        foreach (var frame in new StackTrace(thrown, true).GetFrames())
        {
            if (frame.GetMethod() is not { DeclaringType: { } type } method)
            {
                continue;
            }
            var ofRecipe = false;
            for (var t = type; t != null && !ofRecipe; t = t.DeclaringType)
            {
                ofRecipe = recipeTypes.Contains(t);
            }
            var inDotNet = type.Namespace?.StartsWith("System", StringComparison.Ordinal) == true ||
                           type.Namespace?.StartsWith("Microsoft", StringComparison.Ordinal) == true;
            if (!outsideDotNet && !inDotNet || outsideDotNet && ofRecipe)
            {
                var file = frame.GetFileName() is { } path ? $"{Path.GetFileName(path)}:{frame.GetFileLineNumber()}"
                    : "unknown source";
                description.Append('\n').Append(indent).Append("    at ")
                    .Append(type.FullName?.Replace('+', '.')).Append('.').Append(method.Name)
                    .Append('(').Append(file).Append(')');
                if (outsideDotNet || ofRecipe)
                {
                    break;
                }
                outsideDotNet = true;
            }
        }
        return description.ToString();
    }

    private static void AddTypes(Recipe recipe, HashSet<Type> types)
    {
        if (!types.Add(recipe.GetType()))
        {
            return;
        }
        try
        {
            foreach (var child in recipe.GetRecipeList())
            {
                AddTypes(child, types);
            }
        }
        catch (Exception)
        {
            // a recipe that can't list its children is still named
        }
    }
}
