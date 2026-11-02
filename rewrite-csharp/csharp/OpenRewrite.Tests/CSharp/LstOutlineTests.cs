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
using OpenRewrite.CSharp;

namespace OpenRewrite.Tests.CSharp;

public class LstOutlineTests
{
    private static LstOutline Outline(string source, string? focusType = null)
    {
        var tree = new CSharpParser().Parse(source);
        return LstOutline.Of(tree, focusType == null ? null : LstOutline.Find(tree, focusType));
    }

    private static LstOutlineNode Node(LstOutlineNode from, string type)
    {
        if (from.Type == type)
        {
            return from;
        }
        foreach (var child in from.Children)
        {
            var match = Find(child, type);
            if (match != null)
            {
                return match;
            }
        }
        throw new InvalidOperationException($"No {type} in the outline");
    }

    private static LstOutlineNode? Find(LstOutlineNode from, string type)
    {
        if (from.Type == type)
        {
            return from;
        }
        foreach (var child in from.Children)
        {
            var match = Find(child, type);
            if (match != null)
            {
                return match;
            }
        }
        return null;
    }

    private static string Covers(LstOutline outline, LstOutlineNode node) =>
        outline.Source.Substring(node.Start, node.Length);

    [Fact]
    public void SourceIsTheWholeTreePrintedBack()
    {
        Assert.Equal("using a;\n", Outline("using a;\n").Source);
    }

    [Fact]
    public void NodeTypesAreQualifiedByLanguage()
    {
        var outline = Outline("using a;\n");

        Assert.Equal("Cs.CompilationUnit", outline.Root.Type);
        Assert.Equal("Cs.UsingDirective", outline.Root.Children.Single().Type);
        Assert.Equal("J.Identifier", outline.Root.Children.Single().Children.Single().Type);
    }

    [Fact]
    public void SpanStartsBeforeThePrefixWhitespaceTheNodeOwns()
    {
        var outline = Outline("using   a;\n");

        Assert.Equal("   a", Covers(outline, Node(outline.Root, "J.Identifier")));
    }

    [Fact]
    public void ParentPropertyNamesTheFieldHoldingTheNode()
    {
        var outline = Outline("class C { int Field; }\n");

        Assert.Equal("Body", Node(outline.Root, "J.Block").Property);
        Assert.Equal("TypeExpression", Node(outline.Root, "J.Primitive").Property);
    }

    [Fact]
    public void ListPropertiesCarryTheirIndex()
    {
        var outline = Outline("class C { void A() {} void B() {} }\n");

        var members = Node(outline.Root, "J.Block").Children;
        Assert.Equal(["Statements[0]", "Statements[1]"], members.Select(m => m.Property));
    }

    [Fact]
    public void PaddingKindRecordsTheWrapperTheNodeSitsIn()
    {
        var outline = Outline("class C { void M(int a) { x = 1; } }\n");

        Assert.Equal(PaddingKind.RightPadded, Node(outline.Root, "J.MethodDeclaration").Padding);
        Assert.Equal(PaddingKind.Container, Node(outline.Root, "J.VariableDeclarations").Padding);
        Assert.Equal(PaddingKind.LeftPadded, Node(outline.Root, "J.Literal").Padding);
        Assert.Equal(PaddingKind.None, Node(outline.Root, "J.Identifier").Padding);
    }

    [Fact]
    public void MarkersAreListedUnderTheNodeTheyDecorate()
    {
        var outline = Outline("class C { int M(a b) { return b?.C; } }\n");

        var marker = Node(outline.Root, "J.FieldAccess").Children.Single(c => c.IsMarker);
        Assert.Equal("J.NullSafe", marker.Type);
        Assert.Equal("Markers", marker.Property);
    }

    [Fact]
    public void FocusStartsTheOutlineDeeperWhileSourceStaysWhole()
    {
        var outline = Outline("class C { void M() { x.Y(); } }\n", "MethodInvocation");

        Assert.Equal("J.MethodInvocation", outline.Root.Type);
        Assert.Equal("class C { void M() { x.Y(); } }\n", outline.Source);
        Assert.Equal("x.Y()", Covers(outline, outline.Root).TrimStart());
        Assert.Equal("", outline.Root.Property);
    }

    [Fact]
    public void FindMatchesBareAndQualifiedTypeNames()
    {
        var tree = new CSharpParser().Parse("using a;\n");

        Assert.NotNull(LstOutline.Find(tree, "UsingDirective"));
        Assert.NotNull(LstOutline.Find(tree, "Cs.UsingDirective"));
        Assert.NotNull(LstOutline.Find(tree, "usingdirective"));
        Assert.Null(LstOutline.Find(tree, "ClassDeclaration"));
    }

    [Fact]
    public void AttestedTypeIsReportedWhenTheNodeCarriesOne()
    {
        var outline = Outline("class C { int Field = 1; }\n");

        Assert.Equal("int", Node(outline.Root, "J.Literal").AttestedType);
        Assert.Null(Node(outline.Root, "J.Block").AttestedType);
    }

    [Fact]
    public void AVariableAndItsNameIdentifierCarrySeparateSpans()
    {
        var outline = Outline("record Point(int X, int Y);\n");

        var named = Node(Node(outline.Root, "J.VariableDeclarations"), "J.NamedVariable");
        Assert.Equal(" X", Covers(outline, named));
        Assert.Equal("X", Covers(outline, Node(named, "J.Identifier")));
        Assert.False(named.IsApproximate);
    }

    [Fact]
    public void SpansOfPrinterInlinedNodesAreDerivedAndMarkedApproximate()
    {
        var outline = Outline("class C { void M() { System.Func<int, int> f = q => q; } }\n");

        var parameters = Node(outline.Root, "J.Lambda.Parameters");
        Assert.True(parameters.IsApproximate);
        Assert.Equal("q", Covers(outline, parameters));

        Assert.False(Node(outline.Root, "J.Lambda").IsApproximate);
    }
}
