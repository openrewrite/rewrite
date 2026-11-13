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
using OpenRewrite.CSharp;
using OpenRewrite.Java;

namespace OpenRewrite.Tests.CSharp;

public class CSharpPrinterTests
{
    private const string Conditional =
        """
        class C
        {
        #if DEBUG
            void Debug() { }
        #else
            void Release() { }
        #endif
        }
        """;

    /// <summary>
    /// The branches of a conditional directive are printed apart from the rest of the file, and
    /// used to be printed with the default marker printer whichever one the caller had asked for.
    /// </summary>
    [Fact]
    public void ConditionalDirectiveBranchesUseTheCallersMarkerPrinter()
    {
        var source = new CSharpParser().Parse(Conditional, sourcePath: "c.cs");
        var marked = new MarkMethods().Visit(source, 0)!;

        Assert.Equal(Conditional.Replace("void", "/*~~>*/void"), Print(marked, MarkerPrinter.Default));
        Assert.Equal(Conditional, Print(marked, MarkerPrinter.Sanitized));
    }

    /// <summary>
    /// A markup is printed as Java prints it: its message, and its detail only when verbose.
    /// </summary>
    [Fact]
    public void MarkupPrintsItsDetailOnlyWhenVerbose()
    {
        var marked = new Empty(Guid.NewGuid(), Space.Empty,
            new Markers(Guid.NewGuid(), [new Markup.Info(Guid.NewGuid(), "message", "detail")]));

        Assert.Equal("/*~~(message)~~>*/", Print(marked, MarkerPrinter.Default));
        Assert.Equal("/*~~(detail)~~>*/", Print(marked, MarkerPrinter.Verbose));
        Assert.Equal("", Print(marked, MarkerPrinter.SearchMarkersOnly));

        var withoutDetail = new Empty(Guid.NewGuid(), Space.Empty,
            new Markers(Guid.NewGuid(), [new Markup.Info(Guid.NewGuid(), "message", null)]));
        Assert.Equal("/*~~(message)~~>*/", Print(withoutDetail, MarkerPrinter.Verbose));
    }

    [Fact]
    public void MarkersOnCommentsArePrinted()
    {
        var found = new Markers(Guid.NewGuid(), [new SearchResult(Guid.NewGuid(), "here")]);
        var commented = new Empty(Guid.NewGuid(),
            new Space(" ", [new TextComment(" line", "\n", false, found), new TextComment(" block ", "", true, found)]),
            Markers.Empty);

        Assert.Equal(" /*~~(here)~~>*/// line\n/*~~(here)~~>*//* block */", Print(commented, MarkerPrinter.Default));
        Assert.Equal(" // line\n/* block */", Print(commented, MarkerPrinter.Sanitized));
    }

    /// <summary>
    /// A literal built from a value alone is written as the C# for that value, and a number
    /// by the type of the literal, since a whole real arrives over RPC as any other number.
    /// </summary>
    [Theory]
    [InlineData(1.0, JavaType.PrimitiveKind.Double, "1.0")]
    [InlineData(1L, JavaType.PrimitiveKind.Double, "1.0")]
    [InlineData(-2.5, JavaType.PrimitiveKind.Double, "-2.5")]
    [InlineData(1e20, JavaType.PrimitiveKind.Double, "1.0E20")]
    [InlineData(0.00012, JavaType.PrimitiveKind.Double, "1.2E-4")]
    [InlineData(42.0, JavaType.PrimitiveKind.Int, "42")]
    [InlineData(1.5, JavaType.PrimitiveKind.Float, "1.5f")]
    [InlineData(1.1f, JavaType.PrimitiveKind.Float, "1.1f")]
    [InlineData(2L, JavaType.PrimitiveKind.Float, "2.0f")]
    [InlineData(42.0, JavaType.PrimitiveKind.Long, "42L")]
    [InlineData(42L, JavaType.PrimitiveKind.Int, "42")]
    [InlineData(true, JavaType.PrimitiveKind.Boolean, "true")]
    [InlineData("say \"hi\"\n", JavaType.PrimitiveKind.String, "\"say \\\"hi\\\"\\n\"")]
    [InlineData("'", JavaType.PrimitiveKind.Char, "'\\''")]
    [InlineData(null, JavaType.PrimitiveKind.Null, "null")]
    public void LiteralWithoutSource(object? value, JavaType.PrimitiveKind kind, string source)
    {
        var literal = new Literal(Guid.NewGuid(), Space.Empty, Markers.Empty, value, null, null,
            new JavaType.Primitive(kind));

        Assert.Equal(source, Print(literal, MarkerPrinter.Default));
    }

    [Fact]
    public void DecimalLiteralWithoutSource()
    {
        var literal = new Literal(Guid.NewGuid(), Space.Empty, Markers.Empty, 1.50m, null, null,
            new JavaType.Primitive(JavaType.PrimitiveKind.Double));

        Assert.Equal("1.50m", Print(literal, MarkerPrinter.Default));
    }

    private static string Print(J tree, IMarkerPrinter markerPrinter)
    {
        var capture = new PrintOutputCapture<int>(0, markerPrinter);
        new CSharpPrinter<int>().Visit(tree, capture);
        return capture.ToString();
    }

    private class MarkMethods : CSharpVisitor<int>
    {
        public override J VisitMethodDeclaration(MethodDeclaration method, int p) =>
            SearchResult.Found((MethodDeclaration)base.VisitMethodDeclaration(method, p));
    }
}
