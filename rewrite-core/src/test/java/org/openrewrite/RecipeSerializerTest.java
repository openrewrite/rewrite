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

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class RecipeSerializerTest {

    @Test
    void shouldBeAbleToSerializeAndDeserializeEstimatedEffortPerOccurrence() {
        RecipeSerializer serializer = new RecipeSerializer();
        final var recipe = new RecipeWithEstimatedEffortPerOccurrence(Duration.ofDays(1));
        final Recipe read = serializer.read(serializer.write(recipe));
        assertThat(read.getEstimatedEffortPerOccurrence()).isEqualTo(Duration.ofDays(1));
    }

    @NullMarked
    static class RecipeWithEstimatedEffortPerOccurrence extends Recipe {
        @Nullable
        private Duration estimatedEffortPerOccurrence;

        public RecipeWithEstimatedEffortPerOccurrence() {
            this(null);
        }

        public RecipeWithEstimatedEffortPerOccurrence(@Nullable Duration estimatedEffortPerOccurrence) {
            this.estimatedEffortPerOccurrence = estimatedEffortPerOccurrence;
        }

        @Override
        public String getDisplayName() {
            return "Recipe with estimatedEffortPerOccurrence";
        }

        @Override
        public String getDescription() {
            return "A fancy description.";
        }

        @Override
        public @Nullable Duration getEstimatedEffortPerOccurrence() {
            return this.estimatedEffortPerOccurrence;
        }

        public void setEstimatedEffortPerOccurrence(@Nullable Duration estimatedEffortPerOccurrence) {
            this.estimatedEffortPerOccurrence = estimatedEffortPerOccurrence;
        }
    }
}
