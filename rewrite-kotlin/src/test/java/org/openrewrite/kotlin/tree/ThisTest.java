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

class ThisTest implements RewriteTest {

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/302")
    @Test
    void qualifiedThis() {
        rewriteRun(
          kotlin(
            """
              import kotlin.collections.MutableMap.MutableEntry

              abstract class LinkedHashTreeMap<K, V> : AbstractMutableMap<K, V>() {
                override var size = 0

                abstract inner class EntrySet : AbstractMutableSet<MutableEntry<K, V>>() {
                  override val size: Int
                   /*C1*/ get() = this@LinkedHashTreeMap.size

                  override fun iterator(): MutableIterator<MutableEntry<K, V>> {
                    return null!!
                  }
                }
              }
              """
          )
        );
    }

    @Test
    void innerClass() {
        rewriteRun(
          kotlin(
            """
              class Foo {
                  fun bar() = this.Bar()
                  inner class Bar
              }
              """
          )
        );
    }
}
