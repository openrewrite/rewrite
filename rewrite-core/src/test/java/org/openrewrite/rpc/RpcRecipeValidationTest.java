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
package org.openrewrite.rpc;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.openrewrite.Validated;
import org.openrewrite.ValidationException;
import org.openrewrite.config.OptionDescriptor;
import org.openrewrite.config.RecipeDescriptor;
import org.openrewrite.rpc.request.PrepareRecipeResponse.OptionValidation;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;

class RpcRecipeValidationTest {

    @Test
    void absentValidationsAreNone() {
        Validated<Object> v = RpcRecipe.toValidated(descriptor(option("text", true, null)), null);
        assertThat(v.isValid()).isTrue();
        assertThat(v.failures()).isEmpty();
    }

    @Test
    void emptyValidationsAreNone() {
        Validated<Object> v = RpcRecipe.toValidated(descriptor(option("text", true, null)), emptyList());
        assertThat(v.isValid()).isTrue();
        assertThat(v.failures()).isEmpty();
    }

    @Test
    void requiredOptionBecomesMissing() {
        Validated<Object> v = RpcRecipe.toValidated(descriptor(option("text", true, null)),
                List.of(new OptionValidation("text", "is required")));

        assertThat(v.isInvalid()).isTrue();
        assertThat(v.failures()).singleElement().satisfies(f -> {
            assertThat(f).isInstanceOf(Validated.Missing.class);
            assertThat(f.getProperty()).isEqualTo("org.example.Demo.text");
            assertThat(f.getInvalidValue()).isNull();
            assertThat(f.getMessage()).isEqualTo("is required");
        });
        assertThat(new ValidationException(v).getMessage())
                .isEqualTo("org.example.Demo.text was 'null' but it is required");
    }

    @Test
    void suppliedValueBecomesInvalidNotMissing() {
        Validated<Object> v = RpcRecipe.toValidated(descriptor(option("text", true, "x")),
                List.of(new OptionValidation("text", "should not be blank")));

        assertThat(v.failures()).singleElement().satisfies(f -> {
            assertThat(f).isNotInstanceOf(Validated.Missing.class);
            assertThat(f.getInvalidValue()).isEqualTo("x");
            assertThat(f.getMessage()).isEqualTo("should not be blank");
        });
    }

    @Test
    void unknownOptionNameYieldsNullValue() {
        Validated<Object> v = RpcRecipe.toValidated(descriptor(option("text", true, null)),
                List.of(new OptionValidation("nope", "is required")));

        assertThat(v.failures()).singleElement().satisfies(f -> {
            assertThat(f).isNotInstanceOf(Validated.Missing.class);
            assertThat(f.getProperty()).isEqualTo("org.example.Demo.nope");
            assertThat(f.getInvalidValue()).isNull();
        });
    }

    @Test
    void twoEntriesBothReportAsFailures() {
        Validated<Object> v = RpcRecipe.toValidated(
                descriptor(option("first", true, null), option("second", true, null)),
                List.of(new OptionValidation("first", "is required"),
                        new OptionValidation("second", "is required")));

        assertThat(v.failures())
                .extracting(Validated.Invalid::getProperty)
                .containsExactly("org.example.Demo.first", "org.example.Demo.second");
    }

    private static RecipeDescriptor descriptor(OptionDescriptor... options) {
        return new RecipeDescriptor("org.example.Demo", "Demo", "Demo", "A demo recipe.",
                emptySet(), null, Arrays.asList(options), emptyList(), emptyList(), emptyList(),
                emptyList(), emptyList(), URI.create("urn:test"));
    }

    private static OptionDescriptor option(String name, boolean required, @Nullable Object value) {
        return new OptionDescriptor(name, "String", name, name + " option",
                null, null, required, value);
    }
}
