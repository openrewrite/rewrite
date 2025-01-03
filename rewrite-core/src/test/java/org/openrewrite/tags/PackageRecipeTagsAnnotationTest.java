/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.tags;

import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;
import org.openrewrite.config.RecipeDescriptor;
import org.openrewrite.tags.child.ChildRecipeWithNoTags;

import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class PackageRecipeTagsAnnotationTest {

    private static class RecipeWithNoTags extends Recipe {
        @Override
        public String getDisplayName() {
            return "Recipe with no tags";
        }

        @Override
        public String getDescription() {
            return "It's tag-less!";
        }

        @Override
        public Set<String> getTags() {
            // Look ma, no tags! 🤣
            return Collections.emptySet();
        }
    }

    @Test
    void testRecipeWithNoTags() {
        RecipeWithNoTags recipe = new RecipeWithNoTags();
        RecipeDescriptor descriptor = recipe.getDescriptor();
        assertThat(descriptor.getTags())
          .withFailMessage("Expected tag to be pulled from package-info.java")
          .contains("What a fun tag!");
    }

    @Test
    void testChildRecipeWithNoTags() {
        ChildRecipeWithNoTags recipe = new ChildRecipeWithNoTags();
        RecipeDescriptor descriptor = recipe.getDescriptor();
        assertThat(descriptor.getTags())
          .withFailMessage("Expected tag to be pulled from parent package-info.java")
          .contains("What a fun tag!");
    }
}
