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
using OpenRewrite.Java;

namespace OpenRewrite.CSharp.Format;

/// <summary>
/// Visitor entry point for auto-formatting C# code using Roslyn.
/// Delegates to <see cref="RoslynFormatter"/> for the actual formatting pipeline.
/// </summary>
public class AutoFormatVisitor<P> : CSharpVisitor<P>
{
    private readonly J? _stopAfter;

    public AutoFormatVisitor(J? stopAfter = null)
    {
        _stopAfter = stopAfter;
    }

    public override J VisitCompilationUnit(CompilationUnit cu, P p)
    {
        return RoslynFormatter.Format(cu, targetSubtree: null, stopAfter: _stopAfter);
    }

    /// <summary>
    /// Formats a subtree within its enclosing compilation unit.
    /// Returns the formatted subtree (not the entire CU).
    /// </summary>
    public J Format(J tree, Cursor cursor)
    {
        if (tree is CompilationUnit treeCu)
            return RoslynFormatter.Format(treeCu, targetSubtree: null, stopAfter: _stopAfter);

        var cu = cursor.FirstEnclosing<CompilationUnit>();
        if (cu == null)
            return tree;

        // The cursor's CU is the old tree before structural changes.
        // Splice the modified subtree into the CU so that the print/format/reconcile
        // pipeline sees the actual structural change (e.g. a new Block wrapping a statement).
        var updatedCu = SpliceIntoTree(cu, tree);

        var formattedCu = RoslynFormatter.Format(updatedCu, targetSubtree: tree, stopAfter: _stopAfter);

        // Find the target subtree in the formatted CU by matching ID
        return FindById(formattedCu, tree.Id) ?? tree;
    }

    /// <summary>
    /// Replaces the node in <paramref name="root"/> whose ID matches <paramref name="replacement"/>
    /// with <paramref name="replacement"/> itself. Returns the updated root.
    /// </summary>
    private static CompilationUnit SpliceIntoTree(CompilationUnit root, J replacement)
    {
        var splicer = new NodeSplicer(replacement);
        var result = splicer.Visit(root, 0);
        return result as CompilationUnit ?? root;
    }

    private static J? FindById(J root, Guid targetId)
    {
        J? found = null;
        var finder = new IdFinder(targetId, f => found = f);
        finder.Visit(root, 0);
        return found;
    }

    private class IdFinder(Guid targetId, Action<J> onFound) : CSharpVisitor<int>
    {
        protected override J? Accept(J tree, int p)
        {
            if (tree.Id == targetId)
            {
                onFound(tree);
                return tree;
            }
            return base.Accept(tree, p);
        }
    }

    /// <summary>
    /// Replaces a node by ID, returning the replacement (which preserves the reference
    /// identity needed by WhitespaceReconciler's ReferenceEquals check).
    /// </summary>
    private class NodeSplicer(J replacement) : CSharpVisitor<int>
    {
        protected override J? Accept(J tree, int p)
        {
            if (tree.Id == replacement.Id)
                return replacement;
            return base.Accept(tree, p);
        }
    }
}
