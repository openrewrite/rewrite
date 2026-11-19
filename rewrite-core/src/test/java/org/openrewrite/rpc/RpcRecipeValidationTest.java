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

import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import io.moderne.jsonrpc.JsonRpc;
import io.moderne.jsonrpc.formatter.JsonMessageFormatter;
import io.moderne.jsonrpc.handler.HeaderDelimitedMessageHandler;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.Validated;
import org.openrewrite.ValidationException;
import org.openrewrite.config.Environment;
import org.openrewrite.config.OptionDescriptor;
import org.openrewrite.config.RecipeDescriptor;
import org.openrewrite.marketplace.RecipeMarketplace;
import org.openrewrite.rpc.request.PrepareRecipeResponse;
import org.openrewrite.rpc.request.PrepareRecipeResponse.OptionValidation;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.openrewrite.marketplace.RecipeBundle.runtimeClasspath;

class RpcRecipeValidationTest {

    RewriteRpc client;
    RewriteRpc server;

    @BeforeEach
    void before() throws IOException {
        PipedOutputStream serverOut = new PipedOutputStream();
        PipedOutputStream clientOut = new PipedOutputStream();
        PipedInputStream serverIn = new PipedInputStream(clientOut);
        PipedInputStream clientIn = new PipedInputStream(serverOut);

        RecipeMarketplace marketplace = Environment.builder()
          .scanRuntimeClasspath("org.openrewrite.text").build().toMarketplace(runtimeClasspath());

        client = new RewriteRpc(new JsonRpc(new HeaderDelimitedMessageHandler(
          new JsonMessageFormatter(new ParameterNamesModule()), clientIn, clientOut),
          new JsonMessageFormatter()), marketplace).batchSize(1);
        server = new RewriteRpc(new JsonRpc(new HeaderDelimitedMessageHandler(
          new JsonMessageFormatter(new ParameterNamesModule()), serverIn, serverOut),
          new JsonMessageFormatter()), marketplace).batchSize(1);
    }

    @AfterEach
    void after() {
        client.shutdown();
        server.shutdown();
    }

    @Test
    void prepareThrowsValidationExceptionForRemoteValidations() {
        assertThatThrownBy(() -> client.prepareRecipe(RequiresText.class.getName(), Map.of()))
          .isInstanceOf(ValidationException.class)
          .hasMessageContaining(".text was 'null' but it is required");
    }

    @Test
    void deferringEntryPointReturnsRecipeCarryingFailures() {
        Recipe recipe = client.prepareRecipeDeferringValidation(RequiresText.class.getName(), Map.of());

        List<Validated<Object>> validations = new ArrayList<>();
        recipe.validateAll(new InMemoryExecutionContext(), validations);

        assertThat(validations.stream().map(Validated::failures).flatMap(List::stream))
          .singleElement()
          .satisfies(f -> {
              assertThat(f.getProperty()).endsWith("RequiresText.text");
              assertThat(f.getMessage()).isEqualTo("is required");
          });
    }

    @Test
    void suppliedOptionsPrepareWithoutThrowing() {
        // prepareRecipe refuses anything the remote reported, so preparing at all is the assertion.
        assertThatNoException().isThrownBy(() ->
          client.prepareRecipe(RequiresText.class.getName(), Map.of("text", "hi")));
    }

    @Test
    void childValidationNamesTheChildRecipe() {
        Validated<Object> v = RewriteRpc.remoteValidations(response(null,
          response(List.of(new OptionValidation("toText", "is required")))));

        assertThat(v.failures()).singleElement()
          .extracting(Validated.Invalid::getProperty)
          .isEqualTo("org.example.Demo.toText");
    }

    @Test
    void delegatesToChildDoesNotContributeValidations() {
        PrepareRecipeResponse delegating = new PrepareRecipeResponse("child", descriptor(), "edit:child",
          emptyList(), null, emptyList(),
          new PrepareRecipeResponse.DelegatesTo("org.openrewrite.text.ChangeText", Map.of()),
          emptyList(), null, null);

        assertThat(RewriteRpc.remoteValidations(response(null, delegating)).isValid()).isTrue();
    }

    @Test
    void absentValidationsDoNotThrow() {
        assertThat(RewriteRpc.remoteValidations(response(null)).isValid()).isTrue();
    }


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

    private static PrepareRecipeResponse response(@Nullable List<OptionValidation> validations,
                                                  PrepareRecipeResponse... children) {
        return new PrepareRecipeResponse("id", descriptor(option("toText", true, null)), "edit:id",
          emptyList(), null, emptyList(), null, Arrays.asList(children), null, validations);
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

    /**
     * Resolved by class name rather than from the marketplace, so the pair needs no bundle resolver.
     */
    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class RequiresText extends Recipe {
        @Option(displayName = "Text", description = "The text this recipe needs.")
        String text;

        @Override
        public String getDisplayName() {
            return "Requires text";
        }

        @Override
        public String getDescription() {
            return "A recipe with one required option.";
        }
    }
}
