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
using System.Linq;
using OpenRewrite.Java;

namespace OpenRewrite.CSharp.Template;

/// <summary>
/// Result of a successful pattern match. Provides typed access to captured AST nodes.
/// </summary>
public sealed class MatchResult
{
    private readonly Dictionary<string, object> _captures;

    internal MatchResult(Dictionary<string, object> captures)
    {
        _captures = captures;
    }

    /// <summary>
    /// Create a <see cref="MatchResult"/> from manually-assembled capture values.
    /// This allows recipe authors to use <see cref="CSharpTemplate.Apply"/> with
    /// substitution values that come from imperative extraction rather than
    /// <see cref="CSharpPattern.Match"/>.
    /// </summary>
    /// <example>
    /// <code>
    /// var values = MatchResult.Of(("reason", skipLiteral));
    /// var result = template.Apply(cursor, values: values);
    /// </code>
    /// </example>
    public static MatchResult Of(params (string name, J value)[] captures)
    {
        var dict = new Dictionary<string, object>(captures.Length);
        foreach (var (name, value) in captures)
        {
            dict[name] = value;
        }
        return new MatchResult(dict);
    }

    /// <summary>
    /// Create a <see cref="MatchResult"/> from <see cref="ICapture"/> keys.
    /// This allows using unnamed captures (with auto-generated names) as keys,
    /// avoiding the need to manually synchronize string names between template
    /// creation and value binding.
    /// </summary>
    /// <example>
    /// <code>
    /// var left = Capture.Expression();
    /// var right = Capture.Expression();
    /// var template = CSharpTemplate.Expression($"{left} &amp;&amp; {right}");
    /// var values = MatchResult.Of((left, outerCond), (right, innerCond));
    /// var result = template.Apply(cursor, values: values);
    /// </code>
    /// </example>
    public static MatchResult Of(params (ICapture capture, J value)[] captures)
    {
        var dict = new Dictionary<string, object>(captures.Length);
        foreach (var (capture, value) in captures)
        {
            dict[capture.Name] = value;
        }
        return new MatchResult(dict);
    }

    /// <summary>
    /// Get a captured value by name.
    /// </summary>
    public T? Get<T>(string name) where T : class, J
        => _captures.TryGetValue(name, out var value) ? value as T : default;

    /// <summary>
    /// Get a captured value by its <see cref="Capture{T}"/> object.
    /// </summary>
    public T? Get<T>(Capture<T> capture) where T : class, J
        => Get<T>(capture.Name);

    /// <summary>
    /// Get a captured value by its <see cref="ICapture"/> object.
    /// Useful when the capture's type parameter doesn't match the desired return type.
    /// </summary>
    public T? Get<T>(ICapture capture) where T : class, J
        => Get<T>(capture.Name);

    /// <summary>
    /// Get a variadic capture result as a list.
    /// Handles both IReadOnlyList&lt;T&gt; and IReadOnlyList&lt;object&gt; (from pattern matcher).
    /// </summary>
    public IReadOnlyList<T> GetList<T>(string name) where T : class, J
    {
        if (!_captures.TryGetValue(name, out var value))
            return [];
        if (value is IReadOnlyList<T> typedList)
            return typedList;
        if (value is IReadOnlyList<object> objectList)
            return objectList.Cast<T>().ToList();
        return [];
    }

    /// <summary>
    /// Check if a capture with the given name was bound.
    /// </summary>
    public bool Has(string name) => _captures.ContainsKey(name);

    /// <summary>
    /// Check if a capture was bound.
    /// </summary>
    public bool Has(ICapture capture) => _captures.ContainsKey(capture.Name);

    internal Dictionary<string, object> AsDict() => _captures;
}
