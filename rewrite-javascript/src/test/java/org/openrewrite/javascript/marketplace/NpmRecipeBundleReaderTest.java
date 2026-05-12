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
package org.openrewrite.javascript.marketplace;

import org.junit.jupiter.api.Test;
import org.openrewrite.javascript.rpc.InstallRecipesResponse;
import org.openrewrite.marketplace.RecipeBundle;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class NpmRecipeBundleReaderTest {

    private static final RecipeBundle BUNDLE =
            new RecipeBundle("npm", "@openrewrite/recipes-nodejs", "0.44.1", "0.44.1", null);

    @Test
    void returnsRegistriesFromInstallResponse() {
        Set<String> repos = new LinkedHashSet<>();
        repos.add("https://registry.npmjs.org");
        repos.add("https://internal.example.com/artifactory/api/npm/npm-virtual");

        NpmRecipeBundleReader reader = new NpmRecipeBundleReader(
                BUNDLE, null, new InstallRecipesResponse(1, "0.44.1", repos));

        assertThat(reader.getResolvedFromRepositories()).containsExactlyInAnyOrderElementsOf(repos);
    }

    @Test
    void returnsEmptySetWhenResponseHasNullRepositories() {
        NpmRecipeBundleReader reader = new NpmRecipeBundleReader(
                BUNDLE, null, new InstallRecipesResponse(1, "0.44.1", null));

        assertThat(reader.getResolvedFromRepositories()).isEmpty();
    }

    @Test
    void returnsEmptySetForTwoArgConstructor() {
        // The 2-arg convenience constructor is used by call sites that don't have an
        // install response handy (e.g. tests, legacy code paths). It must return an
        // empty set rather than throwing.
        NpmRecipeBundleReader reader = new NpmRecipeBundleReader(BUNDLE, null);

        assertThat(reader.getResolvedFromRepositories()).isEmpty();
    }
}
