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

package org.openrewrite.java.nullability;

import org.junit.jupiter.api.Test;
import org.openrewrite.config.Environment;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * <Beschreibung>
 * <br>
 *
 * @author askrock
 */
public class UseJavaxNullabilityAnnotationsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(Environment.builder()
          .scanRuntimeClasspath("org.openrewrite.java.nullability")
          .build()
          .activateRecipes("org.openrewrite.java.nullability.UseJavaxNullabilityAnnotations"));
    }

    @Test
    void replacesOtherWellKnownNullabilityAnnotationsWithJavaxAnnotations() {
        fail("not yet implemented");
    }

    @Test
    void addsMavenDependencyIfNecessary() {
        fail("not yet implemented");
    }

    @Test
    void doesNotAddMavenDependencyIfUnnecessary() {
        fail("not yet implemented");
    }

    @Test
    void addsGradleDependencyIfNecessary() {
        fail("not yet implemented");
    }

    @Test
    void doesNotAddGradleDependencyIfUnnecessary() {
        fail("not yet implemented");
    }
}