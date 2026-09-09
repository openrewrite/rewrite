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
package org.openrewrite.golang;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.golang.rpc.GoRewriteRpc;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TextComment;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@Timeout(value = 120, unit = TimeUnit.SECONDS)
class GolangVisitorCompletenessTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void before() {
        Path binaryPath = Paths.get("build/rewrite-go-rpc").toAbsolutePath();
        GoRewriteRpc.setFactory(GoRewriteRpc.builder()
                .goBinaryPath(binaryPath)
                .log(tempDir.resolve("go-rpc.log")));
    }

    @AfterEach
    void after() {
        GoRewriteRpc.shutdownCurrent();
    }

    static long nl(String s) {
        return s == null ? 0 : s.chars().filter(c -> c == '\n').count();
    }

    long visitorNewlines(SourceFile cu) {
        AtomicInteger n = new AtomicInteger();
        new GolangVisitor<AtomicInteger>() {
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

    void check(String src) {
        SourceFile cu = GolangParser.builder().build()
                .parse(new InMemoryExecutionContext(), src)
                .findFirst().orElseThrow();
        assertThat(cu).as("parse must yield a Go.CompilationUnit for:\n%s", src)
                .isInstanceOf(org.openrewrite.golang.tree.Go.CompilationUnit.class);
        String printed = cu.printAll();
        assertThat(visitorNewlines(cu)).as(() -> "snippet:\n" + src).isEqualTo((int) nl(printed));
    }

    @Test
    void completeness() {
        StringBuilder failures = new StringBuilder();
        for (int i = 0; i < SNIPPETS.size(); i++) {
            String src = SNIPPETS.get(i);
            SourceFile cu = GolangParser.builder().build()
                    .parse(new InMemoryExecutionContext(), src)
                    .findFirst().orElseThrow();
            if (!(cu instanceof org.openrewrite.golang.tree.Go.CompilationUnit)) {
                failures.append("#").append(i).append(" parse error\n").append(src).append("\n---\n");
                continue;
            }
            int expected = (int) nl(cu.printAll());
            int actual = (int) visitorNewlines(cu);
            if (expected != actual) {
                failures.append("#").append(i).append(" expected=").append(expected)
                        .append(" actual=").append(actual).append("\n").append(src).append("\n---\n");
            }
        }
        assertThat(failures.toString()).isEmpty();
    }

    static final List<String> SNIPPETS = List.of(
            // single import
            "package main\n\nimport \"fmt\"\n\nfunc main() {\n\tfmt.Println(\"hi\")\n}\n",
            // grouped imports
            "package main\n\nimport (\n\t\"fmt\"\n\t\"os\"\n)\n\nfunc main() {\n\tfmt.Println(os.Args)\n}\n",
            // grouped imports with blank line separation and aliases
            "package main\n\nimport (\n\t\"fmt\"\n\n\tf \"os\"\n)\n\nfunc main() {\n\tfmt.Println(f.Args)\n}\n",
            // func with params, multiple returns
            "package main\n\nfunc div(a int, b int) (int, error) {\n\treturn a / b, nil\n}\n",
            // variadic\n
            "package main\n\nfunc sum(nums ...int) int {\n\ttotal := 0\n\tfor _, n := range nums {\n\t\ttotal += n\n\t}\n\treturn total\n}\n",
            // generics
            "package main\n\nfunc Map[T any, U any](s []T, f func(T) U) []U {\n\tr := make([]U, len(s))\n\tfor i, v := range s {\n\t\tr[i] = f(v)\n\t}\n\treturn r\n}\n",
            // struct type with tags
            "package main\n\ntype User struct {\n\tName string `json:\"name\"`\n\tAge  int    `json:\"age\"`\n}\n",
            // struct multi-tag
            "package main\n\ntype T struct {\n\tF int `json:\"f\" xml:\"f\"`\n}\n",
            // interface type
            "package main\n\ntype Reader interface {\n\tRead(p []byte) (n int, err error)\n\tClose() error\n}\n",
            // method with receiver
            "package main\n\ntype S struct{}\n\nfunc (s *S) Hello() string {\n\treturn \"hi\"\n}\n",
            // const and var single
            "package main\n\nconst Pi = 3.14\n\nvar name = \"go\"\n",
            // grouped const/var blocks
            "package main\n\nconst (\n\tA = 1\n\tB = 2\n)\n\nvar (\n\tx int\n\ty string\n)\n",
            // iota const block
            "package main\n\nconst (\n\tRed = iota\n\tGreen\n\tBlue\n)\n",
            // type decls grouped
            "package main\n\ntype (\n\tCelsius float64\n\tFahrenheit float64\n)\n",
            // if / else
            "package main\n\nfunc f(x int) string {\n\tif x > 0 {\n\t\treturn \"pos\"\n\t} else if x < 0 {\n\t\treturn \"neg\"\n\t} else {\n\t\treturn \"zero\"\n\t}\n}\n",
            // if with init
            "package main\n\nfunc f(m map[string]int) {\n\tif v, ok := m[\"a\"]; ok {\n\t\t_ = v\n\t}\n}\n",
            // for all forms
            "package main\n\nfunc loops() {\n\tfor i := 0; i < 10; i++ {\n\t}\n\tfor x < 5 {\n\t}\n\tfor {\n\t\tbreak\n\t}\n\tfor k, v := range m {\n\t\t_ = k\n\t\t_ = v\n\t}\n}\n",
            // switch
            "package main\n\nfunc f(x int) {\n\tswitch x {\n\tcase 1:\n\t\treturn\n\tcase 2, 3:\n\t\treturn\n\tdefault:\n\t\treturn\n\t}\n}\n",
            // switch with init
            "package main\n\nfunc f() {\n\tswitch x := g(); x {\n\tcase 1:\n\t\treturn\n\t}\n}\n",
            // type switch
            "package main\n\nfunc f(i interface{}) {\n\tswitch v := i.(type) {\n\tcase int:\n\t\t_ = v\n\tcase string:\n\t\t_ = v\n\tdefault:\n\t}\n}\n",
            // select
            "package main\n\nfunc f(c chan int, d chan int) {\n\tselect {\n\tcase v := <-c:\n\t\t_ = v\n\tcase d <- 1:\n\tdefault:\n\t}\n}\n",
            // defer and go
            "package main\n\nfunc f() {\n\tdefer cleanup()\n\tgo worker()\n}\n",
            // channels
            "package main\n\nfunc f() {\n\tc := make(chan int)\n\tvar r <-chan int = c\n\tvar s chan<- int = c\n\t_ = r\n\t_ = s\n}\n",
            // maps and composite literals
            "package main\n\nfunc f() {\n\tm := map[string]int{\n\t\t\"a\": 1,\n\t\t\"b\": 2,\n\t}\n\t_ = m\n}\n",
            // slices and composite literals with trailing comma
            "package main\n\nfunc f() {\n\ts := []int{\n\t\t1,\n\t\t2,\n\t\t3,\n\t}\n\t_ = s\n}\n",
            // nested composite literal, struct literal trailing comma
            "package main\n\ntype P struct {\n\tX int\n\tY int\n}\n\nfunc f() {\n\tps := []P{\n\t\t{X: 1, Y: 2},\n\t\t{X: 3, Y: 4},\n\t}\n\t_ = ps\n}\n",
            // multi-line function call with trailing comma\n
            "package main\n\nimport \"fmt\"\n\nfunc f() {\n\tfmt.Println(\n\t\t\"a\",\n\t\t\"b\",\n\t\t\"c\",\n\t)\n}\n",
            // line comments own-line and trailing
            "package main\n\n// Package comment\nfunc f() {\n\t// a comment\n\tx := 1 // trailing\n\t_ = x\n}\n",
            // block comments\n
            "package main\n\n/* block\n   comment\n   spanning lines */\nfunc f() {\n\t/* inline */\n\tx := 1\n\t_ = x\n}\n",
            // raw string literal multi-line
            "package main\n\nvar q = `SELECT *\nFROM t\nWHERE x = 1`\n",
            // labeled statements\n
            "package main\n\nfunc f() {\nOuter:\n\tfor {\n\t\tfor {\n\t\t\tbreak Outer\n\t\t}\n\t}\n}\n",
            // goto and labels
            "package main\n\nfunc f() {\n\tgoto End\nEnd:\n\treturn\n}\n",
            // pointer types, address-of, deref
            "package main\n\nfunc f(p *int) {\n\tx := *p\n\tq := &x\n\t_ = q\n}\n",
            // slice expressions
            "package main\n\nfunc f(s []int) {\n\t_ = s[1:3]\n\t_ = s[1:3:5]\n\t_ = s[:2]\n\t_ = s[2:]\n}\n",
            // fixed size array\n
            "package main\n\nvar a [5]int\nvar b [3]string\n",
            // generic type with constraints union
            "package main\n\ntype Number interface {\n\t~int | ~float64\n}\n\nfunc Sum[T Number](xs []T) T {\n\tvar s T\n\tfor _, x := range xs {\n\t\ts += x\n\t}\n\treturn s\n}\n",
            // func type field\n
            "package main\n\ntype Handler struct {\n\tfn func(int) error\n}\n",
            // no trailing newline
            "package main\n\nfunc main() {\n}",
            // multiple top-level decls, mixed
            "package main\n\nimport (\n\t\"fmt\"\n\t\"strings\"\n)\n\nconst greeting = \"hello\"\n\ntype Greeter struct {\n\tprefix string\n}\n\nfunc (g Greeter) Greet(name string) string {\n\treturn fmt.Sprintf(\"%s %s\", g.prefix, strings.ToUpper(name))\n}\n\nfunc main() {\n\tg := Greeter{prefix: greeting}\n\tfmt.Println(g.Greet(\"world\"))\n}\n",
            // go directive comment
            "package main\n\n//go:noinline\nfunc f() {\n}\n",
            // send/receive on channel statements
            "package main\n\nfunc f(c chan int) {\n\tc <- 1\n\tx := <-c\n\t_ = x\n}\n",
            // embedded struct fields
            "package main\n\ntype Base struct {\n\tID int\n}\n\ntype Derived struct {\n\tBase\n\tName string\n}\n",
            // anonymous struct\n
            "package main\n\nfunc f() {\n\tp := struct {\n\t\tX int\n\t\tY int\n\t}{X: 1, Y: 2}\n\t_ = p\n}\n",
            // multi-line param list with trailing comma
            "package main\n\nfunc f(\n\ta int,\n\tb int,\n) int {\n\treturn a + b\n}\n",
            // multi-line generic type params with trailing comma
            "package main\n\nfunc g[\n\tT any,\n\tU any,\n](t T, u U) {\n}\n",
            // multi-line call with trailing comma across nested composite
            "package main\n\ntype P struct{ X int }\n\nfunc f() {\n\tps := []P{\n\t\t{\n\t\t\tX: 1,\n\t\t},\n\t\t{\n\t\t\tX: 2,\n\t\t},\n\t}\n\t_ = ps\n}\n"
    );
}
