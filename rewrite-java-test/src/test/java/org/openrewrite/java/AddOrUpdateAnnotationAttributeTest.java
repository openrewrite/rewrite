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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpec;

import static org.openrewrite.java.Assertions.java;

class AddOrUpdateAnnotationAttributeTest implements RewriteTest {

    @Test
    void addValueAttribute() {
        rewriteRun(
          spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.Foo", null, "hello", null, null, null)),
          java(
            """
              package org.example;
              public @interface Foo {
                  String value() default "";
              }
              """
          ),
          java(
            """
              import org.example.Foo;
              
              @Foo
              public class A {
              }
              """,
            """
              import org.example.Foo;
              
              @Foo("hello")
              public class A {
              }
              """
          )
        );
    }

    @Test
    void addValueAttributeClass() {
        rewriteRun(
          spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.Foo", null, "Integer.class", null, null, null)),
          java(
            """
              package org.example;
              public @interface Foo {
                  Class<? extends Number> value();
              }
              """
          ),
          java(
            """
              import org.example.Foo;
              
              @Foo
              public class A {
              }
              """,
            """
              import org.example.Foo;
              
              @Foo(Integer.class)
              public class A {
              }
              """
          )
        );
    }

    @Test
    void addValueAttributeFullyQualifiedClass() {
        rewriteRun(
          spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.Foo", null, "java.math.BigDecimal.class", null, null, null)),
          java(
            """
              package org.example;
              public @interface Foo {
                  Class<? extends Number> value();
              }
              """
          ),
          java(
            """
              import org.example.Foo;
              
              @Foo
              public class A {
              }
              """,
            """
              import org.example.Foo;
              
              @Foo(java.math.BigDecimal.class)
              public class A {
              }
              """
          )
        );
    }

    @Test
    void updateValueAttribute() {
        rewriteRun(
          spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.Foo", null, "hello", null, null, null)),
          java(
            """
              package org.example;
              public @interface Foo {
                  String value() default "";
              }
              """
          ),

          java(
            """
              import org.example.Foo;
              
              @Foo("goodbye")
              public class A {
              }
              """,
            """
              import org.example.Foo;
              
              @Foo("hello")
              public class A {
              }
              """
          )
        );
    }

    @Test
    void updateValueAttributeClass() {
        rewriteRun(
          spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.Foo", null, "Integer.class", null, null, null)),
          java(
            """
              package org.example;
              public @interface Foo {
                  Class<? extends Number> value();
              }
              """
          ),

          java(
            """
              import org.example.Foo;
              
              @Foo(Long.class)
              public class A {
              }
              """,
            """
              import org.example.Foo;
              
              @Foo(Integer.class)
              public class A {
              }
              """
          )
        );
    }

    @Test
    void removeValueAttribute() {
        rewriteRun(
          spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.Foo", null, null, null, null, null)),
          java(
            """
              package org.example;
              public @interface Foo {
                  String value() default "";
              }
              """
          ),

          java(
            """
              import org.example.Foo;
              
              @Foo("goodbye")
              public class A {
              }
              """,
            """
              import org.example.Foo;
              
              @Foo
              public class A {
              }
              """
          )
        );
    }

    @Test
    void removeValueAttributeClass() {
        rewriteRun(
          spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.Foo", null, null, null, null, null)),
          java(
            """
              package org.example;
              public @interface Foo {
                  Class<? extends Number> value();
              }
              """
          ),

          java(
            """
              import org.example.Foo;
              
              @Foo(Long.class)
              public class A {
              }
              """,
            """
              import org.example.Foo;
              
              @Foo
              public class A {
              }
              """
          )
        );
    }

