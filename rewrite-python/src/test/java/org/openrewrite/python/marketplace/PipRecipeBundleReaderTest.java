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
package org.openrewrite.python.marketplace;

import org.junit.jupiter.api.Test;
import org.openrewrite.marketplace.RecipeBundle;
import org.openrewrite.python.rpc.InstallRecipesResponse;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PipRecipeBundleReaderTest {

    private static final RecipeBundle BUNDLE =
            new RecipeBundle("pip", "openrewrite-static-analysis", "0.2.0", "0.2.0", null);

    @Test
    void returnsIndexesFromInstallResponse() {
        Set<String> repos = new LinkedHashSet<>();
        repos.add("https://files.pythonhosted.org");
        repos.add("https://internal.example.com");

        PipRecipeBundleReader reader = new PipRecipeBundleReader(
                BUNDLE, null, new InstallRecipesResponse(1, "0.2.0", null, repos));

        assertThat(reader.getResolvedFromRepositories()).containsExactlyInAnyOrderElementsOf(repos);
    }

    @Test
    void returnsEmptySetWhenResponseHasNullRepositories() {
        PipRecipeBundleReader reader = new PipRecipeBundleReader(
                BUNDLE, null, new InstallRecipesResponse(1, "0.2.0", null, null));

        assertThat(reader.getResolvedFromRepositories()).isEmpty();
    }
}
