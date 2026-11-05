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
using System.Diagnostics;
using System.Text;
using System.Text.RegularExpressions;
using OpenRewrite.Core;
using OpenRewrite.CSharp;
using OpenRewrite.CSharp.Format;
using OpenRewrite.Java;
using ExecutionContext = OpenRewrite.Core.ExecutionContext;

namespace OpenRewrite.Test;

/// <summary>
/// Finds recipes that auto-format a whole source file, namespace or class that existed before the run. Test inputs
/// are usually formatted already, so the reformatting this causes on real code rarely shows in an expected output.
/// </summary>
internal sealed class FormattingScope(Recipe recipe, RunChecks checks) : FormattingListener
{
    private const int SnippetLength = 60;

    private static readonly Regex Whitespace = new(@"\s+");

    private readonly List<Request> _requests = [];
    private readonly HashSet<Tree> _requested = new(ReferenceEqualityComparer.Instance);

    protected override void OnFormat(Tree tree, Tree? stopAfter, Cursor? cursor)
    {
        if (stopAfter != null || !IsWhole(tree) || !_requested.Add(tree))
        {
            return;
        }
        if ((tree as SourceFile ?? cursor?.FirstEnclosing<SourceFile>()) is not { } sourceFile)
        {
            return;
        }
        var frames = new StackTrace(1, true).GetFrames();
        var i = 0;
        var afterVisit = false;
        while (i < frames.Length - 1 && IsPlumbing(frames[i]))
        {
            // a visitor's Visit that calls another visitor's Visit is running its after-visitors
            afterVisit |= IsVisit(frames[i]) && IsVisit(frames[i + 1]);
            i++;
        }
        // Formatters format whole files, and so does a recipe whose own visitor is a formatter
        if (frames[i].GetMethod()?.DeclaringType is not { } caller || InFormatNamespace(caller) ||
            !afterVisit && IsScheduler(caller))
        {
            return;
        }
        // CSharpVisitor.AutoFormat formats a subtree after the visit, as the visit leaves it
        var deferred = tree is not SourceFile && frames.Take(i).Any(IsCSharpVisitorAutoFormat);
        _requests.Add(new Request(tree, sourceFile.Id, (checks.Recipe ?? recipe).Name,
            CallSite(frames[i], caller, frames[Math.Max(i - 1, 0)], afterVisit), Describe(tree), deferred));
    }

    public void AssertFormattedOnlyWhatChanged(IEnumerable<Result> results)
    {
        if (_requests.Count == 0 || IsFormatter(recipe))
        {
            return;
        }
        var resultsByBefore = new Dictionary<Guid, Result>();
        foreach (var result in results)
        {
            if (result is { Before: { } before, After: not null })
            {
                resultsByBefore.TryAdd(before.Id, result);
            }
        }
        var treesByFile = new Dictionary<SourceFile, Dictionary<Guid, J>>(ReferenceEqualityComparer.Instance);
        var found = new Dictionary<string, Dictionary<string, List<string>>>();
        foreach (var request in _requests)
        {
            if (!resultsByBefore.TryGetValue(request.SourceFileId, out var result) || OnlyWhitespaceChanged(result))
            {
                continue;
            }
            var before = result.Before!;
            if (!TreesById(before, treesByFile).TryGetValue(request.Tree.Id, out var original) ||
                request.Tree is not J formatted)
            {
                continue;
            }
            if (request.Deferred)
            {
                formatted = TreesById(result.After!, treesByFile).GetValueOrDefault(formatted.Id, formatted);
            }
            if (!HoldsNoneOfItsChildren(original, formatted))
            {
                if (!found.TryGetValue(request.Recipe, out var callSites))
                {
                    found[request.Recipe] = callSites = new Dictionary<string, List<string>>();
                }
                if (!callSites.TryGetValue(request.CallSite, out var targets))
                {
                    callSites[request.CallSite] = targets = [];
                }
                var target = $"{request.Target} of {before.SourcePath}";
                if (!targets.Contains(target))
                {
                    targets.Add(target);
                }
            }
        }
        if (found.Count > 0)
        {
            Assert.Fail(Message(found));
        }
    }

    /// <summary>A source file, or a namespace, class or enum, which hold all or much of a file.</summary>
    private static bool IsWhole(Tree tree) =>
        tree is SourceFile or ClassDeclaration or EnumDeclaration or NamespaceDeclaration;

