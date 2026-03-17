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
using System.Collections;
using OpenRewrite.CSharp.Template;
using OpenRewrite.Java;

namespace OpenRewrite.CSharp;

/// <summary>
/// Compares two J (LST) nodes for structural equality, ignoring whitespace,
/// formatting, markers, node IDs, and type attribution.
/// </summary>
public static class StructuralEqualityComparator
{
    /// <summary>
    /// Returns true if the two nodes are structurally equal — same node types,
    /// operators, names, literals, and child structure — regardless of formatting.
    /// </summary>
    public static bool AreStructurallyEqual(J a, J b)
    {
        if (ReferenceEquals(a, b))
            return true;
        return CompareNodes(a, b);
    }

    private static bool CompareNodes(J a, J b)
    {
        if (a.GetType() != b.GetType())
            return false;

        // NullSafe marker distinguishes ?. from . — this is semantic, not formatting
        if (TreeHelper.HasNullSafe(a) != TreeHelper.HasNullSafe(b))
            return false;

        var properties = TreeHelper.GetStructuralProperties(a.GetType());
        foreach (var prop in properties)
        {
            var valA = prop.GetValue(a);
            var valB = prop.GetValue(b);

            if (!CompareValues(valA, valB))
                return false;
        }

        return true;
    }

    private static bool CompareValues(object? a, object? b)
    {
        if (ReferenceEquals(a, b))
            return true;
        if (a == null || b == null)
            return false;

        // J tree nodes — recursive structural comparison
        if (a is J ja && b is J jb)
            return CompareNodes(ja, jb);

        // Generic wrappers — get type info once and dispatch
        var typeA = a.GetType();
        var typeB = b.GetType();
        if (typeA.IsGenericType && typeB.IsGenericType)
        {
            var defA = typeA.GetGenericTypeDefinition();
            var defB = typeB.GetGenericTypeDefinition();
            if (defA == defB)
            {
                // JRightPadded<T> / JLeftPadded<T> — unwrap and recurse
                if (defA == typeof(JRightPadded<>) || defA == typeof(JLeftPadded<>))
                    return CompareValues(TreeHelper.UnwrapPadded(a), TreeHelper.UnwrapPadded(b));

                // JContainer<T> — extract elements (returns JRightPadded<T> wrappers) and compare
                if (defA == typeof(JContainer<>))
                {
                    var elemsA = TreeHelper.GetContainerElements(a);
                    var elemsB = TreeHelper.GetContainerElements(b);
                    if (elemsA == null && elemsB == null) return true;
                    if (elemsA == null || elemsB == null) return false;
                    if (elemsA.Count != elemsB.Count) return false;
                    for (int i = 0; i < elemsA.Count; i++)
                    {
                        if (!CompareValues(elemsA[i], elemsB[i]))
                            return false;
                    }
                    return true;
                }
            }
        }

        // IList — lock-step element comparison
        if (a is IList listA && b is IList listB)
            return CompareList(listA, listB);

        // Primitives and enums
        return Equals(a, b);
    }

    private static bool CompareList(IList a, IList b)
    {
        if (a.Count != b.Count)
            return false;

        for (int i = 0; i < a.Count; i++)
        {
            if (!CompareValues(a[i], b[i]))
                return false;
        }
        return true;
    }
}
