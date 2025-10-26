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
import org.openrewrite.javascript.PackageVersion;
import org.openrewrite.marketplace.RecipeBundle;
import org.openrewrite.marketplace.RecipeListing;

import java.util.Map;

@RequiredArgsConstructor
public class NpmRecipeBundle implements RecipeBundle {
    private final PackageVersion packageVersion;

    @Getter
    private final @Nullable String team;

    @Override
    public String getPackageEcosystem() {
        return "npm";
    }

    @Override
    public String getPackageName() {
        return packageVersion.getPackageName();
    }

    @Override
    public String getVersion() {
        return packageVersion.getVersion() == null ? "" : packageVersion.getVersion();
    }

    @Override
    public RecipeDescriptor describe(RecipeListing listing) {
        throw new UnsupportedOperationException("Implement me!");
    }

    @Override
    public Recipe prepare(RecipeListing listing, Map<String, Object> options) {
        throw new UnsupportedOperationException("Implement me!");
    }
}
