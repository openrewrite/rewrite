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
using OpenRewrite.CSharp;
using OpenRewrite.CSharp.Format;
using Xunit.Sdk;
using ExecutionContext = OpenRewrite.Core.ExecutionContext;

namespace OpenRewrite.Test;

/// <summary>
/// Runs the recipe on an empty C# file when it changed one, and fails the test if the recipe throws there. An empty
/// XML document has no root and a project file can't be empty, so XML is not probed.
/// </summary>
internal static class EmptySources
{
    public static void AssertNoneThrow(Recipe recipe, IEnumerable<Result> results, int cycles)
    {
        if (results.Select(r => r.Before).OfType<CompilationUnit>().FirstOrDefault() is not { } changed)
        {
            return;
        }
        CompilationUnit empty;
        try
        {
            empty = new CSharpParser().Parse("", sourcePath: changed.SourcePath);
        }
        catch (Exception)
        {
            return;
        }

        var thrown = Run(recipe, empty, cycles);
        if (thrown != null)
        {
            throw new XunitException(Message(recipe, empty, thrown), thrown);
        }
    }

    /// <summary>
    /// What the recipe throws when run on the empty file alone, with a fresh execution context, other than the
    /// formatter's failure to reconcile, which production skips.
    /// </summary>
    private static Exception? Run(Recipe recipe, SourceFile empty, int cycles)
    {
        var ctx = new ExecutionContext();
        var sources = new List<SourceFile> { empty };
        try
        {
            for (var cycle = 1; cycle <= cycles; cycle++)
            {
                var changes = RecipeScheduler.Run(recipe, sources, ctx);
                if (changes.Count == 0)
                {
                    break;
                }
                sources = changes.Where(c => c.After != null).Select(c => c.After!)
                    .Concat(sources.Where(s => changes.All(c => !ReferenceEquals(c.Before, s))))
                    .ToList();
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

    private static string Message(Recipe recipe, SourceFile empty, Exception thrown) =>
        "Check for the code the recipe edits before using it, so that it handles an empty file or leaves it " +
        "unchanged instead of throwing.\n" +
        $"{recipe.Name} threw when run on an empty {empty.GetType().Name} at {empty.SourcePath}:\n" +
        Describe(thrown, "    ", recipe) + "\n" +
        "The test gives the recipe only files that contain what it edits, but repositories also hold empty files, " +
        "such as a placeholder a project template left or a file whose code was all deleted.\n" +
        "  - return the tree unchanged when what the visitor assumes is absent: check the type with `is` before a " +
        "cast (`cu.Members[0].Element is ClassDeclaration classDecl`), emptiness before an index or First() " +
        "(`cu.Members.Count > 0`) and null before a dereference " +
        "(`Cursor.FirstEnclosing<ClassDeclaration>() is { } classDecl`)\n" +
        "  - if the recipe adds what is missing, build that content correctly for an empty file too\n" +
        "  - don't catch the exception to make this pass\n" +
        "Then add a test with an empty source, CSharp(\"\"), that states the intended result.";

    /// <summary>
    /// The exception the recipe threw, with the first line of its message, the first frame outside .NET and, when
    /// that frame isn't the recipe's, the first frame that is.
    /// </summary>
    private static string Describe(Exception thrown, string indent, Recipe recipe)
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
