/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.java;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;

class ReplaceConstantWithAnotherConstantTest implements RewriteTest {

    @DocumentExample
    @Test
    void qualifyWhereALocalHasTheNewName() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceConstantWithAnotherConstant("java.io.File.pathSeparator", "java.io.File.separator")),
          java(
            """
              import static java.io.File.pathSeparator;

              class Test {
                  void foo(String separator) {
                      System.out.println(pathSeparator + separator);
                  }
              }
              """,
            """
              import java.io.File;

              class Test {
                  void foo(String separator) {
                      System.out.println(File.separator + separator);
                  }
              }
              """
          )
        );
    }

    @Test
    void bareNameWhereTheClassDeclaresTheNewConstant() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceConstantWithAnotherConstant("java.io.File.pathSeparator", "com.acme.Test.SEPARATOR")),
          java(
            """
              package com.acme;

              import static java.io.File.pathSeparator;

              class Test {
                  static final String SEPARATOR = ":";

                  String foo() {
                      return pathSeparator;
                  }
              }
              """,
            """
              package com.acme;

              class Test {
                  static final String SEPARATOR = ":";

                  String foo() {
                      return SEPARATOR;
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceConstant() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceConstantWithAnotherConstant("java.io.File.pathSeparator", "java.io.File.separator")),
          java(
            """
              import java.io.File;

              import static java.io.File.pathSeparator;

              class Test {
                  Object o = File.pathSeparator;
                  void foo() {
                      System.out.println(pathSeparator);
                      System.out.println(java.io.File.pathSeparator);
                  }
              }
              """,
            """
              import java.io.File;

              import static java.io.File.separator;

              class Test {
                  Object o = File.separator;
                  void foo() {
                      System.out.println(separator);
                      System.out.println(File.separator);
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceConstantInAnnotation() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceConstantWithAnotherConstant("java.io.File.pathSeparator", "com.constant.B.PATH_SEPARATOR")),
          java(
            """
              package com.constant;
              public class B {
                  public static final String PATH_SEPARATOR = ":";
              }
              """
          ),
          java(
            """
              import java.io.File;

              @SuppressWarnings(File.pathSeparator)
              class Test {
                  @SuppressWarnings(value = File.pathSeparator)
                  void foo() {
                      System.out.println("Annotation");
                  }
              }
              """,
            """
              import com.constant.B;

              @SuppressWarnings(B.PATH_SEPARATOR)
              class Test {
                  @SuppressWarnings(value = B.PATH_SEPARATOR)
                  void foo() {
                      System.out.println("Annotation");
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceEnumConstant() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceConstantWithAnotherConstant("com.constant.OldValue.A", "com.constant.NewValue.B")),
          java(
            """
              package com.constant;
              public enum OldValue {
                  A
              }
              """
          ),
          java(
            """
              package com.constant;
              public enum NewValue {
                  B
              }
              """
          ),
          java(
            """
              import com.constant.OldValue;

              class Test {
                  void foo() {
                      Object test = OldValue.A;
                  }
              }
              """,
            """
              import com.constant.NewValue;

              class Test {
                  void foo() {
                      Object test = NewValue.B;
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceStaticallyImportedEnumConstant() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceConstantWithAnotherConstant("com.constant.OldValue.A", "com.constant.NewValue.B")),
          java(
            """
              package com.constant;
              public enum OldValue {
                  A
              }
              """
          ),
          java(
            """
              package com.constant;
              public enum NewValue {
                  B
              }
              """
          ),
          java(
            """
              import static com.constant.OldValue.A;

              class Test {
                  void foo() {
                      Object test = A;
                  }
              }
              """,
            """
              import static com.constant.NewValue.B;

              class Test {
                  void foo() {
                      Object test = B;
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceConstantOfItsOwnClassWithConstantOfThatClassElsewhere() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceConstantWithAnotherConstant(
              "com.constant.Strategy.SNAKE_CASE", "com.constant.Strategies.SNAKE_CASE"))
            .parser(JavaParser.fromJavaVersion().dependsOn(
              """
                package com.constant;
                public class Strategy {
                    public static final Strategy SNAKE_CASE = new Strategy();
                }
                """,
              """
                package com.constant;
                public class Strategies {
                    public static final Strategy SNAKE_CASE = new Strategy();
                }
                """
            )),
          java(
            """
              import com.constant.Strategy;

              class Test {
                  Strategy s = Strategy.SNAKE_CASE;
              }
              """,
            """
              import com.constant.Strategies;
              import com.constant.Strategy;

              class Test {
                  Strategy s = Strategies.SNAKE_CASE;
              }
              """
          )
        );
    }

    @Test
    void replacedEnumConstantIsAnEnumConstantToTheNextReplacement() {
        rewriteRun(
          spec -> spec.recipes(
              new ReplaceConstantWithAnotherConstant("old.core.GeneratorFeature.QUOTE_FIELD_NAMES", "old.core.json.WriteFeature.QUOTE_FIELD_NAMES"),
              new ReplaceConstantWithAnotherConstant("old.core.json.WriteFeature.QUOTE_FIELD_NAMES", "neu.core.json.WriteFeature.QUOTE_PROPERTY_NAMES"),
              new ChangePackage("old.core", "neu.core", true))
            .parser(JavaParser.fromJavaVersion().dependsOn(
              """
                package old.core;
                public enum GeneratorFeature {
                    QUOTE_FIELD_NAMES
                }
                """,
              """
                package old.core.json;
                public enum WriteFeature {
                    QUOTE_FIELD_NAMES
                }
                """,
              """
                package neu.core.json;
                public enum WriteFeature {
                    QUOTE_PROPERTY_NAMES
                }
                """
            )),
          java(
            """
              import old.core.GeneratorFeature;

              class Test {
                  Object o = GeneratorFeature.QUOTE_FIELD_NAMES;
              }
              """,
            """
              import neu.core.json.WriteFeature;

              class Test {
                  Object o = WriteFeature.QUOTE_PROPERTY_NAMES;
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/pull/3448")
    @Test
    void replaceConstantInCurlyBracesInAnnotation() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceConstantWithAnotherConstant("java.io.File.pathSeparator", "java.io.File.separator"))
            .parser(JavaParser.fromJavaVersion().classpath("guava")),
          java(
            """
              import java.io.File;

              class Test {
                  @SuppressWarnings({File.pathSeparator})
                  private String bar;
              }
              """,
            """
              import java.io.File;

              class Test {
                  @SuppressWarnings({File.separator})
                  private String bar;
              }
              """
          )
        );
    }

    @Test
    void removeTopLevelClassImport() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceConstantWithAnotherConstant("foo.Bar.Baz.QUX", "Test.FOO")),
          java(
            """
              package foo;

              public class Bar {
                  public static class Baz {
                      public static final String QUX = "QUX";
                  }
              }
              """
          ),
          java(
            """
              import foo.Bar;

              class Test {
                  static final String FOO = "foo";
                  Object o = Bar.Baz.QUX;
              }
              """,
            """
              class Test {
                  static final String FOO = "foo";
                  Object o = Test.FOO;
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/3555")
    @Test
    void replaceConstantForAnnotatedParameter() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceConstantWithAnotherConstant("java.io.File.pathSeparator", "java.io.File.separator")),
          java(
            """
              import java.io.File;

              class Test {
                  void foo(@SuppressWarnings(value = File.pathSeparator) String param) {
                      System.out.println(param);
                  }
              }
              """,
            """
              import java.io.File;

              class Test {
                  void foo(@SuppressWarnings(value = File.separator) String param) {
                      System.out.println(param);
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/5174")
    @Test
    void shouldUpdateWithinMethod() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceConstantWithAnotherConstant("foo.Bar.QUX1", "foo.Bar.QUX2")),
          java(
            """
              package foo;

              public class Bar {
                  public static final String QUX1 = "QUX1";
                  public static final String QUX2 = "QUX2";
              }
              """
          ),
          java(
            """
              import static foo.Bar.QUX1;

              class Test {
                  String out = QUX1;
                  void a() {
                      String in = QUX1;
                  }
              }
              """,
            """
              import static foo.Bar.QUX2;

              class Test {
                  String out = QUX2;
                  void a() {
                      String in = QUX2;
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/5174")
    @Test
    void shouldUpdateWithinMethod2() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceConstantWithAnotherConstant("foo.Bar.QUX1", "foo.Bar.QUX2")),
          java(
            """
              package foo;

              public class Bar {
                  public static final String QUX1 = "QUX1";
                  public static final String QUX2 = "QUX2";
              }
              """
          ),
          java(
            """
              import foo.Bar;

              import static foo.Bar.QUX2;

              class Test {
                  public static final String QUX1 = "QUX111"; // the same name as Bar.QUX1

                  void a() {
                      String in = Bar.QUX1;
                      String in2 = QUX2;
                  }
              }
              """,
            """
              import foo.Bar;

              import static foo.Bar.QUX2;

              class Test {
                  public static final String QUX1 = "QUX111"; // the same name as Bar.QUX1

                  void a() {
                      String in = Bar.QUX2;
                      String in2 = QUX2;
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/5224")
    @Test
    void shouldFullyQualifyWhenNewTypeIsAmbiguous() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceConstantWithAnotherConstant("foo1.Bar.QUX1", "foo2.Bar.QUX1")),
          java(
            """
              package foo1;

              public class Bar {
                  public static final String QUX1 = "QUX1_FROM_FOO1";
                  public static final String QUX2 = "QUX1_FROM_FOO2";
              }
              """
          ),
          java(
            """
              package foo2;

              public class Bar {
                  public static final String QUX1 = "QUX1_FROM_FOO1";
              }
              """
          ),
          java(
            """
              import foo1.Bar;

              class Test {
                  void a() {
                      System.out.println(Bar.QUX1);
                      System.out.println(Bar.QUX2);
                  }
              }
              """,
            """
              import foo1.Bar;

              class Test {
                  void a() {
                      System.out.println(foo2.Bar.QUX1);
                      System.out.println(Bar.QUX2);
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/5224")
    @Test
    void shouldFullyQualifyWhenNewTypeIsAmbiguous2() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceConstantWithAnotherConstant("foo1.Bar.QUX1", "foo3.Bar.QUX2")),
          java(
            """
              package foo1;

              public class Bar {
                  public static final String QUX1 = "QUX_FROM_FOO1";
              }
              """
          ),
          java(
            """
              package foo2;

              public class Bar {
                  public static final String QUX2 = "QUX_FROM_FOO2";
              }
              """
          ),
          java(
            """
              package foo3;

              public class Bar {
                  public static final String QUX2 = "QUX_FROM_FOO3";
              }
              """
          ),
          java(
            """
              import static foo1.Bar.QUX1;
              import static foo2.Bar.QUX2;

              class Test {
                  void a() {
                      System.out.println(QUX1);
                      System.out.println(QUX2);
                  }
              }
              """,
            """
              import static foo2.Bar.QUX2;

              class Test {
                  void a() {
                      System.out.println(foo3.Bar.QUX2);
                      System.out.println(QUX2);
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/5224")
    @Test
    void shouldFullyQualifyWhenNewTypeIsAmbiguous3() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceConstantWithAnotherConstant("foo1.Bar.QUX1", "foo2.Bar.QUX1")),
          java(
            """
              package foo1;

              public class Bar {
                  public static final String QUX1 = "QUX_FROM_FOO1";
              }
              """
          ),
          java(
            """
              package foo2;

              public class Bar {
                  public static final String QUX1 = "QUX_FROM_FOO2";
              }
              """
          ),
          java(
            """
              import static foo1.Bar.QUX1;

              class Test {
                  void a() {
                      System.out.println(QUX1);
                  }
              }
              """,
            """
              import static foo2.Bar.QUX1;

              class Test {
                  void a() {
                      System.out.println(QUX1);
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceWithConstantOfNestedClass() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceConstantWithAnotherConstant("a.Old.A", "b.Outer$Inner.B"))
            .parser(JavaParser.fromJavaVersion().dependsOn(OLD, OUTER)),
          java(
            """
              import a.Old;

              class Test {
                  Object o = Old.A;
              }
              """,
            """
              import b.Outer;

              class Test {
                  Object o = Outer.Inner.B;
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                J.VariableDeclarations o = (J.VariableDeclarations) cu.getClasses().getFirst().getBody().getStatements().getFirst();
                J.FieldAccess constant = (J.FieldAccess) o.getVariables().getFirst().getInitializer();
                assertThat(TypeUtils.asFullyQualified(constant.getName().getFieldType().getOwner()).getFullyQualifiedName()).isEqualTo("b.Outer$Inner");
                J.FieldAccess inner = (J.FieldAccess) constant.getTarget();
                assertThat(inner.getSimpleName()).isEqualTo("Inner");
                assertThat(TypeUtils.isOfClassType(inner.getType(), "b.Outer$Inner")).isTrue();
                assertThat(TypeUtils.isOfClassType(inner.getName().getType(), "b.Outer$Inner")).isTrue();
                J.Identifier outer = (J.Identifier) inner.getTarget();
                assertThat(outer.getSimpleName()).isEqualTo("Outer");
                assertThat(TypeUtils.isOfClassType(outer.getType(), "b.Outer")).isTrue();
            })
          )
        );
    }

    @Test
    void replaceWithConstantOfNestedClassWrittenWithDots() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceConstantWithAnotherConstant("a.Old.A", "b.Outer.Inner.B"))
            .parser(JavaParser.fromJavaVersion().dependsOn(OLD, OUTER)),
          java(
            """
              import a.Old;

              class Test {
                  Object o = Old.A;
              }
              """,
            """
              import b.Outer;

              class Test {
                  Object o = Outer.Inner.B;
              }
              """
          )
        );
    }

    @Test
    void replaceFullyQualifiedReferenceWithConstantOfNestedClass() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceConstantWithAnotherConstant("a.Old.A", "b.Outer$Inner.B"))
            .parser(JavaParser.fromJavaVersion().dependsOn(OLD, OUTER)),
          java(
            """
              class Test {
                  Object o = a.Old.A;
              }
              """,
            """
              import b.Outer;

              class Test {
                  Object o = Outer.Inner.B;
              }
              """
          )
        );
    }

    @Test
    void replaceStaticallyImportedWithConstantOfNestedClass() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceConstantWithAnotherConstant("a.Old.A", "b.Outer$Inner.B"))
            .parser(JavaParser.fromJavaVersion().dependsOn(OLD, OUTER)),
          java(
            """
              import static a.Old.A;

              class Test {
                  Object o = A;
              }
              """,
            """
              import static b.Outer.Inner.B;

              class Test {
                  Object o = B;
              }
              """
          )
        );
    }

    @Test
    void replaceConstantOfNestedClassWithConstantOfNestedClass() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceConstantWithAnotherConstant("a.OldOuter$OldInner.A", "b.Outer$Inner.B"))
            .parser(JavaParser.fromJavaVersion().dependsOn(
              """
                package a;
                public class OldOuter {
                    public static class OldInner {
                        public static final String A = "a";
                    }
                }
                """,
              OUTER
            )),
          java(
            """
              import a.OldOuter;

              class Test {
                  Object o = OldOuter.OldInner.A;
              }
              """,
            """
              import b.Outer;

              class Test {
                  Object o = Outer.Inner.B;
              }
              """
          ),
          java(
            """
              import a.OldOuter.OldInner;

              class Test2 {
                  Object o = OldInner.A;
              }
              """,
            """
              import b.Outer;

              class Test2 {
                  Object o = Outer.Inner.B;
              }
              """
          )
        );
    }

    @Test
    void fullyQualifyWhenOuterClassOfNewConstantIsAmbiguous() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceConstantWithAnotherConstant("a.Old.A", "b.Outer$Inner.B"))
            .parser(JavaParser.fromJavaVersion().dependsOn(
              OLD,
              OUTER,
              """
                package c;
                public class Outer {
                }
                """,
              """
                package d;
                public class D {
                    public static class Outer {
                    }
                }
                """
            )),
          java(
            """
              import a.Old;
              import c.Outer;

              class Test {
                  Outer outer;
                  Object o = Old.A;
              }
              """,
            """
              import c.Outer;

              class Test {
                  Outer outer;
                  Object o = b.Outer.Inner.B;
              }
              """
          ),

          java(
            """
              import a.Old;

              class Test2 {
                  Object o = Old.A;

                  static class Outer {
                  }
              }
              """,
            """
              class Test2 {
                  Object o = b.Outer.Inner.B;

                  static class Outer {
                  }
              }
              """
          ),

          java(
            """
              package c;

              import a.Old;

              class Test3 {
                  Outer outer;
                  Object o = Old.A;
              }
              """,
            """
              package c;

              class Test3 {
                  Outer outer;
                  Object o = b.Outer.Inner.B;
              }
              """
          ),

          java(
            """
              import a.Old;

              class Test4 {
                  void m() {
                      class Outer {
                      }
                      Object o = Old.A;
                  }
              }
              """,
            """
              class Test4 {
                  void m() {
                      class Outer {
                      }
                      Object o = b.Outer.Inner.B;
                  }
              }
              """
          ),

          java(
            """
              import a.Old;
              import static d.D.Outer;

              class Test5 {
                  Object o = Old.A;
              }
              """,
            """
              import static d.D.Outer;

              class Test5 {
                  Object o = b.Outer.Inner.B;
              }
              """
          ),

          java(
            """
              import a.Old;

              class Test6<Outer> {
                  Object o = Old.A;
              }
              """,
            """
              class Test6<Outer> {
                  Object o = b.Outer.Inner.B;
              }
              """
          )
        );
    }

    @Test
    void importOuterClassOnceTheOldOwnerOfThatNameIsNoLongerImported() {
        rewriteRun(
          spec -> spec.recipes(
              new ReplaceConstantWithAnotherConstant("a.Holder.X", "b.Holder$Kind.X"),
              new ReplaceConstantWithAnotherConstant("a.Holder.Y", "b.Holder$Kind.Y"))
            .parser(JavaParser.fromJavaVersion().dependsOn(
              """
                package a;
                public class Holder {
                    public static final int X = 0;
                    public static final int Y = 1;
                }
                """,
              """
                package b;
                public class Holder {
                    public static class Kind {
                        public static final int X = 0;
                        public static final int Y = 1;
                    }
                }
                """
            )),
          java(
            """
              import a.Holder;

              class Test {
                  Object x = Holder.X;
                  Object y = Holder.Y;
              }
              """,
            """
              import b.Holder;

              class Test {
                  Object x = Holder.Kind.X;
                  Object y = Holder.Kind.Y;
              }
              """
          ),

          java(
            """
              import a.Holder;

              class Test2 {
                  Object x = Holder.X;
              }
              """,
            """
              import b.Holder;

              class Test2 {
                  Object x = Holder.Kind.X;
              }
              """
          )
        );
    }

    @Test
    void replaceConstantAccessedThroughAnExpression() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceConstantWithAnotherConstant("a.Old.A", "b.Outer$Inner.B"))
            .parser(JavaParser.fromJavaVersion().dependsOn(OLD, OUTER)),
          java(
            """
              import a.Old;

              class Test {
                  Old old() {
                      return null;
                  }
                  Object o = old().A;
              }
              """,
            """
              import a.Old;
              import b.Outer;

              class Test {
                  Old old() {
                      return null;
                  }
                  Object o = Outer.Inner.B;
              }
              """
          )
        );
    }

    @Test
    void staticallyImportConstantOfNestedClassDespiteOuterClassNameClash() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceConstantWithAnotherConstant("a.Old.A", "b.Outer$Inner.B"))
            .parser(JavaParser.fromJavaVersion().dependsOn(
              OLD,
              OUTER,
              """
                package c;
                public class Outer {
                }
                """
            )),
          java(
            """
              import c.Outer;

              import static a.Old.A;

              class Test {
                  Outer outer;
                  Object o = A;
              }
              """,
            """
              import c.Outer;

              import static b.Outer.Inner.B;

              class Test {
                  Outer outer;
                  Object o = B;
              }
              """
          )
        );
    }

    @Test
    void importOuterClassOfStaticallyImportedConstantWhoseNestedOwnerNameIsImported() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceConstantWithAnotherConstant("a.Old.A", "b.Outer$Inner.B"))
            .parser(JavaParser.fromJavaVersion().dependsOn(
              OLD,
              OUTER,
              """
                package c;
                public class Outer {
                    public static class Inner {
                    }
                }
                """
            )),
          java(
            """
              import c.Outer.Inner;

              import static a.Old.A;

              class Test {
                  Inner inner;
                  Object o = A;
              }
              """,
            """
              import b.Outer;
              import c.Outer.Inner;

              class Test {
                  Inner inner;
                  Object o = Outer.Inner.B;
              }
              """
          )
        );
    }

    @Nested
    class InheritedConstant implements RewriteTest {
        @Override
        public void defaults(RecipeSpec spec) {
            spec.recipe(new ReplaceConstantWithAnotherConstant("example.Settings.OLD", "example.Settings.NEW"))
              .parser(JavaParser.fromJavaVersion().dependsOn(
                """
                  package example;
                  public interface Settings {
                      String OLD = "key";
                      String NEW = "key";
                      String OTHER = "other";
                  }
                  """,
                """
                  package example;
                  public class Base {
                      public static final String BASE_KEY = "base";
                      public static String helper() { return "helper"; }
                  }
                  """,
                """
                  package example;
                  public class Environment extends Base implements Settings {
                      public static Environment create() { return null; }
                  }
                  """,
                """
                  package example;
                  public class Outer {
                      public static class Env implements Settings {}
                  }
                  """
              ));
        }

        @ParameterizedTest
        @CsvSource({
          "Environment, Environment",
          "Outer, Outer.Env",
          "Environment, example.Environment"
        })
        void removeUnusedSubtypeImport(String importedType, String qualifier) {
            rewriteRun(
              java(
                """
                  import example.%s;

                  class Test {
                      String key = %s.OLD;
                  }
                  """.formatted(importedType, qualifier),
                """
                  import example.Settings;

                  class Test {
                      String key = Settings.NEW;
                  }
                  """
              )
            );
        }

        @Test
        void keepStaticImportOfNestedSubtypeUsedElsewhere() {
            rewriteRun(
              java(
                """
                  import static example.Outer.Env;

                  class Test {
                      Env env;
                      String key = Env.OLD;
                  }
                  """,
                """
                  import example.Settings;

                  import static example.Outer.Env;

                  class Test {
                      Env env;
                      String key = Settings.NEW;
                  }
                  """
              )
            );
        }

        @Test
        void keepStaticImportThroughSubtype() {
            rewriteRun(
              java(
                """
                  import example.Environment;

                  import static example.Environment.OTHER;

                  class Test {
                      String key = Environment.OLD;
                      String other = OTHER;
                  }
                  """,
                """
                  import example.Settings;

                  import static example.Environment.OTHER;

                  class Test {
                      String key = Settings.NEW;
                      String other = OTHER;
                  }
                  """
              )
            );
        }

        @Test
        void keepStaticWildcardForInheritedFieldsAndMethod() {
            rewriteRun(
              java(
                """
                  import example.Environment;

                  import static example.Environment.*;

                  class Test {
                      String key = Environment.OLD;
                      String other = OTHER + helper() + BASE_KEY;
                  }
                  """,
                """
                  import example.Settings;

                  import static example.Environment.*;

                  class Test {
                      String key = Settings.NEW;
                      String other = OTHER + helper() + BASE_KEY;
                  }
                  """
              )
            );
        }

        @Test
        void importNewOwnerWithSameNameAsSubtype() {
            rewriteRun(
              spec -> spec.recipe(new ReplaceConstantWithAnotherConstant("a.Base.OLD", "b.Config.NEW"))
                .parser(JavaParser.fromJavaVersion().dependsOn(
                  """
                    package a;
                    public class Base {
                        public static final String OLD = "key";
                    }
                    """,
                  """
                    package a;
                    public class Config extends Base {}
                    """,
                  """
                    package b;
                    public class Config {
                        public static final String NEW = "key";
                    }
                    """
                )),
              java(
                """
                  import a.Config;

                  class Test {
                      String key = Config.OLD;
                  }
                  """,
                """
                  import b.Config;

                  class Test {
                      String key = Config.NEW;
                  }
                  """
              )
            );
        }

        @Test
        void replaceConstantReadThroughThis() {
            rewriteRun(
              java(
                """
                  import example.Settings;

                  class Test implements Settings {
                      String key = this.OLD;
                  }
                  """,
                """
                  import example.Settings;

                  class Test implements Settings {
                      String key = Settings.NEW;
                  }
                  """
              )
            );
        }

        @Test
        void keepSubtypeImportUsedElsewhere() {
            rewriteRun(
              java(
                """
                  import example.Environment;

                  class Test {
                      Environment environment;
                      String key = Environment.OLD;
                  }
                  """,
                """
                  import example.Environment;
                  import example.Settings;

                  class Test {
                      Environment environment;
                      String key = Settings.NEW;
                  }
                  """
              )
            );
        }

        @Test
        void keepUnrelatedImportWhenAccessedThroughInstance() {
            rewriteRun(
              java(
                """
                  import example.Environment;

                  import static example.Environment.create;

                  class Test {
                      String key() {
                          var instance = create();
                          return instance.OLD;
                      }
                  }
                  """,
                """
                  import example.Environment;
                  import example.Settings;

                  import static example.Environment.create;

                  class Test {
                      String key() {
                          var instance = create();
                          return Settings.NEW;
                      }
                  }
                  """
              )
            );
        }
    }

    private static final String OLD = """
      package a;
      public class Old {
          public static final String A = "a";
      }
      """;

    private static final String OUTER = """
      package b;
      public class Outer {
          public static class Inner {
              public static final String B = "b";
          }
      }
      """;
}
