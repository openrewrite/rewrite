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
using System.Collections.Generic;

namespace OpenRewrite.Core;

/// <summary>
/// Tracks the path from root to the current node during tree traversal.
/// Enables visitors to access parent context when making decisions.
/// </summary>
public class Cursor
{
    public const string ROOT_VALUE = "root";

    private readonly Cursor? _parent;

    public object? Value { get; }

    public Cursor() : this(null, ROOT_VALUE)
    {
    }

    public Cursor(Cursor? parent, object? value)
    {
        _parent = parent;
        Value = value;
    }

    public bool IsRoot => Value?.ToString() == ROOT_VALUE;

    public Cursor? Parent => _parent;

    /// <summary>
    /// Gets an ancestor cursor at the specified number of levels up.
    /// </summary>
    public Cursor? GetParent(int levels = 1)
    {
        var cursor = this;
        for (var i = 0; i < levels && cursor != null; i++)
        {
            cursor = cursor._parent;
        }
        return cursor;
    }

    /// <summary>
    /// Gets the value cast to the specified type, or null if not of that type.
    /// </summary>
    public T? GetValue<T>() where T : class
    {
        return Value as T;
    }

    /// <summary>
    /// Finds the first enclosing (ancestor) node of the specified type.
    /// </summary>
    public T? FirstEnclosing<T>() where T : class
    {
        var c = this;
        while (c != null)
        {
            if (c.Value is T value)
                return value;
            c = c.Parent;
        }
        return default;
    }

    /// <summary>
    /// Finds the first enclosing node of the specified type, throwing if not found.
    /// </summary>
    public T FirstEnclosingOrThrow<T>() where T : class
    {
        return FirstEnclosing<T>() ??
               throw new InvalidOperationException($"Expected to find enclosing {typeof(T).Name}");
    }

    /// <summary>
    /// Gets the root cursor.
    /// </summary>
    public Cursor GetRoot()
    {
        var c = this;
        while (c.Parent != null && !c.Parent.IsRoot)
        {
            c = c.Parent;
        }
        return c;
    }

    /// <summary>
    /// Drops parents until a predicate matches.
    /// </summary>
    public Cursor DropParentUntil(Predicate<object?> valuePredicate)
    {
        Cursor? cursor = Parent;
        while (cursor != null && !valuePredicate(cursor.Value))
        {
            cursor = cursor.Parent;
        }
        if (cursor == null)
        {
            throw new InvalidOperationException("Expected to find a matching parent for " + this);
        }
        return cursor;
    }

    /// <summary>
    /// The first parent pointing to a Tree element, skipping padding.
    /// </summary>
    public Cursor ParentTree => DropParentUntil(it => it is Tree || Equals(it, ROOT_VALUE));

    /// <summary>
    /// Enumerates all ancestor cursors from the immediate parent to the root.
    /// </summary>
    public IEnumerable<Cursor> PathToRoot()
    {
        var cursor = _parent;
        while (cursor != null)
        {
            yield return cursor;
            cursor = cursor._parent;
        }
    }
}