    /// <summary>The formatter and the visitor machinery between it and the code that asked for the format.</summary>
    private static bool IsPlumbing(StackFrame frame)
    {
        if (frame.GetMethod() is not { DeclaringType: { } type } method)
        {
            return true;
        }
        var definition = type.IsGenericType ? type.GetGenericTypeDefinition() : type;
        return method.Name is "AutoFormat" or "MaybeAutoFormat" ||
               typeof(FormattingListener).IsAssignableFrom(type) ||
               definition == typeof(TreeVisitor<,>) || definition == typeof(ITreeVisitor<>) ||
               definition == typeof(JavaVisitor<>) || definition == typeof(CSharpVisitor<>) ||
               definition.Name.StartsWith("TreeVisitorAs", StringComparison.Ordinal) ||
               type.Namespace == typeof(AutoFormatVisitor<>).Namespace ||
               typeof(Check).IsAssignableFrom(type) || typeof(IComposite).IsAssignableFrom(type) ||
               type.FullName?.StartsWith("OpenRewrite.Core.ScanningRecipe", StringComparison.Ordinal) == true ||
               type.FullName?.StartsWith("OpenRewrite.Core.TreeVisitorExtensions", StringComparison.Ordinal) == true;
    }

    /// <summary>A visitor's <c>Visit(tree, p)</c>, which runs its after-visitors once it visited the tree.</summary>
    private static bool IsVisit(StackFrame frame) =>
        frame.GetMethod() is { Name: "Visit", DeclaringType: { IsGenericType: true } type } method &&
        type.GetGenericTypeDefinition() == typeof(TreeVisitor<,>) && method.GetParameters().Length == 2;

    private static bool IsScheduler(Type type) =>
        type.FullName?.StartsWith(typeof(RecipeScheduler).FullName!, StringComparison.Ordinal) == true;

    private static bool IsCSharpVisitorAutoFormat(StackFrame frame) =>
        frame.GetMethod() is { Name: "AutoFormat", DeclaringType: { IsGenericType: true } type } &&
        type.GetGenericTypeDefinition() == typeof(CSharpVisitor<>);

    private static string CallSite(StackFrame frame, Type caller, StackFrame entry, bool afterVisit)
    {
        if (afterVisit)
        {
            // the visitor that asked is gone from the stack by the time its after-visitors run
            return "A visitor's DoAfterVisit(new AutoFormatVisitor<P>()) ran on";
        }
        var method = entry.GetMethod();
        var called = method?.Name is "AutoFormat" or "MaybeAutoFormat"
            ? $" called {method.Name} on"
            : method?.DeclaringType == typeof(RoslynFormatter)
                ? " called RoslynFormatter.Format on"
                : " ran an AutoFormatVisitor on";
        var file = frame.GetFileName() is { } path ? $"{Path.GetFileName(path)}:{frame.GetFileLineNumber()}"
            : "unknown source";
        return $"{TypeName(caller)}.{frame.GetMethod()!.Name}({file}){called}";
    }

    // A formatting recipe may format whole files, and so may the recipes run alongside it
    private static bool IsFormatter(Recipe recipe)
    {
        if (InFormatNamespace(recipe.GetType()) || recipe is not IScanningRecipe && VisitsWithAFormatter(recipe))
        {
            return true;
        }
        try
        {
            return recipe.GetRecipeList().Any(IsFormatter);
        }
        catch (Exception)
        {
            return false;
        }
    }

    private static bool VisitsWithAFormatter(Recipe recipe)
    {
        try
        {
            return recipe.GetVisitor() is AutoFormatVisitor<ExecutionContext>;
        }
        catch (Exception)
        {
            return false;
        }
    }

    private static bool InFormatNamespace(Type type) => ("." + type.Namespace + ".").Contains(".Format.");

    private static bool OnlyWhitespaceChanged(Result result) =>
        Whitespace.Replace(Print(result.Before!), "") == Whitespace.Replace(Print(result.After!), "");

    private static Dictionary<Guid, J> TreesById(SourceFile sourceFile,
        Dictionary<SourceFile, Dictionary<Guid, J>> treesByFile)
    {
        if (!treesByFile.TryGetValue(sourceFile, out var trees))
        {
            treesByFile[sourceFile] = trees = new Dictionary<Guid, J>();
            new TreeCollector(trees).Visit(sourceFile, 0);
        }
        return trees;
    }

    private sealed class TreeCollector(Dictionary<Guid, J> trees) : CSharpVisitor<int>
    {
        public override J? PreVisit(J tree, int p)
        {
            trees.TryAdd(tree.Id, tree);
            return tree;
        }
    }

