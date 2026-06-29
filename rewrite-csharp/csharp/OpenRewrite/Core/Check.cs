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
using ExecutionContext = OpenRewrite.Core.ExecutionContext;

namespace OpenRewrite.Core;

/// <summary>
/// A visitor wrapper that runs a precondition check before the actual visitor.
/// If the precondition modifies the tree (indicating a match), the inner visitor runs.
/// Otherwise the tree is returned unchanged.
///
/// Extends TreeVisitor (not JavaVisitor) so it works with any language tree,
/// matching how Java's Preconditions.Check works in rewrite-core.
/// </summary>
public class Check : TreeVisitor<Tree, ExecutionContext>
{
    public ITreeVisitor<ExecutionContext> Precondition { get; }
    public ITreeVisitor<ExecutionContext> Visitor { get; }

    internal Check(ITreeVisitor<ExecutionContext> precondition, ITreeVisitor<ExecutionContext> visitor)
    {
        Precondition = precondition;
        Visitor = visitor;
    }

    public override Tree? Visit(Tree? tree, ExecutionContext ctx)
    {
        if (tree is not SourceFile || Precondition.Visit(tree, ctx) != tree)
        {
            return Visitor.Visit(tree, ctx);
        }
        return tree;
    }
}

/// <summary>
/// A Check that holds a reference to the Recipe used as the precondition,
/// matching the Java RecipeCheck pattern.
/// </summary>
public class RecipeCheck : Check
{
    public Recipe Recipe { get; }

    internal RecipeCheck(Recipe check, ITreeVisitor<ExecutionContext> visitor)
        : base(check.GetVisitor(), visitor)
    {
        Recipe = check;
    }
}

/// <summary>
/// Marker interface for composite precondition visitors (Or / And / Not).
/// Exposes the operator and operands so the RPC server's PrepareRecipe
/// introspection can emit the matching nested wire entry, mirroring
/// Java's <c>org.openrewrite.Preconditions.or</c> / <c>and</c> / <c>not</c>.
/// </summary>
public interface IComposite
{
    /// <summary>The composite operator: <c>"or"</c>, <c>"and"</c>, or <c>"not"</c>.</summary>
    string Op { get; }

    /// <summary>The nested precondition operands.</summary>
    IReadOnlyList<ITreeVisitor<ExecutionContext>> Operands { get; }
}

/// <summary>
/// Negates a precondition: matches when the inner visitor does NOT modify the tree.
/// </summary>
public class NotVisitor : TreeVisitor<Tree, ExecutionContext>, IComposite
{
    private readonly ITreeVisitor<ExecutionContext> _visitor;

    public NotVisitor(ITreeVisitor<ExecutionContext> visitor)
    {
        _visitor = visitor;
    }

    public string Op => "not";
    public IReadOnlyList<ITreeVisitor<ExecutionContext>> Operands => new[] { _visitor };

    public override Tree? Visit(Tree? tree, ExecutionContext ctx)
    {
        var t2 = _visitor.Visit(tree, ctx);
        if (tree == t2 && tree != null)
        {
            return SearchResult.Found(tree);
        }
        return tree;
    }
}

/// <summary>
/// Combines multiple precondition visitors with AND semantics.
/// All must match (modify the tree) for the result to be modified.
/// </summary>
public class AndVisitor : TreeVisitor<Tree, ExecutionContext>, IComposite
{
    private readonly ITreeVisitor<ExecutionContext>[] _visitors;

    public AndVisitor(ITreeVisitor<ExecutionContext>[] visitors)
    {
        _visitors = visitors;
    }

    public string Op => "and";
    public IReadOnlyList<ITreeVisitor<ExecutionContext>> Operands => _visitors;

    public override Tree? Visit(Tree? tree, ExecutionContext ctx)
    {
        Tree? t2 = tree;
        foreach (var v in _visitors)
        {
            t2 = v.Visit(tree, ctx);
            if (tree == t2)
            {
                return tree;
            }
        }
        return t2;
    }
}

/// <summary>
/// Combines multiple precondition visitors with OR semantics.
/// Returns the first operand's modified result if any matches; otherwise
/// returns the original tree unchanged. Mirrors Java's
/// <c>org.openrewrite.Preconditions.or</c>.
/// </summary>
public class OrVisitor : TreeVisitor<Tree, ExecutionContext>, IComposite
{
    private readonly ITreeVisitor<ExecutionContext>[] _visitors;

    public OrVisitor(ITreeVisitor<ExecutionContext>[] visitors)
    {
        _visitors = visitors;
    }

    public string Op => "or";
    public IReadOnlyList<ITreeVisitor<ExecutionContext>> Operands => _visitors;

    public override Tree? Visit(Tree? tree, ExecutionContext ctx)
    {
        foreach (var v in _visitors)
        {
            var t2 = v.Visit(tree, ctx);
            if (t2 != tree)
            {
                return t2;
            }
        }
        return tree;
    }
}
