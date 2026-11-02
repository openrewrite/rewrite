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
using System.Text;
using System.Text.RegularExpressions;
using Microsoft.CodeAnalysis;
using Microsoft.CodeAnalysis.CSharp;
using OpenRewrite.Core;
using OpenRewrite.CSharp;
using OpenRewrite.Java;
using OpenRewrite.Xml;
using CsAssignmentOperation = OpenRewrite.CSharp.AssignmentOperation;
using JAssignmentOperation = OpenRewrite.Java.AssignmentOperation;

namespace OpenRewrite.Test;

/// <summary>
/// Finds nodes in a recipe's output that the parser builds differently from the text they print, which recipes that
/// run after it don't match the way they match parsed code.
/// </summary>
internal static class TreeShapes
{
    private const int MaxPerFile = 5;
    private const int SnippetLength = 60;

    private static readonly Regex Whitespace = new(@"\s+");

    /// <param name="fresh">A fresh parse of each compilation unit the recipe left, by the recipe's output.</param>
    public static void AssertParserShaped(Recipe recipe, IEnumerable<Result> results,
        IReadOnlyDictionary<SourceFile, RewriteTest.FreshParse> fresh)
    {
        var found = new StringBuilder();
        foreach (var (before, after) in results)
        {
            if (after is not CompilationUnit cu || !fresh.TryGetValue(after, out var parse) ||
                before != null && Print(before) == Print(after))
            {
                continue;
            }
            var problems = Problems(before, cu, parse);
            if (problems.Count == 0)
            {
                continue;
            }
            found.Append("  ").Append(after.SourcePath).Append(":\n");
            foreach (var problem in problems)
            {
                found.Append("    ").Append(problem).Append('\n');
            }
        }

        if (found.Length > 0)
        {
            Assert.Fail(
                "Write code with a CSharpTemplate or the C# parser, or build the nodes the way the parser builds the " +
                "text they print.\n" +
                $"The output of {recipe.Name} holds nodes that the parser makes differently of the same text:\n" +
                found +
                "A recipe that runs after it matches the tree, not the text, so it misses or mistakes a node that " +
                "parsed code never holds. If the output is meant to differ from parsed code, opt out with " +
                "spec.SetValidations(new Validations { ParserShapedTrees = false }).");
        }
    }

    private static List<string> Problems(SourceFile? before, CompilationUnit after, RewriteTest.FreshParse parse)
    {
        var problems = new List<string>();
        // a recipe that keeps code the parser couldn't read, such as a test's deliberately broken code, didn't break it
        if (SyntaxError(parse.SyntaxTree) is { } error &&
            (before is not CompilationUnit || SyntaxError(CSharpSyntaxTree.ParseText(Print(before))) == null))
        {
            problems.Add("the text it prints doesn't parse: " + error);
        }
        else if (parse.Unit != null)
        {
            Compare(Skeleton(after), Skeleton(parse.Unit), problems);
        }
        return problems;
    }

    private static string? SyntaxError(SyntaxTree tree)
    {
        var error = tree.GetDiagnostics().FirstOrDefault(d => d.Severity == DiagnosticSeverity.Error);
        if (error == null)
        {
            return null;
        }
        var line = tree.GetText().Lines[error.Location.GetLineSpan().StartLinePosition.Line].ToString().Trim();
        return $"{error.Id} {error.GetMessage()} at `{Truncate(line)}`";
    }

    private static void Compare(Node node, Node parsed, List<string> problems)
    {
        if (problems.Count >= MaxPerFile)
        {
            return;
        }
        if (node.Tree.GetType() != parsed.Tree.GetType())
        {
            problems.Add($"{node} is {WithArticle(node.Tree.GetType())} where the parser makes " +
                         WithArticle(parsed.Tree.GetType()));
            return;
        }
        var value = Value(node.Tree);
        var parsedValue = Value(parsed.Tree);
        if (value != parsedValue)
        {
            problems.Add($"{node} has {value} where the parser makes {parsedValue}");
        }
        if (node.Children.Count != parsed.Children.Count)
        {
            problems.Add($"{node} holds {Kinds(node.Children)} where the parser makes {Kinds(parsed.Children)}");
            return;
        }
        for (var i = 0; i < node.Children.Count; i++)
        {
            Compare(node.Children[i], parsed.Children[i], problems);
        }
    }

