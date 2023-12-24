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

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.ruby.Assertions.ruby;

public class MethodDeclarationTest implements RewriteTest {

    @Test
    void noArgs() {
        rewriteRun(
          ruby(
            """
              def test
                 i = 42
              end
              """
          )
        );
    }

    @Test
    void singleArg() {
        rewriteRun(
          ruby(
            """
              def test(a1 = "Ruby")
                  puts "The programming language is #{a1}"
              end
              """
          )
        );
    }

    @Test
    void twoArgs() {
        rewriteRun(
          ruby(
            """
              def test(a1 = "Ruby", a2 = "Perl")
                  puts "The programming language is #{a1}"
              end
              """
          )
        );
    }

    @Test
    void argNoInitializer() {
        rewriteRun(
          ruby(
            """
              def sum(a1, a2)
                  a1 + a2
              end
              """
          )
        );
    }
}
