/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.config.Environment;
import org.openrewrite.java.style.IntelliJ;

import static org.assertj.core.api.Assertions.assertThat;

class EnvironmentTest {

    @Issue("https://github.com/openrewrite/rewrite/issues/616")
    @Test
    void canLoadRecipeWithZeroArgsConstructorAndPrimaryConstructor() {
        var env = Environment.builder().scanRuntimeClasspath().build();
        var recipe = env.activateRecipes(MixedConstructorRecipe.class.getCanonicalName());
        assertThat(recipe).isNotNull();
    }

    @Test
    void listCategoryDescriptors() {
        var env = Environment.builder().scanRuntimeClasspath().build();
        var categoryDescriptors = env.listCategoryDescriptors();
        assertThat(categoryDescriptors).isNotEmpty();
    }

    @Test
    void listStyles() {
        var env = Environment.builder().scanRuntimeClasspath().build();
        var styles = env.listStyles();
        var intelliJStyle = styles.stream()
          .filter(s -> s.getName().equals(IntelliJ.defaults().getName()))
          .findAny()
          .orElseThrow();
        assertThat(intelliJStyle)
          .as("Environment should be able to find and activate the IntelliJ IDEA style")
          .isNotNull();
    }
}

