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
using System.Runtime.CompilerServices;
using System.Text;
using OpenRewrite.Java;

namespace OpenRewrite.CSharp.Template;

/// <summary>
/// Custom interpolated string handler that intercepts <see cref="Capture{T}"/> and
/// <see cref="Raw"/> interpolation holes, registering captures and building the template string.
/// <para>
/// This is the C# equivalent of JavaScript's tagged template literals
/// and Python's f-string <c>__format__</c> mechanism. Each <c>{hole}</c> in the
/// interpolated string is intercepted at the call site with its actual type,
/// providing compile-time type safety with no global state.
/// </para>
/// </summary>
[InterpolatedStringHandler]
public ref struct TemplateStringHandler
{
    private readonly StringBuilder _builder;
    private readonly Dictionary<string, object> _captures;

    public TemplateStringHandler(int literalLength, int formattedCount)
    {
        _builder = new StringBuilder(literalLength);
        _captures = new Dictionary<string, object>(formattedCount);
    }

    public void AppendLiteral(string s) => _builder.Append(s);

    /// <summary>
    /// Intercepts <see cref="Capture{T}"/> holes: registers the capture and appends its placeholder.
    /// </summary>
    public void AppendFormatted<T>(Capture<T> capture) where T : J
    {
        _captures[capture.Name] = capture;
        _builder.Append(Placeholder.ToPlaceholder(capture.Name));
    }

    /// <summary>
    /// Intercepts <see cref="Raw"/> holes: splices the code directly into the template string.
    /// </summary>
    public void AppendFormatted(Raw raw) => _builder.Append(raw.Value);

    /// <summary>
    /// Fallback for any other interpolated value (string, int, etc.).
    /// Spliced directly like <see cref="Raw"/>.
    /// </summary>
    public void AppendFormatted<T>(T value) => _builder.Append(value);

    internal string GetCode() => _builder.ToString();

    internal Dictionary<string, object> GetCaptures() => new(_captures);
}
