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

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.openrewrite.Recipe;
import org.openrewrite.config.RecipeDescriptor;

import java.util.Map;

@RequiredArgsConstructor
public class ThrowingRecipeBundleReader implements RecipeBundleReader {
    private final @Getter RecipeBundle bundle;
    private final RuntimeException t;

    @Override
    public RecipeMarketplace read() {
        throw t;
    }

    @Override
    public RecipeDescriptor describe(RecipeListing listing) {
        throw t;
    }

    @Override
    public Recipe prepare(RecipeListing listing, Map<String, Object> options) {
        throw t;
    }

    @Override
    public ClassLoader classLoader() {
        throw t;
    }
}
