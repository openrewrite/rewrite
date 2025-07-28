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
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.groovy.Assertions.groovy;

@SuppressWarnings({"GroovyUnusedAssignment", "GroovyUnusedIncOrDec", "GrUnnecessarySemicolon"})
class UnaryTest implements RewriteTest {
    @Test
    void format() {
        rewriteRun(
          groovy(
            """
              int i = 0;
              int j = ++i;
              int k = i ++;
              """
          )
        );
    }

    @Test
    void postfix() {
        rewriteRun(
          groovy(
            """
              int k = i ++;
              """
          )
        );
    }

    @Test
    void prefix() {
        rewriteRun(
          groovy(
            """
              int k = ++i;
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1524")
    @SuppressWarnings("GroovyPointlessBoolean")
    @Test
    void negation() {
        rewriteRun(
          groovy(
            """
              def a = !true
              """
          )
        );
    }
}
