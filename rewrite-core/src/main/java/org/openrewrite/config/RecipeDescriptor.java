/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.config;

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.With;
import org.openrewrite.internal.lang.Nullable;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Set;

@Value
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class RecipeDescriptor {
    @EqualsAndHashCode.Include
    String name;

    String displayName;

    String description;

    Set<String> tags;

    @Nullable
    Duration estimatedEffortPerOccurrence;

    @EqualsAndHashCode.Include
    /*~~>*/List<OptionDescriptor> options;

    @Deprecated
    /*~~>*/List<String> languages;

    @With
    /*~~>*/List<RecipeDescriptor> recipeList;

    URI source;
}
