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
package org.openrewrite.java.cleanup;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class ReplaceDeprecatedRuntimeExecMethodsTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ReplaceDeprecatedRuntimeExecMethods());
    }

    @Test
    void rawString() {
        rewriteRun(
          java(
            """
              import java.io.IOException;

              class A {
                  void method() throws IOException {
                      Runtime runtime = Runtime.getRuntime();
                      Process process = runtime.exec("ls -al");
                  }
              }
              """,
            """
              import java.io.IOException;

              class A {
                  void method() throws IOException {
                      Runtime runtime = Runtime.getRuntime();
                      Process process = runtime.exec(new String[]{"ls", "-al"});
                  }
              }
              """
          )
        );
    }

    @Test
    void stringVariableAsInput() {
        rewriteRun(
          java(
            """
              import java.io.IOException;

              class B {
                  void method() throws IOException {
                      Runtime runtime = Runtime.getRuntime();
                      String command = "ls -al";
                      Process process = runtime.exec(command);
                  }
              }
              """,
            """
              import java.io.IOException;

              class B {
                  void method() throws IOException {
                      Runtime runtime = Runtime.getRuntime();
                      String command = "ls -al";
                      Process process = runtime.exec(command.split(" "));
                  }
              }
              """
          )
        );
    }

    @Test
    void methodInvocationAsInput() {
        rewriteRun(
          java(
            """
              import java.io.IOException;

              class B {
                  String command() {
                      return "ls -al";
                  }
                  void method() throws IOException {
                      Runtime runtime = Runtime.getRuntime();
                      Process process = runtime.exec(command());
                  }
              }
              """,
            """
              import java.io.IOException;

              class B {
                  String command() {
                      return "ls -al";
                  }
                  void method() throws IOException {
                      Runtime runtime = Runtime.getRuntime();
                      Process process = runtime.exec(command().split(" "));
                  }
              }
              """
          )
        );
    }

    @Test
    void concatenatedRawStringsAsInput() {
        rewriteRun(
          java(
            """
              import java.io.IOException;

              class A {
                  void method() throws IOException {
                      Runtime runtime = Runtime.getRuntime();
                      Process process = runtime.exec("ls" + " " + "-a" + " " + "-l");
                  }
              }
              """,
            """
              import java.io.IOException;

              class A {
                  void method() throws IOException {
                      Runtime runtime = Runtime.getRuntime();
                      Process process = runtime.exec(new String[]{"ls", "-a", "-l"});
                  }
              }
              """
          )
        );
    }

    @Test
    void concatenatedObjectsAsInput() {
        rewriteRun(
          java(
            """
              import java.io.IOException;

              class B {
                  String options = "-a -l";
                  void method() throws IOException {
                      Runtime runtime = Runtime.getRuntime();
                      Process process = runtime.exec("ls" + " " + options);
                  }
              }
              """,
            """
              import java.io.IOException;

              class B {
                  String options = "-a -l";
                  void method() throws IOException {
                      Runtime runtime = Runtime.getRuntime();
                      Process process = runtime.exec(("ls" + " " + options).split(" "));
                  }
              }
              """
          )
        );
    }
}
