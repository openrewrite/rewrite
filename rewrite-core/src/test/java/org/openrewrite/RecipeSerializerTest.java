package org.openrewrite;

import java.time.Duration;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

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
