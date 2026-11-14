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
package org.openrewrite.groovy;

import groovy.lang.GroovyClassLoader;
import org.junit.jupiter.api.Test;
import org.openrewrite.java.AddImport;
import org.openrewrite.java.RemoveImport;
import org.openrewrite.test.RewriteTest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.nio.file.Paths;
import java.util.List;

import static org.openrewrite.groovy.Assertions.groovy;
import static org.openrewrite.test.RewriteTest.toRecipe;

public class GroovyImportHeaderTest implements RewriteTest {
    @Target(ElementType.LOCAL_VARIABLE)
    public @interface Library {
        String[] value();
    }

    @Test
    void addImportPreservesLeadingAnnotatedStatement() throws Exception {
        var classpath = List.of(Paths.get(getClass().getProtectionDomain().getCodeSource().getLocation().toURI()));
        rewriteRun(
          spec -> spec.parser(GroovyParser.builder().classpath(classpath))
            .recipe(toRecipe(() -> new AddImport<>("java.math.RoundingMode", null, false))),
          groovy(
            """
              /* License header */
              @Library(['shared@main']) _

              import java.util.List
              import org.openrewrite.groovy.GroovyImportHeaderTest.Library

              def modes = []
              """,
            """
              /* License header */
              @Library(['shared@main']) _

              import java.math.RoundingMode
              import java.util.List
              import org.openrewrite.groovy.GroovyImportHeaderTest.Library

              def modes = []
              """,
            source -> source.afterRecipe(cu -> {
                try (var loader = new GroovyClassLoader(getClass().getClassLoader())) {
                    loader.parseClass(cu.printAll());
                }
            })
          )
        );
    }

    @Test
    void removeImportPreservesLeadingAnnotatedStatement() throws Exception {
        var classpath = List.of(Paths.get(getClass().getProtectionDomain().getCodeSource().getLocation().toURI()));
        rewriteRun(
          spec -> spec.parser(GroovyParser.builder().classpath(classpath))
            .recipe(toRecipe(() -> new RemoveImport<>("java.math.RoundingMode"))),
          groovy(
            """
              /* License header */
              @Library(['shared@main']) _

              import java.math.RoundingMode
              import java.util.List
              import org.openrewrite.groovy.GroovyImportHeaderTest.Library

              def modes = []
              """,
            """
              /* License header */
              @Library(['shared@main']) _

              import java.util.List
              import org.openrewrite.groovy.GroovyImportHeaderTest.Library

              def modes = []
              """,
            source -> source.afterRecipe(cu -> {
                try (var loader = new GroovyClassLoader(getClass().getClassLoader())) {
                    loader.parseClass(cu.printAll());
                }
            })
          )
        );
    }

    @Test
    void addImportWithoutLeadingStatement() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new AddImport<>("java.math.RoundingMode", null, false))),
          groovy(
            """
              /* License header */
              import java.util.List

              def modes = []
              """,
            """
              /* License header */
              import java.math.RoundingMode
              import java.util.List

              def modes = []
              """
          )
        );
    }

    @Test
    void addExistingImportDoesNotMoveStatements() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new AddImport<>("java.util.List", null, false))),
          groovy(
            """
              /* License header */
              println 'before imports'

              import java.util.List

              def modes = []
              """
          )
        );
    }

}
