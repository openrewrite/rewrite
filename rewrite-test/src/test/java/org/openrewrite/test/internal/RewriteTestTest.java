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
import lombok.Getter;
import lombok.Value;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.openrewrite.*;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpecs;
import org.openrewrite.test.TypeValidation;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextVisitor;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.openrewrite.test.SourceSpecs.text;

@SuppressWarnings("UnnecessarySemicolon")
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
        AssertionError e = assertThrows(AssertionError.class,
          () -> rewriteRun(
            spec -> spec.recipe(new CreatesTwoFilesSamePath()),
            text(null, "duplicate", spec -> spec.path("duplicate.txt"))));
        assertThat(e).hasStackTraceContaining("Recipe generated multiple source files at the same path");
    }

    @Test
    void generatesFileCollidingWithExistingFile() {
        AssertionError e = assertThrows(AssertionError.class,
          () -> rewriteRun(
            spec -> spec.recipe(new GeneratesExistingFile()),
            text("existing content", spec -> spec.path("existing.txt"))));
        assertThat(e).hasStackTraceContaining("Recipe generated a source file that already exists in the source set");
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


    @Test
    void rejectRecipeValidationFailure() {
        assertThrows(AssertionError.class, () ->
          rewriteRun(
            spec -> spec.recipeFromYaml("""
              type: specs.openrewrite.org/v1beta/recipe
              name: org.openrewrite.RefersToNonExistentRecipe
              displayName: Refers to non-existent recipe
              description: Deliberately has a non-existent recipe in its recipe list to trigger a validation failure.
              recipeList:
                - org.openrewrite.DoesNotExist

              """, "org.openrewrite.RefersToNonExistentRecipe")
          ));
    }

    @Test
    void rejectExecutionContextMutation() {
        assertThrows(AssertionError.class, () ->
          rewriteRun(
            spec -> spec.recipe(new MutateExecutionContext()),
            text("irrelevant")
          ));
    }

    @Test
    void rejectScannerEdit() {
        assertThrows(AssertionError.class, () -> rewriteRun(
          spec -> spec.recipe(new ScannerEdit()),
          text("foo")
        ));
    }

    @Test
    void allowScannerEdit() {
        rewriteRun(
          spec -> spec
            .typeValidationOptions(TypeValidation.builder().immutableScanning(false).build())
            .recipe(new ScannerEdit()),
          text("foo")
        );
    }

    @Test
    void rejectUnconfiguredRecipeWithOptionalOrValidation() {
        // A recipe with optional params and validate() requiring at least one
        // fails when loaded with no arguments and default validation is enabled
        assertThrows(AssertionError.class, () ->
          rewriteRun(
            spec -> spec.recipe(new RecipeWithOptionalOrValidation(null, null)),
            new SourceSpecs[0]
          ));
    }

    @Test
    void acceptUnconfiguredRecipeWithOptionalOrValidationWhenSkipped() {
        // The same recipe succeeds when validation is disabled, as
        // assertRecipesConfigure() does for imperative recipes
        rewriteRun(
          spec -> spec
            .recipe(new RecipeWithOptionalOrValidation(null, null))
            .validateRecipe(false),
          new SourceSpecs[0]
        );
    }

    @Test
    void rejectConfiguredRecipeWithOptionalOrValidationStillValidated() {
        // When the recipe IS configured (e.g. from YAML), validation should
        // still catch real problems like referring to non-existent sub-recipes
        assertThrows(AssertionError.class, () ->
          rewriteRun(
            spec -> spec.recipeFromYaml("""
              type: specs.openrewrite.org/v1beta/recipe
              name: org.openrewrite.test.internal.StillValidated
              displayName: Still validated
              description: Declarative recipe with a non-existent sub-recipe should still fail validation.
              recipeList:
                - org.openrewrite.DoesNotExist

              """, "org.openrewrite.test.internal.StillValidated")
          ));
    }
}

@EqualsAndHashCode(callSuper = false)
@NullMarked
@Value
class ScannerEdit extends ScanningRecipe<AtomicBoolean> {

    String displayName = "Attempts mutation during getScanner()";

    String description = "Any changes attempted by a visitor returned from getScanner() should be an error during test execution.";

    @Override
    public AtomicBoolean getInitialValue(ExecutionContext ctx) {
        return new AtomicBoolean();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(AtomicBoolean acc) {
        return new PlainTextVisitor<>() {
            @Override
            public PlainText visitText(PlainText text, ExecutionContext ctx) {
                return text.withText("mutated");
            }
        };
    }
}

@EqualsAndHashCode(callSuper = false)
@NullMarked
@Value
class MutateExecutionContext extends Recipe {

