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
package org.openrewrite.csharp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TextComment;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ensures {@link CSharpVisitor} visits every {@link Space} the printer emits: the count of newlines the
 * visitor observes while walking the LST must equal the count in {@code printAll()}'s output. Any gap means
 * the visitor skipped a space, which is invisible to in-process LST-walking code (e.g. a line counter).
 */
class CSharpVisitorCompletenessTest {

    static long nl(String s) {
        return s == null ? 0 : s.chars().filter(c -> c == '\n').count();
    }

    long visitorNewlines(SourceFile cu) {
        AtomicInteger n = new AtomicInteger();
        new CSharpVisitor<AtomicInteger>() {
            @Override
            public Space visitSpace(Space s, Space.Location l, AtomicInteger a) {
                a.addAndGet((int) nl(s.getWhitespace()));
                for (Comment c : s.getComments()) {
                    a.addAndGet((int) (c instanceof TextComment ? nl(((TextComment) c).getText()) : nl(c.printComment(getCursor()))));
                    a.addAndGet((int) nl(c.getSuffix()));
                }
                return super.visitSpace(s, l, a);
            }

            @Override
            public J visitLiteral(J.Literal lit, AtomicInteger a) {
                a.addAndGet((int) nl(lit.getValueSource()));
                return super.visitLiteral(lit, a);
            }
        }.visit(cu, n);
        return n.get();
    }

    @Test
    @Timeout(300)
    void completeness() {
        CSharpParser parser = CSharpParser.builder().build();
        InMemoryExecutionContext ctx = new InMemoryExecutionContext();

        // Parse every snippet in one project (one RPC round-trip) by giving each a distinct source path.
        List<Parser.Input> inputs = new ArrayList<>();
        for (int i = 0; i < SNIPPETS.size(); i++) {
            String src = SNIPPETS.get(i);
            inputs.add(Parser.Input.fromString(Path.of("Src" + i + ".cs"), src));
        }
        Map<Integer, SourceFile> byIdx = new HashMap<>();
        parser.parseInputs(inputs, null, ctx).forEach(sf -> {
            String name = sf.getSourcePath().getFileName().toString();
            byIdx.put(Integer.parseInt(name.substring(3, name.length() - 3)), sf);
        });
        assertThat(byIdx).hasSameSizeAs(SNIPPETS);

        StringBuilder failures = new StringBuilder();
        for (int i = 0; i < SNIPPETS.size(); i++) {
            SourceFile cu = byIdx.get(i);
            long expected = nl(cu.printAll());
            long actual = visitorNewlines(cu);
            if (expected != actual) {
                failures.append("### idx=").append(i).append(" expected=").append(expected).append(" visitor=").append(actual)
                  .append('\n').append(SNIPPETS.get(i).replace("\n", "\\n")).append('\n');
            }
        }
        assertThat(failures.toString()).isEmpty();
    }

