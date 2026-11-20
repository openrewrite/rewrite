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

import com.sun.source.util.JavacTask;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.openrewrite.Cursor;
import org.openrewrite.DocumentExample;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.service.ScopeService;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.marketplace.RecipeClassLoader;
import org.openrewrite.test.RewriteTest;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
                  void m(boolean c) { do { try { break; } finally { return; } } while (c); }
                  void n(boolean c) { do { X: while (c) break X; return; } while (c); }
                  void o() { do { } while ((true)); }
                  void p() { while (true) { L: { break L; } } }
                  void q(boolean c, int x) { do { switch (x) { case 1: continue; } return; } while (c); }
                  void r(boolean c, int x) { do { switch (x) { case 1: break; } return; } while (c); }
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
                  void m(boolean c) { /*~~(leaves)~~>*/do { try { break; } finally { return; } } while (c); }
                  void n(boolean c) { /*~~(leaves)~~>*/do { X: while (c) break X; return; } while (c); }
                  void o() { /*~~(leaves)~~>*/do { } while ((true)); }
                  void p() { /*~~(leaves)~~>*/while (true) { L: { break L; } } }
                  void q(boolean c, int x) { /*~~(completes)~~>*/do { switch (x) { case 1: continue; } return; } while (c); }
                  void r(boolean c, int x) { /*~~(leaves)~~>*/do { switch (x) { case 1: break; } return; } while (c); }
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
            .collect(joining(", ")) + ", with nothing else taken y=" + scope.freeName("y"))),
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
                      /*~~(x=x2, size=size, f=f1, l=l1, y=y1, class=class1, with nothing else taken y=y)~~>*/here();
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
    void declarationIsTheTreeDeclaringTheNameWhereTheScopeHoldsOne() {
        rewriteRun(
          spec -> spec.recipe(atHere(scope -> Stream.of(scope.lookup("local"), scope.lookup("inherited"), scope.lookup("m", Scope.Namespace.METHOD))
            .map(binding -> binding.orElseThrow().getDeclaration().map(d -> d.getValue().getClass().getSimpleName()).orElse("none"))
            .collect(joining(" ")))),
          java(
            """
              class B {
                  protected int inherited;
              }
              """
          ),
          java(
            """
              class A extends B {
                  void m() {
                      int local = 0;
                      here();
                  }
                  void here() {}
              }
              """,
            """
              class A extends B {
                  void m() {
                      int local = 0;
                      /*~~(NamedVariable none MethodDeclaration)~~>*/here();
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
    void nameAClassBindsWithoutSpellingItIsTakenWhenAskedAtTheClass() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                  Scope scope = Scope.at(getCursor());
                  return "A".equals(classDecl.getSimpleName()) ?
                    SearchResult.found(classDecl, Stream.of("inherited", "fresh").filter(scope::isFree).collect(joining(" "))) :
                    classDecl;
              }
          })),
          java(
            """
              class B {
                  protected int inherited;
              }
              """
          ),
          java(
            """
              class A extends B {
              }
              """,
            """
              /*~~(fresh)~~>*/class A extends B {
              }
              """
          )
        );
    }

    @Test
    void classBindsAPrivateMemberOnlyAttributionLists() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().classpath("lombok"))
            .recipe(atHere(scope -> scope.lookup("log").map(b -> b.getKind().toString()).orElse("unbound"))),
          java(
            """
              import lombok.extern.java.Log;

              @Log
              class A {
                  void m() {
                      here();
                  }
                  void here() {}
              }
              """,
            """
              import lombok.extern.java.Log;

              @Log
              class A {
                  void m() {
                      /*~~(INHERITED)~~>*/here();
                  }
                  void here() {}
              }
              """
          )
        );
    }

    @Test
    void bindingOfADeclarationFromItsOwnCursorOrOneAboveOrBelow() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                  J.VariableDeclarations parameter = (J.VariableDeclarations) method.getParameters().get(0);
                  return SearchResult.found(method, Stream.of(
                      Scope.bindingOf(getCursor(), method),
                      Scope.bindingOf(getCursor(), parameter.getVariables().get(0)),
                      Scope.bindingOf(new Cursor(getCursor(), method.getBody()), parameter.getVariables().get(0)))
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
                  /*~~(m:METHOD:MEMBER p:VARIABLE:PARAMETER p:VARIABLE:PARAMETER)~~>*/void m(int p) {
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
                  void breaksOuterLoop(Object o, boolean c) {
                      L: while (c) {
                          while (!(o instanceof String)) { break L; }
                          here();
                      }
                  }
                  void breaksOwnLabel(Object o) {
                      L: while (!(o instanceof String)) { break L; }
                      here();
                  }
                  void breakDiscardedByFinally(Object o) {
                      while (!(o instanceof String)) { try { break; } finally { return; } }
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
                  void breaksOuterLoop(Object o, boolean c) {
                      L: while (c) {
                          while (!(o instanceof String)) { break L; }
                          /*~~(matched)~~>*/here();
                      }
                  }
                  void breaksOwnLabel(Object o) {
                      L: while (!(o instanceof String)) { break L; }
                      /*~~(open)~~>*/here();
                  }
                  void breakDiscardedByFinally(Object o) {
                      while (!(o instanceof String)) { try { break; } finally { return; } }
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
    void cursorToATreeBelowHoldsEveryTreeBetween() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                  J.VariableDeclarations c = (J.VariableDeclarations) method.getBody().getStatements().getFirst();
                  J.Block lambdaBody = (J.Block) ((J.Lambda) c.getVariables().getFirst().getInitializer()).getBody();
                  J.Block edited = lambdaBody.withEnd(Space.SINGLE_SPACE);
                  getCursor().putMessage("given", true);
                  Cursor copy = Scope.cursorTo(getCursor(), edited);
                  // Cursors are equal by value, so only a message tells the given cursor from another on its tree
                  assertThat(copy.<Boolean>getNearestMessage("given")).isTrue();
                  assertThatThrownBy(() -> Scope.cursorTo(new Cursor(getCursor(), method.getBody()), method.getName()))
                    .isInstanceOf(IllegalArgumentException.class);
                  // The parent of a method's cursor holds padding and no tree
                  assertThatThrownBy(() -> Scope.cursorTo(getCursor().getParentOrThrow(), method.getName()))
                    .isInstanceOf(IllegalArgumentException.class);
                  // A cursor on the lambda's body that left out the lambda would find the field
                  return SearchResult.found(method, Stream.of(Scope.cursorTo(getCursor(), lambdaBody), copy)
                    .map(body -> Scope.at(body).lookup("p").map(b -> b.getKind().toString()).orElse("?"))
                    .collect(joining(" ")) + (copy.getValue() == edited ? " on the copy" : ""));
              }
          })),
          java(
            """
              import java.util.function.Consumer;

              class A {
                  String p;
                  void m() {
                      Consumer<String> c = p -> { System.out.println(p); };
                  }
              }
              """,
            """
              import java.util.function.Consumer;

              class A {
                  String p;
                  /*~~(PARAMETER PARAMETER on the copy)~~>*/void m() {
                      Consumer<String> c = p -> { System.out.println(p); };
                  }
              }
              """
          )
        );
    }

    @Test
    void referencesWithinAnEditedRegionComeFromTheCopy() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                  List<Statement> statements = method.getBody().getStatements();
                  J.Assignment assignsY = (J.Assignment) statements.get(2);
                  // A recipe mid-visit holds a copy that assigns x where the method still assigns y.
                  J.Assignment assignsX = assignsY.withVariable(((J.Identifier) assignsY.getVariable()).withSimpleName("x"));
                  Cursor copy = new Cursor(new Cursor(getCursor(), method.getBody()), assignsX);
                  return SearchResult.found(method, Scope.bindingOf(getCursor(), ((J.VariableDeclarations) statements.getFirst()).getVariables().getFirst())
                    .map(x -> x.references().count() + " in the method, " + x.references(copy).count() + " in the copy")
                    .orElse("?"));
              }
          })),
          java(
            """
              class A {
                  void m() {
                      int x = 0;
                      int y = 0;
                      y = 1;
                  }
              }
              """,
            """
              class A {
                  /*~~(0 in the method, 1 in the copy)~~>*/void m() {
                      int x = 0;
                      int y = 0;
                      y = 1;
                  }
              }
              """
          )
        );
    }

    @Test
    void blockCanMoveWhereEveryNameKeepsItsMeaning() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                  if (!method.getSimpleName().startsWith("before") && !method.getSimpleName().startsWith("after")) {
                      return method;
                  }
                  Cursor body = new Cursor(getCursor(), method.getBody());
                  // The block to move is the last one of the first statement that has a block
                  for (Statement statement : method.getBody().getStatements()) {
                      List<J.Block> blocks = new JavaIsoVisitor<List<J.Block>>() {
                          @Override
                          public J.Block visitBlock(J.Block block, List<J.Block> found) {
                              found.add(block);
                              return super.visitBlock(block, found);
                          }

                          @Override
                          public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, List<J.Block> found) {
                              return classDecl == statement ? super.visitClassDeclaration(classDecl, found) : classDecl;
                          }
                      }.reduce(statement, new ArrayList<>());
                      if (!blocks.isEmpty()) {
                          Cursor destination = new Cursor(body, statement);
                          Scope moving = Scope.at(Scope.cursorTo(body, blocks.getLast()));
                          boolean moves = method.getSimpleName().startsWith("before") ?
                            moving.canMoveBefore(destination) : moving.canMoveAfter(destination);
                          return SearchResult.found(method, moves ? "moves" : "stays");
                      }
                  }
                  return method;
              }
          })),
          java(
            """
              import java.util.function.IntConsumer;

              class A {
                  int field;
                  void beforeDeclaredLater() {
                      { int x = 1; use(x); }
                      for (int x = 0; x < 1; x++) { }
                  }
                  void beforeReadLater() {
                      { int field = 1; use(field); }
                      use(field);
                  }
                  void beforeUntouched() {
                      { int y = 1; use(y); }
                      use(field);
                  }
                  void beforeLocalClassLater() {
                      { class Helper {} }
                      class Helper {}
                  }
                  void beforeRestOfStatement() {
                      apply(() -> { int v = 1; use(v); }, v -> use(v));
                  }
                  void beforeVisibleLocal() {
                      int x = 0;
                      new Object() {{ int x = 1; use(x); }};
                  }
                  void beforeOwnMember() {
                      new Object() {{ use(hashCode()); }};
                  }
                  void beforeThis() {
                      new Object() {{ use(this.hashCode()); }};
                  }
                  void beforeNestedParameter() {
                      int x = 0;
                      new Object() {{ IntConsumer c = x -> use(x); c.accept(1); }};
                  }
                  void beforeItsOwnLocalClass() {
                      class Node {{ Node next = null; use(next == null ? 0 : 1); }}
                  }
                  void afterThenDeclaresTheName(boolean c) {
                      if (c) { int n = 1; use(n); return; } else { int n = 2; use(n); }
                  }
                  void afterPatternInScope(Object o) {
                      if (!(o instanceof String s)) { return; } else { use(s.length()); }
                  }
                  void afterDeclaredByStatement() {
                      Object list = new Object() {{ int list = 1; use(list); }};
                  }
                  void afterParameterBeforeLocalClass(int x) {
                      new Object() {{ int x = 1; use(x); }};
                      class Local {}
                  }
                  void use(int i) {}
                  void apply(Runnable r, IntConsumer c) {}
              }
              """,
            """
              import java.util.function.IntConsumer;

              class A {
                  int field;
                  /*~~(stays)~~>*/void beforeDeclaredLater() {
                      { int x = 1; use(x); }
                      for (int x = 0; x < 1; x++) { }
                  }
                  /*~~(stays)~~>*/void beforeReadLater() {
                      { int field = 1; use(field); }
                      use(field);
                  }
                  /*~~(moves)~~>*/void beforeUntouched() {
                      { int y = 1; use(y); }
                      use(field);
                  }
                  /*~~(stays)~~>*/void beforeLocalClassLater() {
                      { class Helper {} }
                      class Helper {}
                  }
                  /*~~(stays)~~>*/void beforeRestOfStatement() {
                      apply(() -> { int v = 1; use(v); }, v -> use(v));
                  }
                  /*~~(stays)~~>*/void beforeVisibleLocal() {
                      int x = 0;
                      new Object() {{ int x = 1; use(x); }};
                  }
                  /*~~(stays)~~>*/void beforeOwnMember() {
                      new Object() {{ use(hashCode()); }};
                  }
                  /*~~(stays)~~>*/void beforeThis() {
                      new Object() {{ use(this.hashCode()); }};
                  }
                  /*~~(stays)~~>*/void beforeNestedParameter() {
                      int x = 0;
                      new Object() {{ IntConsumer c = x -> use(x); c.accept(1); }};
                  }
                  /*~~(stays)~~>*/void beforeItsOwnLocalClass() {
                      class Node {{ Node next = null; use(next == null ? 0 : 1); }}
                  }
                  /*~~(moves)~~>*/void afterThenDeclaresTheName(boolean c) {
                      if (c) { int n = 1; use(n); return; } else { int n = 2; use(n); }
                  }
                  /*~~(moves)~~>*/void afterPatternInScope(Object o) {
                      if (!(o instanceof String s)) { return; } else { use(s.length()); }
                  }
                  /*~~(stays)~~>*/void afterDeclaredByStatement() {
                      Object list = new Object() {{ int list = 1; use(list); }};
                  }
                  /*~~(stays)~~>*/void afterParameterBeforeLocalClass(int x) {
                      new Object() {{ int x = 1; use(x); }};
                      class Local {}
                  }
                  void use(int i) {}
                  void apply(Runnable r, IntConsumer c) {}
              }
              """
          )
        );
        // The same methods with each block moved. javac rejects those that stay, apart from beforeReadLater,
        // beforeOwnMember and beforeThis, which compile and read something else.
        assertThat(methodsJavacRejects(
          """
            import java.util.function.IntConsumer;

            class A {
                int field;
                void beforeDeclaredLater() {
                    int x = 1; use(x);
                    for (int x = 0; x < 1; x++) { }
                }
                void beforeReadLater() {
                    int field = 1; use(field);
                    use(field);
                }
                void beforeUntouched() {
                    int y = 1; use(y);
                    use(field);
                }
                void beforeLocalClassLater() {
                    class Helper {}
                    class Helper {}
                }
                void beforeRestOfStatement() {
                    int v = 1; use(v);
                    apply(() -> { }, v -> use(v));
                }
                void beforeVisibleLocal() {
                    int x = 0;
                    int x = 1; use(x);
                    new Object() {{ }};
                }
                void beforeOwnMember() {
                    use(hashCode());
                    new Object() {{ }};
                }
                void beforeThis() {
                    use(this.hashCode());
                    new Object() {{ }};
                }
                void beforeNestedParameter() {
                    int x = 0;
                    IntConsumer c = x -> use(x); c.accept(1);
                    new Object() {{ }};
                }
                void beforeItsOwnLocalClass() {
                    Node next = null; use(next == null ? 0 : 1);
                    class Node {{ }}
                }
                void afterThenDeclaresTheName(boolean c) {
                    if (c) { int n = 1; use(n); return; }
                    int n = 2; use(n);
                }
                void afterPatternInScope(Object o) {
                    if (!(o instanceof String s)) { return; }
                    use(s.length());
                }
                void afterDeclaredByStatement() {
                    Object list = new Object() {{ }};
                    int list = 1; use(list);
                }
                void afterParameterBeforeLocalClass(int x) {
                    new Object() {{ }};
                    int x = 1; use(x);
                    class Local {}
                }
                void use(int i) {}
                void apply(Runnable r, IntConsumer c) {}
            }
            """
        )).containsExactly("afterDeclaredByStatement", "afterParameterBeforeLocalClass", "beforeDeclaredLater",
          "beforeItsOwnLocalClass", "beforeLocalClassLater", "beforeNestedParameter", "beforeRestOfStatement",
          "beforeVisibleLocal");
    }

    @Test
    void lambdaCanCaptureEffectivelyFinalLocalsItDoesNotAssign() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                  Cursor body = new Cursor(getCursor(), method.getBody());
                  // The statements after expect() are the ones to wrap in a lambda
                  List<Cursor> regions = null;
                  for (Statement statement : method.getBody().getStatements()) {
                      if (regions != null) {
                          regions.add(new Cursor(body, statement));
                      } else if (statement instanceof J.MethodInvocation invocation && "expect".equals(invocation.getSimpleName())) {
                          regions = new ArrayList<>();
                      }
                  }
                  return regions == null ? method : SearchResult.found(method, Scope.lambdaCanCapture(regions) ? "captures" : "refuses");
              }
          })),
          java(
            """
              class A {
                  int f;
                  void readsFinal(int p) { expect(); use(p); }
                  void readsReassigned() { int i = 0; i++; expect(); use(i); }
                  void assignsBlank() { int r; expect(); r = 1; use(r); }
                  void declaresItsOwn() { expect(); int n = 0; n++; use(n); }
                  void assignsField() { expect(); f = 1; }
                  void expect() {}
                  void use(int i) {}
              }
              """,
            """
              class A {
                  int f;
                  /*~~(captures)~~>*/void readsFinal(int p) { expect(); use(p); }
                  /*~~(refuses)~~>*/void readsReassigned() { int i = 0; i++; expect(); use(i); }
                  /*~~(refuses)~~>*/void assignsBlank() { int r; expect(); r = 1; use(r); }
                  /*~~(captures)~~>*/void declaresItsOwn() { expect(); int n = 0; n++; use(n); }
                  /*~~(captures)~~>*/void assignsField() { expect(); f = 1; }
                  void expect() {}
                  void use(int i) {}
              }
              """
          )
        );
        assertThat(methodsJavacRejects(
          """
            class A {
                int f;
                void readsFinal(int p) { run(() -> { use(p); }); }
                void readsReassigned() { int i = 0; i++; run(() -> { use(i); }); }
                void assignsBlank() { int r; run(() -> { r = 1; use(r); }); }
                void declaresItsOwn() { run(() -> { int n = 0; n++; use(n); }); }
                void assignsField() { run(() -> { f = 1; }); }
                void run(Runnable r) {}
                void use(int i) {}
            }
            """
        )).containsExactly("assignsBlank", "readsReassigned");
    }

    @Test
    void nameInScopeCannotBeReferencedAcrossAStaticContext() {
        @Language("java") String source =
          """
            class B {
                int inherited;
            }
            class A<T> extends B {
                int inst;
                static int stat;
                int instMethod() { return 0; }
                static int use(String row, Object read) { return 0; }
                A() {
                    this(use("constructorCallArgument", inst));
                }
                A(String s) {
                    this(use("constructorCallNamesTypeParameter", (T) null));
                }
                A(int i) {}
                int instanceMethod() {
                    return use("instanceMethod", inst);
                }
                static int staticMethod() {
                    return use("staticMethod", inst);
                }
                static int staticMethodReadsStaticField() {
                    return use("staticMethodReadsStaticField", stat);
                }
                static int staticMethodReadsInheritedField() {
                    return use("staticMethodReadsInheritedField", inherited);
                }
                static int staticMethodCallsInstanceMethod() {
                    return use("staticMethodCallsInstanceMethod", instMethod());
                }
                static int staticMethodNamesClassTypeParameter() {
                    return use("staticMethodNamesClassTypeParameter", (T) null);
                }
                static {
                    use("staticInitializer", inst);
                }
                static int staticField =
                        use("staticFieldInitializer", inst);
                int instanceField =
                        use("instanceFieldInitializer", inst);
                class Inner {
                    Inner() {
                        this(use("innerConstructorCallReadsOuterField", inst));
                    }
                    Inner(int i) {}
                    int m() {
                        return use("innerClass", inst);
                    }
                }
                static class Nested {
                    int own;
                    int m() {
                        return use("staticNestedClassOwnField", own) +
                               use("staticNestedClass", inst);
                    }
                }
                record Rec(int component) {
                    int m() {
                        return use("nestedRecord", inst);
                    }
                }
                static void localClassInStaticMethod() {
                    class Local {
                        int m() {
                            return use("localClassInStaticMethod", inst);
                        }
                    }
                }
                enum E {
                    X(use("enumConstantArgument", hidden)) {
                        int m() {
                            return use("enumConstantBodyReadsPrivateField", hidden);
                        }
                    };
                    private int hidden;
                    E(int i) {}
                }
                void local() {
                    int local = 1;
                    record LocalRec() {
                        int m() {
                            return use("localRecordReadsLocal", local);
                        }
                    }
                }
            }
            """;
        assertThat(readsRefused(source)).containsExactly(
          "constructorCallArgument",
          "enumConstantArgument",
          "enumConstantBodyReadsPrivateField",
          "localClassInStaticMethod",
          "localRecordReadsLocal",
          "nestedRecord",
          "staticFieldInitializer",
          "staticInitializer",
          "staticMethod",
          "staticMethodCallsInstanceMethod",
          "staticMethodNamesClassTypeParameter",
          "staticMethodReadsInheritedField",
          "staticNestedClass");
        assertThat(rowsJavacRejects(source, USE_ROW)).isEqualTo(readsRefused(source));
    }

    @Test
    void initializerCannotReadAFieldDeclaredAtOrAfterIt() {
        @Language("java") String source =
          """
            class A {
                static int use(String row, Object read) { return 0; }
                static {
                    use("staticInitializerBeforeStaticField", LATER);
                }
                int early =
                        use("fieldInitializerBeforeInstanceField", later);
                {
                    use("instanceInitializerBeforeStaticField", LATER);
                }
                {
                    use("instanceInitializerAssignsLaterField", later = 3);
                    use("instanceInitializerCompoundAssignsLaterField", later += 3);
                    use("instanceInitializerIncrementsLaterField", later++);
                }
                Runnable lambda = () ->
                        use("lambdaInInitializerBeforeField", later);
                Object anonymous = new Object() {
                    int m() {
                        return use("anonymousClassInInitializerBeforeField", later);
                    }
                };
                int method() {
                    return use("methodBeforeField", later);
                }
                int own =
                        use("ownInitializer", own);
                int first =
                        use("laterDeclaratorOfOneStatement", second), second =
                        use("earlierDeclaratorOfOneStatement", first);
                enum E {
                    X(use("enumConstantBeforeStaticField", COUNT));
                    static int COUNT = 1;
                    E(int i) {}
                }
                static final int LATER = 1;
                int later = 2;
                static {
                    use("staticInitializerAfterStaticField", LATER);
                }
                int after =
                        use("fieldInitializerAfterInstanceField", later);
            }
            """;
        assertThat(readsRefused(source)).containsExactly(
          "enumConstantBeforeStaticField",
          "fieldInitializerBeforeInstanceField",
          "instanceInitializerCompoundAssignsLaterField",
          "instanceInitializerIncrementsLaterField",
          "lambdaInInitializerBeforeField",
          "laterDeclaratorOfOneStatement",
          "ownInitializer",
          "staticInitializerBeforeStaticField");
        assertThat(rowsJavacRejects(source, USE_ROW)).isEqualTo(readsRefused(source));
    }

    @Test
    void enumCannotReadItsOwnStaticFieldWhileAConstantIsConstructed() {
        @Language("java") String source =
          """
            interface Shared {
                Object INHERITED = new Object();
            }
            enum E implements Shared {
                A {
                    Object inBody =
                            use("constantBodyFieldInitializer", logger);
                    int m() {
                        return use("constantBodyMethod", logger);
                    }
                };
                static int use(String row, Object read) { return 0; }
                static int helper() { return 0; }
                static final Object logger = new Object();
                static final int PRIMITIVE = -1;
                static final String TEXT = "a" + 1;
                static final long CONVERTED = (long) (true ? 1 : 2);
                static final Integer BOXED = 1;
                static final int COMPUTED = "a".length();
                static final String NULL = null;
                static int notFinal = 1;
                E() {
                    use("constructor", logger);
                    use("constructorCallsStaticMethod", helper());
                    use("constructorReadsEnumConstant", A);
                    use("constructorReadsConstantPrimitive", PRIMITIVE);
                    use("constructorReadsConstantString", TEXT);
                    use("constructorReadsConstantCastOfConditional", CONVERTED);
                    use("constructorReadsBoxedField", BOXED);
                    use("constructorReadsComputedField", COMPUTED);
                    use("constructorReadsNullField", NULL);
                    use("constructorReadsLiteralFieldThatIsNotFinal", notFinal);
                    use("constructorReadsInterfaceField", INHERITED);
                    Runnable lambda = () ->
                            use("lambdaInConstructor", logger);
                    new Object() {
                        int m() {
                            return use("classBodyInConstructor", logger);
                        }
                    };
                }
                {
                    use("instanceInitializer", logger);
                }
                int field =
                        use("instanceFieldInitializer", logger);
                static {
                    use("staticInitializer", logger);
                }
                int method() {
                    return use("method", logger);
                }
            }
            class C {
                static final Object logger = new Object();
                C() {
                    E.use("constructorOfAClass", logger);
                }
            }
            """;
        assertThat(readsRefused(source)).containsExactly(
          "constantBodyFieldInitializer",
          "constructor",
          "constructorReadsBoxedField",
          "constructorReadsComputedField",
          "constructorReadsEnumConstant",
          "constructorReadsLiteralFieldThatIsNotFinal",
          "constructorReadsNullField",
          "instanceFieldInitializer",
          "instanceInitializer",
          "lambdaInConstructor");
        assertThat(rowsJavacRejects(source, USE_ROW)).isEqualTo(readsRefused(source));
    }

    @Test
    void enumConstructorCannotReadAStaticFieldOnlyAttributionLists() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().classpath("lombok"))
            .recipe(atHere(scope -> scope.lookup("log").map(scope::canReference).map(String::valueOf).orElse("unbound"))),
          java(
            """
              import lombok.extern.java.Log;

              @Log
              enum E {
                  A;
                  E() {
                      here();
                  }
                  void m() {
                      here();
                  }
                  void here() {}
              }
              """,
            """
              import lombok.extern.java.Log;

              @Log
              enum E {
                  A;
                  E() {
                      /*~~(false)~~>*/here();
                  }
                  void m() {
                      /*~~(true)~~>*/here();
                  }
                  void here() {}
              }
              """
          )
        );
    }

    @Test
    void shadowedBindingCannotBeReferenced() {
        rewriteRun(
          spec -> spec.recipe(atHere(scope -> {
              Scope classScope = scope;
              while (!(classScope.getTree() instanceof J.ClassDeclaration)) {
                  classScope = classScope.enclosing().orElseThrow();
              }
              Scope.Binding field = classScope.getBindings().stream().filter(b -> "x".equals(b.getName())).findFirst().orElseThrow();
              return String.valueOf(scope.canReference(field));
          })),
          java(
            """
              class A {
                  int x;
                  void shadows(int x) {
                      here();
                  }
                  void reads() {
                      here();
                  }
                  void here() {}
              }
              """,
            """
              class A {
                  int x;
                  void shadows(int x) {
                      /*~~(false)~~>*/here();
                  }
                  void reads() {
                      /*~~(true)~~>*/here();
                  }
                  void here() {}
              }
              """
          )
        );
    }

    @Test
    void localIsReferencedFromALambdaOrClassBodyOnlyIfEffectivelyFinal() {
        @Language("java") String source =
          """
            class A {
                static int use(String row, Object read) { return 0; }
                void m() {
                    int constant = 1;
                    int reassigned = 1;
                    reassigned++;
                    use("sameMethodReadsReassignedLocal", reassigned);
                    Runnable lambda = () -> {
                        use("lambdaReadsEffectivelyFinalLocal", constant);
                        use("lambdaReadsReassignedLocal", reassigned);
                    };
                    new Object() {
                        int m() {
                            return use("anonymousClassReadsReassignedLocal", reassigned);
                        }
                    };
                }
            }
            """;
        assertThat(readsRefused(source)).containsExactly("anonymousClassReadsReassignedLocal", "lambdaReadsReassignedLocal");
        assertThat(rowsJavacRejects(source, USE_ROW)).isEqualTo(readsRefused(source));
    }

    @Test
    void expressionCanBeEvaluatedEarlierWhereItReadsTheSameVariablesUnassigned() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                  Cursor body = new Cursor(getCursor(), method.getBody());
                  // Asks whether the argument of use() could be evaluated before the statement that calls before()
                  Cursor[] earlier = {null};
                  Cursor[] read = {null};
                  for (Statement statement : method.getBody().getStatements()) {
                      new JavaIsoVisitor<Integer>() {
                          @Override
                          public J.MethodInvocation visitMethodInvocation(J.MethodInvocation invocation, Integer p) {
                              if ("before".equals(invocation.getSimpleName())) {
                                  earlier[0] = new Cursor(body, statement);
                              } else if ("use".equals(invocation.getSimpleName())) {
                                  read[0] = Scope.cursorTo(body, invocation.getArguments().getFirst());
                              }
                              return super.visitMethodInvocation(invocation, p);
                          }
                      }.visit(statement, 0, body);
                  }
                  return read[0] == null ? method :
                    SearchResult.found(method, Scope.canEvaluateBefore(read[0], earlier[0]) ? "yes" : "no");
              }
          })),
          java(
            """
              class A {
                  int field;
                  void readsEarlierLocal(int p) { Object o = before(); use(p); }
                  void readsLocalDeclaredBetween() { Object o = before(); int n = 1; use(n); }
                  void readsWhatTheStatementDeclares() { Object o = before(); use(o); }
                  void namesTheLocalClassTheStatementDeclares() { class L { Object o = before(); } use(new L()); }
                  void readsLocalAssignedBetween(int p) { Object o = before(); p = 2; use(p); }
                  void readsFieldAssignedBetween() { Object o = before(); this.field = 2; use(field); }
                  void assignsInItsOwnStatement(int p) { Object o = before(); use(p = 3); }
                  void readsQualifiedFieldAssignedBetween() { Object o = before(); field = 2; use(this.field); }
                  void readsFieldWithACallBetween() { Object o = before(); other(); use(field); }
                  void standsInTheStatementItself(int p) { use(before() == null ? p : 0); }
                  void standsBeforeTheStatement(int p) { use(p); Object o = before(); }
                  void namesThisOfAClassBetween() { Object o = before(); new Object() { void m() { use(this); } }; }
                  Object before() { return null; }
                  void other() {}
                  void use(Object read) {}
              }
              """,
            """
              class A {
                  int field;
                  /*~~(yes)~~>*/void readsEarlierLocal(int p) { Object o = before(); use(p); }
                  /*~~(no)~~>*/void readsLocalDeclaredBetween() { Object o = before(); int n = 1; use(n); }
                  /*~~(no)~~>*/void readsWhatTheStatementDeclares() { Object o = before(); use(o); }
                  /*~~(no)~~>*/void namesTheLocalClassTheStatementDeclares() { class L { Object o = before(); } use(new L()); }
                  /*~~(no)~~>*/void readsLocalAssignedBetween(int p) { Object o = before(); p = 2; use(p); }
                  /*~~(no)~~>*/void readsFieldAssignedBetween() { Object o = before(); this.field = 2; use(field); }
                  /*~~(no)~~>*/void assignsInItsOwnStatement(int p) { Object o = before(); use(p = 3); }
                  /*~~(no)~~>*/void readsQualifiedFieldAssignedBetween() { Object o = before(); field = 2; use(this.field); }
                  /*~~(yes)~~>*/void readsFieldWithACallBetween() { Object o = before(); other(); use(field); }
                  /*~~(yes)~~>*/void standsInTheStatementItself(int p) { use(before() == null ? p : 0); }
                  /*~~(no)~~>*/void standsBeforeTheStatement(int p) { use(p); Object o = before(); }
                  /*~~(no)~~>*/void namesThisOfAClassBetween() { Object o = before(); new Object() { void m() { use(this); } }; }
                  Object before() { return null; }
                  void other() {}
                  void use(Object read) {}
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

    @Test
    void recipeSideTraitResolvesABindingAcrossTheRecipeClassLoader() throws Exception {
        Cursor read = lastIdentifier(parse("class A { int x; void m() { x++; } }"), "x");
        URL ownRewriteJava = Scope.class.getProtectionDomain().getCodeSource().getLocation();
        try (RecipeClassLoader recipeLoader = new RecipeClassLoader(new URL[]{ownRewriteJava}, getClass().getClassLoader())) {
            Class<?> recipeSide = recipeLoader.loadClass(VariableAccess.class.getName());
            assertThat(recipeSide).isNotSameAs(VariableAccess.class);
            Object binding = recipeSide.getMethod("getBinding").invoke(recipeSide.getConstructor(Cursor.class).newInstance(read));
            assertThat((Optional<?>) binding).containsInstanceOf(Scope.Binding.class);
        }
    }

    @Test
    void recipeSideMatcherAsksTheHostScopeAcrossTheRecipeClassLoader() throws Exception {
        // The class name carries no field type, so the matcher asks the scope service what it names
        Cursor className = lastIdentifier(parse("class A {}"), "A");
        URL ownRewriteJava = Scope.class.getProtectionDomain().getCodeSource().getLocation();
        try (RecipeClassLoader recipeLoader = new RecipeClassLoader(new URL[]{ownRewriteJava}, getClass().getClassLoader())) {
            Class<?> recipeSide = recipeLoader.loadClass(VariableAccess.Matcher.class.getName());
            assertThat(recipeSide).isNotSameAs(VariableAccess.Matcher.class);
            Object access = recipeSide.getMethod("get", Cursor.class).invoke(recipeSide.getConstructor().newInstance(), className);
            assertThat((Optional<?>) access).isEmpty();
        }
    }

    @Test
    void overloadCanBeReferencedWhicheverIsDeclaredFirst() {
        rewriteRun(
          spec -> spec.recipe(atHere(scope -> {
              Scope classScope = scope;
              while (!(classScope.getTree() instanceof J.ClassDeclaration)) {
                  classScope = classScope.enclosing().orElseThrow();
              }
              return classScope.getBindings().stream()
                .filter(b -> "overloaded".equals(b.getName()))
                .map(b -> String.valueOf(scope.canReference(b)))
                .collect(joining(", "));
          })),
          java(
            """
              class A {
                  void overloaded() {}
                  void overloaded(int i) {}
                  void m() {
                      here();
                  }
                  void here() {}
              }
              """,
            """
              class A {
                  void overloaded() {}
                  void overloaded(int i) {}
                  void m() {
                      /*~~(true, true)~~>*/here();
                  }
                  void here() {}
              }
              """
          )
        );
    }

    @Test
    void memberOfAnInterfaceIsAStaticContextWithoutTheModifier() {
        @Language("java") String source =
          """
            interface A {
                static int use(String row, Object read) { return 0; }
                int instMethod();
                int CONSTANT =
                        use("interfaceFieldInitializer", instMethod());
                class Nested {
                    int m() {
                        return use("classNestedInInterface", instMethod());
                    }
                }
                default int m() {
                    return use("defaultMethod", instMethod());
                }
            }
            """;
        assertThat(readsRefused(source)).containsExactly("classNestedInInterface", "interfaceFieldInitializer");
        assertThat(rowsJavacRejects(source, USE_ROW)).isEqualTo(readsRefused(source));
    }

    @Test
    void sourceFileThatRefusesTheServiceIsReadByJavaRules() {
        JavaSourceFile cu = parse("class A { int x; }");
        J.ClassDeclaration a = cu.getClasses().getFirst();
        for (RuntimeException refusal : List.of(
          new UnsupportedOperationException("Service not supported"),
          // A Kotlin, Groovy or Scala source file wraps what it throws
          new RuntimeException(new UnsupportedOperationException("Service not supported")))) {
            JavaSourceFile refusing = (JavaSourceFile) Proxy.newProxyInstance(getClass().getClassLoader(),
              new Class<?>[]{JavaSourceFile.class}, (proxy, method, args) -> {
                  if ("service".equals(method.getName())) {
                      throw refusal;
                  }
                  return method.invoke(cu, args);
              });
            Cursor body = new Cursor(new Cursor(new Cursor(new Cursor(null, Cursor.ROOT_VALUE), refusing), a), a.getBody());
            assertThat(Scope.at(body).lookup("x")).isPresent();
        }
    }

    private static final Pattern USE_ROW = Pattern.compile("use\\(\"(\\w+)\"");

    /**
     * The rows of a source whose second argument to {@code use(row, read)} names something
     * {@link Scope#canReference(Scope.Binding)} refuses there, in alphabetical order.
     */
    private static Set<String> readsRefused(@Language("java") String source) {
        Set<String> refused = new TreeSet<>();
        new JavaIsoVisitor<Integer>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Integer p) {
                if ("use".equals(method.getSimpleName())) {
                    Expression read = method.getArguments().get(1);
                    J.Identifier name = read instanceof J.MethodInvocation call ? call.getName() :
                      read instanceof J.TypeCast cast ? (J.Identifier) cast.getClazz().getTree() :
                      read instanceof J.Assignment assignment ? (J.Identifier) assignment.getVariable() :
                      read instanceof J.AssignmentOperation operation ? (J.Identifier) operation.getVariable() :
                      read instanceof J.Unary unary ? (J.Identifier) unary.getExpression() : (J.Identifier) read;
                    Cursor at = Scope.cursorTo(getCursor(), name);
                    Scope scope = Scope.at(at);
                    Scope.Binding binding = scope.lookup(name.getSimpleName(), Scope.service(at).namespaceOf(at)).orElseThrow();
                    if (!scope.canReference(binding)) {
                        refused.add((String) ((J.Literal) method.getArguments().getFirst()).getValue());
                    }
                }
                return super.visitMethodInvocation(method, p);
            }
        }.visit(parse(source), 0);
        return refused;
    }

    /**
     * The names of the methods in which javac reports an error, in alphabetical order.
     */
    private static Set<String> methodsJavacRejects(@Language("java") String source) {
        return rowsJavacRejects(source, Pattern.compile("void (\\w+)\\("));
    }

    /**
     * The rows in which javac reports an error, in alphabetical order. A row starts at the line the pattern's first
     * group names it on.
     */
    private static Set<String> rowsJavacRejects(@Language("java") String source, Pattern row) {
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        JavaFileObject file = new SimpleJavaFileObject(URI.create("string:///A.java"), JavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                return source;
            }
        };
        try {
            ((JavacTask) ToolProvider.getSystemJavaCompiler()
              .getTask(null, null, diagnostics, List.of("-proc:none"), null, List.of(file))).analyze();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        String[] lines = source.split("\n");
        Set<String> rejected = new TreeSet<>();
        for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
            if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
                for (int line = (int) diagnostic.getLineNumber() - 1; line >= 0; line--) {
                    Matcher declared = row.matcher(lines[line]);
                    if (declared.find()) {
                        rejected.add(declared.group(1));
                        break;
                    }
                }
            }
        }
        return rejected;
    }

    private static JavaSourceFile parse(@Language("java") String source) {
        return (JavaSourceFile) JavaParser.fromJavaVersion().build().parse(source).findFirst().orElseThrow();
    }

    private static Cursor lastIdentifier(JavaSourceFile sourceFile, String name) {
        Cursor[] found = {null};
        new JavaIsoVisitor<Integer>() {
            @Override
            public J.Identifier visitIdentifier(J.Identifier identifier, Integer p) {
                if (identifier.getSimpleName().equals(name)) {
                    found[0] = getCursor();
                }
                return identifier;
            }
        }.visit(sourceFile, 0);
        return found[0];
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
