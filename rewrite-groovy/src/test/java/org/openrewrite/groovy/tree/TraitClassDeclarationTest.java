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
package org.openrewrite.groovy.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.SourceFile;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.Space;
import org.openrewrite.test.RewriteTest;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.groovy.Assertions.groovy;

class TraitClassDeclarationTest implements RewriteTest {

    @Test
    void trait() {
        rewriteRun(
          groovy(
            """
              trait Foo {
              }
              """
          )
        );
    }

    @Test
    void traitExtendsTrait() {
        rewriteRun(
          groovy(
            """
              trait A {
              }
              trait B extends A {
              }
              """
          )
        );
    }

    @Test
    void traitWithMethod() {
        rewriteRun(
          groovy(
            """
              trait Greeter {
                  String greet() {
                      "hello"
                  }
              }
              """
          )
        );
    }

    @Test
    void traitWithMethodCallInBody() {
        rewriteRun(
          groovy(
            """
              trait Greeter {
                  def greet() {
                      println("hello")
                  }
              }
              """
          )
        );
    }

    @Test
    void traitWithMethodTakingParameter() {
        rewriteRun(
          groovy(
            """
              trait Greeter {
                  String greet(String name) {
                      "hello $name"
                  }
              }
              """
          )
        );
    }

    @Test
    void traitWithMultipleMethods() {
        rewriteRun(
          groovy(
            """
              trait Greeter {
                  String greet() {
                      "hello"
                  }

                  String shout(String msg) {
                      msg.toUpperCase()
                  }
              }
              """
          )
        );
    }

    @Test
    void traitWithField() {
        rewriteRun(
          groovy(
            """
              trait Greeter {
                  String prefix = "Hello"
              }
              """
          )
        );
    }

    @Test
    void traitWithPrivateField() {
        rewriteRun(
          groovy(
            """
              trait Counter {
                  private int count = 0
              }
              """
          )
        );
    }

    @Test
    void traitWithStaticField() {
        rewriteRun(
          groovy(
            """
              trait Counter {
                  static int MAX = 10
              }
              """
          )
        );
    }

    @Test
    void traitWithAbstractMethod() {
        rewriteRun(
          groovy(
            """
              trait Greeter {
                  abstract String greet()
              }
              """
          )
        );
    }

    @Test
    void traitWithVoidMethod() {
        rewriteRun(
          groovy(
            """
              trait Greeter {
                  void greet() {
                      println("hello")
                  }
              }
              """
          )
        );
    }

    @Test
    void traitWithDefMethod() {
        rewriteRun(
          groovy(
            """
              trait Greeter {
                  def greet() {
                      "hello"
                  }
              }
              """
          )
        );
    }

    @Test
    void traitWithVarargsMethod() {
        rewriteRun(
          groovy(
            """
              trait Greeter {
                  def greet(String... names) {
                      names.join(", ")
                  }
              }
              """
          )
        );
    }

    @Test
    void traitWithGenericMethod() {
        rewriteRun(
          groovy(
            """
              trait Container {
                  def <T> T pick(List<T> items) {
                      items.first()
                  }
              }
              """
          )
        );
    }

