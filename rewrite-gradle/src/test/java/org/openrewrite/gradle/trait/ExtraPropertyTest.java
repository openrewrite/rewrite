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
package org.openrewrite.gradle.trait;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;

class ExtraPropertyTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(RewriteTest.toRecipe(() -> new ExtraProperty.Matcher()
          .asVisitor(prop -> SearchResult.found(prop.getTree()))));
    }

    @DocumentExample
    @Test
    void findsAllSyntaxForms() {
        rewriteRun(
          buildGradle(
            """
              def varProperty = "value1"

              ext {
                  blockProperty = "value2"
              }

              ext.fieldProperty = "value3"

              ext.set("setProperty", "value4")
              """,
            """
              def /*~~>*/varProperty = "value1"

              ext {
                  /*~~>*/blockProperty = "value2"
              }

              /*~~>*/ext.fieldProperty = "value3"

              /*~~>*/ext.set("setProperty", "value4")
              """
          )
        );
    }

    @Test
    void matchesSpecificProperty() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() -> new ExtraProperty.Matcher()
            .propertyName("targetProperty")
            .asVisitor(prop -> SearchResult.found(prop.getTree())))),
          buildGradle(
            """
              ext {
                  targetProperty = "found"
                  otherProperty = "not found"
              }
              """,
            """
              ext {
                  /*~~>*/targetProperty = "found"
                  otherProperty = "not found"
              }
              """
          )
        );
    }

    @Test
    void updatesPropertyValue() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() -> new ExtraProperty.Matcher()
            .propertyName("myVersion")
            .matchVariableDeclarations(false)
            .asVisitor((prop, ctx) -> prop.withValue("2.0.0").getTree()))),
          buildGradle(
            """
              ext {
                  myVersion = "1.0.0"
              }
              """,
            """
              ext {
                  myVersion = "2.0.0"
              }
              """
          )
        );
    }

    @Test
    void updatesSetSyntax() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() -> new ExtraProperty.Matcher()
            .propertyName("myVersion")
            .matchVariableDeclarations(false)
            .asVisitor((prop, ctx) -> prop.withValue("2.0.0").getTree()))),
          buildGradle(
            """
              ext.set("myVersion", "1.0.0")
              """,
            """
              ext.set("myVersion", "2.0.0")
              """
          )
        );
    }

    @Test
    void updatesFieldAccessSyntax() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() -> new ExtraProperty.Matcher()
            .propertyName("myVersion")
            .matchVariableDeclarations(false)
            .asVisitor((prop, ctx) -> prop.withValue("2.0.0").getTree()))),
          buildGradle(
            """
              ext.myVersion = "1.0.0"
              """,
            """
              ext.myVersion = "2.0.0"
              """
          )
        );
    }

    @Test
    void updatesVariableDeclaration() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() -> new ExtraProperty.Matcher()
            .propertyName("myVersion")
            .matchVariableDeclarations(true)
            .asVisitor((prop, ctx) -> prop.withValue("2.0.0").getTree()))),
          buildGradle(
            """
              def myVersion = "1.0.0"
              """,
            """
              def myVersion = "2.0.0"
              """
          )
        );
    }
}
