/*
 * Copyright 2022 the original author or authors.
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
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Issue;
import org.openrewrite.java.search.FindMissingTypes;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.kotlin.KotlinParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.gradle.Assertions.buildGradleKts;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.kotlin.Assertions.kotlin;

@SuppressWarnings("ClassInitializerMayBeStatic")
class FindMissingTypesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new FindMissingTypes(false))
          .parser(JavaParser.fromJavaVersion())
          .typeValidationOptions(TypeValidation.none());
    }

    @DocumentExample
    @Test
    void missingAnnotationType() {
        rewriteRun(
          java(
            """
              import org.junit.Test;

              class ATest {
                  @Test
                  void foo() {}
              }
              """,
            """
              import org.junit.Test;

              class ATest {
                  @/*~~(Identifier type is missing or malformed)~~>*/Test
                  void foo() {}
              }
              """
          )
        );
    }

    @Test
    void variableDeclaration() {
        rewriteRun(
          java(
            """
              class A {
                  {
                      Foo f;
                  }
              }
              """,
            """
              class A {
                  {
                      /*~~(Identifier type is missing or malformed)~~>*/Foo f;
                  }
              }
              """
          )
        );
    }

    @Test
    void classReference() {
        rewriteRun(
          java(
            """
              class A {
                  {
                      Class<?> c = Unknown.class;
                  }
              }
              """,
            """
              class A {
                  {
                      Class<?> c = /*~~(Identifier type is missing or malformed)~~>*/Unknown.class;
                  }
              }
              """
          )
        );
    }

    @Test
    void methodReference() {
        rewriteRun(
          java(
            """
              import java.util.function.Consumer;

              class A {
                  Consumer<String> r = System.out::printlns;
              }
              """,
            """
              import java.util.function.Consumer;

              class A {
                  Consumer<String> r = /*~~(MemberReference type is missing or malformed)~~>*/System.out::printlns;
              }
              """
          )
        );
    }

    @Test
    void newClass() {
        rewriteRun(
          java(
            """
              import some.org.Unknown;

              class A {
                  {
                      Object o = new Unknown();
                  }
              }
              """,
            """
              import some.org.Unknown;

              class A {
                  {
                      Object o = /*~~(NewClass type is missing or malformed)~~>*/new Unknown();
                  }
              }
              """
          )
        );
    }

    @Test
    void packageAnnotationUnknownType() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion()/* No implied classpath entry for jspecify! */),
          java(
            """
              @NullMarked
              package org.openrewrite.java;
              import org.jspecify.annotations.NullMarked;
              """,
            """
              /*~~(Annotation type is missing or malformed)~~>*/@NullMarked
              package org.openrewrite.java;
              import org.jspecify.annotations.NullMarked;
              """,
            spec -> spec.path("src/main/java/org/openrewrite/java/package-info.java")
              .afterRecipe(cu ->
                assertThat(cu.getPackageDeclaration().getAnnotations().getFirst().getType())
                  .isInstanceOf(JavaType.Unknown.class))
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/4405")
    @Test
    void missingMethodReturnTypeNoMethodArguments() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none()),
          java(
            """
              import java.util.GenClass1;

              abstract class Foo1 {
                  public abstract GenClass1 noArg();
              }
              """,
            """
              import java.util.GenClass1;

              abstract class Foo1 {
                  /*~~(MethodDeclaration type is missing or malformed)~~>*/public abstract GenClass1 noArg();
              }
              """
          ),
          java(
            """
              import java.util.GenClass1;

              abstract class Foo2 {
                  public abstract GenClass1 withArg(String a);
              }
              """,
            """
              import java.util.GenClass1;

              abstract class Foo2 {
                  /*~~(MethodDeclaration type is missing or malformed)~~>*/public abstract GenClass1 withArg(String a);
              }
              """
          )
        );
    }

    @SuppressWarnings("JavadocDeclaration")
    @Test
    void missingJavadocReference() {
        rewriteRun(
          spec -> spec.recipe(new FindMissingTypes(true)),
          java(
            """
              /**
               * See {@code #baz}.
               *
               * @see #baz()
               */
              interface Foo {
                  void bar();
              }
              """,
            """
              /**
               * See {@code #baz}.
               *
               * @see ~~(MethodInvocation type is missing or malformed)~~>#baz()
               */
              interface Foo {
                  void bar();
              }
              """
          )
        );
    }

    @Test
    void noMissingJavadocReference() {
        rewriteRun(
          spec -> spec.recipe(new FindMissingTypes(true)),
          java(
            """
              /**
               * See {@code #baz}.
               *
               * @see #bar()
               */
              interface Foo {
                  void bar();
              }
              """
          )
        );
    }

  
    // Groovy Kotlin DSL seems to be missing from the classpath
    @Test
    void repositoryByUrlAndPurposeProjectKts() {
        rewriteRun(
          buildGradleKts(
            """
              plugins {
                  id("java")
                  id("kotlin") version "1.9.22"
                  `maven-publish`
              }
              
              group = "com.some.project"
              version = "1.0.0"
              
              java {
                  withJavadocJar()
                  withSourcesJar()
                  toolchain {
                      languageVersion.set(JavaLanguageVersion.of(JavaVersion.VERSION_21.majorVersion))
                  }
              }
              
              repositories {
                  mavenCentral()
              }
              
              dependencies {
              
              }
              
              tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
                  kotlinOptions.jvmTarget = "21"
              }
              """
          )
        );
    }
  
    @Test
    void kotlinClassReference() {
        rewriteRun(
          spec -> spec.parser(
            KotlinParser.builder().classpathFromResources(new InMemoryExecutionContext(), "jakarta.persistence-api")),
          kotlin(
            """
              package com.some.other

              class EventPublisher {
              }
              """
          ),
          kotlin(
            """
              package com.some.card

              import com.some.other.EventPublisher
              import jakarta.persistence.EntityListeners

              @EntityListeners(EventPublisher::class)
              data class Card(
              )
              """
          )
        );
    }

    @Test
    void invalidKotlinClassReference() {
        rewriteRun(
          spec -> spec.parser(
            KotlinParser.builder().classpathFromResources(new InMemoryExecutionContext(), "jakarta.persistence-api")),
          kotlin(
            """
              package com.some.card

              import jakarta.persistence.EntityListeners

              @EntityListeners(EventPublisher::class)
              data class Card(
              )
              """,
            """
              package com.some.card

              import jakarta.persistence.EntityListeners

              @EntityListeners(/*~~(MemberReference Parameterized type is missing or malformed)~~>*//*~~(Identifier type is missing or malformed)~~>*/EventPublisher::class)
              data class Card(
              )
              """
          )
        );
    }
}
