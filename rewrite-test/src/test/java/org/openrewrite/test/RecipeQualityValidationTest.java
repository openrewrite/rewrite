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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RecipeQualityValidationTest {

    @Test
    void allChecksAreOnExceptTypeAttributedMatching() {
        RecipeQualityValidation all = RecipeQualityValidation.all();
        assertThat(all.cursorAcyclic()).isTrue();
        assertThat(all.immutableExecutionContext()).isTrue();
        assertThat(all.immutableScanning()).isTrue();
        assertThat(all.reportingMarkersOnly()).isTrue();
        assertThat(all.minimalFormatting()).isTrue();
        assertThat(all.matchByTypeNotName()).isTrue();
        // until its false positives are measured
        assertThat(all.typeAttributedMatching()).isFalse();
    }

    @Test
    void deprecatedTypeValidationOptionsStillTurnChecksOff() {
        RecipeQualityValidation quality = RecipeQualityValidation.of(
          RecipeSpec.defaults().typeValidationOptions(TypeValidation.none()), RecipeSpec.defaults());
        assertThat(quality.cursorAcyclic()).isFalse();
        assertThat(quality.immutableExecutionContext()).isFalse();
        assertThat(quality.immutableScanning()).isFalse();
        assertThat(quality.reportingMarkersOnly()).isTrue();
        assertThat(quality.minimalFormatting()).isTrue();
    }

    @Test
    void validateTheTypesOfTheRecipeOutput() {
        TypeValidation none = TypeValidation.none();
        RecipeSpec testMethodSpec = RecipeSpec.defaults()
          .recipeQualityValidation(RecipeQualityValidation.builder().typeValidationOptions(none).build());
        assertThat(TypeValidation.after(testMethodSpec, RecipeSpec.defaults())).isSameAs(none);
    }

    @Test
    void validateTheRecipeOutputLikeItsInputByDefault() {
        TypeValidation before = TypeValidation.builder().methodInvocations(false).build();
        RecipeSpec testMethodSpec = RecipeSpec.defaults().typeValidationOptions(before);
        assertThat(TypeValidation.after(testMethodSpec, RecipeSpec.defaults())).isSameAs(before);
    }

    @SuppressWarnings("deprecation")
    @Test
    void testMethodAfterTypeValidationOptionsOverrideTheTestClass() {
        TypeValidation none = TypeValidation.none();
        RecipeSpec testClassSpec = RecipeSpec.defaults().recipeQualityValidation(
          RecipeQualityValidation.builder().typeValidationOptions(TypeValidation.all()).build());
        assertThat(TypeValidation.after(RecipeSpec.defaults().afterTypeValidationOptions(none), testClassSpec))
          .isSameAs(none);
    }
}
