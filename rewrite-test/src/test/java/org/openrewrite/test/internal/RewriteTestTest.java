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
import org.junit.jupiter.api.Test;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.internal.lang.NonNullApi;
import org.openrewrite.test.RewriteTest;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.openrewrite.test.SourceSpecs.text;

class RewriteTestTest implements RewriteTest {

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
    void acceptsRecipeWithDescriptionListOfLinks() {
        validateRecipeNameAndDescription(new RecipeWithDescriptionListOfLinks());
    }

    @Test
    void acceptsRecipeWithDescriptionListOfDescribedLinks() {
        validateRecipeNameAndDescription(new RecipeWithDescriptionListOfDescribedLinks());
    }

    @Test
    void rejectsRecipeWithDescriptionNotEndingWithPeriod() {
        assertThrows(
          AssertionError.class,
          () -> validateRecipeNameAndDescription(new RecipeWithDescriptionNotEndingWithPeriod())
        );
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

@NonNullApi
class RecipeWithDescriptionListOfLinks extends Recipe {

    @Override
    public String getDisplayName() {
        return "Recipe with name option";
    }

    @Override
    public String getDescription() {
        return "A fancy description.\n" +
                "For more information, see:\n" +
                "  - [link 1](https://example.com/link1)\n" +
                "  - [link 2](https://example.com/link2)";
    }
}

@NonNullApi
class RecipeWithDescriptionListOfDescribedLinks extends Recipe {

    @Override
    public String getDisplayName() {
        return "Recipe with name option";
    }

    @Override
    public String getDescription() {
        return "A fancy description.\n" +
               "For more information, see:\n" +
               "  - First Resource [link 1](https://example.com/link1).\n" +
               "  - Second Resource [link 2](https://example.com/link2).";
    }
}

@NonNullApi
class RecipeWithDescriptionNotEndingWithPeriod extends Recipe {

    @Override
    public String getDisplayName() {
        return "Recipe with name option";
    }

    @Override
    public String getDescription() {
        return "A fancy description";
    }
}