    String displayName = "Mutate execution context";

    String description = "Mutates the execution context to trigger a validation failure.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new TreeVisitor<>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                ctx.putMessage("mutated", true);
                return tree;
            }
        };
    }
}

@EqualsAndHashCode(callSuper = false)
@NullMarked
@Value
class ImproperCursorUsage extends Recipe {

    String displayName = "Uses cursor improperly";

    String description = "LST elements are acyclic. So a cursor which indicates an element is its own parent is invalid.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        //noinspection NullableProblems
        return new TreeVisitor<>() {
            @Override
            public @Nullable Tree visit(Tree tree, ExecutionContext ctx) {
                return new TreeVisitor<>() {
                }.visit(tree, ctx, new Cursor(getCursor(), tree));
            }
        };
    }
}

@EqualsAndHashCode(callSuper = false)
@NullMarked
@Value
class CreatesTwoFilesSamePath extends ScanningRecipe<AtomicBoolean> {

    String displayName = "Creates two source files with the same path";

    String description = "A source file's path must be unique. " +
          "This recipe creates two source files with the same path to show that the test framework helps protect against this mistake.";

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
                    if ("duplicate.txt".equals(s.getSourcePath().toString())) {
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
        Path duplicatePath = Path.of("duplicate.txt");
        return List.of(PlainText.builder()
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

@NullMarked
@SuppressWarnings({"FieldCanBeLocal", "unused"})
class RecipeWithNameOption extends Recipe {
    @Option
    private final String name;

    @JsonCreator
    public RecipeWithNameOption(String name) {
        this.name = name;
    }

    @Getter
    final String displayName = "Recipe with name option";

    @Getter
    final String description = "A fancy description.";
}

@NullMarked
class RecipeWithDescriptionListOfLinks extends Recipe {

    @Getter
    final String displayName = "Recipe with name option";

    @Getter
    final String description = """
          A fancy description.
          For more information, see:
            - [link 1](https://example.com/link1)
            - [link 2](https://example.com/link2)""";
}

@NullMarked
class RecipeWithDescriptionListOfDescribedLinks extends Recipe {

    @Getter
    final String displayName = "Recipe with name option";

    @Getter
    final String description = """
          A fancy description.
          For more information, see:
            - First Resource [link 1](https://example.com/link1).
            - Second Resource [link 2](https://example.com/link2).""";
}

@NullMarked
class RecipeWithDescriptionNotEndingWithPeriod extends Recipe {

    @Getter
    final String displayName = "Recipe with name option";

    @Getter
    final String description = "A fancy description";
}

@EqualsAndHashCode(callSuper = false)
@NullMarked
@Value
class GeneratesExistingFile extends ScanningRecipe<AtomicBoolean> {

    String displayName = "Generates a file that already exists";

    String description = "A recipe that generates a source file at a path that already exists in the source set.";

    @Override
    public AtomicBoolean getInitialValue(ExecutionContext ctx) {
        return new AtomicBoolean(false);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(AtomicBoolean acc) {
        return TreeVisitor.noop();
    }

    @Override
    public Collection<? extends SourceFile> generate(AtomicBoolean acc, ExecutionContext ctx) {
        return List.of(PlainText.builder()
          .text("generated content")
          .sourcePath(Path.of("existing.txt"))
          .build());
    }
}

@EqualsAndHashCode(callSuper = false)
class RecipeWithOptionalOrValidation extends Recipe {

    @Getter
    final String displayName = "Recipe with optional OR validation";

    @Getter
    final String description = "Has two optional parameters where at least one must be set.";

    @Option(displayName = "Option A",
            description = "First optional parameter.",
            example = "valueA",
            required = false)
    @Nullable
    final String optionA;

    @Option(displayName = "Option B",
            description = "Second optional parameter.",
            example = "valueB",
            required = false)
    @Nullable
    final String optionB;

    @JsonCreator
    RecipeWithOptionalOrValidation(
            @JsonProperty("optionA") @Nullable String optionA,
            @JsonProperty("optionB") @Nullable String optionB) {
        this.optionA = optionA;
        this.optionB = optionB;
    }

    @Override
    public Validated<Object> validate() {
        return super.validate().and(
                Validated.required("optionA", optionA)
                        .or(Validated.required("optionB", optionB)));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return TreeVisitor.noop();
    }
}
