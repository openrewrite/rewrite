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
package org.openrewrite;

import org.jspecify.annotations.Nullable;
import org.openrewrite.config.DataTableDescriptor;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Abstract base class for recipes that delegate some or all their behavior to another recipe.
 */
public abstract class DecoratedRecipe extends Recipe implements Recipe.DelegatingRecipe {

    /**
     * @return the recipe to delegate all behavior to
     */
    public abstract Recipe getDelegate();

    @Override
    public String getName() {
        return getDelegate().getName();
    }

    @Override
    public String getDisplayName() {
        return getDelegate().getDisplayName();
    }

    @Override
    public String getDescription() {
        return getDelegate().getDescription();
    }

    @Override
    public String getInstanceName() {
        return getDelegate().getInstanceName();
    }

    @Override
    public String getInstanceNameSuffix() {
        return getDelegate().getInstanceNameSuffix();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return getDelegate().getVisitor();
    }

    @Override
    public List<Recipe> getRecipeList() {
        return getDelegate().getRecipeList();
    }

    @Override
    public boolean causesAnotherCycle() {
        return getDelegate().causesAnotherCycle();
    }

    @Override
    public @Nullable Duration getEstimatedEffortPerOccurrence() {
        return getDelegate().getEstimatedEffortPerOccurrence();
    }

    @Override
    public List<Maintainer> getMaintainers() {
        return getDelegate().getMaintainers();
    }

    @Override
    public List<Contributor> getContributors() {
        return getDelegate().getContributors();
    }

    @Override
    public List<org.openrewrite.config.RecipeExample> getExamples() {
        return getDelegate().getExamples();
    }

    @Override
    public Set<String> getTags() {
        return getDelegate().getTags();
    }

    @Override
    public int maxCycles() {
        return getDelegate().maxCycles();
    }

    @Override
    public List<DataTableDescriptor> getDataTableDescriptors() {
        return getDelegate().getDataTableDescriptors();
    }

    @Override
    public void onComplete(ExecutionContext ctx) {
        getDelegate().onComplete(ctx);
    }

    @Override
    public Validated<Object> validate() {
        return getDelegate().validate();
    }

    @Override
    public Validated<Object> validate(ExecutionContext ctx) {
        return getDelegate().validate(ctx);
    }

    @Override
    public Collection<Validated<Object>> validateAll(ExecutionContext ctx, Collection<Validated<Object>> acc) {
        return getDelegate().validateAll(ctx, acc);
    }
}
