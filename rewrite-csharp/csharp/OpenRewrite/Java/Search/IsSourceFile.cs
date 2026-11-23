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
using ExecutionContext = OpenRewrite.Core.ExecutionContext;

namespace OpenRewrite.Java.Search;

/// <summary>
/// Match <see cref="SourceFile"/> trees by path glob.
///
/// Mirrors <c>org.openrewrite.FindSourceFiles</c>. Used as the
/// LocalVisitor bundled with the <see cref="RecipeRef"/> returned by
/// <c>HasSourcePath</c> so unit tests without an active RPC connection
/// still see real filtering.
///
/// Glob semantics:
///   * <c>*</c> matches any sequence of characters except <c>/</c>
///   * <c>**</c> matches any sequence of characters including <c>/</c>
///   * <c>?</c> matches a single character except <c>/</c>
/// </summary>
public class IsSourceFile : TreeVisitor<Tree, ExecutionContext>
{
    private readonly string _filePattern;

    public IsSourceFile(string filePattern)
    {
        _filePattern = filePattern;
    }

    public override Tree? Visit(Tree? tree, ExecutionContext ctx)
    {
        if (tree is not SourceFile sf) return tree;
        var path = sf.SourcePath;
        if (string.IsNullOrEmpty(path)) return tree;
        if (MatchGlob(_filePattern, path))
        {
            return SearchResult.Found(tree);
        }
        return tree;
    }

    private static bool MatchGlob(string pattern, string path)
    {
        // Convert the glob to a regex. We interpret ** as ".*", * as
        // "[^/]*", ? as "[^/]". Other characters are literal.
        var regex = new System.Text.StringBuilder("^");
        for (var i = 0; i < pattern.Length; i++)
        {
            var c = pattern[i];
            if (c == '*' && i + 1 < pattern.Length && pattern[i + 1] == '*')
            {
                regex.Append(".*");
                i++;
            }
            else if (c == '*')
            {
                regex.Append("[^/]*");
            }
            else if (c == '?')
            {
                regex.Append("[^/]");
            }
            else if ("\\.+()[]{}^$|".IndexOf(c) >= 0)
            {
                regex.Append('\\').Append(c);
            }
            else
            {
                regex.Append(c);
            }
        }
        regex.Append('$');
        return System.Text.RegularExpressions.Regex.IsMatch(path, regex.ToString());
    }
}
