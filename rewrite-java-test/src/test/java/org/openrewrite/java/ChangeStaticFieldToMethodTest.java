/*
 * Copyright 2021 the original author or authors.
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
import org.openrewrite.Issue;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class ChangeStaticFieldToMethodTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ChangeStaticFieldToMethod(
            "java.util.Collections",
            "EMPTY_LIST",
            "com.acme.Lists",
            null,
            "of"
        ));
    }

    @Language("java")
    String acmeLists = """
      package com.acme;

      import java.util.Collections;
      import java.util.List;

      class Lists {
          static <E> List<E> of() {
              return Collections.emptyList();
          }
      }
      """;

    @Language("java")
    String staticStringClass = """
      package com.acme;

      public class Example {
          public static final String EXAMPLE = "example";
      }
      """;

    @Test
    @SuppressWarnings("unchecked")
    void migratesQualifiedField() {
        rewriteRun(
          java(acmeLists),
          java(
            """
              import java.util.Collections;
              import java.util.List;

              class A {
                  static List<String> empty() {
                      return Collections.EMPTY_LIST;
                  }
              }
              """,
            """
              import com.acme.Lists;

              import java.util.List;

              class A {
                  static List<String> empty() {
                      return Lists.of();
                  }
              }
              """
          )
        );
    }

    @Test
    void migratesStaticImportedField() {
        rewriteRun(
          java(acmeLists),
          java(
            """
              import static java.util.Collections.EMPTY_LIST;

              class A {
                  static Object empty() {
                      return EMPTY_LIST;
                  }
              }
              """,
            """
              import com.acme.Lists;

              class A {
                  static Object empty() {
                      return Lists.of();
                  }
              }
              """
          )
        );
    }

    @Test
    void migratesFullyQualifiedField() {
        rewriteRun(
          java(acmeLists),
          java(
            """
              class A {
                  static Object empty() {
                      return java.util.Collections.EMPTY_LIST;
                  }
              }
              """,
            """
              import com.acme.Lists;

              class A {
                  static Object empty() {
                      return Lists.of();
                  }
              }
              """
          )
        );
    }

    @Test
    void migratesFieldInitializer() {
        rewriteRun(
          java(acmeLists),
          java(
            """
              import java.util.Collections;

              class A {
                  private final Object collection = Collections.EMPTY_LIST;
              }
              """,
            """
              import com.acme.Lists;

              class A {
                  private final Object collection = Lists.of();
              }
              """
          )
        );
    }

    @Test
    void ignoresUnrelatedFields() {
        rewriteRun(
          java(
            """
              import java.util.Collections;

              class A {
                  static Object EMPTY_LIST = null;

                  static Object empty1() {
                      return A.EMPTY_LIST;
                  }

                  static Object empty2() {
                      return EMPTY_LIST;
                  }
              }
              """
          )
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/1156")
    void migratesToJavaLangClass() {
        rewriteRun(
          spec -> spec.recipe(new ChangeStaticFieldToMethod(
            "com.acme.Example",
            "EXAMPLE",
            "java.lang.System",
            null,
            "lineSeparator"
          )),
          java(staticStringClass),
          java(
            """
              package example;

              import com.acme.Example;

              class A {
                  static String lineSeparator() {
                      return Example.EXAMPLE;
                  }
              }
              """,
            """
              package example;

              class A {
                  static String lineSeparator() {
                      return System.lineSeparator();
                  }
              }
              """
          )
        );
    }

    @Test
    void leavesOwnerAlone() {
        rewriteRun(
          spec -> spec.recipe(new ChangeStaticFieldToMethod(
            "com.example.Test",
            "EXAMPLE",
            "com.doesntmatter.Foo",
            null,
            "BAR")),
          java(
            """
              package com.example;
                            
              class Test {
                  public static Object EXAMPLE = null;
              }
              """
          )
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/1626")
    void constantToMethodOnStaticTarget() {
        rewriteRun(
          spec -> spec.recipe(new ChangeStaticFieldToMethod(
            "constants.Constants",
            "SUCCESS_CODE",
            "io.netty.handler.codec.http.HttpResponseStatus",
            "OK",
            "codeAsText")),
          java(
            """
              package constants;

              public class Constants {
                  public static final String SUCCESS_CODE = "200";
              }
              """
          ),
          java(
            """
              package io.netty.handler.codec.http;

              public class HttpResponseStatus {
                  public static final HttpResponseStatus OK = new HttpResponseStatus(200);

                  private final int code;
                  private HttpResponseStatus(int code) {
                      this.code = code;
                  }
                  
                  String codeAsText() {
                      return String.valueOf(code);
                  }
              }
              """
          ),
          java(
            """
              package com.example;
              
              import constants.Constants;
              
              class Test {
                  public static String testMe() {
                      return Constants.SUCCESS_CODE;
                  }
              }
              """,
            """
              package com.example;
              
              import io.netty.handler.codec.http.HttpResponseStatus;
              
              class Test {
                  public static String testMe() {
                      return HttpResponseStatus.OK.codeAsText();
                  }
              }
              """
          )
        );
    }
}
