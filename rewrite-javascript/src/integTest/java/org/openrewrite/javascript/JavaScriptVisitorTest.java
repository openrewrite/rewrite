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
package org.openrewrite.javascript;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Tree;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TextComment;
import org.openrewrite.javascript.tree.JS;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RewriteTest;

import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.javascript.Assertions.javascript;
import static org.openrewrite.javascript.Assertions.typescript;
import static org.openrewrite.test.RewriteTest.toRecipe;

/**
 * Every newline the JavaScript printer emits must be reachable by an in-process {@link JavaScriptVisitor}
 * walk (whitespace, comments, and literal value sources), otherwise LST-walking tools such as line counters
 * silently undercount. Regression coverage for the {@code ExpressionStatement}/{@code StatementExpression}
 * prefix and {@code TrailingComma} suffix gaps.
 * <p>
 * {@code rewriteRun} already verifies the source round-trips (parse+print is idempotent), so the visitor's
 * newline count must equal the newline count of the source itself.
 */
class JavaScriptVisitorTest implements RewriteTest {

    static long nl(String s) {
        return s == null ? 0 : s.chars().filter(c -> c == '\n').count();
    }

    void assertVisitorSeesEveryNewline(String src) {
        rewriteRun(javascript(src, spec -> spec.afterRecipe(cu -> {
            AtomicInteger n = new AtomicInteger();
            new JavaScriptVisitor<AtomicInteger>() {
                @Override
                public Space visitSpace(Space space, Space.Location loc, AtomicInteger a) {
                    a.addAndGet((int) nl(space.getWhitespace()));
                    for (Comment c : space.getComments()) {
                        a.addAndGet((int) (c instanceof TextComment ? nl(((TextComment) c).getText()) : nl(c.printComment(getCursor()))));
                        a.addAndGet((int) nl(c.getSuffix()));
                    }
                    return super.visitSpace(space, loc, a);
                }

                @Override
                public J visitLiteral(J.Literal literal, AtomicInteger a) {
                    a.addAndGet((int) nl(literal.getValueSource()));
                    return super.visitLiteral(literal, a);
                }
            }.visit(cu, n);
            assertThat((long) n.get())
                    .as(() -> "visitor missed newlines the printer emits for:\n" + src)
                    .isEqualTo(nl(src));
        })));
    }

    @Test
    void directiveProloguePrefix() {
        assertVisitorSeesEveryNewline("function f() {\n    'use strict';\n    foo();\n}");
        assertVisitorSeesEveryNewline("(function () {\n    'use strict';\n    foo();\n})();");
    }

    @Test
    void trailingCommaSuffix() {
        assertVisitorSeesEveryNewline("const a = [\n    1,\n    2,\n];");
        assertVisitorSeesEveryNewline("const o = {\n    a: 1,\n    b: 2,\n};");
        assertVisitorSeesEveryNewline("g(\n    1,\n    2,\n);");
        assertVisitorSeesEveryNewline("import {\n    a,\n    b,\n} from './m';");
    }

    @Test
    void commentsClassesAndTemplates() {
        assertVisitorSeesEveryNewline("// header\n/* block\n   comment */\nclass C {\n    m() {\n        return 1; // trailing\n    }\n}");
        assertVisitorSeesEveryNewline("const s = `line one\nline two\nline three`;");
    }

