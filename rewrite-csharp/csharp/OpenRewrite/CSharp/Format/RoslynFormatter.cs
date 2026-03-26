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
using Microsoft.CodeAnalysis;
using Microsoft.CodeAnalysis.CSharp;
using Microsoft.CodeAnalysis.Formatting;
using Microsoft.CodeAnalysis.Text;
using OpenRewrite.Core;
using OpenRewrite.Java;

namespace OpenRewrite.CSharp.Format;

/// <summary>
/// Orchestrates the C# auto-formatting pipeline:
/// print → Roslyn format → parse → reconcile whitespace.
/// </summary>
public static class RoslynFormatter
{
    /// <summary>
    /// Formats the entire compilation unit.
    /// </summary>
    public static CompilationUnit Format(CompilationUnit cu)
    {
        return Format(cu, targetSubtree: null, stopAfter: null);
    }

    /// <summary>
    /// Formats the compilation unit, optionally limiting to a subtree.
    /// When a <paramref name="targetSubtree"/> is provided, only the region of the source
    /// corresponding to that subtree is formatted by Roslyn, avoiding O(file_size) formatting
    /// cost on every template application.
    /// </summary>
    public static CompilationUnit Format(CompilationUnit cu, J? targetSubtree, J? stopAfter)
    {
        // 1. Ensure minimum spacing so printed output is parseable
        cu = (CompilationUnit)(new MinimumViableSpacingVisitor().Visit(cu, 0) ?? cu);

        // 2. Print to string, tracking the position of the target subtree if provided
        string source;
        TextSpan? formatSpan = null;

        if (targetSubtree != null)
        {
            var trackingPrinter = new PositionTrackingPrinter(targetSubtree.Id);
            source = trackingPrinter.Print(cu);
            var (start, end) = trackingPrinter.GetTrackedSpan();
            if (start >= 0 && end > start)
            {
                formatSpan = TextSpan.FromBounds(start, end);
            }
        }
        else
        {
            var printer = new CSharpPrinter<int>();
            source = printer.Print(cu);
        }

        // 3. Detect style
        var style = FormatStyle.DetectStyle(source);

        // 4. Format with Roslyn (scoped to the target span when available)
        var formattedSource = FormatWithRoslyn(source, style, formatSpan);

        // 5. If formatting didn't change anything, return original
        if (string.Equals(source, formattedSource, StringComparison.Ordinal))
            return cu;

        // 6. Parse formatted string back to LST (no type attribution)
        var parser = new CSharpParser();
        CompilationUnit formattedCu;
        try
        {
            formattedCu = parser.Parse(formattedSource, cu.SourcePath);
        }
        catch (Exception)
        {
            // If parsing fails (shouldn't happen with Roslyn), return original
            return cu;
        }

        // 7. Reconcile whitespace
        var reconciler = new WhitespaceReconciler();
        var result = reconciler.Reconcile(cu, formattedCu, targetSubtree, stopAfter);

        if (!reconciler.IsCompatible)
            return cu;

        return result as CompilationUnit ?? cu;
    }

