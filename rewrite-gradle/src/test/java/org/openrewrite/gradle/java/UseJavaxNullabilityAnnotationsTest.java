/*
  ------------------------------------------------------------------------------
        (c) by data experts gmbh
              Postfach 1130
              Woldegker Str. 12
              17001 Neubrandenburg

  Dieses Dokument und die hierin enthaltenen Informationen unterliegen
  dem Urheberrecht und duerfen ohne die schriftliche Genehmigung des
  Herausgebers weder als ganzes noch in Teilen dupliziert oder reproduziert
  noch manipuliert werden.
*/

package org.openrewrite.gradle.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.config.Environment;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.Assertions.settingsGradle;
import static org.openrewrite.java.Assertions.*;

/**
 * <Beschreibung>
 * <br>
 *
 * @author askrock
 */
class UseJavaxNullabilityAnnotationsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(Environment.builder()
          .scanRuntimeClasspath("org.openrewrite.gradle.java")
          .build()
          .activateRecipes("org.openrewrite.gradle.java.UseJavaxNullabilityAnnotations"));
    }

    @Test
    void addsGradleDependencyIfNecessary() {
        rewriteRun(mavenProject("nullability",
          settingsGradle(
            """
              rootProject.name = "nullability"
              """
          ),
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }
              """,
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }
                          
              dependencies {
                  implementation "javax.annotation:javax.annotation-api:1"
              }
              """
          ),
          srcMainJava(java("""
            import org.openrewrite.internal.lang.NonNull;
                            
            class Test {
              @NonNull
              String nonNullVariable = "";
            }
            """, """
            import javax.annotation.Nonnull;
                            
            class Test {
              @Nonnull
              String nonNullVariable = "";
            }
            """)
          )
        ));
    }

    @Test
    void doesNotAddGradleDependencyIfUnnecessary() {
        rewriteRun(mavenProject("nullability",
          settingsGradle(
            """
              rootProject.name = "nullability"
              """
          ),
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }
              """
          ),
          srcMainJava(java("""
            class Test {
              String nonNullVariable = "";
            }
            """)
          )
        ));
    }
}