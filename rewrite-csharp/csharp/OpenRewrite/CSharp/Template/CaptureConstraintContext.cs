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
/// Context passed to capture constraint functions during pattern matching.
/// Provides the cursor positioned at the captured node and a read-only view
/// of all captures that have been bound so far, enabling dependent constraints.
/// </summary>
/// <param name="Cursor">The cursor from the visitor, positioned at the node being matched.</param>
/// <param name="Captures">Read-only snapshot of captures already bound at the point this
/// constraint is evaluated. Enables dependent constraints where one capture's validity
/// depends on another's value.</param>
/// <param name="PatternType">The Roslyn-resolved <see cref="JavaType"/> of the pattern
/// placeholder, when available. Used by typed captures to compare against the candidate's
/// type using the fully-resolved type from the scaffold rather than the raw type string.</param>
public sealed record CaptureConstraintContext(
    Cursor Cursor,
    IReadOnlyDictionary<string, object> Captures,
    JavaType? PatternType = null
);
