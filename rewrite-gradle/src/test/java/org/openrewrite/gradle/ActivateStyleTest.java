/*
 * Copyright 2022 the original author or authors.
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
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.properties.Assertions.properties;
import static org.openrewrite.test.SourceSpecs.other;

class ActivateStyleTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ActivateStyle("org.openrewrite.java.IntelliJ", true))
          .typeValidationOptions(TypeValidation.none());
    }

    @Test
    void addToRewriteDsl() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id("java")
                  id("org.openrewrite.rewrite") version "5.34.0"
              }
              
              rewrite {
                  activeRecipe("org.openrewrite.java.format.AutoFormat")
              }
              """,
            """
              plugins {
                  id("java")
                  id("org.openrewrite.rewrite") version "5.34.0"
              }
              
              rewrite {
                  activeRecipe("org.openrewrite.java.format.AutoFormat")
                  activeStyle("org.openrewrite.java.IntelliJ")
              }
              """
          )
        );
    }

    @Test
    void addToRewriteDslExistingStyle() {
        rewriteRun(
          spec -> spec.recipe(new ActivateStyle("org.openrewrite.java.IntelliJ", false)),
          buildGradle(
            """
              plugins {
                  id("java")
                  id("org.openrewrite.rewrite") version "5.34.0"
              }
              
              rewrite {
                  activeRecipe("org.openrewrite.java.format.AutoFormat")
                  activeStyle("otherStyle")
              }
              """,
            """
              plugins {
                  id("java")
                  id("org.openrewrite.rewrite") version "5.34.0"
              }
              
              rewrite {
                  activeRecipe("org.openrewrite.java.format.AutoFormat")
                  activeStyle("otherStyle", "org.openrewrite.java.IntelliJ")
              }
              """
          )
        );
    }

    @Test
    void addToRewriteDslOverwriteStyle() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id("java")
                  id("org.openrewrite.rewrite") version "5.34.0"
              }
              
              rewrite {
                  activeRecipe("org.openrewrite.java.format.AutoFormat")
                  activeStyle("com.your.Style")
              }
              """,
            """
              plugins {
                  id("java")
                  id("org.openrewrite.rewrite") version "5.34.0"
              }
              
              rewrite {
                  activeRecipe("org.openrewrite.java.format.AutoFormat")
                  activeStyle("org.openrewrite.java.IntelliJ")
              }
              """
          )
        );
    }

    @Test
    void addToProperties() {
        rewriteRun(
          other("", spec -> spec.path(".gradle.kts")),
          properties(
            """
              org.gradle.someProp=true
              """,
            """
              org.gradle.someProp=true
              systemProp.rewrite.activeStyles=org.openrewrite.java.IntelliJ
              """,
            spec -> spec.path("gradle.properties")
          )
        );
    }

    @Test
    void addToPropertiesStyles() {
        rewriteRun(
          spec -> spec.recipe(new ActivateStyle("org.openrewrite.java.IntelliJ", false)),
          other("", spec -> spec.path(".gradle.kts")),
          properties(
            """
              org.gradle.someProp=true
              systemProp.rewrite.activeStyles=org.openrewrite.java.Other
              """,
            """
              org.gradle.someProp=true
              systemProp.rewrite.activeStyles=org.openrewrite.java.Other,org.openrewrite.java.IntelliJ
              """,
            spec -> spec.path("gradle.properties")
          )
        );
    }

    @Test
    void overwritePropertiesStyles() {
        rewriteRun(
          other("", spec -> spec.path(".gradle.kts")),
          properties(
            """
              org.gradle.someProp=true
              systemProp.rewrite.activeStyles=org.openrewrite.java.Other
              """,
            """
              org.gradle.someProp=true
              systemProp.rewrite.activeStyles=org.openrewrite.java.IntelliJ
              """,
            spec -> spec.path("gradle.properties")
          )
        );
    }
}
