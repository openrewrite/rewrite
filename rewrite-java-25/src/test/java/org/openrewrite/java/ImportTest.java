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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;

//Temporarily added for testing -> move final content back to rewrite-java-tck
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

    @Disabled("Parser does not support semicolon after package declaration yet")
    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/396")
    @Test
    void semicolonAfterPackage() {
        //language=java
        rewriteRun(
          spec -> spec
            .typeValidationOptions(TypeValidation.all().allowNonWhitespaceInWhitespace(true)),
          java(
            """
              package p;;
              import java.util.List;
              class AfterPackage { }
              """
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

    @Issue("https://openjdk.org/jeps/511")
    @Test
    void moduleImportBasic() {
        rewriteRun(
          java(
            """
              import module java.base;
              
              public class A {
                  String s = "test";
                  List<String> list;
              }
              """
          )
        );
    }

    @Issue("https://openjdk.org/jeps/511")
    @Test
    void moduleImportMultiple() {
        rewriteRun(
          java(
            """
              import module java.base;
              import module java.sql;
              
              public class A {
                  String s;
                  Connection conn;
              }
              """
          )
        );
    }

    @Issue("https://openjdk.org/jeps/511")
    @Test
    void moduleImportWithRegularImports() {
        rewriteRun(
          java(
            """
              import module java.base;
              import java.util.HashMap;
              import static java.util.Collections.emptyList;
              
              public class A {
                  String s;
                  HashMap<String, Integer> map;
                  List<String> list = emptyList();
              }
              """
          )
        );
    }

    @Issue("https://openjdk.org/jeps/511")
    @Test
    void moduleImportShadowing() {
        rewriteRun(
          java(
            """
              import module java.base;
              import java.util.List;
              import java.awt.*;
              
              public class A {
                  List<String> list;
                  Color color;
              }
              """
          )
        );
    }

    @Issue("https://openjdk.org/jeps/511")
    @Test
    void moduleImportAggregator() {
        rewriteRun(
          java(
            """
              import module java.se;
              
              public class A {
                  String s;
                  List<String> list;
                  Connection conn;
                  Path path;
              }
              """
          )
        );
    }

    @Issue("https://openjdk.org/jeps/511")
    @Test
    void moduleImportTypeName() {
        rewriteRun(
          java(
            """
              import module java.base;
              import module java.sql;
              """,
            spec -> spec.afterRecipe(cu -> assertThat(cu.getImports().stream()
              .filter(J.Import::isModule)
              .map(J.Import::getQualid)
              .map(J.FieldAccess::toString))
              .containsExactly(
                "java.base",
                "java.sql"
              ))
          )
        );
    }

    @Issue("https://openjdk.org/jeps/511")
    @Test
    void moduleImportPackageName() {
        rewriteRun(
          java(
            """
              import module java.base;
              import module java.logging;
              """,
            spec -> spec.afterRecipe(cu -> assertThat(cu.getImports().stream()
                .filter(J.Import::isModule)
              .map(J.Import::getQualid)
              .map(J.FieldAccess::toString))
              .containsExactly(
                "java.base",
                "java.logging"
              )
            )
          )
        );
    }

    @Issue("https://openjdk.org/jeps/511")
    @Test
    void moduleImportOrdering() {
        rewriteRun(
          java(
            """
              import java.util.List;
              import module java.base;
              import static java.util.Collections.emptyList;
              import module java.sql;
              import java.util.*;
              
              public class A {}
              """
          )
        );
    }
}