    @Test
    void deleteOperandIsVisited() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.Identifier visitIdentifier(J.Identifier identifier, ExecutionContext ctx) {
                  J.Identifier i = super.visitIdentifier(identifier, ctx);
                  return "obj".equals(i.getSimpleName()) ? i.withSimpleName("o2") : i;
              }
          })),
          javascript(
            """
              const obj = {a: 1};
              delete obj.a;
              """,
            """
              const o2 = {a: 1};
              delete o2.a;
              """
          )
        );
    }

    @Test
    void deleteIsVisitedAsStatement() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public Statement visitStatement(Statement statement, ExecutionContext ctx) {
                  Statement s = super.visitStatement(statement, ctx);
                  return s instanceof JS.Delete ? SearchResult.found(s) : s;
              }
          })),
          javascript(
            """
              const obj = {a: 1};
              delete obj.a;
              """,
            """
              const obj = {a: 1};
              /*~~>*/delete obj.a;
              """
          )
        );
    }

    @Test
    void forOfLoopAwaitSpaceIsVisited() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaScriptIsoVisitor<>() {
              @Override
              public Space visitSpace(Space space, Space.Location loc, ExecutionContext ctx) {
                  Space s = super.visitSpace(space, loc, ctx);
                  return s.withComments(ListUtils.map(s.getComments(), c ->
                    c instanceof TextComment && " c ".equals(((TextComment) c).getText()) ? ((TextComment) c).withText(" d ") : c));
              }
          })),
          typescript(
            "async function f(xs: AsyncIterable<number>) { for /* c */ await (const x of xs) {} }",
            "async function f(xs: AsyncIterable<number>) { for /* d */ await (const x of xs) {} }"
          )
        );
    }

    @Test
    void visitsTheTypeOfEveryJsNode() {
        rewriteRun(typescript(TYPED_NODES, spec -> spec.afterRecipe(cu -> {
            List<Tree> typedNodes = new ArrayList<>();
            Set<Tree> typeVisited = Collections.newSetFromMap(new IdentityHashMap<>());
            new JavaScriptVisitor<Integer>() {
                @Override
                public J preVisit(J tree, Integer p) {
                    if (tree instanceof JS && declaresType(tree.getClass())) {
                        typedNodes.add(tree);
                    }
                    return tree;
                }

                @Override
                public @Nullable JavaType visitType(@Nullable JavaType javaType, Integer p) {
                    typeVisited.add(getCursor().getValue());
                    return javaType;
                }
            }.visit(cu, 0);

            assertThat(typedNodes).extracting(Object::getClass)
              .containsAll(nestedClasses(JS.class).filter(JavaScriptVisitorTest::declaresType).toList());
            assertThat(typedNodes).filteredOn(t -> !typeVisited.contains(t))
              .extracting(t -> t.getClass().getSimpleName())
              .isEmpty();
        })));
    }

    private static Stream<Class<?>> nestedClasses(Class<?> c) {
        return Arrays.stream(c.getDeclaredClasses()).flatMap(n -> Stream.concat(Stream.of(n), nestedClasses(n)));
    }

    private static boolean declaresType(Class<?> c) {
        return Arrays.stream(c.getDeclaredFields())
          .anyMatch(f -> !Modifier.isStatic(f.getModifiers()) && JavaType.class.isAssignableFrom(f.getType()));
    }

    static final String TYPED_NODES = """
      import { a as a1, b } from "m";
      export { a1 as c };
      type M = import("m");
      type L = "lit";
      type Tu = [number, string];
      type O = { a: number };
      type U = string | number;
      type I = O & { b: string };
      type Q = typeof b;
      type V = O["a"];
      type Mp<T> = { [K in keyof T]: T[K] };
      type C<T> = T extends Array<infer E> ? E : never;
      type F = (x: number) => string;
      interface Ix { [k: string]: number }
      class Base<T> {}
      class D extends Base<string> {
          [Symbol.iterator]() { return [][Symbol.iterator](); }
      }
      function isS(x: unknown): x is string { return typeof x === "string"; }
      function tag(s: TemplateStringsArray) { return s; }
      async function g(p: Promise<number>, xs: number[], fs: (() => void)[], n?: number) {
          const v = await p;
          const s = p as unknown;
          let m = n;
          m ??= 1;
          const [x, y] = xs;
          const { length } = xs;
          const z = [...xs];
          const t = `a${v}`;
          const tt = tag`b`;
          const sat = {} satisfies object;
          fs[0]();
          const eq = x === y;
      }
      """;
}
