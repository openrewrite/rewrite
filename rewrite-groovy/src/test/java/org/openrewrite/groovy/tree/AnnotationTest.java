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

@SuppressWarnings({"GroovyUnusedAssignment", "GrUnnecessarySemicolon"})
class AnnotationTest implements RewriteTest {

    @Test
    void simple() {
        rewriteRun(
          groovy(
            """
              @Foo
              class Test {
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/4055")
    @Test
    void nested() {
        rewriteRun(
          groovy(
            """
              @Foo(bar = @Bar)
              class Test {
              }
              """
          )
        );
    }
}
