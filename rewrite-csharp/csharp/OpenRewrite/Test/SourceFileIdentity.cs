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
using System.Text;
using OpenRewrite.Core;
using CompilationUnit = OpenRewrite.CSharp.CompilationUnit;
using Document = OpenRewrite.Xml.Document;

namespace OpenRewrite.Test;

/// <summary>
/// Fails a recipe run that changes a source file into another one rather than editing it: one with another id, path,
/// charset or byte order mark, or with markers other than the ones it had.
/// </summary>
internal static class SourceFileIdentity
{
    public static void AssertKept(Recipe recipe, IEnumerable<Result> results)
    {
        var found = new StringBuilder();
        foreach (var (before, after) in results)
        {
            if (before == null || after == null)
            {
                continue;
            }
            var changes = Changes(before, after).ToList();
            if (changes.Count > 0)
            {
                found.Append("  ").Append(before.SourcePath).Append(":\n");
                changes.ForEach(c => found.Append("    ").Append(c).Append('\n'));
            }
        }

        if (found.Length > 0)
        {
            Assert.Fail(
                "Edit the source file the recipe was given, with its id, path, charset, byte order mark and markers.\n" +
                $"{recipe.Name} changed what identifies a source file:\n" +
                found +
                "A tool that applies the results finds the file by its id and path, writes it in its charset, and " +
                "reads what the markers say of it.");
        }
    }

    private static IEnumerable<string> Changes(SourceFile before, SourceFile after)
    {
        if (before.Id != after.Id)
        {
            yield return $"its id {before.Id} became {after.Id}";
        }
        if (before.SourcePath != after.SourcePath)
        {
            yield return $"its path became {after.SourcePath}";
        }
        if (Encoding(before) is var (charset, bom) && Encoding(after) is var (afterCharset, afterBom))
        {
            if (charset != afterCharset)
            {
                yield return $"its charset {charset ?? "(none)"} became {afterCharset ?? "(none)"}";
            }
            if (bom != afterBom)
            {
                yield return bom ? "it lost its byte order mark" : "it gained a byte order mark";
            }
        }
        if (!ReferenceEquals(before.Markers, after.Markers))
        {
            yield return "its markers are other ones";
        }
    }

    private static (string? Charset, bool BomMarked)? Encoding(SourceFile source) => source switch
    {
        CompilationUnit cu => (cu.Charset, cu.CharsetBomMarked),
        Document doc => (doc.CharsetName, doc.CharsetBomMarked),
        _ => null
    };
}