    /// <summary>
    /// Formats a subtree within a compilation unit.
    /// Splices <paramref name="replacement"/> into <paramref name="cu"/> at the node
    /// whose ID matches <paramref name="nodeToReplaceId"/>, formats the full CU with
    /// Roslyn, and extracts the formatted replacement by its ID.
    /// If the splice fails (node not found), returns <paramref name="replacement"/> unchanged.
    /// </summary>
    public static J FormatSubtree(CompilationUnit cu, Guid nodeToReplaceId, J replacement, J? stopAfter)
    {
        // 1. Splice the replacement into the CU
        var splicer = new NodeSplicer(nodeToReplaceId, replacement);
        splicer.Cursor = new Cursor(null, Cursor.ROOT_VALUE);
        var splicedCu = splicer.VisitNonNull(cu, 0) as CompilationUnit;

        if (splicedCu == null)
        {
            // Splice produced a non-CU result — should not happen
            return replacement;
        }

        if (ReferenceEquals(splicedCu, cu) && FindById(cu, nodeToReplaceId) == null)
        {
            // Splice was a no-op AND the node doesn't exist in the CU at all.
            // This means the CU is stale (earlier visitor changes removed the node).
            // TODO: implement string-level indent normalization fallback
            return replacement;
        }

        // If splicedCu == cu, the replacement was already in the CU (same reference).
        // Format the CU directly — the reconciler will find it via ReferenceEquals.

        // 2. Format the CU, scoped to the replacement subtree
        var formattedCu = Format(splicedCu, targetSubtree: replacement, stopAfter: stopAfter);

        // 3. If formatting didn't change anything, return as-is
        if (ReferenceEquals(formattedCu, splicedCu))
            return replacement;

        // 4. Extract the formatted subtree by ID
        return FindById(formattedCu, replacement.Id) ?? replacement;
    }

    internal static J? FindById(J root, Guid targetId)
    {
        var finder = new IdFinder(targetId);
        finder.Cursor = new Cursor(null, Cursor.ROOT_VALUE);
        finder.Visit(root, 0);
        return finder.Result;
    }

    /// <summary>
    /// Replaces a node by ID with a replacement, short-circuiting after the first match.
    /// </summary>
    private class NodeSplicer(Guid targetId, J replacement) : CSharpVisitor<int>
    {
        private bool _found;

        protected override J? Accept(J tree, int p)
        {
            if (_found)
                return tree;
            if (tree.Id == targetId)
            {
                _found = true;
                return replacement;
            }
            return base.Accept(tree, p);
        }
    }

    /// <summary>
    /// Finds a node by ID, short-circuiting after the first match.
    /// </summary>
    private class IdFinder(Guid targetId) : CSharpVisitor<int>
    {
        internal J? Result { get; private set; }

        protected override J? Accept(J tree, int p)
        {
            if (Result != null)
                return tree;
            if (tree.Id == targetId)
            {
                Result = tree;
                return tree;
            }
            return base.Accept(tree, p);
        }
    }

    /// <summary>
    /// A printer that tracks the character offset of a specific node by ID.
    /// Used to compute the <see cref="TextSpan"/> for span-scoped Roslyn formatting.
    /// </summary>
    private class PositionTrackingPrinter(Guid targetId) : CSharpPrinter<int>
    {
        private int _start = -1;
        private int _end = -1;

        public (int Start, int End) GetTrackedSpan() => (_start, _end);

        protected override void BeforeSyntax(J j, PrintOutputCapture<int> p)
        {
            if (_start < 0 && j.Id == targetId)
                _start = p.Length;
            base.BeforeSyntax(j, p);
        }

        protected override void AfterSyntax(J j, PrintOutputCapture<int> p)
        {
            base.AfterSyntax(j, p);
            if (_end < 0 && j.Id == targetId)
                _end = p.Length;
        }
    }

    internal static string FormatWithRoslyn(string source, FormatStyle style, TextSpan? span = null)
    {
        var syntaxTree = CSharpSyntaxTree.ParseText(source);
        var root = syntaxTree.GetRoot();

        using var workspace = new AdhocWorkspace();
        var options = workspace.Options
            .WithChangedOption(FormattingOptions.UseTabs, LanguageNames.CSharp, style.UseTabs)
            .WithChangedOption(FormattingOptions.IndentationSize, LanguageNames.CSharp, style.IndentationSize)
            .WithChangedOption(FormattingOptions.TabSize, LanguageNames.CSharp, style.IndentationSize)
            .WithChangedOption(FormattingOptions.NewLine, LanguageNames.CSharp, style.NewLine);

        var formatted = span != null
            ? Formatter.Format(root, span.Value, workspace, options)
            : Formatter.Format(root, workspace, options);
        return formatted.ToFullString();
    }
}
