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

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Recipe;
import org.openrewrite.config.RecipeDescriptor;
import org.openrewrite.javascript.rpc.JavaScriptRewriteRpc;
import org.openrewrite.marketplace.*;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class NpmRecipeBundleReader implements RecipeBundleReader {
    private final @Getter RecipeBundle bundle;
    private final JavaScriptRewriteRpc rpc;

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
