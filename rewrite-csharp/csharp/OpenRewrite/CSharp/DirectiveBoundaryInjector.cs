/*
 * Copyright 2024 the original author or authors.
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

using System.Text.RegularExpressions;
using OpenRewrite.Core;
using OpenRewrite.Java;

namespace OpenRewrite.CSharp;

/// <summary>
/// Scans a parsed tree for ghost comments (//DIRECTIVE:N) embedded in Space whitespace strings
/// and attaches <see cref="DirectiveBoundaryMarker"/> to the owning nodes.
/// <para>
/// Ghost comments are emitted by <see cref="PreprocessorSourceTransformer.Transform"/> in place
/// of directive lines (e.g. #if, #else, #endif). Because the C# parser does not yet extract
/// comments from trivia, these appear as raw text in Space.Whitespace rather than as TextComment
/// entries in Space.Comments.
/// </para>
/// <para>
/// Ghost comments are intentionally left in the whitespace strings so the printer can use them
/// as sentinels for section-based assembly. The markers provide structured metadata for recipes.
/// </para>
/// </summary>
public partial class DirectiveBoundaryInjector : CSharpVisitor<int>
{
    [GeneratedRegex(@"//DIRECTIVE:(\d+)")]
    private static partial Regex GhostPattern();

    /// <summary>
    /// Inject directive boundary markers into the given tree by scanning for ghost comments.
    /// Ghost comments remain in the whitespace for printer use as sentinels.
    /// </summary>
    public static J Inject(J tree)
    {
        return new DirectiveBoundaryInjector().VisitNonNull(tree, 0);
    }

    public override J? PostVisit(J tree, int p)
    {
        return AddMarkerIfNeeded(tree, tree.Prefix.Whitespace);
    }

    public override J VisitBlock(Block block, int p)
    {
        block = (Block)base.VisitBlock(block, p);

        var indices = FindDirectiveIndices(block.End.Whitespace);
        if (indices.Count > 0)
        {
            var marker = new DirectiveBoundaryMarker(Guid.NewGuid(), indices);
            block = block.WithMarkers(block.Markers.Add(marker));
        }

        return block;
    }

    public override J VisitCompilationUnit(CompilationUnit compilationUnit, int p)
    {
        compilationUnit = (CompilationUnit)base.VisitCompilationUnit(compilationUnit, p);

        var indices = FindDirectiveIndices(compilationUnit.Eof.Whitespace);
        if (indices.Count > 0)
        {
            var marker = new DirectiveBoundaryMarker(Guid.NewGuid(), indices);
            compilationUnit = compilationUnit.WithMarkers(compilationUnit.Markers.Add(marker));
        }

        return compilationUnit;
    }

    private J AddMarkerIfNeeded(J node, string whitespace)
    {
        var indices = FindDirectiveIndices(whitespace);
        if (indices.Count == 0)
            return node;

        var marker = new DirectiveBoundaryMarker(Guid.NewGuid(), indices);
        var newMarkers = node.Markers.Add(marker);
        return SetMarkers(node, newMarkers);
    }

    private static List<int> FindDirectiveIndices(string whitespace)
    {
        var indices = new List<int>();
        var matches = GhostPattern().Matches(whitespace);
        foreach (Match match in matches)
        {
            indices.Add(int.Parse(match.Groups[1].Value));
        }
        return indices;
    }

    /// <summary>
    /// Uses reflection to call WithMarkers on any concrete J type,
    /// following the pattern established by <see cref="SearchResult.Found{T}"/>.
    /// </summary>
    private static J SetMarkers(J node, Markers markers)
    {
        var withMarkers = node.GetType().GetMethod("WithMarkers", [typeof(Markers)]);
        return withMarkers != null ? (J)withMarkers.Invoke(node, [markers])! : node;
    }
}
