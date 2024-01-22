/*
 * Copyright 2022 the original author or authors.
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
import org.openrewrite.DocumentExample;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Issue;
import org.openrewrite.Recipe;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

@SuppressWarnings({"StringOperationCanBeSimplified", "ConstantConditions"})
class JavaTemplateTest2Test implements RewriteTest {

    private final Recipe replaceToStringWithLiteralRecipe = toRecipe(() -> new JavaVisitor<>() {
        private final MethodMatcher toString = new MethodMatcher("java.lang.String toString()");

        @Override
        public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J mi = super.visitMethodInvocation(method, ctx);
            if (mi instanceof J.MethodInvocation && toString.matches((J.MethodInvocation) mi)) {
                return JavaTemplate.apply("#{any(java.lang.String)}", getCursor(), ((J.MethodInvocation) mi).getCoordinates().replace(),
                  ((J.MethodInvocation) mi).getSelect());
            }
            return mi;
        }
    });

    @SuppressWarnings("StringOperationCanBeSimplified")
    @Issue("https://github.com/openrewrite/rewrite/issues/2185")
    @Test
    void chainedMethodInvocationsAsNewClassArgument() {
        rewriteRun(
          spec -> spec.recipe(replaceToStringWithLiteralRecipe),
          java(
            """
              import java.util.ArrayList;
              import java.util.Collections;
              public class T {
                  void m(String arg) {
                      U u = new U(arg.toString().toCharArray());
                  }
                  class U {
                      U(char[] chars){}
                  }
              }
              """,
            """
              import java.util.ArrayList;
              import java.util.Collections;
              public class T {
                  void m(String arg) {
                      U u = new U(arg.toCharArray());
                  }
                  class U {
                      U(char[] chars){}
                  }
              }
              """
          )
        );
    }

    @DocumentExample
    @Test
    void chainedMethodInvocationsAsNewClassArgument2() {
        rewriteRun(
          spec -> spec.recipe(replaceToStringWithLiteralRecipe),
          java(
            """
              class T {
                  void m(String jsonPayload) {
                      HttpEntity entity = new HttpEntity(jsonPayload.toString(), 0);
                  }
                  class HttpEntity {
                      HttpEntity(String s, int i){}
                  }
              }
              """,
            """
              class T {
                  void m(String jsonPayload) {
                      HttpEntity entity = new HttpEntity(jsonPayload, 0);
                  }
                  class HttpEntity {
                      HttpEntity(String s, int i){}
                  }
              }
              """
          )
        );
    }

    @Test
    void methodArgumentStopCommentsOnlyTerminateEnumInitializers() {
        rewriteRun(
          spec -> spec.recipe(replaceToStringWithLiteralRecipe),
          java(
            """
              import java.io.File;
              import java.io.IOException;
              import java.util.List;
                            
              class Test {
                  File getFile(File testDir, List<String> compileClassPath ) throws IOException {
                      assertEquals(new File(testDir, "ejbs/target/classes").getCanonicalFile(),
                          new File(compileClassPath.get(1).toString()).getCanonicalFile());
                  }
                  void assertEquals(File f1, File f2) {}
              }
              """,
            """
              import java.io.File;
              import java.io.IOException;
              import java.util.List;
                            
              class Test {
                  File getFile(File testDir, List<String> compileClassPath ) throws IOException {
                      assertEquals(new File(testDir, "ejbs/target/classes").getCanonicalFile(),
                          new File(compileClassPath.get(1)).getCanonicalFile());
                  }
                  void assertEquals(File f1, File f2) {}
              }
              """
          )
        );
    }


    @Issue("https://github.com/openrewrite/rewrite/issues/2475")
    @Test
    void enumWithinEnum() {
        rewriteRun(
          spec -> spec.recipe(replaceToStringWithLiteralRecipe),
          java(
            """
              public enum Test {
                  INSTANCE;
                  public enum MatchMode { DEFAULT }
                  public String doSomething() {
                      return "STARTING".toString();
                  }
              }
              """,
            """
              public enum Test {
                  INSTANCE;
                  public enum MatchMode { DEFAULT }
                  public String doSomething() {
                      return "STARTING";
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1339")
    @Test
    void templateStatementIsWithinTryWithResourcesBlock() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
                  var md = getCursor().firstEnclosing(J.MethodDeclaration.class);
                  if (md != null && md.getSimpleName().equals("createBis")) {
                      return newClass;
                  }
                  if (newClass.getType() != null &&
                      TypeUtils.asFullyQualified(newClass.getType()).getFullyQualifiedName().equals("java.io.ByteArrayInputStream") &&
                      !newClass.getArguments().isEmpty()) {
                      return JavaTemplate.builder("createBis(#{anyArray()})")
                        .contextSensitive()
                        .build()
                        .apply(getCursor(), newClass.getCoordinates().replace(), newClass.getArguments().get(0));
                  }
                  return newClass;
              }
          })),
          java(
            """
              import java.io.*;
              import java.nio.charset.StandardCharsets;
                            
              class Test {
                  ByteArrayInputStream createBis(byte[] bytes) {
                      return new ByteArrayInputStream(bytes);
                  }
                  
                  void doSomething() {
                      String sout = "";
                      try (BufferedReader br = new BufferedReader(new FileReader(null))) {
                          new ByteArrayInputStream("bytes".getBytes(StandardCharsets.UTF_8));
                      } catch (IOException e) {
                          e.printStackTrace();
                      }
                  }
              }
              """,
            """
              import java.io.*;
              import java.nio.charset.StandardCharsets;
                            
              class Test {
                  ByteArrayInputStream createBis(byte[] bytes) {
                      return new ByteArrayInputStream(bytes);
                  }
                  
                  void doSomething() {
                      String sout = "";
                      try (BufferedReader br = new BufferedReader(new FileReader(null))) {
                          createBis("bytes".getBytes(StandardCharsets.UTF_8));
                      } catch (IOException e) {
                          e.printStackTrace();
                      }
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1796")
    @Test
    void replaceIdentifierWithMethodInvocation() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                  return method.withBody((J.Block) visit(method.getBody(), ctx));
              }

              @Override
              public J visitIdentifier(J.Identifier identifier, ExecutionContext ctx) {
                  if (identifier.getSimpleName().equals("f")) {
                      return JavaTemplate.apply("#{any(java.io.File)}.getCanonicalFile().toPath()",
                        getCursor(), identifier.getCoordinates().replace(), identifier);
                  }
                  return identifier;
              }
          }).withMaxCycles(1)),
          java(
            """
              import java.io.File;
              class Test {
                  void test(File f) {
                      System.out.println(f);
                  }
              }
              """,
            """
              import java.io.File;
              class Test {
                  void test(File f) {
                      System.out.println(f.getCanonicalFile().toPath());
                  }
              }
              """
          )
        );
    }

    @Test
    void enumClassWithAnonymousInnerClassConstructor() {
        rewriteRun(
          spec -> spec.recipe(replaceToStringWithLiteralRecipe),
          java(
            """
              enum MyEnum {
                  THING_ONE(new MyEnumThing() {
                      @Override
                      public String getName() {
                          return "Thing One".toString();
                      }
                  });
                  private final MyEnumThing enumThing;
                  MyEnum(MyEnumThing myEnumThing) {
                      this.enumThing = myEnumThing;
                  }
                  interface MyEnumThing {
                      String getName();
                  }
              }
              """,
            """
              enum MyEnum {
                  THING_ONE(new MyEnumThing() {
                      @Override
                      public String getName() {
                          return "Thing One";
                      }
                  });
                  private final MyEnumThing enumThing;
                  MyEnum(MyEnumThing myEnumThing) {
                      this.enumThing = myEnumThing;
                  }
                  interface MyEnumThing {
                      String getName();
                  }
              }
              """
          )
        );
    }

    @Test
    void replacingMethodInvocationWithinEnum() {
        rewriteRun(
          spec -> spec.recipe(replaceToStringWithLiteralRecipe),
          java(
            """
              public enum Options {

                  JAR("instance.jar.file"),
                  JVM_ARGUMENTS("instance.vm.args");

                  private String name;

                  Options(String name) {
                      this.name = name;
                  }

                  public String asString() {
                      return System.getProperty(name);
                  }

                  public Integer asInteger(int defaultValue) {
                      String string  = asString();
                      return new Integer(string.toString());
                  }
              }
              """,
            """
              public enum Options {

                  JAR("instance.jar.file"),
                  JVM_ARGUMENTS("instance.vm.args");

                  private String name;

                  Options(String name) {
                      this.name = name;
                  }

                  public String asString() {
                      return System.getProperty(name);
                  }

                  public Integer asInteger(int defaultValue) {
                      String string  = asString();
                      return new Integer(string);
                  }
              }
              """
          )
        );
    }

    @Test
    void replacingMethodInvocationWithinInnerEnum() {
        rewriteRun(
          spec -> spec.recipe(replaceToStringWithLiteralRecipe),
          java(
            """
              public class Test {
                  void doSomething(Options options) {
                      switch (options) {
                          case JAR:
                          case JVM_ARGUMENTS:
                              System.out.println("");
                      }
                  }
                  enum Options {
                      JAR(0, "instance.jar.file".toString()),
                      JVM_ARGUMENTS(1, "instance.vm.args");

                      private final String name;
                      private final int id;

                      Options(int id,String name) {
                          this.id = id;
                          this.name = name;
                      }

                      public String asString() {
                          return System.getProperty(name);
                      }

                      public Integer asInteger(int defaultValue) {
                          String string  = asString();
                          return new Integer(string);
                      }
                  }
              }
              """,
            """
              public class Test {
                  void doSomething(Options options) {
                      switch (options) {
                          case JAR:
                          case JVM_ARGUMENTS:
                              System.out.println("");
                      }
                  }
                  enum Options {
                      JAR(0, "instance.jar.file"),
                      JVM_ARGUMENTS(1, "instance.vm.args");

                      private final String name;
                      private final int id;

                      Options(int id,String name) {
                          this.id = id;
                          this.name = name;
                      }

                      public String asString() {
                          return System.getProperty(name);
                      }

                      public Integer asInteger(int defaultValue) {
                          String string  = asString();
                          return new Integer(string);
                      }
                  }
              }
              """
          )
        );
    }
}
