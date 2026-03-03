/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.java.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Issue;
import org.openrewrite.SourceFile;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.MinimumJava21;
import org.openrewrite.java.service.AnnotationService;
import org.openrewrite.test.RewriteTest;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

class AnnotationTest implements RewriteTest {

    @Test
    void annotationWithDefaultArgument() {
        rewriteRun(
          java(
            """
              @SuppressWarnings("ALL")
              public class A {}
              """,
                spec -> spec.afterRecipe(cu -> {
                            J.ClassDeclaration c = cu.getClasses().get(0);
                            var type = (JavaType.Class) c.getType();
                            var a = (JavaType.Annotation) type.getAnnotations().get(0);
                            assertThat(a.getValues()).hasSize(1);
                            assertThat(a.getValues().get(0).getValue()).isEqualTo(singletonList("ALL"));
                        }
                )
          )
        );
    }

    @Test
    void annotationWithArgument() {
        rewriteRun(
          java(
            """
              @SuppressWarnings(value = "ALL")
              public class A {}
              """
          )
        );
    }

    @Test
    void preserveOptionalEmptyParentheses() {
        rewriteRun(
          java(
            """
              @Deprecated ( )
              public class A {}
              """
          )
        );
    }

    @Test
    void newArrayArgument() {
        rewriteRun(
          java(
            """
              import java.lang.annotation.Target;
              import static java.lang.annotation.ElementType.*;

              @Target({ FIELD, PARAMETER })
              public @interface Annotation {}
              """
          )
        );
    }

    @Test
    void newArrayArgumentTrailingComma() {
        rewriteRun(
          java(
            """
              import java.lang.annotation.Target;
              import static java.lang.annotation.ElementType.*;

              @Target({ FIELD, PARAMETER , })
              public @interface Annotation {}
              """
          )
        );
    }

    @SuppressWarnings("FinalMethodInFinalClass")
    @Test
    void annotationsInManyLocations() {
        rewriteRun(
          java(
            """
              import java.lang.annotation.*;
              @Ho
              public @Ho final @Ho class Test {
                  @Ho private @Ho transient @Ho String s;
                  @Ho
                  public @Ho final @Ho <T> @Ho T merryChristmas() {
                      return null;
                  }
                  @Ho
                  public @Ho Test() {
                  }
              }
              @Target({ElementType.TYPE_USE, ElementType.TYPE, ElementType.FIELD})
              @interface Hos {
                  Ho[] value();
              }
              @Target({ElementType.TYPE_USE, ElementType.TYPE, ElementType.FIELD})
              @Repeatable(Hos.class)
              @interface Ho {
              }
              """
          )
        );
    }

