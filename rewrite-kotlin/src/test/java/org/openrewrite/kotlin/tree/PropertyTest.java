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

class PropertyTest implements RewriteTest {

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/270")
    @Test
    void genericTypeParameter() {
        rewriteRun(
          kotlin(
            """
              val <T : Any> Collection<T>.nullable: Collection<T?>
                  /*c1*/ get() = this
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/325")
    @SuppressWarnings("RedundantGetter")
    @Test
    void interfaceWithEmptyGetter() {
        rewriteRun(
          kotlin(
            """
              interface Test {
                  val foo: String
                     /*c1*/ get
                  fun bar() = 2
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/299")
    @Test
    void propertyAccessorsWithoutBody() {
        rewriteRun(
          kotlin(
            """
              class Test {
                  var foo: Long
                      private   set
                  var bar: Long
                      @Suppress  get /*C1*/

                  init {
                      foo = 1
                      bar = 2
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/160")
    @Test
    void multipleTypeConstraints() {
        rewriteRun(
          kotlin(
            """
              val <T> T.plus2: Int where  T   : CharSequence  ,   T : Comparable<T>
                  get() = length + 2
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/568")
    @SuppressWarnings({"TrailingWhitespacesInTextBlock", "RedundantSemicolon"})
    @Test
    void propertyAccessorWithTrailingSemiColon() {
        rewriteRun(
          kotlin(
            """
              val <T : Any> Collection<T>.nullable: Collection<T?>
                  /*c1*/ get() = this ;
              """
          )
        );
    }
}
