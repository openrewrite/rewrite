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

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.Assertions.kotlin;

class CastTest implements RewriteTest {

    @Test
    void castAs() {
        rewriteRun(
          kotlin(
            """
              fun method ( a : Any ) {
                  val b = a as String
                  val c : String ? = a as? String
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/276")
    @Test
    void parenthesized() {
        rewriteRun(
          kotlin(
            """
              val s = "s" as (String?)
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/276")
    @Test
    void nestedParenthesize() {
        rewriteRun(
          kotlin(
            """
              val b = "s" as (  (   /*c0*/    ( /*c1*/  String? /*c2*/  )   /*c3*/    )      )
              """
          )
        );
    }
}
