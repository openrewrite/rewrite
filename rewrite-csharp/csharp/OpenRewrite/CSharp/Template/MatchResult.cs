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
    /// Get a variadic capture result as a list.
    /// </summary>
    public IReadOnlyList<T> GetList<T>(string name) where T : class, J
        => _captures.TryGetValue(name, out var value) && value is IReadOnlyList<T> list
            ? list : [];

    /// <summary>
    /// Check if a capture with the given name was bound.
    /// </summary>
    public bool Has(string name) => _captures.ContainsKey(name);

    internal Dictionary<string, object> AsDict() => _captures;
}
