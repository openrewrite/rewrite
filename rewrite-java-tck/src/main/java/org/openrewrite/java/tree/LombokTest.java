/*
 * Copyright 2024 the original author or authors.
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

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

@SuppressWarnings("CaughtExceptionImmediatelyRethrown")
class LombokTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().classpath("lombok"));
    }

    @Test
    void getter() {
        rewriteRun(
            java(
                """
                import lombok.Getter;
                
                @Getter
                class A {
                    int n;
                
                    void test() {
                        System.out.println(getN());
                    }
                }
                """
            )
        );
    }

    @Test
    void builder() {
        rewriteRun(
            java(
                """
                import lombok.Builder;
                
                @Builder
                class A {
                    boolean b;
                    int n;
                    String s;
                
                    void test() {
                        A a = A.builder().n(1).b(true).s("foo").build();
                    }
                }
                """
            )
        );
    }

    @Test
    void singular() {
        rewriteRun(
          java(
            """
              import lombok.Builder;
              import lombok.Singular;
              import java.util.Collection;
              import java.util.Set;
              import java.util.SortedMap;
              
              @Builder
              public class SingularExample<T extends Number> {
                  private @Singular Set<String> occupations;
                  private @Singular SortedMap<Integer, T> elves;
                  private @Singular Collection<?> minutiae;
              }
              """
          )
        );
    }

    @Test
    void slf4j() {
        rewriteRun(
          java(
            """
            import lombok.extern.slf4j.Slf4j;
            
            import java.util.Map;
            
            @Slf4j
            class A {
                int n;
                String string;
                Map<String, String> map;
            }
            """
          )
        );
    }

    @Test
    void sneakyThrows() {
        rewriteRun(
          java(
            """
              import lombok.SneakyThrows;
              
              import java.nio.charset.StandardCharsets;
              
              public class SneakyThrowsExample implements Runnable {
                  @SneakyThrows(UnsupportedEncodingException.class)
                  public String utf8ToString(byte[] bytes) {
                      return new String(bytes, StandardCharsets.UTF_8);
                  }
              
                  @SneakyThrows
                  public void run() {
                      try {
                          throw new Throwable();
                      } catch (RuntimeException e) {
                          throw e;
                      }
                  }
              }
              """
          )
        );
    }
}
