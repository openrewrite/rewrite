/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.groovy.search;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.java.search.HasMinimumJavaVersion;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.groovy.Assertions.groovy;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.javaVersion;

@Issue("https://github.com/moderneinc/customer-requests/issues/2389")
class HasMinimumJavaVersionTest implements RewriteTest {

    @DocumentExample
    @Test
    void groovyOnly() {
        rewriteRun(
          spec -> spec.recipe(new HasMinimumJavaVersion("17", false)),
          groovy(
            "class A {}",
            "/*~~(Java version 17)~~>*/class A {}",
            spec -> spec.markers(javaVersion(17))
          )
        );
    }

    @Test
    void mixedJavaAndGroovy() {
        rewriteRun(
          spec -> spec.recipe(new HasMinimumJavaVersion("17", false)),
          java(
            "class A {}",
            "/*~~(Java version 17)~~>*/class A {}",
            spec -> spec.markers(javaVersion(17))
          ),
          groovy(
            "class B {}",
            "/*~~(Java version 17)~~>*/class B {}",
            spec -> spec.markers(javaVersion(17))
          )
        );
    }

    @Test
    void groovyBelowMinimumFailsCheck() {
        rewriteRun(
          spec -> spec.recipe(new HasMinimumJavaVersion("17", false)),
          java(
            "class A {}",
            spec -> spec.markers(javaVersion(17))
          ),
          groovy(
            "class B {}",
            spec -> spec.markers(javaVersion(11))
          )
        );
    }
}
