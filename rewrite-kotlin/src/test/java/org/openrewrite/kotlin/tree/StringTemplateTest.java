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

@SuppressWarnings({"KotlinConstantConditions", "ControlFlowWithEmptyBody"})
class StringTemplateTest implements RewriteTest {

    @Test
    void interpolationWithLeadingWhitespace() {
        rewriteRun(
          kotlin(
            """
              val n_functions = ""
              val s =
                  ""\"
                              ${n_functions}
              ""\".trimIndent()
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/260")
    @Test
    void stringTemplate() {
        rewriteRun(
          kotlin(
            """
              import java.lang.annotation.RetentionPolicy

              fun addMember(format: String?, vararg args: Any?): String? {
                  return ""
              }

              fun method(i : Int) {
                  addMember("${'$'}T.${'$'}N", RetentionPolicy::class.java, "CLASS")
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/293")
    @Test
    void templateWithConstDollarBeforeSubstitution() {
        rewriteRun(
          kotlin(
            """
              val s = "$<init>${null}"
              """
          )
        );
    }


    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/306")
    @Test
    void dollarTemplateString() {
        rewriteRun(
          kotlin(
            """
              val customHandler = "${"$"}Handler"
              """
          )
        );
    }

    @Test
    void escapeStringTemplateEntry() {
        rewriteRun(
          kotlin(
            """
              fun addStatement(format: String, vararg args: Any) {
              }

              fun method() {
                  val fields = listOf(1,2,3)
                  val field = "x"
                  val it = "42"
                  addStatement("return ${field}(\\n${fields.joinToString("\\n") { "  ${it} = ${it}," }}\\n)")
              }
              """
          )
        );
    }
}
