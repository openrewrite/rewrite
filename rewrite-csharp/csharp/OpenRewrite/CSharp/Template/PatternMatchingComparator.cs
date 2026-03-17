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
using OpenRewrite.Core;
using OpenRewrite.Java;

namespace OpenRewrite.CSharp.Template;

/// <summary>
/// Structural tree comparator for pattern matching.
/// Uses reflection-based property iteration (like Python's dataclass.fields() approach)
/// to generically compare any LST node type without listing them explicitly.
/// When the pattern contains a placeholder identifier, binds the corresponding
/// candidate subtree to the capture name.
/// </summary>
internal class PatternMatchingComparator
{
    private readonly IReadOnlyDictionary<string, object> _captures;
    private readonly Dictionary<string, object> _bindings = new();

    public PatternMatchingComparator(IReadOnlyDictionary<string, object> captures)
    {
        _captures = captures;
    }

    /// <summary>
    /// Attempt to match the pattern against the candidate.
    /// Returns the bindings dictionary if successful, null if the match fails.
    /// </summary>
    public Dictionary<string, object>? Match(J pattern, J candidate, Cursor cursor)
    {
        _bindings.Clear();
        return MatchNode(pattern, candidate, cursor) ? new Dictionary<string, object>(_bindings) : null;
    }

    private bool MatchNode(J pattern, J candidate, Cursor cursor)
    {
        // Check if pattern node is a placeholder identifier
        if (pattern is Identifier patternId)
        {
            var captureName = Placeholder.FromPlaceholder(patternId.SimpleName);
            if (captureName != null && _captures.ContainsKey(captureName))
            {
                // This is a capture placeholder — bind the candidate
                if (_bindings.TryGetValue(captureName, out var existing))
                {
                    // Already bound — check consistency
                    return MatchValue(existing, candidate, cursor);
                }
                _bindings[captureName] = candidate;
                return true;
            }
        }

        // Different node types don't match
        if (pattern.GetType() != candidate.GetType())
            return false;

        // NullSafe marker must match: ?. and . are structurally different
        if (TreeHelper.HasNullSafe(pattern) != TreeHelper.HasNullSafe(candidate))
            return false;

        // Generic property-based comparison: iterate all structural properties
        // and compare them recursively, skipping formatting/identity fields.
        var properties = TreeHelper.GetStructuralProperties(pattern.GetType());
        foreach (var prop in properties)
        {
            var patternValue = prop.GetValue(pattern);
            var candidateValue = prop.GetValue(candidate);

            if (!MatchValue(patternValue, candidateValue, cursor))
                return false;
        }

        return true;
    }

    /// <summary>
    /// Compare two property values, dispatching based on their type.
    /// Handles: null, J nodes, padded wrappers, containers, lists, and primitives.
    /// </summary>
    private bool MatchValue(object? patternValue, object? candidateValue, Cursor cursor)
    {
        // Both null
        if (patternValue == null && candidateValue == null)
            return true;
        // One null
        if (patternValue == null || candidateValue == null)
            return false;

        // J tree nodes — recursive structural match
        if (patternValue is J pj && candidateValue is J cj)
            return MatchNode(pj, cj, cursor);

        // JRightPadded<T> — unwrap and compare the element
        if (TreeHelper.IsRightPadded(patternValue) && TreeHelper.IsRightPadded(candidateValue))
            return MatchPaddedElement(patternValue, candidateValue, cursor);

        // JLeftPadded<T> — unwrap and compare the element
        if (TreeHelper.IsLeftPadded(patternValue) && TreeHelper.IsLeftPadded(candidateValue))
            return MatchPaddedElement(patternValue, candidateValue, cursor);

        // JContainer<T> — compare elements with variadic support
        if (TreeHelper.IsContainer(patternValue) && TreeHelper.IsContainer(candidateValue))
            return MatchContainer(patternValue, candidateValue, cursor);

        // IList — compare element-by-element
        if (patternValue is IList patternList && candidateValue is IList candidateList)
            return MatchList(patternList, candidateList, cursor);

        // Primitive values (string, int, enum, bool, etc.)
        return Equals(patternValue, candidateValue);
    }

    /// <summary>
    /// Compare elements inside padded wrappers (JRightPadded or JLeftPadded).
    /// </summary>
    private bool MatchPaddedElement(object patternPadded, object candidatePadded, Cursor cursor)
    {
        var patternElement = TreeHelper.UnwrapPadded(patternPadded);
        var candidateElement = TreeHelper.UnwrapPadded(candidatePadded);
        return MatchValue(patternElement, candidateElement, cursor);
    }

