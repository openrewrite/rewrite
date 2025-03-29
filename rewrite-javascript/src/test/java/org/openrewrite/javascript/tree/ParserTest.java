/*
 * Copyright 2024 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.javascript.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.javascript.Assertions.javaScript;

class ParserTest implements RewriteTest {

    @Issue("https://github.com/openrewrite/rewrite-javascript/issues/57")
    @Test
    void preservesOrder() {
        rewriteRun(
          javaScript("class A {}", s -> s.path("A.js")),
          javaScript("class B {}", s -> s.path("B.js")),
          javaScript("class C {}", s -> s.path("C.js"))
        );
    }
}
