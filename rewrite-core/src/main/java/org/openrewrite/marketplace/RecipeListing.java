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
package org.openrewrite.marketplace;

import lombok.*;
import org.jspecify.annotations.Nullable;
import org.openrewrite.NlsRewrite;
import org.openrewrite.Recipe;
import org.openrewrite.config.DataTableDescriptor;
import org.openrewrite.config.OptionDescriptor;
import org.openrewrite.config.RecipeDescriptor;

import java.time.Duration;
import java.util.*;

@Getter
@RequiredArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class RecipeListing implements Comparable<RecipeListing> {
    /**
     * The marketplace that this listing belongs to.
     */
    private final @Nullable RecipeMarketplace marketplace;

    private final @EqualsAndHashCode.Include String name;
    private final @NlsRewrite.DisplayName String displayName;
    private final @NlsRewrite.Description String description;
    private final @Nullable Duration estimatedEffortPerOccurrence;
    private final List<OptionDescriptor> options;
    private final List<DataTableDescriptor> dataTables;

    /**
     * The count of all recipes listed in {@link Recipe#getRecipeList()} both directly
     * and transitively. This simple count is a useful measure to expose in the recipe
     * marketplace because it can be used as a sorting criteria. In the "more is better"
     * view of relevance, the higher the recipe count, the more likely the recipe is to
     * be complete.
     * <br>
     * It also recognizes another design consideration. Migration recipes generally are
     * built incrementally by version so that developers can use migration recipes to get
     * to a version short of the absolute latest version that a recipe is available for.
     * Migration recipes for a particular version compose the migration recipe from the prior
     * version. As a result, the latest available version migration will "by construction" always
     * have a greater recipe count than a migration for an earlier version.
     */
    private final int recipeCount;

    private final Map<String, Object> metadata = new LinkedHashMap<>();

    private final RecipeBundle bundle;

    public RecipeListing withMarketplace(@Nullable RecipeMarketplace marketplace) {
        if (this.marketplace == marketplace) {
            return this;
        }
        RecipeListing copy = new RecipeListing(marketplace, name, displayName, description,
                estimatedEffortPerOccurrence, options, dataTables, recipeCount, bundle);
        copy.metadata.putAll(this.metadata);
        return copy;
    }

    RecipeListing withBundle(RecipeBundle bundle) {
        if (this.bundle == bundle) {
            return this;
        }
        RecipeListing copy = new RecipeListing(marketplace, name, displayName, description,
                estimatedEffortPerOccurrence, options, dataTables, recipeCount, bundle);
        copy.metadata.putAll(this.metadata);
        return copy;
    }

    private RecipeBundleReader resolve(Collection<RecipeBundleResolver> resolvers) {
        for (RecipeBundleResolver resolver : resolvers) {
            if (resolver.getEcosystem().equals(bundle.getPackageEcosystem())) {
                return resolver.resolve(bundle);
            }
        }
        throw new IllegalStateException(String.format("No available resolver for '%s' ecosystem", bundle.getPackageEcosystem()));
    }

    public RecipeDescriptor describe(Collection<RecipeBundleResolver> resolvers) {
        // noinspection resource
        return resolve(resolvers).describe(this);
    }

    public Recipe prepare(Collection<RecipeBundleResolver> resolvers, Map<String, Object> options) {
        // noinspection resource
        return resolve(resolvers).prepare(this, options);
    }

    @Override
    public int compareTo(RecipeListing o) {
        return name.compareTo(o.name);
    }

    public static RecipeListing fromDescriptor(RecipeDescriptor descriptor, RecipeBundle bundle) {
        int recipeCount = 1;
        for (Queue<RecipeDescriptor> queue = new LinkedList<>(descriptor.getRecipeList()); !queue.isEmpty(); ) {
            RecipeDescriptor d = queue.poll();
            recipeCount++;
            queue.addAll(d.getRecipeList());
        }

        return new RecipeListing(null, descriptor.getName(),
                descriptor.getDisplayName(),
                descriptor.getDescription(),
                descriptor.getEstimatedEffortPerOccurrence(),
                descriptor.getOptions(),
                descriptor.getDataTables(),
                recipeCount,
                bundle
        );
    }
}