    /// <summary>
    /// A tree formatted with none of the children it had reformats no existing code below it: a class with its body
    /// emptied to be put back afterwards, or a copy of a container holding only a new element. A tree that had no
    /// children to begin with would format them on real code.
    /// </summary>
    private static bool HoldsNoneOfItsChildren(J original, J formatted)
    {
        var children = Children(original);
        return children.Count > 0 && !children.Overlaps(Children(formatted));
    }

    /// <summary>The members of a class's body, or the direct children of any other tree.</summary>
    private static HashSet<Guid> Children(J tree)
    {
        J parent = tree is ClassDeclaration classDecl ? classDecl.Body : tree;
        var children = new HashSet<Guid>();
        new ChildCollector(parent, children).Visit(parent, 0);
        return children;
    }

    private sealed class ChildCollector(J parent, HashSet<Guid> children) : CSharpVisitor<int>
    {
        public override J? PreVisit(J tree, int p)
        {
            if (!ReferenceEquals(tree, parent))
            {
                StopAfterPreVisit();
                children.Add(tree.Id);
            }
            return tree;
        }
    }

    private static string Describe(Tree tree) => tree switch
    {
        SourceFile => "all",
        ClassDeclaration classDecl => $"class `{classDecl.Name.SimpleName}`",
        EnumDeclaration enumDecl => $"enum `{enumDecl.NamePadded.Element.SimpleName}`",
        NamespaceDeclaration ns => $"namespace `{Snippet(ns.Name.Element)}`",
        _ => TypeName(tree.GetType())
    };

    private static string Snippet(J tree)
    {
        try
        {
            var printed = Whitespace.Replace(Print(J.SetPrefix(tree, Space.Empty)).Trim(), " ");
            return printed.Length > SnippetLength ? printed[..SnippetLength] + "..." : printed;
        }
        catch (Exception)
        {
            return "?";
        }
    }

    private static string TypeName(Type type)
    {
        var name = type.Name;
        var arity = name.IndexOf('`');
        if (arity >= 0)
        {
            name = name[..arity];
        }
        return type.DeclaringType == null ? name : TypeName(type.DeclaringType) + "." + name;
    }

    private static string Print(Tree tree)
    {
        var capture = new PrintOutputCapture<object>(null!, MarkerPrinter.Sanitized);
        if (tree is Xml.Xml)
        {
            new Xml.XmlPrinter<object>().Visit(tree, capture);
        }
        else
        {
            new CSharpPrinter<object>().Visit(tree, capture);
        }
        return capture.ToString();
    }

    private static string Message(Dictionary<string, Dictionary<string, List<string>>> found)
    {
        var message = new StringBuilder(
            "Format only what the recipe changed, not the whole class, namespace or file around it. These calls " +
            "auto-formatted a whole file, namespace or class that existed before the run, with no stopAfter:\n");
        foreach (var (recipeName, callSites) in found)
        {
            message.Append("  ").Append(recipeName).Append(":\n");
            foreach (var (callSite, targets) in callSites)
            {
                message.Append("    ").Append(callSite).Append('\n');
                foreach (var target in targets)
                {
                    message.Append("      ").Append(target).Append('\n');
                }
            }
        }
        return message
            .Append("That rewrites the whitespace of code the recipe did not change. Test inputs are usually ")
            .Append("formatted already, so expected outputs rarely show it, but on real code the diff will carry ")
            .Append("unrelated reformatting. Instead:\n")
            .Append("  - format the element you created or replaced: MaybeAutoFormat(original, changed, ctx, Cursor) ")
            .Append("on that element, or AutoFormat(newElement, ctx, Cursor)\n")
            .Append("  - after changing a declaration's attributes, modifiers or base types, stop after its header: ")
            .Append("MaybeAutoFormat(classDecl, updated, updated.Name, ctx, Cursor.ParentTree)\n")
            .Append("  - when you remove an element, give its prefix to the element that takes its place\n")
            .Append("  - a CSharpTemplate already formats what it inserts, so drop the extra call\n")
            .Append("Recipes in a *.Format namespace are exempt. Any other recipe whose purpose is formatting opts ")
            .Append("out with spec.SetValidations(new Validations { MinimalFormatting = false }).")
            .ToString();
    }

    /// <param name="Recipe">The name of the recipe the scheduler was running.</param>
    /// <param name="Deferred">Whether the tree is formatted once the visit is done, as the visit leaves it.</param>
    private sealed record Request(Tree Tree, Guid SourceFileId, string Recipe, string CallSite, string Target,
        bool Deferred);
}