    @Test
    void traitWithIfStatement() {
        rewriteRun(
          groovy(
            """
              trait Greeter {
                  def greet(String name) {
                      if (name) {
                          println("hi " + name)
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void traitWithForLoop() {
        rewriteRun(
          groovy(
            """
              trait Greeter {
                  def greetAll(List<String> names) {
                      for (String name : names) {
                          println(name)
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void traitWithClosureInBody() {
        rewriteRun(
          groovy(
            """
              trait Greeter {
                  def greetAll(List<String> names) {
                      names.each { println(it) }
                  }
              }
              """
          )
        );
    }

    @Test
    void traitWithGStringInBody() {
        rewriteRun(
          groovy(
            """
              trait Greeter {
                  def greet(String name) {
                      println("hello $name")
                  }
              }
              """
          )
        );
    }

    @Test
    void traitWithMultilineGString() {
        rewriteRun(
          groovy(
            """
              trait Greeter {
                  def greet(String name) {
                      println(\"\"\"
                          hello $name
                      \"\"\")
                  }
              }
              """
          )
        );
    }

    @Test
    void traitWithCastExpression() {
        rewriteRun(
          groovy(
            """
              trait Greeter {
                  def asArray(List items) {
                      items.toArray() as String[]
                  }
              }
              """
          )
        );
    }

    @Test
    void traitWithLeftShiftOperator() {
        rewriteRun(
          groovy(
            """
              trait Builder {
                  StringBuilder sb = new StringBuilder()
                  def append(String s) {
                      sb << s
                  }
              }
              """
          )
        );
    }

    @Test
    void traitImplementsInterface() {
        rewriteRun(
          groovy(
            """
              trait Greeter implements Runnable {
                  void run() {
                      println("hello")
                  }
              }
              """
          )
        );
    }

    @Test
    void traitWithAnnotatedMethod() {
        rewriteRun(
          groovy(
            """
              trait Greeter {
                  @Deprecated
                  def greet() {
                      "hello"
                  }
              }
              """
          )
        );
    }

    @Test
    void traitWithStaticMethod() {
        rewriteRun(
          groovy(
            """
              trait Greeter {
                  static String greet() {
                      "hello"
                  }
              }
              """
          )
        );
    }

    @Test
    void traitWithGenericTypeParameter() {
        rewriteRun(
          groovy(
            """
              trait Container<T> {
                  T item
              }
              """
          )
        );
    }

    @Test
    void traitMethodCallsMethodOnAbstractGetter() {
        rewriteRun(
          groovy(
            """
              trait HasItems {
                  abstract List<String> getItems()
                  void addItem(String item) {
                      items.add(item)
                  }
              }
              """
          )
        );
    }

    @Test
    void traitMethodWithDefaultParameterReferencingConstant() {
        rewriteRun(
          groovy(
            """
              class Outer {
                  static final String DEFAULT = "x"
              }
              trait Greeter {
                  String greet(String name = Outer.DEFAULT) {
                      "hello $name"
                  }
              }
              """
          )
        );
    }

    @Test
    void traitMethodWithThisFieldAssignment() {
        rewriteRun(
          groovy(
            """
              trait HasFlag {
                  boolean flag
                  void setFlag(boolean flag) {
                      this.flag = flag
                  }
              }
              """
          )
        );
    }

    @Test
    void traitMethodUsingThisClassName() {
        rewriteRun(
          groovy(
            """
              trait HasName {
                  String myName() {
                      this.class.name
                  }
              }
              """
          )
        );
    }

    @Test
    void traitMethodWithGStringPropertyInterpolation() {
        rewriteRun(
          groovy(
            """
              class Lang {
                  String name = "java"
              }
              trait LangAware {
                  abstract Lang getLanguage()
                  String pathFor(String file) {
                      "src/main/${language.name}/${file}.${language.name}"
                  }
              }
              """
          )
        );
    }

    @Test
    void traitMethodWithArrowSwitchAndQualifiedEnumCase() {
        rewriteRun(
          groovy(
            """
              enum Kind { A, B }
              trait Switcher {
                  String pick(Kind k) {
                      switch (k) {
                          case Kind.A -> "a"
                          case Kind.B -> "b"
                          default -> "?"
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void traitMethodWithDefLocalFromImplicitReceiver() {
        rewriteRun(
          groovy(
            """
              trait FileMaker {
                  abstract Object getTemporaryFolder()
                  void make(String name) {
                      def f = temporaryFolder.file(name)
                      f.toString()
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/9228")
    @Test
    void staticBlockBeforeAMethod() {
        rewriteRun(
          groovy(
            """
              trait T {
                static String X
                static {
                  X = 'x'
                }
                def m() { 1 }
              }
              """,
            spec -> spec.afterRecipe(TraitClassDeclarationTest::nothingKeptAsWhitespace)
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/9228")
    @Test
    void twoStaticBlocksBetweenMethods() {
        rewriteRun(
          groovy(
            """
              trait T {
                static String X
                static int N
                static {
                  X = 'x'
                }
                def m() { X }
                static {
                  N = 2
                }
                def n() { N }
              }
              """,
            spec -> spec.afterRecipe(TraitClassDeclarationTest::nothingKeptAsWhitespace)
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/9228")
    @Test
    void instanceInitializerBlock() {
        rewriteRun(
          groovy(
            """
              trait T {
                String y
                {
                  y = 'y'
                }
                def m() { y }
              }
              """,
            spec -> spec.afterRecipe(TraitClassDeclarationTest::nothingKeptAsWhitespace)
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/9228")
    @Test
    void privateMethod() {
        rewriteRun(
          groovy(
            """
              trait T {
                def m() {
                  n()
                }

                private n() {
                  [*:k()]
                }

                private k() {
                  [a: 1]
                }
              }
              """,
            spec -> spec.afterRecipe(TraitClassDeclarationTest::nothingKeptAsWhitespace)
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/9228")
    @Test
    void fieldInitializersInAPackagedTrait() {
        rewriteRun(
          groovy(
            """
              package a.b

              trait T {
                public static int PORT = 8080
                static String NAME = 'n'
                String s = 's'
              }
              """,
            spec -> spec.afterRecipe(TraitClassDeclarationTest::nothingKeptAsWhitespace)
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/9228")
    @Test
    void staticFinalFieldValues() {
        rewriteRun(
          groovy(
            """
              package a

              trait T {
                static final String A = "a"
                private static final int B = 8
                final static var C = \"""
                  {"c": 1}
                \"""
              }
              """,
            spec -> spec.afterRecipe(TraitClassDeclarationTest::nothingKeptAsWhitespace)
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/9228")
    @Test
    void annotationsWhoseClassesAreNotOnTheClasspath() {
        rewriteRun(
          groovy(
            """
              package a

              import spock.lang.Shared

              trait T {
                @Shared
                static List w = []

                @Autowired
                List v

                @Shared @Other
                static int n
              }
              """,
            spec -> spec.afterRecipe(TraitClassDeclarationTest::nothingKeptAsWhitespace)
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/9228")
    @Test
    void superCalls() {
        rewriteRun(
          groovy(
            """
              package a

              trait T implements U {
                void setup() {
                  super.setup()
                  super.go(1, "x")
                  def r = super.get()
                }
              }
              """,
            spec -> spec.afterRecipe(TraitClassDeclarationTest::nothingKeptAsWhitespace)
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/9228")
    @Test
    void dynamicNames() {
        rewriteRun(
          groovy(
            """
              package a

              trait T {
                static {
                  foo(undeclared)
                }

                def m() {
                  x.a().b(other).c()
                  def v = third
                }
              }
              """,
            spec -> spec.afterRecipe(TraitClassDeclarationTest::nothingKeptAsWhitespace)
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/9228")
    @Test
    void anOverloadThatForwardsToAnother() {
        rewriteRun(
          groovy(
            """
              trait T {
                private m(String a) {
                  m(a, 1)
                }

                private m(String a, int b) {
                  foo(b)
                }
              }
              """,
            spec -> spec.afterRecipe(TraitClassDeclarationTest::nothingKeptAsWhitespace)
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/9228")
    @Test
    void aParameterWithADefaultValue() {
        rewriteRun(
          groovy(
            """
              trait T {
                def m(String a, int b = 2) {
                  b
                }
              }
              """,
            spec -> spec.afterRecipe(TraitClassDeclarationTest::nothingKeptAsWhitespace)
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/9228")
    @Test
    void allOfTheseInOneTrait() {
        rewriteRun(
          groovy(
            """
              package com.example.shared

              import static com.example.Stubs.*

              trait StubServerTrait {
                static Server server

                public static int PORT = 8992

                final static var OK_RESPONSE = \"""
                  {
                    "status": "ok"
                  }
                \"""

                static {
                  server = new Server(PORT)
                  server.start()
                  println "Server started on ${server.port()}"
                }

                def stubOk() {
                  return server.stub(post("/x")
                    .willReturn(ok()
                      .withBody(OK_RESPONSE)))
                }

                private body(String action, long id) {
                  [
                    Request: [
                      *:common(id),
                      Action: action,
                    ]
                  ]
                }

                private common(long id) {
                  [Id: "$id"]
                }
              }
              """,
            spec -> spec.afterRecipe(TraitClassDeclarationTest::nothingKeptAsWhitespace)
          )
        );
    }

    /**
     * Printing back alone can't tell whether source text was parsed: text the parser never visits is kept as
     * whitespace and still prints. Each source here must leave none.
     */
    private static void nothingKeptAsWhitespace(SourceFile sf) {
        List<String> kept = new ArrayList<>();
        new JavaIsoVisitor<Integer>() {
            @Override
            public Space visitSpace(Space space, Space.Location loc, Integer p) {
                if (!space.getWhitespace().isBlank()) {
                    kept.add(loc + ": " + space.getWhitespace().strip());
                }
                for (Comment c : space.getComments()) {
                    if (!c.getSuffix().isBlank()) {
                        kept.add(loc + ": " + c.getSuffix().strip());
                    }
                }
                return space;
            }
        }.visit(sf, 0);
        assertThat(kept).as("source text kept as whitespace").isEmpty();
    }
}
