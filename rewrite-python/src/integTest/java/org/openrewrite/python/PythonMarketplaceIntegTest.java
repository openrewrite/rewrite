/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.python;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.marketplace.RecipeBundle;
import org.openrewrite.marketplace.RecipeListing;
import org.openrewrite.marketplace.RecipeMarketplace;
import org.openrewrite.python.rpc.PythonRewriteRpc;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end check that the Python RPC server emits listing-weight marketplace rows (no full
 * descriptor) and that the JVM host reconstructs them into {@link RecipeListing}s over RPC.
 */
@Timeout(value = 120, unit = TimeUnit.SECONDS)
class PythonMarketplaceIntegTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void before() {
        PythonRewriteRpc.setFactory(PythonRewriteRpc.builder()
                .log(tempDir.resolve("python-rpc.log")));
    }

    @AfterEach
    void after() {
        PythonRewriteRpc.shutdownCurrent();
        PythonRewriteRpc.setFactory(PythonRewriteRpc.builder());
    }

    @Test
    void getMarketplaceReturnsLightweightListings() {
        // Built-in Python recipes are attributed to the "openrewrite" package by the server.
        RecipeBundle bundle = new RecipeBundle("pip", "openrewrite", null, null, null);
        RecipeMarketplace marketplace = PythonRewriteRpc.getOrStart().getMarketplace(bundle);

        assertThat(marketplace).isNotNull();
        assertThat(marketplace.getAllRecipes()).isNotEmpty();
        // Every listing was built from a lightweight row: name and the transitive recipe count
        // survive the round-trip, proving the host didn't need the full descriptor.
        for (RecipeListing listing : marketplace.getAllRecipes()) {
            assertThat(listing.getName()).isNotBlank();
            assertThat(listing.getRecipeCount()).isGreaterThanOrEqualTo(1);
        }
    }
}
