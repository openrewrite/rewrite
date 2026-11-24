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
using ExecutionContext = OpenRewrite.Core.ExecutionContext;

namespace OpenRewrite.CSharp;

/// <summary>
/// Runs a visitor on everything but generated C# files. In a file that is only partly generated, the visitor's
/// changes to a generated type are undone; a type with a generated member that the visitor changed is undone as a
/// whole, so that its members stay consistent with each other. A file whose usings are all that then differs is left
/// alone too, as the visitor changed them for the sake of the generated code.
/// </summary>
internal sealed class GeneratedCodeGuard(ITreeVisitor<ExecutionContext> visitor) : ITreeVisitor<ExecutionContext>
{
    public Tree? Visit(Tree? tree, ExecutionContext ctx) =>
        Guarded(tree, () => visitor.Visit(tree, ctx));

    public Tree? Visit(Tree? tree, ExecutionContext ctx, Cursor parent) =>
        Guarded(tree, () => visitor.Visit(tree, ctx, parent));

    private static Tree? Guarded(Tree? tree, Func<Tree?> visit)
    {
        if (tree is not CompilationUnit before)
            return visit();
        if (GeneratedCode.IsGenerated(before))
            return before;

        var result = visit();
        return result is CompilationUnit after && !ReferenceEquals(before, after) ? Restore(before, after) : result;
    }

    private static CompilationUnit Restore(CompilationUnit before, CompilationUnit after)
    {
        var generated = new Dictionary<Guid, (J Declaration, J Type)>();
        new Collector(generated).Visit(before, 0);
        if (generated.Count == 0)
            return after;

        var current = new Dictionary<Guid, J>();
        new Finder(generated.Values.SelectMany(g => new[] { g.Declaration.Id, g.Type.Id }).ToHashSet(), current)
            .Visit(after, 0);

        var restored = new Dictionary<Guid, J>();
        foreach (var (declaration, type) in generated.Values)
        {
            if (current.TryGetValue(declaration.Id, out var now) && IsUnchanged(declaration, now))
                continue;
            if (!current.ContainsKey(type.Id))
                return before;
            restored[type.Id] = type;
        }
        if (restored.Count == 0)
            return after;

        var result = (CompilationUnit)new Restorer(restored).VisitNonNull(after, 0);
        return ChangesOnlyUsings(before, result) ? before : result;
    }

    private static bool ChangesOnlyUsings(CompilationUnit before, CompilationUnit after)
    {
        var printer = new CSharpPrinter<int>();
        return printer.Print(after.WithUsings(before.Usings)) == printer.Print(before);
    }

    private static bool IsUnchanged(J before, J after) =>
        ReferenceEquals(before, after) ||
        before is AnnotatedStatement was && after is AnnotatedStatement now &&
        ReferenceEquals(was.Markers, now.Markers) &&
        ReferenceEquals(was.AttributeLists, now.AttributeLists) &&
        ReferenceEquals(was.Statement, now.Statement);

    private sealed class Collector(Dictionary<Guid, (J, J)> generated) : CSharpVisitor<int>
    {
        private readonly Stack<J> _types = new();

        public override J? Visit(Tree? tree, int p)
        {
            if (tree is not Statement statement)
                return base.Visit(tree, p);

            var isType = GeneratedCode.IsType(statement);
            if (GeneratedCode.IsGenerated(statement) && (isType || _types.Count > 0))
            {
                generated[statement.Id] = (statement, isType ? statement : _types.Peek());
                return statement;
            }

            var opens = isType && Cursor.Value is not AnnotatedStatement;
            if (opens)
                _types.Push(statement);
            base.Visit(tree, p);
            if (opens)
                _types.Pop();
            return statement;
        }
    }

    private sealed class Finder(HashSet<Guid> ids, Dictionary<Guid, J> found) : CSharpVisitor<int>
    {
        public override J? PreVisit(J tree, int p)
        {
            if (ids.Contains(tree.Id))
                found.TryAdd(tree.Id, tree);
            return tree;
        }
    }

    private sealed class Restorer(Dictionary<Guid, J> restored) : CSharpVisitor<int>
    {
        public override J? PreVisit(J tree, int p)
        {
            if (!restored.TryGetValue(tree.Id, out var original))
                return tree;
            StopAfterPreVisit();
            return J.SetPrefix(original, tree.Prefix);
        }
    }
}
