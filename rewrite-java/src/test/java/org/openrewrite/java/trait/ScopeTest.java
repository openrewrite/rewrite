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
import org.openrewrite.java.tree.Space;
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
    void anonymousClassInheritsWhatItsOwnPackageCanAccess() {
        rewriteRun(
          spec -> spec.recipe(visibleAtHere("packagePrivate", "inherited")),
          java(
            """
              package a;

              public class Base {
                  int packagePrivate;
                  protected int inherited;
              }
              """
          ),
          java(
            """
              package b;

              import a.Base;

              class A {
                  void m() {
                      new Base() {
                          void n() {
                              here();
                          }
                      };
                  }
                  void here() {}
              }
              """,
            """
              package b;

              import a.Base;

              class A {
                  void m() {
                      new Base() {
                          void n() {
                              /*~~(inherited)~~>*/here();
                          }
                      };
                  }
                  void here() {}
              }
              """
          )
        );
    }

    @Test
    void staticImportOfANameShadowsAnOnDemandOne() {
        rewriteRun(
          spec -> spec.recipe(atHere(scope -> scope.lookup("max", Scope.Namespace.METHOD)
            .map(binding -> ((J.Import) binding.getTree()).getTypeName())
            .orElse("none"))),
          java(
            """
              import static java.lang.Math.*;
              import static java.lang.StrictMath.max;

              class A {
                  void m() {
                      here();
                  }
                  void here() {}
              }
              """,
            """
              import static java.lang.Math.*;
              import static java.lang.StrictMath.max;

              class A {
                  void m() {
                      /*~~(java.lang.StrictMath)~~>*/here();
                  }
                  void here() {}
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
          spec -> spec.recipe(visibleAtHere("shared", "inner", "s", "n", "guarded")),
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
                          case Object any when any instanceof Long guarded -> here();
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
                          case Object any when any instanceof Long guarded -> /*~~(guarded)~~>*/here();
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
    void superSelectsTheInheritedMemberAndNotTheOneRedeclared() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.Identifier visitIdentifier(J.Identifier identifier, ExecutionContext ctx) {
                  return Scope.bindingOf(getCursor(), identifier)
                    .filter(b -> b.getName().endsWith("_"))
                    .map(b -> SearchResult.found(identifier, b.references().count() + " references"))
                    .orElse(identifier);
              }
          })),
          java(
            """
              class Base {
                  int x_;
                  void m_() {}
              }
              class Sub extends Base {
                  int x_;
                  void m_() {
                      super.x_ = this.x_;
                      super.m_();
                  }
              }
              """,
            """
              class Base {
                  int /*~~(0 references)~~>*/x_;
                  void /*~~(0 references)~~>*/m_() {}
              }
              class Sub extends Base {
                  int /*~~(1 references)~~>*/x_;
                  void /*~~(0 references)~~>*/m_() {
                      super.x_ = this.x_;
                      super.m_();
                  }
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
                  return Scope.bindingOf(getCursor(), identifier)
                    .filter(b -> b.getNamespace() == Scope.Namespace.VARIABLE)
                    .map(b -> SearchResult.found(identifier, b.references().count() + " references, " +
                                                             (b.isEffectivelyFinal() ? "effectively final" : "not effectively final") +
                                                             (b.isLocal() ? "" : ", member")))
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
    void referencesIncludeArrayElementsAndAnAnnotationDefault() {
        rewriteRun(
          spec -> spec.recipe(referenceCounts()),
          java(
            """
              @interface Limit {
                  int MAX = 1;
                  int value() default MAX;
              }
              class A {
                  int[] m(int p) {
                      int[] braces = {p};
                      return new int[]{p};
                  }
              }
              """,
            """
              @interface Limit {
                  int /*~~(1 references)~~>*/MAX = 1;
                  int value() default MAX;
              }
              class A {
                  int[] m(int /*~~(2 references)~~>*/p) {
                      int[] /*~~(0 references)~~>*/braces = {p};
                      return new int[]{p};
                  }
              }
              """
          )
        );
    }

    @Test
    void enumConstantInACaseLabelIsNotLookedUpInScope() {
        rewriteRun(
          spec -> spec.recipe(referenceCounts()),
          java(
            """
              enum Level {
                  LOW;
                  int rank(Level level, int LOW) {
                      switch (level) {
                          case LOW:
                              return LOW;
                          default:
                              return 0;
                      }
                  }
              }
              """,
            """
              enum Level {
                  /*~~(1 references)~~>*/LOW;
                  int rank(Level /*~~(1 references)~~>*/level, int /*~~(1 references)~~>*/LOW) {
                      switch (level) {
                          case LOW:
                              return LOW;
                          default:
                              return 0;
                      }
                  }
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
    void referencesOfABindingAllOrOnlyThoseInARegion() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                  Cursor lastBlock = new Cursor(new Cursor(getCursor(), method.getBody()), method.getBody().getStatements().get(1));
                  String facts = Scope.declaredWithin(getCursor())
                    .map(b -> b.getName() + ":" + b.references().count() + "/" + b.references(lastBlock).count() +
                              (b.isEffectivelyFinal() ? "" : "!"))
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
                  /*~~(m:0/0 p:2/1 n:1/0! n:2/2)~~>*/void m(int p) {
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
                  return Scope.bindingOf(getCursor(), identifier)
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
                  void g(boolean c) { do { if (c) continue; return; } while (c); }
                  void h(boolean c) { L: do { while (c) continue L; return; } while (c); }
                  void i(boolean c) { do { while (c) continue; return; } while (c); }
                  void j() { do { continue; } while (true); }
                  void k(boolean c) { M: do { L: while (c) continue L; return; } while (c); }
                  void l(boolean c) { do { try { continue; } finally { return; } } while (c); }
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
                  void g(boolean c) { /*~~(completes)~~>*/do { if (c) continue; return; } while (c); }
                  void h(boolean c) { /*~~(completes)~~>*/L: do { while (c) continue L; return; } while (c); }
                  void i(boolean c) { /*~~(leaves)~~>*/do { while (c) continue; return; } while (c); }
                  void j() { /*~~(leaves)~~>*/do { continue; } while (true); }
                  void k(boolean c) { /*~~(leaves)~~>*/M: do { L: while (c) continue L; return; } while (c); }
                  void l(boolean c) { /*~~(leaves)~~>*/do { try { continue; } finally { return; } } while (c); }
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
                                               ", read " + names(Scope.referencedWithin(getCursor())));
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
    void declaredWithinAStatementBindsInTheScopeAroundIt() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                  return SearchResult.found(multiVariable, Scope.declaredWithin(getCursor())
                    .map(b -> b.getName() + " has " + b.references().count() + " references")
                    .collect(joining(", ")));
              }
          })),
          java(
            """
              class A {
                  int m() {
                      int a = 1;
                      return a + a;
                  }
              }
              """,
            """
              class A {
                  int m() {
                      /*~~(a has 2 references)~~>*/int a = 1;
                      return a + a;
                  }
              }
              """
          )
        );
    }

    @Test
    void bindingsTellWhatKindOfNameTheyAre() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                  if (!"here".equals(m.getSimpleName())) {
                      return m;
                  }
                  Scope scope = Scope.at(getCursor());
                  return SearchResult.found(m, Stream.concat(
                      Stream.of("param", "local", "caught", "field", "inherited").map(scope::lookup),
                      Stream.of(scope.lookup("max", Scope.Namespace.METHOD)))
                    .map(b -> b.map(binding -> binding.getName() + "=" + binding.getKind()).orElse("?"))
                    .collect(joining(" ")));
              }

              @Override
              public J.Block visitBlock(J.Block block, ExecutionContext ctx) {
                  J.Block b = super.visitBlock(block, ctx);
                  if (!(getCursor().getParentTreeCursor().getValue() instanceof J.Block)) {
                      return b;
                  }
                  // What a lambda made of the block would capture
                  return SearchResult.found(b, Scope.referencedWithin(getCursor())
                    .filter(binding -> binding.getKind() == Scope.Kind.LOCAL || binding.getKind() == Scope.Kind.PARAMETER)
                    .map(Scope.Binding::getName)
                    .collect(joining(" ")));
              }
          })),
          java(
            """
              import static java.lang.Math.max;

              class Base {
                  int inherited;
              }
              class A extends Base {
                  int field;
                  void m(int param) {
                      int local = 0;
                      int written;
                      {
                          written = max(param, field);
                      }
                      try {
                      } catch (RuntimeException caught) {
                          here();
                      }
                  }
                  void here() {}
              }
              """,
            """
              import static java.lang.Math.max;

              class Base {
                  int inherited;
              }
              class A extends Base {
                  int field;
                  void m(int param) {
                      int local = 0;
                      int written;
                      /*~~(written param)~~>*/{
                          written = max(param, field);
                      }
                      try {
                      } catch (RuntimeException caught) {
                          /*~~(param=PARAMETER local=LOCAL caught=LOCAL field=MEMBER inherited=INHERITED max=IMPORTED)~~>*/here();
                      }
                  }
                  void here() {}
              }
              """
          )
        );
    }

    @Test
    void blankLocalIsEffectivelyFinalWhereEachPathAssignsItOnce() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.Identifier visitIdentifier(J.Identifier identifier, ExecutionContext ctx) {
                  return Scope.bindingOf(getCursor(), identifier)
                    .filter(b -> b.getName().endsWith("_"))
                    .map(b -> SearchResult.found(identifier, b.isEffectivelyFinal() ? "final" : "assigned"))
                    .orElse(identifier);
              }
          })),
          java(
            """
              class A {
                  void m(boolean c, int k) {
                      int branches_;
                      if (c) branches_ = 1; else branches_ = 2;
                      int twice_;
                      if (c) twice_ = 1;
                      twice_ = 2;
                      int arrows_;
                      switch (k) { case 1 -> arrows_ = 1; default -> arrows_ = 2; }
                      int fallsThrough_;
                      switch (k) { case 1: fallsThrough_ = 1; default: fallsThrough_ = 2; }
                      int ternary_;
                      int unused = c ? (ternary_ = 1) : (ternary_ = 2);
                  }
              }
              """,
            """
              class A {
                  void m(boolean c, int k) {
                      int /*~~(final)~~>*/branches_;
                      if (c) branches_ = 1; else branches_ = 2;
                      int /*~~(assigned)~~>*/twice_;
                      if (c) twice_ = 1;
                      twice_ = 2;
                      int /*~~(final)~~>*/arrows_;
                      switch (k) { case 1 -> arrows_ = 1; default -> arrows_ = 2; }
                      int /*~~(assigned)~~>*/fallsThrough_;
                      switch (k) { case 1: fallsThrough_ = 1; default: fallsThrough_ = 2; }
                      int /*~~(final)~~>*/ternary_;
                      int unused = c ? (ternary_ = 1) : (ternary_ = 2);
                  }
              }
              """
          )
        );
    }

    @Test
    void nameIsFreeUnlessVisibleSpelledInTheScopeOrReserved() {
        rewriteRun(
          spec -> spec.recipe(atHere(scope -> Stream.of("field", "p", "later", "class", "here", "fresh")
            .filter(scope::isFree)
            .collect(joining(" ")))),
          java(
            """
              class A {
                  int field;
                  void m(int p) {
                      here();
                      int later = 1;
                  }
                  void here() {}
              }
              """,
            """
              class A {
                  int field;
                  void m(int p) {
                      /*~~(here fresh)~~>*/here();
                      int later = 1;
                  }
                  void here() {}
              }
              """
          )
        );
    }

    @Test
    void bindingOfADeclarationFromItsOwnCursorOrOneAbove() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                  J.VariableDeclarations parameter = (J.VariableDeclarations) method.getParameters().get(0);
                  return SearchResult.found(method, Stream.of(
                      Scope.bindingOf(getCursor(), method),
                      Scope.bindingOf(getCursor(), parameter.getVariables().get(0)))
                    .map(b -> b.map(binding -> binding.getName() + ":" + binding.getNamespace() + ":" + binding.getKind()).orElse("?"))
                    .collect(joining(" ")));
              }
          })),
          java(
            """
              class A {
                  void m(int p) {
                  }
              }
              """,
            """
              class A {
                  /*~~(m:METHOD:MEMBER p:VARIABLE:PARAMETER)~~>*/void m(int p) {
                  }
              }
              """
          )
        );
    }

    @Test
    void instanceOfIsMatchedWhereAPatternVariableOfItWouldBeInScope() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              Cursor instanceOf;

              @Override
              public J.InstanceOf visitInstanceOf(J.InstanceOf tested, ExecutionContext ctx) {
                  instanceOf = getCursor();
                  return super.visitInstanceOf(tested, ctx);
              }

              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                  if (!"here".equals(m.getSimpleName())) {
                      return m;
                  }
                  ScopeService service = getCursor().firstEnclosingOrThrow(J.CompilationUnit.class).service(ScopeService.class);
                  return SearchResult.found(m, service.isMatchedAt(instanceOf, getCursor()) ? "matched" : "open");
              }
          })),
          java(
            """
              class A {
                  void leaves(Object o) {
                      if (!(o instanceof String)) { here(); return; }
                      here();
                  }
                  void branches(Object o) {
                      if (!(o instanceof String)) { here(); } else { here(); }
                      here();
                  }
                  void conjunction(Object o) {
                      if (o instanceof String && here()) { here(); }
                      here();
                  }
                  boolean here() { return true; }
              }
              """,
            """
              class A {
                  void leaves(Object o) {
                      if (!(o instanceof String)) { /*~~(open)~~>*/here(); return; }
                      /*~~(matched)~~>*/here();
                  }
                  void branches(Object o) {
                      if (!(o instanceof String)) { /*~~(open)~~>*/here(); } else { /*~~(matched)~~>*/here(); }
                      /*~~(open)~~>*/here();
                  }
                  void conjunction(Object o) {
                      if (o instanceof String && /*~~(matched)~~>*/here()) { /*~~(matched)~~>*/here(); }
                      /*~~(open)~~>*/here();
                  }
                  boolean here() { return true; }
              }
              """
          )
        );
    }

    @Test
    void parameterIsVisibleInAnEditedCopyOfTheBody() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                  // A recipe mid-visit holds an edited copy of the body the method does not have yet.
                  Cursor copy = new Cursor(getCursor(), method.getBody().withEnd(Space.SINGLE_SPACE));
                  return SearchResult.found(method, Scope.at(copy).lookup("p").isPresent() ? "visible" : "hidden");
              }
          })),
          java(
            """
              class A {
                  void m(int p) {
                  }
              }
              """,
            """
              class A {
                  /*~~(visible)~~>*/void m(int p) {
                  }
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
    void localOfAClassBodyMayTakeTheNameOfAnEnclosingLocal() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.Identifier visitIdentifier(J.Identifier identifier, ExecutionContext ctx) {
                  return Scope.bindingOf(getCursor(), identifier)
                    .filter(b -> b.getName().endsWith("_"))
                    .map(b -> SearchResult.found(identifier, b.canRenameTo("taken") ? "safe" : "unsafe"))
                    .orElse(identifier);
              }
          })),
          java(
            """
              class A {
                  void m() {
                      int taken = 1;
                      Runnable lambda = () -> {
                          int inLambda_ = 2;
                      };
                      Runnable anonymous = new Runnable() {
                          public void run() {
                              int inClassBody_ = 3;
                          }
                      };
                  }
                  void outer() {
                      int enclosing_ = 1;
                      new Object() {
                          void inner() {
                              int taken = 2;
                          }
                      };
                  }
              }
              """,
            """
              class A {
                  void m() {
                      int taken = 1;
                      Runnable lambda = () -> {
                          int /*~~(unsafe)~~>*/inLambda_ = 2;
                      };
                      Runnable anonymous = new Runnable() {
                          public void run() {
                              int /*~~(safe)~~>*/inClassBody_ = 3;
                          }
                      };
                  }
                  void outer() {
                      int /*~~(safe)~~>*/enclosing_ = 1;
                      new Object() {
                          void inner() {
                              int taken = 2;
                          }
                      };
                  }
              }
              """
          )
        );
    }

    @Test
    void renameIsSafeUnlessANameWouldReadSomethingElse() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.Identifier visitIdentifier(J.Identifier identifier, ExecutionContext ctx) {
                  return Scope.bindingOf(getCursor(), identifier)
                    .filter(b -> b.getNamespace() == Scope.Namespace.VARIABLE && b.getName().endsWith("_"))
                    .map(b -> SearchResult.found(identifier, b.canRenameTo("taken") ? "safe" : "unsafe"))
                    .orElse(identifier);
              }
          })),
          java(
            """
              class A {
                  int qualified_;
                  A set(int taken) {
                      this.qualified_ = taken;
                      return this;
                  }
              }
              class C {
                  int bare_;
                  int get(int taken) {
                      return bare_ + taken;
                  }
              }
              class D {
                  int sibling_;
                  int taken;
              }
              class E {
                  int taken;
                  class Inner {
                      int capturing_;
                      int get() {
                          return taken;
                      }
                  }
              }
              class B {
                  int free_;
                  int m(int p) {
                      int local_ = p;
                      { int taken = 1; }
                      return local_ + free_;
                  }
              }
              """,
            """
              class A {
                  int /*~~(safe)~~>*/qualified_;
                  A set(int taken) {
                      this.qualified_ = taken;
                      return this;
                  }
              }
              class C {
                  int /*~~(unsafe)~~>*/bare_;
                  int get(int taken) {
                      return bare_ + taken;
                  }
              }
              class D {
                  int /*~~(unsafe)~~>*/sibling_;
                  int taken;
              }
              class E {
                  int taken;
                  class Inner {
                      int /*~~(unsafe)~~>*/capturing_;
                      int get() {
                          return taken;
                      }
                  }
              }
              class B {
                  int /*~~(safe)~~>*/free_;
                  int m(int p) {
                      int /*~~(unsafe)~~>*/local_ = p;
                      { int taken = 1; }
                      return local_ + free_;
                  }
              }
              """
          )
        );
    }

    private static Recipe referenceCounts() {
        return toRecipe(() -> new JavaIsoVisitor<>() {
            @Override
            public J.Identifier visitIdentifier(J.Identifier identifier, ExecutionContext ctx) {
                return Scope.bindingOf(getCursor(), identifier)
                  .filter(b -> b.getNamespace() == Scope.Namespace.VARIABLE)
                  .map(b -> SearchResult.found(identifier, b.references().count() + " references"))
                  .orElse(identifier);
            }
        });
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
