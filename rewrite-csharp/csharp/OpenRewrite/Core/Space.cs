namespace Rewrite.Core;

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

    public static Space Format(string whitespace) =>
        string.IsNullOrEmpty(whitespace) ? Empty : new Space(whitespace, []);

    public bool IsEmpty => string.IsNullOrEmpty(Whitespace) && Comments.Count == 0;

    public Space WithWhitespace(string whitespace) =>
        string.Equals(whitespace, Whitespace, StringComparison.Ordinal) ? this : new(whitespace, Comments);

    public Space WithComments(IList<Comment> comments) =>
        ReferenceEquals(comments, Comments) ? this : new(Whitespace, comments);
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
