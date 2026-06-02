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
using static OpenRewrite.Tests.CSharp.TestHelpers;

namespace OpenRewrite.Tests.CSharp;

public class AttributeMatcherTests
{
    private static AnnotatedStatement MakeAnnotatedStatement(params Annotation[] annotations)
    {
        var attrList = new AttributeList(
            Guid.NewGuid(), Space.Empty, Markers.Empty,
            null, annotations.Select(Pad).ToList());
        return new AnnotatedStatement(
            Guid.NewGuid(), Space.Empty, Markers.Empty,
            [attrList],
            new Empty(Guid.NewGuid(), Space.Empty, Markers.Empty));
    }

    private static Cursor CursorInside(AnnotatedStatement stmt) =>
        new(new Cursor(new Cursor(), stmt), MakeId("inner"));

    // =============================================================
    // Matches(Annotation) — simple name, no type attribution
    // =============================================================

    [Fact]
    public void Matches_SimpleNameIdentifier()
    {
        var matcher = new AttributeMatcher("Obsolete");
        Assert.True(matcher.Matches(MakeAnnotation(MakeId("Obsolete"))));
    }

    [Fact]
    public void Matches_SimpleNameFieldAccess()
    {
        var matcher = new AttributeMatcher("Obsolete");
        Assert.True(matcher.Matches(MakeAnnotation(MakeFieldAccess("System", "Obsolete"))));
    }

    [Fact]
    public void Matches_NoMatch()
    {
        var matcher = new AttributeMatcher("Obsolete");
        Assert.False(matcher.Matches(MakeAnnotation(MakeId("Serializable"))));
    }

    // =============================================================
    // Attribute suffix normalization
    // =============================================================

    [Fact]
    public void Matches_SourceHasSuffix_PatternDoesNot()
    {
        var matcher = new AttributeMatcher("Obsolete");
        Assert.True(matcher.Matches(MakeAnnotation(MakeId("ObsoleteAttribute"))));
    }

    [Fact]
    public void Matches_PatternHasSuffix_SourceDoesNot()
    {
        var matcher = new AttributeMatcher("ObsoleteAttribute");
        Assert.True(matcher.Matches(MakeAnnotation(MakeId("Obsolete"))));
    }

    [Fact]
    public void Matches_BothHaveSuffix()
    {
        var matcher = new AttributeMatcher("ObsoleteAttribute");
        Assert.True(matcher.Matches(MakeAnnotation(MakeId("ObsoleteAttribute"))));
    }

    [Fact]
    public void NormalizeName_DoesNotStripShortNames()
    {
        // "Attribute" itself should not be stripped to empty string
        Assert.Equal("Attribute", AttributeMatcher.NormalizeAttributeName("Attribute"));
    }

    [Fact]
    public void NormalizeName_MultiSegmentFqn()
    {
        Assert.Equal("System.ComponentModel.Description",
            AttributeMatcher.NormalizeAttributeName("System.ComponentModel.DescriptionAttribute"));
    }

    // =============================================================
    // Matches(Annotation) — with type attribution
    // =============================================================

    [Fact]
    public void Matches_TypeAttributed_SimpleName()
    {
        var type = new JavaType.Class { FullyQualifiedName = "System.ObsoleteAttribute" };
        var matcher = new AttributeMatcher("Obsolete");
        Assert.True(matcher.Matches(MakeAnnotation(MakeIdWithType("Obsolete", type))));
    }

    [Fact]
    public void Matches_TypeAttributed_FullyQualified()
    {
        var type = new JavaType.Class { FullyQualifiedName = "System.ObsoleteAttribute" };
        var matcher = new AttributeMatcher("System.ObsoleteAttribute");
        Assert.True(matcher.Matches(MakeAnnotation(MakeIdWithType("Obsolete", type))));
    }

    [Fact]
    public void Matches_TypeAttributed_FullyQualified_NoMatch()
    {
        var type = new JavaType.Class { FullyQualifiedName = "System.ObsoleteAttribute" };
        var matcher = new AttributeMatcher("MyNamespace.ObsoleteAttribute");
        Assert.False(matcher.Matches(MakeAnnotation(MakeIdWithType("Obsolete", type))));
    }

    [Fact]
    public void Matches_TypeAttributed_SimpleNameOnly_MatchesAnyNamespace()
    {
        var type = new JavaType.Class { FullyQualifiedName = "MyNamespace.Custom.ObsoleteAttribute" };
        var matcher = new AttributeMatcher("Obsolete");
        Assert.True(matcher.Matches(MakeAnnotation(MakeIdWithType("Obsolete", type))));
    }

    // =============================================================
    // Matches(AnnotatedStatement)
    // =============================================================

    [Fact]
    public void Matches_AnnotatedStatement_Found()
    {
        var matcher = new AttributeMatcher("Obsolete");
        var stmt = MakeAnnotatedStatement(MakeAnnotation(MakeId("Obsolete")));
        Assert.True(matcher.Matches(stmt));
    }

    [Fact]
    public void Matches_AnnotatedStatement_NotFound()
    {
        var matcher = new AttributeMatcher("Obsolete");
        var stmt = MakeAnnotatedStatement(MakeAnnotation(MakeId("Serializable")));
        Assert.False(matcher.Matches(stmt));
    }

    [Fact]
    public void Matches_AnnotatedStatement_MultipleAttributes()
    {
        var matcher = new AttributeMatcher("Obsolete");
        var stmt = MakeAnnotatedStatement(
            MakeAnnotation(MakeId("Serializable")),
            MakeAnnotation(MakeId("Obsolete")));
        Assert.True(matcher.Matches(stmt));
    }

    // =============================================================
    // Matches(Cursor)
    // =============================================================

    [Fact]
    public void Matches_Cursor_Found()
    {
        var matcher = new AttributeMatcher("Obsolete");
        var stmt = MakeAnnotatedStatement(MakeAnnotation(MakeId("Obsolete")));
        Assert.True(matcher.Matches(CursorInside(stmt)));
    }

    [Fact]
    public void Matches_Cursor_NoAnnotatedStatement()
    {
        var matcher = new AttributeMatcher("Obsolete");
        var cursor = new Cursor(new Cursor(), MakeId("inner"));
        Assert.False(matcher.Matches(cursor));
    }

    [Fact]
    public void Matches_Cursor_MultipleAttributeLists()
    {
        var attrList1 = new AttributeList(
            Guid.NewGuid(), Space.Empty, Markers.Empty,
            null, [Pad(MakeAnnotation(MakeId("Serializable")))]);
        var attrList2 = new AttributeList(
            Guid.NewGuid(), Space.Empty, Markers.Empty,
            null, [Pad(MakeAnnotation(MakeId("Obsolete")))]);
        var stmt = new AnnotatedStatement(
            Guid.NewGuid(), Space.Empty, Markers.Empty,
            [attrList1, attrList2],
            new Empty(Guid.NewGuid(), Space.Empty, Markers.Empty));

        var matcher = new AttributeMatcher("Obsolete");
        Assert.True(matcher.Matches(CursorInside(stmt)));
    }
}