    /// <summary>
    /// Compare JContainer elements with support for variadic captures.
    /// </summary>
    private bool MatchContainer(object patternContainer, object candidateContainer, Cursor cursor)
    {
        var patternElements = TreeHelper.GetContainerElements(patternContainer);
        var candidateElements = TreeHelper.GetContainerElements(candidateContainer);

        if (patternElements == null || candidateElements == null)
            return patternElements == null && candidateElements == null;

        return MatchPaddedList(patternElements, candidateElements, cursor);
    }

    /// <summary>
    /// Compare two lists of (potentially padded) elements with variadic support.
    /// Uses recursive backtracking so variadic captures can appear at any position.
    /// </summary>
    private bool MatchPaddedList(IList<object> patternElements, IList<object> candidateElements, Cursor cursor)
    {
        return MatchPaddedListRecursive(patternElements, candidateElements, 0, 0, cursor);
    }

    /// <summary>
    /// Recursive sequence matcher with backtracking for variadic captures.
    /// Tries greedy consumption (max to min) and backtracks on failure.
    /// </summary>
    private bool MatchPaddedListRecursive(
        IList<object> patternElements, IList<object> candidateElements,
        int pi, int ci, Cursor cursor)
    {
        // All pattern elements matched; ensure no unconsumed candidates remain
        if (pi >= patternElements.Count)
            return ci >= candidateElements.Count;

        var patternEl = patternElements[pi];
        var innerPattern = TreeHelper.UnwrapPadded(patternEl) ?? patternEl;

        // Check if this is a variadic capture placeholder
        if (innerPattern is Identifier patternId)
        {
            var captureName = Placeholder.FromPlaceholder(patternId.SimpleName);
            if (captureName != null && _captures.TryGetValue(captureName, out var captureObj)
                && IsVariadic(captureObj))
            {
                var (min, max) = GetVariadicBounds(captureObj);

                // Count non-variadic patterns remaining after this one
                var nonVariadicRemaining = 0;
                for (var i = pi + 1; i < patternElements.Count; i++)
                {
                    var inner = TreeHelper.UnwrapPadded(patternElements[i]) ?? patternElements[i];
                    if (inner is Identifier id)
                    {
                        var name = Placeholder.FromPlaceholder(id.SimpleName);
                        if (name != null && _captures.TryGetValue(name, out var cap) && IsVariadic(cap))
                            continue;
                    }
                    nonVariadicRemaining++;
                }

                var remaining = candidateElements.Count - ci;
                var maxPossible = Math.Min(remaining - nonVariadicRemaining, max);

                // Greedy: try from max to min
                for (var consume = maxPossible; consume >= min; consume--)
                {
                    var captured = new List<object>();
                    for (var k = 0; k < consume; k++)
                    {
                        var candidateEl = candidateElements[ci + k];
                        var innerCandidate = TreeHelper.UnwrapPadded(candidateEl) ?? candidateEl;
                        captured.Add(innerCandidate);
                    }

                    // Save bindings for backtracking
                    var savedBindings = new Dictionary<string, object>(_bindings);
                    _bindings[captureName] = captured.AsReadOnly();

                    if (MatchPaddedListRecursive(patternElements, candidateElements, pi + 1, ci + consume, cursor))
                        return true;

                    // Backtrack
                    _bindings.Clear();
                    foreach (var kvp in savedBindings)
                        _bindings[kvp.Key] = kvp.Value;
                }

                return false;
            }
        }

        // Non-variadic: need a candidate element to match against
        if (ci >= candidateElements.Count)
            return false;

        if (!MatchValue(patternEl, candidateElements[ci], cursor))
            return false;

        return MatchPaddedListRecursive(patternElements, candidateElements, pi + 1, ci + 1, cursor);
    }

    /// <summary>
    /// Compare two plain lists (IList of Annotation, Modifier, etc.).
    /// </summary>
    private bool MatchList(IList patternList, IList candidateList, Cursor cursor)
    {
        if (patternList.Count != candidateList.Count)
            return false;

        for (int i = 0; i < patternList.Count; i++)
        {
            if (!MatchValue(patternList[i], candidateList[i], cursor))
                return false;
        }
        return true;
    }

    private static bool IsVariadic(object captureObj)
    {
        var prop = captureObj.GetType().GetProperty("IsVariadic");
        return prop != null && (bool)(prop.GetValue(captureObj) ?? false);
    }

    private static (int min, int max) GetVariadicBounds(object captureObj)
    {
        var type = captureObj.GetType();
        var minProp = type.GetProperty("MinCount");
        var maxProp = type.GetProperty("MaxCount");
        var min = minProp?.GetValue(captureObj) as int? ?? 0;
        var max = maxProp?.GetValue(captureObj) as int? ?? int.MaxValue;
        return (min, max);
    }

}
