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

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class AddOrUpdateAnnotationAttributeTest implements RewriteTest {

    @Test
    void addValueAttribute() {
        rewriteRun(
          spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.Foo", null, "hello", null)),
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
    void updateValueAttribute() {
        rewriteRun(
          spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.Foo", null, "hello", null)),
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
    void removeValueAttribute() {
        rewriteRun(
          spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.example.Foo", null, null, null)),
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
    void addNamedAttribute() {
        rewriteRun(spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.junit.Test", "timeout", "500", null)),
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
          spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.junit.Test", "timeout", "500", null)),
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
          spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.junit.Test", "timeout", null, null)),
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
          spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.junit.Test", "timeout", "500", null)),
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
        rewriteRun(spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.junit.Test", "other", "1", null))
            .expectedCyclesThatMakeChanges(2),
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
    void dontChangeWhenSetToAddOnly() {
        rewriteRun(
          spec -> spec.recipe(new AddOrUpdateAnnotationAttribute("org.junit.Test", "other", "1", true)),
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
}
