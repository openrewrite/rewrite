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
package org.openrewrite.java;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.java.search.FindTypes;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.NameTree;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpec;

import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.properties.Assertions.properties;
import static org.openrewrite.xml.Assertions.xml;
import static org.openrewrite.yaml.Assertions.yaml;

@SuppressWarnings("ConstantConditions")
class ChangePackageTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ChangePackage("org.openrewrite", "org.openrewrite.test", null));
    }

    @Language("java")
    String testClassBefore = """
      package org.openrewrite;
      public class Test extends Exception {
          public static void stat() {}
          public void foo() {}
      }
      """;

    @Language("java")
    String testClassAfter = """
      package org.openrewrite.test;
      public class Test extends Exception {
          public static void stat() {}
          public void foo() {}
      }
      """;

    @DocumentExample
    @Test
    void renameUsingSimplePackageName() {
        rewriteRun(
          spec -> spec.recipe(new ChangePackage(
            "org.openrewrite",
            "openrewrite",
            false
          )),
          java(
            """
              import org.openrewrite.Foo;
              class Test {
              }
              """,
            """
              import openrewrite.Foo;
              class Test {
              }
              """
          )
        );
    }

    @Test
    void renamePackageNonRecursive() {
        rewriteRun(
          spec -> spec.recipe(new ChangePackage(
            "org.openrewrite",
            "org.openrewrite.test",
            false
          )),
          java(
            """
              package org.openrewrite.internal;
              class Test {
              }
              """
          )
        );
    }

    @Test
    void dontAddImportWhenNoChangesWereMade() {
        rewriteRun(
          java(
            """
              public class B {}
              """
          )
        );
    }

    @Test
    void renameImport() {
        rewriteRun(
          java(
            """
              package org.openrewrite;
              public class Test {
              }
              """,
            """
              package org.openrewrite.test;
              public class Test {
              }
              """
          ),
          java(
            """
              import org.openrewrite.Test;
              class A {
              }
              """,
            """
              import org.openrewrite.test.Test;
              class A {
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                J.Import imported = cu.getImports().getFirst();
                assertThat(imported.getPackageName()).isEqualTo("org.openrewrite.test");
            })
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1997")
    @Test
    void typesInUseContainsOneTypeReference() {
        rewriteRun(
          java(testClassBefore, testClassAfter),
          java(
            """
              import org.openrewrite.Test;
              public class A {
                  Test a;
                  Test b;
                  Test c;
              }
              """,
            """
              import org.openrewrite.test.Test;
              public class A {
                  Test a;
                  Test b;
                  Test c;
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                assertThat(cu.getTypesInUse().getTypesInUse()).hasSize(1);
                assertThat(cu.findType("org.openrewrite.Test")).isEmpty();
                assertThat(cu.findType("org.openrewrite.test.Test")).isNotEmpty();
            })
          )
        );
    }

    @Test
    void typesInUseContainsOneMethodTypeReference() {
        rewriteRun(
          java(testClassBefore, testClassAfter),
          java(
            """
              import org.openrewrite.Test;
              public class A {
                  void method() {
                      Test a = test(null);
                      Test b = test(null);
                      Test c = test(null);
                  }
                  Test test(Test test) {
                      return test;
                  }
              }
              """,
            """
              import org.openrewrite.test.Test;
              public class A {
                  void method() {
                      Test a = test(null);
                      Test b = test(null);
                      Test c = test(null);
                  }
                  Test test(Test test) {
                      return test;
                  }
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                assertThat(cu.getTypesInUse().getUsedMethods()).hasSize(1);
                assertThat(cu.findType("org.openrewrite.Test")).isEmpty();
                assertThat(cu.findType("org.openrewrite.test.Test")).isNotEmpty();
            })
          )
        );
    }

    @Test
    void updateMethodType() {
        rewriteRun(
          java(
            """
              package org.openrewrite;
              public class Test {
              }
              """,
            """
              package org.openrewrite.test;
              public class Test {
              }
              """
          ),
          java(
            """
              package org.foo;
              import org.openrewrite.Test;
              public class Example {
                  public static Test method(Test test) {
                      return test;
                  }
              }
              """,
            """
              package org.foo;
              import org.openrewrite.test.Test;
              public class Example {
                  public static Test method(Test test) {
                      return test;
                  }
              }
              """
          ),
          java(
            """
              import org.openrewrite.Test;
              import org.foo.Example;
              public class A {
                  Test local = Example.method(null);
              }
              """,
            """
              import org.openrewrite.test.Test;
              import org.foo.Example;
              public class A {
                  Test local = Example.method(null);
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                JavaType.Method methodType = cu.getTypesInUse().getUsedMethods().iterator().next();
                assertThat(TypeUtils.asFullyQualified(methodType.getReturnType()).getFullyQualifiedName())
                  .isEqualTo("org.openrewrite.test.Test");
                assertThat(TypeUtils.asFullyQualified(methodType.getParameterTypes().getFirst()).getFullyQualifiedName())
                  .isEqualTo("org.openrewrite.test.Test");
            })
          )
        );
    }

    @Test
    void updateVariableType() {
        rewriteRun(
          java(testClassBefore, testClassAfter),
          java(
            """
              import org.openrewrite.Test;
              public class A {
                  Test a;
              }
              """,
            """
              import org.openrewrite.test.Test;
              public class A {
                  Test a;
              }
              """,
            spec -> spec.afterRecipe(cu -> assertThat(TypeUtils.asFullyQualified(cu.getTypesInUse().getVariables().iterator().next().getType()).
              getFullyQualifiedName()).isEqualTo("org.openrewrite.test.Test"))
          )
        );
    }

    @Test
    void renamePackage() {
        rewriteRun(
          java(
            """
              package org.openrewrite;
              class Test {
              }
              """,
            """
              package org.openrewrite.test;
              class Test {
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                assertThat(cu.findType("org.openrewrite.Test")).isEmpty();
                assertThat(cu.findType("org.openrewrite.test.Test")).isNotEmpty();
            })
          )
        );
    }

    @Test
    void renamePackageRecursive() {
        rewriteRun(
          java(
            """
              package org.openrewrite.internal;
              class Test {
              }
              """,
            """
              package org.openrewrite.test.internal;
              class Test {
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                assertThat(cu.getSourcePath()).isEqualTo(Paths.get("org/openrewrite/test/internal/Test.java"));
                assertThat(cu.findType("org.openrewrite.internal.Test")).isEmpty();
                assertThat(cu.findType("org.openrewrite.test.internal.Test")).isNotEmpty();
            })
          )
        );
    }

    @Test
    void renamePackageRecursiveImported() {
        rewriteRun(
          spec -> spec.recipe(new ChangePackage(
            "org.openrewrite",
            "org.openrewrite.test",
            true
          )),
          java(
            """
              package org.openrewrite.other;
              public class Test {}
              """,
            """
              package org.openrewrite.test.other;
              public class Test {}
              """
          ),
          java(
            """
              import org.openrewrite.other.Test;
              class A {
                  Test test = null;
              }
              """,
            """
              import org.openrewrite.test.other.Test;
              class A {
                  Test test = null;
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                assertThat(cu.findType("org.openrewrite.other.Test")).isEmpty();
                assertThat(cu.findType("org.openrewrite.test.other.Test")).isNotEmpty();
            })
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/pull/4189")
    @Test
    void renamePackageNullRecursiveImportedCheckStrictPackageMatch() {
        rewriteRun(
          spec -> spec.recipe(new ChangePackage(
            "org.openrewrite.other",
            "org.openrewrite.test.other",
            null
          )),
          java(
            """
              package org.openrewrite.other;
              public class Test {}
              """,
            """
              package org.openrewrite.test.other;
              public class Test {}
              """
          ),
          java(
            """
              package org.openrewrite.otherone;
              public class OtherTest {}
              """
          ),
          java(
            """
              import org.openrewrite.otherone.OtherTest;
              class B {
                  OtherTest test = null;
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                assertThat(cu.findType("org.openrewrite.otherone.OtherTest")).isNotEmpty();
                assertThat(cu.findType("org.openrewrite.test.otherone.OtherTest")).isEmpty();
            })
          ),
          java(
            """
              import org.openrewrite.other.Test;
              class A {
                  Test test = null;
              }
              """,
            """
              import org.openrewrite.test.other.Test;
              class A {
                  Test test = null;
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                assertThat(cu.findType("org.openrewrite.other.Test")).isEmpty();
                assertThat(cu.findType("org.openrewrite.test.other.Test")).isNotEmpty();
            })
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/pull/4189")
    @Test
    void renamePackageImportedCheckStrictPackageMatch() {
        rewriteRun(
          spec -> spec.recipe(new ChangePackage(
            "org.openrewrite.other",
            "org.openrewrite.test.other",
            false
          )),
          java(
            """
              package org.openrewrite.other;
              public class Test {}
              """,
            """
              package org.openrewrite.test.other;
              public class Test {}
              """
          ),
          java(
            """
              package org.openrewrite.otherone;
              public class OtherTest {}
              """
          ),
          java(
            """
              import org.openrewrite.otherone.OtherTest;
              class B {
                  OtherTest test = null;
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                assertThat(cu.findType("org.openrewrite.otherone.OtherTest")).isNotEmpty();
                assertThat(cu.findType("org.openrewrite.test.otherone.OtherTest")).isEmpty();
            })
          ),
          java(
            """
              import org.openrewrite.other.Test;
              class A {
                  Test test = null;
              }
              """,
            """
              import org.openrewrite.test.other.Test;
              class A {
                  Test test = null;
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                assertThat(cu.findType("org.openrewrite.other.Test")).isEmpty();
                assertThat(cu.findType("org.openrewrite.test.other.Test")).isNotEmpty();
            })
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/pull/4189")
    @Test
    void renamePackageRecursiveImportedStrictPackageMatch() {
        rewriteRun(
          spec -> spec.recipe(new ChangePackage(
            "org.openrewrite.other",
            "org.openrewrite.test.other",
            true
          )),
          java(
            """
              package org.openrewrite.other;
              public class Test {}
              """,
            """
              package org.openrewrite.test.other;
              public class Test {}
              """
          ),
          java(
            """
              package org.openrewrite.otherone;
              public class OtherTest {}
              """
          ),
          java(
            """
              import org.openrewrite.otherone.OtherTest;
              class B {
                  OtherTest test = null;
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                assertThat(cu.findType("org.openrewrite.otherone.OtherTest")).isNotEmpty();
                assertThat(cu.findType("org.openrewrite.test.otherone.OtherTest")).isEmpty();
            })
          ),
          java(
            """
              import org.openrewrite.other.Test;
              class A {
                  Test test = null;
              }
              """,
            """
              import org.openrewrite.test.other.Test;
              class A {
                  Test test = null;
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                assertThat(cu.findType("org.openrewrite.other.Test")).isEmpty();
                assertThat(cu.findType("org.openrewrite.test.other.Test")).isNotEmpty();
            })
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1997")
    @Test
    void typeParameter() {
        rewriteRun(
          java(
            """
              package org.openrewrite;
              public class Test {
              }
              """,
            """
              package org.openrewrite.test;
              public class Test {
              }
              """
          ),
          java(
            """
              import org.openrewrite.Test;
              import java.util.List;
              class A {
                  List<Test> list;
              }
              """,
            """
              import org.openrewrite.test.Test;
              import java.util.List;
              class A {
                  List<Test> list;
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                assertThat(cu.findType("org.openrewrite.Test")).isEmpty();
                assertThat(cu.findType("org.openrewrite.test.Test")).isNotEmpty();
            })
          )
        );
    }

    @Test
    void classTypeParameter() {
        rewriteRun(
          java(
            """
              package org.openrewrite;
              public class Test {
              }
              """,
            """
              package org.openrewrite.test;
              public class Test {
              }
              """
          ),
          java(
            """
              import org.openrewrite.Test;
              class A<T extends Test> {
              }
              """,
            """
              import org.openrewrite.test.Test;
              class A<T extends Test> {
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                assertThat(cu.findType("org.openrewrite.Test")).isEmpty();
                assertThat(cu.findType("org.openrewrite.test.Test")).isNotEmpty();
            })
          )
        );
    }

    @Test
    void boundedGenericType() {
        rewriteRun(
          java(testClassBefore, testClassAfter),
          java(
            """
              import org.openrewrite.Test;
              public class A {
                  <T extends Test> T method(T t) {
                      return t;
                  }
              }
              """,
            """
              import org.openrewrite.test.Test;
              public class A {
                  <T extends Test> T method(T t) {
                      return t;
                  }
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                assertThat(cu.findType("org.openrewrite.Test")).isEmpty();
                assertThat(cu.findType("org.openrewrite.test.Test")).isNotEmpty();
            })
          )
        );
    }

    @Test
    void ambiguousImportIssue() {
        rewriteRun(
          spec -> spec.recipe(new ChangePackage("origpkg.validation", "newpkg.validation", true)),
          //language=java
          java(
            """
              package origpkg.validation;
              
              import java.lang.annotation.ElementType;
              import java.lang.annotation.Retention;
              import java.lang.annotation.RetentionPolicy;
              import java.lang.annotation.Target;
              @Target({ElementType.TYPE, ElementType.FIELD})
              @Retention(RetentionPolicy.RUNTIME)
              public @interface ExtraneousAnnotation {}
              """,
            SourceSpec::skip
          ),
          //language=java
          java(
            """
              package newpkg.validation;
              
              import java.lang.annotation.ElementType;
              import java.lang.annotation.Retention;
              import java.lang.annotation.RetentionPolicy;
              import java.lang.annotation.Target;
              @Target({ElementType.TYPE, ElementType.FIELD})
              @Retention(RetentionPolicy.RUNTIME)
              public @interface ExtraneousAnnotation {}
              """,
            SourceSpec::skip
          ),
          //language=java
          java(
            """
              package otherpkg.validation;
              
              import java.lang.annotation.ElementType;
              import java.lang.annotation.Retention;
              import java.lang.annotation.RetentionPolicy;
              import java.lang.annotation.Target;
              @Target({ElementType.TYPE, ElementType.FIELD})
              @Retention(RetentionPolicy.RUNTIME)
              public @interface NotBlank {}
              """,
            SourceSpec::skip
          ),
          //language=java
          java(
            """
              package newpkg.validation;
              
              import java.lang.annotation.ElementType;
              import java.lang.annotation.Retention;
              import java.lang.annotation.RetentionPolicy;
              import java.lang.annotation.Target;
              @Target({ElementType.TYPE, ElementType.FIELD})
              @Retention(RetentionPolicy.RUNTIME)
              public @interface NotBlank {}
              """,
            SourceSpec::skip
          ),
          //language=java
          java(
            """
              package xyz;
              
              import origpkg.validation.*;
              import otherpkg.validation.*;
              
              class A {
                  @NotBlank
                  private String someField;
                  @ExtraneousAnnotation
                  private String otherField;
              }
              """,
            """
              package xyz;
              
              import newpkg.validation.*;
              import otherpkg.validation.*;
              
              class A {
                  @NotBlank
                  private String someField;
                  @ExtraneousAnnotation
                  private String otherField;
              }
              """
          ),
          // TODO: This will fail to parse correctly, in comparison
          //language=java
          java(
            """
              package xyz;
              
              import newpkg.validation.*;
              import otherpkg.validation.*;
              
              class B {
                  @NotBlank
                  private String someField;
                  @ExtraneousAnnotation
                  private String otherField;
              }
              """
          )
        );
    }

    @Test
    void annotation() {
        rewriteRun(
          java(
            """
              package org.openrewrite;
              import java.lang.annotation.ElementType;
              import java.lang.annotation.Retention;
              import java.lang.annotation.RetentionPolicy;
              import java.lang.annotation.Target;
              @Target({ElementType.TYPE, ElementType.METHOD})
              @Retention(RetentionPolicy.RUNTIME)
              public @interface Test {}
              """,
            SourceSpec::skip
          ),
          java(
            """
              import org.openrewrite.Test;
              public class ATest {
                  @Test
                  void method() {}
              }
              """,
            """
              import org.openrewrite.test.Test;
              public class ATest {
                  @Test
                  void method() {}
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                assertThat(cu.findType("org.openrewrite.Test")).isEmpty();
                assertThat(cu.findType("org.openrewrite.test.Test")).isNotEmpty();
            })
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/3200")
    @Test
    void annotationArgument() {
        rewriteRun(
          java(
            """
              package org.openrewrite;
              public class Argument {}
              """,
            SourceSpec::skip),
          java(
            """
              package com.acme;

              import java.lang.annotation.ElementType;
              import java.lang.annotation.Retention;
              import java.lang.annotation.RetentionPolicy;
              import java.lang.annotation.Target;

              @Target({ElementType.TYPE, ElementType.METHOD})
              @Retention(RetentionPolicy.RUNTIME)
              public @interface Test {
                  Class<?> value();
              }
              """,
            SourceSpec::skip
          ),
          java(
            """
              import com.acme.Test;
              import org.openrewrite.Argument;
              public class ATest {
                  @Test(Argument.class)
                  void method() {}
              }
              """,
            """
              import com.acme.Test;
              import org.openrewrite.test.Argument;
              public class ATest {
                  @Test(Argument.class)
                  void method() {}
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                assertThat(cu.findType("org.openrewrite.Argument")).isEmpty();
                assertThat(cu.findType("org.openrewrite.test.Argument")).isNotEmpty();
            })
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/3200")
    @Test
    void annotationArgumentNamed() {
        rewriteRun(
          java(
            """
              package org.openrewrite;
              public class Argument {}
              """,
            SourceSpec::skip),
          java(
            """
              package com.acme;
              import java.lang.annotation.ElementType;
              import java.lang.annotation.Retention;
              import java.lang.annotation.RetentionPolicy;
              import java.lang.annotation.Target;
              @Target({ElementType.TYPE, ElementType.METHOD})
              @Retention(RetentionPolicy.RUNTIME)
              public @interface Test {
                  Class<?> named();
              }
              """,
            SourceSpec::skip
          ),
          java(
            """
              import com.acme.Test;
              import org.openrewrite.Argument;
              public class ATest {
                  @Test(named = Argument.class)
                  void method() {}
              }
              """,
            """
              import com.acme.Test;
              import org.openrewrite.test.Argument;
              public class ATest {
                  @Test(named = Argument.class)
                  void method() {}
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                assertThat(cu.findType("org.openrewrite.Argument")).isEmpty();
                assertThat(cu.findType("org.openrewrite.test.Argument")).isNotEmpty();
            })
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/3200")
    @Test
    void annotationArgumentFullyQualified() {
        rewriteRun(
          java(
            """
              package org.openrewrite;
              public class Argument {}
              """,
            SourceSpec::skip),
          java(
            """
              package com.acme;
              import java.lang.annotation.ElementType;
              import java.lang.annotation.Retention;
              import java.lang.annotation.RetentionPolicy;
              import java.lang.annotation.Target;
              @Target({ElementType.TYPE, ElementType.METHOD})
              @Retention(RetentionPolicy.RUNTIME)
              public @interface Test {
                  Class<?> value();
              }
              """,
            SourceSpec::skip
          ),
          java(
            """
              import com.acme.Test;
              public class ATest {
                  @Test(org.openrewrite.Argument.class)
                  void method() {}
              }
              """,
            """
              import com.acme.Test;
              public class ATest {
                  @Test(org.openrewrite.test.Argument.class)
                  void method() {}
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                assertThat(cu.findType("org.openrewrite.Argument")).isEmpty();
                assertThat(cu.findType("org.openrewrite.test.Argument")).isNotEmpty();
            })
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/3200")
    @Test
    void annotationArgumentNamedFullyQualified() {
        rewriteRun(
          java(
            """
              package org.openrewrite;
              public class Argument {}
              """,
            SourceSpec::skip),
          java(
            """
              package com.acme;
              import java.lang.annotation.ElementType;
              import java.lang.annotation.Retention;
              import java.lang.annotation.RetentionPolicy;
              import java.lang.annotation.Target;
              @Target({ElementType.TYPE, ElementType.METHOD})
              @Retention(RetentionPolicy.RUNTIME)
              public @interface Test {
                  Class<?> named();
              }
              """,
            SourceSpec::skip
          ),
          java(
            """
              import com.acme.Test;
              public class ATest {
                  @Test(named = org.openrewrite.Argument.class)
                  void method() {}
              }
              """,
            """
              import com.acme.Test;
              public class ATest {
                  @Test(named = org.openrewrite.test.Argument.class)
                  void method() {}
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                assertThat(cu.findType("org.openrewrite.Argument")).isEmpty();
                assertThat(cu.findType("org.openrewrite.test.Argument")).isNotEmpty();
            })
          )
        );
    }

    @Test
    void array() {
        rewriteRun(
          java(testClassBefore, testClassAfter),
          java(
            """
              public class B {
                 org.openrewrite.Test[] a = new org.openrewrite.Test[0];
              }
              """,
            """
              public class B {
                 org.openrewrite.test.Test[] a = new org.openrewrite.test.Test[0];
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                assertThat(cu.findType("org.openrewrite.Test")).isEmpty();
                assertThat(cu.findType("org.openrewrite.test.Test")).isNotEmpty();
            })
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1997")
    @Test
    void multiDimensionalArray() {
        rewriteRun(
          java(testClassBefore, testClassAfter),
          java(
            """
              import org.openrewrite.Test;
              public class A {
                  Test[][] multiDimensionalArray;
              }
              """,
            """
              import org.openrewrite.test.Test;
              public class A {
                  Test[][] multiDimensionalArray;
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                assertThat(cu.findType("org.openrewrite.Test")).isEmpty();
                assertThat(cu.findType("org.openrewrite.test.Test")).isNotEmpty();
            })
          )
        );
    }

    @Test
    void classDecl() {
        rewriteRun(
          java(testClassBefore, testClassAfter),
          java("public interface I1 {}"),
          java(
            """
              public class B extends org.openrewrite.Test implements I1 {}
              """,
            """
              public class B extends org.openrewrite.test.Test implements I1 {}
              """,
            spec -> spec.afterRecipe(cu -> {
                assertThat(cu.findType("org.openrewrite.Test")).isEmpty();
                assertThat(cu.findType("org.openrewrite.test.Test")).isNotEmpty();
            })
          )
        );
    }

    @Test
    void updatesImplements() {
        rewriteRun(
          java(
            """
              package org.openrewrite;
              public interface Oi{}
              """,
            SourceSpec::skip
          ),
          java(
            """
              package org.openrewrite;
              public class Mi implements org.openrewrite.Oi {
              }
              """,
            """
              package org.openrewrite.test;
              public class Mi implements org.openrewrite.test.Oi {
              }
              """
          )
        );
    }

    @Test
    void moveToSubPackageRemoveImport() {
        rewriteRun(
          spec -> spec.recipe(new ChangePackage(
            "com.acme.project",
            "com.acme.product",
            null
          )),
          java(
            """
              package com.acme.product;
              public class RunnableFactory {
                  public static Runnable getRunnable() {
                      return null;
                  }
              }
              """
          ),
          java(
            """
              package com.acme.project;
              import com.acme.product.RunnableFactory;
              public class StaticImportWorker {
                  public void work() {
                      RunnableFactory.getRunnable().run();
                  }
              }
              """,
            """
              package com.acme.product;
              public class StaticImportWorker {
                  public void work() {
                      RunnableFactory.getRunnable().run();
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1997")
    @Test
    void moveToSubPackageDoNotRemoveImport() {
        rewriteRun(
          spec -> spec.recipe(new ChangePackage(
            "com.acme.project",
            "com.acme.product",
            true
          )),
          java(
            """
              package com.acme.product;
              public class RunnableFactory {
                  public static Runnable getRunnable() {
                      return null;
                  }
              }
              """
          ),
          java(
            """
              package com.acme.project.other;
              import com.acme.product.RunnableFactory;
              public class StaticImportWorker {
                  public void work() {
                      RunnableFactory.getRunnable().run();
                  }
              }
              """,
            """
              package com.acme.product.other;
              import com.acme.product.RunnableFactory;
              public class StaticImportWorker {
                  public void work() {
                      RunnableFactory.getRunnable().run();
                  }
              }
              """
          )
        );
    }

    @Test
    void lambda() {
        rewriteRun(
          java(
            """
              package org.openrewrite;
              public interface Procedure {
                  void execute();
              }
              """,
            SourceSpec::skip
          ),
          java(
            """
              import org.openrewrite.Procedure;
              public abstract class Worker {
                  void callWorker() {
                      worker(() -> {});
                  }
                  abstract void worker(Procedure procedure);
              }
              """,
            """
              import org.openrewrite.test.Procedure;
              public abstract class Worker {
                  void callWorker() {
                      worker(() -> {});
                  }
                  abstract void worker(Procedure procedure);
              }
              """,
            spec -> spec.afterRecipe(cu -> assertThat(cu.findType("org.openrewrite.Procedure")).isEmpty())
          )
        );
    }

    @Test
    void typeInNestedPackageInheritingFromTypeInBasePackage() {
        rewriteRun(
          spec -> spec.recipe(new ChangePackage("java.util", "util", null)),
          java(
            """
              import java.util.concurrent.ConcurrentHashMap;
              public class Test {
              }
              """
          )
        );
    }

    @Test
    void method() {
        rewriteRun(
          java(testClassBefore, testClassAfter),
          java(
            """
              public class A {
                 public org.openrewrite.Test foo() { return null; }
              }
              """,
            """
              public class A {
                 public org.openrewrite.test.Test foo() { return null; }
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                assertThat(cu.findType("org.openrewrite.Test")).isEmpty();
                assertThat(cu.findType("org.openrewrite.test.Test")).isNotEmpty();
            })
          )
        );
    }

    @Test
    void methodInvocationTypeParametersAndWildcard() {
        rewriteRun(
          java(testClassBefore, testClassAfter),
          java(
            """
              package org.openrewrite;
              public class ThingOne {
              }
              """,
            """
              package org.openrewrite.test;
              public class ThingOne {
              }
              """
          ),
          java(
            """
              package org.openrewrite;
              public class ThingTwo {
                  public static ThingOne getThingOne() {
                      return new ThingOne();
                  }
              }
              """,
            """
              package org.openrewrite.test;
              public class ThingTwo {
                  public static ThingOne getThingOne() {
                      return new ThingOne();
                  }
              }
              """
          ),
          java(
            """
              import java.util.List;
              import org.openrewrite.ThingOne;
              import org.openrewrite.ThingTwo;
              public class B {
                  public <T extends org.openrewrite.Test> T generic(T n, List<? super org.openrewrite.Test> in) {
                      return null;
                  }
                  public void test() {
                      org.openrewrite.Test.stat();
                      this.<org.openrewrite.Test>generic(null, null);
                      ThingOne t1 = ThingTwo.getThingOne();
                  }
              }
              """,
            """
              import java.util.List;
              import org.openrewrite.test.ThingOne;
              import org.openrewrite.test.ThingTwo;
              public class B {
                  public <T extends org.openrewrite.test.Test> T generic(T n, List<? super org.openrewrite.test.Test> in) {
                      return null;
                  }
                  public void test() {
                      org.openrewrite.test.Test.stat();
                      this.<org.openrewrite.test.Test>generic(null, null);
                      ThingOne t1 = ThingTwo.getThingOne();
                  }
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                assertThat(cu.findType("org.openrewrite.Test")).isEmpty();
                assertThat(cu.findType("org.openrewrite.test.Test")).isNotEmpty();
            })
          )
        );
    }

    @Test
    void multiCatch() {
        rewriteRun(
          java(testClassBefore, testClassAfter),
          java(
            """
              public class B {
                 public void test() {
                     try {System.out.println();}
                     catch(org.openrewrite.Test | RuntimeException ignored) {}
                 }
              }
              """,
            """
              public class B {
                 public void test() {
                     try {System.out.println();}
                     catch(org.openrewrite.test.Test | RuntimeException ignored) {}
                 }
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                assertThat(cu.findType("org.openrewrite.Test")).isEmpty();
                assertThat(cu.findType("org.openrewrite.test.Test")).isNotEmpty();
            })
          )
        );
    }

    @Test
    void multiVariable() {
        rewriteRun(
          java(testClassBefore, testClassAfter),
          java(
            """
              public class B {
                 org.openrewrite.Test f1, f2;
              }
              """,
            """
              public class B {
                 org.openrewrite.test.Test f1, f2;
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                assertThat(cu.findType("org.openrewrite.Test")).isEmpty();
                assertThat(cu.findType("org.openrewrite.test.Test")).isNotEmpty();
            })
          )
        );
    }

    @Test
    void assignment() {
        rewriteRun(
          java(testClassBefore, testClassAfter),
          java(
            """
              public class B {
                 org.openrewrite.Test t;
                 void method(org.openrewrite.Test param) {
                     t = param;
                 }
              }
              """,
            """
              public class B {
                 org.openrewrite.test.Test t;
                 void method(org.openrewrite.test.Test param) {
                     t = param;
                 }
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                assertThat(cu.findType("org.openrewrite.Test")).isEmpty();
                assertThat(cu.findType("org.openrewrite.test.Test")).isNotEmpty();
            })
          )
        );
    }

    @Test
    void newClass() {
        rewriteRun(
          java(testClassBefore, testClassAfter),
          java(
            """
              public class B {
                 org.openrewrite.Test test = new org.openrewrite.Test();
              }
              """,
            """
              public class B {
                 org.openrewrite.test.Test test = new org.openrewrite.test.Test();
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                assertThat(cu.findType("org.openrewrite.Test")).isEmpty();
                assertThat(cu.findType("org.openrewrite.test.Test")).isNotEmpty();
            })
          )
        );
    }

    @Test
    void parameterizedType() {
        rewriteRun(
          java(testClassBefore, testClassAfter),
          java(
            """
              import org.openrewrite.Test;
              import java.util.Map;
              public class B {
                 Map<Test, Test> m;
              }
              """,
            """
              import org.openrewrite.test.Test;
              import java.util.Map;
              public class B {
                 Map<Test, Test> m;
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                assertThat(cu.findType("org.openrewrite.Test")).isEmpty();
                assertThat(cu.findType("org.openrewrite.test.Test")).isNotEmpty();
            })
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1997")
    @Test
    void typeCast() {
        rewriteRun(
          java(testClassBefore, testClassAfter),
          java(
            """
              import org.openrewrite.Test;
              public class A {
                  Test method(Object obj) {
                      return (Test) obj;
                  }
              }
              """,
            """
              import org.openrewrite.test.Test;
              public class A {
                  Test method(Object obj) {
                      return (Test) obj;
                  }
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                assertThat(cu.findType("org.openrewrite.Test")).isEmpty();
                assertThat(cu.findType("org.openrewrite.test.Test")).isNotEmpty();
            })
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1997")
    @Test
    void classReference() {
        rewriteRun(
          java(testClassBefore, testClassAfter),
          java(
            """
              import org.openrewrite.Test;
              public class A {
                  Class<?> clazz = Test.class;
              }
              """,
            """
              import org.openrewrite.test.Test;
              public class A {
                  Class<?> clazz = Test.class;
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                assertThat(cu.findType("org.openrewrite.Test")).isEmpty();
                assertThat(cu.findType("org.openrewrite.test.Test")).isNotEmpty();
            })
          )
        );
    }

    @Test
    void methodSelect() {
        rewriteRun(
          java(testClassBefore, testClassAfter),
          java(
            """
              public class B {
                 org.openrewrite.Test test = null;
                 public void getFoo() { test.foo(); }
              }
              """,
            """
              public class B {
                 org.openrewrite.test.Test test = null;
                 public void getFoo() { test.foo(); }
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                assertThat(cu.findType("org.openrewrite.Test")).isEmpty();
                assertThat(cu.findType("org.openrewrite.test.Test")).isNotEmpty();
            })
          )
        );
    }

    @Test
    void staticImport() {
        rewriteRun(
          java(testClassBefore, testClassAfter),
          java(
            """
              import static org.openrewrite.Test.stat;

              public class B {
                  public void test() {
                      stat();
                  }
              }
              """,
            """
              import static org.openrewrite.test.Test.stat;

              public class B {
                  public void test() {
                      stat();
                  }
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                assertThat(cu.findType("org.openrewrite.Test")).isEmpty();
                assertThat(cu.findType("org.openrewrite.test.Test")).isNotEmpty();
            })
          )
        );
    }

    @Test
    void changeTypeWithInnerClass() {
        rewriteRun(
          spec -> spec.recipe(new ChangePackage(
            "com.acme.product",
            "com.acme.product.v2",
            null
          )),
          java(
            """
              package com.acme.product;
              public class OuterClass {
                  public static class InnerClass {
                  }
              }
              """,
            """
              package com.acme.product.v2;
              public class OuterClass {
                  public static class InnerClass {
                  }
              }
              """
          ),
          java(
            """
              package de;
              import com.acme.product.OuterClass.InnerClass;
              import com.acme.product.OuterClass;
              public class UseInnerClass {
                  public String work() {
                      return new InnerClass().toString();
                  }
                  public String work2() {
                      return new OuterClass().toString();
                  }
              }
              """,
            """
              package de;
              import com.acme.product.v2.OuterClass.InnerClass;
              import com.acme.product.v2.OuterClass;
              public class UseInnerClass {
                  public String work() {
                      return new InnerClass().toString();
                  }
                  public String work2() {
                      return new OuterClass().toString();
                  }
              }
              """
          )
        );
    }

    @Test
    void updateImportPrefixWithEmptyPackage() {
        rewriteRun(
          spec -> spec.recipe(new ChangePackage("a.b", "", false)),
          java(
            """
              package a.b;
              import java.util.List;
              class Test {
              }
              """,
            """
              import java.util.List;
              class Test {
              }
              """
          )
        );
    }

    @Test
    void updateClassPrefixWithEmptyPackage() {
        rewriteRun(
          spec -> spec.recipe(new
            ChangePackage(
            "a.b",
            "",
            false
          )),
          java(
            """
              package a.b;
              class Test {
              }
              """,
            """
              class Test {
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2328")
    @Test
    void renameSingleSegmentPackage() {
        rewriteRun(
          spec -> spec.recipe(new
            ChangePackage(
            "x",
            "y",
            false
          )),
          java(
            """
              package x;
              class A {
              }
              """,
            """
              package y;
              class A {
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2328")
    @Test
    void removePackage() {
        rewriteRun(
          spec -> spec.recipe(new
            ChangePackage(
            "x.y.z",
            "x",
            false
          )),
          java(
            """
              package x.y.z;
              class A {
              }
              """,
            """
              package x;
              class A {
              }
              """
          )
        );
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Test
    void staticImportEnumSamePackage() {
        rewriteRun(
          java(
            """
              package org.openrewrite;
              public enum MyEnum {
                  A,
                  B
              }
              """,
            SourceSpec::skip
          ),
          java(
            """
              package org.openrewrite;
              import static org.openrewrite.MyEnum.A;
              import static org.openrewrite.MyEnum.B;
              public class App {
                  public void test(String s) {
                      if (s.equals(" " + A + B)) {
                      }
                  }
              }
              """,
            """
              package org.openrewrite.test;
              import static org.openrewrite.test.MyEnum.A;
              import static org.openrewrite.test.MyEnum.B;
              public class App {
                  public void test(String s) {
                      if (s.equals(" " + A + B)) {
                      }
                  }
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                assertThat(cu.findType("org.openrewrite.MyEnum")).isEmpty();
                assertThat(cu.findType("org.openrewrite.test.MyEnum")).isNotEmpty();
                assertThat(cu.findType("org.openrewrite.App")).isEmpty();
                assertThat(cu.findType("org.openrewrite.test.App")).isNotEmpty();
            })
          )
        );
    }

    @Test
    void packageInfoAnnotation() {
        rewriteRun(
          java(
            """
              package org.openrewrite;
              import java.lang.annotation.ElementType;
              import java.lang.annotation.Retention;
              import java.lang.annotation.RetentionPolicy;
              import java.lang.annotation.Target;
              @Target(ElementType.PACKAGE)
              @Retention(RetentionPolicy.RUNTIME)
              public @interface MyAnnotation {
                  MyEnum myEnum() default MyEnum.FOO;
              }
              """,
            SourceSpec::skip
          ),
          java(
            """
              package org.openrewrite;
              public enum MyEnum {
                  FOO,
                  BAR
              }
              """,
            SourceSpec::skip
          ),
          java(
            """
              @org.openrewrite.MyAnnotation(myEnum = org.openrewrite.MyEnum.BAR)
              package com.acme;
              """,
            """
              @org.openrewrite.test.MyAnnotation(myEnum = org.openrewrite.test.MyEnum.BAR)
              package com.acme;
              """,
            spec -> spec.afterRecipe(cu -> {
                assertThat(cu.findType("org.openrewrite.MyAnnotation")).isEmpty();
                assertThat(cu.findType("org.openrewrite.test.MyAnnotation")).isNotEmpty();
                assertThat(cu.findType("org.openrewrite.MyEnum")).isEmpty();
                assertThat(cu.findType("org.openrewrite.test.MyEnum")).isNotEmpty();
            })
          )
        );
    }

    @Test
    void changePackageInSpringXml() {
        rewriteRun(
          spec -> spec.recipe(new ChangePackage("test.type", "test.test.type", true)),
          xml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <beans xsi:schemaLocation="www.springframework.org/schema/beans">
                <bean id="abc" class="test.type.A"/>
              </beans>
              """,
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <beans xsi:schemaLocation="www.springframework.org/schema/beans">
                <bean id="abc" class="test.test.type.A"/>
              </beans>
              """
          )
        );
    }

    @Test
    void changeTypeInPropertiesFile() {
        rewriteRun(
          spec -> spec.recipe(new ChangePackage("java.lang", "java.cool", true)),
          properties(
            """
              a.property=java.lang.String
              b.property=java.lang.test.String
              c.property=String
              """,
            """
              a.property=java.cool.String
              b.property=java.cool.test.String
              c.property=String
              """,
            spec -> spec.path("application.properties"))
        );
    }

    @Test
    void changePackageInYaml() {
        rewriteRun(
          spec -> spec.recipe(new ChangePackage("java.lang", "java.cool", true)),
          yaml(
            """
              root:
                  a: java.lang.String
                  b: java.lang.test.String
                  c: String
              """,
            """
              root:
                  a: java.cool.String
                  b: java.cool.test.String
                  c: String
              """,
            spec -> spec.path("application.yaml")
          )
        );
    }

    @Test
    void changeNonRecursivePackageInYamlKey() {
        rewriteRun(
          spec -> spec.recipe(new ChangePackage("org.apache.http", "org.apache.hc.core5.http", false)),
          yaml(
            """
              logging:
                level:
                  org.apache.hc: debug
                  org.apache.http: debug
              """,
            """
              logging:
                level:
                  org.apache.hc: debug
                  org.apache.hc.core5.http: debug
              """,
            spec -> spec.path("application.yaml")
          )
        );
    }

    @Test
    void innerType() {
        rewriteRun(
          spec -> spec.recipes(
            new ChangePackage("com.demo", "com.newdemo", true),
            new ChangeType("com.newdemo.A.B", "some.thing.X.Y", true)
          ).parser(JavaParser.fromJavaVersion().dependsOn(
            """
              package com.demo;

              public class A {
                  public static class B {}
              }
              """
          )),
          java(
            """
              package app;

              import com.demo.A.B;

              interface Test {
                  B foo();
              }
              """,
            """
              package app;

              import some.thing.X.Y;

              interface Test {
                  Y foo();
              }
              """
          )
        );
    }

    @Test
    void inheritedTypesUpdated() {
        rewriteRun(
          spec -> spec.recipe(new ChangePackage("com.before", "com.after", true))
            .parser(JavaParser.fromJavaVersion().dependsOn(
                """
                  package com.before;
                  
                  public class A { }
                  """
              )
            ),
          java(
            //language=java
            """
              package app;
              
              import com.before.A;
              
              class X extends A { }
              """,
            """
              package app;
              
              import com.after.A;
              
              class X extends A { }
              """,
            spec -> spec.afterRecipe(cu ->
              assertThat(FindTypes.find(cu, "app.X"))
                .singleElement()
                .extracting(NameTree::getType)
                .matches(type-> TypeUtils.isAssignableTo("com.after.A", type), "Assignable to updated type")
            )
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/6513")
    @Test
    void changePackageUpdatesNestedClassImport() {
        rewriteRun(
          spec -> spec.recipe(new ChangePackage(
            "dev.nipafx.rewrite_bug",
            "dev.nipafx.rewrite_changepackage_bug",
            null
          )),
          java(
            """
              package dev.nipafx.rewrite_bug;
              public class Outer {
                  public static class Inner {
                  }
              }
              """,
            """
              package dev.nipafx.rewrite_changepackage_bug;
              public class Outer {
                  public static class Inner {
                  }
              }
              """
          ),
          java(
            """
              package dev.nipafx.rewrite_bug;
              import dev.nipafx.rewrite_bug.Outer.Inner;
              public class Importer {
                  Inner inner;
              }
              """,
            """
              package dev.nipafx.rewrite_changepackage_bug;
              import dev.nipafx.rewrite_changepackage_bug.Outer.Inner;
              public class Importer {
                  Inner inner;
              }
              """
          )
        );
    }
}
