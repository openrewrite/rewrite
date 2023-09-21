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
package org.openrewrite.kotlin.recipe;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.ChangeType;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.Assertions.kotlin;

class changeTypeTest implements RewriteTest {

    @Test
    void changeType() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("java.util.ArrayList", "java.util.LinkedList", true)),
          kotlin(
            """
              import java.util.ArrayList

              fun main() {
                  val list = ArrayList<String>()
              }
              """,
            """
              import java.util.LinkedList

              fun main() {
                  val list = LinkedList<String>()
              }
              """
          )
        );
    }

    @Test
    void addImportAlias() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("java.util.ArrayList", "java.util.LinkedList", true)),
          kotlin(
            """
              import java.util.ArrayList as MyList

              fun main() {
                  val list = ArrayList<String>()
                  val list2 = MyList<String>()
              }
              """,
            """
              import java.util.LinkedList as MyList

              fun main() {
                  val list = LinkedList<String>()
                  val list2 = MyList<String>()
              }
              """
          )
        );
    }

    @Test
    void updateImportAlias() {
        rewriteRun(
          spec -> spec.recipe(new ChangeType("java.util.ArrayList", "java.util.LinkedList", true)),
          kotlin(
            """
              import java.util.ArrayList as MyList
              import java.util.LinkedList

              fun main() {
                  val list = ArrayList<String>()
                  val list2 = MyList<String>()
              }
              """,
            """
              import java.util.LinkedList as MyList

              fun main() {
                  val list = LinkedList<String>()
                  val list2 = MyList<String>()
              }
              """
          )
        );
    }
}
