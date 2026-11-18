/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.trait;

import org.junit.jupiter.api.Test;
import org.openrewrite.Cursor;
import org.openrewrite.DocumentExample;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.service.ScopeService;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RewriteTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

class ScopeTest implements RewriteTest {

    @DocumentExample
    @Test
    void localIsVisibleFromItsDeclarationOn() {
        rewriteRun(
          spec -> spec.recipe(visibleAtHere("closed", "before", "after", "p")),
          java(
            """
              class A {
                  void m(int p) {
                      { int closed = 0; }
                      int before = 0;
                      here();
                      int after = 0;
                  }
                  void here() {}
              }
              """,
            """
              class A {
                  void m(int p) {
                      { int closed = 0; }
                      int before = 0;
                      /*~~(before, p)~~>*/here();
                      int after = 0;
                  }
                  void here() {}
              }
              """
          )
        );
    }

    @Test
    void visibleListsTheBindingsInScopeInnermostFirst() {
        rewriteRun(
          spec -> spec.recipe(atHere(scope -> names(scope.visible()))),
          java(
            """
              class A {
                  int x;
                  int y;
                  void m(int p) {
                      int x = 0;
                      here();
                  }
                  void here() {}
              }
              """,
            """
              class A {
                  int x;
                  int y;
                  void m(int p) {
                      int x = 0;
                      /*~~(x p y)~~>*/here();
                  }
                  void here() {}
              }
              """
          )
        );
    }

    @Test
    void membersImportsAndInheritedFieldsAreVisibleThroughoutTheClass() {
        rewriteRun(
          spec -> spec.recipe(visibleAtHere("later", "inherited", "hidden", "CONSTANT", "E", "PI")),
          java(
            """
              class B {
                  protected int inherited;
                  private int hidden;
              }
              """
          ),
          java(
            """
              interface I {
                  int CONSTANT = 1;
              }
              """
          ),
          java(
            """
              import static java.lang.Math.*;

              class A extends B implements I {
                  void m() {
                      here();
                  }
                  void here() {}
                  int later;
              }
              """,
            """
              import static java.lang.Math.*;

              class A extends B implements I {
                  void m() {
                      /*~~(later, inherited, CONSTANT, E, PI)~~>*/here();
                  }
                  void here() {}
                  int later;
              }
              """
          )
        );
    }

    @Test
    void loopTryCatchAndLambdaVariablesReachOnlyTheirOwnPart() {
        rewriteRun(
          spec -> spec.recipe(visibleAtHere("s", "i", "r", "e", "x", "list", "in")),
          java(
            """
              import java.io.*;
              import java.util.List;

              class A {
                  void m(List<String> list, Reader in) throws IOException {
                      for (String s : list.subList(0, here())) {
                          here();
                      }
                      for (int i = 0; i < here(); i++) {
                      }
                      try (Reader r = in) {
                          here();
                      } catch (IOException e) {
                          here();
                      }
                      list.forEach(x -> here());
                  }
                  int here() { return 0; }
              }
              """,
            """
              import java.io.*;
              import java.util.List;

              class A {
                  void m(List<String> list, Reader in) throws IOException {
                      for (String s : list.subList(0, /*~~(list, in)~~>*/here())) {
                          /*~~(s, list, in)~~>*/here();
                      }
                      for (int i = 0; i < /*~~(i, list, in)~~>*/here(); i++) {
                      }
                      try (Reader r = in) {
                          /*~~(r, list, in)~~>*/here();
                      } catch (IOException e) {
                          /*~~(e, list, in)~~>*/here();
                      }
                      list.forEach(x -> /*~~(x, list, in)~~>*/here());
                  }
                  int here() { return 0; }
              }
              """
          )
        );
    }

