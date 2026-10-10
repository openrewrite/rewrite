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
using OpenRewrite.Java;

namespace OpenRewrite.Tests.Tree;

file class NamedVariableCollector : CSharpVisitor<List<NamedVariable>>
{
    public override J VisitNamedVariable(NamedVariable namedVariable, List<NamedVariable> collected)
    {
        collected.Add(namedVariable);
        return base.VisitNamedVariable(namedVariable, collected);
    }
}

public class NamedVariablePrefixTests
{
    public static TheoryData<string, string> Sources => new()
    {
        { "class Foo {\n    void Bar() {\n        int x = 1, y = 2;\n    }\n}", "local_declaration" },
        { "class Foo {\n    int x = 1;\n}", "field_declaration" },
        { "class Foo {\n    event System.EventHandler MyEvent;\n}", "event_field_declaration" },
        { "class Foo {\n    void Bar(int x, string y) { }\n}", "parameters" },
        { "delegate void MyDelegate(int x, string y);", "delegate_parameters" },
        { "class Foo {\n    void Bar() {\n        System.Func<int, int> f = (int x) => x;\n    }\n}", "typed_lambda_parameter" },
        { "class Foo {\n    void Bar() {\n        foreach (var item in new int[0]) { }\n    }\n}", "foreach" },
        { "class Foo {\n    void Bar() {\n        for (int i = 0; i < 1; i++) { }\n    }\n}", "for_loop" },
        { "class Foo {\n    void Bar() {\n        try { } catch (System.Exception e) { }\n    }\n}", "catch_declaration" },
        { "class Foo {\n    void Bar(object o) {\n        if (o is string s) { }\n    }\n}", "declaration_pattern" },
        { "class Foo {\n    void Bar(object o) {\n        switch (o) {\n            case int _:\n                break;\n        }\n    }\n}", "discard_designation" },
        { "class Foo {\n    void Bar(object o) {\n        var r = o switch { var v => v };\n    }\n}", "var_pattern" },
        { "class Foo {\n    void Bar() {\n        using (var s = new System.IO.MemoryStream()) { }\n    }\n}", "using_statement" },
    };

    [Theory]
    [MemberData(nameof(Sources))]
    public void NameIdentifierCarriesNoPrefix(string source, string testId)
    {
        var cu = new CSharpParser().Parse(source);

        var collected = new List<NamedVariable>();
        new NamedVariableCollector().Visit(cu, collected);

        Assert.NotEmpty(collected);
        foreach (var namedVariable in collected)
        {
            Assert.True(namedVariable.Name.Prefix.Whitespace.Length == 0,
                $"[{testId}] J.NamedVariable '{namedVariable.Name.SimpleName}' has its leading whitespace " +
                $"|{namedVariable.Name.Prefix.Whitespace}| on the child J.Identifier instead of on J.NamedVariable.");
        }
    }

    [Theory]
    [InlineData("class Foo {\n    void Bar() {\n        int    x = 1;\n    }\n}", "    ")]
    [InlineData("class Foo {\n    void Bar(int    x) { }\n}", "    ")]
    [InlineData("class Foo {\n    void Bar() {\n        foreach (var    item in new int[0]) { }\n    }\n}", "    ")]
    [InlineData("class Foo {\n    void Bar() {\n        try { } catch (System.Exception    e) { }\n    }\n}", "    ")]
    [InlineData("class Foo {\n    void Bar(object o) {\n        if (o is string    s) { }\n    }\n}", "    ")]
    public void PrefixIsAttachedToNamedVariable(string source, string expectedPrefix)
    {
        var cu = new CSharpParser().Parse(source);

        var collected = new List<NamedVariable>();
        new NamedVariableCollector().Visit(cu, collected);

        Assert.Contains(expectedPrefix, collected.Select(v => v.Prefix.Whitespace));
    }
}
