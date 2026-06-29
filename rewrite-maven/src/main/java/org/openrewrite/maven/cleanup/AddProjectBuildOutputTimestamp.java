/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.maven.cleanup;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.maven.AddProperty;

import java.util.List;

import static java.util.Collections.singletonList;

@Value
@EqualsAndHashCode(callSuper = false)
public class AddProjectBuildOutputTimestamp extends Recipe {

    @Option(displayName = "Timestamp",
            description = "ISO 8601 timestamp, integer seconds since the epoch, or property reference such as " +
                          "`${git.commit.author.time}`. Defaults to `1980-01-01T00:00:00Z`, the earliest value " +
                          "the ZIP format can represent.",
            example = "2024-01-01T00:00:00Z",
            required = false)
    @Nullable
    String timestamp;

    @Override
    public String getDisplayName() {
        return "Add `project.build.outputTimestamp` for reproducible builds";
    }

    @Override
    public String getDescription() {
        return "Adds the `project.build.outputTimestamp` property, which Maven uses to make build outputs " +
               "reproducible by stamping archive entries with a fixed timestamp instead of the current time. " +
               "An existing value is preserved. " +
               "See [Configuring for Reproducible Builds](https://maven.apache.org/guides/mini/guide-reproducible-builds.html).";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return singletonList(new AddProperty(
                "project.build.outputTimestamp",
                timestamp == null ? "1980-01-01T00:00:00Z" : timestamp,
                true,
                false));
    }
}
