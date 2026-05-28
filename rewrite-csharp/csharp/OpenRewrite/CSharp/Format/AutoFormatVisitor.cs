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

        return RoslynFormatter.FormatSubtree(cu, tree.Id, tree, _stopAfter);
    }
}
