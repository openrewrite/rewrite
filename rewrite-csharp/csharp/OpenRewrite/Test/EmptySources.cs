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
using OpenRewrite.CSharp;
using Xunit.Sdk;

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
        Probes.Once(recipe, typeof(CompilationUnit), nameof(EmptySources), () =>
        {
            CompilationUnit empty;
            try
            {
                empty = new CSharpParser().Parse("", sourcePath: changed.SourcePath);
            }
            catch (Exception)
            {
                return;
            }

            var thrown = Probes.Run(recipe, [empty], cycles);
            if (thrown != null)
            {
                throw new XunitException(Message(recipe, empty, thrown), thrown);
            }
        });
    }

    private static string Message(Recipe recipe, SourceFile empty, Exception thrown) =>
        "Check for the code the recipe edits before using it, so that it handles an empty file or leaves it " +
        "unchanged instead of throwing.\n" +
        $"{recipe.Name} threw when run on an empty {empty.GetType().Name} at {empty.SourcePath}:\n" +
        Probes.Describe(thrown, "    ", recipe) + "\n" +
        "The test gives the recipe only files that contain what it edits, but repositories also hold empty files, " +
        "such as a placeholder a project template left or a file whose code was all deleted.\n" +
        "  - return the tree unchanged when what the visitor assumes is absent: check the type with `is` before a " +
        "cast (`cu.Members[0].Element is ClassDeclaration classDecl`), emptiness before an index or First() " +
        "(`cu.Members.Count > 0`) and null before a dereference " +
        "(`Cursor.FirstEnclosing<ClassDeclaration>() is { } classDecl`)\n" +
        "  - if the recipe adds what is missing, build that content correctly for an empty file too\n" +
        "  - don't catch the exception to make this pass\n" +
        "Then add a test with an empty source, CSharp(\"\"), that states the intended result.";
}
