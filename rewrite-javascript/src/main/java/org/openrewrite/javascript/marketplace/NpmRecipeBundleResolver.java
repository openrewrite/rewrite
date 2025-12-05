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
        String pkg = bundle.getPackageName();
        Path pkgPath = Paths.get(pkg);
        if (Files.exists(pkgPath)) {
            rpc.installRecipes(pkgPath.toFile());
        } else {
            String scopeName = "";
            String[] pkgParts;

            // check if package is scoped
            if (pkg.startsWith("@")) {
                scopeName = pkg.substring(0, pkg.indexOf("/")) + "/";
                String scopelessPart = pkg.substring(pkg.indexOf("/") + 1);
                pkgParts = scopelessPart.split("@", 2);
            } else {
                pkgParts = pkg.split("@", 2);
            }

            String packageName = scopeName + pkgParts[0];
            String version = pkgParts.length > 1 ? pkgParts[1] : null;
            rpc.installRecipes(packageName, version);
        }

        return new NpmRecipeBundleReader(bundle, rpc);
    }
}
