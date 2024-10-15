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
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.openrewrite.*;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;
import org.openrewrite.text.PlainText;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Collections.emptyList;
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

    @Test
    void multipleFilesWithSamePath() {
        assertThrows(AssertionError.class,
          () -> rewriteRun(
            spec -> spec.recipe(new CreatesTwoFilesSamePath()),
            text(null, "duplicate", spec -> spec.path("duplicate.txt"))));
    }

    @Test
    void cursorValidation() {
        assertThrows(AssertionError.class, () ->
          rewriteRun(
            spec -> spec.recipe(new ImproperCursorUsage()),
            text("")
          )
        );

        rewriteRun(
          spec -> spec.recipe(new ImproperCursorUsage()).typeValidationOptions(TypeValidation.builder()
            .cursorAcyclic(false)
            .build()),
          text("")
        );
    }
}

@Value
@EqualsAndHashCode(callSuper = false)
@NullMarked
class ImproperCursorUsage extends Recipe {

    @Override
    public String getDisplayName() {
        return "Uses cursor improperly";
    }

    @Override
    public String getDescription() {
        return "LST elements are acyclic. So a cursor which indicates an element is its own parent is invalid.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        //noinspection NullableProblems
        return new TreeVisitor<>() {
            @Override
            public @Nullable Tree visit(Tree tree, ExecutionContext ctx) {
                return new TreeVisitor<>(){}.visit(tree, ctx, new Cursor(getCursor(), tree));
            }
        };
    }
}

@Value
@EqualsAndHashCode(callSuper = false)
@NullMarked
class CreatesTwoFilesSamePath extends ScanningRecipe<AtomicBoolean> {

    @Override
    public String getDisplayName() {
        return "Creates two source files with the same path";
    }

    @Override
    public String getDescription() {
        return "A source file's path must be unique. " +
               "This recipe creates two source files with the same path to show that the test framework helps protect against this mistake.";
    }

    @Override
    public AtomicBoolean getInitialValue(ExecutionContext ctx) {
        return new AtomicBoolean(false);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(AtomicBoolean alreadyExists) {
        return new TreeVisitor<>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof SourceFile s) {
                    if (s.getSourcePath().toString().equals("duplicate.txt")) {
                        alreadyExists.set(true);
                    }
                }
                return tree;
            }
        };
    }

    @Override
    public Collection<? extends SourceFile> generate(AtomicBoolean alreadyExists, ExecutionContext ctx) {
        if (alreadyExists.get()) {
            return emptyList();
        }
        Path duplicatePath = Paths.get("duplicate.txt");
        return Arrays.asList(PlainText.builder()
            .text("duplicate")
            .sourcePath(duplicatePath)
            .build(),
          PlainText.builder()
            .text("duplicate")
            .sourcePath(duplicatePath)
            .build()
        );
    }
}

@SuppressWarnings({"FieldCanBeLocal", "unused"})
@NullMarked
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

@NullMarked
class RecipeWithDescriptionListOfLinks extends Recipe {

    @Override
    public String getDisplayName() {
        return "Recipe with name option";
    }

    @Override
    public String getDescription() {
        return """
          A fancy description.
          For more information, see:
            - [link 1](https://example.com/link1)
            - [link 2](https://example.com/link2)""";
    }
}

@NullMarked
class RecipeWithDescriptionListOfDescribedLinks extends Recipe {

    @Override
    public String getDisplayName() {
        return "Recipe with name option";
    }

    @Override
    public String getDescription() {
        return """
          A fancy description.
          For more information, see:
            - First Resource [link 1](https://example.com/link1).
            - Second Resource [link 2](https://example.com/link2).""";
    }
}

@NullMarked
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
