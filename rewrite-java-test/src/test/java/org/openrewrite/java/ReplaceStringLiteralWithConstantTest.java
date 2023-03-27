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
package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class ReplaceStringLiteralWithConstantTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .parser(JavaParser.fromJavaVersion()
            .logCompilationWarningsAndErrors(true)
            .classpath("guava"));
    }

    @Test
    void doNothingIfStringLiteralNotFound() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceStringLiteralWithConstant("UTF_8", "com.google.common.base.Charsets.UTF_8")),
          java(
            """
              class Test {
                  void test(Object obj) {
                      String s = "FooBar";
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotReplaceDefinition() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceStringLiteralWithConstant("someValue", "com.constant.B.VAR")),
          java(
            """
              package com.constant;
              public class B {
                  public static final String VAR = "someValue";
              }
              """
          ));
    }

    @Test
    void shouldNotAddImportWhenUnnecessary() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceStringLiteralWithConstant("Hello World!", EXAMPLE_STRING_FQN)),
          java(
            """
            package org.openrewrite.java;
                            
            class Test {
                Object o = "Hello World!";
            }
            """,
            """                
            package org.openrewrite.java;
            
            class Test {
                Object o = ReplaceStringLiteralWithConstantTest.EXAMPLE_STRING_CONSTANT;
            }
            """
          )
        );
    }

    @Test
    void replaceStringLiteralWithConstant() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceStringLiteralWithConstant("UTF_8", "com.google.common.base.Charsets.UTF_8")),
          java(
            """
              class Test {
                  Object o = "UTF_8";
              }
              """,
            """
              import com.google.common.base.Charsets;

              class Test {
                  Object o = Charsets.UTF_8;
              }
              """
          )
        );
    }

    @Test
    void replaceLiteralWithUserDefinedConstant() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceStringLiteralWithConstant("newValue", "com.constant.B.VAR")),
          java(
            """
              package com.constant;
              public class B {
                  public static final String VAR = "default";
              }
              """
          ),
          java(
            """
              package com.abc;
              class A {
                  String v = "newValue";
                  private String method() {
                      return "newValue";
                  }
              }
              """,
            """
              package com.abc;

              import com.constant.B;

              class A {
                  String v = B.VAR;
                  private String method() {
                      return B.VAR;
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceStringLiteralWithConstantValueWhenLiteralValueIsNotConfigured() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceStringLiteralWithConstant(EXAMPLE_STRING_FQN)),
          java(
            """ 
            class Test {
                Object o = "Hello World!";
            }
            """,
            """
            import org.openrewrite.java.ReplaceStringLiteralWithConstantTest;
            
            class Test {
                Object o = ReplaceStringLiteralWithConstantTest.EXAMPLE_STRING_CONSTANT;
            }
            """
          )
        );
    }

    @Test
    void replaceStringLiteralWithConstantValueWhenLiteralValueIsConfiguredNull() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceStringLiteralWithConstant(null, EXAMPLE_STRING_FQN)),
          java(
            """
            class Test {
                Object o = "Hello World!";
            }
            """,
            """
            import org.openrewrite.java.ReplaceStringLiteralWithConstantTest;
            
            class Test {
                Object o = ReplaceStringLiteralWithConstantTest.EXAMPLE_STRING_CONSTANT;
            }
            """
          )
        );
    }

    @Test
    void replaceStringLiteralWithLiteralValueWhenLiteralValueIsConfiguredEmpty() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceStringLiteralWithConstant("", EXAMPLE_STRING_FQN)),
          java(
            """
            class Test {
                Object o = "";
            }
            """,
            """
            import org.openrewrite.java.ReplaceStringLiteralWithConstantTest;
            
            class Test {
                Object o = ReplaceStringLiteralWithConstantTest.EXAMPLE_STRING_CONSTANT;
            }
            """
          )
        );
    }

    @Test
    void replaceStringLiteralWithLiteralValueWhenLiteralValueIsConfiguredBlank() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceStringLiteralWithConstant(" ", EXAMPLE_STRING_FQN)),
          java(
            """                
            class Test {
                Object o = " ";
            }
            """,
            """
            import org.openrewrite.java.ReplaceStringLiteralWithConstantTest;
            
            class Test {
                Object o = ReplaceStringLiteralWithConstantTest.EXAMPLE_STRING_CONSTANT;
            }
            """
          )
        );
    }

    public static String EXAMPLE_STRING_FQN = ReplaceStringLiteralWithConstantTest.class.getName() + ".EXAMPLE_STRING_CONSTANT";
    public static String EXAMPLE_STRING_CONSTANT = "Hello World!";
}
