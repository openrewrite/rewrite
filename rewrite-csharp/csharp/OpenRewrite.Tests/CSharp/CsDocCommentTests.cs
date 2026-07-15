using OpenRewrite.Core;
using OpenRewrite.CSharp;
using Xunit;

namespace OpenRewrite.Tests.CSharp;

/// <summary>
/// The structured <see cref="CsDocComment"/> tree built from Roslyn's XML doc-comment parse must
/// print back to the exact original source (lossless round trip) and expose real structure for
/// traversal.
/// </summary>
public class CsDocCommentTests
{
    private static void AssertRoundTrip(string code)
    {
        var cu = new CSharpParser().Parse(code);
        var printed = new CSharpPrinter<int>().Print(cu);
        Assert.Equal(code, printed);
    }

    [Fact]
    public void SingleLineSummary() => AssertRoundTrip(
        """
        /// <summary>doc</summary>
        class Foo { }
        """);

    [Fact]
    public void MultilineSummary() => AssertRoundTrip(
        """
        /// <summary>
        /// A summary.
        /// </summary>
        class Foo { }
        """);

    [Fact]
    public void NestedElementsAndAttributes() => AssertRoundTrip(
        """
        /// <summary>
        /// Adds <paramref name="a"/> to <c>b</c>.
        /// </summary>
        /// <param name="a">first</param>
        /// <returns>the <see cref="int"/> sum</returns>
        int Add(int a, int b) => a + b;
        """);

    [Fact]
    public void IndentedDocComment() => AssertRoundTrip(
        """
        class Foo
        {
            /// <summary>
            /// Indented doc comment.
            /// </summary>
            void Bar() { }
        }
        """);

    [Fact]
    public void UnicodeContent() => AssertRoundTrip(
        """
        /// <summary>
        /// 传统方式
        /// </summary>
        class Foo { }
        """);

    [Fact]
    public void TwoDocBlocksSeparatedByBlankLine() => AssertRoundTrip(
        """
        /// <summary>first</summary>

        /// <summary>second</summary>
        class Foo { }
        """);

    [Fact]
    public void DocCommentAlongsideRegularComment() => AssertRoundTrip(
        """
        // regular
        /// <summary>doc</summary>
        class Foo { }
        """);

    [Fact]
    public void ProducesStructuredTree()
    {
        var doc = CsDocCommentParser.ParseDocComment("/// <summary>doc</summary>", "\n");
        Assert.NotEmpty(doc.Body);
        // The body must contain a real XmlElement named "summary", not flat text.
        Assert.Contains(doc.Body, n => n is CsDocComment.XmlElement { Name: "summary" });
    }

    [Fact]
    public void ParamNameAttributeIsStructured()
    {
        var doc = CsDocCommentParser.ParseDocComment("/// <param name=\"a\">x</param>", "\n");
        var element = Assert.IsType<CsDocComment.XmlElement>(
            Assert.Single(doc.Body, n => n is CsDocComment.XmlElement));
        Assert.Equal("param", element.Name);
        Assert.Contains(element.Attributes, a => a is CsDocComment.XmlNameAttribute);
    }
}