    @Test
    void addNamedAttribute() {
        rewriteRun(spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.junit.Test", "timeout", "500", null, null, null)),
          java(
            """
              package org.junit;
              public @interface Test {
                  long timeout() default 0L;
              }
              """
          ),
          java(
            """
              import org.junit.Test;
              
              class SomeTest {
              
                  @Test
                  void foo() {
                  }
              }
              """,
            """
              import org.junit.Test;
              
              class SomeTest {
              
                  @Test(timeout = 500)
                  void foo() {
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceAttribute() {
        rewriteRun(
          spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.junit.Test", "timeout", "500", null, null, null)),
          java(
            """
              package org.junit;
              public @interface Test {
                  long timeout() default 0L;
              }
              """
          ),
          java(
            """
              import org.junit.Test;
              
              class SomeTest {
              
                  @Test(timeout = 1)
                  void foo() {
                  }
              }
              """,
            """
              import org.junit.Test;
              
              class SomeTest {
              
                  @Test(timeout = 500)
                  void foo() {
                  }
              }
              """
          )
        );
    }

    @Test
    void removeAttribute() {
        rewriteRun(
          spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.junit.Test", "timeout", null, null, null, null)),
          java(
            """
              package org.junit;
              public @interface Test {
                  long timeout() default 0L;
              }
              """
          ),
          java(
            """
              import org.junit.Test;
              
              class SomeTest {
              
                  @Test(timeout = 1)
                  void foo() {
                  }
              }
              """,
            """
              import org.junit.Test;
              
              class SomeTest {
              
                  @Test
                  void foo() {
                  }
              }
              """
          )
        );
    }

    @Test
    void preserveExistingAttributes() {
        rewriteRun(
          spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.junit.Test", "timeout", "500", null, null, null)),
          java(
            """
              package org.junit;
              public @interface Test {
                  long timeout() default 0L;
                  String foo() default "";
              }
              """
          ),

          java(
            """
              import org.junit.Test;
              
              class SomeTest {
              
                  @Test(foo = "")
                  void foo() {
                  }
              }
              """,
            """
              import org.junit.Test;
              
              class SomeTest {
              
                  @Test(timeout = 500, foo = "")
                  void foo() {
                  }
              }
              """
          )
        );
    }

    @Test
    void implicitValueToExplicitValue() {
        rewriteRun(spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.junit.Test", "other", "1", null, null, null)),
          java(
            """
              package org.junit;
              public @interface Test {
                  long other() default 0L;
                  String value() default "";
              }
              """
          ),
          java(
            """
              import org.junit.Test;
              
              class SomeTest {
              
                  @Test("foo")
                  void foo() {
                  }
              }
              """,
            """
              import org.junit.Test;
              
              class SomeTest {
              
                  @Test(other = 1, value = "foo")
                  void foo() {
                  }
              }
              """
          )
        );
    }

    @Test
    void implicitValueToExplicitValueClass() {
        rewriteRun(spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.junit.Test", "other", "1", null, null, null)),
          java(
            """
              package org.junit;
              public @interface Test {
                  long other() default 0L;
                  Class<? extends Number> value();
              }
              """
          ),
          java(
            """
              import org.junit.Test;
              
              class SomeTest {
              
                  @Test(Integer.class)
                  void foo() {
                  }
              }
              """,
            """
              import org.junit.Test;
              
              class SomeTest {
              
                  @Test(other = 1, value = Integer.class)
                  void foo() {
                  }
              }
              """
          )
        );
    }

    @Test
    void dontChangeWhenSetToAddOnly() {
        rewriteRun(
          spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.junit.Test", "other", "1", null, true, false)),
          java(
            """
              package org.junit;
              public @interface Test {
                  long other() default 0L;
                  String value() default "";
              }
              """
          ),
          java(
            """
              import org.junit.Test;
              
              class SomeTest {
                  @Test(other = 0)
                  void foo() {
                  }
              }
              """
          )
        );
    }

    @Test
    void arrayInAnnotationAttribute() {
        rewriteRun(
          spec -> spec.recipe(new AddOrUpdateAnnotationAttribute(
            "org.example.Foo",
            "array",
            "newTest",
            null,
            false,
            false)),
          java(
            """
              package org.example;
              public @interface Foo {
                  String[] array() default {};
              }
              """
          ),
          java(
            """
              import org.example.Foo;
              
              @Foo(array = {"oldTest"})
              public class A {
              }
              """,
            """
              import org.example.Foo;
              
              @Foo(array = {"newTest"})
              public class A {
              }
              """
          )
        );
    }

    @Test
    void arrayInputMoreThanOneInAnnotationAttribute() {
        rewriteRun(
          spec -> spec.recipe(new AddOrUpdateAnnotationAttribute(
            "org.example.Foo",
            "array",
            "newTest1,newTest2",
            null,
            false,
            false)),
          java(
            """
              package org.example;
              public @interface Foo {
                  String[] array() default {};
              }
              """
          ),
          java(
            """
              import org.example.Foo;
              
              @Foo(array = {"oldTest"})
              public class A {
              }
              """,
            """
              import org.example.Foo;
              
              @Foo(array = {"newTest1", "newTest2"})
              public class A {
              }
              """
          )
        );
    }

    @Test
    void addArrayInputInAnnotationAttribute() {
        rewriteRun(
          spec -> spec.recipe(new AddOrUpdateAnnotationAttribute(
            "org.example.Foo",
            "array",
            "newTest1,newTest2",
            null,
            false,
            false)),
          java(
            """
              package org.example;
              public @interface Foo {
                  String[] array() default {};
              }
              """
          ),
          java(
            """
              import org.example.Foo;
              
              @Foo
              public class A {
              }
              """,
            """
              import org.example.Foo;
              
              @Foo(array = {"newTest1", "newTest2"})
              public class A {
              }
              """
          )
        );
    }

    @Test
    void addArrayInputInAnnotationAttributeWithAppendTrue() {
        rewriteRun(
          spec -> spec.recipe(new AddOrUpdateAnnotationAttribute(
            "org.example.Foo",
            "array",
            "newTest1,newTest2",
            null,
            false,
            true)),
          java(
            """
              package org.example;
              public @interface Foo {
                  String[] array() default {};
              }
              """
          ),
          java(
            """
              import org.example.Foo;
              
              @Foo
              public class A {
              }
              """,
            """
              import org.example.Foo;
              
              @Foo(array = {"newTest1", "newTest2"})
              public class A {
              }
              """
          )
        );
    }

    @Test
    void addArrayInputInAnnotationAttributeEmptyBraces() {
        rewriteRun(
          spec -> spec.recipe(new AddOrUpdateAnnotationAttribute(
            "org.example.Foo",
            "array",
            "newTest1,newTest2",
            null,
            false,
            null)),
          java(
            """
              package org.example;
              public @interface Foo {
                  String[] array() default {};
              }
              """
          ),
          java(
            """
              import org.example.Foo;
              
              @Foo()
              public class A {
              }
              """,
            """
              import org.example.Foo;
              
              @Foo(array = {"newTest1", "newTest2"})
              public class A {
              }
              """
          )
        );
    }

    @Test
    void removeArrayInputInAnnotationAttribute() {
        rewriteRun(
          spec -> spec.recipe(new AddOrUpdateAnnotationAttribute(
            "org.example.Foo",
            "array",
            null,
            null,
            null,
            false)),
          java(
            """
              package org.example;
              public @interface Foo {
                  String[] array() default {};
              }
              """
          ),
          java(
            """
              import org.example.Foo;
              
              @Foo(array = {"newTest1", "newTest2"})
              public class A {
              }
              """,
            """
              import org.example.Foo;
              
              @Foo
              public class A {
              }
              """
          )
        );
    }

    @Test
    void addOtherAttributeInArrayAnnotation() {
        rewriteRun(
          spec -> spec.recipe(new AddOrUpdateAnnotationAttribute(
            "org.example.Foo",
            "string",
            "test",
            null,
            null,
            false)),
          java(
            """
              package org.example;
              public @interface Foo {
                  String[] array() default {};
                  String string() default "";
              }
              """
          ),
          java(
            """
              import org.example.Foo;
              
              @Foo(array = {"newTest1", "newTest2"})
              public class A {
              }
              """,
            """
              import org.example.Foo;
              
              @Foo(string = "test", array = {"newTest1", "newTest2"})
              public class A {
              }
              """
          )
        );
    }

    @Test
    void appendSingleValueToExistingArrayAttribute() {
        rewriteRun(
          spec -> spec.recipe(new AddOrUpdateAnnotationAttribute(
            "org.example.Foo",
            "array",
            "b",
            null,
            false,
            true)),
          java(
            """
              package org.example;
              public @interface Foo {
                  String[] array() default {};
              }
              """
          ),
          java(
            """
              import org.example.Foo;
              
              @Foo(array = {"a"})
              public class A {
              }
              """,
            """
              import org.example.Foo;
              
              @Foo(array = {"a", "b"})
              public class A {
              }
              """
          )
        );
    }

    @Test
    void appendMultipleValuesToExistingArrayAttribute() {
        rewriteRun(
          spec -> spec.recipe(new AddOrUpdateAnnotationAttribute(
            "org.example.Foo",
            "array",
            "b,c",
            null,
            false,
            true)),
          java(
            """
              package org.example;
              public @interface Foo {
                  String[] array() default {};
              }
              """
          ),
          java(
            """
              import org.example.Foo;
              
              @Foo(array = {"a"})
              public class A {
              }
              """,
            """
              import org.example.Foo;
              
              @Foo(array = {"a", "b", "c"})
              public class A {
              }
              """
          )
        );
    }

    @Test
    void appendMultipleValuesToExistingArrayAttributeWithOverlap() {
        rewriteRun(
          spec -> spec.recipe(new AddOrUpdateAnnotationAttribute(
            "org.example.Foo",
            "array",
            "b,c",
            null,
            false,
            true)),
          java(
            """
              package org.example;
              public @interface Foo {
                  String[] array() default {};
              }
              """
          ),
          java(
            """
              import org.example.Foo;
              
              @Foo(array = {"a", "b"})
              public class A {
              }
              """,
            """
              import org.example.Foo;
              
              @Foo(array = {"a", "b", "c"})
              public class A {
              }
              """
          )
        );
    }

    @Test
    void appendMultipleValuesToExistingArrayAttributeNonSet() {
        rewriteRun(
          spec -> spec.recipe(new AddOrUpdateAnnotationAttribute(
            "org.example.Foo",
            "array",
            "b,c",
            null,
            true,
            true)),
          java(
            """
              package org.example;
              public @interface Foo {
                  String[] array() default {};
              }
              
              public class A {
              }
              """
          ),
          java(
            """
              import org.example.Foo;
              
              @Foo(array = {"a", "b"})
              public class A {
              }
              """,
            """
              import org.example.Foo;
              
              @Foo(array = {"a", "b", "b", "c"})
              public class A {
              }
              """
          )
        );
    }

    @Test
    void updateFieldAccessAttribute() {
        rewriteRun(
          spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.Foo", "value", "hello", null, false, null)),
          java(
            """
              package org.example;
              
              public class Const {
                    public static final String HI = "hi";
              }
              """),
          java(
            """
              package org.example;
              public @interface Foo {
                  String value() default "";
              }
              """
          ),
          java(
            """
              import org.example.Foo;
              import org.example.Const;
              
              @Foo(value = Const.HI)
              public class A {
              }
              """,
            """
              import org.example.Foo;
              import org.example.Const;
              
              @Foo(value = "hello")
              public class A {
              }
              """
          )
        );
    }

    @Test
    void addAttributeToNestedAnnotationArray() {
        rewriteRun(
          spec -> spec.recipe(new AddOrUpdateAnnotationAttribute(
            "org.example.Bar",
            "attribute",
            "",
            null,
            null,
            false)),
          java(
            """
              package org.example;
              public @interface Foo {
                  Bar[] array() default {};
              }
              """
          ),
          java(
            """
              package org.example;
              public @interface Bar {
                  String attribute() default "";
              }
              """
          ),
          java(
            """
              import org.example.Foo;
              import org.example.Bar;
              
              @Foo(array = { @Bar() })
              public class A {
              }
              """,
            """
              import org.example.Foo;
              import org.example.Bar;
              
              @Foo(array = { @Bar(attribute = "") })
              public class A {
              }
              """
          )
        );
    }

    @Nested
    class OnMatch {
        @Test
        void matchValue() {
            rewriteRun(
              spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.Foo", null, "hello", "goodbye", null, null)),
              java(
                """
                  package org.example;
                  public @interface Foo {
                      String value() default "";
                  }
                  """,
                SourceSpec::skip
              ),
              java(
                """
                  import org.example.Foo;
                  
                  @Foo("goodbye")
                  public class A {
                  }
                  """,
                """
                  import org.example.Foo;
                  
                  @Foo("hello")
                  public class A {
                  }
                  """
              )
            );
        }

        @Test
        void matchEnumValue() {
            rewriteRun(
              spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.Foo", null, "Values.TWO", "Values.ONE", null, null)),
              java(
                """
                  package org.example;
                  public @interface Foo {
                      Values value() default "";
                  }
                  public enum Values {ONE, TWO}
                  """,
                SourceSpec::skip
              ),
              java(
                """
                  import org.example.Foo;
                  
                  @Foo(Values.ONE)
                  public class A {
                  }
                  """,
                """
                  import org.example.Foo;
                  
                  @Foo(Values.TWO)
                  public class A {
                  }
                  """
              )
            );
        }

        @Test
        void matchValueInArray() {
            rewriteRun(
              spec -> spec.recipe(new AddOrUpdateAnnotationAttribute(
                "org.example.Foo",
                null,
                "hello",
                "goodbye",
                null,
                true)),
              java(
                """
                  package org.example;
                  public @interface Foo {
                      String[] value() default "";
                  }
                  """,
                SourceSpec::skip
              ),
              java(
                """
                  import org.example.Foo;
                  
                  @Foo({"goodbye", "hi"})
                  public class A {
                  }
                  """,
                """
                  import org.example.Foo;
                  
                  @Foo({"hi", "hello"})
                  public class A {
                  }
                  """
              )
            );
        }

        @Test
        void noMatchValue() {
            rewriteRun(
              spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.Foo", null, "hello", "hi", null, null)),
              java(
                """
                  package org.example;
                  public @interface Foo {
                      String value() default "";
                  }
                  """,
                SourceSpec::skip
              ),
              java(
                """
                  import org.example.Foo;
                  
                  @Foo("goodbye")
                  public class A {
                  }
                  """
              )
            );
        }

        @Test
        void matchClass() {
            rewriteRun(
              spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.Foo", null, "Integer.class", "Long.class", null, null)),
              java(
                """
                  package org.example;
                  public @interface Foo {
                      Class<? extends Number> value();
                  }
                  """,
                SourceSpec::skip
              ),
              java(
                """
                  import org.example.Foo;
                  
                  @Foo(Long.class)
                  public class A {
                  }
                  """,
                """
                  import org.example.Foo;
                  
                  @Foo(Integer.class)
                  public class A {
                  }
                  """
              )
            );
        }

        @Test
        void nomatchClass() {
            rewriteRun(
              spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.Foo", null, "Integer.class", "Double.class", null, null)),
              java(
                """
                  package org.example;
                  public @interface Foo {
                      Class<?> value();
                  }
                  """,
                SourceSpec::skip
              ),
              java(
                """
                  import org.example.Foo;
                  
                  @Foo(Long.class)
                  public class A {
                  }
                  """
              )
            );
        }
    }

    @Nested
    class AsValueAttribute {

        @Language("java")
        private static final String FOO_ANNOTATION_WITH_STRING_ARRAY_VALUE = """
          package org.example;
          public @interface Foo {
              String[] value() default {};
          }
          """;

        @Test
        void implicitWithNullAttributeName() {
            rewriteRun(
              spec -> spec.recipe(new AddOrUpdateAnnotationAttribute(
                "org.example.Foo",
                null,
                "b",
                null,
                false,
                true)),
              java(
                FOO_ANNOTATION_WITH_STRING_ARRAY_VALUE,
                SourceSpec::skip
              ),
              java(
                """
                  import org.example.Foo;
                  
                  @Foo({"a"})
                  public class A {
                  }
                  """,
                """
                  import org.example.Foo;
                  
                  @Foo({"a", "b"})
                  public class A {
                  }
                  """
              )
            );
        }

        @Test
        void implicitWithAttributeNameValue() {
            rewriteRun(
              spec -> spec.recipe(new AddOrUpdateAnnotationAttribute(
                "org.example.Foo",
                null,
                "b",
                null,
                false,
                true)),
              java(
                FOO_ANNOTATION_WITH_STRING_ARRAY_VALUE,
                SourceSpec::skip
              ),
              java(
                """
                  import org.example.Foo;
                  
                  @Foo(value = {"a"})
                  public class A {
                  }
                  """,
                """
                  import org.example.Foo;
                  
                  @Foo(value = {"a", "b"})
                  public class A {
                  }
                  """
              )
            );
        }

        @Test
        void explicitWithNullAttributeName() {
            rewriteRun(
              spec -> spec.recipe(new AddOrUpdateAnnotationAttribute(
                "org.example.Foo",
                null,
                "b",
                null,
                false,
                true)),
              java(
                FOO_ANNOTATION_WITH_STRING_ARRAY_VALUE,
                SourceSpec::skip
              ),
              java(
                """
                  import org.example.Foo;
                  
                  @Foo(value = {"a"})
                  public class A {
                  }
                  """,
                """
                  import org.example.Foo;
                  
                  @Foo(value = {"a", "b"})
                  public class A {
                  }
                  """
              )
            );
        }

        @Test
        void explicitWithAttributeNameValue() {
            rewriteRun(
              spec -> spec.recipe(new AddOrUpdateAnnotationAttribute(
                "org.example.Foo",
                "value",
                "b",
                null,
                false,
                true)),
              java(
                FOO_ANNOTATION_WITH_STRING_ARRAY_VALUE,
                SourceSpec::skip
              ),
              java(
                """
                  import org.example.Foo;
                  
                  @Foo(value = {"a"})
                  public class A {
                  }
                  """,
                """
                  import org.example.Foo;
                  
                  @Foo(value = {"a", "b"})
                  public class A {
                  }
                  """
              )
            );
        }
    }
}
