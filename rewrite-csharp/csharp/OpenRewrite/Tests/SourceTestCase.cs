namespace OpenRewrite.Tests;

public record SourceTestCase(string Name, string SourceText)
{
    public override string ToString() => Name;
}
