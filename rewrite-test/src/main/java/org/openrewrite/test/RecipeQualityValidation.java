/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.test;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.jspecify.annotations.Nullable;

/**
 * Checks of how a recipe behaves and what it produces. {@link TypeValidation} checks the LST the recipe is given.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(fluent = true)
@Builder
public class RecipeQualityValidation {

    /**
     * The type attribution the recipe's output is validated to have. When not set, the output is validated as the LST
     * the recipe was given is.
     */
    private @Nullable TypeValidation types;

    /**
     * Controls whether the recipe is validated to leave the execution context, which every recipe and source file in
     * a run shares, unchanged. State across source files belongs in a ScanningRecipe's accumulator, and state within
     * one source file in Cursor messages.
     */
    @Builder.Default
    private boolean executionContextUnchanged = true;

    /**
     * Controls whether a ScanningRecipe's scanner is validated to make no edits, which the scanning phase discards.
     */
    @Builder.Default
    private boolean noEditsInScanningPhase = true;

    /**
     * Controls whether the recipe is validated to add only markers that report, such as search results and markup,
     * and none that it keeps information in.
     */
    @Builder.Default
    private boolean reportingMarkersOnly = true;

    /**
     * Controls whether the recipe is validated to format only what it changed, not a whole source file, class or
     * document root that existed before the run.
     */
    @Builder.Default
    private boolean minimalFormatting = true;

    /**
     * Controls whether the recipe is rerun with the code it changed respelled to mean the same (a static import for a
     * qualified call or the reverse, a fully qualified name for an imported type), and must still change it. Skipped
     * when the test doesn't validate the type attribution of the recipe's input and output.
     */
    @Builder.Default
    private boolean matchByTypeNotName = true;

    /**
     * Controls whether the recipe is validated to import the types and static members the code it writes names, and
     * its templates to declare those they name with imports and staticImports.
     */
    @Builder.Default
    private boolean importReferencedTypes = true;

    /**
     * Controls whether the recipe is validated to produce the tree that the source's parser makes of the text the
     * recipe's output prints, such as a J.Primitive and not a J.Identifier for {@code int}.
     */
    @Builder.Default
    private boolean parserShapedTrees = true;

    /**
     * Controls whether a composite recipe is validated to run each step before the steps that rename what it looks
     * for, such as a ChangeType of a type in a package before a ChangePackage of that package.
     */
    @Builder.Default
    private boolean orderStepsForEarlierChanges = true;

    /**
     * Enable all recipe quality checks.
     */
    public static RecipeQualityValidation all() {
        return new RecipeQualityValidation();
    }

    /**
     * The checks a test asks for, without those it still turns off through the deprecated options of
     * {@link TypeValidation}.
     */
    @SuppressWarnings("deprecation")
    static RecipeQualityValidation of(RecipeSpec testMethodSpec, RecipeSpec testClassSpec) {
        RecipeQualityValidation quality = testMethodSpec.getRecipeQualityValidation() != null ?
                testMethodSpec.getRecipeQualityValidation() : testClassSpec.getRecipeQualityValidation();
        if (quality == null) {
            quality = all();
        }
        TypeValidation before = TypeValidation.before(testMethodSpec, testClassSpec);
        return builder()
                .types(quality.types())
                .executionContextUnchanged(quality.executionContextUnchanged() && before.immutableExecutionContext())
                .noEditsInScanningPhase(quality.noEditsInScanningPhase() && before.immutableScanning())
                .reportingMarkersOnly(quality.reportingMarkersOnly())
                .minimalFormatting(quality.minimalFormatting())
                .matchByTypeNotName(quality.matchByTypeNotName())
                .importReferencedTypes(quality.importReferencedTypes())
                .parserShapedTrees(quality.parserShapedTrees())
                .orderStepsForEarlierChanges(quality.orderStepsForEarlierChanges())
                .build();
    }
}
