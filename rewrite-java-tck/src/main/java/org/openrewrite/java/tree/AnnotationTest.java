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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Issue;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpec;

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

    @SuppressWarnings("rawtypes")
    @Disabled
    @Issue("https://github.com/openrewrite/rewrite/issues/881")
    @Test
    void annotationsInFullyQualified() {
        rewriteRun(
          java(
            """
                  package annotation.fun;
                  import java.lang.annotation.*;
                          
                  @Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD, ElementType.TYPE_USE})
                  @Retention(RetentionPolicy.RUNTIME)
                  public @interface Nullable {
                  }
              """,
            SourceSpec::skip
          ),
          java(
            """
              import annotation.fun.Nullable;
              public class AnnotationFun {
                  public void justBecauseYouCanDoesntMeanYouShould(java.util.@Nullable List myList) {
                  }
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
                          
                  public @Deprecated @A TypeAnnotationTests() {
                  }
                          
                  @Target({ ElementType.TYPE, ElementType.TYPE_USE, ElementType.TYPE_PARAMETER })
                  private @interface A {
                  }
              }
              """,
            """
              import java.lang.annotation.*;
              public class TypeAnnotationTest {
                          
                  public @Deprecated TypeAnnotationTests() {
                  }
                          
                  @Target({ ElementType.TYPE, ElementType.TYPE_USE, ElementType.TYPE_PARAMETER })
                  private @interface A {
                  }
              }
              """,
            spec -> spec.afterRecipe(cu -> new JavaIsoVisitor<>() {
                @Override
                public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, Object o) {
                    assertThat(method.getAllAnnotations()).hasSize(1);
                    assertThat(method.getAllAnnotations().get(0).getSimpleName()).isEqualTo("Deprecated");
                    return method;
                }
            }.visit(cu, 0))
          )
        );

    }

}
