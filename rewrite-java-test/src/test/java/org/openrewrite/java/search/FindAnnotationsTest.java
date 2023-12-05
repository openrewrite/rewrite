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
package org.openrewrite.java.search;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;

class FindAnnotationsTest implements RewriteTest {
    @Language("java")
    String foo = """
      package com.netflix.foo;
      public @interface Foo {
          String bar();
          String baz();
      }
      """;

    @DocumentExample
    @Test
    void matchMetaAnnotation() {
        rewriteRun(
          spec -> spec.recipe(new FindAnnotations("@javax.annotation.Nonnull", true))
            .parser(JavaParser.fromJavaVersion().classpath(JavaParser.runtimeClasspath())),
          java(
            """
              import org.openrewrite.internal.lang.Nullable;
              public class Test {
                  @Nullable String name;
              }
              """,
            """
              import org.openrewrite.internal.lang.Nullable;
              public class Test {
                  /*~~>*/@Nullable String name;
              }
              """
          )
        );
    }

    @SuppressWarnings("NewClassNamingConvention")
    @Issue("https://github.com/openrewrite/rewrite/issues/357")
    @Test
    void matchesClassArgument() {
        rewriteRun(
          spec -> spec.recipe(new FindAnnotations("@org.junit.jupiter.api.extension.ExtendWith(org.openrewrite.MyExtension.class)", null))
            .parser(JavaParser.fromJavaVersion().classpath("junit-jupiter-api")),
          java(
            """
              import org.junit.jupiter.api.extension.ExtendWith;
              import org.openrewrite.MyExtension;
              @ExtendWith(MyExtension.class) public class A {}
              @ExtendWith({MyExtension.class}) class B {}
              @ExtendWith(value = MyExtension.class) class C {}
              @ExtendWith(value = {MyExtension.class}) class D {}
              """,
            """
              import org.junit.jupiter.api.extension.ExtendWith;
              import org.openrewrite.MyExtension;
              /*~~>*/@ExtendWith(MyExtension.class) public class A {}
              /*~~>*/@ExtendWith({MyExtension.class}) class B {}
              /*~~>*/@ExtendWith(value = MyExtension.class) class C {}
              /*~~>*/@ExtendWith(value = {MyExtension.class}) class D {}
              """
          ),
          java(
            """
              package org.openrewrite;
              import org.junit.jupiter.api.extension.Extension;
              public class MyExtension implements Extension {}
              """
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"@java.lang.Deprecated", "java.lang.Deprecated"})
    void matchesSimpleFullyQualifiedAnnotation(String annotationPattern) {
        rewriteRun(
          spec -> spec.recipe(new FindAnnotations(annotationPattern, null)),
          java(
            "@Deprecated public class A {}",
            "/*~~>*/@Deprecated public class A {}"
          )
        );
    }

    @Test
    void matchesWildcard() {
        rewriteRun(
          spec -> spec.recipe(new FindAnnotations("@java.lang.*", null)),
          java(
            "@Deprecated public class A {}",
            "/*~~>*/@Deprecated public class A {}"
          )
        );
    }

    @Test
    void matchesSubpackageWildcard() {
        rewriteRun(
          spec -> spec.recipe(new FindAnnotations("@java..*", null)),
          java(
            "@Deprecated public class A {}",
            "/*~~>*/@Deprecated public class A {}"
          )
        );
    }

    @Test
    void matchesAnnotationOnMethod() {
        rewriteRun(
          spec -> spec.recipe(new FindAnnotations("@java.lang.Deprecated", null)),
          java(
            """
              public class A {
                  @Deprecated
                  public void foo() {}
              }
              """,
            """
              public class A {
                  /*~~>*/@Deprecated
                  public void foo() {}
              }
              """
          )
        );
    }

    @Test
    void matchesAnnotationOnField() {
        rewriteRun(
          spec -> spec.recipe(new FindAnnotations("@java.lang.Deprecated", null)),
          java(
            """
              public class A {
                  @Deprecated String s;
              }
              """,
            """
              public class A {
                  /*~~>*/@Deprecated String s;
              }
              """
          )
        );
    }

    @Test
    void doesNotMatchNotFullyQualifiedAnnotations() {
        rewriteRun(
          spec -> spec.recipe(new FindAnnotations("@Deprecated", null)),
          java("@Deprecated public class A {}")
        );
    }

    @Test
    void matchesSingleAnnotationParameter() {
        rewriteRun(
          spec -> spec.recipe(new FindAnnotations("@java.lang.SuppressWarnings(\"deprecation\")", null)),
          java(
            "@SuppressWarnings(\"deprecation\") public class A {}",
            "/*~~>*/@SuppressWarnings(\"deprecation\") public class A {}"
          )
        );
    }

    @Test
    void doesNotMatchDifferentSingleAnnotationParameter() {
        rewriteRun(
          spec -> spec.recipe(new FindAnnotations("@java.lang.SuppressWarnings(\"foo\")", null)),
          java("@SuppressWarnings(\"deprecation\") public class A {}")
        );
    }

    @Test
    void matchesNamedParameters() {
        rewriteRun(
          spec -> spec.recipe(new FindAnnotations("@com.netflix.foo.Foo(bar=\"quux\",baz=\"bar\")", null)),
          java(
            """
              import com.netflix.foo.Foo;
              @Foo(bar="quux", baz="bar")
              public class A {}
              """,
            """
              import com.netflix.foo.Foo;
              /*~~>*/@Foo(bar="quux", baz="bar")
              public class A {}
              """
          ),
          java(foo)
        );
    }

    @Test
    void doesNotMatchDifferentNamedParameters() {
        rewriteRun(
          spec -> spec.recipe(new FindAnnotations("@com.netflix.foo.Foo(bar=\"qux\",baz=\"baz\")", null)),
          java(
            """
              import com.netflix.foo.Foo;
              @Foo(bar="quux", baz="bar")
              public class A {}
              """
          ),
          java(foo)
        );
    }

    @Test
    void matchesPartialNamedParameters() {
        rewriteRun(
          spec -> spec.recipe(new FindAnnotations("@com.netflix.foo.Foo(baz=\"bar\")", null)),
          java(
            """
              import com.netflix.foo.Foo;
              @Foo(bar="quux", baz="bar")
              public class A {}
              """,
            """
              import com.netflix.foo.Foo;
              /*~~>*/@Foo(bar="quux", baz="bar")
              public class A {}
              """
          ),
          java(foo)
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/358")
    @Test
    void matchesNamedParametersRegardlessOfOrder() {
        rewriteRun(
          spec -> spec.recipe(new FindAnnotations("@com.netflix.foo.Foo(baz=\"bar\",bar=\"quux\")", null)),
          java(
            """
              import com.netflix.foo.Foo;
              @Foo(bar="quux", baz="bar")
              public class A {}
              """,
            """
              import com.netflix.foo.Foo;
              /*~~>*/@Foo(bar="quux", baz="bar")
              public class A {}
              """
          ),
          java(foo)
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/394")
    @Test
    void findAnnotationWithClassTypeArgument() {
        rewriteRun(
          java(
            """
              package com.foo;
                          
              import java.lang.annotation.ElementType;
              import java.lang.annotation.Inherited;
              import java.lang.annotation.Retention;
              import java.lang.annotation.RetentionPolicy;
              import java.lang.annotation.Target;
                          
              @Retention(RetentionPolicy.RUNTIME)
              @Target({ElementType.TYPE})
              @Inherited
              public @interface Example {
                  Class<?> value();
              }
              """
          ),
          java(
            """
              package com.foo;
                            
              @Example(Foo.class)
              public class Foo {}
              """,
            spec -> spec.afterRecipe(cu ->
              assertThat(FindAnnotations.find(cu, "@com.foo.Example(com.foo.Foo.class)")).hasSize(1))
          )
        );
    }

    @Test
    void enumArgument() {
        rewriteRun(
          spec -> spec.recipe(new FindAnnotations("@com.fasterxml.jackson.annotation.JsonTypeInfo(use=com.fasterxml.jackson.annotation.JsonTypeInfo$Id.CLASS)", null))
            .parser(JavaParser.fromJavaVersion().classpath("jackson-annotations")),
          java(
            """
              import com.fasterxml.jackson.annotation.JsonTypeInfo;
              import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
                            
              class PenetrationTesting {
                  @JsonTypeInfo(use = Id.CLASS)
                  Object name;
              }
              """,
            """
              import com.fasterxml.jackson.annotation.JsonTypeInfo;
              import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
                            
              class PenetrationTesting {
                  /*~~>*/@JsonTypeInfo(use = Id.CLASS)
                  Object name;
              }
              """
          )
        );
    }
}
