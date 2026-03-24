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

namespace OpenRewrite.CSharp.Template;

/// <summary>
/// Describes what syntactic position a capture occupies.
/// Used internally by the template engine to generate appropriate scaffold code.
/// </summary>
internal enum CaptureKind
{
    /// <summary>An expression-position capture (default). Scaffold emits a typed field declaration.</summary>
    Expression,
    /// <summary>A type-position capture (e.g., base type, generic argument). Scaffold strategy TBD.</summary>
    Type,
    /// <summary>A name/identifier-position capture. No preamble needed; pure identifier substitution.</summary>
    Name,
}

/// <summary>
/// Non-generic interface for captures, providing access to capture metadata
/// and constraint evaluation without requiring knowledge of the captured type.
/// </summary>
public interface ICapture
{
    string Name { get; }
    bool IsVariadic { get; }
    int? MinCount { get; }
    int? MaxCount { get; }

    /// <summary>
    /// Evaluate the single-node constraint (if any) against a candidate.
    /// Returns true when there is no constraint or when the constraint passes.
    /// </summary>
    bool EvaluateConstraint(object candidate, CaptureConstraintContext context);

    /// <summary>
    /// Evaluate the variadic constraint (if any) against a list of captured elements.
    /// Returns true when there is no constraint or when the constraint passes.
    /// </summary>
    bool EvaluateVariadicConstraint(IReadOnlyList<object> captured, CaptureConstraintContext context);
}

/// <summary>
/// A named placeholder for pattern matching and template substitution.
/// When used in an interpolated string passed to <see cref="CSharpTemplate.Expression"/>
/// or <see cref="CSharpPattern.Expression"/>, the <see cref="TemplateStringHandler"/>
/// intercepts it and registers the capture automatically.
/// </summary>
/// <typeparam name="T">The type of AST node this capture matches.</typeparam>
public sealed class Capture<T> : ICapture where T : J
{
    public string Name { get; }
    public bool IsVariadic { get; }
    public int? MinCount { get; }
    public int? MaxCount { get; }
    public string? Type { get; }
    internal CaptureKind Kind { get; }
    public Func<T, CaptureConstraintContext, bool>? Constraint { get; }
    public Func<IReadOnlyList<T>, CaptureConstraintContext, bool>? VariadicConstraint { get; }

    internal Capture(string name, bool variadic = false,
        int? minCount = null, int? maxCount = null,
        string? type = null,
        CaptureKind kind = CaptureKind.Expression,
        Func<T, CaptureConstraintContext, bool>? constraint = null,
        Func<IReadOnlyList<T>, CaptureConstraintContext, bool>? variadicConstraint = null)
    {
        Name = name;
        IsVariadic = variadic;
        MinCount = minCount;
        MaxCount = maxCount;
        Type = type;
        Kind = kind;
        Constraint = constraint;
        VariadicConstraint = variadicConstraint;
    }

    /// <summary>
    /// Returns the placeholder identifier for this capture.
    /// Used by <see cref="TemplateStringHandler"/> and as a defense-in-depth
    /// fallback when used in a plain string interpolation.
    /// </summary>
    public override string ToString() => Placeholder.ToPlaceholder(Name);

    /// <inheritdoc />
    public bool EvaluateConstraint(object candidate, CaptureConstraintContext context)
    {
        if (Constraint == null) return true;
        return candidate is T typed && Constraint(typed, context);
    }

    /// <inheritdoc />
    public bool EvaluateVariadicConstraint(IReadOnlyList<object> captured, CaptureConstraintContext context)
    {
        if (VariadicConstraint == null) return true;
        // IReadOnlyList<T> is covariant and T is a reference type (constrained to J),
        // so we can cast IReadOnlyList<object> → IReadOnlyList<T> via an adapter.
        var typed = captured.Select(x => (T)x).ToList().AsReadOnly();
        return VariadicConstraint(typed, context);
    }
}

/// <summary>
/// Factory methods for creating captures.
/// </summary>
public static class Capture
{
    private static int _counter;

    /// <summary>
    /// Create a capture that matches a single AST node of type <typeparamref name="T"/>.
    /// When <paramref name="type"/> is specified, the template engine generates a typed
    /// variable declaration in the scaffold preamble, giving the placeholder proper type
    /// attribution from the parser.
    /// <para>
    /// Prefer the position-specific factories (<see cref="Expression"/>, <see cref="Type"/>,
    /// <see cref="Name"/>) when the capture position is known. Use <c>Of</c> as a generic
    /// fallback for AST node types that don't have a dedicated factory.
    /// </para>
    /// </summary>
    public static Capture<T> Of<T>(string? name = null, string? type = null,
        Func<T, CaptureConstraintContext, bool>? constraint = null) where T : J
        => new(name ?? $"_capture_{Interlocked.Increment(ref _counter)}",
            type: type, constraint: constraint);

    /// <summary>
    /// Create a capture for an expression-position node.
    /// When <paramref name="type"/> is specified, the template engine generates a typed
    /// field declaration in the scaffold preamble for type attribution.
    /// </summary>
    public static Capture<Expression> Expression(string? name = null, string? type = null,
        Func<Expression, CaptureConstraintContext, bool>? constraint = null)
        => new(name ?? $"_capture_{Interlocked.Increment(ref _counter)}",
            type: type, kind: CaptureKind.Expression, constraint: constraint);

    /// <summary>
    /// Create a variadic capture that matches zero or more elements.
    /// Useful for matching argument lists, statement sequences, etc.
    /// </summary>
    public static Capture<T> Variadic<T>(string? name = null,
        int? min = null, int? max = null,
        Func<IReadOnlyList<T>, CaptureConstraintContext, bool>? constraint = null) where T : J
        => new(name ?? $"_capture_{Interlocked.Increment(ref _counter)}",
            variadic: true, minCount: min, maxCount: max,
            variadicConstraint: constraint);

    /// <summary>
    /// Create a capture for a type-position node (e.g., base type, generic argument, variable type).
    /// The template engine will use an appropriate scaffold strategy so Roslyn parses
    /// the placeholder in a type context.
    /// </summary>
    public static Capture<NameTree> Type(string? name = null,
        Func<NameTree, CaptureConstraintContext, bool>? constraint = null)
        => new(name ?? $"_capture_{Interlocked.Increment(ref _counter)}",
            kind: CaptureKind.Type, constraint: constraint);

    /// <summary>
    /// Create a capture for a name/identifier-position node.
    /// No preamble declaration is needed; the placeholder is substituted directly.
    /// </summary>
    public static Capture<Identifier> Name(string? name = null,
        Func<Identifier, CaptureConstraintContext, bool>? constraint = null)
        => new(name ?? $"_capture_{Interlocked.Increment(ref _counter)}",
            kind: CaptureKind.Name, constraint: constraint);
}
