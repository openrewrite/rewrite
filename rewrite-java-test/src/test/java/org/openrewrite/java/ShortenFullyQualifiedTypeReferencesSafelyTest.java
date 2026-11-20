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
package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Tree;
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.test.RecipeQualityValidation;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.List;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

class ShortenFullyQualifiedTypeReferencesSafelyTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ShortenFullyQualifiedTypeReferencesSafely());
    }

    @DocumentExample
    @Test
    void shortenJava() {
        rewriteRun(
          java(
            """
              class T {
                  java.util.List<String> list;
              }
              """,
            """
              import java.util.List;

              class T {
                  List<String> list;
              }
              """
          )
        );
    }

    @Test
    void useExplicitImportEvenWhenAWildcardIsPresent() {
        rewriteRun(
          // The explicit import intentionally disambiguates a same-package type absent from the source-set index.
          spec -> spec.parser(JavaParser.fromJavaVersion().dependsOn("package a; public class List {}"))
            .recipeQualityValidation(RecipeQualityValidation.builder().removeUnusedImports(false).build()),
          java(
            """
              package a;

              import java.util.*;

              class T {
                  java.util.List<String> values;
                  Set<String> set;
              }
              """,
            """
              package a;

              import java.util.*;

              import java.util.List;

              class T {
                  List<String> values;
                  Set<String> set;
              }
              """
          )
        );
    }

    @Test
    void doNotFoldExistingImports() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().dependsOn("package a; public class List {}")),
          java(
            """
              package a;

              import java.util.Collection;
              import java.util.List;
              import java.util.Map;
              import java.util.Set;

              class T {
                  Collection<?> collection;
                  List<?> list;
                  Map<?, ?> map;
                  Set<?> set;
                  java.util.Queue<?> queue;
              }
              """,
            """
              package a;

              import java.util.Collection;
              import java.util.List;
              import java.util.Map;
              import java.util.Queue;
              import java.util.Set;

              class T {
                  Collection<?> collection;
                  List<?> list;
                  Map<?, ?> map;
                  Set<?> set;
                  Queue<?> queue;
              }
              """
          )
        );
    }

    @Test
    void preserveJavaLangAnnotationWhenSamePackageMaySupplyItsName() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().dependsOn("package a; public @interface Deprecated {}")),
          java(
            """
              package a;

              @java.lang.Deprecated
              class T {}
              """
          )
        );
    }

    @Test
    void preserveTypeAnnotationLocation() {
        rewriteRun(
          java(
            """
              import java.lang.annotation.*;

              @Retention(RetentionPolicy.RUNTIME)
              @Target({ElementType.FIELD, ElementType.TYPE_USE})
              @interface Both {}

              class T {
                  java.util.@Both List<?> field;
              }
              """
          )
        );
    }

    @Test
    void preserveCommentsInQualifiedNames() {
        rewriteRun(
          java(
            """
              class T {
                  java./* explanation */util.List<?> field;
              }
              """
          )
        );
    }

    @Test
    void keepQualifiedTypeWhenDependencyMaySupplyAnInheritedMemberType() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().dependsOn(
            """
              package a;
              public class Base {
                  public static class List {}
              }
              """
          )),
          java(
            """
              import a.Base;

              class T extends Base {
                  java.util.List value;
              }
              """
          )
        );
    }

    @Test
    void anotherClassCannotProveAnInheritedNameIsFree() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().dependsOn(
            """
              package a;
              public class Base {
                  public static class List {}
              }
              """
          )),
          java(
            """
              import a.Base;

              import java.util.List;

              class U {
                  List<?> value;
              }
              class T extends Base {
                  java.util.List value;
              }
              """
          )
        );
    }

    @Test
    void existingSimpleNameInClassProvesInheritedNameIsFree() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().dependsOn(
            """
              package a;
              public class Base {}
              """
          )),
          java(
            """
              import a.Base;

              import java.util.List;

              class T extends Base {
                  List<?> first;
                  java.util.List<?> second;
              }
              """,
            """
              import a.Base;

              import java.util.List;

              class T extends Base {
                  List<?> first;
                  List<?> second;
              }
              """
          )
        );
    }

    @Test
    void partialDependencyIndexCannotProveAnInheritedNameIsFree() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().dependsOn(
            """
              package a;
              public class Base {
                  public static class List {}
              }
              """
          )).beforeRecipe(sources -> sources.replaceAll(source -> source.withMarkers(source.getMarkers().add(
            new JavaSourceSet(Tree.randomId(), "main", singletonList(JavaType.ShallowClass.build("a.Base")),
              singletonMap("a:base:1", singletonList(JavaType.ShallowClass.build("a.Base"))))
          )))),
          java(
            """
              import a.Base;

              class T extends Base {
                  java.util.List value;
              }
              """
          )
        );
    }

    @Test
    void dependencyIndexRetainsAConflictingInheritedName() {
        List<JavaType.FullyQualified> types = List.of(
          JavaType.ShallowClass.build("a.Base"), JavaType.ShallowClass.build("a.Base$List"));
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().dependsOn(
            """
              package a;
              public class Base {
                  public static class List {}
              }
              """
          )).beforeRecipe(sources -> sources.replaceAll(source -> source.withMarkers(source.getMarkers().add(
            new JavaSourceSet(Tree.randomId(), "main", types, singletonMap("a:base:1", types))
          )))),
          java(
            """
              import a.Base;

              class T extends Base {
                  java.util.List value;
              }
              """
          )
        );
    }

    @Test
    void dirtyDependencyIndexCannotProveTheNameIsFree() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().dependsOn(
            """
              package a;
              public class Base {
                  public static class List {}
              }
              """
          )).beforeRecipe(sources -> sources.replaceAll(source -> source.withMarkers(source.getMarkers().add(
            new JavaSourceSet(Tree.randomId(), "main", singletonList(JavaType.ShallowClass.build("a.Base")),
              singletonMap("a:base:1", singletonList(JavaType.ShallowClass.build("a.Base"))))
          )))).recipes(
            toRecipe(() -> new JavaIsoVisitor<>() {
                @Override
                public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
                    JavaSourceSet.markDirty(ctx, "changed-dependencies");
                    return cu;
                }
            }),
            new ShortenFullyQualifiedTypeReferencesSafely()
          ),
          java(
            """
              import a.Base;

              class T extends Base {
                  java.util.List value;
              }
              """
          )
        );
    }

    @Test
    void riskyUsePreventsAnImportElsewhereFromRewritingIt() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().dependsOn(
            """
              package a;
              public class Base {
                  public static class List {}
              }
              """
          )),
          java(
            """
              import a.Base;

              class T extends Base {
                  java.util.List value;
              }
              class U {
                  java.util.List<?> value;
              }
              """
          )
        );
    }

    @Test
    void sourceDeclarationProvesTheNameIsFree() {
        rewriteRun(
          java(
            """
              class Base {}
              class T extends Base {
                  java.util.List<?> value;
              }
              """,
            """
              import java.util.List;

              class Base {}
              class T extends Base {
                  List<?> value;
              }
              """
          )
        );
    }

    @Test
    void sourceDeclarationInAnotherFileProvesTheNameIsFree() {
        rewriteRun(
          java(
            """
              package a;
              public class Base {}
              """
          ),
          java(
            """
              import a.Base;

              class T extends Base {
                  java.util.List<?> value;
              }
              """,
            """
              import a.Base;

              import java.util.List;

              class T extends Base {
                  List<?> value;
              }
              """
          )
        );
    }

    @Test
    void resourceFixtureIsUnchanged() {
        rewriteRun(
          java(
            """
              class T {
                  java.util.List<?> values;
              }
              """,
            spec -> spec.path("module/src/recipeRuntimeCheck/resources/T.java")
          )
        );
    }

    @Test
    void evidenceSourceIsUnchanged() {
        rewriteRun(
          java(
            """
              class T {
                  java.util.List<?> values;
              }
              """,
            spec -> spec.path("docs/evidence/T.java")
          )
        );
    }

    @Test
    void additionalExclusionIsHonored() {
        rewriteRun(
          spec -> spec.recipe(new ShortenFullyQualifiedTypeReferencesSafely("**/generated/Model.java")),
          java(
            """
              class T {
                  java.util.List<?> values;
              }
              """,
            spec -> spec.path("generated/Model.java")
          )
        );
    }

    @Test
    void generatedHeaderIsUnchanged() {
        rewriteRun(
          java(
            """
              // Generated by a source generator. Do not edit.
              class T {
                  java.util.List<?> values;
              }
              """
          )
        );
    }

    @Test
    void preserveLombokBuilderQualificationEvenWithoutAnnotationArguments() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().dependsOn(
            """
              package lombok;
              public @interface Builder {}
              """
          )),
          java(
            """
              @lombok.Builder
              class T {}
              """
          )
        );
    }

    @Test
    void variableNamesDoNotProveATypeBinding() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().dependsOn(
            """
              package a;
              public class Base {
                  public static class List {}
              }
              """
          )),
          java(
            """
              import a.Base;

              class T extends Base {
                  java.util.List List;
                  java.util.List other;
              }
              """
          )
        );
    }

    @Test
    void annotationsCanAddMemberTypesMissingFromSourceDeclarations() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().dependsOn(
            """
              package lombok;
              public @interface Builder {
                  String builderClassName() default "";
              }
              """
          )),
          java(
            """
              @lombok.Builder(builderClassName = "List")
              class Base {}
              class T extends Base {
                  java.util.List value;
              }
              """
          )
        );
    }

    @Test
    void annotationsCanAddOwnMemberTypesMissingFromSourceDeclarations() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().dependsOn(
            """
              package lombok;
              public @interface Builder {
                  String builderClassName() default "";
              }
              """
          )),
          java(
            """
              @lombok.Builder(builderClassName = "List")
              class T {
                  java.util.List value;
              }
              """
          )
        );
    }

    @Test
    void typeWitnessDoesNotProveAnExpressionQualifierIsFree() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().dependsOn(
            """
              package a;
              public class Base {
                  protected volatile java.util.Collections Collections;
              }
              """
          )),
          java(
            """
              import a.Base;
              import java.util.Collections;

              class T extends Base {
                  Collections existing;
                  Object m() {
                      return java.util.Collections.emptyList();
                  }
              }
              """,
            source -> source.mapBeforeRecipe(cu -> cu.withClasses(cu.getClasses().stream().map(declaration ->
              declaration.withType(((JavaType.Class) declaration.getType()).withSupertype(JavaType.ShallowClass.build("a.Base")))
            ).toList()))
          )
        );
    }

    @Test
    void expressionWitnessProvesTheQualifierIsFree() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().dependsOn(
            """
              package a;
              public class Base {}
              """
          )).recipeQualityValidation(RecipeQualityValidation.builder().matchByTypeNotName(false).build()),
          java(
            """
              import a.Base;
              import java.util.Collections;

              class T extends Base {
                  Object first() {
                      return Collections.emptyList();
                  }
                  Object second() {
                      return java.util.Collections.emptyList();
                  }
              }
              """,
            """
              import a.Base;
              import java.util.Collections;

              class T extends Base {
                  Object first() {
                      return Collections.emptyList();
                  }
                  Object second() {
                      return Collections.emptyList();
                  }
              }
              """
          )
        );
    }

    @Test
    void semanticsCallbackRetainsItsQualifiedTemplateSyntax() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().dependsOn(
            """
              package org.openrewrite.java.template;
              public class Semantics {
                  public static <T> T expression(java.util.function.Supplier<T> template) {
                      return null;
                  }
              }
              """
          )),
          java(
            """
              import java.util.Collections;

              import static org.openrewrite.java.template.Semantics.expression;

              class T {
                  Object value = Collections.emptyList();
                  Object template = expression(() -> java.util.Collections.emptyList());
              }
              """
          )
        );
    }

    @Test
    void refasterMethodsRetainTheirQualifiedTemplateSyntax() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().dependsOn(
            """
              package com.google.errorprone.refaster.annotation;
              public @interface BeforeTemplate {}
              """
          )),
          java(
            """
              import com.google.errorprone.refaster.annotation.BeforeTemplate;
              import java.util.Collections;

              class T {
                  Object value = Collections.emptyList();
                  @BeforeTemplate
                  Object before() {
                      return java.util.Collections.emptyList();
                  }
              }
              """
          )
        );
    }

}
