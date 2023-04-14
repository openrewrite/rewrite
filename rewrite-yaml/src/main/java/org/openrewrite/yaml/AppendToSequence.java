/*
 * Copyright 2023 the original author or authors.
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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.HasSourcePath;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;

import java.util.List;

@Value
@EqualsAndHashCode(callSuper = true)
public class AppendToSequence extends Recipe {
    @Option(displayName = "sequence path",
            description = "A JsonPath expression to locate a YAML sequence.",
            example = "$.universe.planets")
    String sequencePath;

    @Option(displayName = "New value",
            description = "The new value to be appended to the sequence.",
            example = "earth")
    String value;

    @Option(displayName = "Optional: match existing sequence values",
            description = "Rule applies only when existing sequence values match",
            example = "a,b,c",
            required = false)
    @Nullable
    List<String> existingSequenceValues;

    @Option(displayName = "Optional file matcher",
            description = "Matching files will be modified. This is a glob expression.",
            required = false,
            example = "**/application-*.yml")
    @Nullable
    String fileMatcher;

    @Override
    public String getDisplayName() {
        return "Append to sequence";
    }

    @Override
    public String getDescription() {
        return "Append item to YAML sequence.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        if (fileMatcher != null) {
            return new HasSourcePath<>(fileMatcher);
        }
        return null;
    }

    @Override
    public YamlVisitor<ExecutionContext> getVisitor() {
        JsonPathMatcher matcher = new JsonPathMatcher(sequencePath);
        return new AppendToSequenceVisitor(matcher, value, existingSequenceValues);
    }
}