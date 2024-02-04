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
package org.openrewrite.groovy.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.groovy.Assertions.groovy;

class TernaryTest implements RewriteTest {

    @SuppressWarnings("GroovyConstantConditional")
    @Test
    void insideParentheses() {
        rewriteRun(
          groovy("(true ? 1 : 2)"),
          groovy("((true ? 1 : 2))"),

          // NOT inside parentheses, but verifies the parser's
          // test for "inside parentheses" condition
          groovy("(true) ? 1 : 2")
        );
    }

    @Test
    void ternary() {
        rewriteRun(
          groovy("1 == 2 ? /no it isn't/ : /yes it is/")
        );
    }

    @Test
    void elvis() {
        rewriteRun(
          groovy(
            """
              void test() {
                  p = project.findProperty("newVersion") ?: project.findProperty("defaultVersion")
              }
              """
          )
        );
    }

    @Test
    void complex() {
        rewriteRun(
          groovy("""
            (System.env.SYS_USER != null && System.env.SYS_USER != '') ? System.env.SYS_USER : System.env.LOCAL_USER
            (System.env.SYS_PASSWORD != null && System.env.SYS_PASSWORD != '') ? System.env.SYS_PASSWORD : System.env.LOCAL_PASSWORD
            """)
        );
    }

}
