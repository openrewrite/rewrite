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
package org.openrewrite.scala.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.scala.Assertions.scala;

public class MemberReferenceTest implements RewriteTest {
    
    @Test
    void simpleMemberReference() {
        rewriteRun(
            scala(
                """
                class Test {
                  def greet(name: String): String = s"Hello, $name"
                  
                  val greeter = greet _
                }
                """
            )
        );
    }
    
    @Test
    void memberReferenceOnObject() {
        rewriteRun(
            scala(
                """
                class Test {
                  val str = "hello"
                  val upperCaser = str.toUpperCase _
                }
                """
            )
        );
    }
    
    @Test
    void staticMemberReference() {
        rewriteRun(
            scala(
                """
                object Utils {
                  def double(x: Int): Int = x * 2
                }
                
                class Test {
                  val doubler = Utils.double _
                }
                """
            )
        );
    }
    
    @Test
    void memberReferenceAsArgument() {
        rewriteRun(
            scala(
                """
                class Test {
                  def process(x: Int): String = x.toString
                  
                  val numbers = List(1, 2, 3)
                  val strings = numbers.map(process _)
                }
                """
            )
        );
    }
    
    @Test
    void constructorReference() {
        rewriteRun(
            scala(
                """
                case class Person(name: String, age: Int)
                
                class Test {
                  val personConstructor = Person.apply _
                }
                """
            )
        );
    }
    
    @Test
    void partiallyAppliedFunction() {
        rewriteRun(
            scala(
                """
                class Test {
                  def add(x: Int, y: Int): Int = x + y
                  
                  val addFive = add(5, _)
                }
                """
            )
        );
    }
    
    @Test
    void memberReferenceWithTypeParameters() {
        rewriteRun(
            scala(
                """
                class Test {
                  def identity[A](x: A): A = x
                  
                  val identityRef = identity _
                }
                """
            )
        );
    }
    
    @Test
    void memberReferenceInHigherOrderFunction() {
        rewriteRun(
            scala(
                """
                class Test {
                  def twice(x: Int): Int = x * 2
                  
                  def applyFunction(f: Int => Int, x: Int): Int = f(x)
                  
                  val result = applyFunction(twice _, 5)
                }
                """
            )
        );
    }
}