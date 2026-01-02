/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.marketplace;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openrewrite.Validated;

import static org.assertj.core.api.Assertions.assertThat;

class RecipeMarketplaceContentValidatorTest {

    private static final String VALID_DISPLAY_NAME = "Remove unnecessary parentheses";
    private static final String VALID_DESCRIPTION = "Removes unnecessary parentheses from code.";

    @ParameterizedTest
    @CsvSource({
      VALID_DISPLAY_NAME  + ',' + VALID_DESCRIPTION,
      VALID_DISPLAY_NAME  + ',' + "`Removes` unnecessary parentheses from code.",
      VALID_DISPLAY_NAME  + ',' + "[Removes](link) unnecessary parentheses from code.",
      VALID_DISPLAY_NAME  + ',' + " - Some list item." ,
      VALID_DISPLAY_NAME  + ',' + " - `other` list item." ,
      "`Remove` unnecessary parentheses"  + ',' + VALID_DESCRIPTION,
      "[Remove](link) unnecessary parentheses"  + ',' + VALID_DESCRIPTION,
    })
    void validMarketplace(String displayName, String description) {
         RecipeMarketplace marketplace = new RecipeMarketplaceReader().fromCsv("""
          name,displayName,description,category,ecosystem,packageName
          org.openrewrite.java.cleanup.UnnecessaryParentheses,%s,%s,Java,maven,org.openrewrite:rewrite-java
          """.formatted(displayName, description));
        RecipeMarketplaceContentValidator validator = new RecipeMarketplaceContentValidator();
        Validated<RecipeMarketplace> validation = validator.validate(marketplace);

        assertThat(validation.isValid()).isTrue();
    }

    @Test
    void invalidDisplayName() {
        RecipeMarketplace marketplace = new RecipeMarketplaceReader().fromCsv("""
          name,displayName,description,category,ecosystem,packageName
          org.openrewrite.java.cleanup.UnnecessaryParentheses,remove unnecessary parentheses,Removes unnecessary parentheses from code.,Java,maven,org.openrewrite:rewrite-java
          """);

        RecipeMarketplaceContentValidator validator = new RecipeMarketplaceContentValidator();
        Validated<RecipeMarketplace> validation = validator.validate(marketplace);

        assertThat(validation.isInvalid()).isTrue();
        assertThat(validation.failures()).hasSize(1);
        assertThat(validation.failures().getFirst().getMessage()).contains("sentence cased");
    }

    @Test
    void invalidDescription() {
        RecipeMarketplace marketplace = new RecipeMarketplaceReader().fromCsv("""
          name,displayName,description,category,ecosystem,packageName
          org.openrewrite.java.cleanup.UnnecessaryParentheses,Remove unnecessary parentheses,removes unnecessary parentheses from code,Java,maven,org.openrewrite:rewrite-java
          """);

        RecipeMarketplaceContentValidator validator = new RecipeMarketplaceContentValidator();
        Validated<RecipeMarketplace> validation = validator.validate(marketplace);

        assertThat(validation.isInvalid()).isTrue();
        assertThat(validation.failures()).hasSize(2);
    }

    @Test
    void displayNameEndsWithPeriod() {
        RecipeMarketplace marketplace = new RecipeMarketplaceReader().fromCsv("""
          name,displayName,description,category,ecosystem,packageName
          org.openrewrite.java.cleanup.UnnecessaryParentheses,Remove unnecessary parentheses.,Removes unnecessary parentheses from code.,Java,maven,org.openrewrite:rewrite-java
          """);

        RecipeMarketplaceContentValidator validator = new RecipeMarketplaceContentValidator();
        Validated<RecipeMarketplace> validation = validator.validate(marketplace);

        assertThat(validation.isInvalid()).isTrue();
        assertThat(validation.failures()).hasSize(1);
        assertThat(validation.failures().getFirst().getMessage()).contains("must not end with a period");
    }
}
