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
package org.openrewrite;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.openrewrite.config.OptionDescriptor;
import org.openrewrite.config.RecipeDescriptor;
import org.openrewrite.text.ChangeText;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RecipeBasicsTest {

    @Test
    void cloneRecipe() throws Exception {
        ChangeText ct = new ChangeText("hi");
        ChangeText ct2 = (ChangeText) ct.clone();
        ObjectMapper mapper = new ObjectMapper();
        mapper.updateValue(ct2, new HashMap<String, String>() {{
            put("toText", "hello");
        }});

        assertThat(ct2).isNotSameAs(ct);
        assertThat(ct.getToText()).isEqualTo("hi");
        assertThat(ct2.getToText()).isEqualTo("hello");
    }

    @Test
    void instanceName() {
        ChangeText ct = new ChangeText("hi");
        assertThat(ct.getInstanceName()).isEqualTo("Change text to `hi`");
    }

    static class RecipeWithListOption extends Recipe {
        @Option(displayName = "Field names", description = "Field names.", example = "firstName,lastName")
        List<String> fieldNames;

        public RecipeWithListOption(List<String> fieldNames) {
            this.fieldNames = fieldNames;
        }

        @Override
        public String getDisplayName() {
            return "Recipe with list option";
        }

        @Override
        public String getDescription() {
            return "Recipe with list option.";
        }
    }

    enum Color { RED, GREEN, BLUE }

    @Getter
    static class RecipeWithEnumField extends Recipe {
        private final String displayName = "Enum field recipe";
        private final String description = "Enum field recipe.";

        @Option(displayName = "Color", description = "Pick a color.")
        final Color color;

        public RecipeWithEnumField(Color color) {
            this.color = color;
        }
    }

    @Test
    void enumFieldOptionDeducesValidValues() {
        RecipeDescriptor d = new RecipeWithEnumField(Color.RED).createRecipeDescriptor();
        OptionDescriptor opt = d.getOptions().getFirst();
        assertThat(opt.getValid()).containsExactly("RED", "GREEN", "BLUE");
    }

    @Getter
    static class RecipeWithExplicitValid extends Recipe {
        private final String displayName = "Explicit valid recipe";
        private final String description = "Explicit valid recipe.";

        @Option(displayName = "Color", description = "Pick a color.", valid = {"RED", "GREEN"})
        final Color color;

        public RecipeWithExplicitValid(Color color) {
            this.color = color;
        }
    }

    @Test
    void explicitValidTakesPrecedenceOverEnumConstants() {
        RecipeDescriptor d = new RecipeWithExplicitValid(Color.RED).createRecipeDescriptor();
        OptionDescriptor opt = d.getOptions().getFirst();
        assertThat(opt.getValid()).containsExactly("RED", "GREEN");
    }

    @Getter
    static class RecipeWithSecretOption extends Recipe {
        private final String displayName = "Secret option recipe";
        private final String description = "Secret option recipe.";

        @Option(displayName = "Password", description = "The password.", secret = true)
        final String password;

        public RecipeWithSecretOption(String password) {
            this.password = password;
        }
    }

    @Test
    void secretOptionIsMarkedAndKeepsItsValueForTheRecipe() {
        OptionDescriptor opt = new RecipeWithSecretOption("hunter2").getDescriptor().getOptions().getFirst();
        assertThat(opt.isSecret()).isTrue();
        assertThat(opt.getValue()).isEqualTo("hunter2");
        assertThat(opt.getDisplayValue()).isEqualTo(OptionDescriptor.REDACTED);
    }

    @Test
    void secretOptionIsNotShownInInstanceName() {
        assertThat(new RecipeWithSecretOption("hunter2").getInstanceName()).isEqualTo("Secret option recipe");
    }

    @Test
    void redactedDescriptorMasksSecretsOnly() {
        RecipeDescriptor redacted = new RecipeWithSecretOption("hunter2").getDescriptor().redacted();
        assertThat(redacted.getOptions().getFirst().getValue()).isEqualTo(OptionDescriptor.REDACTED);
        assertThat(new ChangeText("hi").getDescriptor().redacted().getOptions().getFirst().getValue()).isEqualTo("hi");
    }

    @Test
    void unsetSecretIsNotMasked() {
        OptionDescriptor opt = new RecipeWithSecretOption(null).getDescriptor().getOptions().getFirst();
        assertThat(opt.redacted().getValue()).isNull();
    }

    @Test
    void withOptionsDeserializesCommaSeparatedStringToList() {
        // given
        RecipeWithListOption recipe = new RecipeWithListOption(List.of());
        Map<String, Object> options = new HashMap<>();
        options.put("fieldNames", "firstName,lastName,email,emailAddress");

        // when
        Recipe result = recipe.withOptions(options);

        // then
        assertThat(result).isInstanceOf(RecipeWithListOption.class);
        assertThat(((RecipeWithListOption) result).fieldNames)
                .containsExactly("firstName", "lastName", "email", "emailAddress");
    }

    @Test
    void withOptionsPreservesNonStringOptionValuesInDescriptor() {
        // given
        RecipeWithMixedOptionTypes recipe = new RecipeWithMixedOptionTypes(null, null, null);
        Map<String, Object> options = new HashMap<>();
        options.put("text", "hello");
        options.put("flag", true);
        options.put("count", 5);

        // when
        Recipe result = recipe.withOptions(options);

        // then the descriptor must report every supplied value, not just the String one
        Map<String, Object> values = new HashMap<>();
        for (OptionDescriptor o : result.getDescriptor().getOptions()) {
            values.put(o.getName(), o.getValue());
        }
        assertThat(values.get("text")).isEqualTo("hello");
        assertThat(values.get("flag")).isEqualTo(true);
        assertThat(values.get("count")).isEqualTo(5);
    }

    @Getter
    static class RecipeWithMixedOptionTypes extends Recipe {
        private final String displayName = "Mixed option types recipe";
        private final String description = "Mixed option types recipe.";

        @Option(displayName = "Text", description = "A string option.", example = "hello")
        final @Nullable String text;

        @Option(displayName = "Flag", description = "A boolean option.", required = false)
        final @Nullable Boolean flag;

        @Option(displayName = "Count", description = "An integer option.", required = false)
        final @Nullable Integer count;

        public RecipeWithMixedOptionTypes(@Nullable String text, @Nullable Boolean flag, @Nullable Integer count) {
            this.text = text;
            this.flag = flag;
            this.count = count;
        }
    }
}
