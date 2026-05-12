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
package org.openrewrite.csharp.marketplace;

import org.junit.jupiter.api.Test;
import org.openrewrite.csharp.rpc.InstallRecipesResponse;
import org.openrewrite.marketplace.RecipeBundle;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class NuGetRecipeBundleReaderTest {

    private static final RecipeBundle BUNDLE =
            new RecipeBundle("nuget", "OpenRewrite.CodeQuality", "0.1.0", "0.1.0", null);

    @Test
    void returnsFeedsFromInstallResponse() {
        Set<String> repos = new LinkedHashSet<>();
        repos.add("https://api.nuget.org/v3/index.json");
        repos.add("https://internal.example.com/artifactory/api/nuget/v3/nuget-virtual/index.json");

        NuGetRecipeBundleReader reader = new NuGetRecipeBundleReader(
                BUNDLE, null, new InstallRecipesResponse(1, "0.1.0", repos));

        assertThat(reader.getResolvedFromRepositories()).containsExactlyInAnyOrderElementsOf(repos);
    }

    @Test
    void returnsEmptySetWhenResponseHasNullRepositories() {
        NuGetRecipeBundleReader reader = new NuGetRecipeBundleReader(
                BUNDLE, null, new InstallRecipesResponse(1, "0.1.0", null));

        assertThat(reader.getResolvedFromRepositories()).isEmpty();
    }

    @Test
    void returnsEmptySetForTwoArgConstructor() {
        // The 2-arg convenience constructor is used by call sites that don't have an
        // install response handy. It must return an empty set rather than throwing.
        NuGetRecipeBundleReader reader = new NuGetRecipeBundleReader(BUNDLE, null);

        assertThat(reader.getResolvedFromRepositories()).isEmpty();
    }
}
