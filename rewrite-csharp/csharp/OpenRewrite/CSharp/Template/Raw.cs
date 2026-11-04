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
namespace OpenRewrite.CSharp.Template;

/// <summary>
/// Raw code to be spliced into a template at construction time.
/// Unlike captures (resolved at apply time), Raw splices its content
/// directly into the template string before parsing.
/// </summary>
/// <example>
/// <code>
/// // Splice a method name from a recipe option
/// var tmpl = CSharpTemplate.Expression($"logger.{Raw.Code(Level)}({msg})");
/// </code>
/// </example>
public sealed class Raw
{
    public string Value { get; }

    private Raw(string value) => Value = value;

    /// <summary>
    /// Create a raw code splice from a string value.
    /// The value is inserted directly into the template code before parsing.
    /// </summary>
    public static Raw Code(string code) => new(code);

    public override string ToString() => Value;
}
