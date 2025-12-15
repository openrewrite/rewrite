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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
@RequiredArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class RecipeListing implements Comparable<RecipeListing> {
    /**
     * The marketplace that this listing belongs to.
     */
    @With
    private final @Nullable RecipeMarketplace marketplace;

    private final @EqualsAndHashCode.Include String name;
    private final @NlsRewrite.DisplayName String displayName;
    private final @NlsRewrite.Description String description;
    private final @Nullable Duration estimatedEffortPerOccurrence;
    private final List<OptionDescriptor> options;
    private final List<DataTableDescriptor> dataTables;
    private final Map<String, Object> metadata = new LinkedHashMap<>();

    @With(AccessLevel.PACKAGE)
    private final RecipeBundle bundle;

    public RecipeBundleReader resolve() {
        if (marketplace != null) {
            for (RecipeBundleResolver resolver : marketplace.getResolvers()) {
                if (resolver.getEcosystem().equals(bundle.getPackageEcosystem())) {
                    return resolver.resolve(bundle);
                }
            }
        }
        throw new IllegalStateException("This listing has not been configured with a resolver.");
    }

    public RecipeDescriptor describe() {
        return resolve().describe(this);
    }

    public Recipe prepare(Map<String, Object> options) {
        return resolve().prepare(this, options);
    }

    @Override
    public int compareTo(RecipeListing o) {
        return name.compareTo(o.name);
    }

    public static RecipeListing fromDescriptor(RecipeDescriptor descriptor, RecipeBundle bundle) {
        return new RecipeListing(null, descriptor.getName(),
                descriptor.getDisplayName(),
                descriptor.getDescription(),
                descriptor.getEstimatedEffortPerOccurrence(),
                descriptor.getOptions(),
                descriptor.getDataTables(),
                bundle
        );
    }
}
