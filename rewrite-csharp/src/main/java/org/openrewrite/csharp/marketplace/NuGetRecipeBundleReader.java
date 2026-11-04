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
package org.openrewrite.csharp.marketplace;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.openrewrite.Recipe;
import org.openrewrite.config.RecipeDescriptor;
import org.openrewrite.csharp.rpc.CSharpRewriteRpc;
import org.openrewrite.marketplace.RecipeBundle;
import org.openrewrite.marketplace.RecipeBundleReader;
import org.openrewrite.marketplace.RecipeListing;
import org.openrewrite.marketplace.RecipeMarketplace;

import java.util.Map;

@RequiredArgsConstructor
public class NuGetRecipeBundleReader implements RecipeBundleReader {
    private final @Getter RecipeBundle bundle;
    private final CSharpRewriteRpc rpc;

    @Override
    public RecipeMarketplace read() {
        return rpc.getMarketplace(bundle);
    }

    @Override
    public RecipeDescriptor describe(RecipeListing listing) {
        return rpc.prepareRecipe(listing.getName()).getDescriptor();
    }

    @Override
    public Recipe prepare(RecipeListing listing, Map<String, Object> options) {
        return rpc.prepareRecipe(listing.getName(), options);
    }
}
