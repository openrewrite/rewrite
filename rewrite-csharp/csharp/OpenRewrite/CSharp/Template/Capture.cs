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
/// A named placeholder for pattern matching and template substitution.
/// When used in an interpolated string passed to <see cref="CSharpTemplate.Create"/>
/// or <see cref="CSharpPattern.Create"/>, the <see cref="TemplateStringHandler"/>
/// intercepts it and registers the capture automatically.
/// </summary>
/// <typeparam name="T">The type of AST node this capture matches.</typeparam>
/// <summary>
/// Non-generic interface for accessing capture metadata without reflection.
/// </summary>
internal interface ICaptureMetadata
{
    string Name { get; }
    bool IsVariadic { get; }

    /// <summary>
    /// Evaluate the constraint against a candidate node.
    /// Returns true if no constraint is set or if the candidate satisfies the constraint.
    /// Returns false if the candidate's type is incompatible or the constraint rejects it.
    /// </summary>
    bool EvaluateConstraint(J candidate, Cursor cursor);
}

public sealed class Capture<T> : ICaptureMetadata where T : J
{
    public string Name { get; }
    public bool IsVariadic { get; }
    public int? MinCount { get; }
    public int? MaxCount { get; }
    public Func<T, Cursor, bool>? Constraint { get; }

    internal Capture(string name, bool variadic = false,
        int? minCount = null, int? maxCount = null,
        Func<T, Cursor, bool>? constraint = null)
    {
        Name = name;
        IsVariadic = variadic;
        MinCount = minCount;
        MaxCount = maxCount;
        Constraint = constraint;
    }

    bool ICaptureMetadata.EvaluateConstraint(J candidate, Cursor cursor)
    {
        if (Constraint == null)
            return true;
        if (candidate is not T typed)
            return false;
        return Constraint(typed, cursor);
    }

    /// <summary>
    /// Returns the placeholder identifier for this capture.
    /// Used by <see cref="TemplateStringHandler"/> and as a defense-in-depth
    /// fallback when used in a plain string interpolation.
    /// </summary>
    public override string ToString() => Placeholder.ToPlaceholder(Name);
}

/// <summary>
/// Factory methods for creating captures.
/// </summary>
public static class Capture
{
    private static int _counter;

    /// <summary>
    /// Create a capture that matches a single AST node of type <typeparamref name="T"/>.
    /// </summary>
    public static Capture<T> Of<T>(string? name = null) where T : J
        => new(name ?? $"_capture_{Interlocked.Increment(ref _counter)}");

    /// <summary>
    /// Create a variadic capture that matches zero or more elements.
    /// Useful for matching argument lists, statement sequences, etc.
    /// </summary>
    public static Capture<T> Variadic<T>(string? name = null,
        int? min = null, int? max = null) where T : J
        => new(name ?? $"_capture_{Interlocked.Increment(ref _counter)}",
            variadic: true, minCount: min, maxCount: max);

    /// <summary>
    /// Create a capture with a constraint predicate that must be satisfied for matching.
    /// </summary>
    public static Capture<T> WithConstraint<T>(string name,
        Func<T, Cursor, bool> constraint) where T : J
        => new(name, constraint: constraint);
}
