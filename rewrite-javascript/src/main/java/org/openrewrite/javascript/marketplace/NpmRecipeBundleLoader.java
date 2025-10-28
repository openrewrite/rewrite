/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.javascript.marketplace;

import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.PackageVersion;
import org.openrewrite.marketplace.RecipeBundle;
import org.openrewrite.marketplace.RecipeBundleLoader;

public class NpmRecipeBundleLoader implements RecipeBundleLoader {
    @Override
    public String getEcosystem() {
        return "npm";
    }

    @Override
    public RecipeBundle createBundle(String packageName, String version, @Nullable String team) {
        PackageVersion packageVersion = new PackageVersion(packageName, version);
        return new NpmRecipeBundle(packageVersion, team);
    }
}
