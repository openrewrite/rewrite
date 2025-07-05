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
package org.openrewrite.groovy.tree;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.groovy.Assertions.groovy;

class DestructuringTest implements RewriteTest {

    @Test
    void basicDestructuring() {
        rewriteRun(
          groovy(
            """
              def (key, value) = "a1:b2".split(":")
              println(key)
              println(value)
              """
          )
        );
    }

    @Test
    void destructuringWithSpaces() {
        rewriteRun(
          groovy(
            """
              def ( key, value ) = "a1:b2".split(":")
              println(key)
              println(value)
              """
          )
        );
    }

    @Test
    void destructuringWithoutSpaces() {
        rewriteRun(
          groovy(
            """
              def(key,value)="a1:b2".split(":")
              println(key)
              println(value)
              """
          )
        );
    }

    @Test
    void destructuringWithType() {
        rewriteRun(
          groovy(
            """
              def (String key, java.lang.String value) = "a1:b2".split(":")
              println("${key} ${value}")
              """
          )
        );
    }

    @Test
    void destructuringWithTypeAndSpaces() {
        rewriteRun(
          groovy(
            """
              def (   String key  , java.lang.String value  ) = "a1:b2".split(":")
              println("${key} ${value}")
              """
          )
        );
    }

    @Test
    void destructuringWithList() {
        rewriteRun(
          groovy(
            """
              def (first, second, third) = [1, 2, 3]
              println("${first} ${second} ${third}")
              """
          )
        );
    }

    @Test
    void destructuringWithVariousSpacing() {
        rewriteRun(
          groovy(
            """
              def  (first,  int           second,   third)   =     [1, 2, 3]
              println("${first} ${second} ${third}")
              """
          )
        );
    }
}
