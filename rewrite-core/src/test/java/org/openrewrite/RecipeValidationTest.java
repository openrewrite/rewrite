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

import static org.assertj.core.api.Assertions.assertThat;

public class RecipeValidationTest {

    @Test
    void validate() {
        assertThat(new JSpecifyAnnotatedRecipeOptions(null).validate().isValid())
          .isTrue();
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class JSpecifyAnnotatedRecipeOptions extends Recipe {
        @Option(displayName = "An optional field",
          description = "Something that can be null.")
        @Nullable
        String name;

        @Override
        public String getDisplayName() {
            return "Validate nullable JSpecify annotations";
        }

        @Override
        public String getDescription() {
            return "NullUtils should see these annotations.";
        }
    }
}
