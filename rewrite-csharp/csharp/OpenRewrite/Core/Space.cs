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
using System.Collections.Concurrent;

namespace OpenRewrite.Core;

/// <summary>
/// Represents whitespace and comments in the LST.
/// Space is immutable and can be shared across multiple tree elements.
/// </summary>
public sealed class Space(string whitespace, IList<Comment> comments)
{
    public string Whitespace { get; } = whitespace;
    public IList<Comment> Comments { get; } = comments;

    public static readonly Space Empty = new("", []);
    public static readonly Space SingleSpace = new(" ", []);
    public static readonly Space Newline = new("\n", []);

    private const int MaxFlyweightLength = 50;
    private static readonly ConcurrentDictionary<string, Space> Flyweights = new();

    static Space()
    {
        Flyweights[" "] = SingleSpace;
        Flyweights["\n"] = Newline;
    }

    private static Space Build(string whitespace, IList<Comment> comments)
    {
        if (comments.Count == 0)
        {
            if (string.IsNullOrEmpty(whitespace))
                return Empty;
            if (whitespace.Length <= MaxFlyweightLength)
                return Flyweights.GetOrAdd(whitespace, static ws => new Space(ws, []));
        }
        return new Space(whitespace, comments);
    }

    public static Space Format(string whitespace) =>
        string.IsNullOrEmpty(whitespace) ? Empty : Build(whitespace, []);

    /// <summary>
    /// Parses a formatting string that may contain C-style comments (/* ... */ and // ...)
    /// into structured whitespace and Comment entries.
    /// Use this instead of Format() when the text is known to come from real source code
    /// (not ghost/directive markers).
    /// </summary>
    public static Space FormatWithComments(string formatting)
    {
        if (string.IsNullOrEmpty(formatting))
            return Empty;

        var prefix = new System.Text.StringBuilder();
        var comment = new System.Text.StringBuilder();
        var comments = new List<Comment>();

        bool inSingleLineComment = false;
        bool inMultiLineComment = false;
        char last = '\0';

        for (int i = 0; i < formatting.Length; i++)
        {
            char c = formatting[i];
            switch (c)
            {
                case '/':
                    if (inSingleLineComment)
                    {
                        comment.Append(c);
                    }
                    else if (last == '/' && !inMultiLineComment)
                    {
                        inSingleLineComment = true;
                        comment.Clear();
                        prefix.Length -= 1;
                    }
                    else if (last == '*' && inMultiLineComment && comment.Length > 0)
                    {
                        inMultiLineComment = false;
                        comment.Length -= 1; // trim the last '*'
                        comments.Add(new TextComment(comment.ToString(),
                            prefix.Length > 0 ? prefix.ToString(0, prefix.Length - 1) : "", true));
                        prefix.Clear();
                        comment.Clear();
                        last = '\0'; // reset to prevent next '/' from being misinterpreted
                        continue;
                    }
                    else if (inMultiLineComment)
                    {
                        comment.Append(c);
                    }
                    else
                    {
                        prefix.Append(c);
                    }
                    break;
                case '\r':
                case '\n':
                    if (inSingleLineComment)
                    {
                        inSingleLineComment = false;
                        comments.Add(new TextComment(comment.ToString(), prefix.ToString(), false));
                        prefix.Clear();
                        comment.Clear();
                        prefix.Append(c);
                    }
                    else if (!inMultiLineComment)
                    {
                        prefix.Append(c);
                    }
                    else
                    {
                        comment.Append(c);
                    }
                    break;
                case '*':
                    if (inSingleLineComment)
                    {
                        comment.Append(c);
                    }
                    else if (last == '/' && !inMultiLineComment)
                    {
                        inMultiLineComment = true;
                        comment.Clear();
                    }
                    else if (inMultiLineComment)
                    {
                        comment.Append(c);
                    }
                    else
                    {
                        prefix.Append(c);
                    }
                    break;
                default:
                    if (inSingleLineComment || inMultiLineComment)
                    {
                        comment.Append(c);
                    }
                    else
                    {
                        prefix.Append(c);
                    }
                    break;
            }
            last = c;
        }

        // Unterminated comment at end of input
        if (comment.Length > 0 || inSingleLineComment)
        {
            comments.Add(new TextComment(comment.ToString(), prefix.ToString(), inMultiLineComment));
            prefix.Clear();
        }

        // During parsing above, whitespace before each comment is stored in its Suffix field.
        // OpenRewrite's Space model stores whitespace *after* each comment as Comment.Suffix,
        // and the Space.Whitespace is the whitespace *before* the first comment.
        // Rotate: move each comment's collected "prefix" to be the preceding comment's suffix,
        // and the first comment's prefix becomes the Space's leading whitespace.
        string ws = prefix.ToString();
        if (comments.Count > 0)
        {
            for (int i = comments.Count - 1; i >= 0; i--)
            {
                var cmt = comments[i];
                string next = cmt.Suffix;
                comments[i] = new TextComment(cmt.Text, ws, cmt.Multiline);
                ws = next;
            }
        }

        // Group consecutive /// doc comments into a single XmlDocComment.
        // After rotation, doc comment lines are TextComment with text starting with "/".
        if (comments.Count > 0)
        {
            var grouped = new List<Comment>();
            int j = 0;
            while (j < comments.Count)
            {
                var cmt = comments[j];
                if (cmt is TextComment tc && !tc.Multiline && tc.Text.StartsWith("/"))
                {
                    // Start of a doc comment block — group consecutive /// lines.
                    var sb = new System.Text.StringBuilder();
                    sb.Append(tc.Text);
                    string lastSuffix = tc.Suffix;
                    j++;
                    while (j < comments.Count &&
                           comments[j] is TextComment next && !next.Multiline &&
                           next.Text.StartsWith("/"))
                    {
                        sb.Append(lastSuffix);
                        sb.Append("//");
                        sb.Append(next.Text);
                        lastSuffix = next.Suffix;
                        j++;
                    }
                    grouped.Add(new XmlDocComment(sb.ToString(), lastSuffix, true));
                }
                else
                {
                    grouped.Add(cmt);
                    j++;
                }
            }
            comments = grouped;
        }

        return comments.Count > 0 ? new Space(ws, comments) : Build(formatting, []);
    }

    public bool IsEmpty => string.IsNullOrEmpty(Whitespace) && Comments.Count == 0;

    public Space WithWhitespace(string whitespace) =>
        string.Equals(whitespace, Whitespace, StringComparison.Ordinal) ? this : Build(whitespace, Comments);

    public Space WithComments(IList<Comment> comments) =>
        ReferenceEquals(comments, Comments) ? this : Build(Whitespace, comments);
}

/// <summary>
/// Represents a comment in source code.
/// </summary>
public abstract class Comment(string text, string suffix, bool multiline)
{
    public string Text { get; } = text;
    public string Suffix { get; } = suffix;
    public bool Multiline { get; } = multiline;
}

/// <summary>
/// A single-line or multi-line text comment.
/// </summary>
public sealed class TextComment(string text, string suffix, bool multiline)
    : Comment(text, suffix, multiline);

/// <summary>
/// An XML documentation comment (///) block.
/// The Text property contains the raw content after the initial "//",
/// including continuation "///" prefixes on subsequent lines.
/// </summary>
public sealed class XmlDocComment(string text, string suffix, bool multiline)
    : Comment(text, suffix, multiline);
