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
package org.openrewrite.gradle;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;

@SuppressWarnings("GroovyAssignabilityCheck")
class ChangeExtraPropertyTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ChangeExtraProperty("foo", "baz"));
    }

    @Test
    void closureStyle() {
        rewriteRun(
          buildGradle(
            """
              buildscript {
                  ext {
                      foo = "bar"
                  }
              }
              ext {
                  foo = "bar"
              }
              """,
            """
              buildscript {
                  ext {
                      foo = "baz"
                  }
              }
              ext {
                  foo = "baz"
              }
              """)
        );
    }

    @Test
    void propertyAssignment() {
        rewriteRun(
          buildGradle(
            """
              project.ext.foo = "bar"
              ext.foo = "bar"
              """,
            """
              project.ext.foo = "baz"
              ext.foo = "baz"
              """)
        );
    }

    @Test
    void maintainQuoteStyle() {
        rewriteRun(
          buildGradle(
            """
              ext {
                foo = 'bar'
              }
              """,
            """
              ext {
                foo = 'baz'
              }
              """)
        );
    }
}
