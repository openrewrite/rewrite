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
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;

class ImportTest implements RewriteTest {

    @Test
    void typeName() {
        rewriteRun(
          java(
            """
              import static java.util.Map.Entry;
              import java.util.Map.Entry;

              import java.util.List;
              import java.util.*;

              import static java.nio.charset.StandardCharsets.UTF_8;
              import static java.util.Collections.emptyList;
              """,
            spec -> spec.afterRecipe(cu -> assertThat(cu.getImports().stream().map(J.Import::getTypeName))
              .containsExactly(
                "java.util.Map$Entry",
                "java.util.Map$Entry",
                "java.util.List",
                "java.util.*",
                "java.nio.charset.StandardCharsets",
                "java.util.Collections"
              ))
          )
        );
    }

    @Test
    void packageName() {
        rewriteRun(
          java(
            """
              import static java.util.Map.Entry;
              import java.util.Map.Entry;

              import java.util.List;
              import java.util.*;

              import static java.nio.charset.StandardCharsets.UTF_8;
              import static java.util.Collections.emptyList;
              """,
            spec -> spec.afterRecipe(cu -> assertThat(cu.getImports().stream().map(J.Import::getPackageName))
              .containsExactly(
                "java.util",
                "java.util",
                "java.util",
                "java.util",
                "java.nio.charset",
                "java.util"
              ))
          )
        );
    }

    @Test
    void classImport() {
        rewriteRun(
          java(
            """
              import java.util.List;
              public class A {}
              """
          )
        );
    }

    @Test
    void starImport() {
        rewriteRun(
          java(
            """
              import java.util.*;
              public class A {}
              """
          )
        );
    }

    @Test
    void compare() {
        rewriteRun(
          java(
            """
              import b.B;
              import c.c.C;
              """,
            spec -> spec.afterRecipe(cu -> {
                var b = cu.getImports().get(0);
                var c = cu.getImports().get(1);
                assertThat(b.compareTo(c)).isLessThan(0);
                assertThat(c.compareTo(b)).isGreaterThan(0);
            })
          )
        );
    }

    @Test
    void compareSamePackageDifferentNameLengths() {
        rewriteRun(
          java(
            """
              import org.springframework.context.annotation.Bean;
              import org.springframework.context.annotation.Configuration;
              """,
            spec -> spec.afterRecipe(cu -> {
                var b = cu.getImports().get(0);
                var c = cu.getImports().get(1);
                assertThat(b.compareTo(c)).isLessThan(0);
                assertThat(c.compareTo(b)).isGreaterThan(0);
            })
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2156")
    @Test
    void uppercasePackage() {
        //noinspection ConstantConditions
        rewriteRun(
          java(
            """
              package org.openrewrite.BadPackage;

              public class Foo {
                  public static class Bar {
                  }
              }
              """,
            spec -> spec.afterRecipe(cu -> assertThat(cu.getPackageDeclaration().getPackageName())
              .isEqualTo("org.openrewrite.BadPackage"))
          ),
          java(
            """
              package org.openrewrite;

              import org.openrewrite.BadPackage.Foo;
              import org.openrewrite.BadPackage.Foo.Bar;

              public class Bar {
                  private Foo foo;
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                assertThat(cu.getImports().get(0).getPackageName()).isEqualTo("org.openrewrite.BadPackage");
                assertThat(cu.getImports().get(1).getPackageName()).isEqualTo("org.openrewrite.BadPackage");
            })
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/4569")
    @Test
    void spaceBeforeSemiColon() {
        rewriteRun(
          java(
            """
              import java.util.List ;
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/396")
    @Test
    void semicolonAfterPackage() {
        rewriteRun(
          spec -> spec
            .typeValidationOptions(TypeValidation.all()
              .allowNonWhitespaceInWhitespace(true)
              .parseAndPrintEquality(false)),
          java(
            //language=java
            """
              package p;;
              import java.util.List;
              class AfterPackage { }
              """,
            spec -> spec.beforeRecipe(cu -> {
                System.out.println(cu.printAll());
                assertThat(cu.getImports().getFirst().getQualid()).hasToString("java.util.List");
            })
          )
        );
    }

    @Disabled("Parser does not support semicolon between imports yet")
    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/396")
    @Test
    void semicolonBetweenImports() {
        //language=java
        rewriteRun(
          spec -> spec
            .typeValidationOptions(TypeValidation.all().allowNonWhitespaceInWhitespace(true)),
          java(
            """
              import java.util.List;
              ;import java.util.Set;
              class BetweenImport { }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/396")
    @Test
    void semicolonAfterImports() {
        //language=java
        rewriteRun(
          spec -> spec
            .typeValidationOptions(TypeValidation.all().allowNonWhitespaceInWhitespace(true)),
          java(
            """
              import java.util.List;
              ;class BetweenImport { }
              """
          )
        );
    }
}
