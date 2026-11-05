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

import io.moderne.jsonrpc.JsonRpc;
import io.moderne.jsonrpc.JsonRpcError;
import io.moderne.jsonrpc.JsonRpcRequest;
import io.moderne.jsonrpc.formatter.JsonMessageFormatter;
import io.moderne.jsonrpc.handler.HeaderDelimitedMessageHandler;
import org.junit.jupiter.api.Test;
import org.openrewrite.marketplace.RecipeMarketplace;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.UncheckedIOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class IncompatibleRecipeBundleExceptionTest {

    // The shape install-recipes.ts sends, see its "refuses a released host" test
    private static final String REFUSAL_DATA =
      "{\"packageName\":\"recipes\",\"packageVersion\":\"1.0.0\",\"requiredRange\":\"^8.98.0\",\"hostVersion\":\"8.97.0\"}";

    @Test
    void readsTheServersRefusal() throws IOException {
        Throwable refusal = sendAnsweredWith(new JsonRpcError.Detail(1001,
          "recipes@1.0.0 requires @openrewrite/rewrite ^8.98.0, but this host runs 8.97.0", REFUSAL_DATA));

        assertThat(refusal).isInstanceOfSatisfying(IncompatibleRecipeBundleException.class, e -> {
            assertThat(e.getMessage()).isEqualTo("recipes@1.0.0 requires @openrewrite/rewrite ^8.98.0, but this host runs 8.97.0");
            assertThat(e.getPackageName()).isEqualTo("recipes");
            assertThat(e.getPackageVersion()).isEqualTo("1.0.0");
            assertThat(e.getRequiredRange()).isEqualTo("^8.98.0");
            assertThat(e.getHostVersion()).isEqualTo("8.97.0");
        });
    }

    @Test
    void leavesOtherErrorsAlone() throws IOException {
        Throwable failure = sendAnsweredWith(new JsonRpcError.Detail(-32603,
          "Request InstallRecipes failed with message: boom", REFUSAL_DATA));

        assertThat(failure).isNotInstanceOf(IncompatibleRecipeBundleException.class).hasMessageContaining("boom");
    }

    /**
     * What {@link RewriteRpc#send} throws when the server answers its request with this error.
     */
    private static Throwable sendAnsweredWith(JsonRpcError.Detail error) throws IOException {
        PipedOutputStream clientOut = new PipedOutputStream();
        PipedOutputStream serverOut = new PipedOutputStream();
        HeaderDelimitedMessageHandler server = new HeaderDelimitedMessageHandler(new PipedInputStream(clientOut), serverOut);
        RewriteRpc client = new RewriteRpc(new JsonRpc(new HeaderDelimitedMessageHandler(new PipedInputStream(serverOut), clientOut), new JsonMessageFormatter()),
          new RecipeMarketplace());
        JsonMessageFormatter formatter = new JsonMessageFormatter();
        Thread answer = new Thread(() -> {
            try {
                JsonRpcRequest request = (JsonRpcRequest) server.receive(formatter);
                server.send(new JsonRpcError(request.getId(), error), formatter);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        answer.start();
        try {
            return catchThrowable(() -> client.send("InstallRecipes", null, Object.class));
        } finally {
            client.shutdown();
        }
    }
}
