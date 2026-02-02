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

import lombok.RequiredArgsConstructor;
import org.openrewrite.python.rpc.InstallRecipesResponse;
import org.openrewrite.python.rpc.PythonRewriteRpc;
import org.openrewrite.marketplace.RecipeBundle;
import org.openrewrite.marketplace.RecipeBundleReader;
import org.openrewrite.marketplace.RecipeBundleResolver;

@RequiredArgsConstructor
public class PythonRecipeBundleResolver implements RecipeBundleResolver {
    private final PythonRewriteRpc rpc;

    @Override
    public String getEcosystem() {
        return "python";
    }

    @Override
    public RecipeBundleReader resolve(RecipeBundle bundle) {
        InstallRecipesResponse response = rpc.installRecipes(bundle.getPackageName(), bundle.getVersion());
        if (response.getVersion() != null) {
            bundle.setVersion(response.getVersion());
        }
        return new PythonRecipeBundleReader(bundle, rpc);
    }
}
