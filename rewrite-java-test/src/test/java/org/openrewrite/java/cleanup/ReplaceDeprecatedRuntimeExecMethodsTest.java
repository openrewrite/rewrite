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
import static org.openrewrite.java.Assertions.version;

class ReplaceDeprecatedRuntimeExecMethodsTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ReplaceDeprecatedRuntimeExecMethods());
    }

    @Test
    void rawString() {
        rewriteRun(
          version(
            java(
              """
                import java.io.File;
                import java.io.IOException;

                class A {
                    void method() throws IOException {
                        Runtime runtime = Runtime.getRuntime();
                        String[] envp = { "E1=1", "E2=2"};
                        File dir = new File("/tmp");

                        Process process1 = runtime.exec("ls -a -l");
                        Process process2 = runtime.exec("ls -a -l", envp);
                        Process process3 = runtime.exec("ls -a -l", envp, dir);
                    }
                }
                """,
              """
                import java.io.File;
                import java.io.IOException;

                class A {
                    void method() throws IOException {
                        Runtime runtime = Runtime.getRuntime();
                        String[] envp = { "E1=1", "E2=2"};
                        File dir = new File("/tmp");

                        Process process1 = runtime.exec(new String[]{"ls", "-a", "-l"});
                        Process process2 = runtime.exec(new String[]{"ls", "-a", "-l"}, envp);
                        Process process3 = runtime.exec(new String[]{"ls", "-a", "-l"}, envp, dir);
                    }
                }
                """
            ), 18)
        );
    }

    @Test
    void stringVariableAsInput() {
        rewriteRun(
          version(
            java(
              """
                import java.io.File;
                import java.io.IOException;

                class B {
                    void method() throws IOException {
                        Runtime runtime = Runtime.getRuntime();
                        String command = "ls -al";
                        String[] envp = { "E1=1", "E2=2"};
                        File dir = new File("/tmp");
                        Process process1 = runtime.exec(command);
                        Process process2 = runtime.exec(command, envp);
                        Process process3 = runtime.exec(command, envp, dir);
                    }
                }
                """,
              """
                import java.io.File;
                import java.io.IOException;

                class B {
                    void method() throws IOException {
                        Runtime runtime = Runtime.getRuntime();
                        String command = "ls -al";
                        String[] envp = { "E1=1", "E2=2"};
                        File dir = new File("/tmp");
                        Process process1 = runtime.exec(command.split(" "));
                        Process process2 = runtime.exec(command.split(" "), envp);
                        Process process3 = runtime.exec(command.split(" "), envp, dir);
                    }
                }
                """
            ), 18)
        );
    }

    @Test
    void methodInvocationAsInput() {
        rewriteRun(
          version(
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
            ), 18)
        );
    }

    @Test
    void concatenatedRawStringsAsInput() {
        rewriteRun(
          version(
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
            ), 18)
        );
    }

    @Test
    void concatenatedObjectsAsInput() {
        rewriteRun(
          version(
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
            ), 18)
        );
    }

    @Test
    void deprecatedMethod2() {
        rewriteRun(
          version(
            java(
              """
                import java.io.IOException;

                class A {
                    void method() throws IOException {
                        Runtime runtime = Runtime.getRuntime();
                        String[] envp = { "E1=1", "E2=2"};
                        Process process = runtime.exec("ls -a -l", envp);
                    }
                }
                """,
              """
                import java.io.IOException;

                class A {
                    void method() throws IOException {
                        Runtime runtime = Runtime.getRuntime();
                        String[] envp = { "E1=1", "E2=2"};
                        Process process = runtime.exec(new String[]{"ls", "-a", "-l"}, envp);
                    }
                }
                """
            ), 18)
        );
    }


    @Test
    void doNotChangeIfUsingNewMethods() {
        rewriteRun(
          version(
            java(
              """
                import java.io.File;
                import java.io.IOException;

                class A {
                    void method() throws IOException {
                        Runtime runtime = Runtime.getRuntime();
                        String[] envp = { "E1=1", "E2=2"};
                        File dir = new File("/tmp");

                        Process process1 = runtime.exec(new String[]{"ls", "-al"});
                        Process process2 = runtime.exec(new String[]{"ls", "-al"}, envp);
                        Process process3 = runtime.exec(new String[]{"ls", "-a", "-l"}, envp, dir);
                    }
                }
                """
            ), 18)
        );
    }

    @Test
    void doNotChangeIfUnderJava18() {
        rewriteRun(
          version(
            java(
              """
                import java.io.File;
                import java.io.IOException;

                class A {
                    void method() throws IOException {
                        Runtime runtime = Runtime.getRuntime();
                        String[] envp = { "E1=1", "E2=2"};
                        File dir = new File("/tmp");

                        Process process1 = runtime.exec("ls -al");
                        Process process2 = runtime.exec("ls -al", envp);
                        Process process3 = runtime.exec("ls -al", envp, dir);
                    }
                }
                """
            ), 17)
        );
    }
}