    static final List<String> SNIPPETS = List.of(
      // using directives
      "using System;\nusing System.Collections.Generic;\n",
      "using static System.Math;\n",
      "global using System;\n",
      "global using static System.Console;\n",
      "using Con = System.Console;\n",
      "using System;\nusing System.Text;\nnamespace N {\n    using System.Linq;\n}\n",

      // namespaces
      "namespace N {\n    class C {\n    }\n}\n",
      "namespace N.M {\n}\n",
      "namespace N;\nclass C {\n}\n",

      // type declarations
      "class C {\n}\n",
      "struct S {\n}\n",
      "record R(int X, int Y);\n",
      "record class RC {\n}\n",
      "enum E {\n    A,\n    B,\n    C,\n}\n",
      "enum E : byte {\n    A = 1,\n    B = 2,\n}\n",
      "interface I {\n    void M();\n    int P { get; set; }\n}\n",
      "abstract class A {\n    public abstract void M();\n}\n",

      // fields
      "class C {\n    private int x;\n    public const int Y = 3;\n    static readonly string Z = \"z\";\n}\n",

      // properties
      "class C {\n    public int P { get; set; }\n}\n",
      "class C {\n    public int P { get; private set; }\n}\n",
      "class C {\n    public int P => 42;\n}\n",
      "class C {\n    private int _x;\n    public int P {\n        get { return _x; }\n        set { _x = value; }\n    }\n}\n",
      "class C {\n    public int P { get; set; } = 10;\n}\n",
      "class C {\n    public string Name { get; init; }\n}\n",

      // methods, constructors, local functions
      "class C {\n    public int Add(int a, int b) {\n        return a + b;\n    }\n}\n",
      "class C {\n    public C() {\n    }\n    public C(int x) : this() {\n    }\n}\n",
      "class C {\n    int Compute() => 1 + 2;\n}\n",
      "class C {\n    void M() {\n        int Local(int x) {\n            return x * 2;\n        }\n        Local(3);\n    }\n}\n",

      // attributes
      "[Serializable]\nclass C {\n}\n",
      "class C {\n    [Obsolete]\n    public void M() {\n    }\n}\n",
      "[assembly: AssemblyVersion(\"1.0\")]\n",
      "class C {\n    [Obsolete(\"msg\")]\n    [Serializable]\n    public void M() {\n    }\n}\n",

      // generics + constraints
      "class C<T> where T : class {\n}\n",
      "class C<T, U>\n    where T : class\n    where U : struct {\n}\n",
      "class C {\n    T Identity<T>(T value) where T : new() {\n        return value;\n    }\n}\n",

      // LINQ query expressions
      "using System.Linq;\nclass C {\n    void M() {\n        var q = from x in items\n                where x > 0\n                select x;\n    }\n}\n",
      "using System.Linq;\nclass C {\n    void M() {\n        var q = from x in items\n                orderby x descending\n                group x by x.Key;\n    }\n}\n",

      // lambdas
      "class C {\n    void M() {\n        Func<int, int> f = x => x + 1;\n    }\n}\n",
      "class C {\n    void M() {\n        Action a = () => {\n            System.Console.WriteLine();\n        };\n    }\n}\n",

      // switch expressions + arms
      "class C {\n    int M(int x) {\n        return x switch {\n            1 => 10,\n            2 => 20,\n            _ => 0,\n        };\n    }\n}\n",
      "class C {\n    string M(object o) {\n        return o switch {\n            int i when i > 0 => \"pos\",\n            _ => \"other\",\n        };\n    }\n}\n",

      // pattern matching
      "class C {\n    bool M(object o) {\n        if (o is int i) {\n            return true;\n        }\n        return false;\n    }\n}\n",
      "class C {\n    bool M(object o) {\n        return o is string { Length: > 0 };\n    }\n}\n",

      // switch statement
      "class C {\n    void M(int x) {\n        switch (x) {\n            case 1:\n                break;\n            default:\n                break;\n        }\n    }\n}\n",

      // using statements
      "class C {\n    void M() {\n        using (var r = Get()) {\n            r.Use();\n        }\n    }\n}\n",
      "class C {\n    void M() {\n        using var r = Get();\n        r.Use();\n    }\n}\n",

      // try/catch/finally
      "class C {\n    void M() {\n        try {\n            Do();\n        }\n        catch (Exception e) {\n            Handle(e);\n        }\n        finally {\n            Cleanup();\n        }\n    }\n}\n",
      "class C {\n    void M() {\n        try {\n            Do();\n        }\n        catch {\n        }\n    }\n}\n",

      // loops
      "class C {\n    void M() {\n        foreach (var x in items) {\n            Use(x);\n        }\n    }\n}\n",
      "class C {\n    void M() {\n        for (int i = 0; i < 10; i++) {\n            Use(i);\n        }\n    }\n}\n",
      "class C {\n    void M() {\n        while (Cond()) {\n            Do();\n        }\n    }\n}\n",
      "class C {\n    void M() {\n        do {\n            Do();\n        } while (Cond());\n    }\n}\n",

      // events, delegates, indexers, operators
      "class C {\n    public event System.EventHandler Changed;\n}\n",
      "class C {\n    public event System.EventHandler Changed {\n        add {\n        }\n        remove {\n        }\n    }\n}\n",
      "delegate int MyDelegate(int x);\n",
      "class C {\n    public int this[int i] {\n        get { return i; }\n    }\n}\n",
      "class C {\n    public static C operator +(C a, C b) {\n        return a;\n    }\n}\n",

      // comments
      "// a line comment\nclass C {\n}\n",
      "/* a block\n   comment */\nclass C {\n}\n",
      "class C {\n    // inner comment\n    void M() {\n    }\n}\n",
      "/// <summary>\n/// Doc comment\n/// </summary>\nclass C {\n}\n",

      // string literals
      "class C {\n    string s = @\"line1\nline2\nline3\";\n}\n",
      "class C {\n    void M() {\n        var s = $\"value: {x}\";\n    }\n}\n",
      "class C {\n    void M() {\n        var s = $@\"multi\n{x}\nline\";\n    }\n}\n",
      "class C {\n    string s = \"\"\"\n        raw\n        string\n        \"\"\";\n}\n",

      // no trailing newline
      "class C {\n}",
      "using System;",
      "namespace N {\n    class C {\n    }\n}",
      "class C {\n    public int P => 42;\n}",

      // async / await
      "using System.Threading.Tasks;\nclass C {\n    public async Task M() {\n        await Task.Delay(1);\n    }\n}\n",

      // object & collection initializers
      "class C {\n    void M() {\n        var p = new P {\n            X = 1,\n            Y = 2,\n        };\n    }\n}\n",
      "class C {\n    void M() {\n        var list = new List<int> {\n            1,\n            2,\n            3,\n        };\n    }\n}\n",

      // collection expression (C# 12)
      "class C {\n    int[] xs = [\n        1,\n        2,\n        3,\n    ];\n}\n",

      // ternary / null-coalescing / nullable types
      "class C {\n    int M(bool b) {\n        return b\n            ? 1\n            : 2;\n    }\n}\n",
      "class C {\n    string M(string? s) {\n        return s ?? \"default\";\n    }\n}\n",
      "class C {\n    int? Nullable { get; set; }\n}\n",

      // tuples & deconstruction
      "class C {\n    (int, string) M() {\n        return (1, \"a\");\n    }\n}\n",
      "class C {\n    void M() {\n        var (a, b) = (1, 2);\n    }\n}\n",

      // LINQ join / let / into
      "using System.Linq;\nclass C {\n    void M() {\n        var q = from a in xs\n                join b in ys on a.Id equals b.Id\n                let c = a.Name\n                select c;\n    }\n}\n",

      // static / partial / nested types
      "static class Extensions {\n    public static int Twice(this int x) {\n        return x * 2;\n    }\n}\n",
      "partial class C {\n    partial void M();\n}\n",
      "class Outer {\n    class Inner {\n        int X;\n    }\n}\n",

      // record struct, primary constructor, positional record with body
      "record struct Point(int X, int Y);\n",
      "record Person(string First, string Last) {\n    public string Full => $\"{First} {Last}\";\n}\n",

      // multiple constraints, generic method
      "class Repo<T>\n    where T : class, new() {\n    public T Create() {\n        return new T();\n    }\n}\n",

      // indexer with get and set
      "class C {\n    private int[] _data = new int[10];\n    public int this[int i] {\n        get { return _data[i]; }\n        set { _data[i] = value; }\n    }\n}\n",

      // throw expression, conditional access
      "class C {\n    string M(string s) {\n        return s ?? throw new System.ArgumentNullException();\n    }\n}\n",
      "class C {\n    int? M(string s) {\n        return s?.Length;\n    }\n}\n",

      // switch statement with multiple labels and fallthrough sections
      "class C {\n    string M(int x) {\n        switch (x) {\n            case 1:\n            case 2:\n                return \"low\";\n            case 3:\n                return \"three\";\n            default:\n                return \"other\";\n        }\n    }\n}\n",

      // verbatim + interpolation combos and doc comment on member
      "class C {\n    /// <summary>Adds.</summary>\n    /// <param name=\"a\">first</param>\n    public int Add(int a, int b) {\n        return a + b;\n    }\n}\n",

      // mixed / broader
      "using System;\n\nnamespace App {\n    [Serializable]\n    public class Widget {\n        public int Id { get; set; }\n        public string Name { get; init; }\n\n        public Widget(int id) {\n            Id = id;\n        }\n\n        public override string ToString() => $\"Widget({Id})\";\n    }\n}\n"
    );
}
