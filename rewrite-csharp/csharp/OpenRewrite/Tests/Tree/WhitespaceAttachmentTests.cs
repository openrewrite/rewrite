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

namespace OpenRewrite.Tests.Tree;

file class OutputNode(OpenRewrite.Core.Tree element)
{
    public OpenRewrite.Core.Tree Element { get; } = element;
    public List<object> Children { get; } = [];

    public override string ToString()
    {
        var childrenStr = string.Join(", ", Children.Select(c =>
            c is OutputNode node ? node.ToString() : $"Text({c})"));
        return $"{PrettifyType(Element)}{{{childrenStr}}}";
    }

    internal static string PrettifyType(OpenRewrite.Core.Tree tree)
    {
        var type = tree.GetType();
        var ns = type.Namespace;
        if (ns != null && ns.Contains("CSharp"))
            return $"Cs.{type.Name}";
        if (ns != null && ns.Contains("Java"))
            return $"J.{type.Name}";
        return type.Name;
    }
}

file class TreeStructurePrintOutputCapture : PrintOutputCapture<int>
{
    public List<OutputNode> RootNodes { get; } = [];
    private readonly Stack<OutputNode> _nodeStack = new();

    public TreeStructurePrintOutputCapture() : base(0)
    {
    }

    public void StartNode(OpenRewrite.Core.Tree element)
    {
        var node = new OutputNode(element);
        if (_nodeStack.Count > 0)
            _nodeStack.Peek().Children.Add(node);
        else
            RootNodes.Add(node);
        _nodeStack.Push(node);
    }

    public void EndNode()
    {
        _nodeStack.Pop();
    }

    public override PrintOutputCapture<int> Append(string? text)
    {
        if (text is { Length: > 0 } && _nodeStack.Count > 0)
            _nodeStack.Peek().Children.Add(text);
        return base.Append(text);
    }

    public override PrintOutputCapture<int> Append(char c)
    {
        if (_nodeStack.Count > 0)
            _nodeStack.Peek().Children.Add(c.ToString());
        return base.Append(c);
    }
}

file class TreeCapturingCSharpPrinter : CSharpPrinter<int>
{
    protected override void BeforeSyntax(J j, PrintOutputCapture<int> p)
    {
        if (p is TreeStructurePrintOutputCapture capture)
            capture.StartNode(j);
        base.BeforeSyntax(j, p);
    }

    protected override void AfterSyntax(J j, PrintOutputCapture<int> p)
    {
        base.AfterSyntax(j, p);
        if (p is TreeStructurePrintOutputCapture capture)
            capture.EndNode();
    }
}

file static class ViolationChecker
{
    internal static List<string> FindWhitespaceViolations(List<OutputNode> rootNodes)
    {
        var violations = new List<string>();

        void CheckNode(OutputNode node)
        {
            if (node.Children.Count > 0 && node.Children[0] is OutputNode firstChild)
            {
                if (firstChild.Children.Count > 0 && firstChild.Children[0] is string grandchild
                    && grandchild.Length > 0 && grandchild.Trim().Length == 0)
                {
                    var parentKind = OutputNode.PrettifyType(node.Element);
                    var childKind = OutputNode.PrettifyType(firstChild.Element);
                    violations.Add(
                        $"{parentKind} has child {childKind} starting with whitespace " +
                        $"|{grandchild}|. The whitespace should rather be attached to {parentKind}.");
                }
            }

            foreach (var child in node.Children)
            {
                if (child is OutputNode childNode)
                    CheckNode(childNode);
            }
        }

        foreach (var node in rootNodes)
            CheckNode(node);

        return violations;
    }
}

