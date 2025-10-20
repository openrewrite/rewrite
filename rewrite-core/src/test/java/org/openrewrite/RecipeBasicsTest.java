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
import org.junit.jupiter.api.Test;
import org.openrewrite.config.RecipeDescriptor;
import org.openrewrite.text.ChangeText;

import java.util.HashMap;

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


    static class RecipeBase extends Recipe {
        String option;
        public RecipeBase(String option) {
            this.option = option;
        }

        @Override
        public String getDisplayName() {
            return "Recipe base";
        }

        @Override
        public String getDescription() {
            return "Recipe base.";
        }
    }

    static class ExtendingRecipe extends RecipeBase {
        @SuppressWarnings("unused")
        @Option(displayName = "Option", description = "Option description", example = "Example")
        String getOption() {
            return option;
        }

        public ExtendingRecipe(String option) {
            super(option);
        }
    }

    @Test
    void subclassExposesOptionsViaMethod() {
        RecipeDescriptor d = new ExtendingRecipe("option").createRecipeDescriptor();
        assertThat(d.getOptions().getFirst().getDisplayName()).isEqualTo("Option");
    }

    static class RecipeWithConstructorParameter extends Recipe {
        String option;

        public RecipeWithConstructorParameter(@Option(displayName = "Option", description = "Option description", example = "Example") String option) {
            this.option = option;
        }

        @Override
        public String getDisplayName() {
            return "";
        }

        @Override
        public String getDescription() {
            return "";
        }
    }

    @Test
    void optionExposedViaConstructorParameter() {
        RecipeDescriptor d = new RecipeWithConstructorParameter("option").createRecipeDescriptor();
        assertThat(d.getOptions().getFirst().getDisplayName()).isEqualTo("Option");
    }
}
