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

namespace OpenRewrite.CSharp.Format;

/// <summary>
/// Extension methods for auto-formatting C# LST nodes.
/// </summary>
public static class AutoFormatExtensions
{
    /// <summary>
    /// Auto-formats this node using Roslyn within its enclosing compilation unit.
    /// </summary>
    public static T AutoFormat<T>(this T tree, Cursor cursor) where T : class, J
    {
        var visitor = new AutoFormatVisitor<int>();
        var result = visitor.Format(tree, cursor);
        return result as T ?? tree;
    }

    /// <summary>
    /// Auto-formats this node using Roslyn, stopping after the specified node.
    /// </summary>
    public static T AutoFormat<T>(this T tree, Cursor cursor, J? stopAfter) where T : class, J
    {
        var visitor = new AutoFormatVisitor<int>(stopAfter);
        var result = visitor.Format(tree, cursor);
        return result as T ?? tree;
    }
}
