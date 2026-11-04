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
package org.openrewrite.java.search;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.*;
import static org.openrewrite.test.SourceSpecs.text;

class ModuleContainsFileTest implements RewriteTest {

    @DocumentExample
    @Test
    void marksFilesInModulesContainingMatchingFile() {
        rewriteRun(
          spec -> spec.recipe(new ModuleContainsFile("**/application.properties")),
          mavenProject("module-a",
            srcMainJava(
              java(
                """
                  class A {}
                  """,
                """
                  /*~~(Module contains file matching pattern: **/application.properties)~~>*/class A {}
                  """
              )
            ),
            srcMainResources(
              text(
                "app.name=test",
                "~~(Module contains file matching pattern: **/application.properties)~~>app.name=test",
                spec -> spec.path("application.properties")
              )
            )
          ),
          mavenProject("module-b",
            srcMainJava(
              java(
                """
                  class B {}
                  """
              )
            )
          )
        );
    }

    @Test
    void noMatchingFile() {
        rewriteRun(
          spec -> spec.recipe(new ModuleContainsFile("**/nonexistent.txt")),
          mavenProject("module-a",
            srcMainJava(
              java(
                """
                  class A {}
                  """
              )
            )
          )
        );
    }

    @Test
    void multipleModulesWithMatchingFile() {
        rewriteRun(
          spec -> spec.recipe(new ModuleContainsFile("**/config.yml")),
          mavenProject("module-a",
            srcMainJava(
              java(
                """
                  class A {}
                  """,
                """
                  /*~~(Module contains file matching pattern: **/config.yml)~~>*/class A {}
                  """
              )
            ),
            srcMainResources(
              text(
                "key: value",
                "~~(Module contains file matching pattern: **/config.yml)~~>key: value",
                spec -> spec.path("config.yml")
              )
            )
          ),
          mavenProject("module-b",
            srcMainJava(
              java(
                """
                  class B {}
                  """,
                """
                  /*~~(Module contains file matching pattern: **/config.yml)~~>*/class B {}
                  """
              )
            ),
            srcMainResources(
              text(
                "other: value",
                "~~(Module contains file matching pattern: **/config.yml)~~>other: value",
                spec -> spec.path("config.yml")
              )
            )
          ),
          mavenProject("module-c",
            srcMainJava(
              java(
                """
                  class C {}
                  """
              )
            )
          )
        );
    }

    @Test
    void multipleFilePatterns() {
        rewriteRun(
          spec -> spec.recipe(new ModuleContainsFile("**/application.properties;**/application.yml")),
          mavenProject("module-a",
            srcMainJava(
              java(
                """
                  class A {}
                  """,
                """
                  /*~~(Module contains file matching pattern: **/application.properties;**/application.yml)~~>*/class A {}
                  """
              )
            ),
            srcMainResources(
              text(
                "app.name=test",
                "~~(Module contains file matching pattern: **/application.properties;**/application.yml)~~>app.name=test",
                spec -> spec.path("application.properties")
              )
            )
          ),
          mavenProject("module-b",
            srcMainJava(
              java(
                """
                  class B {}
                  """,
                """
                  /*~~(Module contains file matching pattern: **/application.properties;**/application.yml)~~>*/class B {}
                  """
              )
            ),
            srcMainResources(
              text(
                "app:\n  name: test",
                "~~(Module contains file matching pattern: **/application.properties;**/application.yml)~~>app:\n  name: test",
                spec -> spec.path("application.yml")
              )
            )
          ),
          mavenProject("module-c",
            srcMainJava(
              java(
                """
                  class C {}
                  """
              )
            )
          )
        );
    }

    @Test
    void nullPatternMatchesAll() {
        rewriteRun(
          spec -> spec.recipe(new ModuleContainsFile(null)),
          mavenProject("module-a",
            srcMainJava(
              java(
                """
                  class A {}
                  """,
                """
                  /*~~(Module contains file matching pattern: null)~~>*/class A {}
                  """
              )
            )
          )
        );
    }
}
