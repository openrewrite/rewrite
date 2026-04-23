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

namespace OpenRewrite.Core;

/// <summary>
/// Generates unified diffs for print idempotency diagnostics.
/// </summary>
public static class DiffUtils
{
    private const int ContextLines = 3;

    /// <summary>
    /// Produces a unified diff between <paramref name="before"/> and <paramref name="after"/>,
    /// using git-style headers with the given <paramref name="path"/>.
    /// Returns an empty string when the inputs are identical.
    /// </summary>
    public static string UnifiedDiff(string before, string after, string path)
    {
        if (before == after)
            return "";

        var aLines = SplitLines(before);
        var bLines = SplitLines(after);

        var edits = ComputeEdits(aLines, bLines);
        var hunks = GroupIntoHunks(edits);

        var sb = new StringBuilder();
        sb.AppendLine($"--- a/{path}");
        sb.AppendLine($"+++ b/{path}");

        foreach (var hunk in hunks)
            FormatHunk(sb, edits, hunk, aLines, bLines);

        return sb.ToString();
    }

    private static string[] SplitLines(string text)
    {
        if (text.Length == 0)
            return [];

        var lines = text.Split('\n');
        if (lines.Length > 0 && lines[^1] == "")
            return lines[..^1];
        return lines;
    }

    private enum EditKind { Equal, Delete, Insert }

    private readonly record struct Edit(EditKind Kind, int AIndex, int BIndex);

    private static List<Edit> ComputeEdits(string[] a, string[] b)
    {
        var lcs = ComputeLcsTable(a, b);
        var edits = new List<Edit>();
        int i = 0, j = 0;

        while (i < a.Length && j < b.Length)
        {
            if (a[i] == b[j])
            {
                edits.Add(new Edit(EditKind.Equal, i, j));
                i++;
                j++;
            }
            else if (lcs[i + 1, j] >= lcs[i, j + 1])
            {
                edits.Add(new Edit(EditKind.Delete, i, -1));
                i++;
            }
            else
            {
                edits.Add(new Edit(EditKind.Insert, -1, j));
                j++;
            }
        }

        while (i < a.Length)
        {
            edits.Add(new Edit(EditKind.Delete, i++, -1));
        }

        while (j < b.Length)
        {
            edits.Add(new Edit(EditKind.Insert, -1, j++));
        }

        return edits;
    }

    private static int[,] ComputeLcsTable(string[] a, string[] b)
    {
        var m = a.Length;
        var n = b.Length;
        var dp = new int[m + 1, n + 1];

        for (var i = m - 1; i >= 0; i--)
        for (var j = n - 1; j >= 0; j--)
            dp[i, j] = a[i] == b[j]
                ? dp[i + 1, j + 1] + 1
                : Math.Max(dp[i + 1, j], dp[i, j + 1]);

        return dp;
    }

    private readonly record struct HunkRange(int StartEdit, int EndEdit);

    private static List<HunkRange> GroupIntoHunks(List<Edit> edits)
    {
        // Find contiguous ranges of non-Equal edits
        var changeRanges = new List<(int Start, int End)>();
        int? rangeStart = null;

        for (var idx = 0; idx < edits.Count; idx++)
        {
            if (edits[idx].Kind != EditKind.Equal)
            {
                rangeStart ??= idx;
            }
            else if (rangeStart.HasValue)
            {
                changeRanges.Add((rangeStart.Value, idx - 1));
                rangeStart = null;
            }
        }

        if (rangeStart.HasValue)
            changeRanges.Add((rangeStart.Value, edits.Count - 1));

        if (changeRanges.Count == 0)
            return [];

        // Expand each range by context lines and merge overlapping ranges
        var hunks = new List<HunkRange>();
        var curStart = Math.Max(0, changeRanges[0].Start - ContextLines);
        var curEnd = Math.Min(edits.Count - 1, changeRanges[0].End + ContextLines);

        for (var r = 1; r < changeRanges.Count; r++)
        {
            var nextStart = Math.Max(0, changeRanges[r].Start - ContextLines);
            var nextEnd = Math.Min(edits.Count - 1, changeRanges[r].End + ContextLines);

            if (nextStart <= curEnd + 1)
            {
                curEnd = nextEnd;
            }
            else
            {
                hunks.Add(new HunkRange(curStart, curEnd));
                curStart = nextStart;
                curEnd = nextEnd;
            }
        }

        hunks.Add(new HunkRange(curStart, curEnd));
        return hunks;
    }

    private static void FormatHunk(StringBuilder sb, List<Edit> edits, HunkRange hunk, string[] aLines,
        string[] bLines)
    {
        int aCount = 0, bCount = 0;
        int aStart = -1, bStart = -1;

        // Compute line ranges for the header
        for (var i = hunk.StartEdit; i <= hunk.EndEdit; i++)
        {
            var edit = edits[i];
            switch (edit.Kind)
            {
                case EditKind.Equal:
                    if (aStart < 0) aStart = edit.AIndex;
                    if (bStart < 0) bStart = edit.BIndex;
                    aCount++;
                    bCount++;
                    break;
                case EditKind.Delete:
                    if (aStart < 0) aStart = edit.AIndex;
                    aCount++;
                    break;
                case EditKind.Insert:
                    if (bStart < 0) bStart = edit.BIndex;
                    bCount++;
                    break;
            }
        }

        if (aStart < 0) aStart = 0;
        if (bStart < 0) bStart = 0;

        sb.AppendLine($"@@ -{aStart + 1},{aCount} +{bStart + 1},{bCount} @@");

        for (var i = hunk.StartEdit; i <= hunk.EndEdit; i++)
        {
            var edit = edits[i];
            switch (edit.Kind)
            {
                case EditKind.Equal:
                    sb.AppendLine($" {aLines[edit.AIndex]}");
                    break;
                case EditKind.Delete:
                    sb.AppendLine($"-{aLines[edit.AIndex]}");
                    break;
                case EditKind.Insert:
                    sb.AppendLine($"+{bLines[edit.BIndex]}");
                    break;
            }
        }
    }
}
