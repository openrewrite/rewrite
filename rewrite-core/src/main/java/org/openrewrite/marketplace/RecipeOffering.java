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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.NlsRewrite;

import java.time.Duration;
import java.util.List;
import java.util.Set;

/**
 * Offered by the marketplace, but not yet installed and/or
 * loaded for use. An offering turns into a runnable recipe
 * through its load method.
 */
@Value
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class RecipeOffering implements RecipeListing {
    @EqualsAndHashCode.Include
    String name;

    @NlsRewrite.DisplayName
    String displayName;

    @NlsRewrite.DisplayName
    String instanceName;

    @NlsRewrite.Description
    String description;

    Set<String> tags;

    @Nullable
    Duration estimatedEffortPerOccurrence;

    List<Option> options;

    @Nullable RecipeBundle bundle;

    @Value
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    public static class Option implements RecipeListing.Option {
        @EqualsAndHashCode.Include
        String name;

        @NlsRewrite.DisplayName
        String displayName;

        @NlsRewrite.Description
        String description;
    }
}
