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
namespace OpenRewrite.Test;

/// <summary>
/// Controls which invariant checks are run during <see cref="RewriteTest.RewriteRun"/>.
/// All validations are enabled by default. Tests can disable specific validations
/// when a recipe is known to produce results that don't satisfy a particular invariant.
/// <para>
/// Analogous to Java's <c>TypeValidation</c> (which despite its name covers more than
/// type validation).
/// </para>
/// </summary>
public sealed class Validations
{
    /// <summary>
    /// Whether Space fields are validated to contain only whitespace and comments
    /// (no leaked source tokens). Default: enabled.
    /// </summary>
    public bool WhitespaceInSpaces { get; init; } = true;

    /// <summary>
    /// Whether the printed output must match the original input (round-trip fidelity).
    /// Default: enabled.
    /// </summary>
    public bool PrintEqualsInput { get; init; } = true;

    /// <summary>
    /// Whether re-parsing the printed output must produce identical output (idempotency).
    /// Default: enabled.
    /// </summary>
    public bool PrintIdempotence { get; init; } = true;

    /// <summary>
    /// Maximum number of structural mismatches allowed in the whitespace reconciler
    /// before it throws. Set to 0 to require perfect structural alignment between
    /// recipe output and Roslyn-formatted output. Set to <c>int.MaxValue</c> to
    /// allow unlimited mismatches (production behavior).
    /// Default: 5 — enough to catch systematic issues while tolerating minor
    /// type mismatches from recipe-constructed nodes.
    /// </summary>
    public int MaxWhitespaceMismatches { get; init; } = 5;

    /// <summary>All validations enabled with default settings.</summary>
    public static Validations All { get; } = new();

    /// <summary>All validations disabled.</summary>
    public static Validations None { get; } = new()
    {
        WhitespaceInSpaces = false,
        PrintEqualsInput = false,
        PrintIdempotence = false,
        MaxWhitespaceMismatches = int.MaxValue
    };
}
