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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnJre;
import org.junit.jupiter.api.condition.JRE;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import java.util.List;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.CollectionAssert.assertThatCollection;
import static org.openrewrite.java.Assertions.java;

@SuppressWarnings({"CaughtExceptionImmediatelyRethrown", "LombokGetterMayBeUsed", "LombokSetterMayBeUsed", "DefaultAnnotationParam", "NotNullFieldNotInitialized", "ProtectedMemberInFinalClass", "WriteOnlyObject", "ConcatenationWithEmptyString"})
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

    @Test
    void accessors() {
        rewriteRun(
          java(
            """
              import lombok.experimental.Accessors;
              import lombok.Getter;
              import lombok.Setter;
              
              @Accessors(fluent = true)
              public class AccessorsExample {
                  @Getter @Setter
                  private int age = 10;
                  public static void test() {
                        new AccessorsExample().age(20);
                  }
              }
              
              class PrefixExample {
                  @Accessors(prefix = "f") @Getter
                  private String fName = "Hello, World!";
                  public static String test() {
                      return new PrefixExample().getName();
                  }
              }
              """
          )
        );
    }

    @Test
    void fieldDefaults() {
        rewriteRun(
            java(
                """
                import lombok.AccessLevel;
                import lombok.experimental.FieldDefaults;
                import lombok.experimental.NonFinal;
                import lombok.experimental.PackagePrivate;
                
                @FieldDefaults(makeFinal=true, level=AccessLevel.PRIVATE)
                public class FieldDefaultsExample {
                  public final int a;
                  int b;
                  @NonFinal int c;
                  @PackagePrivate int d;
                  FieldDefaultsExample() {
                    a = 0;
                    b = 0;
                    d = 0;
                  }
                }
                """,
              spec -> spec.afterRecipe(cu -> {
                  List<Statement> statements = cu.getClasses().get(0).getBody().getStatements();
                  assertThatCollection(requireNonNull(((J.VariableDeclarations) statements.get(0)).getVariables().get(0).getVariableType()).getFlags())
                    .as("Field 'a' explicitly specifies its modifiers, overriding the lombok defaults")
                    .containsExactlyInAnyOrder(Flag.Public, Flag.Final);
                  assertThatCollection(requireNonNull(((J.VariableDeclarations) statements.get(1)).getVariables().get(0).getVariableType()).getFlags())
                    .as("Field 'b' does not specify its modifiers, so it should use the lombok defaults")
                    .containsExactlyInAnyOrder(Flag.Private, Flag.Final);
                  assertThatCollection(requireNonNull(((J.VariableDeclarations) statements.get(2)).getVariables().get(0).getVariableType()).getFlags())
                    .as("Field 'c' is annotated with @NonFinal, so it should not be final")
                    .containsExactlyInAnyOrder(Flag.Private);
                  assertThatCollection(requireNonNull(((J.VariableDeclarations) statements.get(3)).getVariables().get(0).getVariableType()).getFlags())
                    .as("Field 'd' is annotated with @PackagePrivate, so it should be package-private")
                    .containsExactlyInAnyOrder(Flag.Final);
              })
            )
        );
    }

    @Test
    void delegate() {
        rewriteRun(
          java("""
                import java.util.ArrayList;
                import java.util.Collection;
                
                import lombok.experimental.Delegate;
                
                public class DelegationExample {
                    private interface SimpleCollection {
                        boolean add(String item);
                        boolean remove(Object item);
                    }
                    @Delegate(types=SimpleCollection.class)
                    private final Collection<String> collection = new ArrayList<>();
                
                    static void test() {
                        DelegationExample example = new DelegationExample();
                        example.add("s");
                        example.remove("s");
                    }
                }
                
                class ExcludesDelegateExample {
                    long counter = 0L;
                    private interface Add {
                        boolean add(String x);
                        boolean addAll(Collection<? extends String> x);
                    }
                    @Delegate(excludes=Add.class)
                    private final Collection<String> collection = new ArrayList<>();
                    public boolean add(String item) {
                        counter++;
                        return collection.add(item);
                    }
                    public boolean addAll(Collection<? extends String> col) {
                        counter += col.size();
                        return collection.addAll(col);
                    }
                }
                """
          )
        );
    }

    @Test
    void utilityClass() {
        rewriteRun(
          java("""
            import lombok.experimental.UtilityClass;
            
            @UtilityClass
            public class UtilityClassExample {
              private final int CONSTANT = 5;
            
              public int addSomething(int in) {
                return in + CONSTANT;
              }
            }
            """
          )
        );
    }

    @Test
    void fieldNameConstants() {
        rewriteRun(
          java("""
            import lombok.experimental.FieldNameConstants;
            
            @FieldNameConstants
            public class FieldNameConstantsExample {
                private final String iAmAField;
                private final int andSoAmI;
                @FieldNameConstants.Exclude private final int asAmI;
            
                public void test() {
                    System.out.println(FieldNameConstantsExample.Fields.iAmAField);
                    System.out.println(FieldNameConstantsExample.Fields.andSoAmI);
                }
            }
            """)
        );
    }

    @Test
    void tolerate() {
        rewriteRun(
          java("""
            import lombok.experimental.Tolerate;
            import lombok.Setter;
            
            public class TolerateExample {
                @Setter
                private String s;
            
                @Tolerate
                public void setS(Object s) {
                    this.s = s.toString();
                }
            
                public void both(String s) {
                    setS(s);
                    setS((Object)s);
                }
            }
            """)
        );
    }

    @Test
    void jacksonized() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().classpath("jackson-annotations", "lombok")),
          java("""
            import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
            import lombok.Builder;
            import lombok.extern.jackson.Jacksonized;
            
            @Jacksonized
            @Builder
            @JsonIgnoreProperties(ignoreUnknown = true)
            public class JacksonExample {
                private List<String> strings;
            }
            """
          )
        );
    }

    @Test
    void standardException() {
        rewriteRun(
          java("""
            import lombok.experimental.StandardException;
            
            @StandardException
            public class ExampleException extends Exception {
            }
            """
          )
        );
    }

    /**
     * These test lombok features that we do not fully support.
     * Code should still parse and print back to its original source code but type information may be missing.
     */
    @SuppressWarnings("MismatchedReadAndWriteOfArray")
    @Nested
    class LessSupported {
        @Test
        void extensionMethod() {
            rewriteRun(
              spec -> spec.typeValidationOptions(TypeValidation.none()),
              java(
                """
                  import lombok.experimental.ExtensionMethod;
                  
                  @ExtensionMethod({java.util.Arrays.class, Extensions.class})
                  public class ExtensionMethodExample {
                      public String test() {
                          int[] intArray = {5, 3, 8, 2};
                          intArray.sort();
                          String iAmNull = null;
                          return iAmNull.or("hELlO, WORlD!".toTitleCase());
                      }
                  }
                  
                  class Extensions {
                      public static <T> T or(T obj, T ifNull) {
                          return obj != null ? obj : ifNull;
                      }
                      public static String toTitleCase(String in) {
                          if (in.isEmpty()) return in;
                          return "" + Character.toTitleCase(in.charAt(0)) + in.substring(1).toLowerCase();
                      }
                  }
                  """
              )
            );
        }

        @Test
        void onConstructor() {
            rewriteRun(
              spec -> spec.typeValidationOptions(TypeValidation.none()),
              java("""
                import lombok.AllArgsConstructor;
                import lombok.Getter;
                import lombok.Setter;
                
                import javax.inject.Inject;
                import javax.persistence.Id;
                import javax.persistence.Column;
                import javax.validation.constraints.Max;
                
                // @__ missing attribution
                @AllArgsConstructor(onConstructor=@__(@Inject))
                public class OnXExample {
                    @Getter(onMethod_={@Id, @Column(name="unique-id")}) //JDK8
                    @Setter(onParam_=@Max(10000)) //JDK8
                    private long unid;
                }
                """)
            );
        }

        @Test
        void onConstructorNoArgs() {
            rewriteRun(
              spec -> spec.typeValidationOptions(TypeValidation.none()),
              java("""
                import lombok.NoArgsConstructor;
                import lombok.Getter;
                import lombok.RequiredArgsConstructor;import lombok.Setter;
                
                import javax.inject.Inject;
                // @__ missing attribution
                @NoArgsConstructor(onConstructor = @__(@Inject))
                @RequiredArgsConstructor(onConstructor_ = @__(@Inject))
                public class OnXExample {
                    private long unid;
                }
                """)
            );
        }

        @Test
        void helper() {
            rewriteRun(
              spec -> spec.typeValidationOptions(TypeValidation.none()),
              java("""
                import lombok.experimental.Helper;
                
                public class HelperExample {
                    int someMethod(int arg1) {
                        int localVar = 5;
                
                        @Helper
                        class Helpers {
                            int helperMethod(int arg) {
                                return arg + localVar;
                            }
                        }
                
                        // helperMethod missing type attribution
                        return helperMethod(10);
                    }
                }
                """
              )
            );
        }
    }
}