public class WhitespaceAttachmentTests
{
    public static TheoryData<string, string> TestCases => new()
    {
        { "class Foo { }", "empty_class" },
        { "public static class Foo { }", "class_with_modifiers" },
        { "class Foo : Bar { }", "class_with_base_type" },
        { "struct Point { public int X; public int Y; }", "struct" },
        { "record Person(string Name, int Age);", "record" },
        { "interface IFoo { void Bar(); }", "interface" },
        { "enum Color { Red, Green, Blue }", "enum" },
        { "using System;", "using_directive" },
        { "using static System.Math;", "using_static" },
        { "namespace MyApp { class Foo { } }", "namespace" },
        { "namespace MyApp;\nclass Foo { }", "file_scoped_namespace" },
        { "class Foo {\n    public int X { get; set; }\n}", "property" },
        { "class Foo {\n    public void Bar() { }\n}", "method" },
        { "class Foo {\n    public int Bar() => 42;\n}", "expression_bodied_method" },
        { "class Foo {\n    void Bar() {\n        if (true) { }\n        else { }\n    }\n}", "if_else" },
        { "class Foo {\n    void Bar() {\n        for (int i = 0; i < 10; i++) { }\n    }\n}", "for_loop" },
        { "class Foo {\n    void Bar() {\n        foreach (var x in new int[] { 1, 2, 3 }) { }\n    }\n}", "foreach_loop" },
        { "class Foo {\n    void Bar() {\n        while (true) { }\n    }\n}", "while_loop" },
        { "class Foo {\n    void Bar() {\n        do { } while (true);\n    }\n}", "do_while_loop" },
        { "class Foo {\n    void Bar() {\n        try { }\n        catch (System.Exception e) { }\n        finally { }\n    }\n}", "try_catch_finally" },
        { "class Foo {\n    void Bar(int x) {\n        switch (x) {\n            case 1:\n                break;\n            default:\n                break;\n        }\n    }\n}", "switch_statement" },
        { "class Foo {\n    void Bar() {\n        System.Func<int, int> f = x => x + 1;\n    }\n}", "lambda" },
        { "class Foo {\n    (int, string) Bar() => (1, \"hello\");\n}", "tuple" },
        { "class Foo {\n    string Bar(int x) => $\"Value is {x}\";\n}", "interpolated_string" },
        { "class Foo {\n    void Bar() {\n        var x = 1;\n        var y = \"hello\";\n    }\n}", "var_declarations" },
        { "class Foo {\n    void Bar(object o) {\n        if (o is int i) { }\n    }\n}", "pattern_matching" },
        { "class Foo {\n    int[] arr = new int[] { 1, 2, 3 };\n}", "array" },
        { "delegate void MyDelegate(int x, string y);", "delegate" },
        { "class Foo {\n    void Bar() {\n        throw new System.Exception(\"error\");\n    }\n}", "throw" },
        { "class Foo {\n    int Bar(bool b) => b ? 1 : 2;\n}", "conditional_expression" },
        { "class Foo {\n    string Bar(string? s) => s ?? \"default\";\n}", "null_coalescing" },
        { "class Foo {\n    void Bar() {\n        var x = (int)1.0;\n    }\n}", "cast" },
        { "class Foo {\n    int X { get; set; }\n    void Bar() {\n        var f = new Foo { X = 1 };\n    }\n}", "object_initializer" },
        { "[System.Serializable]\nclass Foo { }", "attribute" },
        { "class Foo {\n    void Bar(ref int x, out int y, in int z) { y = 0; }\n}", "ref_parameters" },
        { "class Foo<T, U> {\n    T Value { get; set; }\n}", "generic_class" },
        { "// This is a comment\nclass Foo { }", "single_line_comment" },
        { "/* This is a\n   multi-line comment */\nclass Foo { }", "multi_line_comment" },
        { "class Foo {\n    void Bar() {\n        int x = 1, y = 2, z = 3;\n    }\n}", "multiple_var_declarations" },
        { "class Foo {\n    void Bar() {\n        Bar(x: 1, y: 2);\n    }\n    void Bar(int x, int y) { }\n}", "named_arguments" },
        { "class Foo {\n    string Bar(int x) => x switch {\n        1 => \"one\",\n        2 => \"two\",\n        _ => \"other\"\n    };\n}", "switch_expression" },
        { "class Foo {\n    async System.Threading.Tasks.Task Bar() {\n        await System.Threading.Tasks.Task.Delay(1);\n    }\n}", "async_await" },
        { "class Foo {\n    System.Collections.Generic.IEnumerable<int> Bar() {\n        yield return 1;\n        yield return 2;\n    }\n}", "yield_return" },
        { "class Foo {\n    void Bar() {\n        using (var x = new System.IO.MemoryStream()) { }\n    }\n}", "using_statement" },
        { "class Foo {\n    object _lock = new object();\n    void Bar() {\n        lock (_lock) { }\n    }\n}", "lock_statement" },
        { "class Foo {\n    int this[int i] => i;\n}", "indexer" },
        { "class Foo {\n    public static Foo operator +(Foo a, Foo b) => a;\n}", "operator_overload" },
        { "class Foo {\n    public static implicit operator int(Foo f) => 0;\n}", "conversion_operator" },
        { "interface IFoo { void Bar(); }\nclass Foo : IFoo {\n    void IFoo.Bar() { }\n}", "explicit_interface_impl" },
        { "class Outer {\n    class Inner { }\n}", "nested_class" },
        { "class Foo {\n    static Foo() { }\n}", "static_constructor" },
        { "class Foo {\n    ~Foo() { }\n}", "destructor" },
        { "class Foo {\n    void Bar() {\n        var x = (int)1.0;\n    }\n}", "cast_expression" },
        { "class Foo {\n    void Bar() {\n        var list = new System.Collections.Generic.List<int> { 1, 2, 3 };\n    }\n}", "collection_initializer" },
        { "class Foo {\n    string? Name { get; set; }\n    int? Count { get; set; }\n}", "nullable" },
        { "class Foo {\n    void Bar(object o) {\n        if (o is string s) { }\n    }\n}", "is_pattern_string" },
        { "class Foo {\n    void Bar(object o) {\n        var t = typeof(int);\n    }\n}", "typeof" },
        { "class Foo {\n    void Bar() {\n        goto end;\n        end:\n        return;\n    }\n}", "goto_statement" },
        { "class Foo {\n    int Bar() => default;\n    int Baz() => default(int);\n}", "default_expression" },
        { "class Foo {\n    event System.EventHandler MyEvent;\n}", "event" },
        { "class Foo {\n    void Bar(string? s) {\n        var len = s?.Length;\n    }\n}", "null_conditional" },
        { "class Foo {\n    void Bar() {\n        int[] arr = { 1, 2, 3, 4, 5 };\n        var slice = arr[1..3];\n    }\n}", "range_expression" },
        { "record Point(int X, int Y);\nclass Foo {\n    void Bar() {\n        var p = new Point(1, 2);\n        var q = p with { X = 3 };\n    }\n}", "with_expression" },
        { "class Foo {\n    void Bar() {\n        int.TryParse(\"1\", out var result);\n    }\n}", "declaration_expression" },
        { "class Foo {\n    void Bar(object o) {\n        _ = o;\n    }\n}", "discard" },
        { "class Foo {\n    void Bar() {\n        checked { int x = int.MaxValue; }\n    }\n}", "checked_block" },
        { "class Foo {\n    void Bar() {\n        var nums = new int[] { 1, 2, 3 };\n        var q = from n in nums\n                where n > 1\n                select n;\n    }\n}", "linq_query" },
        { "/// <summary>\n/// My class\n/// </summary>\nclass Foo { }", "xml_doc_comment" },
        { "class Foo {\n    public int X { get; set; } = 10;\n}", "property_with_initializer" },
        { "class Foo<T> where T : class, new() { }", "generic_constraints" },
        { "class Foo {\n    void Bar() {\n        var x = new { Name = \"test\", Value = 42 };\n    }\n}", "anonymous_type" },
        { "class Foo {\n    void Bar(object o) {\n        switch (o) {\n            case int i:\n                break;\n            case string s:\n                break;\n        }\n    }\n}", "switch_type_pattern" },
        { "class Foo {\n    void Bar(int x) {\n        var y = x is > 0 and < 100;\n    }\n}", "relational_pattern" },
        { "class Foo {\n    void Bar(object o) {\n        if (o is not null) { }\n    }\n}", "negated_pattern" },
        { "class Foo : System.IDisposable {\n    public void Dispose() { }\n}", "qualified_interface" },
        { "using Sys = System;\nclass Foo { }", "using_alias" },
        { "class Foo {\n    void Bar() {\n        System.Action a = () => { };\n    }\n}", "lambda_block" },
        { "class Foo {\n    void Bar() {\n        var x = true ? 1 : 2;\n        var y = x + 1;\n    }\n}", "multi_statement" },
        { "[System.Flags]\nenum Permissions { Read = 1, Write = 2, Execute = 4 }", "flags_enum" },
        { "[System.Serializable]\nstruct Foo { }", "attribute_on_struct" },
        { "class Foo {\n    [System.Obsolete]\n    void Bar() { }\n}", "attribute_on_method" },
        { "class Foo {\n    [System.Obsolete]\n    public int X { get; set; }\n}", "attribute_on_property" },
        { "class Foo {\n    void Bar([System.Obsolete] int x) { }\n}", "attribute_on_parameter" },
        { "class Foo {\n    void Bar() {\n        for (int i = 0; i < 10; i++) {\n            if (i == 5) continue;\n            if (i == 8) break;\n        }\n    }\n}", "break_continue" },
        { "class Foo {\n    void Bar() {\n        var s = \"hello\" + \" world\";\n    }\n}", "string_concatenation" },
        { "class Foo {\n    void Bar() {\n        var x = !true;\n        var y = -1;\n    }\n}", "unary_operators" },
        { "class Foo {\n    void Bar() {\n        var x = 1;\n        x++;\n        x--;\n    }\n}", "postfix_operators" },
        { "class Foo {\n    void Bar() {\n        object o = \"test\";\n        var s = o as string;\n    }\n}", "as_expression" },
        { "class Foo {\n    void Bar(int x) {\n        switch (x) {\n            case 1:\n            case 2:\n                break;\n        }\n    }\n}", "switch_fallthrough" },
    };

    [Theory]
    [MemberData(nameof(TestCases))]
    public void WhitespaceAttachment(string source, string testId)
    {
        // given
        var parser = new CSharpParser();
        var cu = parser.Parse(source);
        var capture = new TreeStructurePrintOutputCapture();
        var printer = new TreeCapturingCSharpPrinter();

        // when
        printer.Visit(cu, capture);

        // then
        Assert.Equal(source, capture.ToString());
        var violations = ViolationChecker.FindWhitespaceViolations(capture.RootNodes);
        Assert.True(violations.Count == 0,
            $"[{testId}] Expected no whitespace attachment violations but found {violations.Count}:\n" +
            string.Join("\n", violations));
    }

}
