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

package org.openrewrite.maven.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.config.Environment;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.*;
import static org.openrewrite.maven.Assertions.pomXml;

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
          .scanRuntimeClasspath("org.openrewrite.maven.java")
          .build()
          .activateRecipes("org.openrewrite.maven.java.UseJavaxNullabilityAnnotations"));
    }

    @Test
    void addsMavenDependencyIfNecessary() {
        rewriteRun(mavenProject("nullability",
          pomXml("""
              <project>
                <groupId>org.openrewrite</groupId>
                <artifactId>rewrite</artifactId>
                <version>1</version>
              </project>
              """,
            """
              <project>
                <groupId>org.openrewrite</groupId>
                <artifactId>rewrite</artifactId>
                <version>1</version>
                <dependencies>
                  <dependency>
                    <groupId>javax.annotation</groupId>
                    <artifactId>javax.annotation-api</artifactId>
                    <version>1</version>
                  </dependency>
                </dependencies>
              </project>
              """),
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
    void doesNotAddMavenDependencyIfUnnecessary() {
        rewriteRun(mavenProject("nullability",
          pomXml("""
            <project>
              <groupId>org.openrewrite</groupId>
              <artifactId>rewrite</artifactId>
              <version>1</version>
            </project>
            """),
          srcMainJava(java(""" 
            class Test {
              String nonNullVariable = "";
            }
            """)
          )
        ));
    }
}