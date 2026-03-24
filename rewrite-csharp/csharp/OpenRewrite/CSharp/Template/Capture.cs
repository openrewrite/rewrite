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
/// Options for variadic captures that match zero or more elements.
/// Bundles min/max bounds and a list-level constraint.
/// </summary>
/// <typeparam name="T">The element type this variadic capture matches.</typeparam>
public sealed record VariadicOptions<T>(
    int? Min = null,
    int? Max = null,
    Func<IReadOnlyList<T>, CaptureConstraintContext, bool>? Constraint = null
) where T : J;

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
    public bool IsVariadic => Variadic != null;
    public int? MinCount => Variadic?.Min;
    public int? MaxCount => Variadic?.Max;
    public string? Type { get; }
    internal CaptureKind Kind { get; }
    public Func<T, CaptureConstraintContext, bool>? Constraint { get; }
    public VariadicOptions<T>? Variadic { get; }

    internal Capture(string name,
        string? type = null,
        CaptureKind kind = CaptureKind.Expression,
        Func<T, CaptureConstraintContext, bool>? constraint = null,
        VariadicOptions<T>? variadic = null)
    {
        if (constraint != null && variadic != null)
            throw new ArgumentException(
                "A capture cannot have both a single-node constraint and variadic options. " +
                "Use VariadicOptions.Constraint for list-level constraints.");

        Name = name;
        Type = type;
        Kind = kind;
        Constraint = constraint;
        Variadic = variadic;
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
        // When the capture declares a type constraint and the candidate has type
        // attribution, verify the candidate's semantic type is assignable to it.
        // If the candidate has no type info (expr.Type == null), skip the check —
        // type attribution may be unavailable when reference assemblies aren't provided.
        if (Type != null && candidate is Expression { Type: not null } expr)
        {
            // Prefer the Roslyn-resolved type from the pattern scaffold when available.
            // This handles generics (IDictionary<object, object> resolves to its FQN)
            // and primitives (int resolves to System.Int32) correctly without string parsing.
            var matched = context.PatternType != null
                ? TypeUtils.IsAssignableTo(expr.Type, context.PatternType)
                : TypeUtils.IsAssignableTo(expr.Type, ResolveCSharpAlias(Type));
            if (!matched) return false;
        }

        if (Constraint == null) return true;
        // The `is T` check acts as an implicit type guard: if the candidate is not
        // assignable to T, the constraint fails without invoking the delegate.
        return candidate is T typed && Constraint(typed, context);
    }

    /// <summary>
    /// Resolve C# keyword aliases (e.g. <c>string</c>, <c>int</c>) to their
    /// fully-qualified .NET type names for use with <see cref="TypeUtils.IsAssignableTo"/>.
    /// Returns the input unchanged if it is not a keyword alias.
    /// </summary>
    private static string ResolveCSharpAlias(string type) => type switch
    {
        "bool" => "System.Boolean",
        "byte" => "System.Byte",
        "sbyte" => "System.SByte",
        "char" => "System.Char",
        "decimal" => "System.Decimal",
        "double" => "System.Double",
        "float" => "System.Single",
        "int" => "System.Int32",
        "uint" => "System.UInt32",
        "long" => "System.Int64",
        "ulong" => "System.UInt64",
        "short" => "System.Int16",
        "ushort" => "System.UInt16",
        "string" => "System.String",
        "object" => "System.Object",
        _ => type
    };

    /// <inheritdoc />
    public bool EvaluateVariadicConstraint(IReadOnlyList<object> captured, CaptureConstraintContext context)
    {
        if (Variadic?.Constraint == null) return true;
        var typed = captured.OfType<T>().ToList();
        if (typed.Count != captured.Count) return false;
        return Variadic.Constraint(typed.AsReadOnly(), context);
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
        Func<T, CaptureConstraintContext, bool>? constraint = null,
        VariadicOptions<T>? variadic = null) where T : J
        => new(name ?? $"_capture_{Interlocked.Increment(ref _counter)}",
            type: type, constraint: constraint, variadic: variadic);

    /// <summary>
    /// Create a capture for an expression-position node.
    /// When <paramref name="type"/> is specified, the template engine generates a typed
    /// field declaration in the scaffold preamble for type attribution.
    /// </summary>
    public static Capture<Expression> Expression(string? name = null, string? type = null,
        Func<Expression, CaptureConstraintContext, bool>? constraint = null,
        VariadicOptions<Expression>? variadic = null)
        => new(name ?? $"_capture_{Interlocked.Increment(ref _counter)}",
            type: type, kind: CaptureKind.Expression, constraint: constraint,
            variadic: variadic);

    /// <summary>
    /// Create a capture for a type-position node (e.g., base type, generic argument, variable type).
    /// The template engine will use an appropriate scaffold strategy so Roslyn parses
    /// the placeholder in a type context.
    /// </summary>
    public static Capture<NameTree> Type(string? name = null,
        Func<NameTree, CaptureConstraintContext, bool>? constraint = null,
        VariadicOptions<NameTree>? variadic = null)
        => new(name ?? $"_capture_{Interlocked.Increment(ref _counter)}",
            kind: CaptureKind.Type, constraint: constraint, variadic: variadic);

    /// <summary>
    /// Create a capture for a name/identifier-position node.
    /// No preamble declaration is needed; the placeholder is substituted directly.
    /// </summary>
    public static Capture<Identifier> Name(string? name = null,
        Func<Identifier, CaptureConstraintContext, bool>? constraint = null,
        VariadicOptions<Identifier>? variadic = null)
        => new(name ?? $"_capture_{Interlocked.Increment(ref _counter)}",
            kind: CaptureKind.Name, constraint: constraint, variadic: variadic);
}
