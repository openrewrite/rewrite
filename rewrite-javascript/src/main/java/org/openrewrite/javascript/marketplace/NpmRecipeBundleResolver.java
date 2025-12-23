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
package org.openrewrite.javascript.marketplace;

import lombok.RequiredArgsConstructor;
import org.openrewrite.javascript.rpc.JavaScriptRewriteRpc;
import org.openrewrite.marketplace.RecipeBundle;
import org.openrewrite.marketplace.RecipeBundleReader;
import org.openrewrite.marketplace.RecipeBundleResolver;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RequiredArgsConstructor
public class NpmRecipeBundleResolver implements RecipeBundleResolver {
    private final JavaScriptRewriteRpc rpc;

    @Override
    public String getEcosystem() {
        return "npm";
    }

    @Override
    public RecipeBundleReader resolve(RecipeBundle bundle) {
        Path pkgPath = Paths.get(bundle.getPackageName());
        if (Files.exists(pkgPath)) {
            rpc.installRecipes(pkgPath.toFile());
        } else {
            rpc.installRecipes(bundle.getPackageName(), bundle.getVersion());
        }
        return new NpmRecipeBundleReader(bundle, rpc);
    }
}
