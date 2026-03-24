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

namespace OpenRewrite.CSharp.Template;

/// <summary>
/// Context passed to capture constraint functions during pattern matching.
/// Provides the cursor positioned at the captured node and a read-only view
/// of all captures that have been bound so far, enabling dependent constraints.
/// </summary>
public sealed class CaptureConstraintContext
{
    /// <summary>
    /// The cursor from the visitor, positioned at the node being matched.
    /// </summary>
    public Cursor Cursor { get; }

    /// <summary>
    /// Read-only snapshot of captures already bound at the point this constraint is evaluated.
    /// Enables dependent constraints where one capture's validity depends on another's value.
    /// </summary>
    public IReadOnlyDictionary<string, object> Captures { get; }

    public CaptureConstraintContext(Cursor cursor, IReadOnlyDictionary<string, object> captures)
    {
        Cursor = cursor;
        Captures = captures;
    }
}
