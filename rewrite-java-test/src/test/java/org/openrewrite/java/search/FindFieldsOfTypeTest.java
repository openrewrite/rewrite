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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.java.table.FieldsOfTypeUses;
import org.openrewrite.test.RewriteTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;

class FindFieldsOfTypeTest implements RewriteTest {

    @DocumentExample
    @Test
    void findArrayOfType() {
        rewriteRun(
          spec -> spec.recipe(new FindFieldsOfType("java.lang.String", null)),
          java(
            """
              import java.util.*;
              public class A {
                 private String[] s;
              }
              """,
            """
              import java.util.*;
              public class A {
                 /*~~>*/private String[] s;
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2199")
    @Test
    void findFieldNotVariable() {
        rewriteRun(
          spec -> spec.recipe(new FindFieldsOfType("java.io.File", null)),
          java(
            """
              import java.io.*;
              public class Test {
                  public static void main(String[] args) {
                      File f = new File("/dev/null");
                  }
              }
              """
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"java.util.List", "java.util.*", "java.util..*"})
    void findPrivateNonInheritedField(String type) {
        rewriteRun(
          spec -> spec.recipe(new FindFieldsOfType(type, null)),
          java(
            """
              import java.security.SecureRandom;
              import java.util.List;
              public class A {
                 private List<?> list;
                 private SecureRandom rand;
              }
              """,
            """
              import java.security.SecureRandom;
              import java.util.List;
              public class A {
                 /*~~>*/private List<?> list;
                 private SecureRandom rand;
              }
              """
          )
        );
    }

    @SuppressWarnings("EmptyTryBlock")
    @Test
    void skipsMultiCatches() {
        rewriteRun(
          spec -> spec.recipe(new FindFieldsOfType("java.io.File", null)),
          java(
            """
              import java.io.*;
              public class A {
                  File f;
                  public void test() {
                      try(FileInputStream fis = new FileInputStream(f)) {}
                      catch(FileNotFoundException | RuntimeException ignored) {}
                  }
              }
              """,
            """
              import java.io.*;
              public class A {
                  /*~~>*/File f;
                  public void test() {
                      try(FileInputStream fis = new FileInputStream(f)) {}
                      catch(FileNotFoundException | RuntimeException ignored) {}
                  }
              }
              """
          )
        );
    }

    @Test
    void verifyDataTable() {
        List<FieldsOfTypeUses.Row> expectedFields = List.of(
          new FieldsOfTypeUses.Row("A.java", "list", "java.util.List<Generic{?}>", "java.util.List<Generic{?}>", "private", "private List<?> list"),
          new FieldsOfTypeUses.Row("A.java", "list1", "java.util.List<java.lang.String>", "java.util.ArrayList<java.lang.String>", "private static", "private static List<String> list1 = new ArrayList<>()"),
          new FieldsOfTypeUses.Row("A.java", "list2", "java.util.LinkedList<java.lang.String>", "java.util.LinkedList<java.lang.String>", "private", "private LinkedList<String> list2 = new LinkedList<>()")
        );
        rewriteRun(
          spec -> spec.recipe(new FindFieldsOfType("java.util.List", true))
            .afterRecipe(recipeRun -> {
                List<FieldsOfTypeUses.Row> fields = recipeRun.getDataTableRows(FieldsOfTypeUses.class.getName());
                assertThat(fields).containsExactlyInAnyOrderElementsOf(expectedFields);
            }),
          java(
            """
              import java.security.SecureRandom;
              import java.util.ArrayList;
              import java.util.LinkedList;
              import java.util.List;

              public class A {
                 private List<?> list;
                 private static List<String> list1 = new ArrayList<>();
                 private LinkedList<String> list2 = new LinkedList<>();
                 private SecureRandom rand;
              }
              """,
            """
              import java.security.SecureRandom;
              import java.util.ArrayList;
              import java.util.LinkedList;
              import java.util.List;

              public class A {
                 /*~~>*/private List<?> list;
                 /*~~>*/private static List<String> list1 = new ArrayList<>();
                 /*~~>*/private LinkedList<String> list2 = new LinkedList<>();
                 private SecureRandom rand;
              }
              """
          )
        );
    }
}
