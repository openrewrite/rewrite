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
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

public class JavaTemplateMatchTest implements RewriteTest {

    @SuppressWarnings("ConstantValue")
    @Test
    void matchBinary() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              private final JavaTemplate template = JavaTemplate.builder(this::getCursor, "1 == #{any(int)}").build();

              @Override
              public J visitBinary(J.Binary binary, ExecutionContext ctx) {
                  return template.matches(binary) ? SearchResult.found(binary) : super.visitBinary(binary, ctx);
              }
          })),
          java(
            """
              class Test {
                  boolean b1 = 1 == 2;
                  boolean b2 = 1 == 3;

                  boolean b3 = 2 == 1;
              }
              """,
            """
              class Test {
                  boolean b1 = /*~~>*/1 == 2;
                  boolean b2 = /*~~>*/1 == 3;

                  boolean b3 = 2 == 1;
              }
              """
          ));
    }

    @SuppressWarnings("ConstantValue")
    @Test
    void matchBinaryUsingCompile() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              // matches manually written class JavaTemplateMatchTest$2_Equals1 below
              private final JavaTemplate template = JavaTemplate.compile(this, "Equals1", (Integer i) -> 1 == i).build();

              @Override
              public J visitBinary(J.Binary binary, ExecutionContext ctx) {
                  return template.matches(binary) ? SearchResult.found(binary) : super.visitBinary(binary, ctx);
              }
          })),
          java(
            """
              class Test {
                  boolean b1 = 1 == 2;
                  boolean b2 = 1 == 3;

                  boolean b3 = 2 == 1;
              }
              """,
            """
              class Test {
                  boolean b1 = /*~~>*/1 == 2;
                  boolean b2 = /*~~>*/1 == 3;

                  boolean b3 = 2 == 1;
              }
              """
          ));
    }

    @Test
    @SuppressWarnings({"UnnecessaryCallToStringValueOf", "RedundantCast"})
    void matchCompatibleTypes() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              private final JavaTemplate template = JavaTemplate.builder(this::getCursor, "#{any(long)}").build();

              @Override
              public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  return template.matches(method) ? SearchResult.found(method) : super.visitMethodInvocation(method, ctx);
              }
          })),
          java(
            """
              class Test {
                  void m() {
                      System.out.println(new Object().hashCode());
                      System.out.println((int) new Object().hashCode());
                      System.out.println(Long.parseLong("123"));
                      System.out.println(String.valueOf(Long.parseLong("123")));

                      System.out.println(new Object());
                      System.out.println(1L);
                  }
              }
              """,
            """
              class Test {
                  void m() {
                      System.out.println(/*~~>*/new Object().hashCode());
                      System.out.println((int) /*~~>*/new Object().hashCode());
                      System.out.println(/*~~>*/Long.parseLong("123"));
                      System.out.println(String.valueOf(/*~~>*/Long.parseLong("123")));

                      System.out.println(new Object());
                      System.out.println(1L);
                  }
              }
              """)
        );
    }

    @Test
    void dontMatchOverloads() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              private final JavaTemplate template = JavaTemplate.builder(this::getCursor, "#{any(java.lang.StringBuilder)}.append(#{any(int)})").build();

              @Override
              public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  return template.matches(method) ? SearchResult.found(method) : super.visitMethodInvocation(method, ctx);
              }
          })),
          java(
            """
              class Test {
                  void m(StringBuilder sb, int i, long l) {
                      sb.append(i);
                      sb.append(1);
                      sb.append(1 + 1);
                      sb.append(1 + 'c');

                      sb.append((short) 1);
                      sb.append(l);
                      sb.append(1L);
                      sb.append((long) 1);
                  }
              }
              """,
            """
              class Test {
                  void m(StringBuilder sb, int i, long l) {
                      /*~~>*/sb.append(i);
                      /*~~>*/sb.append(1);
                      /*~~>*/sb.append(1 + 1);
                      /*~~>*/sb.append(1 + 'c');

                      sb.append((short) 1);
                      sb.append(l);
                      sb.append(1L);
                      sb.append((long) 1);
                  }
              }
              """)
        );
    }
}

/**
 * This class looks like a class which would be generated by the `rewrite-templating` annotation processor
 * and is used by the test {@link JavaTemplateMatchTest#matchBinaryUsingCompile()}.
 */
@SuppressWarnings("unused")
class JavaTemplateMatchTest$2_Equals1 {
    static JavaTemplate.Builder getTemplate(JavaVisitor<?> visitor) {
        return JavaTemplate.builder(visitor::getCursor, "1 == #{any(int)}");
    }

}