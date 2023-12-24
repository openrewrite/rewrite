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
package org.openrewrite.ruby.tree;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.ruby.Assertions.ruby;

public class WhileTest implements RewriteTest {

    @ParameterizedTest
    @ValueSource(strings = {"while", "until"})
    void explicitDo(String whileOrUntil) {
        rewriteRun(
          ruby(
            """
              %s true do
                  puts "Hello"
              end
              """.formatted(whileOrUntil)
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"while", "until"})
    void noDo(String whileOrUntil) {
        rewriteRun(
          ruby(
            """
              %s true
                  puts "Hello"
              end
              """.formatted(whileOrUntil)
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"while", "until"})
    void whileModifier(String whileOrUntil) {
        rewriteRun(
          ruby(
            """
              puts "Hello" %s true
              """.formatted(whileOrUntil)
          )
        );
    }

    @Disabled
    @Test
    void beginEndWhileModifier() {
        rewriteRun(
          ruby(
            """
              begin
                  puts "Hello"
              end while true
              """
          )
        );
    }
}
