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
package org.openrewrite;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RecipeValidationTest {

    @Test
    void validate() {
        assertThat(new JSpecifyAnnotatedRecipeOptions(null).validate().isValid())
          .isTrue();
    }

    @Test
    void deepBothChainDoesNotOverflow() {
        Validated<Object> chain = Validated.none();
        for (int i = 0; i < 25_000; i++) {
            chain = chain.and(Validated.valid("field" + i, i));
        }
        // Would throw StackOverflowError before the fix
        List<Validated.Invalid<Object>> failures = chain.failures();
        assertThat(failures).isEmpty();
    }

    @Test
    void validateAllWithManyChildRecipesDoesNotOverflow() {
        // Simulates a large composite recipe like MigrateToJava25 with 15,000+ sub-recipes.
        // Before the fix, validate() would .and() all children's validations into one deep
        // Both chain, and Both.iterator() would recurse that chain on the call stack.
        Recipe[] children = new Recipe[15_000];
        for (int i = 0; i < children.length; i++) {
            children[i] = new FailingRecipe("child" + i);
        }
        Recipe root = new ParentRecipe(children);

        Collection<Validated<Object>> all = root.validateAll();

        long failureCount = all.stream()
                .flatMap(v -> v.failures().stream())
                .count();
        assertThat(failureCount).isEqualTo(15_000);
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class JSpecifyAnnotatedRecipeOptions extends Recipe {

        @Option(displayName = "An optional field",
          description = "Something that can be null.",
          required = false,
          example = "Anything")
        @Nullable
        String optionalField;

        String displayName = "Validate nullable JSpecify annotations";

        String description = "NullUtils should see these annotations.";
    }

    static class FailingRecipe extends Recipe {
        private final String id;

        FailingRecipe(String id) {
            this.id = id;
        }

        @Override
        public String getDisplayName() {
            return "Failing recipe " + id;
        }

        @Override
        public String getDescription() {
            return "A recipe that always fails validation.";
        }

        @Override
        public String getName() {
            return "test.FailingRecipe." + id;
        }

        @Override
        public Validated<Object> validate() {
            return Validated.invalid(id, null, "always fails");
        }
    }

    static class ParentRecipe extends Recipe {
        private final List<Recipe> children;

        ParentRecipe(Recipe... children) {
            this.children = Arrays.asList(children);
        }

        @Override
        public String getDisplayName() {
            return "Parent recipe";
        }

        @Override
        public String getDescription() {
            return "A recipe with children.";
        }

        @Override
        public List<Recipe> getRecipeList() {
            return children;
        }
    }
}
