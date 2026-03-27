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

public class SynchronizedTest implements RewriteTest {
    
    @Test
    void synchronizedBlock() {
        rewriteRun(
            scala(
                """
                class Counter {
                  private var count = 0
                  
                  def increment(): Unit = synchronized {
                    count += 1
                  }
                }
                """
            )
        );
    }
    
    @Test
    void synchronizedWithExplicitMonitor() {
        rewriteRun(
            scala(
                """
                class SharedResource {
                  private val lock = new Object()
                  private var data = 0
                  
                  def update(value: Int): Unit = lock.synchronized {
                    data = value
                  }
                }
                """
            )
        );
    }
    
    @Test
    void synchronizedMethod() {
        rewriteRun(
            scala(
                """
                class SyncExample {
                  def syncMethod(): String = synchronized {
                    "thread-safe"
                  }
                }
                """
            )
        );
    }
    
    @Test
    void nestedSynchronized() {
        rewriteRun(
            scala(
                """
                class NestedSync {
                  private val lock1 = new Object()
                  private val lock2 = new Object()
                  
                  def doWork(): Unit = lock1.synchronized {
                    println("Outer lock")
                    lock2.synchronized {
                      println("Inner lock")
                    }
                  }
                }
                """
            )
        );
    }
    
    @Test
    void synchronizedWithReturn() {
        rewriteRun(
            scala(
                """
                class ReturnSync {
                  def getValue(): Int = synchronized {
                    if (true) return 42
                    0
                  }
                }
                """
            )
        );
    }
}