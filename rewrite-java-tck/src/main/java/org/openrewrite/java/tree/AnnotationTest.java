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
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.service.AnnotationService;
import org.openrewrite.test.RewriteTest;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
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
               """
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
                  if (annotation.getSimpleName().equals("A")) {
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
                J.VariableDeclarations field = (J.VariableDeclarations) cu.getClasses().get(0).getBody().getStatements().get(0);
                assertThat(service.getAllAnnotations(new Cursor(null, field))).satisfiesExactly(
                  leading -> assertThat(leading.getSimpleName()).isEqualTo("Leading")
                );
                J.ParameterizedType fieldType = (J.ParameterizedType) field.getTypeExpression();
                assertThat(fieldType).isNotNull();
                J.AnnotatedType annotatedType = (J.AnnotatedType) fieldType.getClazz();
                assertThat(service.getAllAnnotations(new Cursor(null, annotatedType))).satisfiesExactly(
                  multi1 -> assertThat(multi1.getSimpleName()).isEqualTo("Multi1"),
                  multi2 -> assertThat(multi2.getSimpleName()).isEqualTo("Multi2")
                );

                J.MethodDeclaration method = (J.MethodDeclaration) cu.getClasses().get(0).getBody().getStatements().get(1);
                assertThat(service.getAllAnnotations(new Cursor(null, method))).satisfiesExactly(
                  leading -> assertThat(leading.getSimpleName()).isEqualTo("Leading")
                );
                J.ParameterizedType returnType = (J.ParameterizedType) method.getReturnTypeExpression();
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
              """, spec -> spec.afterRecipe(cu -> {
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
              """, spec -> spec.afterRecipe(cu -> {
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
}
