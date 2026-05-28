/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.golang.marketplace;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.openrewrite.golang.rpc.GoRewriteRpc;
import org.openrewrite.golang.rpc.InstallRecipesResponse;
import org.openrewrite.marketplace.RecipeBundle;
import org.openrewrite.marketplace.RecipeBundleReader;
import org.openrewrite.marketplace.RecipeBundleResolver;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RequiredArgsConstructor
public class GolangRecipeBundleResolver implements RecipeBundleResolver {
    private final GoRewriteRpc rpc;

    /**
     * The {@link InstallRecipesResponse} returned by the most recent
     * {@link #resolve(RecipeBundle)} call, or {@code null} if {@code resolve}
     * has not been called yet. Callers (e.g. Moderne CLI) read
     * {@link InstallRecipesResponse#getActivatePkg()} from here after each
     * resolve to learn the just-installed module's activate package.
     */
    @Getter
    private @Nullable InstallRecipesResponse lastResponse;

    @Override
    public String getEcosystem() {
        return "go";
    }

    @Override
    public RecipeBundleReader resolve(RecipeBundle bundle) {
        Path pkgPath = Paths.get(bundle.getPackageName());
        InstallRecipesResponse response = Files.exists(pkgPath)
                ? rpc.installRecipes(pkgPath.toFile())
                : rpc.installRecipes(bundle.getPackageName(), bundle.getVersion());
        this.lastResponse = response;
        if (response.getVersion() != null) {
            bundle.setVersion(response.getVersion());
        }
        return new GolangRecipeBundleReader(bundle, rpc);
    }
}