    @Test
    void switchCasesShareTheirBlockButPatternsStayInTheirCase() {
        rewriteRun(
          spec -> spec.recipe(visibleAtHere("shared", "inner", "s", "n")),
          java(
            """
              class A {
                  void m(Object o, int k) {
                      switch (k) {
                          case 1:
                              int shared = 0;
                              break;
                          case 2:
                              shared = 1;
                              here();
                              break;
                      }
                      switch (o) {
                          case String s when s.isEmpty() -> {
                              int inner = 0;
                              here();
                          }
                          case Integer n -> here();
                          default -> here();
                      }
                  }
                  void here() {}
              }
              """,
            """
              class A {
                  void m(Object o, int k) {
                      switch (k) {
                          case 1:
                              int shared = 0;
                              break;
                          case 2:
                              shared = 1;
                              /*~~(shared)~~>*/here();
                              break;
                      }
                      switch (o) {
                          case String s when s.isEmpty() -> {
                              int inner = 0;
                              /*~~(inner, s)~~>*/here();
                          }
                          case Integer n -> /*~~(n)~~>*/here();
                          default -> /*~~(none)~~>*/here();
                      }
                  }
                  void here() {}
              }
              """
          )
        );
    }

    @Test
    void patternVariablesFollowFlowScoping() {
        rewriteRun(
          spec -> spec.recipe(visibleAtHere("a", "b", "d", "e", "f", "g", "h", "i", "j")),
          java(
            """
              class A {
                  void m(Object o, boolean c) {
                      if (o instanceof String a) { here(); }
                      if (!(o instanceof String b)) { return; }
                      here();
                      if (!(o instanceof Integer d)) { use(); }
                      here();
                      if (o instanceof Long e && here() > 0) { }
                      if (o instanceof Short f || here() > 0) { }
                      if (!(o instanceof Byte g)) { while (c) { return; } }
                      here();
                      while (!(o instanceof Double h)) { o = next(); }
                      here();
                      if (o instanceof Float i) { } else { return; }
                      here();
                      if (!(o instanceof Character j)) { here(); } else { here(); }
                  }
                  int here() { return 0; }
                  void use() {}
                  Object next() { return null; }
              }
              """,
            """
              class A {
                  void m(Object o, boolean c) {
                      if (o instanceof String a) { /*~~(a)~~>*/here(); }
                      if (!(o instanceof String b)) { return; }
                      /*~~(b)~~>*/here();
                      if (!(o instanceof Integer d)) { use(); }
                      /*~~(b)~~>*/here();
                      if (o instanceof Long e && /*~~(b, e)~~>*/here() > 0) { }
                      if (o instanceof Short f || /*~~(b)~~>*/here() > 0) { }
                      if (!(o instanceof Byte g)) { while (c) { return; } }
                      /*~~(b)~~>*/here();
                      while (!(o instanceof Double h)) { o = next(); }
                      /*~~(b, h)~~>*/here();
                      if (o instanceof Float i) { } else { return; }
                      /*~~(b, h, i)~~>*/here();
                      if (!(o instanceof Character j)) { /*~~(b, h, i)~~>*/here(); } else { /*~~(b, h, i, j)~~>*/here(); }
                  }
                  int here() { return 0; }
                  void use() {}
                  Object next() { return null; }
              }
              """
          )
        );
    }

