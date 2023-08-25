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
package org.openrewrite.kotlin;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.search.FindMethods;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.Assertions.kotlin;

public class FindMethodsTest implements RewriteTest {

    @Test
    void jvmStaticMethod() {
        rewriteRun(
          spec -> spec.recipe(new FindMethods("java.lang.Integer decode(..)", false)),
          kotlin(
            """
              import java.lang.Integer
              import java.lang.Integer.decode
              
              val i1 = java.lang.Integer.decode("1")
              val i2 = Integer.decode("1")
              val i3 = decode("1")
              val i4 = listOf("1").map {Integer::decode}
              val i5 = listOf("1").map {::decode}
              """,
            """
              import java.lang.Integer
              import java.lang.Integer.decode
              
              val i1 = /*~~>*/java.lang.Integer.decode("1")
              val i2 = /*~~>*/Integer.decode("1")
              val i3 = /*~~>*/decode("1")
              val i4 = listOf("1").map {Integer::/*~~>*/decode}
              val i5 = listOf("1").map {::/*~~>*/decode}
              """
          )
        );
    }
}