    @Test
    void multipleAnnotations() {
        rewriteRun(
          java(
            """
              import java.lang.annotation.*;
              @B
              @C
              public class A {
              }
              @Target({ElementType.TYPE_USE, ElementType.TYPE, ElementType.FIELD})
              @interface B {
              }
              @Target({ElementType.TYPE_USE, ElementType.TYPE, ElementType.FIELD})
              @interface C {
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/377")
    @Test
    void typeParameterAnnotations() {
        rewriteRun(
          java(
            """
              import java.util.List;
              import java.lang.annotation.*;
              class TypeAnnotationTest {
                  List<@A ? extends @A String> list;

                  @Target({ ElementType.FIELD, ElementType.TYPE_USE, ElementType.TYPE_PARAMETER })
                  private @interface A {
                  }
              }
              """
          )
        );
    }

    @SuppressWarnings("FinalMethodInFinalClass")
    @Test
    void annotationsWithComments() {
        rewriteRun(
          java(
            """
              import java.lang.annotation.*;
              @Yo
              // doc
              @Ho
              public @Yo /* grumpy */ @Ho final @Yo
              // happy
              @Ho class Test {
                  @Yo /* sleepy */ @Ho private @Yo /* bashful */ @Ho transient @Yo /* sneezy */ @Ho String s;
                  @Yo /* dopey */ @Ho
                  public @Yo /* evil queen */ @Ho final @Yo /* mirror */ @Ho <T> @Yo /* apple */ @Ho T itsOffToWorkWeGo() {
                      return null;
                  }
                  @Yo /* snow white */ @Ho
                  public @Yo /* prince */ @Ho Test() {
                  }
              }
              @Target({ElementType.TYPE_USE, ElementType.TYPE, ElementType.FIELD})
              @interface Hos {
                  Ho[] value();
              }
              @Target({ElementType.TYPE_USE, ElementType.TYPE, ElementType.FIELD})
              @Repeatable(Hos.class)
              @interface Ho {
              }
              @Target({ElementType.TYPE_USE, ElementType.TYPE, ElementType.FIELD})
              @interface Yos {
                  Yo[] value();
              }
              @Target({ElementType.TYPE_USE, ElementType.TYPE, ElementType.FIELD})
              @Repeatable(Yos.class)
              @interface Yo {
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/726")
    @Test
    void annotationOnConstructorName() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.Annotation visitAnnotation(J.Annotation annotation, ExecutionContext p) {
                  if ("A".equals(annotation.getSimpleName())) {
                      //noinspection ConstantConditions
                      return null;
                  }
                  return super.visitAnnotation(annotation, p);
              }
          })),
          java(
            """
              import java.lang.annotation.*;
              public class TypeAnnotationTest {

                  public @Deprecated @A TypeAnnotationTest() {
                  }

                  @Target({ ElementType.TYPE, ElementType.TYPE_USE, ElementType.TYPE_PARAMETER })
                  private @interface A {
                  }
              }
              """,
            """
              import java.lang.annotation.*;
              public class TypeAnnotationTest {

                  public @Deprecated TypeAnnotationTest() {
                  }

                  @Target({ ElementType.TYPE, ElementType.TYPE_USE, ElementType.TYPE_PARAMETER })
                  private @interface A {
                  }
              }
              """,
            spec -> spec.afterRecipe(cu -> new JavaIsoVisitor<>() {
                @Override
                public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, Object o) {
                    AnnotationService service = service(AnnotationService.class);
                    assertThat(service.getAllAnnotations(getCursor())).hasSize(1);
                    assertThat(service.getAllAnnotations(getCursor()).get(0).getSimpleName()).isEqualTo("Deprecated");
                    return method;
                }
            }.visit(cu, 0))
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/3683")
    @Test
    void annotationAfterVariableTypePackageName() {
        rewriteRun(
          java(
            """
              import java.lang.annotation.ElementType;
              import java.lang.annotation.Retention;
              import java.lang.annotation.RetentionPolicy;
              import java.lang.annotation.Target;
              import java.util.List;

              import static java.lang.annotation.ElementType.*;

              public class A {
                @Leading java. util. @Multi1 @Multi2 List<String> l;
                @Leading java. util. @Multi1 @Multi2 List<String> m() { return null; }
              }

              @Retention(RetentionPolicy.RUNTIME)
              @Target(value={FIELD, METHOD})
              public @interface Leading {}

              @Retention(RetentionPolicy.RUNTIME)
              @Target(value=TYPE_USE)
              public @interface Multi1 {}

              @Retention(RetentionPolicy.RUNTIME)
              @Target(value=TYPE_USE)
              public @interface Multi2 {}
              """,
            spec -> spec.afterRecipe(cu -> {
                AnnotationService service = cu.service(AnnotationService.class);
                var field = (J.VariableDeclarations) cu.getClasses().get(0).getBody().getStatements().get(0);
                assertThat(service.getAllAnnotations(new Cursor(null, field))).satisfiesExactly(
                  leading -> assertThat(leading.getSimpleName()).isEqualTo("Leading")
                );
                var fieldType = (J.ParameterizedType) field.getTypeExpression();
                assertThat(fieldType).isNotNull();
                var annotatedType = (J.AnnotatedType) fieldType.getClazz();
                assertThat(service.getAllAnnotations(new Cursor(null, annotatedType))).satisfiesExactly(
                  multi1 -> assertThat(multi1.getSimpleName()).isEqualTo("Multi1"),
                  multi2 -> assertThat(multi2.getSimpleName()).isEqualTo("Multi2")
                );

                var method = (J.MethodDeclaration) cu.getClasses().get(0).getBody().getStatements().get(1);
                assertThat(service.getAllAnnotations(new Cursor(null, method))).satisfiesExactly(
                  leading -> assertThat(leading.getSimpleName()).isEqualTo("Leading")
                );
                var returnType = (J.ParameterizedType) method.getReturnTypeExpression();
                assertThat(returnType).isNotNull();
                annotatedType = (J.AnnotatedType) returnType.getClazz();
                assertThat(service.getAllAnnotations(new Cursor(null, annotatedType))).satisfiesExactly(
                  multi1 -> assertThat(multi1.getSimpleName()).isEqualTo("Multi1"),
                  multi2 -> assertThat(multi2.getSimpleName()).isEqualTo("Multi2")
                );
            })
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/3453")
    @Test
    void annotatedArrayType() {
        rewriteRun(
          java(
            """
              import java.lang.annotation.ElementType;
              import java.lang.annotation.Target;

              class TypeAnnotationTest {
                  Integer @A1 [] @A2 [ ] integers;

                  @Target(ElementType.TYPE_USE)
                  private @interface A1 {
                  }

                  @Target(ElementType.TYPE_USE)
                  private @interface A2 {
                  }
              }
              """,
                spec -> spec.afterRecipe(cu -> {
                    AtomicBoolean firstDimension = new AtomicBoolean(false);
                    AtomicBoolean secondDimension = new AtomicBoolean(false);
                    new JavaIsoVisitor<>() {
                        @Override
                        public J.ArrayType visitArrayType(J.ArrayType arrayType, Object o) {
                            if (arrayType.getElementType() instanceof J.ArrayType) {
                                if (arrayType.getAnnotations() != null && !arrayType.getAnnotations().isEmpty()) {
                                    assertThat(arrayType.getAnnotations().get(0).getAnnotationType().toString()).isEqualTo("A1");
                                    assertThat(arrayType.toString()).isEqualTo("Integer @A1 [] @A2 [ ]");
                                    firstDimension.set(true);
                                }
                            } else {
                                if (arrayType.getAnnotations() != null && !arrayType.getAnnotations().isEmpty()) {
                                    assertThat(arrayType.getAnnotations().get(0).getAnnotationType().toString()).isEqualTo("A2");
                                    assertThat(arrayType.toString()).isEqualTo("Integer @A2 [ ]");
                                    secondDimension.set(true);
                                }
                            }
                            return super.visitArrayType(arrayType, o);
                        }
                    }.visit(cu, 0);
                    assertThat(firstDimension.get()).isTrue();
                    assertThat(secondDimension.get()).isTrue();
                })
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/3453")
    @Test
    void annotationOnSecondDimension() {
        rewriteRun(
          java(
            """
              import java.lang.annotation.ElementType;
              import java.lang.annotation.Target;

              class TypeAnnotationTest {
                  Integer [] @A1 [ ] integers;

                  @Target(ElementType.TYPE_USE)
                  private @interface A1 {
                  }
              }
              """,
                spec -> spec.afterRecipe(cu -> {
                    AtomicBoolean firstDimension = new AtomicBoolean(false);
                    AtomicBoolean secondDimension = new AtomicBoolean(false);
                    new JavaIsoVisitor<>() {
                        @Override
                        public J.ArrayType visitArrayType(J.ArrayType arrayType, Object o) {
                            if (arrayType.getElementType() instanceof J.ArrayType) {
                                assertThat(arrayType.toString()).isEqualTo("Integer [] @A1 [ ]");
                                firstDimension.set(true);
                            } else {
                                assertThat(arrayType.toString()).isEqualTo("Integer @A1 [ ]");
                                secondDimension.set(true);
                            }
                            return super.visitArrayType(arrayType, o);
                        }
                    }.visit(cu, 0);
                    assertThat(firstDimension.get()).isTrue();
                    assertThat(secondDimension.get()).isTrue();
                })
          )
        );
    }

    @Test
    void recursiveElementValue() {
        rewriteRun(
          java(
            """
              import java.lang.annotation.ElementType;
              import java.lang.annotation.Target;

              @Target(ElementType.TYPE)
              @A
              private @interface A {
                  A[] value() default @A;
              }

              @A({@A, @A(@A)})
              class TypeAnnotationTest {
              }
              """,
                spec -> spec.afterRecipe(cu -> {
                    J.ClassDeclaration c = cu.getClasses().get(1);
                    var type = (JavaType.Class) c.getType();
                    var a = (JavaType.Annotation) type.getAnnotations().get(0);
                    assertThat(a.getValues()).satisfiesExactly(
                            v -> {
                                assertThat(v.getElement()).isIn(a.getMethods());
                                assertThat((List<JavaType.Annotation>) (((JavaType.Annotation.ArrayElementValue) v).getValues())).satisfiesExactly(
                                        a1 -> {
                                            assertThat(a1.getType()).isSameAs(a.getType());
                                            assertThat(a1.getValues()).isEmpty();
                                        },
                                        a2 -> {
                                            assertThat(a2.getType()).isSameAs(a.getType());
                                            assertThat(a2.getValues()).hasSize(1);
                                        }
                                );
                            }
                    );
                })
          )
        );
    }

    @MinimumJava21
    @Test // Because of `@Deprecated#forRemoval`
    void annotationElementValues() {
        JavaParser p = JavaParser.fromJavaVersion().build();
        List<SourceFile> sourceFiles = p.parse(
          """
          package a.b;
          
          public class Dummy {
              @Deprecated(since = "1.2", forRemoval = true)
              static void deprecatedWithParams() {
              }
          
              @Deprecated
              static void deprecatedWithoutParams() {
              }
          }
          """,
          """
          import a.b.Dummy;
          
          class Test {
            public void test() {
              Dummy.deprecatedWithParams();
              Dummy.deprecatedWithoutParams();
            }
          }
          """
        ).toList();
        var cu = (J.CompilationUnit) sourceFiles.get(1);

        var md = (J.MethodDeclaration) cu.getClasses().get(0).getBody().getStatements().get(0);
        var mi = (J.MethodInvocation) md.getBody().getStatements().get(0);
        var annotation = (JavaType.Annotation) mi.getMethodType().getAnnotations().get(0);

        // Thread.currentThread().stop();
        assertEquals("java.lang.Deprecated", annotation.getType().getFullyQualifiedName());
        assertEquals("since", ((JavaType.Method) annotation.getValues().get(0).getElement()).getName());
        assertEquals("1.2", annotation.getValues().get(0).getValue());
        assertEquals("forRemoval", ((JavaType.Method) annotation.getValues().get(1).getElement()).getName());
        assertEquals(Boolean.TRUE, annotation.getValues().get(1).getValue());

        // Thread.currentThread().getContextClassLoader();
        mi = (J.MethodInvocation) md.getBody().getStatements().get(1);
        annotation = (JavaType.Annotation) mi.getMethodType().getAnnotations().get(0);
        assertEquals("java.lang.Deprecated", annotation.getType().getFullyQualifiedName());
        assertTrue(annotation.getValues().isEmpty());
    }

    @Test
    void arrayTypeAnnotationElementValues() {
        rewriteRun(
          java(
            """
              import java.lang.annotation.ElementType;
              import java.lang.annotation.Target;

              @Annotation(type = int[].class)
              class Test {
              }

              @Target(ElementType.TYPE)
              @interface Annotation {
                  Class<?> type();
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                J.ClassDeclaration c = cu.getClasses().get(0);
                var type = (JavaType.Class) c.getType();
                var a = (JavaType.Annotation) type.getAnnotations().get(0);
                assertThat(a.getValues()).hasSize(1);
                var v = (JavaType.Annotation.SingleElementValue) a.getValues().get(0);
                assertThat(v.getElement()).isSameAs(a.getMethods().get(0));
                assertThat(v.getValue()).isInstanceOf(JavaType.Array.class);
                var array = (JavaType.Array) v.getValue();
                assertThat(array.getElemType()).isInstanceOf(JavaType.Primitive.class);
                assertThat(((JavaType.Primitive) array.getElemType()).getKeyword()).isEqualTo("int");
            })
          )
        );
    }

    @Test
    void modifierNoSpaceThenAnnotation() {
        rewriteRun(
          java(
            """
              public class A {
                  public@jdk.jfr.Name("A") void test() {
                  }
              }
              """
          )
        );
    }

    @Test
    void modifierWithMultipleSpaceThenAnnotation() {
        rewriteRun(
          java(
            """
              public class A {
                  public   @jdk.jfr.Name("A") void test() {
                  }
              }
              """
          )
        );
    }

    @Test
    void modifierWithMultipleSpaceThenAnnotationScenario2() {
        rewriteRun(
          java(
            """
              public class A {
                  public    @jdk.jfr.Name("A") static   @jdk.jfr.Label("2nd") void test() {
                  }
              }
              """
          )
        );
    }

    @Test
    void modifierNoSpaceThenAnnotationScenario2() {
        rewriteRun(
          java(
            """
              public class A {
                  public@jdk.jfr.Name("A") static@jdk.jfr.Label("2nd") void test() {
                  }
              }
              """
          )
        );
    }

    @Test
    void modifierNoSpaceThenAnnotationScenario3() {
        rewriteRun(
          java(
            """
              public class A {
                  public@jdk.jfr.Name("A")   static  @jdk.jfr.Label("2nd") void test() {
                  }
              }
              """
          )
        );
    }

    @Test
    void modifierNoSpaceThenMultipleAnnotation() {
        rewriteRun(
          java(
            """
              public class A {
                  public@jdk.jfr.Name("A")@jdk.jfr.Label("test") void test() {
                  }
              }
              """
          )
        );
    }

    @Test
    void multipleModifiersNoSpaceThenAnnotation() {
        rewriteRun(
          java(
            """
              public class A {
                  public static@jdk.jfr.Name("A") void test() {
                  }
              }
              """
          )
        );
    }

    @Test
    void modifierWithSpaceThenAnnotation() {
        rewriteRun(
          java(
            """
              public class A {
                  public static @jdk.jfr.Name("A") void test() {
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/5329")
    @Test
    void arraysWithAnnotations() {
        rewriteRun(
          java(
            """
            import java.lang.annotation.ElementType;
            import java.lang.annotation.Target;

            class A {
               @Target({ ElementType.TYPE_USE, ElementType.TYPE_PARAMETER })
               private static @interface C {
               }
               @Target({ ElementType.TYPE_USE, ElementType.TYPE_PARAMETER })
               private static @interface B {
               }

               Comparable<@C Object @C []> specialArray1;
               Comparable<@C Object @B []> specialArray2;
            }
            """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/3881")
    @Test
    void annotatedVarargs() {
        rewriteRun(
          java(
            """
              package com.example;

              import org.jspecify.annotations.NonNull;

              class Test {
                  void method(@NonNull String @NonNull ... args) {
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/3881")
    @Test
    void annotatedVarargsFinal() {
        rewriteRun(
          java(
            """
              package com.example;

              import org.jspecify.annotations.NonNull;

              class Test {
                  private String method(@NonNull final String @NonNull... args) {
                      return "";
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/3881")
    @Test
    void multipleParametersWithAnnotatedVarargs() {
        rewriteRun(
          java(
            """
              package com.example;

              import org.jspecify.annotations.NonNull;

              class Test {
                  public static int resolvePoolSize(@NonNull String propertyName, @NonNull String value,
                          @NonNull String @NonNull... magicValues) {
                      return 0;
                  }
              }
              """
          )
        );
    }
}