    @Test
    void bindingKnowsItsReferencesAndWhetherItIsEffectivelyFinal() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.Identifier visitIdentifier(J.Identifier identifier, ExecutionContext ctx) {
                  return new Scope.Binding.Matcher().get(getCursor())
                    .filter(b -> b.getNamespace() == Scope.Namespace.VARIABLE)
                    .map(b -> SearchResult.found(identifier, b.references().count() + " references, " +
                                                             (b.isEffectivelyFinal() ? "effectively final" : "not effectively final") +
                                                             (b.isMember() ? ", member" : "")))
                    .orElse(identifier);
              }
          })),
          java(
            """
              class A {
                  int n;
                  void m(A other) {
                      { int n = 0; n++; }
                      { int n = 0; use(n); other.n = 1; n(); }
                      int k = 0;
                      k += 1;
                      String s;
                      s = "x";
                      String t;
                      t = "a";
                      t = "b";
                  }
                  void n() {}
                  void use(int i) {}
                  void p() { this.n = 2; }
              }
              """,
            """
              class A {
                  int /*~~(2 references, not effectively final, member)~~>*/n;
                  void m(A /*~~(1 references, effectively final)~~>*/other) {
                      { int /*~~(1 references, not effectively final)~~>*/n = 0; n++; }
                      { int /*~~(1 references, effectively final)~~>*/n = 0; use(n); other.n = 1; n(); }
                      int /*~~(1 references, not effectively final)~~>*/k = 0;
                      k += 1;
                      String /*~~(1 references, effectively final)~~>*/s;
                      s = "x";
                      String /*~~(2 references, not effectively final)~~>*/t;
                      t = "a";
                      t = "b";
                  }
                  void n() {}
                  void use(int /*~~(0 references, effectively final)~~>*/i) {}
                  void p() { this.n = 2; }
              }
              """
          )
        );
    }

    @Test
    void methodsAndTypesAreLookedUpInTheirOwnNamespace() {
        rewriteRun(
          spec -> spec.recipe(atHere(scope -> visible(scope, Scope.Namespace.TYPE, "T", "Inner", "Local", "List", "A", "B") + " | " +
                                              visible(scope, Scope.Namespace.METHOD, "m", "n", "here", "value", "made") + " | " +
                                              visible(scope, Scope.Namespace.VARIABLE, "list", "value"))),
          java(
            """
              import java.util.List;

              class A<T> {
                  int value;
                  class Inner {}
                  void m(List<T> list) {
                      class Local {}
                      here();
                  }
                  void here() {}
              }
              class B extends A<String> {
                  void n() {
                      here();
                  }
                  void value() {}
                  Inner made() { return null; }
              }
              """,
            """
              import java.util.List;

              class A<T> {
                  int value;
                  class Inner {}
                  void m(List<T> list) {
                      class Local {}
                      /*~~(T Inner Local List A B | m here | list value)~~>*/here();
                  }
                  void here() {}
              }
              class B extends A<String> {
                  void n() {
                      /*~~(Inner List A B | m n here value made | value)~~>*/here();
                  }
                  void value() {}
                  Inner made() { return null; }
              }
              """
          )
        );
    }

    @Test
    void referencesWithinARegionAnswerForEveryBindingAtOnce() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                  Scope.References references = Scope.referencesWithin(getCursor());
                  String facts = Scope.declaredWithin(getCursor())
                    .map(b -> b.getName() + ":" + references.of(b).count() + (references.isEffectivelyFinal(b) ? "" : "!"))
                    .collect(joining(" "));
                  return SearchResult.found(method, facts);
              }
          })),
          java(
            """
              class A {
                  int f;
                  void m(int p) {
                      { int n = p; n++; }
                      { int n = p; this.f = n + n; }
                  }
              }
              """,
            """
              class A {
                  int f;
                  /*~~(m:0 p:2 n:1! n:2)~~>*/void m(int p) {
                      { int n = p; n++; }
                      { int n = p; this.f = n + n; }
                  }
              }
              """
          )
        );
    }

    @Test
    void methodAndTypeReferencesIncludeSelectionsFromThis() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.Identifier visitIdentifier(J.Identifier identifier, ExecutionContext ctx) {
                  return new Scope.Binding.Matcher().get(getCursor())
                    .map(b -> SearchResult.found(identifier, b.references().count() + " references"))
                    .orElse(identifier);
              }
          })),
          java(
            """
              class A {
                  void run() { run(); this.run(); new A().run(); }
                  A self(A other) { return other; }
              }
              """,
            """
              class /*~~(3 references)~~>*/A {
                  void /*~~(2 references)~~>*/run() { run(); this.run(); new A().run(); }
                  A /*~~(0 references)~~>*/self(A /*~~(1 references)~~>*/other) { return other; }
              }
              """
          )
        );
    }

    @Test
    void statementCompletesNormallyUnlessEveryPathLeaves() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                  J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);
                  if (m.getBody() == null || m.getBody().getStatements().isEmpty()) {
                      return m;
                  }
                  Statement statement = m.getBody().getStatements().getFirst();
                  boolean completes = new ScopeService().completesNormally(statement);
                  return m.withBody(m.getBody().withStatements(ListUtils.mapFirst(m.getBody().getStatements(),
                    s -> SearchResult.found(s, completes ? "completes" : "leaves"))));
              }
          })),
          java(
            """
              class A {
                  void a(boolean c) { if (c) return; else throw new RuntimeException(); }
                  void b(boolean c) { while (c) return; }
                  void c() { while (true) { } }
                  void d() { L: while (true) { break L; } }
                  void e() { System.exit(1); }
                  void f() { try { return; } finally { } }
              }
              """,
            """
              class A {
                  void a(boolean c) { /*~~(leaves)~~>*/if (c) return; else throw new RuntimeException(); }
                  void b(boolean c) { /*~~(completes)~~>*/while (c) return; }
                  void c() { /*~~(leaves)~~>*/while (true) { } }
                  void d() { /*~~(completes)~~>*/L: while (true) { break L; } }
                  void e() { /*~~(completes)~~>*/System.exit(1); }
                  void f() { /*~~(leaves)~~>*/try { return; } finally { } }
              }
              """
          )
        );
    }

    @Test
    void freeNameAvoidsWhatItWouldCollideWithOrCapture() {
        rewriteRun(
          spec -> spec.recipe(atHere(scope -> Arrays.stream(new String[]{"x", "size", "f", "l", "y", "class"})
            .map(n -> n + "=" + scope.freeName(n, "y"::equals))
            .collect(joining(", ")))),
          java(
            """
              import java.util.*;

              class A {
                  int f;
                  void m(List<String> l, Map<String, String> map) {
                      here();
                      int x = map.size();
                      l.forEach(x1 -> {});
                  }
                  void here() {}
              }
              """,
            """
              import java.util.*;

              class A {
                  int f;
                  void m(List<String> l, Map<String, String> map) {
                      /*~~(x=x2, size=size, f=f1, l=l1, y=y1, class=class1)~~>*/here();
                      int x = map.size();
                      l.forEach(x1 -> {});
                  }
                  void here() {}
              }
              """
          )
        );
    }

    @Test
    void bindingsDeclaredAndReadWithinARegion() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.Block visitBlock(J.Block block, ExecutionContext ctx) {
                  J.Block b = super.visitBlock(block, ctx);
                  if (!(getCursor().getParentTreeCursor().getValue() instanceof J.Block)) {
                      return b;
                  }
                  return SearchResult.found(b, "declared " + names(Scope.declaredWithin(getCursor())) +
                                               ", read " + names(Scope.readWithin(getCursor())));
              }
          })),
          java(
            """
              class A {
                  int field;
                  void m(int outer) {
                      {
                          int a = outer + field;
                          if (Integer.valueOf(a) instanceof Integer s) { use(s); }
                          Runnable r = () -> { int b = a; };
                      }
                  }
                  void use(int i) {}
              }
              """,
            """
              class A {
                  int field;
                  void m(int outer) {
                      /*~~(declared a s r b, read outer field)~~>*/{
                          int a = outer + field;
                          if (Integer.valueOf(a) instanceof Integer s) { use(s); }
                          Runnable r = () -> { int b = a; };
                      }
                  }
                  void use(int i) {}
              }
              """
          )
        );
    }

    @Test
    void declaredWithinAnEditedCopyComeFromTheCopy() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.If visitIf(J.If iff, ExecutionContext ctx) {
                  J.If i = super.visitIf(iff, ctx);
                  J.ClassDeclaration c = getCursor().firstEnclosingOrThrow(J.ClassDeclaration.class);
                  J.MethodDeclaration other = (J.MethodDeclaration) c.getBody().getStatements().get(1);
                  if (getCursor().firstEnclosingOrThrow(J.MethodDeclaration.class) == other) {
                      return i;
                  }
                  // A recipe mid-visit holds an edited copy its ancestors do not have yet.
                  J.If edited = (J.If) other.getBody().getStatements().getFirst();
                  Cursor copy = new Cursor(getCursor().getParentOrThrow(), edited);
                  return SearchResult.found(i, "declared " + names(Scope.declaredWithin(copy)));
              }
          })),
          java(
            """
              class A {
                  void m(Object o) {
                      if (o instanceof String a) { }
                  }
                  void other(Object o) {
                      if (o instanceof Integer moved) { }
                  }
              }
              """,
            """
              class A {
                  void m(Object o) {
                      /*~~(declared moved)~~>*/if (o instanceof String a) { }
                  }
                  void other(Object o) {
                      if (o instanceof Integer moved) { }
                  }
              }
              """
          )
        );
    }

    @Test
    void blockCollidesAcrossLaterStatementsThatDeclareOrReadItsNames() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.Block visitBlock(J.Block block, ExecutionContext ctx) {
                  J.Block b = super.visitBlock(block, ctx);
                  Cursor parent = getCursor().getParentTreeCursor();
                  if (!(parent.getValue() instanceof J.Block)) {
                      return b;
                  }
                  List<Statement> statements = parent.<J.Block>getValue().getStatements();
                  List<Cursor> later = new ArrayList<>();
                  for (Statement statement : statements.subList(statements.indexOf(block) + 1, statements.size())) {
                      later.add(new Cursor(parent, statement));
                  }
                  return SearchResult.found(b, Scope.at(getCursor()).collidesAcross(later) ? "collides" : "free");
              }
          })),
          java(
            """
              class A {
                  int field;
                  void declaredLater() {
                      { int x = 1; }
                      for (int x = 0; x < 1; x++) { }
                  }
                  void readLater() {
                      { int field = 1; }
                      use(field);
                  }
                  void untouched() {
                      { int y = 1; }
                      use(field);
                  }
                  void localClass() {
                      { class Helper {} }
                      class Helper {}
                  }
                  void use(int i) {}
              }
              """,
            """
              class A {
                  int field;
                  void declaredLater() {
                      /*~~(collides)~~>*/{ int x = 1; }
                      for (int x = 0; x < 1; x++) { }
                  }
                  void readLater() {
                      /*~~(collides)~~>*/{ int field = 1; }
                      use(field);
                  }
                  void untouched() {
                      /*~~(free)~~>*/{ int y = 1; }
                      use(field);
                  }
                  void localClass() {
                      /*~~(collides)~~>*/{ class Helper {} }
                      class Helper {}
                  }
                  void use(int i) {}
              }
              """
          )
        );
    }

    @Test
    void matcherFindsTheScopesThatDeclareAName() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new Scope.Matcher().declaring("x")
            .asVisitor(scope -> SearchResult.found(scope.getTree())))),
          java(
            """
              import java.util.function.IntUnaryOperator;

              class A {
                  int x;
                  void m(int y) {
                      int x = y;
                      IntUnaryOperator f = z -> { int x2 = z; return x2; };
                  }
                  void n(int x) {}
              }
              """,
            """
              import java.util.function.IntUnaryOperator;

              /*~~>*/class A {
                  int x;
                  void m(int y) /*~~>*/{
                      int x = y;
                      IntUnaryOperator f = z -> { int x2 = z; return x2; };
                  }
                  /*~~>*/void n(int x) {}
              }
              """
          )
        );
    }

    private static String names(Stream<Scope.Binding> bindings) {
        return bindings.filter(b -> b.getNamespace() == Scope.Namespace.VARIABLE).map(Scope.Binding::getName).collect(joining(" "));
    }

    private static String visible(Scope scope, Scope.Namespace namespace, String... candidates) {
        return Arrays.stream(candidates)
          .filter(name -> scope.lookup(name, namespace).isPresent())
          .collect(joining(" "));
    }

    private static Recipe visibleAtHere(String... candidates) {
        return atHere(scope -> {
            String visible = Arrays.stream(candidates)
              .filter(name -> scope.lookup(name).isPresent())
              .collect(joining(", "));
            return visible.isEmpty() ? "none" : visible;
        });
    }

    private static Recipe atHere(Function<Scope, String> question) {
        return toRecipe(() -> new JavaIsoVisitor<>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                return "here".equals(m.getSimpleName()) ? SearchResult.found(m, question.apply(Scope.at(getCursor()))) : m;
            }
        });
    }
}
