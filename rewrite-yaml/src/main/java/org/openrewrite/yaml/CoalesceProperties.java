/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.yaml;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;

import java.util.List;

import static java.util.Collections.emptyList;

@Value
@EqualsAndHashCode(callSuper = false)
public class CoalesceProperties extends Recipe {
    @Option(displayName = "Exclusions",
            description = "An optional list of [JsonPath](https://docs.openrewrite.org/reference/jsonpath-and-jsonpathmatcher-reference) expressions to specify keys that should not be unfolded.",
            example = "$..[org.springframework.security]",
            required = false)
    @Nullable List<String> exclusions;

    @Option(displayName = "Apply to",
            description = "An optional list of [JsonPath](https://docs.openrewrite.org/reference/jsonpath-and-jsonpathmatcher-reference) expressions that specify which keys the recipe should target only. " +
                    "Only the properties matching these expressions will be unfolded.",
            example = "$..[org.springframework.security]",
            required = false)
    @Nullable List<String> applyTo;

    @Deprecated
    public CoalesceProperties() {
        this(null, null);
    }

    @JsonCreator
    public CoalesceProperties(@Nullable final List<String> exclusions, @Nullable final List<String> applyTo) {
        this.exclusions = exclusions == null ? emptyList() : exclusions;
        this.applyTo = applyTo == null ? emptyList() : applyTo;
    }

    @Override
    public String getDisplayName() {
        return "Coalesce YAML properties";
    }

    @Override
    public String getDescription() {
        return "Simplify nested map hierarchies into their simplest dot separated property form, similar to how Spring Boot interprets `application.yml` files.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new CoalescePropertiesVisitor<>(exclusions, applyTo);
    }
}
