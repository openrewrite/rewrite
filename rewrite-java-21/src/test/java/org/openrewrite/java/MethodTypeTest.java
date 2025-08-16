/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

class MethodTypeTest implements RewriteTest {
    @Issue("https://github.com/openrewrite/rewrite/issues/5400")
    @Test
    void guavaMethodParameterNamesPresent() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().classpath("guava")),
          java(
            """
              import com.google.common.base.Strings;
              class Regular {
                  String repeatString(String s, int n) {
                      return Strings.repeat(s, n);
                  }
              }
              """,
            spec -> spec.beforeRecipe(cu -> {
                J.MethodDeclaration md = (J.MethodDeclaration) cu.getClasses().getFirst().getBody().getStatements().getFirst();
                J.Return re = (J.Return) md.getBody().getStatements().getFirst();
                J.MethodInvocation mi = (J.MethodInvocation) re.getExpression();
                assertThat(mi.getMethodType().getParameterNames()).containsExactly("string", "count");
            })
          )
        );
    }
}
