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
package org.openrewrite.java.service;

import org.junit.jupiter.api.Test;
import org.openrewrite.Cursor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RewriteTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;

class AnnotationServiceTest implements RewriteTest {

    @Test
    void classAnnotations() {
        rewriteRun(
          java(
            """
              import javax.annotation.processing.Generated;
                            
              @SuppressWarnings("all")
              public @Generated("foo") class T {}
              """,
            spec -> spec.afterRecipe(cu -> new JavaIsoVisitor<Integer>() {
                @Override
                public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, Integer p) {
                    AnnotationService service = service(AnnotationService.class);
                    assertThat(service.getAllAnnotations(getCursor())).isEmpty();
                    return super.visitCompilationUnit(cu, p);
                }

                @Override
                public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, Integer p) {
                    AnnotationService service = service(AnnotationService.class);
                    assertThat(service.getAllAnnotations(getCursor())).satisfiesExactly(
                      ann0 -> assertThat(ann0.getSimpleName()).isEqualTo("SuppressWarnings"),
                      ann1 -> assertThat(ann1.getSimpleName()).isEqualTo("Generated")
                    );
                    return classDecl;
                }
            }.visit(cu, 0))
          )
        );
    }

    @Test
    void annotatedType() {
        rewriteRun(
          java(
            """
              import java.lang.annotation.*;
              
              import static java.lang.annotation.ElementType.*;
                            
              class T {
                  public @A1 Integer @A2 [] arg;
              }

              @Retention(RetentionPolicy.RUNTIME)
              @Target(value=TYPE_USE)
              @interface A1 {}
              
              @Retention(RetentionPolicy.RUNTIME)
              @Target(value=TYPE_USE)
              @interface A2 {}
              """,
            spec -> spec.afterRecipe(cu -> new JavaIsoVisitor<Integer>() {
                @Override
                public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, Integer p) {
                    AnnotationService service = service(AnnotationService.class);
                    assertThat(service.getAllAnnotations(getCursor())).isEmpty();
                    return super.visitCompilationUnit(cu, p);
                }

                @Override
                public J.AnnotatedType visitAnnotatedType(J.AnnotatedType annotatedType, Integer integer) {
                    if (annotatedType.getTypeExpression() instanceof J.AnnotatedType) {
                        AnnotationService service = service(AnnotationService.class);
                        List<J.Annotation> annotations = service.getAllAnnotations(getCursor());
                        assertThat(annotations.size()).isEqualTo(2);
                    }
                    return super.visitAnnotatedType(annotatedType, integer);
                }
            }.visit(cu, 0))
          )
        );
    }

    @Test
    void arrayTypeAnnotations() {
        rewriteRun(
          java(
            """
              import java.lang.annotation.*;
              
              import static java.lang.annotation.ElementType.*;
              
              class T {
                  Integer @A1 [] @A2 [] foo;
              }
              
              @Retention(RetentionPolicy.RUNTIME)
              @Target(value=TYPE_USE)
              @interface A1 {}
              
              @Retention(RetentionPolicy.RUNTIME)
              @Target(value=TYPE_USE)
              @interface A2 {}
              """,
            spec -> spec.afterRecipe(cu -> new JavaIsoVisitor<Integer>() {
                @Override
                public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, Integer p) {
                    AnnotationService service = service(AnnotationService.class);
                    assertThat(service.getAllAnnotations(getCursor())).isEmpty();
                    return super.visitCompilationUnit(cu, p);
                }

                @Override
                public J.ArrayType visitArrayType(J.ArrayType arrayType, Integer integer) {
                    AnnotationService service = service(AnnotationService.class);
                    if (arrayType.getElementType() instanceof J.Identifier) {
                        assertThat(service.getAllAnnotations(new Cursor(null, arrayType))).satisfiesExactly(
                          ann -> assertThat(ann.getSimpleName()).isEqualTo("A2")
                        );
                    } else if (arrayType.getElementType() instanceof J.ArrayType) {
                        assertThat(service.getAllAnnotations(new Cursor(null, arrayType))).satisfiesExactly(
                          ann -> assertThat(ann.getSimpleName()).isEqualTo("A1")
                        );
                    }
                    return super.visitArrayType(arrayType, integer);
                }
            }.visit(cu, 0))
          )
        );
    }

    @Test
    void fieldAccessAnnotations() {
        rewriteRun(
          java(
            """
              import java.lang.annotation.*;
              
              import static java.lang.annotation.ElementType.*;
                            
              class T {
                  java. lang. @Ann Map arg;
              }

              @Retention(RetentionPolicy.RUNTIME)
              @Target(value=TYPE_USE)
              @interface Ann {}
              """,
            spec -> spec.afterRecipe(cu -> new JavaIsoVisitor<Integer>() {
                @Override
                public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, Integer p) {
                    AnnotationService service = service(AnnotationService.class);
                    assertThat(service.getAllAnnotations(getCursor())).isEmpty();
                    return super.visitCompilationUnit(cu, p);
                }

                @Override
                public J.FieldAccess visitFieldAccess(J.FieldAccess fieldAccess, Integer integer) {
                    if (fieldAccess.getSimpleName().equals("Integer")) {
                        AnnotationService service = service(AnnotationService.class);
                        assertThat(service.getAllAnnotations(new Cursor(null, fieldAccess))).satisfiesExactly(
                          ann -> assertThat(ann.getSimpleName()).isEqualTo("Ann")
                        );
                    }
                    return super.visitFieldAccess(fieldAccess, integer);
                }
            }.visit(cu, 0))
          )
        );
    }
}
