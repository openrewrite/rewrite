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
    /// </summary>
    public static CompilationUnit Format(CompilationUnit cu, J? targetSubtree, J? stopAfter)
    {
        // 1. Print to string
        var printer = new CSharpPrinter<int>();
        var source = printer.Print(cu);

        // 2. Detect style
        var style = FormatStyle.DetectStyle(source);

        // 3. Format with Roslyn
        var formattedSource = FormatWithRoslyn(source, style);

        // 4. If formatting didn't change anything, return original
        if (string.Equals(source, formattedSource, StringComparison.Ordinal))
            return cu;

        // 5. Parse formatted string back to LST (no type attribution)
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

        // 6. Reconcile whitespace
        var reconciler = new WhitespaceReconciler();
        var result = reconciler.Reconcile(cu, formattedCu, targetSubtree, stopAfter);

        if (!reconciler.IsCompatible)
            return cu;

        return result as CompilationUnit ?? cu;
    }

    internal static string FormatWithRoslyn(string source, FormatStyle style)
    {
        var syntaxTree = CSharpSyntaxTree.ParseText(source);
        var root = syntaxTree.GetRoot();

        using var workspace = new AdhocWorkspace();
        var options = workspace.Options
            .WithChangedOption(FormattingOptions.UseTabs, LanguageNames.CSharp, style.UseTabs)
            .WithChangedOption(FormattingOptions.IndentationSize, LanguageNames.CSharp, style.IndentationSize)
            .WithChangedOption(FormattingOptions.TabSize, LanguageNames.CSharp, style.IndentationSize)
            .WithChangedOption(FormattingOptions.NewLine, LanguageNames.CSharp, style.NewLine);

        var formatted = Formatter.Format(root, workspace, options);
        return formatted.ToFullString();
    }
}
