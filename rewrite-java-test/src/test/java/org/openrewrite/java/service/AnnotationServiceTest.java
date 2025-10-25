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
                  java. lang. @Ann Integer arg;
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
                    if ("Integer".equals(fieldAccess.getSimpleName())) {
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

    @Test
    void classAnnotationInheritance() {
        rewriteRun(
          java(
            """
              import java.lang.annotation.*;

              @Retention(RetentionPolicy.RUNTIME)
              @Target(ElementType.TYPE)
              @interface BaseAnnotation {}

              @BaseAnnotation
              class BaseClass {}

              class ChildClass extends BaseClass {}
              """,
            spec -> spec.afterRecipe(cu -> new JavaIsoVisitor<Integer>() {
                @Override
                public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, Integer p) {
                    AnnotationService service = service(AnnotationService.class);
                    if ("ChildClass".equals(classDecl.getSimpleName())) {
                        // Direct getAllAnnotations should not find inherited annotations
                        assertThat(service.getAllAnnotations(getCursor())).isEmpty();

                        // But isAnnotatedWith should find it through the type hierarchy
                        assertThat(service.isAnnotatedWith(classDecl, "BaseAnnotation")).isTrue();
                        assertThat(service.annotatedWith(classDecl, "BaseAnnotation")).hasSize(1);
                    }
                    return classDecl;
                }
            }.visit(cu, 0))
          )
        );
    }

    @Test
    void classAnnotationFromInterface() {
        rewriteRun(
          java(
            """
              import java.lang.annotation.*;

              @Retention(RetentionPolicy.RUNTIME)
              @Target(ElementType.TYPE)
              @interface InterfaceAnnotation {}

              @InterfaceAnnotation
              interface MyInterface {}

              class ImplementingClass implements MyInterface {}
              """,
            spec -> spec.afterRecipe(cu -> new JavaIsoVisitor<Integer>() {
                @Override
                public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, Integer p) {
                    AnnotationService service = service(AnnotationService.class);
                    if ("ImplementingClass".equals(classDecl.getSimpleName())) {
                        assertThat(service.isAnnotatedWith(classDecl, "InterfaceAnnotation")).isTrue();
                        assertThat(service.annotatedWith(classDecl, "InterfaceAnnotation")).hasSize(1);
                    }
                    return classDecl;
                }
            }.visit(cu, 0))
          )
        );
    }

    @Test
    void methodAnnotationInheritance() {
        rewriteRun(
          java(
            """
              import java.lang.annotation.*;

              @Retention(RetentionPolicy.RUNTIME)
              @Target(ElementType.METHOD)
              @interface MethodAnnotation {}

              class BaseClass {
                  @MethodAnnotation
                  public void doSomething() {}
              }

              class ChildClass extends BaseClass {
                  @Override
                  public void doSomething() {}
              }
              """,
            spec -> spec.afterRecipe(cu -> new JavaIsoVisitor<Integer>() {
                @Override
                public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, Integer p) {
                    AnnotationService service = service(AnnotationService.class);
                    Cursor parentCursor = getCursor().getParentTreeCursor();
                    if (parentCursor.getValue() instanceof J.ClassDeclaration) {
                        J.ClassDeclaration classDecl = parentCursor.getValue();
                        if ("ChildClass".equals(classDecl.getSimpleName()) &&
                            "doSomething".equals(method.getSimpleName())) {
                            // Direct getAllAnnotations should only show @Override
                            List<J.Annotation> directAnnotations = service.getAllAnnotations(getCursor());
                            assertThat(directAnnotations).hasSize(1);
                            assertThat(directAnnotations.get(0).getSimpleName()).isEqualTo("Override");

                            // But isAnnotatedWith should find @MethodAnnotation from parent
                            assertThat(service.isAnnotatedWith(method, "MethodAnnotation")).isTrue();
                            assertThat(service.annotatedWith(method, "MethodAnnotation")).hasSize(1);
                        }
                    }
                    return method;
                }
            }.visit(cu, 0))
          )
        );
    }
}