    /// <summary>
    /// What tells a node apart from another node of its type with the same children, such as an identifier's name or
    /// a literal's value, or null when nothing does.
    /// </summary>
    private static string? Value(J tree) => tree switch
    {
        Identifier identifier => $"the name `{identifier.SimpleName}`",
        Literal literal => (literal.Value == null
                               ? "the value null"
                               : $"the {literal.Value.GetType().Name} value {Quoted(literal.Value)}") +
                           " of type " + (literal.Type is JavaType.Primitive type
                               ? type.Kind.ToString()
                               : literal.Type?.GetType().Name ?? "null"),
        Modifier { Type: Modifier.ModifierType.LanguageExtension } modifier => $"the modifier {modifier.Keyword}",
        Modifier modifier => $"the modifier {modifier.Type}",
        Primitive primitive => $"the type {primitive.Kind}",
        Binary binary => $"the operator {binary.Operator.Element}",
        CsBinary binary => $"the operator {binary.Operator.Element}",
        Unary unary => $"the operator {unary.Operator.Element}",
        CsUnary unary => $"the operator {unary.Operator.Element}",
        JAssignmentOperation assignment => $"the operator {assignment.Operator.Element}",
        CsAssignmentOperation assignment => $"the operator {assignment.Operator.Element}",
        Keyword keyword => $"the keyword {keyword.Kind}",
        _ => null
    };

    private static string Quoted(object value)
    {
        var printed = Truncate(value.ToString()!.Replace("\\", "\\\\").Replace("\n", "\\n").Replace("\r", "\\r")
            .Replace("\t", "\\t"), 40);
        return value switch
        {
            char => $"'{printed}'",
            string => $"\"{printed}\"",
            _ => printed
        };
    }

    private static string Kinds(List<Node> nodes) =>
        nodes.Count == 0 ? "nothing" : string.Join(", ", nodes.Select(n => TypeName(n.Tree.GetType())));

    /// <summary>The nodes of a compilation unit, each with the nodes it holds.</summary>
    private static Node Skeleton(CompilationUnit cu)
    {
        var builder = new SkeletonBuilder();
        builder.Visit(cu, 0);
        return builder.Root ?? new Node(cu);
    }

    private sealed class SkeletonBuilder : CSharpVisitor<int>
    {
        private readonly Dictionary<J, Node> _nodes = new(ReferenceEqualityComparer.Instance);

        public Node? Root { get; private set; }

        public override J? PreVisit(J tree, int p)
        {
            // the parser adds empty elements where recipes often leave none
            if (tree is Empty)
            {
                StopAfterPreVisit();
                return tree;
            }
            var node = new Node(tree);
            Node? parent = null;
            foreach (var cursor in Cursor.PathToRoot())
            {
                if (cursor.Value is J ancestor && _nodes.TryGetValue(ancestor, out parent))
                {
                    break;
                }
            }
            if (parent == null)
            {
                Root ??= node;
            }
            else
            {
                parent.Children.Add(node);
            }
            _nodes[tree] = node;
            return tree;
        }
    }

    private sealed class Node(J tree)
    {
        public J Tree { get; } = tree;

        public List<Node> Children { get; } = [];

        public override string ToString()
        {
            string printed;
            try
            {
                printed = Whitespace.Replace(Print(J.SetPrefix(Tree, Space.Empty)).Trim(), " ");
            }
            catch (Exception)
            {
                printed = "?";
            }
            return $"`{Truncate(printed)}`";
        }
    }

    private static string WithArticle(Type type)
    {
        var name = TypeName(type);
        return ("AEIOU".Contains(name[0]) ? "an " : "a ") + name;
    }

    private static string TypeName(Type type)
    {
        var name = type.Name;
        var arity = name.IndexOf('`');
        return arity >= 0 ? name[..arity] : name;
    }

    private static string Truncate(string text, int length = SnippetLength) =>
        text.Length > length ? text[..length] + "..." : text;

    private static string Print(Tree tree)
    {
        var capture = new PrintOutputCapture<object>(null!, MarkerPrinter.Sanitized);
        if (tree is Xml.Xml)
        {
            new XmlPrinter<object>().Visit(tree, capture);
        }
        else
        {
            new CSharpPrinter<object>().Visit(tree, capture);
        }
        return capture.ToString();
    }
}
