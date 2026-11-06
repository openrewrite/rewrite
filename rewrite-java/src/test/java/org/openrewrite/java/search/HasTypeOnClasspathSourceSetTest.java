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
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.List;

import static java.util.Collections.emptyMap;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

class HasTypeOnClasspathSourceSetTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(toRecipe(() -> new HasTypeOnClasspathSourceSet<>("org.example.Lib")));
    }

    @Test
    void matchesOnlyWhenSourceSetClasspathHasType() {
        rewriteRun(
          java(
            "class WithType {}",
            "/*~~>*/class WithType {}",
            spec -> spec.markers(sourceSet(JavaType.ShallowClass.build("org.example.Lib")))
          ),
          java(
            "class WithoutType {}",
            spec -> spec.markers(sourceSet(JavaType.ShallowClass.build("org.example.Other")))
          ),
          java(
            "class WithoutSourceSet {}"
          )
        );
    }

    private static JavaSourceSet sourceSet(JavaType.FullyQualified onClasspath) {
        return new JavaSourceSet(randomId(), "main", List.of(onClasspath), emptyMap());
    }
}
