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
package org.openrewrite.kotlin.tree;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.tree.ParserAssertions.kotlin;

public class WhenTest implements RewriteTest {

    @Test
    void when() {
        rewriteRun(
          kotlin(
            """
                  fun method(i: Int) : String {
                      when (i) {
                          1 -> return "1"
                          2 -> return "2"
                          else -> {
                              return "42"
                          }
                      }
                  }
              """
          )
        );
    }

    @Test
    void multiCase() {
        rewriteRun(
          kotlin(
            """
                  fun method(i: Int) : String {
                      when (i) {
                          1 , 2 , 3 -> return "1 or 2 or 3"
                          else -> {
                              return "42"
                          }
                      }
                  }
              """
          )
        );
    }

    @Test
    void inRange() {
        rewriteRun(
          kotlin(
            """
                  fun method(i: Int) : String {
                      when (i) {
                          in 1..10 -> return "in range 1"
                          !in 10..20 -> return "not in range 2"
                          else -> "42"
                      }
                  }
              """
          )
        );
    }
}
