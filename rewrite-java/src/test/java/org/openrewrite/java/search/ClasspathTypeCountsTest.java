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
package org.openrewrite.java.search;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.marker.JavaProject;
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.java.table.ClasspathTypeCount;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.List;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.java.Assertions.java;

class ClasspathTypeCountsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ClasspathTypeCounts());
    }

    @DocumentExample
    @Test
    void countTypesPerSourceSet() {
        JavaProject project = new JavaProject(randomId(), "app", null);
        JavaSourceSet main = new JavaSourceSet(randomId(), "main", List.of(
          JavaType.ShallowClass.build("org.example.A"),
          JavaType.ShallowClass.build("org.example.B")), emptyMap());
        JavaSourceSet test = new JavaSourceSet(randomId(), "test", List.of(
          JavaType.ShallowClass.build("org.example.C")), emptyMap());
        rewriteRun(
          spec -> spec.dataTable(ClasspathTypeCount.Row.class, rows ->
            assertThat(rows).containsExactlyInAnyOrder(
              new ClasspathTypeCount.Row("app", "main", 2),
              new ClasspathTypeCount.Row("app", "test", 1))),
          java(
            "class Main {}",
            spec -> spec.mapBeforeRecipe(cu -> cu.withMarkers(cu.getMarkers().setByType(project).setByType(main)))
          ),
          java(
            "class OtherMain {}",
            spec -> spec.mapBeforeRecipe(cu -> cu.withMarkers(cu.getMarkers().setByType(project).setByType(main)))
          ),
          java(
            "class MainTest {}",
            spec -> spec.mapBeforeRecipe(cu -> cu.withMarkers(cu.getMarkers().setByType(project).setByType(test)))
          )
        );
    }
}
