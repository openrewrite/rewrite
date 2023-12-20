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
import org.openrewrite.Contributor;
import org.openrewrite.Incubating;
import org.openrewrite.Maintainer;
import org.openrewrite.internal.lang.Nullable;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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
    List<OptionDescriptor> options;

    @With
    List<RecipeDescriptor> recipeList;

    @With
    List<DataTableDescriptor> dataTables;

    List<Maintainer> maintainers;

    List<Contributor> contributors;

    List<RecipeExample> examples;

    URI source;

    @Incubating(since = "8.11.3")
    public void print(Consumer<String> consumer) {
        StringBuilder recipeString = new StringBuilder(name);

        if (options != null && !options.isEmpty()) {
            String opts = options.stream().map(option -> {
                if (option.getValue() != null) {
                    return String.format("%s=%s", option.getName(), option.getValue());
                }
                return null;
            }).filter(Objects::nonNull).collect(Collectors.joining(", "));
            if (!opts.isEmpty()) {
                recipeString.append(String.format(": {%s}", opts));
            }
        }

        consumer.accept(recipeString.toString());
    }
}
