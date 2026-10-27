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

import java.util.ArrayList;
import java.util.List;

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
     * Controls whether a ScanningRecipe's accumulator is validated to keep values, such as coordinates, paths and ids,
     * and no {@link org.openrewrite.Heavyweight} objects such as trees, which would stay in memory all cycle.
     */
    @Builder.Default
    private boolean lightweightAccumulators = true;

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
     * Controls whether the recipe is validated to remove the imports that the code it removes was the only use of,
     * and to add none that nothing uses.
     */
    @Builder.Default
    private boolean removeUnusedImports = true;

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

    /// Controls whether the recipe is validated to settle in the cycles the test expects it to make changes in, with
    /// one more cycle run to show that it changes nothing more, even when the test sets the cycles to run.
    @Builder.Default
    private boolean settles = true;

    /// Why the checks this turns off don't apply, which a test must give when it turns any off, so that a reviewer
    /// reads the reason next to the opt-out.
    private @Nullable String because;

    /// How a failed check's message ends. Without an honest way out, an agent told to get a build green works around
    /// the check instead, such as by rebuilding an object from a few of its fields so that the recipe no longer keeps
    /// the real one.
    static final String WHEN_STUCK = "If you can neither fix the recipe nor explain why it must opt out, leave this " +
                                     "test failing and explain what the recipe needs: there may not be a supported " +
                                     "way to do it yet, and a failing test tells the next person more than a " +
                                     "workaround.";

    static String optOut(String check) {
        return "spec.recipeQualityValidation(RecipeQualityValidation.builder()." + check +
               "(false).because(\"<why the check doesn't apply>\").build())";
    }

    /**
     * Enable all recipe quality checks.
     */
    public static RecipeQualityValidation all() {
        return new RecipeQualityValidation();
    }

    private void requireReasonForChecksTurnedOff() {
        if (because != null && !because.trim().isEmpty()) {
            return;
        }
        List<String> off = new ArrayList<>();
        if (!executionContextUnchanged) {
            off.add("executionContextUnchanged");
        }
        if (!noEditsInScanningPhase) {
            off.add("noEditsInScanningPhase");
        }
        if (!lightweightAccumulators) {
            off.add("lightweightAccumulators");
        }
        if (!reportingMarkersOnly) {
            off.add("reportingMarkersOnly");
        }
        if (!minimalFormatting) {
            off.add("minimalFormatting");
        }
        if (!matchByTypeNotName) {
            off.add("matchByTypeNotName");
        }
        if (!importReferencedTypes) {
            off.add("importReferencedTypes");
        }
        if (!removeUnusedImports) {
            off.add("removeUnusedImports");
        }
        if (!parserShapedTrees) {
            off.add("parserShapedTrees");
        }
        if (!orderStepsForEarlierChanges) {
            off.add("orderStepsForEarlierChanges");
        }
        if (!settles) {
            off.add("settles");
        }
        if (!off.isEmpty()) {
            throw new AssertionError("RecipeQualityValidation turns off " + String.join(", ", off) +
                                     " without saying why. Add .because(\"..\") with the reason the check doesn't " +
                                     "apply to this recipe, so that a reviewer reads it next to the opt-out. " +
                                     WHEN_STUCK);
        }
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
        quality.requireReasonForChecksTurnedOff();
        TypeValidation before = TypeValidation.before(testMethodSpec, testClassSpec);
        return builder()
                .types(quality.types())
                .executionContextUnchanged(quality.executionContextUnchanged() && before.immutableExecutionContext())
                .noEditsInScanningPhase(quality.noEditsInScanningPhase() && before.immutableScanning())
                .lightweightAccumulators(quality.lightweightAccumulators())
                .reportingMarkersOnly(quality.reportingMarkersOnly())
                .minimalFormatting(quality.minimalFormatting())
                .matchByTypeNotName(quality.matchByTypeNotName())
                .importReferencedTypes(quality.importReferencedTypes())
                .removeUnusedImports(quality.removeUnusedImports())
                .parserShapedTrees(quality.parserShapedTrees())
                .orderStepsForEarlierChanges(quality.orderStepsForEarlierChanges())
                .settles(quality.settles())
                .because(quality.because())
                .build();
    }
}
