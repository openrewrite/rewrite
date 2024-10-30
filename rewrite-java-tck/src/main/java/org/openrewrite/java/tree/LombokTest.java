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
import org.junit.jupiter.api.condition.EnabledOnJre;
import org.junit.jupiter.api.condition.JRE;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

@SuppressWarnings("CaughtExceptionImmediatelyRethrown")
@EnabledOnJre(JRE.JAVA_17)
class LombokTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().classpath("lombok"));
    }

    @Test
    void cleanup() {
        rewriteRun(
            java(
                """
                import lombok.Cleanup;
                import java.io.*;
                
                public class CleanupExample {
                  public static void main(String[] args) throws IOException {
                    @Cleanup InputStream in = new FileInputStream(args[0]);
                    @Cleanup OutputStream out = new FileOutputStream(args[1]);
                    byte[] b = new byte[10000];
                    while (true) {
                      int r = in.read(b);
                      if (r == -1) break;
                      out.write(b, 0, r);
                    }
                  }
                }
                """
            )
        );
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
    void tostring() {
        rewriteRun(
            java(
                """
                import lombok.ToString;
                
                @ToString
                public class ToStringExample {
                  private static final int STATIC_VAR = 10;
                  private String name;
                  private Shape shape = new Square(5, 10);
                  private String[] tags;
                  @ToString.Exclude private int id;
                
                  public static class Shape {}
                
                  @ToString(callSuper=true, includeFieldNames=true)
                  public static class Square extends Shape {
                    private final int width, height;
                
                    public Square(int width, int height) {
                      this.width = width;
                      this.height = height;
                    }
                  }
                }
                """
            )
        );
    }

    @Test
    void equalsAndHashCode() {
        rewriteRun(
            java(
                """
                import lombok.EqualsAndHashCode;
                
                @EqualsAndHashCode
                public class ToStringExample {
                  private static final int STATIC_VAR = 10;
                  private String name;
                  @EqualsAndHashCode.Exclude private Shape shape = new Square(5, 10);
                  private String[] tags;
                  @EqualsAndHashCode.Exclude private int id;
                
                  public static class Shape {}
                
                  @EqualsAndHashCode(callSuper=true)
                  public static class Square extends Shape {
                    private final int width, height;
                
                    public Square(int width, int height) {
                      this.width = width;
                      this.height = height;
                    }
                  }
                }
                """
            )
        );
    }

    @Test
    void constructor() {
        rewriteRun(
            java(
                """
                import lombok.*;
                
                @RequiredArgsConstructor(staticName = "of")
                @AllArgsConstructor(access = AccessLevel.PROTECTED)
                public class ConstructorExample<T> {
                  private int x, y;
                  @NonNull private T description;
                
                  @NoArgsConstructor
                  public static class NoArgsExample {
                    @NonNull private String field;
                  }
                }
                """
            )
        );
    }

    @Test
    void data() {
        rewriteRun(
            java(
                """
                import lombok.AccessLevel;
                import lombok.Setter;
                import lombok.Data;
                import lombok.ToString;
                
                @Data public class DataExample {
                  private final String name;
                  @Setter(AccessLevel.PACKAGE) private int age;
                  private double score;
                  private String[] tags;
                
                  @ToString(includeFieldNames=true)
                  @Data(staticConstructor="of")
                  public static class Exercise<T> {
                    private final String name;
                    private final T value;
                  }
                }
                """
            )
        );
    }

    @Test
    void value() {
        rewriteRun(
            java(
                """
                import lombok.*;
                import lombok.experimental.*;
                
                @Value public class ValueExample {
                  String name;
                  @With(AccessLevel.PACKAGE) @NonFinal int age;
                  double score;
                  protected String[] tags;
                
                  @ToString(includeFieldNames=true)
                  @Value(staticConstructor="of")
                  public static class Exercise<T> {
                    String name;
                    T value;
                  }
                }
                """
            )
        );
    }

    @Test
    void synchronize() {
        rewriteRun(
            java(
                """
                import lombok.Synchronized;
                
                public class SynchronizedExample {
                  private final Object readLock = new Object();
                
                  @Synchronized
                  public static void hello() {
                    System.out.println("world");
                  }
                
                  @Synchronized
                  public int answerToLife() {
                    return 42;
                  }
                
                  @Synchronized("readLock")
                  public void foo() {
                    System.out.println("bar");
                  }
                }
                """
            )
        );
    }

    @Test
    void locked() {
        rewriteRun(
            java(
                """
                import lombok.Locked;
                
                public class LockedExample {
                  private int value = 0;
                
                  @Locked.Read
                  public int getValue() {
                    return value;
                  }
                
                  @Locked.Write
                  public void setValue(int newValue) {
                    value = newValue;
                  }
                
                  @Locked("baseLock")
                  public void foo() {
                    System.out.println("bar");
                  }
                }
                """
            )
        );
    }

    @Test
    void with() {
        rewriteRun(
            java(
                """
                import lombok.AccessLevel;
                import lombok.NonNull;
                import lombok.With;
                
                public class WithExample {
                  @With(AccessLevel.PROTECTED) @NonNull private final String name;
                  @With private final int age;
                
                  public WithExample(@NonNull String name, int age) {
                    this.name = name;
                    this.age = age;
                  }
                }
                """
            )
        );
    }

    @Test
    void lazyGetter() {
        rewriteRun(
            java(
                """
                import lombok.Getter;
                
                public class GetterLazyExample {
                  @Getter(lazy=true) private final double[] cached = expensive();
                
                  private double[] expensive() {
                    double[] result = new double[1000000];
                    for (int i = 0; i < result.length; i++) {
                      result[i] = Math.asin(i);
                    }
                    return result;
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
    void jul() {
        rewriteRun(
          java(
            """
            import lombok.extern.java.Log;
            
            import java.util.Map;
            
            @Log
            class A {
                String string;
                Map<String, String> map;
                void m() {
                    log.info("string = " + string);
                    log.info(() -> "map = %s".formatted(map));
                }
            }
            """
          )
        );
    }

    @Test
    void val() {
        rewriteRun(
          java(
            """
            import lombok.val;
            
            class A {
                void m() {
                    val foo = "foo";
                }
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
              
              import java.io.UnsupportedEncodingException;
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
