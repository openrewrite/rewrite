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
/// Runs the recipe without the first C# file and the first XML document it changed, which is how it meets a file its
/// parser failed on, and fails the test if the recipe throws there.
/// </summary>
internal static class UnparsedSources
{
    public static void AssertNoneThrow(Recipe recipe, IEnumerable<Result> results, List<SourceFile> sources,
        int cycles)
    {
        var changed = results.Where(r => r.Before != null).Select(r => r.Before!.Id).ToHashSet();
        if (changed.Count == 0 || IsTestScaffolding(recipe))
        {
            return;
        }
        var probedKinds = new HashSet<Type>();
        foreach (var missing in sources)
        {
            if (!changed.Contains(missing.Id) || missing is not (CompilationUnit or Xml.Document) ||
                !probedKinds.Add(missing.GetType()))
            {
                continue;
            }
            Probes.Once(recipe, missing.GetType(), nameof(UnparsedSources), () =>
            {
                var thrown = Probes.Run(recipe, sources.Where(s => !ReferenceEquals(s, missing)).ToList(), cycles);
                if (thrown != null)
                {
                    throw new XunitException(Message(recipe, missing, thrown), thrown);
                }
            });
        }
    }

    /// <summary>
    /// Recipes declared inside a test class, and recipes that run one, are scaffolding for the test, not recipes under
    /// test.
    /// </summary>
    private static bool IsTestScaffolding(Recipe recipe)
    {
        for (var type = recipe.GetType().DeclaringType; type != null; type = type.DeclaringType)
        {
            if (typeof(RewriteTest).IsAssignableFrom(type))
            {
                return true;
            }
        }
        try
        {
            return recipe.GetRecipeList().Any(IsTestScaffolding);
        }
        catch (Exception)
        {
            return true;
        }
    }

    private static string Message(Recipe recipe, SourceFile missing, Exception thrown) =>
        "Check that the scanner found the files the recipe needs before using what it keeps of them, so that it " +
        "changes nothing when one is missing.\n" +
        $"{recipe.Name} threw when {missing.SourcePath} was missing, which is how a C# recipe sees a file its parser " +
        "failed on:\n" +
        Probes.Describe(thrown, "    ", recipe) + "\n" +
        "Besides the files the test gives it, a repository holds files their parser failed on. The host sends a C# " +
        "recipe only the C# and XML files it parsed, so to the recipe such a file is simply not there.\n" +
        "  - when the scanner found no file of the kind the recipe needs, generate and change nothing, instead of " +
        "reading an empty accumulator\n" +
        "  - read what the scanner keeps of a file it may not find with TryGetValue rather than the indexer, and " +
        "check for null before a dereference\n" +
        "  - don't catch the exception to make this pass";
}
