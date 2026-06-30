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
package org.openrewrite;

import lombok.Getter;
import org.junit.jupiter.api.Test;
import org.openrewrite.config.OptionDescriptor;
import org.openrewrite.config.RecipeDescriptor;
import org.openrewrite.marketplace.RecipeMarketplace;
import org.openrewrite.marketplace.RecipeMarketplaceReader;
import org.openrewrite.marketplace.RecipeMarketplaceWriter;

import static org.assertj.core.api.Assertions.assertThat;

class SecretOptionTest {

    @Getter
    static class RecipeWithSecretField extends Recipe {
        private final String displayName = "Secret field recipe";
        private final String description = "Secret field recipe.";

        @Option(displayName = "API token", description = "API token.", secret = true)
        final String apiToken;

        public RecipeWithSecretField(String apiToken) {
            this.apiToken = apiToken;
        }
    }

    @Test
    void fieldOptionPropagatesSecret() {
        RecipeDescriptor d = new RecipeWithSecretField("hunter2").createRecipeDescriptor();
        OptionDescriptor opt = d.getOptions().getFirst();
        assertThat(opt.isSecret()).isTrue();
    }

    @Test
    void fieldOptionRawValuePreservedOnDescriptor() {
        // Critical invariant: OptionDescriptor.value is NOT redacted at the source. RPC peers
        // and the recipe clone/withOptions path rely on the raw value being available via
        // OptionDescriptor#getValue(). Redaction happens only at persistence boundaries
        // (e.g. RecipeMarketplaceWriter.optionsToJson) via withRedactedSecretValue().
        RecipeDescriptor d = new RecipeWithSecretField("hunter2").createRecipeDescriptor();
        OptionDescriptor opt = d.getOptions().getFirst();
        assertThat(opt.getValue()).isEqualTo("hunter2");
    }

    static class RecipeBase extends Recipe {
        String apiToken;

        public RecipeBase(String apiToken) {
            this.apiToken = apiToken;
        }

        @Override
        public String getDisplayName() {
            return "Method secret recipe";
        }

        @Override
        public String getDescription() {
            return "Method secret recipe.";
        }
    }

    static class RecipeWithSecretMethod extends RecipeBase {
        public RecipeWithSecretMethod(String apiToken) {
            super(apiToken);
        }

        @SuppressWarnings("unused")
        @Option(displayName = "API token", description = "API token.", secret = true)
        String getApiToken() {
            return apiToken;
        }
    }

    @Test
    void methodOptionPropagatesSecret() {
        RecipeDescriptor d = new RecipeWithSecretMethod("hunter2").createRecipeDescriptor();
        OptionDescriptor opt = d.getOptions().stream()
                .filter(o -> "apiToken".equals(o.getName()))
                .findFirst().orElseThrow();
        assertThat(opt.isSecret()).isTrue();
    }

    @Getter
    static class RecipeWithSecretConstructorParam extends Recipe {
        private final String displayName = "Constructor secret recipe";
        private final String description = "Constructor secret recipe.";
        final String apiToken;

        public RecipeWithSecretConstructorParam(
                @Option(displayName = "API token", description = "API token.", secret = true) String apiToken) {
            this.apiToken = apiToken;
        }
    }

    @Test
    void constructorParamOptionPropagatesSecret() {
        RecipeDescriptor d = new RecipeWithSecretConstructorParam("hunter2").createRecipeDescriptor();
        OptionDescriptor opt = d.getOptions().getFirst();
        assertThat(opt.isSecret()).isTrue();
    }

    @Test
    void withRedactedSecretValueNullsValueButPreservesOtherFields() {
        OptionDescriptor opt = new OptionDescriptor("apiToken", "String",
                "API token", "API token.", null, null, true, true, "hunter2");

        OptionDescriptor redacted = opt.withRedactedSecretValue();

        assertThat(redacted.getName()).isEqualTo("apiToken");
        assertThat(redacted.getType()).isEqualTo("String");
        assertThat(redacted.getDisplayName()).isEqualTo("API token");
        assertThat(redacted.getDescription()).isEqualTo("API token.");
        assertThat(redacted.isRequired()).isTrue();
        assertThat(redacted.isSecret()).isTrue();
        assertThat(redacted.getValue()).isNull();
    }

    @Test
    void withRedactedSecretValueReturnsSameInstanceWhenNotSecret() {
        OptionDescriptor opt = new OptionDescriptor("methodPattern", "String",
                "Method pattern", "Method pattern.", null, null, true, false, "java.util.List add(..)");

        OptionDescriptor result = opt.withRedactedSecretValue();

        assertThat(result).isSameAs(opt);
        assertThat(result.getValue()).isEqualTo("java.util.List add(..)");
    }

    /**
     * Regression test: a recipe whose only required option is marked secret must NOT
     * include the value of that option in {@link Recipe#getInstanceName()}. Without
     * the filter in {@code Recipe#getInstanceName()}, the credential would be composed
     * into a user-visible string that appears in dashboards, logs, and audit views.
     */
    @Test
    void instanceNameDoesNotLeakSecretOptionValue() {
        RecipeWithSecretField recipe = new RecipeWithSecretField("hunter2");
        assertThat(recipe.getInstanceName()).isEqualTo(recipe.getDisplayName());
        assertThat(recipe.getInstanceName()).doesNotContain("hunter2");
    }

    @Getter
    static class RecipeWithNonSecretRequiredOption extends Recipe {
        private final String displayName = "Non-secret recipe";
        private final String description = "Non-secret recipe.";

        @Option(displayName = "Method pattern", description = "Method pattern.", example = "java.util.List add(..)")
        final String methodPattern;

        public RecipeWithNonSecretRequiredOption(String methodPattern) {
            this.methodPattern = methodPattern;
        }
    }

    @Test
    void instanceNameStillComposesNonSecretRequiredOptionValue() {
        // Sanity: instance-name composition still works for non-secret required options.
        RecipeWithNonSecretRequiredOption recipe = new RecipeWithNonSecretRequiredOption("java.util.List add(..)");
        assertThat(recipe.getInstanceName()).isEqualTo("Non-secret recipe `java.util.List add(..)`");
    }

    @Test
    void marketplaceWriterRedactsSecretOptionValueInJson() {
        // Marketplace storage is durable and externally visible. Even when an option
        // descriptor carries a runtime value (e.g. because the listing was loaded from
        // a recipe instance), the secret value must not be serialized to CSV/JSON.
        RecipeMarketplace marketplace = new RecipeMarketplaceReader().fromCsv("""
          name,displayName,options,category,ecosystem,packageName
          org.example.SecretRecipe,Secret Recipe,"[{""name"":""apiToken"",""type"":""String"",""displayName"":""API token"",""description"":""API token."",""required"":true,""secret"":true,""value"":""hunter2""}]",Example,maven,org.example:example
          """);

        String writtenCsv = new RecipeMarketplaceWriter().toCsv(marketplace);

        assertThat(writtenCsv).doesNotContain("hunter2");
        // The secret flag itself is preserved so downstream consumers know to treat the option as secret.
        assertThat(writtenCsv).contains("\"\"secret\"\":true");
    }
}
