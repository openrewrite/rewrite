/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.test.internal;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.Test;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.internal.lang.NonNullApi;
import org.openrewrite.test.RewriteTest;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.openrewrite.test.SourceSpecs.text;

public class RewriteTestTest implements RewriteTest {

    @Test
    void rejectRecipeWithNameOption() {
        assertThrows(AssertionError.class, () -> rewriteRun(
          spec -> spec.recipe(new RecipeWithNameOption("test")),
          text(
            "hello world!"
          )
        ));
    }

    @Test
    void verifyAll() {
        assertThrows(AssertionError.class, this::assertRecipesConfigure);
    }
}

@SuppressWarnings("FieldCanBeLocal")
@NonNullApi
class RecipeWithNameOption extends Recipe {
    @Option
    private final String name;

    @JsonCreator
    public RecipeWithNameOption(String name) {
        this.name = name;
    }

    @Override
    public String getDisplayName() {
        return "Recipe with name option";
    }

    @Override
    public String getDescription() {
        return "A fancy description.";
    }
}
