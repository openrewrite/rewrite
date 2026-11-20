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
using System.Collections.Concurrent;
using OpenRewrite.Core;
using OpenRewrite.Java;

namespace OpenRewrite.CSharp.Format;

/// <summary>
/// Detects how a source file indents, as Java's Autodetect does: with tabs or spaces, and by how many columns a
/// level of nesting moves its statements and members.
/// </summary>
internal static class IndentationDetector
{
    private static readonly ConcurrentDictionary<(bool, int), CSharpFormatStyle> DefaultWithIndentation = new();

    /// <summary>
    /// The indentation of the statements and members of <paramref name="cu"/> that start a line, or null when none of
    /// them is indented, leaving out the nodes with the ids in <paramref name="formatting"/>, which are about to be
    /// reindented. The indent size is null for a file indented only with tabs.
    /// </summary>
    public static (bool UseTabs, int? IndentSize)? Detect(CompilationUnit cu, IReadOnlySet<Guid>? formatting = null)
    {
        var statistics = new Statistics();
        new IndentVisitor(formatting ?? new HashSet<Guid>()).Visit(cu, statistics);
        return statistics.Indentation();
    }

    /// <summary>
    /// <paramref name="style"/> with the indentation detected in <paramref name="cu"/>, except where
    /// <paramref name="keepUseTabs"/> or <paramref name="keepIndentSize"/> say the style already sets it.
    /// </summary>
    public static CSharpFormatStyle Apply(CSharpFormatStyle style, CompilationUnit cu,
        bool keepUseTabs = false, bool keepIndentSize = false)
    {
        if (Detect(cu) is not var (useTabs, indentSize))
            return style;
        if (keepUseTabs)
            useTabs = style.UseTabs;
        var size = keepIndentSize || indentSize == null ? style.IndentSize : indentSize.Value;
        return style.WithIndentation(useTabs, size, useTabs ? size : style.TabSize);
    }

    /// <summary>
    /// The style to format the nodes with the ids in <paramref name="formatting"/> with when nothing configures one:
    /// Roslyn's defaults, indented as the rest of <paramref name="cu"/> already is.
    /// </summary>
    public static CSharpFormatStyle DefaultFor(CompilationUnit cu, IReadOnlySet<Guid>? formatting = null)
    {
        if (Detect(cu, formatting) is not var (useTabs, indentSize))
            return CSharpFormatStyle.Default;
        var size = indentSize ?? CSharpFormatStyle.Default.IndentSize;
        var tabSize = useTabs ? size : CSharpFormatStyle.Default.TabSize;
        return DefaultWithIndentation.GetOrAdd((useTabs, size),
            _ => CSharpFormatStyle.Default.WithIndentation(useTabs, size, tabSize));
    }

    /// <summary>
    /// The whitespace before a node that starts a line, from the start of that line, or null when the node doesn't
    /// start one.
    /// </summary>
    private static string? LineIndentation(Space prefix)
    {
        var whitespace = prefix.Comments.Count > 0 ? prefix.Comments[^1].Suffix : prefix.Whitespace;
        var lineStart = whitespace.LastIndexOf('\n');
        return lineStart < 0 ? null : whitespace[(lineStart + 1)..];
    }

    private sealed class Statistics
    {
        public int Depth;
        private long _totalDepth;
        private long _tabs;
        private readonly Dictionary<int, int> _spacesPerLevel = [];

        public void Record(Space prefix)
        {
            if (LineIndentation(prefix) is not { } indentation || Depth == 0)
                return;
            var tabs = indentation.Count(c => c == '\t');
            var spaces = indentation.Count(c => c == ' ');
            if (tabs + spaces == 0)
                return;
            _totalDepth += Depth;
            _tabs += tabs;
            if (tabs == 0)
            {
                var perLevel = (int)Math.Round(spaces / (double)Depth);
                if (perLevel > 0)
                    _spacesPerLevel[perLevel] = _spacesPerLevel.GetValueOrDefault(perLevel) + 1;
            }
        }

        /// <summary>
        /// A file uses tabs when they make up at least half of its levels of nesting, as in Java's Autodetect.
        /// </summary>
        public (bool UseTabs, int? IndentSize)? Indentation()
        {
            if (_totalDepth == 0)
                return null;
            int? indentSize = _spacesPerLevel.Count == 0
                ? null
                : _spacesPerLevel.OrderByDescending(e => e.Value).ThenBy(e => e.Key).First().Key;
            return (_tabs >= _totalDepth / 2.0, indentSize);
        }
    }

    /// <summary>
    /// Records the prefix of each statement of a block and each member of a namespace with braces, one level deeper
    /// than the block or namespace itself.
    /// </summary>
    private sealed class IndentVisitor(IReadOnlySet<Guid> formatting) : CSharpVisitor<Statistics>
    {
        public override J? PreVisit(J tree, Statistics statistics)
        {
            if (formatting.Contains(tree.Id))
                StopAfterPreVisit();
            return tree;
        }

        public override J VisitBlock(Block block, Statistics statistics)
        {
            statistics.Depth++;
            foreach (var statement in block.Statements)
                Record(statement.Element, statistics);
            var result = base.VisitBlock(block, statistics);
            statistics.Depth--;
            return result;
        }

        public override J VisitNamespaceDeclaration(NamespaceDeclaration ns, Statistics statistics)
        {
            // A file-scoped namespace is no level of nesting, nor is one whose members the file leaves flush
            if (ns.Name.Markers.FindFirst<Semicolon>() != null || LeavesMembersFlush(ns))
                return base.VisitNamespaceDeclaration(ns, statistics);
            statistics.Depth++;
            foreach (var member in ns.Members)
                Record(member.Element, statistics);
            var result = base.VisitNamespaceDeclaration(ns, statistics);
            statistics.Depth--;
            return result;
        }

        /// <summary>
        /// Whether the first member of <paramref name="ns"/> that starts a line, of those not about to be reindented,
        /// starts in the column of the namespace itself.
        /// </summary>
        private bool LeavesMembersFlush(NamespaceDeclaration ns)
        {
            var own = LineIndentation(ns.Prefix) ?? ns.Prefix.Whitespace;
            foreach (var member in ns.Members)
            {
                var (id, prefix) = RoslynFormatter.PrintableIdAndPrefix(member.Element);
                if (!formatting.Contains(id) && !formatting.Contains(member.Element.Id) &&
                    LineIndentation(prefix) is { } indentation)
                    return indentation == own;
            }
            return false;
        }

        private void Record(Statement statement, Statistics statistics)
        {
            var (id, prefix) = RoslynFormatter.PrintableIdAndPrefix(statement);
            if (!formatting.Contains(id) && !formatting.Contains(statement.Id))
                statistics.Record(prefix);
        }
    }
}
