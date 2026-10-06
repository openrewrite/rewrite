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
package org.openrewrite.gradle;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.Assertions.buildGradleKts;

@SuppressWarnings("GroovyAssignabilityCheck")
class RemoveExtraPropertyTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RemoveExtraProperty("foo"));
    }

    @DocumentExample
    @Test
    void extBlock() {
        rewriteRun(
          buildGradle(
            """
              ext {
                  foo = "bar"
                  baz = "qux"
              }
              """,
            """
              ext {
                  baz = "qux"
              }
              """
          )
        );
    }

    @Test
    void removeAllByName() {
        rewriteRun(
          buildGradle(
            """
              ext {
                  foo = "a"
                  foo = "b"
                  baz = "qux"
              }
              """,
            """
              ext {
                  baz = "qux"
              }
              """
          )
        );
    }

    @Test
    void lastStatementOfExtBlock() {
        rewriteRun(
          buildGradle(
            """
              ext {
                  baz = "qux"
                  foo = "bar"
              }
              """,
            """
              ext {
                  baz = "qux"
              }
              """
          )
        );
    }

    @Test
    void removesExtBlockLeftEmpty() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
              }

              buildscript {
                  ext {
                      foo = "bar"
                  }
              }

              ext {
                  foo = "bar"
              }

              group = "com.example"
              """,
            """
              plugins {
                  id 'java'
              }

              buildscript {
              }

              group = "com.example"
              """
          )
        );
    }

    @Test
    void keepsExtBlockThatWasAlreadyEmpty() {
        rewriteRun(
          buildGradle(
            """
              ext {
              }
              """
          )
        );
    }

    @Test
    void propertyAssignment() {
        rewriteRun(
          buildGradle(
            """
              project.ext.foo = "bar"
              ext.foo = "bar"
              ext.baz = "qux"
              """,
            """
              ext.baz = "qux"
              """
          )
        );
    }

    @Test
    void subscriptAndSetMethod() {
        rewriteRun(
          buildGradle(
            """
              ext['foo'] = "bar"
              ext.set("foo", "bar")
              ext {
                  set('foo', 'bar')
                  set('baz', 'qux')
              }
              """,
            """
              ext {
                  set('baz', 'qux')
              }
              """
          )
        );
    }

    @Test
    void nonLiteralValue() {
        rewriteRun(
          buildGradle(
            """
              def version = "1.0"
              ext.foo = "$version-SNAPSHOT"
              ext.baz = version
              """,
            """
              def version = "1.0"
              ext.baz = version
              """
          )
        );
    }

    @Test
    void removesCommentsAboutRemovedProperty() {
        rewriteRun(
          buildGradle(
            """
              ext {
                  baz = "qux" // kept
                  // pinned for CVE
                  foo = "bar" // removed
                  quux = "corge"
              }
              """,
            """
              ext {
                  baz = "qux" // kept
                  quux = "corge"
              }
              """
          )
        );
    }

    @Test
    void keepsCommentSetApartFromRemovedProperty() {
        rewriteRun(
          buildGradle(
            """
              ext {
                  // Versions

                  foo = "bar"
                  baz = "qux"
              }
              """,
            """
              ext {
                  // Versions

                  baz = "qux"
              }
              """
          )
        );
    }

    @Test
    void leavesVariablesAndReadsAlone() {
        rewriteRun(
          buildGradle(
            """
              def foo = "bar"
              println(ext.foo)
              """
          )
        );
    }

    @Test
    void kotlinDsl() {
        rewriteRun(
          buildGradleKts(
            """
              group = "com.example"
              extra["foo"] = "bar"
              extra.set("foo", "bar")
              extra["baz"] = "qux"
              """,
            """
              group = "com.example"
              extra["baz"] = "qux"
              """
          )
        );
    }
}
