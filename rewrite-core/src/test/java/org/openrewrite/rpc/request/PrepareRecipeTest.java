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
package org.openrewrite.rpc.request;

import com.fasterxml.jackson.databind.module.SimpleModule;
import lombok.EqualsAndHashCode;
import lombok.Value;
import io.moderne.jsonrpc.JsonRpcRequest;
import io.moderne.jsonrpc.formatter.JsonMessageFormatter;
import org.junit.jupiter.api.Test;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.rpc.internal.PreparedRecipeCache;
import org.openrewrite.text.ChangeText;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PrepareRecipeTest {

    @Test
    void causesAnotherCycleOmittedForHostsThatDidNotAskForIt() throws IOException {
        assertThat(wire(PrepareRecipe.Handler.prepareTree(new CausesAnotherCycle(), new PreparedRecipeCache(), false)))
          .doesNotContain("causesAnotherCycle");
    }

    @Test
    void causesAnotherCycleOmittedWhenFalse() throws IOException {
        assertThat(wire(PrepareRecipe.Handler.prepareTree(new org.openrewrite.text.ChangeText("hi"), new PreparedRecipeCache(), true)))
          .doesNotContain("causesAnotherCycle");
    }

    @Test
    void causesAnotherCycleSentWhenRequested() throws IOException {
        assertThat(wire(PrepareRecipe.Handler.prepareTree(new CausesAnotherCycle(), new PreparedRecipeCache(), true)))
          .contains("\"causesAnotherCycle\":true");
    }

    @Test
    void requiredOptionMissingProducesValidation() throws IOException {
        assertThat(wire(PrepareRecipe.Handler.prepareTree(new ChangeText(null), new PreparedRecipeCache(), false)))
          .contains("\"validations\":[{\"option\":\"toText\",\"message\":\"is required\"}]");
    }

    @Test
    void requiredOptionSuppliedProducesNoValidationsKey() throws IOException {
        assertThat(wire(PrepareRecipe.Handler.prepareTree(new ChangeText("hi"), new PreparedRecipeCache(), false)))
          .doesNotContain("validations");
    }

    @Test
    void nullableOptionIsNotRequiredEvenWhenTheAnnotationSaysSo() throws IOException {
        // `@Option.required()` defaults to true and the descriptor copies it verbatim, but
        // Recipe#validate() asks NullUtils, which skips a @Nullable field before it looks at
        // @Option at all. This server must agree with the recipe, not be stricter than it.
        assertThat(wire(PrepareRecipe.Handler.prepareTree(new NullableOption(null), new PreparedRecipeCache(), false)))
          .doesNotContain("validations");
    }

    @Test
    void optionExposedThroughAGetterIsNotReportedAsMissing() throws IOException {
        // The descriptor hardcodes a null value for an option declared on a getter, so a
        // required-ness check that trusted it would reject the recipe whatever the caller supplied.
        assertThat(wire(PrepareRecipe.Handler.prepareTree(new OptionViaGetter(), new PreparedRecipeCache(), false)))
          .doesNotContain("validations");
    }

    @Test
    void childRequiredOptionProducesValidationOnTheChildNode() {
        PrepareRecipeResponse response = PrepareRecipe.Handler.prepareTree(
          new CompositeWithInvalidChild(), new PreparedRecipeCache(), false);

        assertThat(response.getValidations()).isNull();
        assertThat(response.getRecipeList()).singleElement().satisfies(child ->
          assertThat(child.getValidations()).singleElement().satisfies(validation -> {
              assertThat(validation.getOption()).isEqualTo("toText");
              assertThat(validation.getMessage()).isEqualTo("is required");
          }));
    }

    private static String wire(PrepareRecipeResponse response) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new JsonMessageFormatter(new SimpleModule()).serialize(JsonRpcRequest.newRequest("PrepareRecipe", response), out);
        return out.toString(StandardCharsets.UTF_8.name());
    }

    static class CausesAnotherCycle extends Recipe {
        @Override
        public String getDisplayName() {
            return "Causes another cycle";
        }

        @Override
        public String getDescription() {
            return "Causes another cycle.";
        }

        @Override
        public boolean causesAnotherCycle() {
            return true;
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class NullableOption extends Recipe {
        @Option(displayName = "Maybe", description = "A value this recipe can do without.")
        @Nullable
        String maybe;

        @Override
        public String getDisplayName() {
            return "Nullable option";
        }

        @Override
        public String getDescription() {
            return "A recipe whose only option is nullable.";
        }
    }

    static class OptionViaGetter extends Recipe {
        @Option(displayName = "Via getter", description = "Declared on a getter rather than a field.")
        public String getViaGetter() {
            return "set";
        }

        @Override
        public String getDisplayName() {
            return "Option via getter";
        }

        @Override
        public String getDescription() {
            return "A recipe declaring its option on a getter.";
        }
    }

    static class CompositeWithInvalidChild extends Recipe {
        @Override
        public String getDisplayName() {
            return "Composite with an invalid child";
        }

        @Override
        public String getDescription() {
            return "A composite whose child is missing a required option.";
        }

        @Override
        public List<Recipe> getRecipeList() {
            return List.of(new ChangeText(null));
        }
    }
}
