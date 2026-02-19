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

import org.openrewrite.javascript.rpc.InstallRecipesResponse;
import org.openrewrite.javascript.rpc.JavaScriptRewriteRpc;
import org.openrewrite.marketplace.RecipeBundle;
import org.openrewrite.marketplace.RecipeBundleReader;
import org.openrewrite.marketplace.RecipeBundleResolver;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class NpmRecipeBundleResolver implements RecipeBundleResolver {
    private volatile JavaScriptRewriteRpc rpc;

    public NpmRecipeBundleResolver() {
    }

    public NpmRecipeBundleResolver(JavaScriptRewriteRpc rpc) {
        this.rpc = rpc;
    }

    @Override
    public String getEcosystem() {
        return "npm";
    }

    @Override
    public RecipeBundleReader resolve(RecipeBundle bundle) {
        JavaScriptRewriteRpc rpc = this.rpc;
        if (rpc == null) {
            rpc = JavaScriptRewriteRpc.getOrStart();
            this.rpc = rpc;
        }
        Path pkgPath = Paths.get(bundle.getPackageName());
        InstallRecipesResponse response;
        if (Files.exists(pkgPath)) {
            response = rpc.installRecipes(pkgPath.toFile());
        } else {
            response = rpc.installRecipes(bundle.getPackageName(), bundle.getVersion());
        }
        if (response.getVersion() != null) {
            bundle.setVersion(response.getVersion());
        }
        return new NpmRecipeBundleReader(bundle, rpc);
    }
}
