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
package org.openrewrite.java.cleanup;

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class UseThreadNotifyAllTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UseThreadNotifyAll());
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1645")
    @Test
    void useThreadNotifyAll() {
        rewriteRun(
          java(
            """
             public final class ProcessStep implements Runnable {
                 private static final Object lock = new Object();
                 private static int time = 0;
                 private final int step; // Do Perform operations when field time
                                         // reaches this value
                 public ProcessStep(int step) {
                   this.step = step;
                 }           
                
                 @Override public void run() {
                   try {
                     synchronized (lock) {
                       while (time != step) {
                         lock.wait();
                       }             
                       time++;
                       lock.notify();
                       Thread.notify();
                     }
                   } catch (InterruptedException ie) {
                     Thread.currentThread().interrupt(); // Reset interrupted status
                   }
                 }
                 public static void main(String[] args) {
                   for (int i = 4; i >= 0; i--) {
                     new Thread(new ProcessStep(i)).start();
                   }
                 }
               }
              """,
            """
             public final class ProcessStep implements Runnable {
                 private static final Object lock = new Object();
                 private static int time = 0;
                 private final int step; // Do Perform operations when field time
                                         // reaches this value
                 public ProcessStep(int step) {
                   this.step = step;
                 }           
                
                 @Override public void run() {
                   try {
                     synchronized (lock) {
                       while (time != step) {
                         lock.wait();
                       }             
                       time++;
                       lock.notifyAll();
                       Thread.notifyAll();
                     }
                   } catch (InterruptedException ie) {
                     Thread.currentThread().interrupt(); // Reset interrupted status
                   }
                 }
                 public static void main(String[] args) {
                   for (int i = 4; i >= 0; i--) {
                     new Thread(new ProcessStep(i)).start();
                   }
                 }
               }
              """
          )
        );
    }


}
