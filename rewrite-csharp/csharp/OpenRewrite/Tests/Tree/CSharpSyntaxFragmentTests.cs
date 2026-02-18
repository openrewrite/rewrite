using OpenRewrite.Test;

namespace OpenRewrite.Tests.Tree;


public class CSharpSyntaxFragmentTests : RewriteTest
{
    /// <summary>
    /// Verifies that all supported syntax fragments round-trip through the parser and printer.
    /// </summary>
    [Theory]
    [MemberData(nameof(PassingFragments))]
    public void RoundTrip(SourceTestCase testCase)
    {
        RewriteRun(
            CSharp(testCase.SourceText)
        );
    }

    public static IEnumerable<object[]> PassingFragments()
    {
        return CSharpSyntaxFragments.GetData()
            .Select(tc => new object[] { tc });
    }

}
