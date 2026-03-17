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

namespace OpenRewrite.CSharp;

/// <summary>
/// Matches C# attributes by name, analogous to Java's <c>AnnotationMatcher</c>.
/// Handles the C# convention where <c>[Obsolete]</c> and <c>[ObsoleteAttribute]</c> are equivalent.
/// <para>
/// When type attribution is available, matches against the fully qualified type name.
/// Otherwise, falls back to simple name matching from the syntax tree.
/// </para>
/// </summary>
public class AttributeMatcher
{
    private readonly string _simpleName;
    private readonly string? _fullyQualifiedName;

    /// <summary>
    /// Create an <see cref="AttributeMatcher"/> from an attribute name pattern.
    /// The name can be simple (e.g. <c>"Obsolete"</c>) or fully qualified
    /// (e.g. <c>"System.ObsoleteAttribute"</c>).
    /// </summary>
    public AttributeMatcher(string pattern)
    {
        // If the pattern contains a dot, treat it as fully qualified
        var lastDot = pattern.LastIndexOf('.');
        if (lastDot >= 0)
        {
            _fullyQualifiedName = NormalizeAttributeName(pattern);
            _simpleName = NormalizeAttributeName(pattern[(lastDot + 1)..]);
        }
        else
        {
            _fullyQualifiedName = null;
            _simpleName = NormalizeAttributeName(pattern);
        }
    }

    /// <summary>
    /// Check if an <see cref="Annotation"/> matches this pattern.
    /// Prefers type attribution when available, falls back to syntactic name matching.
    /// </summary>
    public bool Matches(Annotation annotation)
    {
        // Try type-attributed matching first
        var type = TypeUtils.AsClass(annotation.Type);
        if (type != null)
        {
            var fqn = NormalizeAttributeName(type.FullyQualifiedName);
            if (_fullyQualifiedName != null)
                return string.Equals(fqn, _fullyQualifiedName, StringComparison.Ordinal);

            // Pattern is simple name only — extract simple name from FQN
            var lastDot = fqn.LastIndexOf('.');
            var typeSimpleName = lastDot >= 0 ? fqn[(lastDot + 1)..] : fqn;
            return string.Equals(typeSimpleName, _simpleName, StringComparison.Ordinal);
        }

        // Fall back to syntactic matching
        var name = Cs.GetSimpleName(annotation.AnnotationType);
        if (name == null)
            return false;

        // When matching by simple name only, ignore any FQN constraint since
        // we don't have type attribution to verify it
        return string.Equals(NormalizeAttributeName(name), _simpleName, StringComparison.Ordinal);
    }

    /// <summary>
    /// Check if an <see cref="AnnotatedStatement"/> has any attribute matching this pattern.
    /// </summary>
    public bool Matches(AnnotatedStatement annotated)
    {
        foreach (var attrList in annotated.AttributeLists)
        {
            foreach (var attr in attrList.Attributes)
            {
                if (Matches(attr.Element))
                    return true;
            }
        }

        return false;
    }

    /// <summary>
    /// Check if the nearest enclosing <see cref="AnnotatedStatement"/> has a matching attribute.
    /// </summary>
    public bool Matches(Cursor cursor)
    {
        var annotated = cursor.FirstEnclosing<AnnotatedStatement>();
        return annotated != null && Matches(annotated);
    }

    /// <summary>
    /// Strip the conventional <c>Attribute</c> suffix from a C# attribute name.
    /// </summary>
    internal static string NormalizeAttributeName(string name)
    {
        const string suffix = "Attribute";
        if (name.Length > suffix.Length && name.EndsWith(suffix, StringComparison.Ordinal))
        {
            // Handle FQN: strip suffix from last segment only
            var lastDot = name.LastIndexOf('.');
            if (lastDot >= 0)
            {
                var segment = name[(lastDot + 1)..];
                if (segment.Length > suffix.Length && segment.EndsWith(suffix, StringComparison.Ordinal))
                    return name[..(name.Length - suffix.Length)];
                return name;
            }

            return name[..^suffix.Length];
        }

        return name;
    }
}
