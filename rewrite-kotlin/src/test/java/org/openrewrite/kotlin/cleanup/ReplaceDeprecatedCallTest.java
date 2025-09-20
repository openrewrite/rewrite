/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.kotlin.cleanup;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.kotlin.Assertions.kotlin;

class ReplaceDeprecatedCallTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ReplaceDeprecatedCall())
          .afterTypeValidationOptions(TypeValidation.all().methodInvocations(false));
    }

    @DocumentExample
    @Test
    void replaceSimpleMethodCall() {
        rewriteRun(
          kotlin(
            """
              class Option<B> {
                  @Deprecated(
                      "orNone is being renamed to getOrNone",
                      ReplaceWith("getOrNone()")
                  )
                  fun orNone(): Option<B> = getOrNone()

                  fun getOrNone(): Option<B> = this

                  fun usage() {
                      orNone()
                  }
              }
              """,
            """
              class Option<B> {
                  @Deprecated(
                      "orNone is being renamed to getOrNone",
                      ReplaceWith("getOrNone()")
                  )
                  fun orNone(): Option<B> = getOrNone()

                  fun getOrNone(): Option<B> = this

                  fun usage() {
                      getOrNone()
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceMethodCallWithTarget() {
        rewriteRun(
          kotlin(
            """
              class Option<B> {
                  @Deprecated(
                      "orNone is being renamed to getOrNone",
                      ReplaceWith("getOrNone()")
                  )
                  fun orNone(): Option<B> = getOrNone()

                  fun getOrNone(): Option<B> = this
              }

              class Usage {
                  fun test(option: Option<String>) {
                      option.orNone()
                  }
              }
              """,
            """
              class Option<B> {
                  @Deprecated(
                      "orNone is being renamed to getOrNone",
                      ReplaceWith("getOrNone()")
                  )
                  fun orNone(): Option<B> = getOrNone()

                  fun getOrNone(): Option<B> = this
              }

              class Usage {
                  fun test(option: Option<String>) {
                      option.getOrNone()
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceMethodWithParameters() {
        rewriteRun(
          kotlin(
            """
              class MyClass {
                  @Deprecated(
                      "Use processData instead",
                      ReplaceWith("processData(value, true)")
                  )
                  fun oldMethod(value: String): String = processData(value, true)

                  fun processData(value: String, flag: Boolean): String = value

                  fun usage() {
                      oldMethod("test")
                  }
              }
              """,
            """
              class MyClass {
                  @Deprecated(
                      "Use processData instead",
                      ReplaceWith("processData(value, true)")
                  )
                  fun oldMethod(value: String): String = processData(value, true)

                  fun processData(value: String, flag: Boolean): String = value

                  fun usage() {
                      processData("test", true)
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceWithImports() {
        rewriteRun(
          kotlin(
            """
              import java.time.Duration

              class Config {
                  @Deprecated(
                      "Use Duration.ofSeconds instead",
                      ReplaceWith("Duration.ofSeconds(seconds)", "java.time.Duration")
                  )
                  fun createDuration(seconds: Long): Duration = Duration.ofSeconds(seconds)

                  fun usage() {
                      createDuration(60)
                  }
              }
              """,
            """
              import java.time.Duration

              class Config {
                  @Deprecated(
                      "Use Duration.ofSeconds instead",
                      ReplaceWith("Duration.ofSeconds(seconds)", "java.time.Duration")
                  )
                  fun createDuration(seconds: Long): Duration = Duration.ofSeconds(seconds)

                  fun usage() {
                      Duration.ofSeconds(60)
                  }
              }
              """
          )
        );
    }

    @Test
    void replacePropertyAccess() {
        rewriteRun(
          kotlin(
            """
              class Person {
                  @Deprecated(
                      "Use fullName instead",
                      ReplaceWith("fullName")
                  )
                  val name: String = fullName

                  val fullName: String = "John Doe"

                  fun usage() {
                      println(name)
                  }
              }
              """,
            """
              class Person {
                  @Deprecated(
                      "Use fullName instead",
                      ReplaceWith("fullName")
                  )
                  val name: String = fullName

                  val fullName: String = "John Doe"

                  fun usage() {
                      println(fullName)
                  }
              }
              """
          )
        );
    }

    @Test
    void replacePropertyWithTarget() {
        rewriteRun(
          kotlin(
            """
              class Person {
                  @Deprecated(
                      "Use fullName instead",
                      ReplaceWith("fullName")
                  )
                  val name: String = fullName

                  val fullName: String = "John Doe"
              }

              class Usage {
                  fun test(person: Person) {
                      println(person.name)
                  }
              }
              """,
            """
              class Person {
                  @Deprecated(
                      "Use fullName instead",
                      ReplaceWith("fullName")
                  )
                  val name: String = fullName

                  val fullName: String = "John Doe"
              }

              class Usage {
                  fun test(person: Person) {
                      println(person.fullName)
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceExtensionFunction() {
        rewriteRun(
          kotlin(
            """
              @Deprecated(
                  "Use toInt() instead",
                  ReplaceWith("this.toInt()")
              )
              fun String.convertToInt(): Int = this.toInt()

              fun usage() {
                  val result = "42".convertToInt()
              }
              """,
            """
              @Deprecated(
                  "Use toInt() instead",
                  ReplaceWith("this.toInt()")
              )
              fun String.convertToInt(): Int = this.toInt()

              fun usage() {
                  val result = "42".toInt()
              }
              """
          )
        );
    }

    @Test
    void replaceWithComplexExpression() {
        rewriteRun(
          kotlin(
            """
              class Calculator {
                  @Deprecated(
                      "Use add with default parameter",
                      ReplaceWith("add(a, b, 0)")
                  )
                  fun addSimple(a: Int, b: Int): Int = add(a, b, 0)

                  fun add(a: Int, b: Int, c: Int = 0): Int = a + b + c

                  fun usage() {
                      val result = addSimple(5, 3)
                  }
              }
              """,
            """
              class Calculator {
                  @Deprecated(
                      "Use add with default parameter",
                      ReplaceWith("add(a, b, 0)")
                  )
                  fun addSimple(a: Int, b: Int): Int = add(a, b, 0)

                  fun add(a: Int, b: Int, c: Int = 0): Int = a + b + c

                  fun usage() {
                      val result = add(5, 3, 0)
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceWithChainedCalls() {
        rewriteRun(
          kotlin(
            """
              class Builder {
                  @Deprecated(
                      "Use setName().setAge() instead",
                      ReplaceWith("setName(name).setAge(age)")
                  )
                  fun configure(name: String, age: Int): Builder = setName(name).setAge(age)

                  fun setName(name: String): Builder = this
                  fun setAge(age: Int): Builder = this

                  fun usage() {
                      configure("John", 30)
                  }
              }
              """,
            """
              class Builder {
                  @Deprecated(
                      "Use setName().setAge() instead",
                      ReplaceWith("setName(name).setAge(age)")
                  )
                  fun configure(name: String, age: Int): Builder = setName(name).setAge(age)

                  fun setName(name: String): Builder = this
                  fun setAge(age: Int): Builder = this

                  fun usage() {
                      setName("John").setAge(30)
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotReplaceInSelfReference() {
        rewriteRun(
          kotlin(
            """
              class Option<B> {
                  @Deprecated(
                      "orNone is being renamed to getOrNone",
                      ReplaceWith("getOrNone()")
                  )
                  fun orNone(): Option<B> = getOrNone()

                  fun getOrNone(): Option<B> = orNone() // Self-reference should not be replaced
              }
              """
          )
        );
    }

    @Test
    void replaceWithMultipleImports() {
        rewriteRun(
          kotlin(
            """
              class Utils {
                  @Deprecated(
                      "Use Duration and Instant directly",
                      ReplaceWith("Duration.between(Instant.EPOCH, Instant.now())", 
                                 "java.time.Duration", "java.time.Instant")
                  )
                  fun getElapsedTime(): Long = 
                      java.time.Duration.between(java.time.Instant.EPOCH, java.time.Instant.now()).toMillis()

                  fun usage() {
                      val elapsed = getElapsedTime()
                  }
              }
              """,
            """
              import java.time.Duration
              import java.time.Instant

              class Utils {
                  @Deprecated(
                      "Use Duration and Instant directly",
                      ReplaceWith("Duration.between(Instant.EPOCH, Instant.now())", 
                                 "java.time.Duration", "java.time.Instant")
                  )
                  fun getElapsedTime(): Long = 
                      java.time.Duration.between(java.time.Instant.EPOCH, java.time.Instant.now()).toMillis()

                  fun usage() {
                      val elapsed = Duration.between(Instant.EPOCH, Instant.now())
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceTopLevelFunction() {
        rewriteRun(
          kotlin(
            """
              @Deprecated(
                  "Use kotlin.io.println instead",
                  ReplaceWith("println(message)")
              )
              fun printMessage(message: String) = println(message)

              fun main() {
                  printMessage("Hello, World!")
              }
              """,
            """
              @Deprecated(
                  "Use kotlin.io.println instead",
                  ReplaceWith("println(message)")
              )
              fun printMessage(message: String) = println(message)

              fun main() {
                  println("Hello, World!")
              }
              """
          )
        );
    }

    @Test
    void replaceWithQualifiedName() {
        rewriteRun(
          kotlin(
            """
              package com.example

              class OldApi {
                  @Deprecated(
                      "Use NewApi instead",
                      ReplaceWith("com.example.NewApi.process(data)", "com.example.NewApi")
                  )
                  fun handle(data: String): String = NewApi.process(data)
              }

              object NewApi {
                  fun process(data: String): String = data
              }

              fun usage() {
                  val api = OldApi()
                  api.handle("test")
              }
              """,
            """
              package com.example

              class OldApi {
                  @Deprecated(
                      "Use NewApi instead",
                      ReplaceWith("com.example.NewApi.process(data)", "com.example.NewApi")
                  )
                  fun handle(data: String): String = NewApi.process(data)
              }

              object NewApi {
                  fun process(data: String): String = data
              }

              fun usage() {
                  val api = OldApi()
                  NewApi.process("test")
              }
              """
          )
        );
    }
}