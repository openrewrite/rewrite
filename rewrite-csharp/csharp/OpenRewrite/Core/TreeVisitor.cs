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
namespace OpenRewrite.Core;

/// <summary>
/// Base visitor for traversing and transforming LST elements.
/// Manages the visit lifecycle: cursor tracking, pre/post visit hooks, and after-visit chaining.
/// Subclasses override <see cref="Accept"/> to dispatch to type-specific visit methods.
/// </summary>
/// <typeparam name="T">The type of tree this visitor handles.</typeparam>
/// <typeparam name="P">A context object passed through every visit method.</typeparam>
/// <summary>
/// Non-generic visitor interface so that Recipe.GetVisitor() can return
/// a visitor without binding to a specific tree type (equivalent to
/// Java's TreeVisitor&lt;?, P&gt;).
/// </summary>
public interface ITreeVisitor<P>
{
    Tree? Visit(Tree? tree, P p);

    static ITreeVisitor<P> Noop() => new NoopVisitor<P>();
}

internal class NoopVisitor<P> : ITreeVisitor<P>
{
    public Tree? Visit(Tree? tree, P p) => tree;
}

public class TreeVisitor<T, P> : ITreeVisitor<P> where T : class, Tree
{
    Tree? ITreeVisitor<P>.Visit(Tree? tree, P p)
    {
        // If the tree is a SourceFile that doesn't match this visitor's type parameter T,
        // pass it through unchanged. Returning null from Visit() for incompatible types
        // would be interpreted as "delete this file" by the recipe scheduler.
        if (tree is SourceFile && tree is not T)
            return tree;
        return Visit(tree, p);
    }
    public Cursor Cursor { get; set; } = new();
    private int _visitCount;
    private bool _stopAfterPreVisit;
    private List<TreeVisitor<T, P>>? _afterVisit;

    public virtual T? PreVisit(T tree, P p) => tree;

    public virtual T? PostVisit(T tree, P p) => tree;

    /// <summary>
    /// Subclasses override to dispatch to type-specific visit methods.
    /// This is the C# equivalent of Java's <c>tree.accept(visitor, p)</c> double-dispatch,
    /// implemented via switch pattern matching on the visitor side.
    /// </summary>
    protected virtual T? Accept(T tree, P p) => tree;

    public virtual T? Visit(Tree? tree, P p)
    {
        if (tree == null) return DefaultValue(null, p);
        if (tree is not T typed) return DefaultValue(tree, p);

        bool topLevel = _visitCount == 0;
        _visitCount++;
        Cursor = new Cursor(Cursor, tree);

        T? t = PreVisit(typed, p);
        if (!_stopAfterPreVisit)
        {
            if (t != null) t = Accept(t, p);
            if (t != null) t = PostVisit(t, p);
        }
        _stopAfterPreVisit = false;

        Cursor = Cursor.Parent!;

        if (topLevel)
        {
            if (t != null && _afterVisit != null)
            {
                foreach (var v in _afterVisit)
                {
                    v.Cursor = Cursor;
                    t = (T?)v.Visit(t, p);
                }
            }

            _afterVisit = null;
            _visitCount = 0;
        }

        return t;
    }

    public T? Visit(Tree? tree, P p, Cursor parent)
    {
        Cursor = parent;
        return Visit(tree, p);
    }

    public T VisitNonNull(Tree tree, P p)
        => Visit(tree, p) ?? throw new InvalidOperationException("Expected non-null visit result");

    public virtual T? DefaultValue(Tree? tree, P p) => tree as T;

    /// <summary>
    /// Call from <see cref="PreVisit"/> to skip Accept and PostVisit for the current node.
    /// Used by RpcVisitor to delegate the full tree transformation to a remote peer.
    /// </summary>
    protected void StopAfterPreVisit() => _stopAfterPreVisit = true;

    /// <summary>
    /// Register a visitor to run once after the whole source file has been visited.
    /// Ideal for one-off operations like auto-formatting, adding/removing imports, etc.
    /// </summary>
    protected void DoAfterVisit(TreeVisitor<T, P> visitor)
    {
        _afterVisit ??= new List<TreeVisitor<T, P>>(2);
        _afterVisit.Add(visitor);
    }

    protected IReadOnlyList<TreeVisitor<T, P>> GetAfterVisit() =>
        _afterVisit ?? (IReadOnlyList<TreeVisitor<T, P>>)[];

    /// <summary>
    /// Register an after-visitor only if an equal instance is not already registered.
    /// Requires the visitor to implement proper equality (e.g., <see cref="IEquatable{T}"/>).
    /// </summary>
    protected void MaybeDoAfterVisit(TreeVisitor<T, P> visitor)
    {
        if (_afterVisit == null || !_afterVisit.Contains(visitor))
            DoAfterVisit(visitor);
    }

    public static TreeVisitor<T, P> Noop() => new NoopVisitor();

    private class NoopVisitor : TreeVisitor<T, P>
    {
        public override T? Visit(Tree? tree, P p) => tree as T;
    }
}
