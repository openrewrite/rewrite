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
    private readonly Dictionary<string, NullSafe> _nullSafeBindings = new();

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
        _nullSafeBindings.Clear();
        return MatchNode(pattern, candidate, cursor) ? new Dictionary<string, object>(_bindings) : null;
    }

    /// <summary>
    /// After a successful <see cref="Match"/>, returns capture names mapped to the
    /// <see cref="NullSafe"/> marker from the candidate MethodInvocation/FieldAccess
    /// when the capture was in the Select position. Used by the template engine to
    /// preserve <c>?.</c> through rewrites.
    /// </summary>
    internal IReadOnlyDictionary<string, NullSafe> NullSafeBindings => _nullSafeBindings;

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
                // Evaluate constraint before binding — pass the pattern placeholder's
                // resolved type so typed captures can compare JavaType-to-JavaType
                if (!EvaluateConstraint(_captures[captureName], candidate, cursor, patternId.Type))
                    return false;
                _bindings[captureName] = candidate;
                return true;
            }
        }

        // Different node types — check for cross-type equivalences before failing
        if (pattern.GetType() != candidate.GetType())
            return MatchCrossType(pattern, candidate, cursor);

        // NullSafe: a pattern with ?. only matches candidates with ?.
        // but a pattern without ?. matches both (asymmetric — patterns are lenient)
        if (TreeHelper.HasNullSafe(pattern) && !TreeHelper.HasNullSafe(candidate))
            return false;

        // Semantic matching for method invocations: when both resolve to the same
        // static method (same declaring type + name), skip receiver comparison.
        bool matched;
        if (pattern is MethodInvocation patMethod && candidate is MethodInvocation candMethod)
            matched = MatchMethodInvocation(patMethod, candMethod, cursor);
        else if (pattern is Binary patBin && candidate is Binary candBin)
        {
            // Generic property-based comparison with backtracking for commutative ops
            var savedBindings = new Dictionary<string, object>(_bindings);
            matched = MatchProperties(pattern, candidate, cursor);
            if (!matched)
            {
                // Restore bindings and try commuted (swapped) operands for == and !=
                _bindings.Clear();
                foreach (var kvp in savedBindings)
                    _bindings[kvp.Key] = kvp.Value;
                matched = MatchCommutedBinary(patBin, candBin, cursor);
            }
        }
        else
            matched = MatchProperties(pattern, candidate, cursor);

        // Record NullSafe associations for captures used as Select in MI/FA
        if (matched)
            RecordNullSafeForCaptures(pattern, candidate);

        return matched;
    }

    /// <summary>
    /// After a successful match of a MethodInvocation, FieldAccess, or ArrayAccess,
    /// check if the candidate has a NullSafe marker that the pattern doesn't have.
    /// If so, find the capture placeholder in the Select/Target/Indexed position and
    /// record the NullSafe association so the template engine can preserve <c>?.</c>
    /// and <c>?[</c> through rewrites.
    /// </summary>
    private void RecordNullSafeForCaptures(J pattern, J candidate)
    {
        if (pattern is MethodInvocation patMi && candidate is MethodInvocation candMi)
        {
            var candNullSafe = candMi.Markers.FindFirst<NullSafe>();
            if (candNullSafe != null && patMi.Markers.FindFirst<NullSafe>() == null)
            {
                if (FindSelectCaptureName(patMi.Select) is { } captureName)
                    _nullSafeBindings[captureName] = candNullSafe;
            }
        }
        else if (pattern is FieldAccess patFa && candidate is FieldAccess candFa)
        {
            var candNullSafe = candFa.Markers.FindFirst<NullSafe>();
            if (candNullSafe != null && patFa.Markers.FindFirst<NullSafe>() == null)
            {
                if (FindTargetCaptureName(patFa.Target) is { } captureName)
                    _nullSafeBindings[captureName] = candNullSafe;
            }
        }
        else if (pattern is ArrayAccess patAa && candidate is ArrayAccess candAa)
        {
            var candNullSafe = candAa.Markers.FindFirst<NullSafe>();
            if (candNullSafe != null && patAa.Markers.FindFirst<NullSafe>() == null)
            {
                if (FindTargetCaptureName(patAa.Indexed) is { } captureName)
                    _nullSafeBindings[captureName] = candNullSafe;
            }
        }
    }

    /// <summary>
    /// Check if a MI's Select (JRightPadded&lt;Expression&gt;?) contains a direct
    /// capture placeholder, returning its name if so.
    /// </summary>
    private string? FindSelectCaptureName(JRightPadded<Expression>? select)
    {
        if (select?.Element is Identifier ident)
        {
            var name = Placeholder.FromPlaceholder(ident.SimpleName);
            if (name != null && _captures.ContainsKey(name))
                return name;
        }
        return null;
    }

    /// <summary>
    /// Check if a FieldAccess's Target is a direct capture placeholder.
    /// </summary>
    private string? FindTargetCaptureName(Expression target)
    {
        if (target is Identifier ident)
        {
            var name = Placeholder.FromPlaceholder(ident.SimpleName);
            if (name != null && _captures.ContainsKey(name))
                return name;
        }
        return null;
    }

    /// <summary>
    /// Compare all structural properties of two same-type nodes.
    /// </summary>
    private bool MatchProperties(J pattern, J candidate, Cursor cursor)
    {
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
    /// For Binary == and != where one side is a literal, try matching with
    /// left and right swapped (commutative match).
    /// </summary>
    private bool MatchCommutedBinary(Binary pattern, Binary candidate, Cursor cursor)
    {
        var op = pattern.Operator.Element;
        if (op != Binary.OperatorType.Equal && op != Binary.OperatorType.NotEqual)
            return false;

        if (!HasLiteral(pattern) && !HasLiteral(candidate))
            return false;

        // Operator must still match
        if (!MatchPaddedElement(pattern.Operator, candidate.Operator, cursor))
            return false;

        // Try swapped: pattern.Left ↔ candidate.Right, pattern.Right ↔ candidate.Left
        return MatchNode(pattern.Left, candidate.Right, cursor)
            && MatchNode(pattern.Right, candidate.Left, cursor);
    }

    /// <summary>
    /// Semantic matching for method invocations. When both sides have a MethodType
    /// that is static and shares the same declaring type FQN and method name,
    /// skip comparing the receiver (Select) — the method is the same regardless
    /// of whether it's called as <c>Math.Abs(x)</c> or <c>Abs(x)</c> via using static.
    /// Falls back to structural matching otherwise.
    /// </summary>
    private bool MatchMethodInvocation(MethodInvocation pattern, MethodInvocation candidate, Cursor cursor)
    {
        var patType = pattern.MethodType;
        var candType = candidate.MethodType;

        // If both have type info, same method name, and the method is static,
        // compare by declaring type instead of by receiver syntax
        if (patType != null && candType != null
            && patType.Name == candType.Name
            && IsStatic(patType) && IsStatic(candType)
            && GetDeclaringTypeFqn(patType) is { } patFqn
            && GetDeclaringTypeFqn(candType) is { } candFqn
            && patFqn == candFqn)
        {
            // Declaring type and method name match — skip Select, compare the rest
            return MatchNode(pattern.Name, candidate.Name, cursor)
                && MatchValue(pattern.TypeParameters, candidate.TypeParameters, cursor)
                && MatchValue(pattern.Arguments, candidate.Arguments, cursor);
        }

        // Implicit this: Foo() ↔ this.Foo()
        // When one side has no Select and the other's Select is "this", they are equivalent
        var patSelect = pattern.Select != null ? TreeHelper.UnwrapPadded(pattern.Select) as J : null;
        var candSelect = candidate.Select != null ? TreeHelper.UnwrapPadded(candidate.Select) as J : null;
        if (IsImplicitThisPair(patSelect, candSelect))
        {
            return MatchNode(pattern.Name, candidate.Name, cursor)
                && MatchValue(pattern.TypeParameters, candidate.TypeParameters, cursor)
                && MatchValue(pattern.Arguments, candidate.Arguments, cursor);
        }

        // Fall back to structural matching
        return MatchProperties(pattern, candidate, cursor);
    }

    /// <summary>
    /// Handle cross-type equivalences (e.g. <c>== null</c> ↔ <c>is null</c>,
    /// <c>Identifier</c> ↔ <c>FieldAccess</c> for static members).
    /// Called when pattern and candidate have different node types.
    /// </summary>
    /// <summary>
    /// Returns <c>true</c> when the two node types form a pair that
    /// <see cref="MatchCrossType"/> knows how to compare. Used by
    /// <see cref="CSharpPattern.Match"/> to avoid rejecting these pairs
    /// in its fast-reject check.
    /// </summary>
    internal static bool HasCrossTypeEquivalence(J pattern, J candidate)
    {
        return (pattern is Binary && candidate is IsPattern)
            || (pattern is IsPattern && candidate is Binary)
            || (pattern is FieldAccess && candidate is Identifier)
            || (pattern is Identifier && candidate is FieldAccess);
    }

    private bool MatchCrossType(J pattern, J candidate, Cursor cursor)
    {
        // Binary(expr == null) pattern ↔ IsPattern(expr is null) candidate
        if (pattern is Binary patBin && candidate is IsPattern candIsP)
            return MatchBinaryPatternToIsNullCandidate(patBin, candIsP, cursor);
        // IsPattern(expr is null) pattern ↔ Binary(expr == null) candidate
        if (pattern is IsPattern patIsP && candidate is Binary candBin)
            return MatchIsNullPatternToBinaryCandidate(patIsP, candBin, cursor);

        // Identifier ↔ FieldAccess for static members and type references
        if (pattern is FieldAccess patFA && candidate is Identifier candId)
            return MatchFieldAccessToIdentifier(patFA, candId);
        if (pattern is Identifier patId2 && candidate is FieldAccess candFA)
            return MatchIdentifierToFieldAccess(patId2, candFA);

        return false;
    }

    /// <summary>
    /// Pattern is a FieldAccess (e.g. <c>Math.PI</c>), candidate is an Identifier
    /// (e.g. <c>PI</c> via using static). Match if both reference the same static
    /// variable (same owner FQN and name) or the same fully qualified type.
    /// </summary>
    private static bool MatchFieldAccessToIdentifier(FieldAccess pattern, Identifier candidate)
    {
        // Static field/variable: Math.PI ↔ PI
        if (pattern.Name.Element.FieldType is JavaType.Variable patVar
            && candidate.FieldType is JavaType.Variable candVar
            && IsStatic(patVar) && IsStatic(candVar)
            && patVar.Name == candVar.Name
            && patVar.Owner is JavaType.Class patOwner
            && candVar.Owner is JavaType.Class candOwner
            && patOwner.FullyQualifiedName == candOwner.FullyQualifiedName)
        {
            return true;
        }

        // Type reference: System.Console ↔ Console (same FullyQualified type)
        if (pattern.Type is JavaType.Class patTypeFq
            && candidate.Type is JavaType.Class candTypeFq
            && patTypeFq.FullyQualifiedName == candTypeFq.FullyQualifiedName
            && candidate.FieldType == null && pattern.Name.Element.FieldType == null)
        {
            return true;
        }

        return false;
    }

    /// <summary>
    /// Pattern is an Identifier (e.g. <c>PI</c> via using static), candidate is
    /// a FieldAccess (e.g. <c>Math.PI</c>). Symmetric to <see cref="MatchFieldAccessToIdentifier"/>.
    /// </summary>
    private static bool MatchIdentifierToFieldAccess(Identifier pattern, FieldAccess candidate)
    {
        return MatchFieldAccessToIdentifier(candidate, pattern);
    }

    /// <summary>
    /// Pattern is <c>expr == null</c> (Binary), candidate is <c>expr is null</c> (IsPattern).
    /// Also handles commuted pattern <c>null == expr</c>.
    /// </summary>
    private bool MatchBinaryPatternToIsNullCandidate(Binary patternBinary, IsPattern candidateIsPattern, Cursor cursor)
    {
        if (patternBinary.Operator.Element != Binary.OperatorType.Equal)
            return false;

        if (candidateIsPattern.Pattern.Element is not ConstantPattern cp || !IsNullLiteral(cp.Value))
            return false;

        // pattern: {s} == null → match {s} against candidate's expression
        if (IsNullLiteral(patternBinary.Right))
            return MatchNode(patternBinary.Left, candidateIsPattern.Expression, cursor);
        // pattern: null == {s} → match {s} against candidate's expression
        if (IsNullLiteral(patternBinary.Left))
            return MatchNode(patternBinary.Right, candidateIsPattern.Expression, cursor);

        return false;
    }

    /// <summary>
    /// Pattern is <c>expr is null</c> (IsPattern), candidate is <c>expr == null</c> (Binary).
    /// Also handles commuted candidate <c>null == expr</c>.
    /// </summary>
    private bool MatchIsNullPatternToBinaryCandidate(IsPattern patternIsPattern, Binary candidateBinary, Cursor cursor)
    {
        if (candidateBinary.Operator.Element != Binary.OperatorType.Equal)
            return false;

        if (patternIsPattern.Pattern.Element is not ConstantPattern cp || !IsNullLiteral(cp.Value))
            return false;

        // candidate: expr == null → match pattern's expression against candidate's left
        if (IsNullLiteral(candidateBinary.Right))
            return MatchNode(patternIsPattern.Expression, candidateBinary.Left, cursor);
        // candidate: null == expr → match pattern's expression against candidate's right
        if (IsNullLiteral(candidateBinary.Left))
            return MatchNode(patternIsPattern.Expression, candidateBinary.Right, cursor);

        return false;
    }

    private static bool IsNullLiteral(J node) =>
        node is Literal { Type: JavaType.Primitive { Kind: JavaType.PrimitiveKind.Null } };

    private static bool HasLiteral(Binary binary) =>
        binary.Left is Literal || binary.Right is Literal;

    /// <summary>
    /// Compare two property values, dispatching based on their type.
    /// Handles: null, J nodes, padded wrappers, containers, lists, and primitives.
    /// </summary>
    private bool MatchValue(object? patternValue, object? candidateValue, Cursor cursor)
    {
        // Both null
        if (patternValue == null && candidateValue == null)
            return true;
        // One null — but a pattern container with only zero-min variadic captures
        // can match a null candidate (e.g., pattern `Fact({args})` vs `[Fact]` with no parens)
        if (patternValue == null || candidateValue == null)
        {
            if (patternValue != null && candidateValue == null && TreeHelper.IsContainer(patternValue))
                return MatchContainerAgainstNull(patternValue, cursor);
            return false;
        }

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
    /// Match a pattern container against a null candidate. Succeeds only when every
    /// element in the container is a variadic capture placeholder with min == 0,
    /// binding each to an empty list.
    /// </summary>
    private bool MatchContainerAgainstNull(object patternContainer, Cursor cursor)
    {
        var patternElements = TreeHelper.GetContainerElements(patternContainer);
        if (patternElements == null || patternElements.Count == 0)
            return true;

        foreach (var el in patternElements)
        {
            var inner = TreeHelper.UnwrapPadded(el) ?? el;
            if (inner is Identifier id)
            {
                var captureName = Placeholder.FromPlaceholder(id.SimpleName);
                if (captureName != null && _captures.TryGetValue(captureName, out var captureObj)
                    && IsVariadic(captureObj))
                {
                    var (min, _) = GetVariadicBounds(captureObj);
                    if (min == 0)
                    {
                        _bindings[captureName] = new List<object>().AsReadOnly();
                        continue;
                    }
                }
            }
            return false;
        }
        return true;
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

                    // Evaluate variadic constraint before binding
                    if (!EvaluateVariadicConstraint(captureObj, captured.AsReadOnly(), cursor))
                        continue;

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

    private static bool IsVariadic(object captureObj) =>
        captureObj is ICapture capture && capture.IsVariadic;

    private static (int min, int max) GetVariadicBounds(object captureObj)
    {
        if (captureObj is not ICapture capture)
            return (0, int.MaxValue);
        return (capture.MinCount ?? 0, capture.MaxCount ?? int.MaxValue);
    }

    /// <summary>
    /// Check if one side is null (no receiver / implicit this) and the other is
    /// an explicit <c>this</c> identifier. Handles both directions.
    /// </summary>
    private static bool IsImplicitThisPair(J? a, J? b)
    {
        return (a == null && IsThis(b)) || (b == null && IsThis(a));
    }

    private static bool IsThis(J? node) =>
        node is Identifier { SimpleName: "this" };

    /// <summary>
    /// Get the FQN of a method's declaring type, unwrapping Parameterized if needed.
    /// <c>List&lt;int&gt;.Contains</c> has declaring type <c>Parameterized(List)</c> —
    /// we need the underlying <c>Class.FullyQualifiedName</c>.
    /// </summary>
    private static string? GetDeclaringTypeFqn(JavaType.Method method) =>
        method.DeclaringType switch
        {
            JavaType.Class cls => cls.FullyQualifiedName,
            JavaType.Parameterized { Type: JavaType.Class cls } => cls.FullyQualifiedName,
            _ => null
        };

    /// <summary>Flag.Static bit value (from Java's Flag enum).</summary>
    private const long FlagStatic = 8;

    private static bool IsStatic(JavaType.Method method) =>
        (method.FlagsBitMap & FlagStatic) != 0;

    private static bool IsStatic(JavaType.Variable variable) =>
        (variable.FlagsBitMap & FlagStatic) != 0;

    private CaptureConstraintContext BuildConstraintContext(Cursor cursor, JavaType? patternType = null) =>
        new(cursor, new Dictionary<string, object>(_bindings), patternType);

    private bool EvaluateConstraint(object captureObj, object candidate, Cursor cursor, JavaType? patternType = null) =>
        captureObj is not ICapture capture || capture.EvaluateConstraint(candidate, BuildConstraintContext(cursor, patternType));

    private bool EvaluateVariadicConstraint(object captureObj, IReadOnlyList<object> captured, Cursor cursor) =>
        captureObj is not ICapture capture || capture.EvaluateVariadicConstraint(captured, BuildConstraintContext(cursor));

}
