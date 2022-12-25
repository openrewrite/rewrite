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
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class FindFieldsOfTypeTest implements RewriteTest {

    @Issue("https://github.com/openrewrite/rewrite/issues/2199")
    @Test
    void findFieldNotVariable() {
        rewriteRun(
          spec -> spec.recipe(new FindFieldsOfType("java.io.File")),
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

    @Test
    void findPrivateNonInheritedField() {
        rewriteRun(
          spec -> spec.recipe(new FindFieldsOfType("java.util.List")),
          java(
            """
              import java.util.*;
              public class A {
                 private List<?> list;
                 private Set<?> set;
              }
              """,
            """
              import java.util.*;
              public class A {
                 /*~~>*/private List<?> list;
                 private Set<?> set;
              }
              """
          )
        );
    }

    @Test
    void findArrayOfType() {
        rewriteRun(
          spec -> spec.recipe(new FindFieldsOfType("java.lang.String")),
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

    @SuppressWarnings("EmptyTryBlock")
    @Test
    void skipsMultiCatches() {
        rewriteRun(
          spec -> spec.recipe(new FindFieldsOfType("java.io.File")),
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

}
