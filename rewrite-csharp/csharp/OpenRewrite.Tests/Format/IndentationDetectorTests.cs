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
using OpenRewrite.CSharp.Format;

namespace OpenRewrite.Tests.Format;

public class IndentationDetectorTests
{
    private readonly CSharpParser _parser = new();

    [Fact]
    public void Tabs()
    {
        var cu = _parser.Parse("class C\n{\n\tvoid M()\n\t{\n\t\tFoo();\n\t}\n}\n");
        Assert.Equal((true, (int?)null), IndentationDetector.Detect(cu));
    }

    [Theory]
    [InlineData("class C\n{\n  void M()\n  {\n    Foo();\n  }\n}\n", 2)]
    [InlineData("class C\n{\n    void M()\n    {\n        Foo();\n    }\n}\n", 4)]
    [InlineData("namespace N\n{\n   class C\n   {\n      int x;\n   }\n}\n", 3)]
    [InlineData("namespace N;\n\nclass C\n{\n  int x;\n}\n", 2)]
    [InlineData("namespace N\n{\nclass C\n{\n    void M()\n    {\n        Foo();\n    }\n}\n}\n", 4)]
    public void Spaces(string source, int indentSize)
    {
        Assert.Equal((false, (int?)indentSize), IndentationDetector.Detect(_parser.Parse(source)));
    }

    [Fact]
    public void MostLinesDecide()
    {
        var cu = _parser.Parse("class C\n{\n\tint a;\n  int b;\n  int c;\n  int d;\n}\n");
        Assert.Equal((false, (int?)2), IndentationDetector.Detect(cu));
    }

    [Theory]
    [InlineData("class C { void M() { Foo(); } }")]
    [InlineData("class C\n{\nint x;\n}\n")]
    public void NothingIndented(string source)
    {
        Assert.Null(IndentationDetector.Detect(_parser.Parse(source)));
    }

    [Fact]
    public void DefaultsIndentAsTheFileDoes()
    {
        var tabs = IndentationDetector.DefaultFor(_parser.Parse("class C\n{\n\tint x;\n}\n"));
        var twoSpaces = IndentationDetector.DefaultFor(_parser.Parse("class C\n{\n  int x;\n}\n"));

        Assert.True(tabs.UseTabs);
        Assert.Equal(tabs.TabSize, tabs.IndentSize);
        Assert.False(twoSpaces.UseTabs);
        Assert.Equal(2, twoSpaces.IndentSize);
        Assert.Same(CSharpFormatStyle.Default, IndentationDetector.DefaultFor(_parser.Parse("class C { }")));
    }
}
