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
using Microsoft.CodeAnalysis.CSharp.Formatting;
using Microsoft.CodeAnalysis.Formatting;
using Microsoft.CodeAnalysis.Text;
using OpenRewrite.Core;
using OpenRewrite.CSharp.Template;
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
        // 1. Ensure minimum spacing so printed output is parseable.
        // MVS may introduce spacing artifacts on nodes outside the target subtree
        // (e.g., adding space to a ParameterizedType whose prefix is empty but whose
        // inner Clazz already carries the space). Use the MVS result only for
        // printing/Roslyn formatting, and reconcile back against the original CU.
        var originalCu = cu;
        var mvsCu = (CompilationUnit)(new MinimumViableSpacingVisitor().Visit(cu, 0) ?? cu);

        // 2. Print to string, tracking the position of the target subtree if provided
        string source;
        TextSpan? formatSpan = null;

        if (targetSubtree != null)
        {
            var trackingPrinter = new PositionTrackingPrinter(targetSubtree.Id);
            source = trackingPrinter.Print(mvsCu);
            var (start, end) = trackingPrinter.GetTrackedSpan();
            if (start >= 0 && end > start)
            {
                formatSpan = TextSpan.FromBounds(start, end);
            }
        }
        else
        {
            var printer = new CSharpPrinter<int>();
            source = printer.Print(mvsCu);
        }

        // 3. Get style from marker (attached during parsing) or fall back to defaults
        var style = cu.Markers.FindFirst<CSharpFormatStyle>() ?? CSharpFormatStyle.Default;

        // 4. Format with Roslyn (scoped to the target span when available)
        var formattedSource = FormatWithRoslyn(source, style, formatSpan);

        // 5. If Roslyn formatting didn't change anything, reconcile MVS changes
        // (if any) within the target subtree and return. MVS changes like added
        // spaces between modifiers and types must not be discarded.
        if (string.Equals(source, formattedSource, StringComparison.Ordinal))
        {
            if (ReferenceEquals(mvsCu, originalCu))
                return originalCu;

            // MVS made changes — reconcile them within the target subtree
            var mvsReconciler = new WhitespaceReconciler();
            var mvsResult = mvsReconciler.Reconcile(originalCu, mvsCu, targetSubtree, stopAfter);
            if (!mvsReconciler.IsCompatible)
                return originalCu;
            return mvsResult as CompilationUnit ?? originalCu;
        }

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
            return originalCu;
        }

        // 7. Reconcile whitespace against the original CU so MVS artifacts
        // outside the target subtree are discarded.
        var reconciler = new WhitespaceReconciler();
        var result = reconciler.Reconcile(originalCu, formattedCu, targetSubtree, stopAfter);

        if (!reconciler.IsCompatible)
            return originalCu;

        return result as CompilationUnit ?? originalCu;
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

    /// <summary>
    /// Returns the ID and prefix that the printer will see in BeforeSyntax.
    /// ExpressionStatement delegates its prefix to its Expression and the printer
    /// skips BeforeSyntax for it, so we track the expression instead.
    /// </summary>
    internal static (Guid id, Space prefix) PrintableIdAndPrefix(J node)
    {
        if (node is ExpressionStatement es)
            return (es.Expression.Id, es.Expression.Prefix);
        return (node.Id, node.Prefix);
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
    /// Formats multiple subtrees within a compilation unit in a single Roslyn pass.
    /// Used by <see cref="DeferredFormatVisitor"/> to batch-format all template replacements.
    /// </summary>
    /// <param name="cu">The compilation unit containing all replacements.</param>
    /// <param name="nodeIds">IDs of the nodes whose regions need formatting.</param>
    /// <param name="preservedPrefixes">Map from node ID to the prefix that should be restored after formatting.</param>
    public static CompilationUnit FormatSpans(CompilationUnit cu, HashSet<Guid> nodeIds,
        Dictionary<Guid, Space> preservedPrefixes)
    {
        if (nodeIds.Count == 0)
            return cu;

        // 1. Ensure minimum spacing so printed output is parseable.
        // MVS may introduce spacing artifacts on nodes outside the target subtrees
        // (e.g., adding space to a ParameterizedType whose prefix is empty but whose
        // inner Clazz already carries the space). Use the MVS result only for
        // printing/Roslyn formatting, and reconcile back against the original CU.
        var originalCu = cu;
        var mvsCu = (CompilationUnit)(new MinimumViableSpacingVisitor().Visit(cu, 0) ?? cu);

        // 2. Print to string, tracking positions of all target nodes
        var trackingPrinter = new MultiPositionTrackingPrinter(nodeIds);
        var source = trackingPrinter.Print(mvsCu);
        var spans = trackingPrinter.GetTrackedSpans();

        if (spans.Count == 0)
            return originalCu;

        // 3. Get style from marker (attached during parsing) or fall back to defaults
        var style = cu.Markers.FindFirst<CSharpFormatStyle>() ?? CSharpFormatStyle.Default;

        // 4. Format with Roslyn, scoped to all target spans
        var formattedSource = FormatWithRoslyn(source, style, spans);

        // 5. If Roslyn formatting didn't change anything, reconcile MVS changes
        // (if any) within the target subtrees and return. MVS changes like added
        // spaces between modifiers and types must not be discarded.
        if (string.Equals(source, formattedSource, StringComparison.Ordinal))
        {
            if (ReferenceEquals(mvsCu, originalCu))
                return originalCu;

            // MVS made changes — reconcile them within the target subtrees
            var mvsReconciler = new WhitespaceReconciler();
            var mvsResult = mvsReconciler.Reconcile(originalCu, mvsCu, nodeIds);
            if (!mvsReconciler.IsCompatible)
                return originalCu;
            cu = mvsResult as CompilationUnit ?? originalCu;

            // Restore preserved prefixes
            if (preservedPrefixes.Count > 0)
            {
                var restorer = new PrefixRestorer(preservedPrefixes);
                restorer.Cursor = new Cursor(null, Cursor.ROOT_VALUE);
                cu = (CompilationUnit)(restorer.Visit(cu, 0) ?? cu);
            }

            return cu;
        }

        // 6. Parse formatted string back to LST
        var parser = new CSharpParser();
        CompilationUnit formattedCu;
        try
        {
            formattedCu = parser.Parse(formattedSource, cu.SourcePath);
        }
        catch (Exception)
        {
            return originalCu;
        }

        // 7. Reconcile whitespace only within the target subtrees.
        // Reconcile against the original CU so MVS artifacts outside targets are discarded.
        var reconciler = new WhitespaceReconciler();
        var result = reconciler.Reconcile(originalCu, formattedCu, nodeIds);

        if (!reconciler.IsCompatible)
            return originalCu;

        cu = result as CompilationUnit ?? originalCu;

        // 8. Restore preserved prefixes
        if (preservedPrefixes.Count > 0)
        {
            var restorer = new PrefixRestorer(preservedPrefixes);
            restorer.Cursor = new Cursor(null, Cursor.ROOT_VALUE);
            cu = (CompilationUnit)(restorer.Visit(cu, 0) ?? cu);
        }

        return cu;
    }

    /// <summary>
    /// A printer that tracks the character offsets of a single node by ID.
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

    /// <summary>
    /// A printer that tracks the character offsets of multiple nodes by their IDs.
    /// </summary>
    private class MultiPositionTrackingPrinter(HashSet<Guid> targetIds) : CSharpPrinter<int>
    {
        private readonly Dictionary<Guid, int> _starts = new(targetIds.Count);
        private readonly Dictionary<Guid, int> _ends = new(targetIds.Count);

        public List<TextSpan> GetTrackedSpans()
        {
            var spans = new List<TextSpan>(_starts.Count);
            foreach (var (id, start) in _starts)
            {
                if (_ends.TryGetValue(id, out var end) && end > start)
                    spans.Add(TextSpan.FromBounds(start, end));
            }
            return spans;
        }

        protected override void BeforeSyntax(J j, PrintOutputCapture<int> p)
        {
            if (targetIds.Contains(j.Id) && !_starts.ContainsKey(j.Id))
                _starts[j.Id] = p.Length;
            base.BeforeSyntax(j, p);
        }

        protected override void AfterSyntax(J j, PrintOutputCapture<int> p)
        {
            base.AfterSyntax(j, p);
            if (targetIds.Contains(j.Id) && !_ends.ContainsKey(j.Id))
                _ends[j.Id] = p.Length;
        }
    }

    /// <summary>
    /// Restores preserved prefixes on specific nodes by ID.
    /// </summary>
    private class PrefixRestorer(Dictionary<Guid, Space> prefixes) : CSharpVisitor<int>
    {
        private int _remaining = prefixes.Count;

        protected override J? Accept(J tree, int p)
        {
            if (_remaining <= 0)
                return tree;
            if (prefixes.TryGetValue(tree.Id, out var prefix))
            {
                _remaining--;
                tree = J.SetPrefix(tree, prefix);
                // Still need to visit children in case of nested deferred nodes
            }
            return base.Accept(tree, p);
        }
    }

    /// <summary>
    /// After-visitor that flattens synthetic blocks and batch-formats all registered nodes
    /// in a single Roslyn pass. Replaces the need for a separate <c>BlockFlattener</c>.
    /// Registered via <see cref="TreeVisitor{T,P}.DoAfterVisit"/> during template application.
    /// </summary>
    public class DeferredFormatVisitor<P> : CSharpVisitor<P>,
        IEquatable<DeferredFormatVisitor<P>>
    {
        private readonly HashSet<Guid> _nodeIds = [];
        private readonly Dictionary<Guid, Space> _preservedPrefixes = [];

        public void Add(J node)
        {
            var (id, prefix) = PrintableIdAndPrefix(node);
            _nodeIds.Add(id);
            _preservedPrefixes[id] = prefix;
        }

        public override J VisitCompilationUnit(CompilationUnit cu, P ctx)
        {
            if (_nodeIds.Count == 0)
                return cu;

            // Flatten synthetic blocks and register their statements for formatting
            // in a single pass — ensures IDs match the actual tree
            cu = (CompilationUnit)FlattenAndRegister(cu);

            return FormatSpans(cu, _nodeIds, _preservedPrefixes);
        }

        /// <summary>
        /// Walk the tree. When a block contains a synthetic block child whose ID is registered,
        /// splice its statements into the parent and register them for formatting.
        /// Also flattens any synthetic blocks unconditionally (they always need flattening).
        /// </summary>
        private J FlattenAndRegister(J tree)
        {
            var flattener = new SyntheticBlockFlattener(_nodeIds, _preservedPrefixes);
            flattener.Cursor = new Cursor(null, Cursor.ROOT_VALUE);
            return flattener.VisitNonNull(tree, 0);
        }

        public bool Equals(DeferredFormatVisitor<P>? other) => other is not null;
        public override bool Equals(object? obj) => obj is DeferredFormatVisitor<P>;
        public override int GetHashCode() => typeof(DeferredFormatVisitor<P>).GetHashCode();
    }

    /// <summary>
    /// Flattens synthetic blocks by splicing their statements into the parent block.
    /// When a synthetic block's ID is in the registered set, replaces it with
    /// the individual statement IDs so formatting targets the actual nodes.
    /// </summary>
    private class SyntheticBlockFlattener(
        HashSet<Guid> nodeIds, Dictionary<Guid, Space> preservedPrefixes) : CSharpVisitor<int>
    {
        public override J VisitBlock(Block block, int ctx)
        {
            block = (Block)base.VisitBlock(block, ctx);

            var statements = block.Statements;
            List<JRightPadded<Statement>>? newStatements = null;

            for (var i = 0; i < statements.Count; i++)
            {
                if (statements[i].Element is Block inner &&
                    inner.Markers.FindFirst<SyntheticBlockContainer>() != null)
                {
                    if (newStatements == null)
                    {
                        newStatements = new List<JRightPadded<Statement>>(statements.Count);
                        for (var j = 0; j < i; j++)
                            newStatements.Add(statements[j]);
                    }

                    // If this synthetic block was registered for formatting,
                    // register its individual statements instead
                    var registered = nodeIds.Remove(inner.Id);
                    if (registered)
                        preservedPrefixes.Remove(inner.Id);

                    var innerStmts = inner.Statements;
                    for (var k = 0; k < innerStmts.Count; k++)
                    {
                        var s = innerStmts[k];
                        if (k == 0)
                        {
                            // Transfer the original statement's prefix to the first spliced statement.
                            // ExpressionStatement delegates its prefix to its inner expression.
                            var prefix = statements[i].Element.Prefix;
                            s = s.WithElement(s.Element is ExpressionStatement es
                                ? es.WithExpression(J.SetPrefix(es.Expression, prefix))
                                : (Statement)J.SetPrefix(s.Element, prefix));
                        }
                        newStatements.Add(s);

                        // Register each spliced statement for formatting.
                        // Don't save prefixes — Roslyn determines the correct
                        // indentation for each statement at its new nesting level.
                        if (registered)
                        {
                            var (sid, _) = PrintableIdAndPrefix(s.Element);
                            nodeIds.Add(sid);
                        }
                    }
                }
                else
                {
                    newStatements?.Add(statements[i]);
                }
            }

            return newStatements != null ? block.WithStatements(newStatements) : block;
        }
    }

    internal static string FormatWithRoslyn(string source, CSharpFormatStyle style, TextSpan? span = null)
    {
        var syntaxTree = CSharpSyntaxTree.ParseText(source);
        var root = syntaxTree.GetRoot();

        using var workspace = CreateWorkspace();
        var options = style.GetOrBuildOptionSet(workspace.Options);

        var formatted = span != null
            ? Formatter.Format(root, span.Value, workspace, options)
            : Formatter.Format(root, workspace, options);
        return formatted.ToFullString();
    }

    internal static string FormatWithRoslyn(string source, CSharpFormatStyle style, IEnumerable<TextSpan> spans)
    {
        var syntaxTree = CSharpSyntaxTree.ParseText(source);
        var root = syntaxTree.GetRoot();

        using var workspace = CreateWorkspace();
        var options = style.GetOrBuildOptionSet(workspace.Options);

        var formatted = Formatter.Format(root, spans, workspace, options);
        return formatted.ToFullString();
    }

    /// <summary>
    /// Cached MEF host services for creating AdhocWorkspaces.
    /// <para>
    /// The default <c>new AdhocWorkspace()</c> relies on <c>Assembly.Load</c> to discover
    /// <c>Microsoft.CodeAnalysis.CSharp.Workspaces</c> via the .NET dependency graph. When the
    /// OpenRewrite SDK is consumed as a NuGet package with <c>PrivateAssets="all"</c>, the
    /// dependency entries are absent from the consumer's <c>.deps.json</c>, causing
    /// <c>Assembly.Load</c> to fail silently. The formatter then becomes a no-op because
    /// MEF cannot discover the C# formatting services.
    /// </para>
    /// <para>
    /// This field works around the issue by referencing the already-loaded assemblies directly,
    /// since <c>typeof(CSharpSyntaxTree)</c> and <c>typeof(Formatter)</c> guarantee that the
    /// required Roslyn assemblies are in the current AppDomain. The host is created once and
    /// reused for every workspace — MEF composition is expensive, workspaces are cheap.
    /// </para>
    /// </summary>
    private static readonly Microsoft.CodeAnalysis.Host.Mef.MefHostServices HostServices =
        Microsoft.CodeAnalysis.Host.Mef.MefHostServices.Create(
        [
            typeof(Microsoft.CodeAnalysis.Workspace).Assembly,               // Microsoft.CodeAnalysis.Workspaces
            typeof(CSharpSyntaxTree).Assembly,                               // Microsoft.CodeAnalysis.CSharp
            typeof(Microsoft.CodeAnalysis.CSharp.Formatting.CSharpFormattingOptions).Assembly  // Microsoft.CodeAnalysis.CSharp.Workspaces
        ]);

    private static AdhocWorkspace CreateWorkspace() => new(HostServices);

    internal static Microsoft.CodeAnalysis.Options.OptionSet BuildOptions(Microsoft.CodeAnalysis.Options.OptionSet baseOptions, CSharpFormatStyle style)
    {
        return baseOptions
            .WithChangedOption(FormattingOptions.UseTabs, LanguageNames.CSharp, style.UseTabs)
            .WithChangedOption(FormattingOptions.IndentationSize, LanguageNames.CSharp, style.IndentSize)
            .WithChangedOption(FormattingOptions.TabSize, LanguageNames.CSharp, style.TabSize)
            .WithChangedOption(FormattingOptions.NewLine, LanguageNames.CSharp, style.NewLine)
            // Indentation
            .WithChangedOption(CSharpFormattingOptions.IndentBlock, style.IndentBlock)
            .WithChangedOption(CSharpFormattingOptions.IndentBraces, style.IndentBraces)
            .WithChangedOption(CSharpFormattingOptions.IndentSwitchCaseSection, style.IndentSwitchCaseSection)
            .WithChangedOption(CSharpFormattingOptions.IndentSwitchCaseSectionWhenBlock, style.IndentSwitchCaseSectionWhenBlock)
            .WithChangedOption(CSharpFormattingOptions.IndentSwitchSection, style.IndentSwitchSection)
            .WithChangedOption(CSharpFormattingOptions.LabelPositioning, (LabelPositionOptions)style.LabelPositioning)
            // Brace placement
            .WithChangedOption(CSharpFormattingOptions.NewLinesForBracesInTypes, style.NewLinesForBracesInTypes)
            .WithChangedOption(CSharpFormattingOptions.NewLinesForBracesInMethods, style.NewLinesForBracesInMethods)
            .WithChangedOption(CSharpFormattingOptions.NewLinesForBracesInProperties, style.NewLinesForBracesInProperties)
            .WithChangedOption(CSharpFormattingOptions.NewLinesForBracesInAccessors, style.NewLinesForBracesInAccessors)
            .WithChangedOption(CSharpFormattingOptions.NewLinesForBracesInAnonymousMethods, style.NewLinesForBracesInAnonymousMethods)
            .WithChangedOption(CSharpFormattingOptions.NewLinesForBracesInAnonymousTypes, style.NewLinesForBracesInAnonymousTypes)
            .WithChangedOption(CSharpFormattingOptions.NewLinesForBracesInControlBlocks, style.NewLinesForBracesInControlBlocks)
            .WithChangedOption(CSharpFormattingOptions.NewLinesForBracesInLambdaExpressionBody, style.NewLinesForBracesInLambdaExpressionBody)
            .WithChangedOption(CSharpFormattingOptions.NewLinesForBracesInObjectCollectionArrayInitializers, style.NewLinesForBracesInObjectCollectionArrayInitializers)
            // Note: NewLinesForBracesInLocalFunctions is stored in the marker for .editorconfig fidelity
            // but Roslyn 5.0's CSharpFormattingOptions does not expose this option.
            // New line before keywords / members
            .WithChangedOption(CSharpFormattingOptions.NewLineForElse, style.NewLineBeforeElse)
            .WithChangedOption(CSharpFormattingOptions.NewLineForCatch, style.NewLineBeforeCatch)
            .WithChangedOption(CSharpFormattingOptions.NewLineForFinally, style.NewLineBeforeFinally)
            .WithChangedOption(CSharpFormattingOptions.NewLineForClausesInQuery, style.NewLineForClausesInQuery)
            .WithChangedOption(CSharpFormattingOptions.NewLineForMembersInAnonymousTypes, style.NewLineForMembersInAnonymousTypes)
            .WithChangedOption(CSharpFormattingOptions.NewLineForMembersInObjectInit, style.NewLineForMembersInObjectInit)
            // Spacing
            .WithChangedOption(CSharpFormattingOptions.SpaceAfterCast, style.SpaceAfterCast)
            .WithChangedOption(CSharpFormattingOptions.SpaceAfterColonInBaseTypeDeclaration, style.SpaceAfterColonInBaseTypeDeclaration)
            .WithChangedOption(CSharpFormattingOptions.SpaceAfterComma, style.SpaceAfterComma)
            .WithChangedOption(CSharpFormattingOptions.SpaceAfterControlFlowStatementKeyword, style.SpaceAfterControlFlowStatementKeyword)
            .WithChangedOption(CSharpFormattingOptions.SpaceAfterDot, style.SpaceAfterDot)
            .WithChangedOption(CSharpFormattingOptions.SpaceAfterMethodCallName, style.SpaceAfterMethodCallName)
            .WithChangedOption(CSharpFormattingOptions.SpaceAfterSemicolonsInForStatement, style.SpaceAfterSemicolonsInForStatement)
            .WithChangedOption(CSharpFormattingOptions.SpaceBeforeColonInBaseTypeDeclaration, style.SpaceBeforeColonInBaseTypeDeclaration)
            .WithChangedOption(CSharpFormattingOptions.SpaceBeforeComma, style.SpaceBeforeComma)
            .WithChangedOption(CSharpFormattingOptions.SpaceBeforeDot, style.SpaceBeforeDot)
            .WithChangedOption(CSharpFormattingOptions.SpaceBeforeOpenSquareBracket, style.SpaceBeforeOpenSquareBracket)
            .WithChangedOption(CSharpFormattingOptions.SpaceBeforeSemicolonsInForStatement, style.SpaceBeforeSemicolonsInForStatement)
            .WithChangedOption(CSharpFormattingOptions.SpaceBetweenEmptyMethodCallParentheses, style.SpaceBetweenEmptyMethodCallParentheses)
            .WithChangedOption(CSharpFormattingOptions.SpaceBetweenEmptyMethodDeclarationParentheses, style.SpaceBetweenEmptyMethodDeclarationParentheses)
            .WithChangedOption(CSharpFormattingOptions.SpaceBetweenEmptySquareBrackets, style.SpaceBetweenEmptySquareBrackets)
            .WithChangedOption(CSharpFormattingOptions.SpacesIgnoreAroundVariableDeclaration, style.SpacesIgnoreAroundVariableDeclaration)
            .WithChangedOption(CSharpFormattingOptions.SpaceWithinCastParentheses, style.SpaceWithinCastParentheses)
            .WithChangedOption(CSharpFormattingOptions.SpaceWithinExpressionParentheses, style.SpaceWithinExpressionParentheses)
            .WithChangedOption(CSharpFormattingOptions.SpaceWithinMethodCallParentheses, style.SpaceWithinMethodCallParentheses)
            .WithChangedOption(CSharpFormattingOptions.SpaceWithinMethodDeclarationParenthesis, style.SpaceWithinMethodDeclarationParenthesis)
            .WithChangedOption(CSharpFormattingOptions.SpaceWithinOtherParentheses, style.SpaceWithinOtherParentheses)
            .WithChangedOption(CSharpFormattingOptions.SpaceWithinSquareBrackets, style.SpaceWithinSquareBrackets)
            .WithChangedOption(CSharpFormattingOptions.SpacingAfterMethodDeclarationName, style.SpacingAfterMethodDeclarationName)
            .WithChangedOption(CSharpFormattingOptions.SpacingAroundBinaryOperator, (BinaryOperatorSpacingOptions)style.SpacingAroundBinaryOperator)
            // Don't preserve single-line formatting — synthesized nodes may have no newlines,
            // and Roslyn must insert structural newlines (after {, before }, before else, etc.)
            .WithChangedOption(CSharpFormattingOptions.WrappingPreserveSingleLine, false)
            .WithChangedOption(CSharpFormattingOptions.WrappingKeepStatementsOnSingleLine, false);
    }
}
