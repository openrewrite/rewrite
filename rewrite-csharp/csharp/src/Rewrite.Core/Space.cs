namespace Rewrite.Core;

/// <summary>
/// Represents whitespace and comments in the LST.
/// Space is immutable and can be shared across multiple tree elements.
/// </summary>
public sealed record Space(string Whitespace, IList<Comment> Comments)
{
    public static readonly Space Empty = new("", []);
    public static readonly Space SingleSpace = new(" ", []);
    public static readonly Space Newline = new("\n", []);

    public static Space Format(string whitespace) =>
        string.IsNullOrEmpty(whitespace) ? Empty : new Space(whitespace, []);

    public bool IsEmpty => string.IsNullOrEmpty(Whitespace) && Comments.Count == 0;
}

/// <summary>
/// Represents a comment in source code.
/// </summary>
public abstract record Comment(string Text, string Suffix, bool Multiline);

/// <summary>
/// A single-line or multi-line text comment.
/// </summary>
public sealed record TextComment(string Text, string Suffix, bool Multiline)
    : Comment(Text, Suffix, Multiline);
